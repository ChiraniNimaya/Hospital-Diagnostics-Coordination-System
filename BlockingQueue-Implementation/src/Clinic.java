//Producer thread which generates test orders at configurable rates
public class Clinic implements Runnable {
    private final String name;
    private final BlockingQueue queue;
    private final SystemState state;
    private volatile boolean running = true;
    private final int producerInterval;

    // Test type probabilities: BLOOD (50%), PCR (30%), HISTOPATHOLOGY (15%)
    private static final double BLOOD_PROBABILITY = 0.5;
    private static final double PCR_PROBABILITY = 0.3;
    private static final double HISTOPATHOLOGY_PROBABILITY = 0.15;

    public Clinic(String name, BlockingQueue queue, SystemState state, int producerInterval) {
        this.name = name;
        this.queue = queue;
        this.state = state;
        this.producerInterval = producerInterval;
    }

    @Override
    public void run() {
        int orderCount = 0;
        while (true) {
            if (!running || Thread.currentThread().isInterrupted()) {
                System.out.println("[" + name + "] Shutting down gracefully");
                break;
            }

            // Generate test order with variable type
            TestOrder.OrderType testType = selectTestType();
            TestOrder order = new TestOrder(testType, name);
            try {
                boolean admitted = queue.produce(order, state.getCurrentPolicy()); // wait max 2 seconds
                if (!admitted) {
                    state.incrementRejected();
                    System.out.println("[" + name + "] Rejected order #" + order.getId() + "(Invalid Order Name)");
                } else {
                    state.incrementSubmitted();
                    System.out.println("[" + name + "] Produced " + testType + " order #" + order.getId());
                    orderCount++;
                }

                Thread.sleep(producerInterval);
            } catch(InterruptedException e){
                System.out.println("[" + name + "] Interrupted - Shutting down gracefully");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[" + name + "] Completed Producing " + orderCount + " orders");
    }

    // Select test type based on probability distribution using enum values directly
    private TestOrder.OrderType selectTestType() {
        double random = Math.random();

        if (random < BLOOD_PROBABILITY) {
            return TestOrder.OrderType.BLOOD;
        } else if (random < BLOOD_PROBABILITY + PCR_PROBABILITY) {
            return TestOrder.OrderType.PCR;
        } else if (random <= BLOOD_PROBABILITY + PCR_PROBABILITY + HISTOPATHOLOGY_PROBABILITY) {
            return TestOrder.OrderType.HISTOPATHOLOGY;
        } else {
            return TestOrder.OrderType.OTHER;
        }
    }

    public void shutdown() {
        running = false;
    }
}
