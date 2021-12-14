package org.chuma.homecontroller.controller.external;

import org.apache.log4j.Logger;
import org.bff.javampd.MPD;
import org.bff.javampd.MPDPlayer;
import org.bff.javampd.MPDPlaylist;
import org.bff.javampd.MPDSong;
import org.bff.javampd.exception.MPDConnectionException;
import org.bff.javampd.exception.MPDException;
import org.bff.javampd.exception.MPDPlayerException;
import org.bff.javampd.exception.MPDPlaylistException;

public class MpdRadio {
    public static final String mpdServerAddress = "192.168.68.150";
    static Logger log = Logger.getLogger(MpdRadio.class.getName());
    MPDSong radioStream;
    private MPD mpdInstance;

    public MpdRadio() {
        radioStream = new MPDSong();
        radioStream.setFile("http://icecast8.play.cz/cro1-128.mp3");
    }

    private MPD getMpd() {
        if (mpdInstance == null || !mpdInstance.isConnected()) {
            try {
                log.debug("Initializing MPD instance");
                mpdInstance = new MPD(mpdServerAddress);
            } catch (Exception e) {
                log.error("MPD init failed", e);
                throw new RuntimeException(e);
            }
        }
        return mpdInstance;
    }

    public void start() {
        log.debug("starting stream: " + radioStream.getFile());
        MPD mpd = getMpd();
        MPDPlayer player = mpd.getMPDPlayer();
        try {
            player.stop();
            MPDPlaylist playlist = mpd.getMPDPlaylist();
            playlist.clearPlaylist();
            playlist.addSong(radioStream);
            player.play();
            log.debug("  started");
        } catch (MPDConnectionException | MPDPlayerException | MPDPlaylistException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        log.debug("stopping stream");
        MPDPlayer player = getMpd().getMPDPlayer();
        try {
            player.stop();
            log.debug("  stopped");
        } catch (MPDConnectionException | MPDPlayerException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isPlaying() {
        MPDPlayer player = getMpd().getMPDPlayer();
        try {
            MPDPlayer.PlayerStatus status = player.getStatus();
            log.debug("getting status -> " + status);
            return status == MPDPlayer.PlayerStatus.STATUS_PLAYING;
        } catch (MPDException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCurrentSong() {
        MPDPlayer player = getMpd().getMPDPlayer();
        try {
            MPDSong currentSong = player.getCurrentSong();
            return (currentSong != null) ? currentSong.getFile() : "none";
        } catch (MPDException e) {
            throw new RuntimeException(e);
        }
    }
}
