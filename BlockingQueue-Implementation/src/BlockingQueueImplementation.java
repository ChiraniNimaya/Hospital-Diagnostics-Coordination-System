import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class BlockingQueueImplementation {
    private final BlockingQueue<TestOrder> queue;
    private int totalAdmitted = 0;
    private int totalRejected = 0;

    public BlockingQueueImplementation(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public void produce(TestOrder order) throws InterruptedException {
        queue.put(order); // Automatically blocks when full
        synchronized(this) {
            totalAdmitted++;
        }
    }

    public TestOrder consume() throws InterruptedException {
        return queue.take(); // Automatically blocks when empty
    }

    public int getSize() {
        return queue.size();
    }

    public synchronized int getTotalAdmitted() {
        return totalAdmitted;
    }
}
