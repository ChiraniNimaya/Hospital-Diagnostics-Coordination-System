//Writer thread
public class Supervisor implements Runnable {
    private final SystemState state;
    private final int updateInterval; // ms
    private volatile boolean running = true;

    public Supervisor(SystemState state, int updateInterval) {
        this.state = state;
        this.updateInterval = updateInterval;
    }

    @Override
    public void run() {
        String[] policies = {"FIFO", "PRIORITY", "EMERGENCY_FIRST"};
        int policyIndex = 0;

        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(updateInterval);

                policyIndex = (policyIndex + 1) % policies.length;
                state.updatePolicy(policies[policyIndex]);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[SUPERVISOR] Shutting down gracefully");
        }
    }

    public void shutdown() {
        running = false;
    }
}
