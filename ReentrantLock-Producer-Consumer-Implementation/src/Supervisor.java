//Writer thread
class Supervisor implements Runnable {
    private final String name;    private SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;
    private boolean newMaintenanceMode = false;
    private int newMaxAnalyzerCapacity;
    private SystemState currentState;
    private final int updateInterval; // ms
    private volatile boolean running = true;

    // cycle count tracker for maintenance
    private int cycleCount = 0;

    public Supervisor(String name, SystemState currentState, SystemState.ProcessingPolicy newPolicy, int newMaxAnalyzerCapacity, int updateInterval) {
        this.name = name;
        this.currentState = currentState;
        this.newPolicy = newPolicy;
        this.newMaxAnalyzerCapacity = newMaxAnalyzerCapacity;
        this.updateInterval = updateInterval;
    }

    @Override
    public void run() {
            while (true) {

                if (!running || Thread.currentThread().isInterrupted()) {
                    System.out.println("[" + name + "] Shutting down gracefully");
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
                    System.out.println("[" + name + "] Shutting down gracefully");
                    break;
                }

                System.out.println("[" + name + "] Reconfiguration of System State : " +
                        "Policy = " + newPolicy.toString() +
                        ", Analyzer Capacity = " + newMaxAnalyzerCapacity +
                        ", Maintenance Mode = " + ((newMaintenanceMode) ? "Yes" : "No"));

            }
    }

    public void shutdown() {
        running = false;
    }
}