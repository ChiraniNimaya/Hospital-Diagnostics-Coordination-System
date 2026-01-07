import java.util.concurrent.locks.ReentrantReadWriteLock;

// Shared state for Reader-Writer pattern using Reentrant Read-Write Lock
class SystemState {
    private int totalSubmitted = 0;
    private int totalProcessed = 0;
    private int totalRejected = 0;
    private int totalExpired = 0;
    private int maxAnalyzerCapacity = 5;
    private int activeAnalyzerSlots = 0;

    public enum ProcessingPolicy {
        FIFO,
        PRIORITY,
        EMERGENCY_FIRST
    }

    private ProcessingPolicy currentPolicy = ProcessingPolicy.FIFO;
    private boolean maintenanceMode = false;

    // ReentrantReadWriteLock with fairness parameter
    private final ReentrantReadWriteLock rwLock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;

    // Blocking time tracking
    private long totalReaderBlockingTime = 0;
    private long totalWriterBlockingTime = 0;
    private int readerBlockCount = 0;
    private int writerBlockCount = 0;

    public SystemState(boolean fair) {
        this.rwLock = new ReentrantReadWriteLock(fair);
        this.readLock = rwLock.readLock();
        this.writeLock = rwLock.writeLock();
    }

    // Simple increment methods (using synchronized for atomicity)
    public synchronized void incrementSubmitted() {
        totalSubmitted++;
    }

    public synchronized void incrementProcessed() {
        totalProcessed++;
    }

    public synchronized void incrementRejected() {
        totalRejected++;
    }

    public synchronized void incrementExpired() {
        totalExpired++;
    }

    private void acquireRead() {
        long startTime = System.currentTimeMillis();
        boolean wasBlocked = !readLock.tryLock();

        if (wasBlocked) {
            readLock.lock(); // Block until available
            long blockingTime = System.currentTimeMillis() - startTime;
            synchronized (this) {
                totalReaderBlockingTime += blockingTime;
                readerBlockCount++;
            }
        }
    }

    private void releaseRead() {
        readLock.unlock();
    }

    private void acquireWrite() {
        long startTime = System.currentTimeMillis();
        boolean wasBlocked = !writeLock.tryLock();

        if (wasBlocked) {
            writeLock.lock(); // Block until available
            long blockingTime = System.currentTimeMillis() - startTime;
            synchronized (this) {
                totalWriterBlockingTime += blockingTime;
                writerBlockCount++;
            }
        }
    }

    private void releaseWrite() {
        writeLock.unlock();
    }

    public SystemSnapshot getSnapshot() {
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

    public boolean isMaintenanceMode() {
        readLock.lock();
        try {
            return maintenanceMode;
        } finally {
            readLock.unlock();
        }
    }

    public ProcessingPolicy getCurrentPolicy() {
        readLock.lock();
        try {
            return currentPolicy;
        } finally {
            readLock.unlock();
        }
    }

    public void setSystemState(ProcessingPolicy newPolicy, int capacity, boolean isMaintenanceMode) {
        acquireWrite();
        try {
            this.currentPolicy = newPolicy;
            this.maxAnalyzerCapacity = capacity;
            this.maintenanceMode = isMaintenanceMode;
        } finally {
            releaseWrite();
        }
    }

    public double getAverageReaderBlockingTime() {
        readLock.lock();
        try {
            return readerBlockCount > 0 ? (double) totalReaderBlockingTime / readerBlockCount : 0.0;
        } finally {
            readLock.unlock();
        }
    }

    public double getAverageWriterBlockingTime() {
        readLock.lock();
        try {
            return writerBlockCount > 0 ? (double) totalWriterBlockingTime / writerBlockCount : 0.0;
        } finally {
            readLock.unlock();
        }
    }

    public String getLockStatistics() {
        return String.format(
                "Lock Statistics: ReadLocks=%d, WriteLocks=%d, Queued=%d, Fair=%b",
                rwLock.getReadLockCount(),
                rwLock.getWriteHoldCount(),
                rwLock.getQueueLength(),
                rwLock.isFair()
        );
    }

    public synchronized void acquireAnalyzerSlot() throws InterruptedException {
        while (activeAnalyzerSlots >= maxAnalyzerCapacity) {
            wait();
        }
        activeAnalyzerSlots++;
    }

    public synchronized void releaseAnalyzerSlot() {
        activeAnalyzerSlots--;
        notifyAll();
    }
}