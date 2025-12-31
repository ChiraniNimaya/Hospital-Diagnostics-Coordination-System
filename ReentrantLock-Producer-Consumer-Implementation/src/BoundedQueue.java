import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor-based Producer-Consumer using ReentrantLock and Condition variables
 */
class BoundedQueue {
    private final List<TestOrder> orderBuffer;
    private final int capacity;
    private int count = 0;
    private int totalAdmitted = 0;
    private int totalProcessed = 0;
    private long totalProduceWaitTime = 0;
    private long totalConsumeWaitTime = 0;
    private final SystemState state;

    // ReentrantLock and Condition variables
    private final ReentrantLock lock = new ReentrantLock(true); // Fair lock
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public BoundedQueue(int capacity, SystemState state) {
        this.capacity = capacity;
        this.orderBuffer = new ArrayList<>(capacity);
        this.state = state;
    }

    /**
     * Producer method using ReentrantLock
     */
    public boolean produce(TestOrder order, SystemState.ProcessingPolicy policy) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        if (order.getType() == TestOrder.OrderType.OTHER) {
            return false;
        }

        lock.lock();
        try {
            // Wait while queue is full or in maintenance mode
            while (count == capacity || state.isMaintenanceMode()) {
                notFull.await();
            }

            totalProduceWaitTime += (System.currentTimeMillis() - startWait);

            // Insert based on policy
            switch (policy) {
                case PRIORITY:
                    insertByPriority(order);
                    break;
                case EMERGENCY_FIRST:
                    if (order.getPriority() == TestOrder.OrderPriority.EMERGENCY) {
                        int insertPos = 0;
                        for (int i = 0; i < orderBuffer.size(); i++) {
                            if (orderBuffer.get(i).getPriority() != TestOrder.OrderPriority.EMERGENCY) {
                                break;
                            }
                            insertPos = i + 1;
                        }
                        orderBuffer.add(insertPos, order);
                    } else {
                        orderBuffer.add(order);
                    }
                    break;
                case FIFO:
                default:
                    orderBuffer.add(order);
                    break;
            }

            count++;
            totalAdmitted++;

            // Signal waiting consumers
            notEmpty.signal();
            return true;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Consumer method using ReentrantLock
     */
    public TestOrder consume(long maxWaitTime) throws InterruptedException {
        long startWait = System.currentTimeMillis();

        lock.lock();
        try {
            // Wait while queue is empty or in maintenance mode
            while (count == 0 || state.isMaintenanceMode()) {
                // Use timed await to check interruption periodically
                notEmpty.await(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            }

            // Remove from buffer (FIFO since items are inserted by priority)
            TestOrder order = orderBuffer.remove(0);
            count--;

            // Check expiration
            long waitTime = System.currentTimeMillis() - order.getSubmissionTime();
            if (waitTime > maxWaitTime) {
                System.out.println("Order #" + order.getId() + " is Expired before analyze(Timeout).");
                notFull.signal();
                return null;
            }

            totalConsumeWaitTime += (System.currentTimeMillis() - startWait);
            totalProcessed++;

            // Signal waiting producers
            notFull.signal();
            return order;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Priority insertion helper
     */
    private void insertByPriority(TestOrder order) {
        int insertIndex = 0;
        for (int i = 0; i < orderBuffer.size(); i++) {
            if (order.comparePriority(orderBuffer.get(i)) < 0) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }
        orderBuffer.add(insertIndex, order);
    }

    /**
     * Thread-safe getters
     */
    public int getSize() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public int getQueueCapacity() {
        lock.lock();
        try {
            return capacity;
        } finally {
            lock.unlock();
        }
    }

    public double getAverageProduceWaitTime() {
        lock.lock();
        try {
            return totalAdmitted > 0 ? (double) totalProduceWaitTime / totalAdmitted : 0;
        } finally {
            lock.unlock();
        }
    }

    public double getAverageConsumeWaitTime() {
        lock.lock();
        try {
            return totalProcessed > 0 ? (double) totalConsumeWaitTime / totalProcessed : 0;
        } finally {
            lock.unlock();
        }
    }
}