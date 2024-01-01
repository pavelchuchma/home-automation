package org.chuma.homecontroller.extensions.external.inverter;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.lang.Math.max;

import org.chuma.homecontroller.controller.actor.IOnOffActor;

/**
 * Manages the main grid connection. It keeps grid connection active only if:
 * - battery SOC is low
 * - battery full to allow feed-in energy
 */
public class GridConnectionManager {
    protected static Logger log = LoggerFactory.getLogger(GridConnectionManager.class.getName());
    private static final Object lock = new Object();
    private final long periodMs;
    private final InverterMonitor inverterMonitor;
    private final InverterManager inverterManager;
    private final IOnOffActor disconnectActor;
    private boolean running;

    public GridConnectionManager(long periodMs, InverterMonitor inverterMonitor, InverterManager inverterManager, IOnOffActor disconnectActor) {
        this.periodMs = periodMs;
        this.inverterMonitor = inverterMonitor;
        this.inverterManager = inverterManager;
        this.disconnectActor = disconnectActor;
    }

    public synchronized void start() {
        running = true;
        new Thread(() -> {
            log.info("Starting GridConnectionManager");
            while (running) {
                loopBody();
            }
            log.info("GridConnectionManager stopped");
        }, "GridConnectionManager").start();
    }

    private void loopBody() {
        log.trace("entering refreshLoopBody");

        synchronized (lock) {
            try {
                log.trace("going to sleep for {} ms before next refresh", periodMs);
                lock.wait(periodMs);
                log.trace("woke up");
            } catch (InterruptedException e) {
                //no action
            }
        }
        if (!running) {
            // stopped, no more fun
            return;
        }
        try {
            processImpl();
        } catch (Exception e) {
            log.error("Failed to update grid connection", e);
        }
    }

    private void processImpl() {
        InverterState state = inverterMonitor.getStateSync(true);
        if (state == null) {
            log.error("Failed to get inverter state");
            return;
        }

        int batterySoc = state.getBatterySoc();
        int minimalSoc = inverterManager.getMinimalSoc();
        int batteryReserve = inverterManager.getBatteryReserve();
        int batteryPower = state.getBatteryPower();

        boolean disconnect = getNewState(batterySoc, minimalSoc, batteryReserve, batteryPower);

        String msg = MessageFormat.format("because: batterySoc={0}, minimalSoc={1}, batteryReserve={2}, batteryPower={3}",
                batterySoc, minimalSoc, batteryReserve, batteryPower);
        if (disconnect != disconnectActor.isOn()) {
            log.info("{}connecting grid {}", (disconnect) ? "dis" : "", msg);
        } else {
            log.debug("gridDisconnect={} {}", disconnect, msg);
        }
        if (disconnect) {
            disconnectActor.switchOn();
        } else {
            disconnectActor.switchOff();
        }
    }

    private boolean getNewState(int batterySoc, int minimalSoc, int batteryReserve, int batteryPower) {
        int lowBuffer = (disconnectActor.isOn()) ? 3 : 4;
        int maxBuffer = (disconnectActor.isOn()) ? 3 : 4;

        if (batterySoc <= max(minimalSoc + batteryReserve + lowBuffer, 17)) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (batterySoc >= 100 - maxBuffer) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unused")
    public synchronized void stop() {
        log.trace("stopping");
        running = false;
        synchronized (lock) {
            lock.notify();
        }
    }
}
