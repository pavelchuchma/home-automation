package controller.external;

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
    static Logger log = Logger.getLogger(MpdRadio.class.getName());
    MPDSong radioStream;
    private MPD mpd;

    public MpdRadio() {
        try {
            mpd = new MPD("10.0.0.150");
        } catch (Exception e) {
            log.error("MPD init failed", e);
//            throw new RuntimeException(e);
        }
        radioStream = new MPDSong();
        radioStream.setFile("http://icecast8.play.cz/cro1-128.mp3");
    }

    public void start() {
        log.debug("starting stream: " + radioStream.getFile());
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
        MPDPlayer player = mpd.getMPDPlayer();
        try {
            player.stop();
            log.debug("  stopped");
        } catch (MPDConnectionException | MPDPlayerException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isPlaying() {
        MPDPlayer player = mpd.getMPDPlayer();
        try {
            MPDPlayer.PlayerStatus status = player.getStatus();
            log.debug("getting status -> " + status);
            return status == MPDPlayer.PlayerStatus.STATUS_PLAYING;
        } catch (MPDException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCurrentSong() {
        MPDPlayer player = mpd.getMPDPlayer();
        try {
            MPDSong currentSong = player.getCurrentSong();
            return (currentSong != null) ? currentSong.getFile() : "none";
        } catch (MPDException e) {
            throw new RuntimeException(e);
        }
    }
}
