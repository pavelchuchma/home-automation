package controller.action.condition;

public class PressDurationCondition implements ICondition {
    int minDurationMs;
    int maxDurationMs;

    public PressDurationCondition(int minDurationMs, int maxDurationMs) {
        this.minDurationMs = minDurationMs;
        this.maxDurationMs = maxDurationMs;
    }

    @Override
    public boolean isTrue(int previousDurationMs) {
        return previousDurationMs >= minDurationMs && previousDurationMs <= maxDurationMs;
    }
}
