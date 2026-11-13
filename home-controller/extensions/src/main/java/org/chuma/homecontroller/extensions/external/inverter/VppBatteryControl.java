package org.chuma.homecontroller.extensions.external.inverter;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;

/**
 * Runs the temporary battery modes through the inverter's VPP interface and remembers which feature holds the
 * battery. The stored work mode is not touched. A command lasts {@link #DURATION_S} and the owner re-issues it
 * every minute, so the inverter returns to its stored work mode by itself when the application stops.
 * See doc/inverter/solax_vpp_protocol.md.
 */
class VppBatteryControl {
    enum Owner {
        NONE,
        FORCE_DISCHARGE
    }

    static final int DURATION_S = 180;
    /**
     * PV target above any PV power, so PV is not limited.
     */
    static final int PV_UNLIMITED_W = 30_000;

    private static final Logger log = LoggerFactory.getLogger(VppBatteryControl.class.getName());
    private final SolaxInverterModbusClient client;
    private final boolean exportAllowed;
    private volatile Owner owner = Owner.NONE;
    private final Map<Owner, String> refusals = new EnumMap<>(Owner.class);

    /**
     * @param exportAllowed false refuses every mode that could feed the grid
     */
    VppBatteryControl(SolaxInverterModbusClient client, boolean exportAllowed) {
        this.client = client;
        this.exportAllowed = exportAllowed;
    }

    Owner getOwner() {
        return owner;
    }

    /**
     * Why the owner's mode is wanted and not running; null when it runs or is not wanted.
     */
    synchronized String getRefusal(Owner wantedBy) {
        return refusals.get(wantedBy);
    }

    /**
     * The first owner whose mode is wanted and not running; null when there is none.
     */
    synchronized InverterManager.BatteryRefusal getBatteryRefusal() {
        return refusals.entrySet().stream().findFirst()
                .map(e -> new InverterManager.BatteryRefusal(e.getKey().name(), e.getValue())).orElse(null);
    }

    /**
     * Starts the battery mode for the owner, or refreshes it when the owner already holds the battery.
     *
     * @param batteryW battery power in W, positive discharges, negative charges
     * @return false when nothing was sent: the inverter cannot take a VPP mode now, or another owner holds
     * the battery. A mode the owner had running is ended then.
     */
    synchronized boolean hold(Owner newOwner, InverterState state, int pvLimitW, int batteryW) {
        String refusal = (!exportAllowed && canExport(pvLimitW, batteryW))
                ? "export is not allowed in this deployment"
                : (owner != Owner.NONE && owner != newOwner)
                ? owner + " holds the battery"
                : getRefusal(client.isVppSupported(), state.getMode(), state.getBatteryMode());
        if (refusal != null) {
            // logged once, until the reason changes or the owner stops wanting the battery
            if (!refusal.equals(refusals.put(newOwner, refusal))) {
                log.warn("{} is wanted but cannot run: {} (SoC {} %, stored min SoC {} %)", newOwner, refusal,
                        state.getBatterySoc(), state.getSelfUseMinimalSoc());
            }
            end(newOwner);
            return false;
        }
        refusals.remove(newOwner);
        if (owner != newOwner) {
            log.info("{}: starting, PV limit {} W, battery {} W", newOwner, pvLimitW, batteryW);
        }
        client.vppPvBattery(pvLimitW, batteryW, DURATION_S);
        owner = newOwner;
        return true;
    }

    /**
     * The owner no longer wants the battery: ends its VPP mode when it runs.
     *
     * @return whether it held the battery
     */
    synchronized boolean release(Owner oldOwner) {
        refusals.remove(oldOwner);
        return end(oldOwner);
    }

    private boolean end(Owner oldOwner) {
        if (owner != oldOwner || owner == Owner.NONE) {
            return false;
        }
        client.vppDisable();
        log.info("{}: released", oldOwner);
        owner = Owner.NONE;
        return true;
    }

    /**
     * Whether the mode can feed the grid: a discharging battery, or any PV, because the mode sends what the
     * house does not take to the grid and the inverter's export limit is not known to apply to it.
     */
    static boolean canExport(int pvLimitW, int batteryW) {
        return pvLimitW > 0 || batteryW > 0;
    }

    /**
     * Why a VPP mode cannot be started now; null when it can.
     */
    static String getRefusal(boolean vppSupported, InverterState.Mode runMode, InverterState.BatteryMode batteryMode) {
        if (!vppSupported) {
            return "the inverter firmware has no VPP";
        }
        if (batteryMode == InverterState.BatteryMode.Manual) {
            return "the stored work mode is Manual";
        }
        if (!runMode.isRunning()) {
            return "the inverter is " + runMode;
        }
        return null;
    }
}
