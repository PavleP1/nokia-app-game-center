import javax.microedition.lcdui.*;

final class BreakoutApp extends App {
    static final int COLS = 8, ROWS = 5, R = 3;
    static final int[] RC = {0xFF453A, 0xFF9F0A, 0xFFD60A, 0x30D158, 0x0A84FF};
    int top, bw, bh = 12, bo, bt, pw, ph = 6, py, px, bx, by, vx, vy, pbx, lives, score, level, left, sp;
    int[] brk = new int[COLS * ROWS];
    boolean launched, over, paused;

    boolean wall() { return false; }
    boolean repeat() { return true; }
    int tickMs() { return 30; }
    void leave(Hub h) { Cfg.save(); }
    void pause(Hub h) { paused = true; }

    void enter(Hub h) {
        top = h.headH;
        bw = (h.w - 4) / COLS;
        bo = (h.w - bw * COLS) / 2;
        bt = top + 10;
        pw = h.w / 5;
        py = h.h - h.barH - 18;
        level = 1; lives = 3; score = 0; over = false; paused = false;
        newLevel(h);
    }

    void newLevel(Hub h) {
        for (int i = 0; i < brk.length; i++) brk[i] = 1;
        left = brk.length;
        sp = 3 + level;
        if (sp > 7) sp = 7;
        resetBall(h);
    }

    void resetBall(Hub h) {
        launched = false;
        px = (h.w - pw) / 2;
        bx = px + pw / 2; by = py - R - 1;
    }

    boolean tick(Hub h, long now) {
        if (!launched || over || paused) return false;
        pbx = bx;
        bx += vx; by += vy;
        if (bx < R) { bx = R; vx = -vx; }
        if (bx > h.w - R) { bx = h.w - R; vx = -vx; }
        if (by < top + R) { by = top + R; vy = -vy; }
        if (vy > 0 && by + R >= py && by - R <= py + ph && bx >= px - R && bx <= px + pw + R) {
            int rel = (bx - (px + pw / 2)) * 6 / pw;
            vx = rel;
            if (vx == 0) vx = (pbx < bx) ? 1 : -1;
            if (vx > sp) vx = sp;
            if (vx < -sp) vx = -sp;
            vy = -sp;
            by = py - R;
        }
        if (bx >= bo && by >= bt) {
            int c = (bx - bo) / bw, r = (by - bt) / bh;
            if (c < COLS && r < ROWS && brk[r * COLS + c] != 0) {
                brk[r * COLS + c] = 0;
                left--;
                score += 10;
                int pc = pbx >= bo ? (pbx - bo) / bw : -1;
                if (pc != c) vx = -vx; else vy = -vy;
                if (left == 0) { level++; newLevel(h); return true; }
            }
        }
        if (by > py + 20) {
            lives--;
            if (lives <= 0) {
                over = true;
                if (score > Cfg.hi[2]) { Cfg.hi[2] = score; Cfg.save(); }
            } else resetBall(h);
        }
        return true;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { paused = !paused; return; }
        int a = h.act(k);
        int step = h.w / 10;
        if (a == Canvas.LEFT) { px -= step; if (px < 0) px = 0; }
        else if (a == Canvas.RIGHT) { px += step; if (px > h.w - pw) px = h.w - pw; }
        else if (a == Canvas.FIRE) {
            if (over) { enter(h); return; }
            if (paused) { paused = false; return; }
            if (!launched) { launched = true; vy = -sp; vx = h.rnd(2) == 0 ? 2 : -2; }
        }
        if (!launched) { bx = px + pw / 2; by = py - R - 1; }
    }

    void paint(Graphics g, Hub h) {
        g.setColor(h.cBg);
        g.fillRect(0, 0, h.w, h.h);
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (brk[r * COLS + c] != 0) {
                    g.setColor(RC[r]);
                    g.fillRoundRect(bo + c * bw + 1, bt + r * bh + 1, bw - 2, bh - 2, 4, 4);
                }
        g.setColor(h.cAcc);
        g.fillRoundRect(px, py, pw, ph, 6, 6);
        g.setColor(h.cTxt);
        g.fillArc(bx - R, by - R, R * 2, R * 2, 0, 360);
        if (over) h.banner(g, "Game Over", "Score " + score + "  FIRE: retry");
        else if (paused) h.banner(g, "Paused", "FIRE: resume");
        else if (!launched) h.banner(g, "Level " + level, "FIRE: launch");
        h.drawHeader(g, "Breakout", "L" + lives + "  " + score);
        h.drawBar(g, "Pause", "Best " + Cfg.hi[2], "Back");
    }
}
