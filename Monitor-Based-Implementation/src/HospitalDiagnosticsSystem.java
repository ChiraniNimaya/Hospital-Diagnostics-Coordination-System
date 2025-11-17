public class HospitalDiagnosticsSystem {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Hospital Diagnostics Coordination System ===");
        System.out.println("Part A: Monitor-Based Implementation\n");

        // Configuration
        int queueCapacity = 10;
        int numClinics = 3;
        int numAnalyzers = 2;
        int numAuditors = 2;
        int runDuration = 20000; // 20 seconds

        // Initialize system
        BoundedQueue queue = new BoundedQueue(queueCapacity);
        SystemState state = new SystemState();

        // Create threads
        Clinic[] clinics = new Clinic[numClinics];
        Thread[] clinicThreads = new Thread[numClinics];
        for (int i = 0; i < numClinics; i++) {
            clinics[i] = new Clinic("Clinic-" + i, queue, state, 2);
            clinicThreads[i] = new Thread(clinics[i]);
        }

        Analyzer[] analyzers = new Analyzer[numAnalyzers];
        Thread[] analyzerThreads = new Thread[numAnalyzers];
        for (int i = 0; i < numAnalyzers; i++) {
            analyzers[i] = new Analyzer(i, queue, state, 1000);
            analyzerThreads[i] = new Thread(analyzers[i]);
        }

        Auditor[] auditors = new Auditor[numAuditors];
        Thread[] auditorThreads = new Thread[numAuditors];
        for (int i = 0; i < numAuditors; i++) {
            auditors[i] = new Auditor(i, state, 3000);
            auditorThreads[i] = new Thread(auditors[i]);
        }

        Supervisor supervisor = new Supervisor(state, 5000);
        Thread supervisorThread = new Thread(supervisor);

        // Start all threads
        for (Thread t : clinicThreads) t.start();
        for (Thread t : analyzerThreads) t.start();
        for (Thread t : auditorThreads) t.start();
        supervisorThread.start();

        // Run for specified duration
        Thread.sleep(runDuration);

        // Graceful shutdown
        System.out.println("\n=== Initiating Graceful Shutdown ===");
        for (Clinic c : clinics) c.shutdown();
        for (Analyzer a : analyzers) a.shutdown();
        for (Auditor a : auditors) a.shutdown();
        supervisor.shutdown();

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

        // Final metrics
        System.out.println("\n=== Final Metrics ===");
        System.out.println("Total Admitted: " + queue.getTotalAdmitted());
        System.out.println("Average Wait Time: " + queue.getAverageWaitTime() + "ms");
        System.out.println("Final Queue Size: " + queue.getSize());

        System.out.println("\n=== All threads terminated successfully ===");
    }
}
