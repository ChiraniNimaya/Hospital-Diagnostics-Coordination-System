public class LifecycleWatcher {
    private LifecycleWatcher() {} //private constructor prevents instantiation

    public static Thread watch(Thread target, String label) {
        Thread watcher = new Thread(() -> {
            Thread.State lastState = target.getState();
            System.out.println("[WATCHER] " + label + " : " + lastState);

            while (lastState != Thread.State.TERMINATED) {
                Thread.State currentState = target.getState();

                if (currentState != lastState) {
                    System.out.println("[WATCHER] " + label +
                            " : " + lastState + " → " + currentState);
                    lastState = currentState;
                }

                try {
                    Thread.sleep(0);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, label + "-watcher");

        watcher.setDaemon(true);
        watcher.start();
        return watcher;
    }
}
