package controller.external;

import org.junit.Assert;
import org.junit.Test;

public class RecuperationTest {
    @Test
    public void testInit() throws Exception {
        Recuperation recu = new Recuperation();


        setSpeedAndValidate(recu, 0);
        Assert.assertEquals(Recuperation.Mode.OFF, recu.getStatus().getMode());
        setSpeedAndValidate(recu, 2);
        Assert.assertEquals(Recuperation.Mode.RUNNING, recu.getStatus().getMode());
        setSpeedAndValidate(recu, 3);
        setSpeedAndValidate(recu, 2);
        Assert.assertEquals(Recuperation.Mode.RUNNING, recu.getStatus().getMode());
        setSpeedAndValidate(recu, 0);
        Assert.assertEquals(Recuperation.Mode.OFF, recu.getStatus().getMode());
        setSpeedAndValidate(recu, 4);
        setSpeedAndValidate(recu, 2);
        Assert.assertEquals(Recuperation.Mode.RUNNING, recu.getStatus().getMode());

        Assert.assertTrue(recu.getStatus().getExternalTemperature() > -20);
        Assert.assertTrue(recu.getStatus().getExternalTemperature() < 25);
    }

    private void setSpeedAndValidate(Recuperation recu, int targetSpeed) {
        Assert.assertTrue(recu.setSpeed(targetSpeed));
        Assert.assertEquals(targetSpeed, recu.getStatus().getSpeed());
    }
}