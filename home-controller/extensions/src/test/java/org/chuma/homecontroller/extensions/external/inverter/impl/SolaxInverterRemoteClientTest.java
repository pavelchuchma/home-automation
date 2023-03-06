package org.chuma.homecontroller.extensions.external.inverter.impl;

import java.text.MessageFormat;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class SolaxInverterRemoteClientTest extends SolaxInverterTestBase {
    @Test
    public void basic() {
        String inverterSN = "H34A15I7587435";
        HttpJsonClient client = new HttpJsonClient("https://www.solaxcloud.com", 10);
//        HttpJsonClient client = new HttpJsonClient("http://localhost:6070", 10);

        JsonObject loginResponse = client.doPost("/phoebus/login/loginNew", "username=chuma%40seznam.cz&userpwd=62377fcf13fcede16e63bde7d783b118", null);
        String token = loginResponse.get("token").toString();
        String userId = ((JsonObject)loginResponse.get("user")).get("id").toString();
        String userRelId = ((JsonObject)loginResponse.get("user")).get("relIds").toString();
        JsonArray getUserAllSiteIdResponse = client.doPost("/phoebus/userIndex/getUserAllSiteId", "userId=" + userId, token);
        String siteId = getUserAllSiteIdResponse.get(0).toString();
        JsonObject getSiteInfoResponse = client.doPost("/phoebus/userIndex/getSiteInfo", "siteId=" + siteId, token);
        JsonObject getSiteByIdResponse = client.doPost("/phoebus/site/getById", "id=" + siteId, token);

        HttpJsonClient configClient = new HttpJsonClient("https://abroad.solaxcloud.com/proxy/", 10);
        JsonObject configLoginResponse = configClient.doPost("/login/remoteLogin.do", String.format("sn=%s&userType=5&userId=%s&firmId=%s", inverterSN, userId, userRelId), null);
        String cfgToken = ((JsonObject)configLoginResponse.get("result")).get("tokenId").toString();
        String wifiSN = ((JsonObject)configLoginResponse.get("result")).get("wifiSN").toString();

        //login
        JsonObject paramSetResponse = configClient.doPost("/settingnew/paramSet",
                MessageFormat.format("optType=setReg&sn={0}&inverterSn={1}&tokenId={2}&num=1&deviceType=1&Data=%5B%7B%22reg%22%3A{3}%2C%22val%22%3A%22{4}%22%7D%5D", wifiSN, inverterSN, cfgToken, 0, "0000"), null);

        JsonObject paramInitResponse = configClient.doPost("/settingnew/paramInit",
                MessageFormat.format("sn={0}&inverterSn={1}&tokenId={2}&num=1&deviceType=1", wifiSN, inverterSN, cfgToken), null);

        // turn on
//        JsonObject turnOnResponse = configClient.doPost("/settingnew/paramSet",
//                MessageFormat.format("optType=setReg&sn={0}&inverterSn={1}&tokenId={2}&num=1&deviceType=1&Data=%5B%7B%22reg%22%3A{3}%2C%22val%22%3A{4}%7D%5D", wifiSN, inverterSN, cfgToken, 47, 1), null);

        JsonObject paramSetResponse2 = configClient.doPost("/settingnew/paramSet",
                MessageFormat.format("optType=setReg&sn={0}&inverterSn={1}&tokenId={2}&num=1&deviceType=1&Data=%5B%7B%22reg%22%3A{3}%2C%22val%22%3A{4}%7D%5D", wifiSN, inverterSN, cfgToken, 29, 20), null);

    }

    @Test
    public void testClient() {
        final SolaxInverterRemoteClient client = new SolaxInverterRemoteClient(localUrl, localPassword, remoteUsername, remotePasswordToken, basicConfigPin);

        int targetValue = 30;
        client.setSelfUseMinimalSoc(targetValue);

        RemoteConfiguration configuration = client.getConfiguration();
        Assert.assertEquals(targetValue, configuration.getSelfUseMinimalSoc());
    }
}
