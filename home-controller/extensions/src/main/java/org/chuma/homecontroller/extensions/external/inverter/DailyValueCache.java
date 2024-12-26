package org.chuma.homecontroller.extensions.external.inverter;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class DailyValueCache<D> {
    DailyValueCache(int successDurationSecs, int failureDurationSecs) {
        this.failureDurationSecs = failureDurationSecs;
        this.successDurationSecs = successDurationSecs;
    }

    class Entry {
        final D data;
        final long expirationTime;

        public Entry(D data, int durationSec) {
            this.data = data;
            expirationTime = System.currentTimeMillis() + (long)durationSec * 1000;
        }
    }

    private final Map<String, Entry> map = new HashMap<>();
    private final int failureDurationSecs;
    private final int successDurationSecs;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Logger log = LoggerFactory.getLogger(DailyValueCache.class);

    String buildKey(Calendar date) {
        return String.format("%d-%d-%d", date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
    }

    synchronized D get(Calendar date) {
        String key = buildKey(date);
        Entry entry = map.get(key);
        if (entry != null) {
            if (System.currentTimeMillis() < entry.expirationTime) {
                return entry.data;
            }
            // remove expired entry
            map.remove(key);
        }
        // store null to skip next requests before fresh value will be filled from the thread
        set(date, null, failureDurationSecs);
        executor.execute(() -> refreshEntry(date));
        return null;
    }

    synchronized void cleanup() {
        long currentTime = System.currentTimeMillis();
        map.entrySet().removeIf(entry -> entry.getValue().expirationTime < currentTime);
    }

    private synchronized void set(Calendar date, D data, int durationSecs) {
        map.put(buildKey(date), new Entry(data, durationSecs));
        cleanup();
    }

    private void refreshEntry(Calendar date) {
        try {
            log.debug("Refreshing daily value for {}", date);
            D data = getEntryImpl(date);
            set(date, data, (data != null) ? successDurationSecs : failureDurationSecs);
        } catch (Exception e) {
            log.error("Failed to refresh entry for {}", date, e);
        }
    }

    public abstract D getEntryImpl(Calendar date);
}
