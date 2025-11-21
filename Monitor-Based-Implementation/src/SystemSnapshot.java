public class SystemSnapshot {
    public final int totalSubmitted, totalProcessed, currentActiveAnalyzers, maxAnalyzerCapacity;
    public final String currentPolicy;
    public final boolean maintenanceMode;
    public final long timestamp;

    public SystemSnapshot(int submitted, int processed,
                          int active, int capacity, String policy, boolean maintenance) {
        this.totalSubmitted = submitted;
        this.totalProcessed = processed;
        this.currentActiveAnalyzers = active;
        this.maxAnalyzerCapacity = capacity;
        this.currentPolicy = policy;
        this.maintenanceMode = maintenance;
        this.timestamp = System.currentTimeMillis();
    }
}
