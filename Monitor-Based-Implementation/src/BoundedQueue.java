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

    public synchronized boolean produce(TestOrder order, long timeoutMs, SystemState.ProcessingPolicy policy) throws InterruptedException {
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


        switch (policy) {
            case PRIORITY:
                insertByPriority(order);  // Insert in priority sorted order
                break;
            case EMERGENCY_FIRST:
                if (order.getPriority() == TestOrder.OrderPriority.EMERGENCY) {
                    // Find position after last emergency order
                    int insertPos = 0;
                    for (int i = 0; i < orderBuffer.size(); i++) {
                        if (orderBuffer.get(i).getPriority() != TestOrder.OrderPriority.EMERGENCY) {
                            break;  // Found first non-emergency
                        }
                        insertPos = i + 1;
                    }
                    orderBuffer.add(insertPos, order);
                } else {
                    orderBuffer.add(order);      // Add to back
                }
                break;
            case FIFO:
            default:
                orderBuffer.add(order);  // Simple add to end
                break;
        }

        count++;
        totalSubmitted++;

        // Notify waiting consumers
        notifyAll();
        return true;
    }

    // Priority insertion
    private void insertByPriority(TestOrder order) {
        int insertIndex = 0;
        // Find correct position based on priority and submission time
        for (int i = 0; i < orderBuffer.size(); i++) {
            // If the order is the priority than i th order in buffer
            if (order.comparePriority(orderBuffer.get(i)) < 0) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }
        orderBuffer.add(insertIndex, order);
    }

    public synchronized TestOrder consume(long maxWaitTime, boolean mode) throws InterruptedException {
        // Wait while queue is empty
        while (count == 0) {
            wait();
        }

        // Remove from buffer (FIFO: remove first element)
        TestOrder order = orderBuffer.remove(0);
        count--;

        // Check maintenance mode
        if (mode) {
            notifyAll();
            return null; // treat as expired due to maintenance mode
        }

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
