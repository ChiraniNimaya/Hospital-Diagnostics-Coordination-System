//Writer thread using writeLock()
class Supervisor implements Runnable {
    private final String name;
    private SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;
    private boolean newMaintenanceMode = false;
    private int newMaxAnalyzerCapacity;
    private SystemState currentState;
    private final int updateInterval;
    private volatile boolean running = true;
    private int cycleCount = 0;

    public Supervisor(String name, SystemState currentState,
                      SystemState.ProcessingPolicy newPolicy,
                      int newMaxAnalyzerCapacity, int updateInterval) {
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

            // Set maintenance mode every 5th cycle
            newMaintenanceMode = (cycleCount % 5 == 0);

            try {
                Thread.sleep(updateInterval);

                // WRITE operation - uses writeLock() internally
                currentState.setSystemState(newPolicy, newMaxAnalyzerCapacity, newMaintenanceMode);

                System.out.println("[" + name + "] Reconfiguration of System State : " +
                        "Policy = " + newPolicy.toString() +
                        ", Analyzer Capacity = " + newMaxAnalyzerCapacity +
                        ", Maintenance Mode = " + (newMaintenanceMode ? "Yes" : "No"));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[" + name + "] Shutting down gracefully");
                break;
            }
        }
    }

    public void shutdown() {
        running = false;
    }
}