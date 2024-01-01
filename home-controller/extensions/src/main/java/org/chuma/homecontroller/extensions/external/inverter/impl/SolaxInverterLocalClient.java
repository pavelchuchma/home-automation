package org.chuma.homecontroller.extensions.external.inverter.impl;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

/**
 * Client for local API of "Solax Pocket wi-fi V3.0" connected to "Solax X3-Hybrid G4 Inverter"
 */
public class SolaxInverterLocalClient {
    private static final Logger log = LoggerFactory.getLogger(SolaxInverterLocalClient.class.getName());
    private final String url;
    private final String password;

    public SolaxInverterLocalClient(String localIp, String password) {
        this.url = "http://" + localIp;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public InverterState getState() {
        long startTime = 0;
        if (log.isTraceEnabled()) {
            startTime = System.currentTimeMillis();
        }

        HttpJsonClient client = new HttpJsonClient(url, 10);
        JsonObject response = client.doPost("", "optType=ReadRealTimeData&pwd=" + password, null);
        log.trace("Refresh done in {} ms", System.currentTimeMillis() - startTime);
        return new SolaxInverterState(response);
    }
}
