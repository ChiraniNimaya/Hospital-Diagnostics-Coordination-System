//Shared state for Reader-Writer pattern
public class SystemState {
    private int totalSubmitted = 0;
    private int totalProcessed = 0;
    private int totalExpired = 0;
    private int activeAnalyzers = 0;
    private String priorityPolicy = "FIFO";
    private boolean maintenanceMode = false;

    // Reader-Writer control using monitors
    private int activeReaders = 0;
    private int waitingWriters = 0;
    private boolean activeWriter = false;

    // Metrics
    public synchronized void incrementSubmitted() { totalSubmitted++; }
    public synchronized void incrementProcessed() { totalProcessed++; }
    public synchronized void incrementExpired() { totalExpired++; }

    // READER-PREFERRING POLICY (can be modified for writer-preferring)
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
    public synchronized SystemSnapshot getSnapshot() throws InterruptedException {
        acquireRead();
        try {
            return new SystemSnapshot(totalSubmitted, totalProcessed,
                    totalExpired, activeAnalyzers,
                    priorityPolicy, maintenanceMode);
        } finally {
            releaseRead();
        }
    }

    // Configuration change for supervisors
    public synchronized void updatePolicy(String newPolicy) throws InterruptedException {
        acquireWrite();
        try {
            this.priorityPolicy = newPolicy;
            System.out.println("[SUPERVISOR] Policy changed to: " + newPolicy);
        } finally {
            releaseWrite();
        }
    }

    public synchronized void setMaintenanceMode(boolean mode) throws InterruptedException {
        acquireWrite();
        try {
            this.maintenanceMode = mode;
            System.out.println("[SUPERVISOR] Maintenance mode: " + mode);
        } finally {
            releaseWrite();
        }
    }
}
