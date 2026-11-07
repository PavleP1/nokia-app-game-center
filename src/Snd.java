import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import java.io.ByteArrayInputStream;

final class Snd implements Runnable {
    static final int TICK = 0, OPEN = 1, BACK = 2, BLIP = 3, BOUNCE = 4, LINE = 5, LOSE = 6, WIN = 7, LOCK = 8;
    // pairs of (midi note, milliseconds)
    static final int[][] PAT = {
        {84, 25},
        {72, 50, 79, 60},
        {79, 45, 72, 60},
        {88, 35},
        {76, 25},
        {72, 50, 76, 50, 79, 50, 84, 90},
        {67, 130, 62, 130, 55, 260},
        {72, 80, 76, 80, 79, 80, 84, 220},
        {60, 30}
    };
    static final String[] TYPES = {"audio/midi", "audio/mid", "audio/x-midi"};
    static final Object lock = new Object();
    static final int[][] q = new int[4][];
    static int qn;
    static Hub hub;
    static Thread th;
    static Player mp;
    static int curKey;

    static void start(Hub h) {
        if (th != null) return;
        hub = h;
        th = new Thread(new Snd());
        th.start();
    }

    static void sfx(int id) {
        if (Cfg.sfxOff != 0 || th == null) return;
        synchronized (lock) {
            if (qn < 4) { q[qn++] = PAT[id]; lock.notify(); }
        }
    }

    static void wake() {
        synchronized (lock) { lock.notify(); }
    }

    public void run() {
        while (hub.running) {
            int[] pat = null;
            synchronized (lock) {
                if (qn == 0) {
                    try { lock.wait(300); } catch (InterruptedException e) {}
                }
                if (qn > 0) {
                    pat = q[0];
                    qn--;
                    for (int i = 0; i < qn; i++) q[i] = q[i + 1];
                }
            }
            try { sync(); } catch (Throwable e) {}
            if (pat != null) play(pat);
        }
        stopMusic();
        th = null;
    }

    static void sync() {
        int want = 0;
        if (hub.running && hub.isShown() && !hub.ringing) want = Cfg.track;
        int key = want == 0 ? 0 : want * 8 + Cfg.quiet;
        if (key == curKey) return;
        stopMusic();
        curKey = key;
        if (want > 0) startMusic(want - 1);
    }

    static void startMusic(int t) {
        byte[] mid = Midi.build(t);
        for (int i = 0; i < TYPES.length && mp == null; i++) {
            Player p = null;
            try {
                p = Manager.createPlayer(new ByteArrayInputStream(mid), TYPES[i]);
                p.realize();
                p.setLoopCount(-1);
                try {
                    VolumeControl v = (VolumeControl) p.getControl("VolumeControl");
                    if (v != null) v.setLevel(100 - Cfg.quiet * 20);
                } catch (Throwable e) {}
                p.start();
                mp = p;
            } catch (Throwable e) {
                if (p != null) {
                    try { p.close(); } catch (Throwable e2) {}
                }
            }
        }
    }

    static void stopMusic() {
        if (mp != null) {
            try { mp.stop(); } catch (Throwable e) {}
            try { mp.close(); } catch (Throwable e) {}
            mp = null;
        }
    }

    static void play(int[] pat) {
        int vol = 100 - Cfg.quiet * 20;
        for (int i = 0; i + 1 < pat.length; i += 2) {
            try { Manager.playTone(pat[i], pat[i + 1], vol); } catch (Throwable e) { return; }
            try { Thread.sleep(pat[i + 1]); } catch (InterruptedException e) {}
        }
    }
}
