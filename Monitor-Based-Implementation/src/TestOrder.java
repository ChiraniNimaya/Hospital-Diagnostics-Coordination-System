class TestOrder {
    public enum OrderType { BLOOD, PCR, HISTOPATHOLOGY }
    public enum OrderPriority { ROUTINE, URGENT, EMERGENCY }

    private static int counter = 0;
    private final long id;
    private final OrderType type;
    private final OrderPriority orderPriority;
    private final long submissionTime;
    private final String source;

    public TestOrder(OrderType type, String source) {
        this.id = ++counter;
        this.type = type;
        this.source = source;
        this.submissionTime = System.currentTimeMillis();
        // Assign priority based on type
        this.orderPriority = assignPriority(type);
    }

    private OrderPriority assignPriority(OrderType type) {
        // HISTOPATHOLOGY is typically urgent, PCR can be emergency, BLOOD is routine
        switch (type) {
            case HISTOPATHOLOGY:
                return OrderPriority.URGENT;
            case PCR:
                return Math.random() < 0.3 ? OrderPriority.EMERGENCY : OrderPriority.URGENT;
            case BLOOD:
            default:
                return Math.random() < 0.1 ? OrderPriority.URGENT : OrderPriority.ROUTINE;
        }
    }

    public long getId() { return id; }
    public OrderType getType() { return type; }
    public long getSubmissionTime() { return submissionTime; }
    public String getSource() { return source; }
    public OrderPriority getPriority() { return orderPriority; }

    // For priority-based sorting
    public int comparePriority(TestOrder other) {
        // Higher priority first (EMERGENCY=0, URGENT=1, ROUTINE=2)
        int priorityCompare = this.orderPriority.compareTo(other.orderPriority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // If same priority, FIFO (earlier submission time first)
        return Long.compare(this.submissionTime, other.submissionTime);
    }
}