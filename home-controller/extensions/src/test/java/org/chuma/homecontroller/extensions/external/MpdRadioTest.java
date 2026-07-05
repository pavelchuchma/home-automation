package org.chuma.homecontroller.extensions.external;

import junit.framework.TestCase;
import org.junit.Assert;

public class MpdRadioTest extends TestCase {
    public void testPlay() throws InterruptedException {
        MpdRadio radio = new MpdRadio("pi.local", "https://rozhlas.stream/radiozurnal_mp3_128.mp3");
        radio.start();
        Assert.assertTrue(radio.isPlaying());
        // let the radio actually play so the sound is audible
        Thread.sleep(5_000);
        radio.stop();
        // default expiryInterval is set to 5s
        Thread.sleep(7_000);
        Assert.assertFalse(radio.isPlaying());
    }
}