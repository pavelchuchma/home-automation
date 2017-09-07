package controller.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

public class Recuperation {
    private static final String baseUrl = "http://10.0.0.151";
    /*
    {bit:1, name:"#bPower"},
    {bit:2, name:"#bBypass"},
    {bit:3, name:"#bAway"},
    {bit:4, name:"#bBoost"},
    {bit:5, name:"#bHeat"},
    {bit:6, name:"#bCool"},
    {bit:8, name:"#aSpP"},
    {bit:9, name:"#aSpM"},
    {bit:10, name:"#aHpP"},
    {bit:11, name:"#aHpM"},
    {bit:12, name:"#aTpP"},
    {bit:13, name:"#aTpM"} ];
     */
    private static final int BTN_NONE = 0;
    private static final int BTN_POWER = 1;
    private static final int BTN_SPEED_UP = 8;
    private static final int BTN_SPEED_DOWN = 9;
    private static final int statusTimeoutMs = 1000;
    private static Logger log = Logger.getLogger(Recuperation.class.getName());
    private Status status;
    private long lastUpdateTime;

    public Recuperation() {
        refreshStatus();
    }

    private static String fixJsonSyntax(String response) {
        response = response.substring("callback(".length());
        response = response.replace('\'', '"');
        response = response.replace("msg:", "\"msg\":");
        response = response.replace("value:", "\"value\":");
        response = response.substring(0, response.length() - 2);
        return response;
    }

    private void refreshStatus() {
        callServer(BTN_NONE);
    }

    public Status getStatus() {
        refreshStatus();
        return status;
    }

    public boolean setSpeed(int targetSpeed) {
        log.debug("Setting speed to " + targetSpeed);
        Validate.inclusiveBetween(0, 4, targetSpeed);
        refreshStatus();
        int currentSpeed = -1;

        for (int i = 0; i < 10; i++) {
            currentSpeed = status.speed;
            if (targetSpeed == currentSpeed) {
                return true;
            }
            if (status.mode == Mode.UNKNOWN || status.mode == Mode.BOOST || status.mode == Mode.AWAY || currentSpeed == 0 || targetSpeed == 0) {
                callServer(BTN_POWER);
            } else if (targetSpeed > currentSpeed) {
                callServer(BTN_SPEED_UP);
            } else if (targetSpeed < currentSpeed) {
                callServer(BTN_SPEED_DOWN);
            }
        }
        return targetSpeed == currentSpeed;
    }

    private synchronized boolean callServer(int commandId) {
        log.debug("Calling server with command: " + commandId);
        long now = System.currentTimeMillis();
        if (commandId == BTN_NONE && now - lastUpdateTime < statusTimeoutMs) {
            return true;
        }
        lastUpdateTime = now;
        try {
            String response = doHttpGet(commandId);
            status = new Status(response);
            return true;
        } catch (IOException e) {
            log.error("Failed to get recuperation status", e);
            status = new Status();
            return false;
        }
    }

    private String doHttpGet(int commandId) throws IOException {
        String url = (commandId != BTN_NONE) ? baseUrl + "/values.cgi?&button=" + commandId : baseUrl + "/values.cgi";
        HttpURLConnection con = new sun.net.www.protocol.http.HttpURLConnection(new URL(url), null);
        con.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return fixJsonSyntax(response.toString());
        }
    }

    public enum Mode {
        OFF,
        RUNNING,
        AWAY,
        BOOST,
        UNKNOWN
    }

    public static class Status {
        private Mode mode;
        private int speed;
        private float externalTemperature;

        private Status(String data) {
            String s = data;
            s = s.substring(1, s.length() - 2);
            Map<String, String> map = new HashMap<>();
            for (String item : s.split(",")) {
                String[] pair = item.replaceAll("\"", "").split(":", 2);
                if (pair.length == 2) {
                    map.put(pair[0], pair[1]);
                }
            }

            speed = Integer.parseInt(map.get("sp"));
            mode = parseMode(map.get("md"));
            externalTemperature = Float.parseFloat(map.get("ta"));
        }

        private Status() {
            mode = Mode.UNKNOWN;
            speed = -1;
            externalTemperature = -100;
        }

        private static Mode parseMode(String md) {
            switch (Integer.parseInt(md)) {
                case 0:
                    return Mode.OFF;
                case 1:
                    return Mode.RUNNING;
                case 5:
                    return Mode.AWAY;
                case 9:
                    return Mode.BOOST;
                default:
                    return Mode.UNKNOWN;
            }
        }

        public Mode getMode() {
            return mode;
        }

        public int getSpeed() {
            return speed;
        }

        public float getExternalTemperature() {
            return externalTemperature;
        }
    }
}
