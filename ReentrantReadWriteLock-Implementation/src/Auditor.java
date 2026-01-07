//Reader thread using readLock()
class Auditor implements Runnable {
    private final String name;
    private final BoundedQueue queue;
    private final SystemState state;
    private final int reportInterval;
    private volatile boolean running = true;

    private SystemSnapshot previousSnapshot = null;
    private int inconsistencyCount = 0;
    private int totalReports = 0;

    public Auditor(String name, BoundedQueue queue, SystemState state, int reportInterval) {
        this.name = name;
        this.queue = queue;
        this.state = state;
        this.reportInterval = reportInterval;
    }

    @Override
    public void run() {
        while (true) {
            if (!running || Thread.currentThread().isInterrupted()) {
                System.out.println("[" + name + "] Shutting down gracefully");
                break;
            }

            try {
                // READ operation - uses readLock() internally
                SystemSnapshot snapshot = state.getSnapshot();
                totalReports++;

                boolean consistencyOk = checkConsistency(snapshot);

                String maintenanceStatus = snapshot.maintenanceMode ? " MAINTENANCE" : " OPERATING";
                String capacityStatus = snapshot.activeAnalyzerSlots + "/" + snapshot.maxAnalyzerCapacity;
                String consistencyStatus = consistencyOk ? " CONSISTENT " : " INCONSISTENT ";

                System.out.println("[" + name + "] Report #" + totalReports +
                        " at " + snapshot.timestamp + ": " +
                        " Queue Occupation=" + queue.getSize() + "/" + queue.getQueueCapacity() +
                        ", Submitted=" + snapshot.totalSubmitted +
                        ", Processed=" + snapshot.totalProcessed +
                        ", Policy=" + snapshot.currentPolicy +
                        ", Analyzers=" + capacityStatus +
                        ", Status=" + maintenanceStatus +
                        ", Consistency=" + consistencyStatus);

                previousSnapshot = snapshot;

                Thread.sleep(reportInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Final report
        printFinalReport();
    }

    private void printFinalReport() {
        try {
            SystemSnapshot finalSnapshot = state.getSnapshot();
            System.out.println("""
        
        ====================================================
        [%s] FINAL REPORT
        ====================================================
        Timestamp: %d
        
        Queue Metrics:
          Queue Size             : %d / %d
          Orders Submitted       : %d
          Orders Processed       : %d
          Orders Rejected        : %d
          Orders Expired         : %d
        
        Performance Metrics:
          Average Producer Wait Time  : %.3f ms
          Average Consumer Wait Time  : %.3f ms
          Average Reader Blocking    : %.3f ms
          Average Writer Blocking    : %.3f ms
        
        Consistency Check:
          Inconsistencies        : %d / %d (%.2f%%)
        
        %s
        ====================================================
        """.formatted(
                    name,
                    finalSnapshot.timestamp,
                    queue.getSize(),
                    queue.getQueueCapacity(),
                    finalSnapshot.totalSubmitted,
                    finalSnapshot.totalProcessed,
                    finalSnapshot.totalRejected,
                    finalSnapshot.totalExpired,
                    queue.getAverageProduceWaitTime(),
                    queue.getAverageConsumeWaitTime(),
                    state.getAverageReaderBlockingTime(),
                    state.getAverageWriterBlockingTime(),
                    inconsistencyCount,
                    totalReports,
                    (inconsistencyCount * 100.0 / totalReports),
                    state.getLockStatistics()
            ));
        } catch (Exception e) {
            System.out.println("[" + name + "] Final snapshot interrupted, skipping final report.");
        }
    }

    private boolean checkConsistency(SystemSnapshot current) {
        if (previousSnapshot == null) {
            return true;
        }

        boolean isConsistent = true;

        // Counters should never decrease
        if (current.totalSubmitted < previousSnapshot.totalSubmitted) {
            System.out.println("[" + name + "] INCONSISTENCY: " +
                    "Total Submitted decreased: " +
                    previousSnapshot.totalSubmitted + " → " + current.totalSubmitted);
            isConsistent = false;
            inconsistencyCount++;
        }

        if (current.totalProcessed < previousSnapshot.totalProcessed) {
            System.out.println("[" + name + "] INCONSISTENCY: " +
                    "Total Processed decreased: " +
                    previousSnapshot.totalProcessed + " → " + current.totalProcessed);
            isConsistent = false;
            inconsistencyCount++;
        }

        // Processed cannot exceed submitted
        if (current.totalProcessed > current.totalSubmitted) {
            System.out.println("[" + name + "] INCONSISTENCY: " +
                    "Processed (" + current.totalProcessed +
                    ") exceeds Submitted (" + current.totalSubmitted + ")");
            isConsistent = false;
            inconsistencyCount++;
        }

        // Active analyzers within capacity
        if (current.activeAnalyzerSlots > current.maxAnalyzerCapacity) {
            System.out.println("[" + name + "] INCONSISTENCY: " +
                    "Active analyzers (" + current.activeAnalyzerSlots +
                    ") exceeds capacity (" + current.maxAnalyzerCapacity + ")");
            isConsistent = false;
            inconsistencyCount++;
        }

        if (current.activeAnalyzerSlots < 0) {
            System.out.println("[" + name + "] INCONSISTENCY: " +
                    "Active analyzers is negative: " + current.activeAnalyzerSlots);
            isConsistent = false;
            inconsistencyCount++;
        }

        // Progress check
        long timeDiff = current.timestamp - previousSnapshot.timestamp;
        int submittedDiff = current.totalSubmitted - previousSnapshot.totalSubmitted;
        int processedDiff = current.totalProcessed - previousSnapshot.totalProcessed;

        if (timeDiff > 10000 && submittedDiff == 0 && processedDiff == 0
                && !current.maintenanceMode && current.activeAnalyzerSlots > 0) {
            System.out.println("[" + name + "] WARNING: " +
                    "No progress in " + timeDiff + "ms (possible deadlock/starvation)");
        }

        return isConsistent;
    }

    public void shutdown() {
        running = false;
    }
}