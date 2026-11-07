import javax.microedition.lcdui.*;

final class SnakeApp extends App {
    static final int CS = 12;
    int gw, gh, ox, oy, len, dir, nextDir, fx, fy, score, interval;
    int[] sx, sy;
    boolean dead, paused;
    long last;

    boolean wall() { return false; }
    int tickMs() { return 30; }
    void leave(Hub h) { Cfg.save(); }
    void pause(Hub h) { paused = true; }

    void enter(Hub h) {
        int ah = h.h - h.headH - h.barH - 8;
        gw = h.w / CS; gh = ah / CS;
        ox = (h.w - gw * CS) / 2;
        oy = h.headH + 4 + (ah - gh * CS) / 2;
        reset(h);
    }

    void reset(Hub h) {
        sx = new int[gw * gh]; sy = new int[gw * gh];
        len = 3; dir = 3; nextDir = 3;
        for (int i = 0; i < len; i++) { sx[i] = gw / 2 - i; sy[i] = gh / 2; }
        score = 0; dead = false; paused = false; interval = 150;
        last = System.currentTimeMillis();
        food(h);
    }

    void food(Hub h) {
        for (int tries = 0; tries < 500; tries++) {
            int x = h.rnd(gw), y = h.rnd(gh);
            boolean ok = true;
            for (int i = 0; i < len; i++) if (sx[i] == x && sy[i] == y) { ok = false; break; }
            if (ok) { fx = x; fy = y; return; }
        }
        fx = -1; fy = -1;
    }

    boolean tick(Hub h, long now) {
        if (dead || paused || now - last < interval) return false;
        last = now;
        dir = nextDir;
        int nx = sx[0] + (dir == 2 ? -1 : dir == 3 ? 1 : 0);
        int ny = sy[0] + (dir == 0 ? -1 : dir == 1 ? 1 : 0);
        boolean eat = (nx == fx && ny == fy);
        boolean hit = nx < 0 || ny < 0 || nx >= gw || ny >= gh;
        int lim = eat ? len : len - 1;
        for (int i = 0; i < lim && !hit; i++) if (sx[i] == nx && sy[i] == ny) hit = true;
        if (hit) {
            dead = true;
            if (score > Cfg.hi[0]) { Cfg.hi[0] = score; Cfg.save(); }
            return true;
        }
        int nl = eat ? len + 1 : len;
        for (int i = nl - 1; i > 0; i--) { sx[i] = sx[i - 1]; sy[i] = sy[i - 1]; }
        sx[0] = nx; sy[0] = ny; len = nl;
        if (eat) {
            score += 10;
            if (interval > 70) interval -= 4;
            if (len >= gw * gh) { dead = true; return true; }
            food(h);
        }
        return true;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { if (dead) reset(h); else paused = !paused; return; }
        int a = h.act(k);
        if (a == Canvas.UP && dir != 1) nextDir = 0;
        else if (a == Canvas.DOWN && dir != 0) nextDir = 1;
        else if (a == Canvas.LEFT && dir != 3) nextDir = 2;
        else if (a == Canvas.RIGHT && dir != 2) nextDir = 3;
        else if (a == Canvas.FIRE) { if (dead) reset(h); else paused = !paused; }
    }

    void paint(Graphics g, Hub h) {
        g.setColor(h.cBg);
        g.fillRect(0, 0, h.w, h.h);
        g.setColor(Hub.mix(h.cBg, h.cAcc, 90));
        g.drawRect(ox - 1, oy - 1, gw * CS + 1, gh * CS + 1);
        for (int i = len - 1; i >= 0; i--) {
            g.setColor(i == 0 ? Hub.mix(h.cAcc, 0xFFFFFF, 90) : h.cAcc);
            g.fillRoundRect(ox + sx[i] * CS + 1, oy + sy[i] * CS + 1, CS - 2, CS - 2, 5, 5);
        }
        if (fx >= 0) {
            g.setColor(0xFF453A);
            g.fillArc(ox + fx * CS + 1, oy + fy * CS + 1, CS - 2, CS - 2, 0, 360);
        }
        if (dead) h.banner(g, "Game Over", "Score " + score + "  FIRE: retry");
        else if (paused) h.banner(g, "Paused", "FIRE: resume");
        h.drawHeader(g, "Snake", "" + score);
        h.drawBar(g, dead ? "Retry" : "Pause", "Best " + Cfg.hi[0], "Back");
    }
}
