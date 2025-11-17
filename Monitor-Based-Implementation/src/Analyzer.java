//Consumer thread
public class Analyzer implements Runnable {
    private final int id;
    private final BoundedQueue queue;
    private final SystemState state;
    private final int processingTime; // ms per order
    private volatile boolean running = true;

    public Analyzer(int id, BoundedQueue queue, SystemState state, int processingTime) {
        this.id = id;
        this.queue = queue;
        this.state = state;
        this.processingTime = processingTime;
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                TestOrder order = queue.consume();

                // Process the order
                Thread.sleep(processingTime);
                state.incrementProcessed();

                System.out.println("[ANALYZER-" + id + "] Processed order " +
                        order.getId() + " from " + order.getSource());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[ANALYZER-" + id + "] Shutting down gracefully");
        }
    }

    public void shutdown() {
        running = false;
    }
}
