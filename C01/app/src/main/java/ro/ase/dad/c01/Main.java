package ro.ase.dad.c01;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static String hex(byte[] b, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, b.length); i++) {
            sb.append(String.format("%02X ", b[i]));
        }
        return sb.toString().trim();
    }

    private static boolean isBmp(byte[] b) {
        return b != null && b.length >= 2 && b[0] == 'B' && b[1] == 'M';
    }

    public static void main(String[] args) {
        String brokerHost = System.getenv().getOrDefault("JMS_BROKER_HOST", "c02");
        int brokerPort = Integer.parseInt(System.getenv().getOrDefault("JMS_BROKER_PORT", "61616"));
        String jmsUser = System.getenv().getOrDefault("JMS_USER", "dad");
        String jmsPass = System.getenv().getOrDefault("JMS_PASS", "dad");

        String bmpTopic = System.getenv().getOrDefault("JMS_BMP_TOPIC", "bmp.topic");
        String doneQueue = System.getenv().getOrDefault("JMS_DONE_QUEUE", "job.done.queue");

        String c06Base = System.getenv().getOrDefault("C06_BASE_URL", "http://c06:3000");

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8081"));

        JobStore store = new JobStore();
        Set<WsContext> wsSessions = ConcurrentHashMap.newKeySet();

        JmsClient jms = new JmsClient(brokerHost, brokerPort, jmsUser, jmsPass, bmpTopic, doneQueue);

        // consumer for DONE notifications from C03
        jms.startDoneConsumer((jobId, imageId) -> {
            String downloadUrl = c06Base + "/images/" + imageId;
            store.markDone(jobId, imageId, downloadUrl);

            String payload = "{\"jobId\":\"" + jobId + "\",\"status\":\"DONE\",\"imageId\":" + imageId
                    + ",\"downloadUrl\":\"" + downloadUrl + "\"}";

            for (WsContext s : wsSessions) {
                try { s.send(payload); } catch (Exception ignored) {}
            }
        });

        Javalin app = Javalin.create(cfg -> {
            cfg.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
            cfg.http.maxRequestSize = 200_000_000L; // 200MB
            cfg.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        });

        // WS for live updates (frontend: ws://localhost:8081/ws)
        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> wsSessions.add(ctx));
            ws.onClose(ctx -> wsSessions.remove(ctx));
            ws.onError(ctx -> wsSessions.remove(ctx));
        });

        // health
        app.get("/health", ctx -> ctx.json(Map.of("ok", true)));

        // create job: multipart file + params
        app.post("/api/jobs", ctx -> {
            UploadedFile file = ctx.uploadedFile("file");
            if (file == null) {
                ctx.status(400).json(Map.of("error", "Missing multipart field 'file'"));
                return;
            }

            String zoomInStr = ctx.formParam("zoomIn");
            if (zoomInStr == null || zoomInStr.isBlank()) zoomInStr = "true";
            boolean zoomIn = Boolean.parseBoolean(zoomInStr);

            String percentStr = ctx.formParam("percent");
            if (percentStr == null || percentStr.isBlank()) percentStr = "20";
            int percent = Integer.parseInt(percentStr);

            String filename = (file.filename() == null || file.filename().isBlank()) ? "input.bmp" : file.filename();
            byte[] bmpBytes = file.content().readAllBytes();

            System.out.println("C01: filename=" + filename + " size=" + bmpBytes.length + " header=" + hex(bmpBytes, 16));

            if (!isBmp(bmpBytes)) {
                ctx.status(400).json(Map.of(
                        "error", "Uploaded file is NOT a BMP. First bytes=" + hex(bmpBytes, 8)
                ));
                return;
            }

            String jobId = UUID.randomUUID().toString();
            store.markPending(jobId);

            jms.publishBmpJob(jobId, zoomIn, percent, filename, bmpBytes);
            System.out.println("IMAGE SENT");

            ctx.json(Map.of("jobId", jobId, "status", "PENDING"));
        });

        // polling status
        app.get("/api/jobs/{jobId}", ctx -> {
            String jobId = ctx.pathParam("jobId");
            JobStore.JobStatus st = store.get(jobId);

            if (st == null) {
                ctx.status(404).json(Map.of("error", "Unknown jobId"));
                return;
            }
            if ("DONE".equals(st.status) && st.imageId != null) {
                ctx.json(Map.of(
                        "jobId", st.jobId,
                        "status", "DONE",
                        "imageId", st.imageId,
                        "downloadUrl", st.downloadUrl
                ));
            } else {
                ctx.json(Map.of("jobId", st.jobId, "status", st.status));
            }
        });

        // redirect to C06 for download
        app.get("/api/download/{jobId}", ctx -> {
            String jobId = ctx.pathParam("jobId");
            JobStore.JobStatus st = store.get(jobId);

            if (st == null) {
                ctx.status(404).json(Map.of("error", "Unknown jobId"));
                return;
            }
            if (!"DONE".equals(st.status) || st.imageId == null) {
                ctx.status(202).result("Not ready");
                return;
            }
            ctx.redirect(st.downloadUrl);
        });

        app.start("0.0.0.0", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { jms.close(); } catch (Exception ignored) {}
        }));

        System.out.println("C01 started on port " + port);
    }
}