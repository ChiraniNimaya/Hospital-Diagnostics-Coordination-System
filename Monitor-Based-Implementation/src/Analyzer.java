//Consumer thread
public class Analyzer implements Runnable {
    private final String name;
    private final BoundedQueue queue;
    private final SystemState state;
    private volatile boolean running = true;
    private long maxWaitTime = 5000; //Wait time to consume before expired

    // Different test types require different processing times
    private static final int BLOOD_PROCESSING_TIME = 500;        // 500ms
    private static final int PCR_PROCESSING_TIME = 1000;         // 1 second
    private static final int HISTOPATHOLOGY_PROCESSING_TIME = 2000; // 2 seconds

    public Analyzer(String name, BoundedQueue queue, SystemState state) {
        this.name = name;
        this.queue = queue;
        this.state = state;
    }

    @Override
    public void run() {
        while (true) {

            // Exit condition
            if (!running || Thread.currentThread().isInterrupted()) {
                System.out.println("[" + name + "] Shutting down gracefully");
                break;
            }

            TestOrder order = null;
            try {
                // Wait for a free analyzer slot
                state.acquireAnalyzerSlot();
                try {
                    order = queue.consume(maxWaitTime);
                    if (order == null) {
                        state.incrementExpired();
                        continue; // skip processing
                    }
                    state.incrementProcessed();
                    System.out.println("[" + name + "] Processing " + order.getType() + " order #" + order.getId() + " from " + order.getSource());
                    // Process based on test type
                    Thread.sleep(getProcessingTime(order.getType()));

                } finally {
                    state.releaseAnalyzerSlot();
                }

            } catch (InterruptedException e) {
                System.out.println("[" + name + "] Interrupted - Draining Queue and Shutting down gracefully");
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Process remaining orders in a shutdown
        while (queue.getSize() > 0) {
            TestOrder order = null;
            try {
                state.acquireAnalyzerSlot();
                order = queue.consume(maxWaitTime);
                try {
                    if (order == null) {
                        state.incrementExpired();
                        continue;
                    }
                    state.incrementProcessed();
                    System.out.println("[SHUTDOWN] [" + name + "] Processing order #" + order.getId());

                    Thread.sleep(getProcessingTime(order.getType()));

                } finally {
                    state.releaseAnalyzerSlot();
                }
            } catch (InterruptedException e) {
                System.out.println("[" + name + "] Interrupted - Shutting down gracefully");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private int getProcessingTime(TestOrder.OrderType testType) {
        switch (testType) {
            case BLOOD:
                return BLOOD_PROCESSING_TIME;
            case PCR:
                return PCR_PROCESSING_TIME;
            case HISTOPATHOLOGY:
                return HISTOPATHOLOGY_PROCESSING_TIME;
            default:
                return 1000; // Default
        }
    }

    public void shutdown() {
        running = false;
    }
}

