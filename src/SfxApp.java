import javax.microedition.lcdui.Graphics;

final class SfxApp extends App {
    final App a;
    int s0, s1, s2, vy0;
    boolean b0, b1;

    SfxApp(App app) { a = app; }
    static App wrap(App app) { return new SfxApp(app); }

    void paint(Graphics g, Hub h) { a.paint(g, h); }
    void enter(Hub h) { a.enter(h); Snd.sfx(Snd.OPEN); }
    void leave(Hub h) { a.leave(h); Snd.sfx(Snd.BACK); }
    void pause(Hub h) { a.pause(h); }
    void resume(Hub h) { a.resume(h); }
    int tickMs() { return a.tickMs(); }
    boolean wall() { return a.wall(); }
    boolean live() { return a.live(); }
    boolean repeat() { return a.repeat(); }

    boolean tick(Hub h, long now) {
        snap();
        boolean r = a.tick(h, now);
        diff();
        return r;
    }

    void key(int k, Hub h) {
        if (a.wall()) Snd.sfx(Snd.TICK);
        snap();
        a.key(k, h);
        diff();
    }

    static int flags(MinesApp s) {
        int c = 0;
        if (s.st == null) return 0;
        for (int i = 0; i < s.n; i++) if (s.st[i] == 2) c++;
        return c;
    }

    void snap() {
        if (a instanceof SnakeApp) {
            SnakeApp s = (SnakeApp) a;
            s0 = s.score; b0 = s.dead;
        } else if (a instanceof Game2048) {
            Game2048 s = (Game2048) a;
            s0 = s.score; b0 = s.over; b1 = s.won;
        } else if (a instanceof BreakoutApp) {
            BreakoutApp s = (BreakoutApp) a;
            s0 = s.score; s1 = s.lives; s2 = s.level; b0 = s.over; vy0 = s.vy;
        } else if (a instanceof MinesApp) {
            MinesApp s = (MinesApp) a;
            s0 = s.left; s1 = flags(s); b0 = s.over; b1 = s.won;
        } else if (a instanceof TetrisApp) {
            TetrisApp s = (TetrisApp) a;
            s0 = s.lines; s1 = s.py; s2 = s.type; b0 = s.over;
        }
    }

    void diff() {
        if (a instanceof SnakeApp) {
            SnakeApp s = (SnakeApp) a;
            if (s.score > s0) Snd.sfx(Snd.BLIP);
            if (s.dead && !b0) Snd.sfx(Snd.LOSE);
        } else if (a instanceof Game2048) {
            Game2048 s = (Game2048) a;
            if (s.score > s0) Snd.sfx(Snd.BLIP);
            if (s.over && !b0) Snd.sfx(Snd.LOSE);
            if (s.won && !b1) Snd.sfx(Snd.WIN);
        } else if (a instanceof BreakoutApp) {
            BreakoutApp s = (BreakoutApp) a;
            if (s.score > s0) Snd.sfx(Snd.BLIP);
            else if (s.vy < 0 && vy0 > 0) Snd.sfx(Snd.BOUNCE);
            if (s.over && !b0) Snd.sfx(Snd.LOSE);
            else if (s.lives < s1) Snd.sfx(Snd.LOSE);
            if (s.level > s2) Snd.sfx(Snd.WIN);
        } else if (a instanceof MinesApp) {
            MinesApp s = (MinesApp) a;
            if (s.left < s0) Snd.sfx(Snd.BLIP);
            if (flags(s) != s1) Snd.sfx(Snd.TICK);
            if (s.over && !b0) Snd.sfx(Snd.LOSE);
            if (s.won && !b1) Snd.sfx(Snd.WIN);
        } else if (a instanceof TetrisApp) {
            TetrisApp s = (TetrisApp) a;
            if (s.lines > s0) Snd.sfx(Snd.LINE);
            else if (!s.over && (s.py < s1 || s.type != s2)) Snd.sfx(Snd.LOCK);
            if (s.over && !b0) Snd.sfx(Snd.LOSE);
        }
    }
}
