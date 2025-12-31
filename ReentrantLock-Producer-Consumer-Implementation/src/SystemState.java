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
    private boolean isActiveWriter = false;

    // Blocking time tracking
    private long totalReaderBlockingTime = 0;
    private long totalWriterBlockingTime = 0;
    private int readerBlockCount = 0;
    private int writerBlockCount = 0;

    public synchronized void incrementSubmitted() { totalSubmitted++; }
    public synchronized void incrementProcessed() { totalProcessed++; }
    public synchronized void incrementRejected() { totalRejected++; }
    public synchronized void incrementExpired() { totalExpired++; }

    // WRITER-PREFERRING POLICY
    public synchronized void acquireRead() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        boolean wasBlocked = false;

        // Wait if there's an active writer OR if writers are waiting
        while (isActiveWriter || waitingWriters > 0) {
            wasBlocked = true;
            wait();
        }

        // Track blocking time if reader was blocked
        if (wasBlocked) {
            long blockingTime = System.currentTimeMillis() - startTime;
            totalReaderBlockingTime += blockingTime;
            readerBlockCount++;
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
        long startTime = System.currentTimeMillis();
        boolean wasBlocked = false;

        waitingWriters++;

        // Wait if there are active readers or an active writer
        while (activeReaders > 0 || isActiveWriter) {
            wasBlocked = true;
            wait();
        }

        // Track blocking time if writer was blocked
        if (wasBlocked) {
            long blockingTime = System.currentTimeMillis() - startTime;
            totalWriterBlockingTime += blockingTime;
            writerBlockCount++;
        }

        waitingWriters--;
        isActiveWriter = true;
    }

    public synchronized void releaseWrite() {
        isActiveWriter = false;
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

    // Check mode and policy before Consume by Analyzer for processing
    public synchronized boolean isMaintenanceMode() {
        return maintenanceMode;
    }
    public synchronized ProcessingPolicy getCurrentPolicy() throws InterruptedException {
        return currentPolicy;
    }


    // Configuration change for supervisors
    public void setSystemState(ProcessingPolicy newPolicy, int capacity, boolean isMaintenanceMode) throws InterruptedException{
        acquireWrite();
        try {
            this.currentPolicy = newPolicy;
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

    // Blocking time statistics
    public synchronized double getAverageReaderBlockingTime() {
        return readerBlockCount > 0 ? (double) totalReaderBlockingTime / readerBlockCount : 0.0;
    }

    public synchronized double getAverageWriterBlockingTime() {
        return writerBlockCount > 0 ? (double) totalWriterBlockingTime / writerBlockCount : 0.0;
    }
}
