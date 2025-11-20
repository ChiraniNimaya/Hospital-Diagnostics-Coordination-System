public class PerformanceMetrics {
    private long startTime;
    private long endTime;
    private int totalOperations;
    private long totalLatency;
    private int maxQueueSize;

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void end() {
        endTime = System.currentTimeMillis();
    }

    public void recordOperation(long latency) {
        totalOperations++;
        totalLatency += latency;
    }

    public void recordQueueSize(int size) {
        if (size > maxQueueSize) {
            maxQueueSize = size;
        }
    }

    public double getThroughput() {
        long duration = endTime - startTime;
        return duration > 0 ? (totalOperations * 1000.0) / duration : 0;
    }

    public double getAverageLatency() {
        return totalOperations > 0 ? (double) totalLatency / totalOperations : 0;
    }

    public void printReport(String implementation) {
        System.out.println("\n=== " + implementation + " Performance ===");
        System.out.println("Duration: " + (endTime - startTime) + "ms");
        System.out.println("Total Operations: " + totalOperations);
        System.out.println("Throughput: " + String.format("%.2f", getThroughput()) + " ops/sec");
        System.out.println("Average Latency: " + String.format("%.2f", getAverageLatency()) + "ms");
        System.out.println("Max Queue Size: " + maxQueueSize);
    }
}
