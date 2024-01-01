package org.chuma.homecontroller.extensions.external.inverter.impl;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

public class SolaxInverterLocalClientTest extends AbstractSolaxInverterTestBase {
    public void testGetState() {
        SolaxInverterLocalClient s = new SolaxInverterLocalClient(localIp, localPassword);

        final InverterState state = s.getState();

        int pv = state.getPv1Power();
        int gridPower = state.getGrid1Power() + state.getGrid2Power() + state.getGrid3Power();
        int epsPower = state.getEps1Power() + state.getEps2Power() + state.getEps3Power();
        int battery = state.getBatteryPower();
        int feedIn = state.getFeedInPower();

        System.out.println("SN: " + state.getInverterSerialNumber());
        System.out.println("Mode: " + state.getMode());
        System.out.println("Battery Mode: " + state.getBatteryMode());
        System.out.println("PV: " + pv + " W");
        System.out.println("AC Power: " + gridPower + " W");
        System.out.println("EPS Power: " + epsPower + " W");
        System.out.println("FeedIn: " + feedIn + " W");
        System.out.println("Battery: " + battery + " W");
        System.out.println("Battery SOC: " + state.getBatterySoc() + "%");
        System.out.println();
        System.out.println("Yield today: " + state.getYieldToday() + " W");
        System.out.println("Consumed today: " + state.getConsumedEnergyToday() + " kWh");
        System.out.println("FeedIn today: " + state.getFeedInEnergyToday() + " kWh");
        System.out.println();
        int load = gridPower + epsPower - feedIn;
        int diff = pv - (battery + gridPower + epsPower);
        System.out.println("Load: " + load + " W");
        System.out.println("Diff: " + diff + " W");
    }
}
