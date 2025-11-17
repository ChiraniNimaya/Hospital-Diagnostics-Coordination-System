//Producer thread
public class Clinic implements Runnable {
    private final String name;
    private final BoundedQueue queue;
    private final SystemState state;
    private final int orderRate; // orders per second
    private volatile boolean running = true;

    public Clinic(String name, BoundedQueue queue, SystemState state, int orderRate) {
        this.name = name;
        this.queue = queue;
        this.state = state;
        this.orderRate = orderRate;
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                TestOrder order = new TestOrder("BLOOD", name);
                queue.produce(order);
                state.incrementSubmitted();

                // Simulate variable rate
                Thread.sleep(1000 / orderRate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + name + "] Shutting down gracefully");
        }
    }

    public void shutdown() {
        running = false;
    }
}
