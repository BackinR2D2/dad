package ro.ase.dad.c03;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpToC06Client {

    private static final String C06_BASE = getenv("C06_BASE_URL", "http://c06:3000");

    public long storeImage(byte[] bmp, String filename, boolean zoomIn, int percent) throws Exception {
        String urlStr = C06_BASE + "/images"
                + "?filename=" + enc(filename == null ? "result.bmp" : filename)
                + "&mime=" + enc("image/bmp")
                + "&zoomIn=" + zoomIn
                + "&percent=" + percent;

        HttpURLConnection con = null;
        try {
            URL url = new URL(urlStr);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(30000);
            con.setRequestProperty("Content-Type", "application/octet-stream");
            con.setRequestProperty("Content-Length", String.valueOf(bmp.length));

            try (OutputStream os = con.getOutputStream()) {
                os.write(bmp);
            }

            int code = con.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new RuntimeException("C06 storeImage HTTP " + code);
            }

            try (InputStream is = con.getInputStream()) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return extractId(json);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static long extractId(String json) {
        int i = json.indexOf("\"id\"");
        if (i < 0) throw new RuntimeException("No id in response: " + json);
        int c = json.indexOf(":", i);
        int end = json.indexOf(",", c);
        if (end < 0) end = json.indexOf("}", c);
        return Long.parseLong(json.substring(c + 1, end).trim());
    }

    private static String getenv(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
