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
        int runDuration = 30000; // 30 seconds

        BoundedQueue queue = new BoundedQueue(queueCapacity);
        SystemState state = new SystemState();

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
            analyzers[i] = new Analyzer(i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        // Create auditors and supervisor
        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor(i, state, 5000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor supervisor = new Supervisor(state, 10000);
        Thread supervisorThread = new Thread(supervisor);

        runSimulation(clinics, analyzers, auditors, supervisor,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThread, queue, state, runDuration);
    }

    // Workload 2: Emergency Surge (High Contention)
    private static void runEmergencySurgeWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 2: EMERGENCY SURGE (High Contention) ===\n");

        int queueCapacity = 5;  // Small queue
        int numClinics = 6;     // Many producers
        int numAnalyzers = 2;   // Few consumers
        int numAuditors = 2;
        int runDuration = 30000;

        BoundedQueue queue = new BoundedQueue(queueCapacity);
        SystemState state = new SystemState();

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
            analyzers[i] = new Analyzer(i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor(i, state, 5000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor supervisor = new Supervisor(state, 10000);
        Thread supervisorThread = new Thread(supervisor);

        runSimulation(clinics, analyzers, auditors, supervisor,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThread, queue, state, runDuration);
    }

    // Workload 3: Realistic Mixed Pattern
    private static void runRealisticWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 3: REALISTIC MIXED PATTERN ===\n");

        int queueCapacity = 10;
        int numClinics = 4;
        int numAnalyzers = 3;
        int numAuditors = 3;
        int runDuration = 60000; // 60 seconds to see full pattern cycle

        BoundedQueue queue = new BoundedQueue(queueCapacity);
        SystemState state = new SystemState();

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("Ward-" + i, queue, state, LoadPattern.REALISTIC);
            clinicThreads[i] = new Thread(clinics[i]);
        }

        Analyzer[] analyzers = new Analyzer[numAnalyzers];
        Thread[] analyzerThreads = new Thread[numAnalyzers];
        for (int i = 0; i < numAnalyzers; i++) {
            analyzers[i] = new Analyzer(i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor(i, state, 8000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor supervisor = new Supervisor(state, 15000);
        Thread supervisorThread = new Thread(supervisor);

        runSimulation(clinics, analyzers, auditors, supervisor,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThread, queue, state, runDuration);
    }

    // Workload 4: Reader-Heavy (Tests Reader-Writer Fairness)
    private static void runReaderHeavyWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 4: READER-HEAVY (Fairness Test) ===\n");

        int queueCapacity = 10;
        int numClinics = 3;
        int numAnalyzers = 3;
        int numAuditors = 10;  // Many readers
        int runDuration = 30000;

        BoundedQueue queue = new BoundedQueue(queueCapacity);
        SystemState state = new SystemState();

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("Clinic-" + i, queue, state, LoadPattern.SCHEDULED);
            clinicThreads[i] = new Thread(clinics[i]);
        }

        Analyzer[] analyzers = new Analyzer[numAnalyzers];
        Thread[] analyzerThreads = new Thread[numAnalyzers];
        for (int i = 0; i < numAnalyzers; i++) {
            analyzers[i] = new Analyzer(i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        // Many auditors with frequent reports
        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor(i, state, 1000); // Every 1 second
            auditorThreads[i] = new Thread(auditors[i]);
        }

        // Supervisor tries to write frequently (tests starvation)
        Supervisor supervisor = new Supervisor(state, 3000);
        Thread supervisorThread = new Thread(supervisor);

        runSimulation(clinics, analyzers, auditors, supervisor,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThread, queue, state, runDuration);
    }

    // Workload 5: Balanced (Default)
    private static void runBalancedWorkload() throws InterruptedException {
        System.out.println("=== WORKLOAD 5: BALANCED (Default) ===\n");

        int queueCapacity = 10;
        int numClinics = 3;
        int numAnalyzers = 3;
        int numAuditors = 2;
        int runDuration = 20000;

        BoundedQueue queue = new BoundedQueue(queueCapacity);
        SystemState state = new SystemState();

        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("Clinic-" + i, queue, state, LoadPattern.SCHEDULED);
            clinicThreads[i] = new Thread(clinics[i]);
        }

        Analyzer[] analyzers = new Analyzer[numAnalyzers];
        Thread[] analyzerThreads = new Thread[numAnalyzers];
        for (int i = 0; i < numAnalyzers; i++) {
            analyzers[i] = new Analyzer(i, queue, state);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor(i, state, 5000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor supervisor = new Supervisor(state, 8000);
        Thread supervisorThread = new Thread(supervisor);

        runSimulation(clinics, analyzers, auditors, supervisor,
                clinicThreads, analyzerThreads, auditorThreads,
                supervisorThread, queue, state, runDuration);
    }

    // Common simulation execution logic
    private static void runSimulation(
            Clinic[] clinics, Analyzer[] analyzers, Auditor[] auditors, Supervisor supervisor,
            Thread[] clinicThreads, Thread[] analyzerThreads, Thread[] auditorThreads,
            Thread supervisorThread, BoundedQueue queue, SystemState state, int duration)
            throws InterruptedException {

        // Start all threads
        System.out.println("Starting simulation...\n");
        for (Thread t : clinicThreads) t.start();
        for (Thread t : analyzerThreads) t.start();
        for (Thread t : auditorThreads) t.start();
        supervisorThread.start();

        // Monitor queue size periodically
        Thread monitor = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(5000);
                    System.out.println("\n[MONITOR] Queue size: " + queue.getSize() +
                            ", Admitted: " + queue.getTotalAdmitted());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        monitor.start();

        // Run for specified duration
        Thread.sleep(duration);

        // Graceful shutdown
        System.out.println("\n=== Initiating Graceful Shutdown ===");
        for (Clinic c : clinics) c.shutdown();
        for (Analyzer a : analyzers) a.shutdown();
        for (Auditor a : auditors) a.shutdown();
        supervisor.shutdown();
        monitor.interrupt();

        // Interrupt all threads
        for (Thread t : clinicThreads) t.interrupt();
        for (Thread t : analyzerThreads) t.interrupt();
        for (Thread t : auditorThreads) t.interrupt();
        supervisorThread.interrupt();

        // Wait for all threads to finish
        for (Thread t : clinicThreads) t.join();
        for (Thread t : analyzerThreads) t.join();
        for (Thread t : auditorThreads) t.join();
        supervisorThread.join();
        monitor.join();

        // Final metrics
        printFinalMetrics(queue, state);
    }

    private static void printFinalMetrics(BoundedQueue queue, SystemState state) {
        System.out.println("\n=== Final Metrics ===");
        System.out.println("Total Admitted to Queue: " + queue.getTotalAdmitted());
        System.out.println("Total Rejected: " + queue.getTotalRejected());
        System.out.println("Average Producer Wait Time: " +
                String.format("%.2f", queue.getAverageWaitTime()) + "ms");
        System.out.println("Final Queue Size: " + queue.getSize());
        System.out.println("\n=== All threads terminated successfully ===");
    }
}