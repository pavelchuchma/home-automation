package org.chuma.homecontroller.extensions.external.inverter.impl;

import com.github.cliftonlabs.json_simple.JsonObject;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

/**
 * Client for local API of "Solax Pocket wi-fi V3.0" connected to "Solax X3-Hybrid G4 Inverter"
 */
public class SolaxInverterLocalClient {
    private final String url;
    private final String password;

    public SolaxInverterLocalClient(String url, String password) {
        this.url = url;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public InverterState getState() {
        HttpJsonClient client = new HttpJsonClient(url, 10);
        JsonObject response = client.doPost("", "optType=ReadRealTimeData&pwd=" + password, null);
        return new SolaxInverterState(response);
    }
}
