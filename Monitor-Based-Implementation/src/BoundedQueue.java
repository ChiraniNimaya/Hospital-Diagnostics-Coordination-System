import java.util.ArrayList;
import java.util.List;

//Monitor-based Producer-Consumer
class BoundedQueue {
    private final List<TestOrder> orderBuffer;
    private final int capacity;
    private int count = 0;
    private final SystemState state;

    public BoundedQueue(int capacity, SystemState state) {
        this.capacity = capacity;
        this.orderBuffer = new ArrayList<>(capacity);
        this.state = state;
    }

    public synchronized boolean produce(TestOrder order, SystemState.ProcessingPolicy policy) throws InterruptedException {

        if (order.getType() == TestOrder.OrderType.OTHER) {
            return false;
        }

        // Wait while queue is full
        while (count == capacity) {
            wait();
        }

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
        // Wait while queue is empty
        while (count == 0) {
            wait();
        }

        // Remove from buffer (Use FIFO since the items are inserted to queue according to priority)
        TestOrder order = orderBuffer.remove(0);
        count--;

        // Check maintenance mode
        if (state.isMaintenanceMode()) {
            System.out.println("Order #" + order.getId() + " is Expired before analyze(Maintenance Mode).");
            notifyAll();
            return null; // treat as expired due to maintenance mode
        }

        // Check expiration
        long waitTime = System.currentTimeMillis() - order.getSubmissionTime();
        if (waitTime > maxWaitTime) {
            System.out.println("Order #" + order.getId() + " is Expired before analyze(Timeout).");
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
}
