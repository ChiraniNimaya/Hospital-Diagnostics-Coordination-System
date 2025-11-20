public class SystemSnapshot {
    public final int totalSubmitted, totalProcessed, totalExpired, activeAnalyzers;
    public final String priorityPolicy;
    public final boolean maintenanceMode;
    public final long timestamp;

    public SystemSnapshot(int submitted, int processed, int expired,
                          int active, String policy, boolean maintenance) {
        this.totalSubmitted = submitted;
        this.totalProcessed = processed;
        this.totalExpired = expired;
        this.activeAnalyzers = active;
        this.priorityPolicy = policy;
        this.maintenanceMode = maintenance;
        this.timestamp = System.currentTimeMillis();
    }
}
