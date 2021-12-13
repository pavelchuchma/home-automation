package node;

public enum CpuFrequency {
    unknown(-1),
    oneMHz(1),
    twoMHz(2),
    eightMHz(8),
    sixteenMHz(16);

    private final int index;

    private CpuFrequency(int index) {
        this.index = index;
    }

    public static CpuFrequency get(int cpuFrequency) {
        switch (cpuFrequency) {
            case 1:
                return oneMHz;
            case 2:
                return twoMHz;
            case 8:
                return eightMHz;
            case 16:
                return sixteenMHz;
            default:
                throw new IllegalArgumentException("Unsupported frequency value");
        }
    }

    public int getValue() {
        return index;
    }
}