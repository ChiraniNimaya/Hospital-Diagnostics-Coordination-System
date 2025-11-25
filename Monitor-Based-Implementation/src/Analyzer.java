//Consumer thread
public class Analyzer implements Runnable {
    private final int id;
    private final BoundedQueue queue;
    private final SystemState state;
    private volatile boolean running = true;
    private long maxWaitTime = 5000; //Wait time to consume before expired

    // Different test types require different processing times
    private static final int BLOOD_PROCESSING_TIME = 500;        // 500ms
    private static final int PCR_PROCESSING_TIME = 1000;         // 1 second
    private static final int HISTOPATHOLOGY_PROCESSING_TIME = 2000; // 2 seconds

    private int bloodProcessed = 0;
    private int pcrProcessed = 0;
    private int histoProcessed = 0;

    public Analyzer(int id, BoundedQueue queue, SystemState state) {
        this.id = id;
        this.queue = queue;
        this.state = state;
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                // Wait for a free analyzer slot
                state.acquireAnalyzerSlot();

                TestOrder order = queue.consume(maxWaitTime, state.isMaintenanceMode());
                if (order == null) {
                    state.incrementExpired();
                    System.out.println("[ANALYZER-" + id + "] An order is Expired.");
                    continue; // skip processing
                }

                // Process based on test type
                int processingTime = getProcessingTime(order.getType());
                Thread.sleep(processingTime);

                // Update counts
                updateStats(order.getType());
                state.incrementProcessed();

                System.out.println("[ANALYZER-" + id + "] Processed " +
                        order.getType() + " order #" + order.getId() +
                        " from " + order.getSource() +
                        " (took " + processingTime + "ms)");

                // Release slot once processing completes
                state.releaseAnalyzerSlot();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ANALYZER-" + id + "] Shutting down gracefully");
        } finally {
            // Print final statistics for this analyzer
            System.out.println("[ANALYZER-" + id + "] Final stats: " +
                    "BLOOD=" + bloodProcessed +
                    ", PCR=" + pcrProcessed +
                    ", HISTO=" + histoProcessed);
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

    private void updateStats(TestOrder.OrderType testType) {
        switch (testType) {
            case BLOOD:
                bloodProcessed++;
                break;
            case PCR:
                pcrProcessed++;
                break;
            case HISTOPATHOLOGY:
                histoProcessed++;
                break;
        }
    }

    public void shutdown() {
        running = false;
    }
}

