//Defines variable load scenarios
enum LoadPattern {
    // Quiet period: 1 order every 2 seconds
    QUIET {
        @Override
        public int getInterArrivalTime(long elapsedMs) {
            return 2000;
        }
    },

    // Normal/scheduled: 1 order per second with small random variation
    SCHEDULED {
        @Override
        public int getInterArrivalTime(long elapsedMs) {
            return 800 + (int)(Math.random() * 400); // 800-1200ms
        }
    },

    // Peak hours: 3 orders per second during certain time windows
    PEAK {
        @Override
        public int getInterArrivalTime(long elapsedMs) {
            int second = (int)((elapsedMs / 1000) % 60);
            // Peak every 20 seconds for 10 seconds
            if (second % 20 < 10) {
                return 300; // High rate during peak
            } else {
                return 1000; // Normal rate otherwise
            }
        }
    },

    // Emergency surge: Very high rate (5 orders/sec) then drop back
    EMERGENCY_SURGE {
        @Override
        public int getInterArrivalTime(long elapsedMs) {
            int second = (int)(elapsedMs / 1000);
            // Surge for first 15 seconds, then calm down
            if (second < 15) {
                return 200; // Emergency rate (5/sec)
            } else {
                return 1500; // Calm period after surge
            }
        }
    },

    // Realistic mixed: Combines quiet, scheduled peaks, and occasional surges
    REALISTIC {
        @Override
        public int getInterArrivalTime(long elapsedMs) {
            int second = (int)((elapsedMs / 1000) % 60);

            // Night shift (quiet): seconds 0-20
            if (second < 20) {
                return 2000;
            }
            // Morning peak: seconds 20-35
            else if (second < 35) {
                return 400;
            }
            // Emergency surge: seconds 35-45
            else if (second < 45) {
                return 150;
            }
            // Afternoon scheduled: seconds 45-60
            else {
                return 1000;
            }
        }
    };

    public abstract int getInterArrivalTime(long elapsedMs);
}