public class HospitalDiagnosticsSystem {
    // Global fairness flag
    private static boolean USE_FAIR_LOCKING = true;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Hospital Diagnostics Coordination System ===");
        System.out.println("Part B: ReentrantReadWriteLock Implementation");

        System.out.println("\nStart Time: " + System.currentTimeMillis() + "\n");

        String workloadType = args.length > 0 ? args[0] : "READER_HEAVY";

        switch (workloadType.toUpperCase()) {
            case "CALM":
                runCalmWorkload();
                break;
            case "SURGE":
                runEmergencySurgeWorkload();
                break;
            case "WRITER_HEAVY":
                runWriterHeavyWorkload();
                break;
            case "READER_HEAVY":
                runReaderHeavyWorkload();
                break;
            default:
                runBalancedWorkload();
                break;
        }

        System.out.println("\nEnd Time: " + System.currentTimeMillis() + "\n");
    }

    // Updated simulation method
    private static void runSimulation(SystemState.ProcessingPolicy newPolicy,
                                      int queueCapacity,
                                      int numClinics,
                                      int numAnalyzers,
                                      int numMaxAnalyzers,
                                      int numAuditors,
                                      int numSupervisors,
                                      int producerInterval,
                                      int reportInterval,
                                      int updateInterval,
                                      int runDuration)
            throws InterruptedException {

        // Create SystemState with fairness parameter
        SystemState state = new SystemState(USE_FAIR_LOCKING);
        BoundedQueue queue = new BoundedQueue(queueCapacity, state);

        // Create threads (same as before)
        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("CLINIC-" + i, queue, state, producerInterval);
            clinicThreads[i] = new Thread(clinics[i]);
        }

        Analyzer[] analyzers = new Analyzer[numAnalyzers];
        Thread[] analyzerThreads = new Thread[numAnalyzers];
        for (int i = 0; i < numAnalyzers; i++) {
            analyzers[i] = new Analyzer("ANALYZER-" + i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor("AUDITOR-" + i, queue, state, reportInterval);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor[] supervisors = new Supervisor[numSupervisors];
        Thread[] supervisorThreads = new Thread[numSupervisors];
        for (int i = 0; i < numSupervisors; i++) {
            supervisors[i] = new Supervisor("SUPERVISOR-" + i, state, newPolicy,
                    numMaxAnalyzers, updateInterval);
            supervisorThreads[i] = new Thread(supervisors[i]);
        }

        printMemoryUsage();

        // Start all threads
        for (Thread t : clinicThreads) t.start();
        for (Thread t : analyzerThreads) t.start();
        Thread.sleep(50);
        for (Thread t : auditorThreads) t.start();
        for (Thread t : supervisorThreads) t.start();

        // Run for specified duration
        Thread.sleep(runDuration);

        // Graceful shutdown
        System.out.println("\n=== Initiating Graceful Shutdown ===");

        for (Clinic c : clinics) c.shutdown();
        for (Thread t : clinicThreads) t.interrupt();
        for (Thread t : clinicThreads) t.join();

        while (queue.getSize() > 0) {
            Thread.sleep(100);
        }

        for (Auditor a : auditors) a.shutdown();
        for (Thread t : auditorThreads) t.interrupt();
        for (Thread t : auditorThreads) t.join();

        for (Analyzer a : analyzers) a.shutdown();
        for (Thread t : analyzerThreads) t.interrupt();
        for (Thread t : analyzerThreads) t.join();

        for (Supervisor s : supervisors) s.shutdown();
        for (Thread t : supervisorThreads) t.interrupt();
        for (Thread t : supervisorThreads) t.join();

        printMemoryUsage();
    }

    private static void runCalmWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 1: CALM PERIOD ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 20, 2, 2, 5, 1, 1,
                100, 200, 4000, 15000);
    }

    private static void runEmergencySurgeWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 2: EMERGENCY SURGE ===\n");
        runSimulation(SystemState.ProcessingPolicy.PRIORITY, 5, 6, 2, 5, 3, 1,
                20, 100, 2000, 10000);
    }

    private static void runReaderHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 3: READER-HEAVY ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 10, 3, 3, 5, 10, 1,
                200, 500, 3000, 10000);
    }

    private static void runWriterHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: WRITER-HEAVY ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 10, 3, 3, 5, 1, 10,
                200, 500, 3000, 20000);
    }

    private static void runBalancedWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 5: BALANCED ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 10, 3, 3, 5, 1, 1,
                200, 1000, 2000, 10000);
    }

    public static void printMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;

        System.out.println("\n=== MEMORY USAGE ===");
        System.out.println("Total Memory: " + (total / (1024 * 1024)) + " MB");
        System.out.println("Free Memory:  " + (free  / (1024 * 1024)) + " MB");
        System.out.println("Used Memory:  " + (used  / (1024 * 1024)) + " MB");
        System.out.println("====================\n");
    }
}