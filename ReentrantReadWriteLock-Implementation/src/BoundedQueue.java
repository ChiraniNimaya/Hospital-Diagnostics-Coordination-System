import java.util.ArrayList;
import java.util.List;

//Monitor-based Producer-Consumer
class BoundedQueue {
    private final List<TestOrder> orderBuffer;
    private final int capacity;
    private int count = 0;
    private int totalAdmitted = 0;
    private int totalProcessed = 0;
    private long totalProduceWaitTime = 0;
    private long totalConsumeWaitTime = 0;
    private final SystemState state;

    public BoundedQueue(int capacity, SystemState state) {
        this.capacity = capacity;
        this.orderBuffer = new ArrayList<>(capacity);
        this.state = state;
    }

    public synchronized boolean produce(TestOrder order, SystemState.ProcessingPolicy policy) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        if (order.getType() == TestOrder.OrderType.OTHER) {
            return false;
        }

        // Wait while queue is full
        while (count == capacity || state.isMaintenanceMode()) {
            wait();
        }

        totalProduceWaitTime += (System.currentTimeMillis() - startWait);

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
        totalAdmitted++;

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

    public synchronized TestOrder consume(long maxWaitTime) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        // Wait while queue is empty
        while (count == 0  || state.isMaintenanceMode()) {
            wait(1000); // Timeout to check interruption periodically
        }

        // Remove from buffer (Use FIFO since the items are inserted to queue according to priority)
        TestOrder order = orderBuffer.remove(0);
        count--;

        // Check expiration
        long waitTime = System.currentTimeMillis() - order.getSubmissionTime();
        if (waitTime > maxWaitTime) {
            System.out.println("Order #" + order.getId() + " is Expired before analyze(Timeout).");
            notifyAll();
            return null; // Analyzer will skip expired orders
        }

        totalConsumeWaitTime += (System.currentTimeMillis() - startWait);
        totalProcessed++;

        // Notify waiting producers
        notifyAll();
        return order;
    }

    public synchronized int getSize() {
        return count;
    }

    public synchronized int getQueueCapacity() { return capacity;}

    public synchronized double getAverageProduceWaitTime() {
        return totalAdmitted > 0 ? (double) totalProduceWaitTime / totalAdmitted : 0;
    }

    public synchronized double getAverageConsumeWaitTime() {
        return totalProcessed > 0 ? (double) totalConsumeWaitTime / totalProcessed : 0;
    }
}
