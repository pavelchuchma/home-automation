package org.chuma.homecontroller.extensions.external;

import org.apache.log4j.Logger;
import org.bff.javampd.MPD;
import org.bff.javampd.Player;
import org.bff.javampd.Playlist;
import org.bff.javampd.exception.MPDException;
import org.bff.javampd.exception.MPDPlayerException;
import org.bff.javampd.exception.MPDPlaylistException;
import org.bff.javampd.objects.MPDSong;

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
                mpdInstance = (new MPD.Builder()).server(mpdServerAddress).build();
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
        Player player = mpd.getPlayer();
        try {
            player.stop();
            Playlist playlist = mpd.getPlaylist();
            playlist.clearPlaylist();
            playlist.addSong(radioStream);
            player.play();
            log.debug("  started");
        } catch (MPDPlayerException | MPDPlaylistException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        log.debug("stopping stream");
        Player player = getMpd().getPlayer();
        try {
            player.stop();
            log.debug("  stopped");
        } catch (MPDPlayerException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isPlaying() {
        Player player = getMpd().getPlayer();
        try {
            Player.Status status = player.getStatus();
            log.debug("getting status -> " + status);
            return status == Player.Status.STATUS_PLAYING;
        } catch (MPDException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCurrentSong() {
        Player player = getMpd().getPlayer();
        try {
            MPDSong currentSong = player.getCurrentSong();
            return (currentSong != null) ? currentSong.getFile() : "none";
        } catch (MPDException e) {
            throw new RuntimeException(e);
        }
    }
}
