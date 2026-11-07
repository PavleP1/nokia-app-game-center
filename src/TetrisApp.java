import javax.microedition.lcdui.*;

final class TetrisApp extends App {
    static final int[][] SHAPES = {
        {0, 1, 1, 1, 2, 1, 3, 1}, {1, 0, 2, 0, 1, 1, 2, 1}, {1, 0, 0, 1, 1, 1, 2, 1},
        {1, 0, 2, 0, 0, 1, 1, 1}, {0, 0, 1, 0, 1, 1, 2, 1}, {0, 0, 0, 1, 1, 1, 2, 1}, {2, 0, 0, 1, 1, 1, 2, 1}
    };
    static final int[] COLS = {0x00BCD4, 0xFFD60A, 0xBF5AF2, 0x30D158, 0xFF453A, 0x0A84FF, 0xFF9F0A};
    static final int[] PTS = {0, 100, 300, 500, 800};
    int[] board = new int[200];
    int[] cells = new int[8];
    int type, next, px, py, score, lines, level, cs, ox, oy;
    boolean over, paused;
    long last;

    boolean wall() { return false; }
    boolean repeat() { return true; }
    int tickMs() { return 30; }
    void leave(Hub h) { Cfg.save(); }
    void pause(Hub h) { paused = true; }

    void enter(Hub h) {
        cs = Math.min((h.h - h.headH - h.barH - 8) / 20, (h.w * 58 / 100) / 10);
        ox = 6;
        oy = h.headH + 4 + ((h.h - h.headH - h.barH - 8) - cs * 20) / 2;
        reset(h);
    }

    void reset(Hub h) {
        for (int i = 0; i < 200; i++) board[i] = 0;
        score = 0; lines = 0; level = 0; over = false; paused = false;
        next = h.rnd(7);
        spawn(h);
        last = System.currentTimeMillis();
    }

    void spawn(Hub h) {
        type = next;
        next = h.rnd(7);
        System.arraycopy(SHAPES[type], 0, cells, 0, 8);
        px = 3; py = 0;
        if (!fits(cells, px, py)) {
            over = true;
            if (score > Cfg.hi[4]) { Cfg.hi[4] = score; Cfg.save(); }
        }
    }

    boolean fits(int[] c, int x0, int y0) {
        for (int i = 0; i < 4; i++) {
            int x = x0 + c[i * 2], y = y0 + c[i * 2 + 1];
            if (x < 0 || x >= 10 || y >= 20) return false;
            if (y >= 0 && board[y * 10 + x] != 0) return false;
        }
        return true;
    }

    void rotate() {
        if (type == 1) return;
        int[] n = new int[8];
        for (int i = 0; i < 4; i++) { n[i * 2] = 3 - cells[i * 2 + 1]; n[i * 2 + 1] = cells[i * 2]; }
        int[] kick = {0, -1, 1, -2, 2};
        for (int j = 0; j < 5; j++)
            if (fits(n, px + kick[j], py)) { cells = n; px += kick[j]; return; }
    }

    void lock(Hub h) {
        for (int i = 0; i < 4; i++) {
            int x = px + cells[i * 2], y = py + cells[i * 2 + 1];
            if (y >= 0) board[y * 10 + x] = type + 1;
        }
        int cleared = 0;
        for (int y = 19; y >= 0; y--) {
            boolean full = true;
            for (int x = 0; x < 10; x++) if (board[y * 10 + x] == 0) { full = false; break; }
            if (full) {
                for (int yy = y; yy > 0; yy--) System.arraycopy(board, (yy - 1) * 10, board, yy * 10, 10);
                for (int x = 0; x < 10; x++) board[x] = 0;
                cleared++;
                y++;
            }
        }
        score += PTS[cleared] * (level + 1);
        lines += cleared;
        level = lines / 10;
        spawn(h);
    }

    boolean tick(Hub h, long now) {
        if (over || paused) return false;
        int interval = 800 - level * 70;
        if (interval < 100) interval = 100;
        if (now - last < interval) return false;
        last = now;
        if (fits(cells, px, py + 1)) py++; else lock(h);
        return true;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { paused = !paused; return; }
        int a = h.act(k);
        if (over) { if (a == Canvas.FIRE) reset(h); return; }
        if (paused) { if (a == Canvas.FIRE) paused = false; return; }
        if (k == Canvas.KEY_NUM0 || k == Canvas.KEY_POUND) {
            while (fits(cells, px, py + 1)) { py++; score += 2; }
            lock(h);
            last = System.currentTimeMillis();
            return;
        }
        if (a == Canvas.LEFT && fits(cells, px - 1, py)) px--;
        else if (a == Canvas.RIGHT && fits(cells, px + 1, py)) px++;
        else if (a == Canvas.DOWN) { if (fits(cells, px, py + 1)) { py++; score++; } }
        else if (a == Canvas.UP || a == Canvas.FIRE) rotate();
    }

    void paint(Graphics g, Hub h) {
        g.setColor(h.cBg);
        g.fillRect(0, 0, h.w, h.h);
        g.setColor(h.cBar);
        g.fillRect(ox, oy, cs * 10, cs * 20);
        for (int i = 0; i < 200; i++)
            if (board[i] != 0) {
                g.setColor(COLS[board[i] - 1]);
                g.fillRect(ox + (i % 10) * cs, oy + (i / 10) * cs, cs - 1, cs - 1);
            }
        if (!over) {
            g.setColor(COLS[type]);
            for (int i = 0; i < 4; i++) {
                int y = py + cells[i * 2 + 1];
                if (y >= 0) g.fillRect(ox + (px + cells[i * 2]) * cs, oy + y * cs, cs - 1, cs - 1);
            }
        }
        g.setColor(h.cAcc);
        g.drawRect(ox - 1, oy - 1, cs * 10 + 1, cs * 20 + 1);
        int sx = ox + cs * 10 + 10;
        g.setFont(h.fs);
        g.setColor(h.cSub);
        g.drawString("Score", sx, oy, Hub.TL);
        g.setColor(h.cTxt);
        g.drawString("" + score, sx, oy + h.fs.getHeight(), Hub.TL);
        g.setColor(h.cSub);
        g.drawString("Lines", sx, oy + h.fs.getHeight() * 3, Hub.TL);
        g.setColor(h.cTxt);
        g.drawString("" + lines, sx, oy + h.fs.getHeight() * 4, Hub.TL);
        g.setColor(h.cSub);
        g.drawString("Level", sx, oy + h.fs.getHeight() * 6, Hub.TL);
        g.setColor(h.cTxt);
        g.drawString("" + (level + 1), sx, oy + h.fs.getHeight() * 7, Hub.TL);
        g.setColor(h.cSub);
        g.drawString("Next", sx, oy + h.fs.getHeight() * 9, Hub.TL);
        g.setColor(COLS[next]);
        int ny = oy + h.fs.getHeight() * 10 + 4, ps = Math.max(6, cs / 2);
        for (int i = 0; i < 4; i++)
            g.fillRect(sx + SHAPES[next][i * 2] * ps, ny + SHAPES[next][i * 2 + 1] * ps, ps - 1, ps - 1);
        if (over) h.banner(g, "Game Over", "Score " + score + "  FIRE: retry");
        else if (paused) h.banner(g, "Paused", "FIRE: resume");
        h.drawHeader(g, "Tetris", "Best " + Cfg.hi[4]);
        h.drawBar(g, "Pause", "5 Rot  0 Drop", "Back");
    }
}
