package org.chuma.homecontroller.extensions.external;

import junit.framework.TestCase;
import org.junit.Assert;

public class MpdRadioTest extends TestCase {
    public void testPlay() {
        MpdRadio radio = new MpdRadio("192.168.68.150", "http://rozhlas.stream/radiozurnal_mp3_128.mp3");
//        MpdRadio radio = new MpdRadio("192.168.68.150", "https://rozhlas.stream/radiozurnal_mp3_128.mp3");
        radio.start();
        Assert.assertTrue(radio.isPlaying());
        radio.stop();
        Assert.assertFalse(radio.isPlaying());
    }
}