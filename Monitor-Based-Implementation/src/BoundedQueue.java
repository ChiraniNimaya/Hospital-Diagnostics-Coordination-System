//Monitor-based Producer-Consumer
public class BoundedQueue {
    private final TestOrder[] buffer;
    private final int capacity;
    private int count = 0;
    private int in = 0;
    private int out = 0;

    // Metrics
    private int totalAdmitted = 0;
    private int totalRejected = 0;
    private long totalWaitTime = 0;

    // INVARIANT: 0 <= count <= capacity
    // INVARIANT: (in - out) mod capacity == count mod capacity

    public BoundedQueue(int capacity) {
        this.capacity = capacity;
        this.buffer = new TestOrder[capacity];
    }

    public synchronized void produce(TestOrder order) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        // Wait while queue is full
        while (count == capacity) {
            wait(); // Thread enters WAITING state
        }

        totalWaitTime += (System.currentTimeMillis() - startWait);

        // Add to buffer
        buffer[in] = order;
        in = (in + 1) % capacity;
        count++;
        totalAdmitted++;

        // Notify waiting consumers
        notifyAll(); // Could use notify() but notifyAll() is safer
    }

    public synchronized TestOrder consume() throws InterruptedException {
        // Wait while queue is empty
        while (count == 0) {
            wait(); // Thread enters WAITING state
        }

        // Remove from buffer
        TestOrder order = buffer[out];
        buffer[out] = null;
        out = (out + 1) % capacity;
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
