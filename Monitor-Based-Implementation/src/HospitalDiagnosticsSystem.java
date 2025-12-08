//Entry point with multiple workload scenarios
public class HospitalDiagnosticsSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Hospital Diagnostics Coordination System ===");
        System.out.println("Part A: Monitor-Based Implementation\n");

        // Allow command-line selection of workload
        String workloadType = args.length > 0 ? args[0] : "BALANCED";

        switch (workloadType.toUpperCase()) {
            case "CALM":
                runCalmWorkload();
                break;
            case "SURGE":
                runEmergencySurgeWorkload();
                break;
            case "REALISTIC":
                runRealisticWorkload();
                break;
            case "READER_HEAVY":
                runReaderHeavyWorkload();
                break;
            default:
                runBalancedWorkload();
                break;
        }
    }

    // Workload 1: Calm Period (Low Contention)
    private static void runCalmWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 1: CALM PERIOD (Low Contention) ===\n");

        int queueCapacity = 20;
        int numClinics = 2;
        int numAnalyzers = 4;
        int numAuditors = 2;
        int numSupervisors = 1;
        int runDuration = 30000; // 30 seconds
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        SystemState state = new SystemState();
        BoundedQueue queue = new BoundedQueue(queueCapacity, state);

        // Create clinics with QUIET load pattern
        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("Clinic-" + i, queue, state, LoadPattern.QUIET);
            clinicThreads[i] = new Thread(clinics[i]);
        }

        // Create analyzers
        Analyzer[] analyzers = new Analyzer[numAnalyzers];
        Thread[] analyzerThreads = new Thread[numAnalyzers];
        for (int i = 0; i < numAnalyzers; i++) {
            analyzers[i] = new Analyzer("ANALYZER-" + i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        // Create auditors and supervisors
        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor("AUDITOR-" + i, queue, state, 1000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor[] supervisors = new Supervisor[numSupervisors];
        Thread[] supervisorThreads = new Thread[numSupervisors];
        for (int i = 0; i < numSupervisors; i++) {
            supervisors[i] = new Supervisor("SUPERVISOR-" + i, state, newPolicy, numAnalyzers,10000);
            supervisorThreads[i] = new Thread(supervisors[i]);
        }

        runSimulation(clinics, analyzers, auditors, supervisors,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThreads, queue, state, runDuration);
    }

    // Workload 2: Emergency Surge (High Contention)
    private static void runEmergencySurgeWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 2: EMERGENCY SURGE (High Contention) ===\n");

        int queueCapacity = 5;  // Small queue
        int numClinics = 6;     // Many producers
        int numAnalyzers = 2;   // Few consumers
        int numAuditors = 2;
        int numSupervisors = 1;
        int runDuration = 30000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.PRIORITY;

        SystemState state = new SystemState();
        BoundedQueue queue = new BoundedQueue(queueCapacity, state);

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            // Mix of surge patterns
            LoadPattern pattern = (i < 4) ? LoadPattern.EMERGENCY_SURGE : LoadPattern.PEAK;
            clinics[i] = new Clinic("Emergency-Ward-" + i, queue, state, pattern);
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
            auditors[i] = new Auditor("AUDITOR-" + i, queue, state, 1000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor[] supervisors = new Supervisor[numSupervisors];
        Thread[] supervisorThreads = new Thread[numSupervisors];
        for (int i = 0; i < numSupervisors; i++) {
            supervisors[i] = new Supervisor("SUPERVISOR-" + i, state, newPolicy, numAnalyzers, 10000);
            supervisorThreads[i] = new Thread(supervisors[i]);
        }

        runSimulation(clinics, analyzers, auditors, supervisors,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThreads, queue, state, runDuration);
    }

    // Workload 3: Realistic Mixed Pattern
    private static void runRealisticWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 3: REALISTIC MIXED PATTERN ===\n");

        int queueCapacity = 10;
        int numClinics = 4;
        int numAnalyzers = 3;
        int numAuditors = 3;
        int numSupervisors = 1;
        int runDuration = 60000; // 60 seconds to see full pattern cycle
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.EMERGENCY_FIRST;

        SystemState state = new SystemState();
        BoundedQueue queue = new BoundedQueue(queueCapacity, state);

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("Ward-" + i, queue, state, LoadPattern.REALISTIC);
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
            auditors[i] = new Auditor("AUDITOR-" + i, queue, state, 1000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor[] supervisors = new Supervisor[numSupervisors];
        Thread[] supervisorThreads = new Thread[numSupervisors];
        for (int i = 0; i < numSupervisors; i++) {
            supervisors[i] = new Supervisor("SUPERVISOR-" + i, state, newPolicy, numAnalyzers, 15000);
            supervisorThreads[i] = new Thread(supervisors[i]);
        }

        runSimulation(clinics, analyzers, auditors, supervisors,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThreads, queue, state, runDuration);
    }

    // Workload 4: Reader-Heavy
    private static void runReaderHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: READER-HEAVY (Fairness Test) ===\n");

        int queueCapacity = 10;
        int numClinics = 3;
        int numAnalyzers = 3;
        int numAuditors = 10;  // Many readers
        int numSupervisors = 1;
        int runDuration = 30000;
        SystemState.ProcessingPolicy newPolicy = SystemState.ProcessingPolicy.FIFO;

        SystemState state = new SystemState();
        BoundedQueue queue = new BoundedQueue(queueCapacity, state);

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("Clinic-" + i, queue, state, LoadPattern.SCHEDULED);
            clinicThreads[i] = new Thread(clinics[i]);
        }

        Analyzer[] analyzers = new Analyzer[numAnalyzers];
        Thread[] analyzerThreads = new Thread[numAnalyzers];
        for (int i = 0; i < numAnalyzers; i++) {
            analyzers[i] = new Analyzer("ANALYZER-" + i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        // Many auditors with frequent reports
        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor("AUDITOR-" + i, queue, state, 1000); // Every 1 second
            auditorThreads[i] = new Thread(auditors[i]);
        }

        // Supervisor tries to write frequently (tests starvation)
        Supervisor[] supervisors = new Supervisor[numSupervisors];
        Thread[] supervisorThreads = new Thread[numSupervisors];
        for (int i = 0; i < numSupervisors; i++) {
            supervisors[i] = new Supervisor("SUPERVISOR-" + i, state, newPolicy, numAnalyzers, 3000);
            supervisorThreads[i] = new Thread(supervisors[i]);
        }

        runSimulation(clinics, analyzers, auditors, supervisors,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThreads, queue, state, runDuration);
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
        int runDuration = 10000;

        SystemState state = new SystemState();
        BoundedQueue queue = new BoundedQueue(queueCapacity, state);

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("CLINIC-" + i, queue, state, LoadPattern.SCHEDULED);
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
            auditors[i] = new Auditor("AUDITOR-" + i, queue, state, 1000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor[] supervisors = new Supervisor[numSupervisors];
        Thread[] supervisorThreads = new Thread[numSupervisors];
        for (int i = 0; i < numSupervisors; i++) {
            supervisors[i] = new Supervisor("SUPERVISOR-" + i, state, newPolicy, numMaxAnalyzers, 1000);
            supervisorThreads[i] = new Thread(supervisors[i]);
        }

        runSimulation(clinics, analyzers, auditors, supervisors,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThreads, queue, state, runDuration);
    }

    // Common simulation execution logic
    private static void runSimulation(
            Clinic[] clinics, Analyzer[] analyzers, Auditor[] auditors, Supervisor[] supervisors,
            Thread[] clinicThreads, Thread[] analyzerThreads, Thread[] auditorThreads,
            Thread[] supervisorThreads, BoundedQueue queue, SystemState state, int duration)
            throws InterruptedException {

        // Start all threads
        System.out.println("Starting simulation...\n");
        for (Thread t : clinicThreads) t.start();
        for (Thread t : analyzerThreads) t.start();
        Thread.sleep(50); //Auditor and Supervisor threads will be started after some Producing happened
        for (Thread t : auditorThreads) t.start();
        for (Thread t : supervisorThreads) t.start();

        // Run for specified duration
        Thread.sleep(duration);

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
    }
}