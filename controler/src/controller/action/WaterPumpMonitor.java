package controller.action;

import java.util.ArrayList;
import java.util.Date;

import controller.actor.IReadableOnOff;

public class WaterPumpMonitor implements IReadableOnOff {
    private final ArrayList<Record> records = new ArrayList<>();
    private boolean isOn;
    private final AbstractActionWithoutActor onAction = new AbstractActionWithoutActor() {
        @Override
        public void perform(int previousDurationMs) {
            isOn = true;
        }
    };

    private final AbstractActionWithoutActor offAction = new AbstractActionWithoutActor() {
        @Override
        public void perform(int previousDurationMs) {
            isOn = false;
            if (previousDurationMs > 0) {
                synchronized (records) {
                    records.add(new Record(new Date(System.currentTimeMillis() - previousDurationMs), previousDurationMs));
                }
            }
        }
    };

    public WaterPumpMonitor() {
    }

    public Action getOnAction() {
        return onAction;
    }

    public Action getOffAction() {
        return offAction;
    }

    public int getRecordCount() {
        synchronized (records) {
            return records.size();
        }
    }

    public Record[] getLastRecords(int count) {
        synchronized (records) {
            int startIndex = (count >= records.size()) ? 0 : records.size() - count;
            int len = records.size() - startIndex;
            Record[] res = new Record[len];
            for (int i = 0; i < len; i++) {
                res[i] = records.get(startIndex + i);
            }
            return res;
        }
    }

    public int getRecordCountInLastHours(double hours) {
        Date t = new Date(System.currentTimeMillis() - (int) (hours * 3600 * 1000));
        synchronized (records) {
            for (int i = records.size() - 1; i >= 0; i--) {
                if (records.get(i).time.compareTo(t) < 0) {
                    return records.size() - 1 - i;
                }
            }
            return records.size();
        }
    }

    @Override
    public boolean isOn() {
        return isOn;
    }

    public static class Record {
        public Date time;
        public int durationMs;

        public Record(Date time, int durationMs) {
            this.time = time;
            this.durationMs = durationMs;
        }
    }
}
