//Writer thread
class Supervisor implements Runnable {
    private final int id;
    private SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;
    private boolean newMaintenanceMode = false;
    private int newMaxAnalyzerCapacity = 5;
    private SystemState currentState;
    private final int updateInterval; // ms
    private volatile boolean running = true;

    // cycle count tracker for maintenance
    private int cycleCount = 0;

    public Supervisor(int id, SystemState currentState, SystemState.ProcessingPolicy newPolicy, int newMaxAnalyzerCapacity, int updateInterval) {
        this.id = id;
        this.currentState = currentState;
        this.newPolicy = newPolicy;
        this.newMaxAnalyzerCapacity = newMaxAnalyzerCapacity;
        this.updateInterval = updateInterval;
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(updateInterval);
                cycleCount++;

                // Set maintence mode in every 25th cycle
                if (cycleCount % 25 == 0) {
                    newMaintenanceMode = true;
                } else {
                    newMaintenanceMode = false;
                }

                currentState.setSystemState(newPolicy, newMaxAnalyzerCapacity, newMaintenanceMode);
                System.out.println("[SUPERVISOR-" + id + "] Reconfiguration of System State : " +
                        "Policy = " + newPolicy.toString() +
                        ", Analyzer Capacity = " + newMaxAnalyzerCapacity +
                        ", Maintenance Mode = " + ((newMaintenanceMode) ? "Yes" : "No"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[SUPERVISOR-" + id + "] Shutting down gracefully");
        }
    }

    public void shutdown() {
        running = false;
    }
}