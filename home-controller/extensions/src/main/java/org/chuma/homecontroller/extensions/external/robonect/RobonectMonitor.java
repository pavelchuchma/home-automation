package org.chuma.homecontroller.extensions.external.robonect;

import java.util.ArrayDeque;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.AbstractStateMonitor;
import org.chuma.homecontroller.extensions.external.robonect.client.RobonectClient;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Gps;
import org.chuma.homecontroller.extensions.external.robonect.client.model.GpsInfo;
import org.chuma.homecontroller.extensions.external.robonect.client.model.MowerInfo;
import org.chuma.homecontroller.extensions.external.robonect.client.model.MowerStatus;
import org.chuma.homecontroller.extensions.external.robonect.client.model.Status;
import org.chuma.homecontroller.extensions.external.robonect.client.model.WeatherInfo;

public class RobonectMonitor extends AbstractStateMonitor<State> {
    private static final Logger log = LoggerFactory.getLogger(RobonectMonitor.class.getName());
    final RobonectClient client;
    final ArrayDeque<GpsHistoryEntry> gpsHistory = new ArrayDeque<>();
    private final Object gpsHistoryLock = new Object();
    static int GPS_HISTORY_LENGTH_SECS = 24 * 3600;

    public record GpsHistoryEntry(
            double latitude,
            double longitude,
            long timestamp
    ) {
    }

    public RobonectMonitor(RobonectClient client, int refreshInternalMs, int maxUnusedRunTimeMs) {
        super("RobonectMonitor", refreshInternalMs, maxUnusedRunTimeMs);
        log.info("Creating RobonectMonitor");

        this.client = client;
    }

    public RobonectClient getClient() {
        return client;
    }

    @Override
    protected State getStateImpl(boolean firstCallAfterSleep) {
        try {
            long startTime = 0;
            if (log.isTraceEnabled()) {
                startTime = System.currentTimeMillis();
            }
            log.debug("refreshing Robonect state");
            MowerInfo mowerInfo = client.getMowerInfo();
            if (mowerInfo == null) {
                log.error("Fail to get Robonect info");
                return null;
            }
            Status mowerStatus = mowerInfo.getStatus();
            MowerStatus status = mowerStatus.getStatus();
            // get weather info only if the mower is at home
            WeatherInfo weatherInfo = (mowerStatus.isHome()) ? client.getWeatherInfo() : null;
            WeatherInfo.Weather weather = (weatherInfo != null && weatherInfo.service().enable()) ? weatherInfo.weather() : null;

            Gps gps = null;
            if (!mowerStatus.isHome() && status != MowerStatus.OFF) {
                GpsInfo gpsInfo = client.getGpsInfo();
                if (gpsInfo != null) {
                    gps = gpsInfo.getGps();
                }
            }

            synchronized (gpsHistoryLock) {
                if (firstCallAfterSleep) {
                    gpsHistory.clear();
                }

                long now = getTimestamp();
                // remove too old entries
                while (!gpsHistory.isEmpty() && gpsHistory.peekFirst().timestamp() < now - GPS_HISTORY_LENGTH_SECS) {
                    gpsHistory.removeFirst();
                }

                if (gps != null && gps.getLatitude() != 0 && gps.getLongitude() != 0) {
                    gpsHistory.addLast(new GpsHistoryEntry(gps.getLatitude(), gps.getLongitude(), now));
                }
            }

            State state = new State(mowerStatus, mowerInfo.getTimer(), mowerInfo.getWlan(), mowerInfo.getHealth(),
                    mowerInfo.getBlades(), mowerInfo.getError(), gps, weather);
            log.trace("done in {} ms, mode: {}", System.currentTimeMillis() - startTime, status);
            return state;
        } catch (Exception e) {
            log.error("Failed to refresh Robonect state", e);
            return null;
        }
    }

    public long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Retrieves an array of GPS history entries that have timestamps greater than the specified fromTimestamp.
     * This method fetches and returns historical GPS data recorded up until the current point in time.
     *
     * @param fromTimestamp The timestamp from which to retrieve GPS history entries. If the value is negative,
     *                      it will be treated as an offset relative to the current time.
     * @return An array of GpsHistoryEntry objects representing the historical GPS data, ordered
     * by their timestamps in ascending order. The array will be empty if no entries match the criteria.
     */
    public GpsHistoryEntry[] getGpsHistory(long fromTimestamp) {
        if (fromTimestamp < 0) {
            // convert negative time to offset from current time
            fromTimestamp = getTimestamp() + fromTimestamp;
        }

        synchronized (gpsHistoryLock) {
            Iterator<GpsHistoryEntry> iterator = gpsHistory.descendingIterator();
            // get count of items to be returned
            int count = 0;
            while (iterator.hasNext() && iterator.next().timestamp() > fromTimestamp) {
                count++;
            }

            GpsHistoryEntry[] result = new GpsHistoryEntry[count];
            Iterator<GpsHistoryEntry> iterator2 = gpsHistory.descendingIterator();
            for (int i = count - 1; i >= 0; i--) {
                result[i] = iterator2.next();
            }
            return result;
        }
    }
}
