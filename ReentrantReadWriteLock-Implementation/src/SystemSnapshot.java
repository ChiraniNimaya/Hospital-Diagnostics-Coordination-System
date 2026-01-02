public class SystemSnapshot {
    public final int totalSubmitted, totalProcessed, totalRejected, totalExpired, maxAnalyzerCapacity, activeAnalyzerSlots;
    public final String currentPolicy;
    public final boolean maintenanceMode;
    public final long timestamp;

    public SystemSnapshot(int submitted, int processed, int rejected, int expired,
                          int capacity, int active, String policy, boolean maintenance) {
        this.totalSubmitted = submitted;
        this.totalProcessed = processed;
        this.totalRejected = rejected;
        this.totalExpired = expired;
        this.maxAnalyzerCapacity = capacity;
        this.activeAnalyzerSlots = active;
        this.currentPolicy = policy;
        this.maintenanceMode = maintenance;
        this.timestamp = System.currentTimeMillis();
    }
}
