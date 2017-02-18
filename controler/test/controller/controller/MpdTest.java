package controller.controller;


import org.bff.javampd.MPD;
import org.bff.javampd.MPDPlayer;
import org.bff.javampd.MPDPlaylist;
import org.bff.javampd.MPDSong;
import org.junit.Test;

public class MpdTest {
    @Test
    public void testMpd() throws Exception {
        MPD mpd = new MPD("pi");
//        String version = mpd.getVersion();
        MPDPlayer player = mpd.getMPDPlayer();
        MPDSong currentSong = player.getCurrentSong();
        MPDSong s = new MPDSong();
        s.setFile("http://icecast8.play.cz/cro1-128.mp3");
        player.stop();

//        player.playId(s);
        MPDPlaylist playlist = mpd.getMPDPlaylist();
        playlist.clearPlaylist();
        playlist.addSong(s);
        player.play();
    }
}