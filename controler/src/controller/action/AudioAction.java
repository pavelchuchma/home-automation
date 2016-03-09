package controller.action;

import controller.actor.Actor;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class AudioAction implements Action {
    static Logger log = Logger.getLogger(AudioAction.class.getName());

    public AudioAction() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        log.info("There are " + mixers.length + " mixer info objects");
        for (Mixer.Info mixerInfo : mixers) {
            log.info("mixer name: " + mixerInfo.getName());
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lineInfos = mixer.getSourceLineInfo();
            for (Line.Info lineInfo : lineInfos) {
                log.info("  Line.Info: " + lineInfo);
                try {
                    Line line = mixer.getLine(lineInfo);
                    line.open();
                    try {
                        Control[] controls = line.getControls();
                        log.info("    Controls: " + Arrays.toString(controls));
                        FloatControl volCtrl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                        log.info("    volCtrl.getValue() = " + volCtrl.getValue());
                    } finally {
                        line.close();
                    }
                } catch (Exception e) {
                    log.error("Faild to get line: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void perform(int previousDurationMs) {
        try {
            playSound(true);
//            playSound(false);
        } catch (Exception e) {
            log.error("failed", e);
        }
    }

    private void playSound(boolean raiseVolume) throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
        log.info("Playing...");
        InputStream in = this.getClass().getResourceAsStream("/resources/PozorNekdoJde.wav");
        log.info("1");
        BufferedInputStream stream = new BufferedInputStream(in);
        log.info("2");
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(stream);
        log.info("3");
        Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        if (raiseVolume) {
            gainControl.setValue(gainControl.getMaximum());
        }
        clip.start();
        Thread.sleep(clip.getMicrosecondLength() / 1000);
        clip.close();
    }

    @Override
    public Actor getActor() {
        return null;
    }
}