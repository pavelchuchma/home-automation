package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.net.UnknownHostException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;
import org.chuma.homecontroller.extensions.external.utils.ModbusClient;

/**
 * Client for local Modbus API of "Solax X3-Hybrid G4 Inverter"
 */
public class SolaxInverterModbusClient {
    protected static Logger log = LoggerFactory.getLogger(SolaxInverterModbusClient.class.getName());
    private final ModbusClient client;

    public SolaxInverterModbusClient(String host) throws UnknownHostException {
        client = new ModbusClient(host, 502, true,
                new int[][]{{0x0003, 0x0053}, {0x006A, 0x009B} /*, {0x0114, 0x0115}*/},
                new int[][]{{0x007D, 0x0093}, {0x00B2, 0x00B2}},
                new int[][]{{0x0000, 0x0006}}
        );
    }

    public InverterState getState() {
        client.refreshData();
        return new State();
    }

    /**
     * Reads 2 registers wide int from the specified and following registers.
     */
    private static int getWideInt(ModbusClient.Registers registers, int lsbIndex) {
        return (int)(registers.getUnsignedInt(lsbIndex) + ((long)registers.getUnsignedInt(lsbIndex + 1) << 16));
    }

    public synchronized void setSelfUseMinimalSoc(int value) {
        log.debug("setSelfUseMinimalSoc({})", value);
        Validate.inclusiveBetween(10, 100, value);
        ensureSwitchedOnForConfigChange();
        client.writeSingleRegisterValue(0x0061, value);
    }

    public synchronized void setPgridBias(InverterState.PgridBias value) {
        log.debug("setPgridBias({})", value);
        ensureSwitchedOnForConfigChange();
        client.writeSingleRegisterValue(0x008D, value.ordinal());
    }

    private synchronized void ensureSwitchedOnForConfigChange() {
        final InverterState.Mode mode = getState().getMode();
        log.debug("ensureSwitchedOnForConfigChange - initial mode: {}", mode);
        if (mode == InverterState.Mode.Idle) {
            throw new IllegalStateException("Cannot configure inverter in IDLE state");
        }
    }

    public class State extends AbstractInverterState {
        @Override
        public String getVersion() {
            return "DSP: " + client.holding.getUnsignedInt(0x007F) + '.' + client.holding.getUnsignedInt(0x007D)
                    + "; ARM: " + client.holding.getUnsignedInt(0x0080) + '.' + client.holding.getUnsignedInt(0x0083)
                    + "; ARM-BL: " + client.holding.getUnsignedInt(0x0084)
                    + "; ModbusRTU: " + client.holding.getUnsignedInt(0x0082)
                    + "; HW: " + client.holding.getUnsignedInt(0x007E);
        }

        @Override
        public String getInverterSerialNumber() {
            return client.holding.getString(0x0000, 7);
        }

        @Override
        public String getWifiSerialNumber() {
            return "???";
        }

        @Override
        public Mode getMode() {
            return Mode.values()[client.input.getUnsignedInt(0x0009)];
        }

        @Override
        public BatteryMode getBatteryMode() {
            return BatteryMode.values()[client.input.getUnsignedInt(0x008B)];
        }

        @Override
        public double getGrid1Voltage() {
            return client.input.getUnsignedInt(0x006A) / 10d;
        }

        @Override
        public double getGrid2Voltage() {
            return client.input.getUnsignedInt(0x006E) / 10d;
        }

        @Override
        public double getGrid3Voltage() {
            return client.input.getUnsignedInt(0x0072) / 10d;
        }

        @Override
        public int getGrid1Power() {
            return client.input.getSignedInt(0x006C);
        }

        @Override
        public int getGrid2Power() {
            return client.input.getSignedInt(0x0070);
        }

        @Override
        public int getGrid3Power() {
            return client.input.getSignedInt(0x0074);
        }

        @Override
        public int getPv1Power() {
            return client.input.getUnsignedInt(0x000A);
        }

        @Override
        public int getPv2Power() {
            return client.input.getUnsignedInt(0x000B);
        }

        public int getEps1Power() {
            return client.input.getSignedInt(0x0078);
        }

        public int getEps2Power() {
            return client.input.getSignedInt(0x007C);
        }

        public int getEps3Power() {
            return client.input.getSignedInt(0x0080);
        }

        @Override
        public int getFeedInPower() {
            return getWideInt(client.input, 0x0046);
        }

        @Override
        public int getBatteryPower() {
            return client.input.getSignedInt(0x0016);
        }

        @Override
        public double getYieldTotal() {
            return getWideInt(client.input, 0x0052) / 10d;
        }

        @Override
        public double getYieldToday() {
            return client.input.getUnsignedInt(0x0050) / 10d;
        }

        @Override
        public double getFeedInEnergyTotal() {
            return getWideInt(client.input, 0x0048) / 100d;
        }

        @Override
        public double getFeedInEnergyToday() {
            return getWideInt(client.input, 0x0098) / 100d;
        }

        @Override
        public double getConsumedEnergyTotal() {
            return getWideInt(client.input, 0x004A) / 100d;
        }

        @Override
        public double getConsumedEnergyToday() {
            return getWideInt(client.input, 0x009A) / 100d;
        }

        @Override
        public int getBatterySoc() {
            return client.input.getUnsignedInt(0x001C);
        }

        @Override
        public int getBatteryTemp() {
            return client.input.getUnsignedInt(0x0018);
        }

        @Override
        public double getBatteryVoltage() {
            return client.input.getSignedInt(0x0014) / 10d;
        }

        @Override
        public int getSelfUseMinimalSoc() {
            return (client.holding.getUnsignedInt(0x0093) & 0xFF00) >> 8;
        }

        @Override
        public PgridBias getPgridBias() {
            return PgridBias.values()[client.holding.getUnsignedInt(0x00B2)];
        }
    }
}
