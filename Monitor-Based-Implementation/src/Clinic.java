//Producer thread which generates test orders at configurable rates
public class Clinic implements Runnable {
    private final String name;
    private final BoundedQueue queue;
    private final SystemState state;
    private LoadPattern loadPattern;
    private volatile boolean running = true;

    // Test type probabilities: BLOOD (60%), PCR (30%), HISTOPATHOLOGY (10%)
    private static final double BLOOD_PROBABILITY = 0.6;
    private static final double PCR_PROBABILITY = 0.3;
    private static final double HISTOPATHOLOGY_PROBABILITY = 0.1;

    public Clinic(String name, BoundedQueue queue, SystemState state, LoadPattern pattern) {
        this.name = name;
        this.queue = queue;
        this.state = state;
        this.loadPattern = pattern;
    }

    @Override
    public void run() {
        int orderCount = 0;
        try {
            long startTime = System.currentTimeMillis();

            while (running && !Thread.currentThread().isInterrupted()) {
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Generate test order with variable type
                TestOrder.Type testType = selectTestType();
                TestOrder order = new TestOrder(testType, name);
                boolean admitted = queue.produce(order, 2000); // wait max 2 seconds                System.out.println("[" + name + "] Produced " + testType + " order #" + order.getId());
                if (!admitted) {
                    state.incrementRejected();
                }
                else {
                    state.incrementSubmitted();
                    orderCount++;
                }
                // Variable rate based on load pattern
                int sleepTime = loadPattern.getInterArrivalTime(elapsedTime);
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + name + "] Shutting down gracefully");
        } finally {
            System.out.println("[" + name + "] Completed " + orderCount + " orders");
        }
    }

    // Select test type based on probability distribution using enum values directly
    private TestOrder.Type selectTestType() {
        double random = Math.random();

        if (random < BLOOD_PROBABILITY) {
            return TestOrder.Type.BLOOD;
        } else if (random < BLOOD_PROBABILITY + PCR_PROBABILITY) {
            return TestOrder.Type.PCR;
        } else {
            return TestOrder.Type.HISTOPATHOLOGY;
        }
    }

    public void shutdown() {
        running = false;
    }
}
