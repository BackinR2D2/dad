package ro.ase.dad.c01;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobStore {

    public static class JobStatus {
        public final String jobId;
        public volatile String status; // PENDING | DONE
        public volatile Long imageId;
        public volatile String downloadUrl;

        public JobStatus(String jobId) {
            this.jobId = jobId;
            this.status = "PENDING";
        }
    }

    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();

    public JobStatus markPending(String jobId) {
        JobStatus js = new JobStatus(jobId);
        jobs.put(jobId, js);
        return js;
    }

    public JobStatus markDone(String jobId, long imageId, String downloadUrl) {
        JobStatus js = jobs.computeIfAbsent(jobId, JobStatus::new);
        js.status = "DONE";
        js.imageId = imageId;
        js.downloadUrl = downloadUrl;
        return js;
    }

    public JobStatus get(String jobId) {
        return jobs.get(jobId);
    }
}