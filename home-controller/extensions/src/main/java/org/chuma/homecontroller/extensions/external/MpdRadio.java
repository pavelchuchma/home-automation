package org.chuma.homecontroller.extensions.external;

import java.util.Optional;

import org.bff.javampd.player.Player;
import org.bff.javampd.playlist.MPDPlaylistSong;
import org.bff.javampd.playlist.Playlist;
import org.bff.javampd.server.MPD;
import org.bff.javampd.song.MPDSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpdRadio {
    static Logger log = LoggerFactory.getLogger(MpdRadio.class.getName());
    public final String mpdServerAddress;
    MPDSong radioStream;
    private MPD mpdInstance;

    public MpdRadio(String mpdServerAddress, String file) {

        radioStream = MPDSong.builder().file(file).build();
        this.mpdServerAddress = mpdServerAddress;
    }

    private MPD getMpd() {
        if (mpdInstance == null || !mpdInstance.isConnected()) {
            try {
                log.debug("Initializing MPD instance");
                mpdInstance = MPD.builder().server(mpdServerAddress).build();
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

        player.stop();
        Playlist playlist = mpd.getPlaylist();
        playlist.clearPlaylist();
        playlist.addSong(radioStream);
        player.play();
        log.debug("  started");
    }

    public void stop() {
        log.debug("stopping stream");
        Player player = getMpd().getPlayer();
        player.stop();
        log.debug("  stopped");
    }

    public boolean isPlaying() {
        Player player = getMpd().getPlayer();
        player.getStatus();
        Player.Status status = player.getStatus();
        log.debug("getting status -> " + status);
        return status == Player.Status.STATUS_PLAYING;
    }

    public String getCurrentSong() {
        Player player = getMpd().getPlayer();
        Optional<MPDPlaylistSong> currentSong = player.getCurrentSong();
        return (currentSong.isPresent()) ? currentSong.get().getFile() : "none";
    }
}
