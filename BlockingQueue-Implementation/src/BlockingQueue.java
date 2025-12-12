import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BlockingQueue {
    private int capacity = 1;
    java.util.concurrent.BlockingQueue<TestOrder> orderBuffer = new ArrayBlockingQueue<>(capacity);

    private final AtomicInteger totalAdmitted = new AtomicInteger(0);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicLong totalProduceWaitTime = new AtomicLong(0);
    private final AtomicLong totalConsumeWaitTime = new AtomicLong(0);

    private SystemState state;

    public BlockingQueue(int capacity, SystemState state) {
        this.capacity = capacity;
        this.orderBuffer = new ArrayBlockingQueue<>(capacity);
        this.state = state;
    }

    public boolean produce(TestOrder order, SystemState.ProcessingPolicy policy) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        if (order.getType() == TestOrder.OrderType.OTHER) {
            return false;
        }

        // Wait while queue is full or in maintenance
        while (state.isMaintenanceMode()) {
            Thread.sleep(100); // Small wait to check maintenance mode periodically
        }

        orderBuffer.put(order); // Blocks if queue is full

        totalAdmitted.incrementAndGet();
        totalProduceWaitTime.addAndGet(System.currentTimeMillis() - startWait);

        return true;
    }

    public TestOrder consume(long maxWaitTime) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        TestOrder order;
        while (true) {
            if (state.isMaintenanceMode()) {
                Thread.sleep(100);
                continue;
            }

            order = orderBuffer.poll(1, TimeUnit.SECONDS); // Timeout to check interruption
            if (order != null) break;
        }

        long waitTime = System.currentTimeMillis() - order.getSubmissionTime();
        if (waitTime > maxWaitTime) {
            System.out.println("Order #" + order.getId() + " is Expired before analyze(Timeout).");
            return null; // Analyzer will skip expired orders
        }

        totalProcessed.incrementAndGet();
        totalConsumeWaitTime.addAndGet(System.currentTimeMillis() - startWait);

        return order;
    }

    public int getSize() {
        return orderBuffer.size();
    }

    public int getQueueCapacity() {
        return capacity;
    }

    public double getAverageProduceWaitTime() {
        int count = totalAdmitted.get();
        return count > 0 ? (double) totalProduceWaitTime.get() / count : 0;
    }

    public double getAverageConsumeWaitTime() {
        int count = totalProcessed.get();
        return count > 0 ? (double) totalConsumeWaitTime.get() / count : 0;
    }
}
