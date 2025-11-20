import java.util.ArrayList;
import java.util.List;

//Monitor-based Producer-Consumer
class BoundedQueue {
    private final List<TestOrder> orderBuffer;
    private final int capacity;
    private int count = 0;

    // Metrics
    private int totalAdmitted = 0;
    private int totalRejected = 0;
    private long totalWaitTime = 0;

    // INVARIANT: 0 <= count <= capacity
    // INVARIANT: orderBuffer.size() == count
    // INVARIANT: All elements in orderBuffer are non-null

    public BoundedQueue(int capacity) {
        this.capacity = capacity;
        this.orderBuffer = new ArrayList<>(capacity);
    }

    public synchronized void produce(TestOrder order) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        // Wait while queue is full
        while (count == capacity) {
            wait();
        }

        totalWaitTime += (System.currentTimeMillis() - startWait);

        // Add to buffer
        orderBuffer.add(order);
        count++;
        totalAdmitted++;

        // Notify waiting consumers
        notifyAll();
    }

    public synchronized TestOrder consume() throws InterruptedException {
        // Wait while queue is empty
        while (count == 0) {
            wait();
        }

        // Remove from buffer (FIFO: remove first element)
        TestOrder order = orderBuffer.remove(0);
        count--;

        // Notify waiting producers
        notifyAll();

        return order;
    }

    public synchronized int getSize() {
        return count;
    }

    public synchronized int getTotalAdmitted() {
        return totalAdmitted;
    }

    public synchronized int getTotalRejected() {
        return totalRejected;
    }

    public synchronized double getAverageWaitTime() {
        return totalAdmitted > 0 ? (double) totalWaitTime / totalAdmitted : 0;
    }
}
