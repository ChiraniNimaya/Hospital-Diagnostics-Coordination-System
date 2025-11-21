//Shared state for Reader-Writer pattern
class SystemState {
    private int totalSubmitted = 0;
    private int totalProcessed = 0;
    private int maxAnalyzerCapacity = 10;
    private int currentActiveAnalyzers = 0;

    // Functional policy that actually affects behavior
    public enum ProcessingPolicy {
        FIFO,              // First In First Out (default)
        PRIORITY,          // Priority-based (Emergency > Urgent > Routine)
        EMERGENCY_FIRST    // Only emergency orders processed first
    }

    private ProcessingPolicy currentPolicy = ProcessingPolicy.FIFO;
    private boolean maintenanceMode = false;

    // Reader-Writer control using monitors
    private int activeReaders = 0;
    private int waitingWriters = 0;
    private boolean activeWriter = false;

    // Timestamp for snapshot
    private long timestamp;

    public synchronized void incrementSubmitted() { totalSubmitted++; }
    public synchronized void incrementProcessed() { totalProcessed++; }

    // READER-PREFERRING POLICY
    public synchronized void acquireRead() throws InterruptedException {
        // Wait if there's an active writer
        while (activeWriter) {
            wait();
        }
        activeReaders++;
    }

    public synchronized void releaseRead() {
        activeReaders--;
        if (activeReaders == 0) {
            notifyAll(); // Wake up waiting writers
        }
    }

    public synchronized void acquireWrite() throws InterruptedException {
        waitingWriters++;
        // Wait if there are active readers or an active writer
        while (activeReaders > 0 || activeWriter) {
            wait();
        }
        waitingWriters--;
        activeWriter = true;
    }

    public synchronized void releaseWrite() {
        activeWriter = false;
        notifyAll(); // Wake up all waiting readers and writers
    }

    // Snapshot for auditors
    public SystemSnapshot getSnapshot() throws InterruptedException {
        acquireRead();
        try {
            return new SystemSnapshot(
                    totalSubmitted,
                    totalProcessed,
                    currentActiveAnalyzers,
                    maxAnalyzerCapacity,
                    currentPolicy.toString(),
                    maintenanceMode
            );
        } finally {
            releaseRead();
        }
    }

    // Configuration change for supervisors
    public void setSystemState(String newPolicy, int capacity, boolean isMaintenanceMode) throws InterruptedException{
        acquireWrite();
        try {
            this.currentPolicy = ProcessingPolicy.valueOf(newPolicy);
            System.out.println("[SUPERVISOR] Policy changed to: " + newPolicy);
            this.maxAnalyzerCapacity = capacity;
            System.out.println("[SUPERVISOR] Analyzer capacity set to: " + capacity);
            this.maintenanceMode = isMaintenanceMode;
            System.out.println("[SUPERVISOR] Maintenance mode: " + isMaintenanceMode);
        } finally {
            releaseWrite();
        }
    }

}
