//Shared state for Reader-Writer pattern
class SystemState {
    private int totalSubmitted = 0;
    private int totalProcessed = 0;
    private int totalRejected = 0;
    private int totalExpired = 0;
    private int maxAnalyzerCapacity = 5;
    private int activeAnalyzerSlots = 0;

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

    public synchronized void incrementSubmitted() { totalSubmitted++; }
    public synchronized void incrementProcessed() { totalProcessed++; }
    public synchronized void incrementRejected() { totalRejected++; }
    public synchronized void incrementExpired() { totalExpired++; }

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
                    totalRejected,
                    totalExpired,
                    maxAnalyzerCapacity,
                    activeAnalyzerSlots,
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
            this.maxAnalyzerCapacity = capacity;
            this.maintenanceMode = isMaintenanceMode;
        } finally {
            releaseWrite();
        }
    }

    //Analyzer slot acquisition
    public synchronized void acquireAnalyzerSlot() throws InterruptedException {
        while (activeAnalyzerSlots == maxAnalyzerCapacity) {
            wait();
        }
        activeAnalyzerSlots++;
    }

    public synchronized void releaseAnalyzerSlot() {
        activeAnalyzerSlots--;
        notifyAll();
    }
}
