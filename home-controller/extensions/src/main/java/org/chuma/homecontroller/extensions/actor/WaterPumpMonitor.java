package org.chuma.homecontroller.extensions.actor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.chuma.homecontroller.base.node.ListenerManager;

import org.chuma.homecontroller.controller.action.Action;
import org.chuma.homecontroller.controller.action.GenericCodeAction;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class WaterPumpMonitor implements IReadableOnOff {
    public interface Listener {
        void onPumpStateChanged(boolean on, long timestampMs);
    }

    private final ArrayList<Record> records = new ArrayList<>();
    private final ListenerManager<Listener> listeners = new ListenerManager<>();
    private boolean isOn;
    private final Action onAction = new GenericCodeAction(timeSinceLastAction -> {
        isOn = true;
        notifyListeners(true, System.currentTimeMillis());
    });
    private final Action offAction = new GenericCodeAction(timeSinceLastAction -> {
        isOn = false;
        long now = System.currentTimeMillis();
        if (timeSinceLastAction > 0) {
            synchronized (records) {
                records.add(new Record(new Date(now - timeSinceLastAction), timeSinceLastAction / 1000.0));
            }
        }
        notifyListeners(false, now);
    });

    public WaterPumpMonitor() {
    }

    public Action getOnAction() {
        return onAction;
    }

    public Action getOffAction() {
        return offAction;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(boolean on, long timestampMs) {
        listeners.callListeners(l -> l.onPumpStateChanged(on, timestampMs));
    }

    /**
     * @return cycles that ended after the given time, oldest first
     */
    public List<Record> getRecordsEndedAfter(long timestampMs) {
        List<Record> result = new ArrayList<>();
        synchronized (records) {
            for (int i = records.size() - 1; i >= 0; i--) {
                Record r = records.get(i);
                if (r.time.getTime() + (long)(r.duration * 1000) <= timestampMs) {
                    break;
                }
                result.add(0, r);
            }
        }
        return result;
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
