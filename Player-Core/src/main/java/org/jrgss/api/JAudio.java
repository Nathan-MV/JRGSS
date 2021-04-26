package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSLogger;
import static org.jrgss.JRGSSLogger.LogLevels.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by matty on 6/27/14.
 */
public class JAudio {

    public static final Object sync = new Object();

    static Music bgm;
    static String bgmFilename;
    static Music bgs;
    static Music me;
    static Sound se;
    static boolean mePlaying = false;

    static Map<String, Sound> se_cache = new HashMap<>();

    //========================================================
    // BGM
    public static void bgm_play(String filename) {
        bgm_play(filename, 100, 100, 0);
    }
    public static void bgm_play(String filename, int volume) {
        bgm_play(filename, volume, 100, 0);
    }
    public static void bgm_play(String filename, int volume, int pitch) {
        bgm_play(filename, volume, pitch, 0);
    }
    public synchronized static void bgm_play(String filename, int volume, int pitch, int pos) {
        JRGSSLogger.println(INFO,"Play BGM "+filename+" @ volume ="+ volume+", pitch ="+pitch+", pos = "+pos);
        if(volume == 0)
            JRGSSLogger.println(INFO,"BGM Requested Volume is 0 : "+filename);

        if(bgm != null && !bgmFilename.equals(filename)) {
            bgm_stop();
        }
        if(bgm == null) {
            bgm = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
            bgm.setLooping(true);
            bgmFilename = filename;
            if(!mePlaying) {
                synchronized (sync) {
                    bgm.play();
                }
            }
        }
        bgm.setVolume(Math.min(1.0f,volume/100f));

    }
    public static void bgm_stop() {
        if(bgm != null) {
            bgm.stop();
            bgm.dispose();
            bgm = null;
        }
    }
    public static void bgm_fade(int millis) {
        bgm_stop();
    }
    public static int bgm_pos() {
        return 0;
    }

    //========================================================
    // BGS
    public static void bgs_play(String filename) {
        bgs_play(filename, 100, 100, 0);
    }

    public static void bgs_play(String filename, int volume) {
        bgs_play(filename, volume, 100, 0);
    }

    public static void bgs_play(String filename, int volume, int pitch) {
        bgs_play(filename, volume, pitch, 0);
    }

    public synchronized static void bgs_play(String filename, int volume, int pitch, int pos) {
        JRGSSLogger.println(INFO,"Play BGS "+filename+" @ volume ="+ volume+", pitch ="+pitch+", pos = "+pos);
        if(volume == 0)
            JRGSSLogger.println(ERROR,"BGS Requested Volume is 0 : "+filename);

        if(bgs != null) {
            bgs_stop();
        }
        bgs = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
        bgs.setLooping(true);
        bgs.setVolume(volume/100f);
        if(!mePlaying) {
            synchronized (sync) {
                bgs.play();
            }
        }
    }
    public static int bgs_pos() {
        return 0;
    }
    public static void bgs_stop() {
        if(bgs != null) {
            bgs.stop();
            bgs.dispose();
            bgs = null;
        }
    }
    public static void bgs_fade(int millis) {
        bgs_stop();
    }

    //========================================================
    // ME
    public static void me_play(String filename) {
        me_play(filename, 100,100);
    }
    public static void me_play(String filename, int volume) {
        me_play(filename, volume, 100);
    }
    public synchronized static void me_play(String filename, int volume, int pitch) {
        JRGSSLogger.println(INFO,"Play ME "+filename+" @ volume ="+ volume+", pitch ="+pitch);
        if(volume == 0)
            JRGSSLogger.println(ERROR,"ME Requested Volume is 0 : "+filename);

        if(bgm != null) {
            bgm.pause();
        }
        me = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
        me.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                if(bgm != null) {
                    bgm.play();
                }
                mePlaying = false;
            }
        });
        me.setLooping(false);
        me.setVolume(volume/100f);
        synchronized (sync) {
            me.play();
        }
        mePlaying = true;
    }
    public static void me_stop() {
        if(me != null) {
            me.stop();
            me.dispose();
            me = null;
        }
    }
    public static void me_fade(int millis) {
        me_stop();
    }

    //========================================================
    public static void se_play(String filename) {
        se_play(filename, 100, 100);
    }
    public static void se_play(String filename, int volume) {
        se_play(filename, volume, 100);
    }
    public synchronized static void se_play(String filename, int volume, int pitch) {
        JRGSSLogger.println(INFO,"Play SE "+filename+" @ volume ="+ volume+", pitch ="+pitch);
        if(volume == 0)
            JRGSSLogger.println(ERROR,"SE Requested Volume is 0 : "+filename);

        Sound newSe = se_cache.get(filename);
        if(newSe == null) {
            newSe = Gdx.audio.newSound(FileUtil.loadAudio(filename));
            se_cache.put(filename, newSe);
        }
        se = newSe;
        synchronized (sync) {
            se.play(volume / 100f, pitch / 100f, 0);
        }

    }
    public static void se_stop() {
        if(se != null)
            se.stop();
    }

}
