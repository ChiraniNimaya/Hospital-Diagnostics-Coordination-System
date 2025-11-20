public class AlternativeImplementationsComparison {
    public static void main(String[] args) throws InterruptedException {
        int runDuration = 10000; // 10 seconds
        int queueCapacity = 10;

        // Test: BlockingQueue
        System.out.println("Testing BlockingQueue Implementation...");
        testBlockingQueue(runDuration, queueCapacity);
    }

    private static void testBlockingQueue(int duration, int capacity) throws InterruptedException {
        BlockingQueueImplementation queue = new BlockingQueueImplementation(capacity);
        PerformanceMetrics metrics = new PerformanceMetrics();

        // Create producers and consumers
        Thread[] producers = new Thread[3];
        Thread[] consumers = new Thread[2];

        metrics.start();

        // Start producers
        for (int i = 0; i < producers.length; i++) {
            final int id = i;
            producers[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        long start = System.currentTimeMillis();
                        queue.produce(new TestOrder("BLOOD", "Clinic-" + id));
                        long latency = System.currentTimeMillis() - start;
                        metrics.recordOperation(latency);
                        metrics.recordQueueSize(queue.getSize());
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            producers[i].start();
        }

        // Start consumers
        for (int i = 0; i < consumers.length; i++) {
            consumers[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        queue.consume();
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumers[i].start();
        }

        Thread.sleep(duration);

        // Stop all threads
        for (Thread t : producers) t.interrupt();
        for (Thread t : consumers) t.interrupt();
        for (Thread t : producers) t.join();
        for (Thread t : consumers) t.join();

        metrics.end();
        metrics.printReport("BlockingQueue");
    }
}
