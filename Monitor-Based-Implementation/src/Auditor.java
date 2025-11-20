//Reader thread
class Auditor implements Runnable {
    private final int id;
    private final SystemState state;
    private final int reportInterval; // ms
    private volatile boolean running = true;

    public Auditor(int id, SystemState state, int reportInterval) {
        this.id = id;
        this.state = state;
        this.reportInterval = reportInterval;
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                SystemSnapshot snapshot = state.getSnapshot();

                System.out.println("[AUDITOR-" + id + "] Report at " +
                        snapshot.timestamp + ": " +
                        "Submitted=" + snapshot.totalSubmitted +
                        ", Processed=" + snapshot.totalProcessed +
                        ", Policy=" + snapshot.priorityPolicy);

                Thread.sleep(reportInterval);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[AUDITOR-" + id + "] Shutting down gracefully");
        }
    }

    public void shutdown() {
        running = false;
    }
}