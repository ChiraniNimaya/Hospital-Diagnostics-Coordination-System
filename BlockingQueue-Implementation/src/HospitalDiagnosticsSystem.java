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

    // Workload 1: Calm Period (Low Contention)
    private static void runCalmWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 1: CALM PERIOD ===\n");

        int queueCapacity = 20;
        int numClinics = 2;
        int numAnalyzers = 2;
        int numMaxAnalyzers = 5;
        int numAuditors = 1;
        int numSupervisors = 1;
        int producerInterval = 100; // Normal production
        int reportInterval = 200;
        int updateInterval = 4000;
        int runDuration = 15000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, producerInterval, reportInterval, updateInterval, runDuration);
    }

    // Workload 2: Emergency Surge (High Contention)
    private static void runEmergencySurgeWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 2: EMERGENCY SURGE ===\n");

        int queueCapacity = 5;  // Small queue
        int numClinics = 6;     // Many producers
        int numAnalyzers = 2;   // Few consumers
        int numMaxAnalyzers = 5;
        int numAuditors = 3;
        int numSupervisors = 1;
        int producerInterval = 20; // Fast production
        int reportInterval = 100;
        int updateInterval = 2000;
        int runDuration = 10000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.PRIORITY;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, producerInterval, reportInterval, updateInterval, runDuration);
    }


    // Workload 3: Reader-Heavy
    private static void runReaderHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: READER-HEAVY ===\n");

        int queueCapacity = 10;
        int numClinics = 3;
        int numAnalyzers = 3;
        int numMaxAnalyzers = 5;
        int numAuditors = 10;  // Many readers
        int producerInterval = 200;
        int numSupervisors = 1;
        int reportInterval = 500;
        int updateInterval = 3000;
        int runDuration = 10000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, producerInterval, reportInterval, updateInterval, runDuration);
    }

    // Workload 4: Writer-Heavy
    private static void runWriterHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: WRITER-HEAVY ===\n");

        int queueCapacity = 10;
        int numClinics = 3;
        int numAnalyzers = 3;
        int numMaxAnalyzers = 5;
        int numAuditors = 1;
        int numSupervisors = 10; // Many writers
        int producerInterval = 200;
        int reportInterval = 1000;
        int updateInterval = 3000;
        int runDuration = 20000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, producerInterval, reportInterval, updateInterval, runDuration);
    }

    // Workload 5: Balanced (Default)
    private static void runBalancedWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 5: BALANCED (Default) ===\n");
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        int queueCapacity = 10;
        int numClinics = 6;
        int numAnalyzers = 3;
        int numMaxAnalyzers = 5;
        int numAuditors = 2;
        int numSupervisors = 1;
        int producerInterval = 200;
        int reportInterval = 1000;
        int updateInterval = 1000;
        int runDuration = 10000;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, producerInterval, reportInterval, updateInterval, runDuration);
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

}