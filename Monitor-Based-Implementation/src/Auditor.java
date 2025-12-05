//Reader thread
class Auditor implements Runnable {
    private final int id;
    private final SystemState state;
    private final int reportInterval; // ms
    private volatile boolean running = true;

    // Track previous snapshot for consistency checks
    private SystemSnapshot previousSnapshot = null;
    private int inconsistencyCount = 0;
    private int totalReports = 0;

    public Auditor(int id, SystemState state, int reportInterval) {
        this.id = id;
        this.state = state;
        this.reportInterval = reportInterval;
    }

    @Override
    public void run() {
        while (true) {
            if (!running || Thread.currentThread().isInterrupted()) {
                System.out.println("[AUDITOR-" + id + "] Shutting down gracefully");
                break;
            }
            try {
                SystemSnapshot snapshot = state.getSnapshot();
                totalReports++;

                boolean consistencyOk = checkConsistency(snapshot);

                // Display functional state information
                String maintenanceStatus = snapshot.maintenanceMode ? " MAINTENANCE" : " Normal";
                String capacityStatus = snapshot.activeAnalyzerSlots + "/" + snapshot.maxAnalyzerCapacity;
                String consistencyStatus = consistencyOk ? " CONSISTENT " : " INCONSISTENT ";

                System.out.println("[AUDITOR-" + id + "] Report #" + totalReports +
                        " at " + snapshot.timestamp + ": " +
                        "Submitted=" + snapshot.totalSubmitted +
                        ", Processed=" + snapshot.totalProcessed +
                        ", Policy=" + snapshot.currentPolicy +
                        ", Analyzers=" + capacityStatus +
                        ", Status=" + maintenanceStatus +
                        ", Consistency=" + consistencyStatus);

                // Update previous snapshot for next check
                previousSnapshot = snapshot;

                Thread.sleep(reportInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Print final consistency report
                SystemSnapshot finalSnapshot = null;
                try {
                    finalSnapshot = state.getSnapshot();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        // Final report
        try {
            SystemSnapshot finalSnapshot = state.getSnapshot();
            System.out.println("[AUDITOR-" + id + "] Final Report " +
                    " at " + finalSnapshot.timestamp + ": " +
                    "Submitted=" + finalSnapshot.totalSubmitted +
                    ", Processed=" + finalSnapshot.totalProcessed +
                    ", Inconsistencies=" + inconsistencyCount +
                    " (" + String.format("%.2f", (inconsistencyCount * 100.0 / totalReports)) + "%)");
        } catch (InterruptedException e) {
            System.out.println("[AUDITOR-" + id + "] Final snapshot interrupted, Skipping final report.");
        }
    }

    // CONSISTENCY CHECKING LOGIC
    private boolean checkConsistency(SystemSnapshot current) {
        if (previousSnapshot == null) {
            return true; // First snapshot, nothing to compare
        }

        boolean isConsistent = true;

        // Counters should never decrease
        if (current.totalSubmitted < previousSnapshot.totalSubmitted) {
            System.out.println("[AUDITOR-" + id + "]  INCONSISTENCY: " +
                    "Total Submitted Order count has decreased: " +
                    previousSnapshot.totalSubmitted + " → " +
                    current.totalSubmitted);
            isConsistent = false;
            inconsistencyCount++;
        }
        if (current.totalProcessed < previousSnapshot.totalProcessed) {
            System.out.println("[AUDITOR-" + id + "]  INCONSISTENCY: " +
                    "Total Processed Order count has decreased: " +
                    previousSnapshot.totalProcessed + " → " +
                    current.totalProcessed);
            isConsistent = false;
            inconsistencyCount++;
        }

        // Processed order count cannot exceed Submitted order count
        if (current.totalProcessed > current.totalSubmitted) {
            System.out.println("[AUDITOR-" + id + "]  INCONSISTENCY: " +
                    "Processed (" + current.totalProcessed +
                    ") exceeds Submitted (" + current.totalSubmitted + ")");
            isConsistent = false;
            inconsistencyCount++;
        }

        // Active analyzers count should be within capacity and cannot be negative
        if (current.activeAnalyzerSlots > current.maxAnalyzerCapacity) {
            System.out.println("[AUDITOR-" + id + "]  INCONSISTENCY: " +
                    "Active analyzers (" + current.activeAnalyzerSlots +
                    ") exceeds capacity (" + current.maxAnalyzerCapacity + ")");
            isConsistent = false;
            inconsistencyCount++;
        }
        if (current.activeAnalyzerSlots < 0) {
            System.out.println("[AUDITOR-" + id + "]  INCONSISTENCY: " +
                    "Active analyzers is negative: " + current.activeAnalyzerSlots);
            isConsistent = false;
            inconsistencyCount++;
        }

        // System should make progress
        long timeDiff = current.timestamp - previousSnapshot.timestamp;
        int submittedDiff = current.totalSubmitted - previousSnapshot.totalSubmitted;
        int processedDiff = current.totalProcessed - previousSnapshot.totalProcessed;

        // If time passed but no progress and system not in maintenance
        if (timeDiff > 10000 && submittedDiff == 0 && processedDiff == 0
                && !current.maintenanceMode && current.activeAnalyzerSlots > 0) {
            System.out.println("[AUDITOR-" + id + "]  WARNING: " +
                    "No progress in " + timeDiff + "ms " +
                    "(possible deadlock or starvation)");
        }

        return isConsistent;
    }

    public void shutdown() {
        running = false;
    }

}