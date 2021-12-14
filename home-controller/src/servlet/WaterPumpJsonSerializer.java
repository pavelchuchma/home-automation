package servlet;

import controller.action.WaterPumpMonitor;

public class WaterPumpJsonSerializer extends AbstractJsonSerializer {
    public static String serialize(WaterPumpMonitor mon, int recordCount, double lastHours) {
        StringBuffer b = new StringBuffer("{");
        appendNameValue(b, "id", "wpmp");
        b.append(',');
        appendNameValue(b, "on", mon.isOn());
        b.append(',');
        appendNameValue(b, "recCount", mon.getRecordCount());
        b.append(',');
        appendNameValue(b, "lastPeriodRecCount", mon.getRecordCountInLastHours(lastHours));
        b.append(',');
        WaterPumpMonitor.Record[] lastRecords = mon.getLastRecords(recordCount);
        b.append("\"lastRecords\":[");
        for (int i = 0; i < lastRecords.length; i++) {
            b.append('{');
            appendNameValue(b, "time", lastRecords[i].time.toString());
            b.append(',');
            appendNameValue(b, "duration", lastRecords[i].durationMs);
            b.append((i < lastRecords.length - 1) ? "}," : "}");
        }
        b.append("]}");

        return b.toString();
    }
}
