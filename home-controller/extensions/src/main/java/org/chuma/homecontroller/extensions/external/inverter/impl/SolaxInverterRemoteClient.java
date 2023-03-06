package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.chuma.homecontroller.extensions.external.inverter.impl.RemoteConfiguration.SELF_USE_MIN_SOC;

import org.chuma.homecontroller.extensions.external.inverter.InverterState;

public class SolaxInverterRemoteClient {
    private static final int MAX_READ_TOKEN_AGE_MS = 15 * 60_000;
    private static final int MAX_CONFIG_TOKEN_AGE_MS = 15 * 60_000;
    protected static Logger log = LoggerFactory.getLogger(SolaxInverterRemoteClient.class.getName());
    private final SolaxInverterLocalClient localClient;
    private final String remoteUsername;
    private final String remotePasswordToken;
    private final String basicConfigPin;
    private final HttpJsonClient configClient;
    private String inverterSerialNumber;
    private String wifiSerialNumber;
    private String userId;
    private String userRelId;
    private String readToken;
    private long readTokenTimestamp;
    private String configToken;
    private long configTokenTimestamp;

    public SolaxInverterRemoteClient(String localUrl, String localPassword, String remoteUsername, String remotePasswordToken, String basicConfigPin) {
        this.remoteUsername = remoteUsername;
        this.remotePasswordToken = remotePasswordToken;
        this.basicConfigPin = basicConfigPin;
        localClient = new SolaxInverterLocalClient(localUrl, localPassword);
        configClient = new HttpJsonClient("https://abroad.solaxcloud.com/proxy/", 10);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private synchronized void initialize() {
        if (!isReadTokenValid()) {
            initializeSerialNumbers();

            HttpJsonClient client = new HttpJsonClient("https://www.solaxcloud.com/phoebus/", 10);
            String payload = MessageFormat.format("username={0}&userpwd={1}", urlEncode(remoteUsername), remotePasswordToken);
            JsonObject loginResponse = client.doPostAndVerify("login/loginNew", payload);
            userId = ((JsonObject)loginResponse.get("user")).get("id").toString();
            userRelId = ((JsonObject)loginResponse.get("user")).get("relIds").toString();
            readToken = loginResponse.get("token").toString();
            readTokenTimestamp = System.currentTimeMillis();
        }
    }

    private void initializeSerialNumbers() {
        if (wifiSerialNumber == null) {
            final InverterState state = localClient.getState();
            inverterSerialNumber = state.getInverterSerialNumber();
            wifiSerialNumber = state.getWifiSerialNumber();
        }
    }

    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public RemoteConfiguration getConfiguration() {
        configLogin();
        JsonObject response = configClient.doPostAndVerify("/settingnew/paramInit", buildCommonParams());
        return new RemoteConfiguration((JsonArray)response.get("result"));
    }

    private String buildCommonParams() {
        return MessageFormat.format("sn={0}&inverterSn={1}&tokenId={2}&num=1&deviceType=1",
                wifiSerialNumber, inverterSerialNumber, configToken);
    }

    public void setConfigValue(int index, int value) {
        log.debug("setConfigValue(#{}={})", index, value);
        configLogin();
        ensureSwitchedOnForConfigChange();
        callSetParamImpl(index, String.valueOf(value));
    }

    public void setSelfUseMinimalSoc(int value) {
        Validate.inclusiveBetween(10, 100, value);
        setConfigValue(SELF_USE_MIN_SOC, value);
    }

    private synchronized void ensureSwitchedOnForConfigChange() {
        final InverterState.Mode initialMode = localClient.getState().getMode();
        log.debug("ensureSwitchedOnForConfigChange - initial mode: {}", initialMode);
        if (initialMode != InverterState.Mode.Idle) {
            return;
        }

        log.debug("  inverter is in Idle mode, switching it ON");
        callSetParamImpl(RemoteConfiguration.SYSTEM_ON, String.valueOf(1));

        for (int i = 0; i < 15; i++) {
            final InverterState.Mode mode = localClient.getState().getMode();
            log.debug("  current mode: {}", mode);
            if (mode != InverterState.Mode.Idle) {
                log.debug("     exiting");
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("Failed to switch inverted ON for config change");
    }

    @SuppressWarnings("SpellCheckingInspection")
    private synchronized void configLogin() {
        initialize();
        if (!isConfigTokenValid()) {
            log.debug("configLogin()");
            // invalidate timestamp before successfull login
            configTokenTimestamp = -1;

            JsonObject configLoginResponse = configClient.doPostAndVerify("/login/remoteLogin.do",
                    String.format("sn=%s&userType=5&userId=%s&firmId=%s", inverterSerialNumber, userId, userRelId));

            configToken = ((JsonObject)configLoginResponse.get("result")).get("tokenId").toString();
            callSetParamImpl(0, "'" + basicConfigPin + "'");
            configTokenTimestamp = System.currentTimeMillis();
            log.debug("configLogin() done");
        }
    }

    private void callSetParamImpl(int index, String value) {
        String data = urlEncode("[{'reg':" + index + ",'val':" + value + "}]");
        configClient.doPostAndVerify("/settingnew/paramSet",
                buildCommonParams() + "&optType=setReg&Data=" + data);
    }

    private boolean isConfigTokenValid() {
        return configToken != null && System.currentTimeMillis() - configTokenTimestamp < MAX_CONFIG_TOKEN_AGE_MS;
    }

    private boolean isReadTokenValid() {
        return readToken != null && System.currentTimeMillis() - readTokenTimestamp < MAX_READ_TOKEN_AGE_MS;
    }
}
