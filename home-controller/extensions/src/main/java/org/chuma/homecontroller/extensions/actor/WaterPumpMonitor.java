package org.chuma.homecontroller.extensions.actor;

import java.util.ArrayList;
import java.util.Date;

import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.action.GenericCodeAction;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class WaterPumpMonitor implements IReadableOnOff {
    private final ArrayList<Record> records = new ArrayList<>();
    private boolean isOn;
    private final Action onAction = new GenericCodeAction(timeSinceLastAction -> isOn = true);
    private final Action offAction = new GenericCodeAction(timeSinceLastAction -> {
        isOn = false;
        if (timeSinceLastAction > 0) {
            synchronized (records) {
                records.add(new Record(new Date(System.currentTimeMillis() - timeSinceLastAction), timeSinceLastAction / 1000.0));
            }
        }
    });

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
        Date t = new Date(System.currentTimeMillis() - (int)(hours * 3600 * 1000));
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
        public double duration;

        public Record(Date time, double duration) {
            this.time = time;
            this.duration = duration;
        }
    }
}
