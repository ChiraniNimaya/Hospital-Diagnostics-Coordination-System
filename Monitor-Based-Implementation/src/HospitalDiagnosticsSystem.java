//Entry point with multiple workload scenarios
public class HospitalDiagnosticsSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Hospital Diagnostics Coordination System ===");
        System.out.println("Part A: Monitor-Based Implementation");
        System.out.println("\nStart Time: " + System.currentTimeMillis() + "\n");
        // Allow command-line selection of workload
        String workloadType = args.length > 0 ? args[0] : "BALANCED";

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
        int numAnalyzers = 4;
        int numMaxAnalyzers = 5;
        int numAuditors = 2;
        int numSupervisors = 1;
        LoadPattern loadPattern = LoadPattern.QUIET;
        int reportInterval = 1000;
        int updateInterval = 10000;
        int runDuration = 30000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, loadPattern, reportInterval, updateInterval, runDuration);
    }

    // Workload 2: Emergency Surge (High Contention)
    private static void runEmergencySurgeWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 2: EMERGENCY SURGE ===\n");

        int queueCapacity = 5;  // Small queue
        int numClinics = 6;     // Many producers
        int numAnalyzers = 2;   // Few consumers
        int numMaxAnalyzers = 5;
        int numAuditors = 2;
        int numSupervisors = 1;
        LoadPattern loadPattern = LoadPattern.EMERGENCY_SURGE;
        int reportInterval = 1000;
        int updateInterval = 10000;
        int runDuration = 30000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.PRIORITY;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, loadPattern, reportInterval, updateInterval, runDuration);
    }


    // Workload 3: Reader-Heavy
    private static void runReaderHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: READER-HEAVY ===\n");

        int queueCapacity = 10;
        int numClinics = 3;
        int numAnalyzers = 3;
        int numMaxAnalyzers = 5;
        int numAuditors = 10;  // Many readers
        int numSupervisors = 1;
        LoadPattern loadPattern = LoadPattern.SCHEDULED;
        int reportInterval = 1000;
        int updateInterval = 3000;
        int runDuration = 10000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, loadPattern, reportInterval, updateInterval, runDuration);
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
        LoadPattern loadPattern = LoadPattern.SCHEDULED;
        int reportInterval = 1000;
        int updateInterval = 3000;
        int runDuration = 20000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, loadPattern, reportInterval, updateInterval, runDuration);
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
        LoadPattern loadPattern = LoadPattern.SCHEDULED;
        int reportInterval = 1000;
        int updateInterval = 1000;
        int runDuration = 10000;

        runSimulation(newPolicy, queueCapacity, numClinics, numAnalyzers, numMaxAnalyzers, numAuditors, numSupervisors, loadPattern, reportInterval, updateInterval, runDuration);
    }

    // Common simulation execution logic
    private static void runSimulation(SystemState.ProcessingPolicy newPolicy,
                                        int queueCapacity,
                                        int numClinics,
                                        int numAnalyzers,
                                        int numMaxAnalyzers,
                                        int numAuditors,
                                        int numSupervisors,
                                        LoadPattern loadPattern,
                                        int reportInterval,
                                        int updateInterval,
                                        int runDuration)
                                                throws InterruptedException {

        SystemState state = new SystemState();
        BoundedQueue queue = new BoundedQueue(queueCapacity, state);

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            if (loadPattern == LoadPattern.EMERGENCY_SURGE)
                loadPattern = (i < 4) ? loadPattern : LoadPattern.PEAK;
            clinics[i] = new Clinic("CLINIC-" + i, queue, state, loadPattern);
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