package org.chuma.homecontroller.app.servlet.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;
import org.chuma.homecontroller.extensions.external.inverter.impl.SolaxInverterModbusClient;

public class InverterHandlerTest {
    /**
     * Records the setter calls instead of writing them, nothing connects to the inverter.
     */
    private static class RecordingClient extends SolaxInverterModbusClient {
        final List<InverterState.PgridBias> pgridBias = new ArrayList<>();

        RecordingClient() throws Exception {
            super("127.0.0.1");
        }

        @Override
        public synchronized void setPgridBias(InverterState.PgridBias value) {
            pgridBias.add(value);
        }
    }

    private static Map<String, String[]> params(String... keyValues) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], new String[]{keyValues[i + 1]});
        }
        return map;
    }

    @Test
    public void pgridBiasOn() throws Exception {
        RecordingClient client = new RecordingClient();

        InverterHandler.applyAction(client, params("id", "inverter", "pgridBias", "true"));

        Assert.assertEquals(List.of(InverterState.PgridBias.Grid), client.pgridBias);
    }

    @Test
    public void pgridBiasOff() throws Exception {
        RecordingClient client = new RecordingClient();

        InverterHandler.applyAction(client, params("id", "inverter", "pgridBias", "false"));

        Assert.assertEquals(List.of(InverterState.PgridBias.Disable), client.pgridBias);
    }

    @Test
    public void nothingWrittenWithoutParam() throws Exception {
        RecordingClient client = new RecordingClient();

        InverterHandler.applyAction(client, params("id", "inverter"));

        Assert.assertTrue(client.pgridBias.isEmpty());
    }
}
