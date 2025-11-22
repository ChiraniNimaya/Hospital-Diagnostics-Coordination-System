import java.util.ArrayList;
import java.util.List;

//Monitor-based Producer-Consumer
class BoundedQueue {
    private final List<TestOrder> orderBuffer;
    private final int capacity;
    private int count = 0;

    // Metrics
    private int totalSubmitted = 0;
    private int totalRejected = 0;
    private long totalWaitTime = 0;

    // INVARIANT: 0 <= count <= capacity
    // INVARIANT: orderBuffer.size() == count
    // INVARIANT: All elements in orderBuffer are non-null

    public BoundedQueue(int capacity) {
        this.capacity = capacity;
        this.orderBuffer = new ArrayList<>(capacity);
    }

    public synchronized boolean produce(TestOrder order, long timeoutMs) throws InterruptedException {
        long startWait = System.currentTimeMillis();
        long deadline = startWait + timeoutMs;

        // Wait while queue is full
        while (count == capacity) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                totalRejected++;
                return false;
            }
            wait(remaining);
        }

        totalWaitTime += (System.currentTimeMillis() - startWait);

        // Add to buffer
        orderBuffer.add(order);
        count++;
        totalSubmitted++;

        // Notify waiting consumers
        notifyAll();
        return true;
    }

    public synchronized TestOrder consume(long maxWaitTime) throws InterruptedException {
        // Wait while queue is empty
        while (count == 0) {
            wait();
        }

        // Remove from buffer (FIFO: remove first element)
        TestOrder order = orderBuffer.remove(0);
        count--;

        // Check expiration
        long waitTime = System.currentTimeMillis() - order.getSubmissionTime();
        if (waitTime > maxWaitTime) {
            notifyAll();
            return null; // Analyzer will skip expired orders
        }

        // Notify waiting producers
        notifyAll();

        return order;
    }

    public synchronized int getSize() {
        return count;
    }

    public synchronized int getTotalAdmitted() {
        return totalSubmitted;
    }

    public synchronized int getTotalRejected() {
        return totalRejected;
    }

    public synchronized double getAverageWaitTime() {
        return totalSubmitted > 0 ? (double) totalWaitTime / totalSubmitted : 0;
    }
}
