class TestOrder {
    private static int counter = 0;
    private final long id;
    private final String type; // "BLOOD", "PCR", "HISTOPATHOLOGY"
    private final long submissionTime;
    private final String source;

    public TestOrder(String type, String source) {
        this.id = ++counter;
        this.type = type;
        this.source = source;
        this.submissionTime = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public String getType() { return type; }
    public long getSubmissionTime() { return submissionTime; }
    public String getSource() { return source; }
}