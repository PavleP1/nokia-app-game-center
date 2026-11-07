import javax.microedition.lcdui.*;

final class MinesApp extends App {
    static final int CS = 24;
    static final int[] NC = {0, 0x0A84FF, 0x30D158, 0xFF453A, 0x5E5CE6, 0xFF9F0A, 0x2EC4B6, 0xFFFFFF, 0x8E8E93};
    int cols, rows, n, mines, ox, oy, cx, cy, left, secs;
    boolean[] mine;
    int[] cnt, st;
    boolean first, over, won;
    long t0;

    boolean wall() { return false; }
    boolean repeat() { return true; }
    int tickMs() { return 500; }
    void leave(Hub h) { Cfg.save(); }

    boolean tick(Hub h, long now) {
        if (!first && !over && !won) {
            int s = (int) ((now - t0) / 1000);
            if (s != secs) { secs = s; return true; }
        }
        return false;
    }

    void enter(Hub h) {
        cols = Math.min(12, h.w / CS);
        rows = Math.min(12, (h.h - h.headH - h.barH - 4) / CS);
        n = cols * rows;
        mines = n / 6;
        ox = (h.w - cols * CS) / 2;
        oy = h.headH + 2;
        reset();
    }

    void reset() {
        mine = new boolean[n]; cnt = new int[n]; st = new int[n];
        first = true; over = false; won = false; secs = 0;
        cx = cols / 2; cy = rows / 2;
        left = n - mines;
    }

    void place(Hub h, int sx, int sy) {
        int placed = 0;
        while (placed < mines) {
            int i = h.rnd(n), x = i % cols, y = i / cols;
            if (mine[i] || (Math.abs(x - sx) <= 1 && Math.abs(y - sy) <= 1)) continue;
            mine[i] = true;
            placed++;
        }
        for (int i = 0; i < n; i++) {
            int c = 0, x = i % cols, y = i / cols;
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx, ny = y + dy;
                    if ((dx != 0 || dy != 0) && nx >= 0 && ny >= 0 && nx < cols && ny < rows && mine[ny * cols + nx]) c++;
                }
            cnt[i] = c;
        }
    }

    void reveal(Hub h, int i) {
        if (st[i] != 0) return;
        if (first) { first = false; t0 = System.currentTimeMillis(); place(h, i % cols, i / cols); }
        if (mine[i]) {
            over = true;
            for (int j = 0; j < n; j++) if (mine[j]) st[j] = 1;
            return;
        }
        int[] stk = new int[n];
        int sp = 0;
        stk[sp++] = i;
        st[i] = 1;
        while (sp > 0) {
            int c = stk[--sp];
            left--;
            if (cnt[c] == 0) {
                int x = c % cols, y = c / cols;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue;
                        int j = ny * cols + nx;
                        if (st[j] == 0 && !mine[j]) { st[j] = 1; stk[sp++] = j; }
                    }
            }
        }
        if (left <= 0) {
            won = true;
            secs = (int) ((System.currentTimeMillis() - t0) / 1000);
            if (Cfg.hi[3] == 0 || secs < Cfg.hi[3]) { Cfg.hi[3] = secs; Cfg.save(); }
        }
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (k == Canvas.KEY_STAR) { reset(); return; }
        int a = h.act(k);
        if (over || won) { if (a == Canvas.FIRE) reset(); return; }
        if (h.isLeft(k) || k == Canvas.KEY_NUM1) {
            int i = cy * cols + cx;
            if (st[i] == 0) st[i] = 2; else if (st[i] == 2) st[i] = 0;
            return;
        }
        if (a == Canvas.LEFT && cx > 0) cx--;
        else if (a == Canvas.RIGHT && cx < cols - 1) cx++;
        else if (a == Canvas.UP && cy > 0) cy--;
        else if (a == Canvas.DOWN && cy < rows - 1) cy++;
        else if (a == Canvas.FIRE) reveal(h, cy * cols + cx);
    }

    void paint(Graphics g, Hub h) {
        g.setColor(h.cBg);
        g.fillRect(0, 0, h.w, h.h);
        int flags = 0;
        for (int i = 0; i < n; i++) {
            int x = ox + (i % cols) * CS, y = oy + (i / cols) * CS;
            if (st[i] == 1) {
                g.setColor(h.cBar);
                g.fillRect(x + 1, y + 1, CS - 2, CS - 2);
                if (mine[i]) {
                    g.setColor(0xFF453A);
                    g.fillArc(x + 6, y + 6, CS - 12, CS - 12, 0, 360);
                } else if (cnt[i] > 0) {
                    g.setFont(h.fb);
                    g.setColor(NC[cnt[i]]);
                    g.drawString("" + cnt[i], x + CS / 2, y + (CS - h.fb.getHeight()) / 2, Hub.TC);
                }
            } else {
                g.setColor(Hub.mix(h.cCard, h.cAcc, 50));
                g.fillRect(x + 1, y + 1, CS - 2, CS - 2);
                if (st[i] == 2) {
                    flags++;
                    g.setFont(h.fb);
                    g.setColor(0xFF453A);
                    g.drawString("F", x + CS / 2, y + (CS - h.fb.getHeight()) / 2, Hub.TC);
                }
            }
        }
        int x = ox + cx * CS, y = oy + cy * CS;
        g.setColor(h.cAcc);
        g.drawRect(x, y, CS - 1, CS - 1);
        g.drawRect(x + 1, y + 1, CS - 3, CS - 3);
        if (over) h.banner(g, "Boom!", "FIRE: new game");
        else if (won) h.banner(g, "You won! " + secs + "s", "Best " + Cfg.hi[3] + "s  FIRE: new");
        h.drawHeader(g, "Mines", (mines - flags) + "  " + secs + "s");
        h.drawBar(g, "Flag", "* New", "Back");
    }
}
