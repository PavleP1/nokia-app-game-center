import javax.microedition.lcdui.*;

final class Game2048 extends App {
    static final int[] TILE = {0xEEE4DA, 0xEDE0C8, 0xF2B179, 0xF59563, 0xF67C5F, 0xF65E3B, 0xEDCF72, 0xEDCC61, 0xEDC850, 0xEDC53F, 0xEDC22E, 0x3C3A32};
    int[] b = new int[16];
    int score;
    boolean over, won;

    boolean wall() { return false; }
    void enter(Hub h) { reset(h); }
    void leave(Hub h) { Cfg.save(); }
    void pause(Hub h) { Cfg.save(); }

    void reset(Hub h) {
        for (int i = 0; i < 16; i++) b[i] = 0;
        score = 0; over = false; won = false;
        spawn(h); spawn(h);
    }

    void spawn(Hub h) {
        int n = 0;
        for (int i = 0; i < 16; i++) if (b[i] == 0) n++;
        if (n == 0) return;
        int r = h.rnd(n);
        for (int i = 0; i < 16; i++) if (b[i] == 0) { if (r == 0) { b[i] = h.rnd(10) == 0 ? 4 : 2; return; } r--; }
    }

    int idx(int d, int i, int j) {
        switch (d) {
            case 0: return j * 4 + i;
            case 1: return (3 - j) * 4 + i;
            case 2: return i * 4 + j;
            default: return i * 4 + (3 - j);
        }
    }

    boolean move(int d) {
        boolean moved = false;
        for (int i = 0; i < 4; i++) {
            int[] out = new int[4];
            int n = 0, last = 0;
            for (int j = 0; j < 4; j++) {
                int v = b[idx(d, i, j)];
                if (v == 0) continue;
                if (last != 0 && last == v) { out[n - 1] = v * 2; score += v * 2; last = 0; }
                else { out[n++] = v; last = v; }
            }
            for (int j = 0; j < 4; j++) {
                int p = idx(d, i, j);
                if (b[p] != out[j]) moved = true;
                b[p] = out[j];
            }
        }
        return moved;
    }

    boolean stuck() {
        for (int i = 0; i < 16; i++) {
            if (b[i] == 0) return false;
            if (i % 4 < 3 && b[i] == b[i + 1]) return false;
            if (i < 12 && b[i] == b[i + 4]) return false;
        }
        return true;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { reset(h); return; }
        int a = h.act(k);
        if (over) { if (a == Canvas.FIRE) reset(h); return; }
        int d = a == Canvas.UP ? 0 : a == Canvas.DOWN ? 1 : a == Canvas.LEFT ? 2 : a == Canvas.RIGHT ? 3 : -1;
        if (d < 0) return;
        if (move(d)) {
            spawn(h);
            if (score > Cfg.hi[1]) Cfg.hi[1] = score;
            for (int i = 0; i < 16; i++) if (b[i] == 2048 && !won) { won = true; h.toast("2048! Keep going"); }
            if (stuck()) { over = true; Cfg.save(); }
        }
    }

    void paint(Graphics g, Hub h) {
        g.setColor(h.cBg);
        g.fillRect(0, 0, h.w, h.h);
        int gap = 6;
        int bs = Math.min(h.w - 20, h.h - h.headH - h.barH - h.fs.getHeight() - 20);
        int tile = (bs - 5 * gap) / 4;
        int x0 = (h.w - bs) / 2, y0 = h.headH + h.fs.getHeight() + 14;
        g.setFont(h.fs);
        g.setColor(h.cSub);
        g.drawString("Best " + Cfg.hi[1], h.w / 2, h.headH + 6, Hub.TC);
        g.setColor(h.cBar);
        g.fillRoundRect(x0, y0, bs, bs, 12, 12);
        for (int i = 0; i < 16; i++) {
            int x = x0 + gap + (i % 4) * (tile + gap), y = y0 + gap + (i / 4) * (tile + gap), v = b[i];
            int ci = 0;
            for (int t = v; t > 2; t >>= 1) ci++;
            g.setColor(v == 0 ? h.cCard : TILE[Math.min(ci, 11)]);
            g.fillRoundRect(x, y, tile, tile, 8, 8);
            if (v != 0) {
                g.setFont(h.fb);
                g.setColor(v <= 4 ? 0x776E65 : 0xFFFFFF);
                g.drawString("" + v, x + tile / 2, y + (tile - h.fb.getHeight()) / 2, Hub.TC);
            }
        }
        if (over) h.banner(g, "Game Over", "Score " + score + "  FIRE: new");
        h.drawHeader(g, "2048", "" + score);
        h.drawBar(g, "New", "", "Back");
    }
}
