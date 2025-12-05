//Writer thread
class Supervisor implements Runnable {
    private final int id;
    private SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;
    private boolean newMaintenanceMode = false;
    private int newMaxAnalyzerCapacity = 4;
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
            while (true) {

                if (!running || Thread.currentThread().isInterrupted()) {
                    System.out.println("[SUPERVISOR-" + id + "] Shutting down gracefully");
                    break;
                }

                cycleCount++;

                // Set maintenance mode in every 5th cycle
                if (cycleCount % 5 == 0) {
                    newMaintenanceMode = true;
                } else {
                    newMaintenanceMode = false;
                }

                try {
                    Thread.sleep(updateInterval);
                    currentState.setSystemState(newPolicy, newMaxAnalyzerCapacity, newMaintenanceMode);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("[SUPERVISOR-" + id + "] Shutting down gracefully");
                    break;
                }

                System.out.println("[SUPERVISOR-" + id + "] Reconfiguration of System State : " +
                        "Policy = " + newPolicy.toString() +
                        ", Analyzer Capacity = " + newMaxAnalyzerCapacity +
                        ", Maintenance Mode = " + ((newMaintenanceMode) ? "Yes" : "No"));

            }
    }

    public void shutdown() {
        running = false;
    }
}