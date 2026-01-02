//Entry point with multiple workload scenarios
public class HospitalDiagnosticsSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Hospital Diagnostics Coordination System ===");
        System.out.println("Part B: BlockingQueue-Based Implementation");
        System.out.println("\nStart Time: " + System.currentTimeMillis() + "\n");
        // Allow command-line selection of workload
        String workloadType = args.length > 0 ? args[0] : "SURGE";

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

    // Common simulation execution logic
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

        SystemState state = new SystemState();
        BlockingQueue queue = new BlockingQueue(queueCapacity, state);

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
            supervisors[i] = new Supervisor("SUPERVISOR-" + i, state, newPolicy, numMaxAnalyzers, updateInterval);
            supervisorThreads[i] = new Thread(supervisors[i]);
        }

        printMemoryUsage();

        // Watch thread states of some threads
        LifecycleWatcher.watch(clinicThreads[0], "CLINIC-0");
//        LifecycleWatcher.watch(analyzerThreads[0], "ANALYZER-0");
//        LifecycleWatcher.watch(auditorThreads[0], "AUDITOR-0");
//        LifecycleWatcher.watch(supervisorThreads[0], "SUPERVISOR-0");

        // Start all threads
        for (Thread t : clinicThreads) t.start();
        for (Thread t : analyzerThreads) t.start();
        Thread.sleep(50); //Auditor and Supervisor threads will be started after some Producing happened
        for (Thread t : auditorThreads) t.start();
        for (Thread t : supervisorThreads) t.start();

        // Run for specified duration
        Thread.sleep(runDuration);

        // Graceful shutdown
        System.out.println("\n=== Initiating Graceful Shutdown ===");

        // Stop clinics
        for (Clinic c : clinics) c.shutdown();
        for (Thread t : clinicThreads) t.interrupt();
        for (Thread t : clinicThreads) t.join();

        // Wait for queue to empty (let analyzers finish consuming)
        while (queue.getSize() > 0) {
            Thread.sleep(100);
        }

        // Stop auditors
        for (Auditor a : auditors) a.shutdown();
        for (Thread t : auditorThreads) t.interrupt();
        for (Thread t : auditorThreads) t.join();

        // Stop Analyzers
        for (Analyzer a : analyzers) a.shutdown();
        for (Thread t : analyzerThreads) t.interrupt();
        for (Thread t : analyzerThreads) t.join();


        // Stop Supervisors
        for (Supervisor a : supervisors) a.shutdown();
        for (Thread t : supervisorThreads) t.interrupt();
        for (Thread t : supervisorThreads) t.join();

        printMemoryUsage();
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


    // Workload 1: Calm Period (Low Contention)
    private static void runCalmWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 1: CALM PERIOD ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 20, 2, 2, 5, 1, 1,
                100, 200, 4000, 15000);
    }

    // Workload 2: Emergency Surge (High Contention)
    private static void runEmergencySurgeWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 2: EMERGENCY SURGE ===\n");
        runSimulation(SystemState.ProcessingPolicy.PRIORITY, 5, 6, 2, 5, 3, 1,
                20, 100, 2000, 10000);
    }

    // Workload 3: Reader-Heavy
    private static void runReaderHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: READER-HEAVY ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 10, 3, 3, 5, 10, 1,
                200, 500, 3000, 10000);
    }

    // Workload 4: Writer-Heavy
    private static void runWriterHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: WRITER-HEAVY ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 10, 3, 3, 5, 1, 10,
                200, 500, 3000, 20000);
    }

    // Workload 5: Balanced (Default)
    private static void runBalancedWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 5: BALANCED (Default) ===\n");
        runSimulation(SystemState.ProcessingPolicy.FIFO, 10, 3, 3, 5, 1, 1,
                200, 1000, 2000, 10000);
    }

}