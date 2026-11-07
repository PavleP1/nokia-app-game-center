import javax.microedition.lcdui.*;
import javax.microedition.io.PushRegistry;
import java.util.Calendar;
import java.util.Random;
import java.util.Vector;

final class Hub extends Canvas implements Runnable {
    static final int TL = Graphics.TOP | Graphics.LEFT;
    static final int TC = Graphics.TOP | Graphics.HCENTER;
    static final int TR = Graphics.TOP | Graphics.RIGHT;
    static final int TRANS_MS = 220;
    static final int NAPPS = 11;
    static final String[] APP_NAMES = {"Notes", "Alarm", "Camera", "Snake", "2048", "Breakout", "Mines", "Tetris", "Tools", "To-do", "Settings"};
    static final int[] APP_COLS = {0x0A84FF, 0xFF9F0A, 0x30D158, 0x8BC34A, 0xEDC22E, 0xFF453A, 0x8E8E93, 0xBF5AF2, 0x5E5CE6, 0xFF375F, 0x636366};
    static final String[] THEME_NAMES = {"Dark", "Light", "Ocean", "Amoled", "Sunset", "Forest"};
    // bg, card, text, subtext, bar, accent
    static final int[][] THEMES = {
        {0x121214, 0x1E1E22, 0xFFFFFF, 0x8E8E93, 0x1C1C1E, 0x0A84FF},
        {0xF2F2F7, 0xFFFFFF, 0x000000, 0x6C6C70, 0xE5E5EA, 0x007AFF},
        {0x0B1B2B, 0x13293D, 0xE8F1F8, 0x7F9BB3, 0x102235, 0x2EC4B6},
        {0x000000, 0x111111, 0xFFFFFF, 0x808080, 0x0A0A0A, 0x30D158},
        {0x2B1331, 0x3D1D45, 0xFFF1E6, 0xC9A3B8, 0x241029, 0xFF7A59},
        {0x0F2318, 0x17331F, 0xE9F5EC, 0x8FB59B, 0x0C1C13, 0x7ED957}
    };
    static final String[] ACC_NAMES = {"Auto", "Blue", "Green", "Orange", "Pink", "Purple", "Red", "Teal", "Yellow"};
    static final int[] ACCENTS = {0, 0x0A84FF, 0x30D158, 0xFF9F0A, 0xFF375F, 0xBF5AF2, 0xFF453A, 0x2EC4B6, 0xFFD60A};
    static final String[] WALL_NAMES = {"Solid", "Gradient", "Aurora", "Dots", "Grid", "Waves", "Stripes"};
    static final String[] PACK_NAMES = {"Rounded", "Circle", "Outline", "Flat", "Mono"};

    final ModernNotes midlet;
    final Display disp;
    final Font fs, fb, fh, fbig;
    volatile boolean running = true;
    boolean started;
    int w, h, headH, barH;
    int cBg, cCard, cTxt, cSub, cBar, cAcc;
    Image headImg, wallImg, backImg;
    int wallKey = -1;
    final Vector stack = new Vector();
    App cur;
    Image tFrom, tTo;
    volatile boolean trans;
    long tStart;
    int tDir;
    String toastMsg;
    long toastTime;
    int toastOff;
    int alH = 7, alM = 0;
    boolean alOn, alDaily, ringing;
    long alTime, ringStart, ringLast;
    final Random rand = new Random();

    Hub(ModernNotes m) {
        midlet = m;
        disp = Display.getDisplay(m);
        fs = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        fb = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        fh = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        fbig = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
        setFullScreenMode(true);
        w = getWidth();
        h = getHeight();
        headH = fh.getHeight() + 14;
        barH = fs.getHeight() + 10;
        refresh();
        loadAlarm();
        cur = new HomeApp();
        stack.addElement(cur);
        cur.enter(this);
    }

    void start() {
        if (started) return;
        started = true;
        new Thread(this).start();
    }

    void stop(boolean exiting) {
        if (exiting) running = false;
        if (cur != null) cur.pause(this);
        Cfg.save();
    }

    void onResume() {
        long now = System.currentTimeMillis();
        if (alOn && alTime > 0 && !ringing) {
            if (now >= alTime && now - alTime < 600000L) ring();
            else if (now >= alTime) {
                if (alDaily) armAlarm(false);
                else { alOn = false; alTime = 0; saveAlarm(); }
            }
        }
        if (cur != null) cur.resume(this);
    }

    protected void sizeChanged(int nw, int nh) { w = nw; h = nh; headImg = null; wallImg = null; wallKey = -1; }
    protected void showNotify() { if (cur != null) cur.resume(this); }
    protected void hideNotify() { if (cur != null) cur.pause(this); }

    // ---------- palette
    void refresh() {
        int[] t = THEMES[Cfg.theme];
        cBg = t[0]; cCard = t[1]; cTxt = t[2]; cSub = t[3]; cBar = t[4];
        cAcc = Cfg.accent == 0 ? t[5] : ACCENTS[Cfg.accent];
        headImg = null; wallImg = null; wallKey = -1;
    }

    // ---------- helpers
    static int mix(int a, int b, int p) {
        int r = (((a >> 16) & 255) * (255 - p) + ((b >> 16) & 255) * p) / 255;
        int g = (((a >> 8) & 255) * (255 - p) + ((b >> 8) & 255) * p) / 255;
        int bl = ((a & 255) * (255 - p) + (b & 255) * p) / 255;
        return (r << 16) | (g << 8) | bl;
    }
    static String two(int v) { return v < 10 ? "0" + v : "" + v; }
    static int tri(int v, int max) { int p = max * 2; v %= p; if (v < 0) v += p; return v < max ? v : p - v; }
    int rnd(int n) { return (rand.nextInt() & 0x7fffffff) % n; }
    String fit(String s, Font f, int maxW) {
        if (f.stringWidth(s) <= maxW) return s;
        while (s.length() > 1 && f.stringWidth(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }
    int act(int k) { try { return getGameAction(k); } catch (Exception e) { return 0; } }
    boolean isLeft(int k) { return k == -6 || k == -21; }
    boolean isRight(int k) { return k == -7 || k == -22; }
    boolean isClear(int k) { return k == -8; }
    void toast(String m) { toastMsg = m; toastTime = System.currentTimeMillis(); }
    void alert() { try { AlertType.ALARM.playSound(disp); disp.vibrate(600); } catch (Throwable e) {} }

    App make(int id) {
        switch (id) {
            case 0: return new NotesApp();
            case 1: return new AlarmApp();
            case 2: return new CameraApp();
            case 3: return new SnakeApp();
            case 4: return new Game2048();
            case 5: return new BreakoutApp();
            case 6: return new MinesApp();
            case 7: return new TetrisApp();
            case 8: return new ToolsApp();
            case 9: return new TodoApp();
            default: return new SettingsApp();
        }
    }

    // ---------- navigation (slide transitions between cached snapshots)
    Image snap(App a) {
        Image im = Image.createImage(w, h);
        renderApp(im.getGraphics(), a);
        return im;
    }

    void startTrans(Image f, Image t, int dir) {
        if (f != null && t != null) {
            tFrom = f; tTo = t; tDir = dir;
            tStart = System.currentTimeMillis();
            trans = true;
        } else {
            trans = false; tFrom = null; tTo = null;
        }
    }

    void open(App a) {
        App old = cur;
        Image f = null, t = null;
        if (!old.live() && !a.live()) { try { f = snap(old); } catch (Throwable e) { f = null; } }
        stack.addElement(a);
        cur = a;
        a.enter(this);
        if (f != null) { try { t = snap(a); } catch (Throwable e) { t = null; } }
        startTrans(f, t, 1);
        repaint();
    }

    void back() {
        if (stack.size() <= 1) return;
        App old = cur;
        Image f = null, t = null;
        if (!old.live()) { try { f = snap(old); } catch (Throwable e) { f = null; } }
        old.leave(this);
        stack.removeElementAt(stack.size() - 1);
        cur = (App) stack.elementAt(stack.size() - 1);
        cur.resume(this);
        if (f != null && !cur.live()) { try { t = snap(cur); } catch (Throwable e) { t = null; } }
        startTrans(f, t, -1);
        repaint();
    }

    // ---------- main loop
    public void run() {
        while (running) {
            long now = System.currentTimeMillis();
            boolean dirty = false;
            int sleep = 100;
            if (trans) {
                dirty = true; sleep = 30;
                if (now - tStart >= TRANS_MS) { trans = false; tFrom = null; tTo = null; }
            }
            int tOff = (toastMsg != null && now - toastTime < 1800) ? barH + fb.getHeight() + 20 : 0;
            if (toastOff != tOff) {
                int d = tOff - toastOff, s = d / 3;
                if (s == 0) s = d > 0 ? 1 : -1;
                toastOff += s;
                dirty = true; sleep = 30;
            } else if (tOff == 0) {
                toastMsg = null;
            }
            App a = cur;
            if (!trans && !ringing && a != null) {
                int ms = a.tickMs();
                if (ms > 0) {
                    if (a.tick(this, now)) dirty = true;
                    if (ms < sleep) sleep = ms;
                }
            }
            if (alOn && alTime > 0 && !ringing && now >= alTime && now - alTime < 600000L) ring();
            if (ringing) {
                dirty = true;
                if (now - ringLast >= 2000) {
                    ringLast = now;
                    try { AlertType.ALARM.playSound(disp); disp.vibrate(800); disp.flashBacklight(1000); } catch (Throwable e) {}
                }
                if (now - ringStart > 90000L) dismiss(false);
            }
            if (dirty) repaint();
            try { Thread.sleep(sleep); } catch (InterruptedException e) {}
        }
    }

    // ---------- painting
    protected void paint(Graphics g) {
        int nw = getWidth(), nh = getHeight();
        if (nw != w || nh != h) { w = nw; h = nh; headImg = null; wallImg = null; wallKey = -1; }
        Graphics gg = g;
        if (!isDoubleBuffered()) {
            try {
                if (backImg == null || backImg.getWidth() != w || backImg.getHeight() != h) backImg = Image.createImage(w, h);
                gg = backImg.getGraphics();
            } catch (Throwable e) { gg = g; backImg = null; }
        }
        if (ringing) drawRing(gg);
        else if (trans && tFrom != null && tTo != null) drawTrans(gg);
        else renderApp(gg, cur);
        drawToast(gg);
        if (gg != g) g.drawImage(backImg, 0, 0, TL);
    }

    void renderApp(Graphics g, App a) {
        if (a.wall()) drawWall(g);
        a.paint(g, this);
    }

    void drawTrans(Graphics g) {
        int el = (int) (System.currentTimeMillis() - tStart);
        if (el > TRANS_MS) el = TRANS_MS;
        int p = el * 256 / TRANS_MS;
        int e = 256 - ((256 - p) * (256 - p)) / 256;
        int off = w * e / 256;
        if (tDir > 0) {
            g.drawImage(tFrom, -off / 3, 0, TL);
            g.drawImage(tTo, w - off, 0, TL);
        } else {
            g.drawImage(tTo, -(w - off) / 3, 0, TL);
            g.drawImage(tFrom, off, 0, TL);
        }
    }

    void drawWall(Graphics g) {
        if (Cfg.wall == 0) { g.setColor(cBg); g.fillRect(0, 0, w, h); return; }
        int key = ((Cfg.theme * 16 + Cfg.accent) * 16 + Cfg.wall) * 8192 + w;
        if (wallKey != key) { wallImg = makeWall(); wallKey = key; }
        if (wallImg != null) g.drawImage(wallImg, 0, 0, TL);
        else { g.setColor(cBg); g.fillRect(0, 0, w, h); }
    }

    Image makeWall() {
        try {
            Image im = Image.createImage(w, h);
            Graphics g = im.getGraphics();
            g.setColor(cBg);
            g.fillRect(0, 0, w, h);
            switch (Cfg.wall) {
                case 1:
                    for (int y = 0; y < h; y++) { g.setColor(mix(cBg, cAcc, y * 70 / h)); g.drawLine(0, y, w, y); }
                    break;
                case 2:
                    for (int i = -h; i < w; i++) { g.setColor(mix(cBg, cAcc, 10 + tri((i + h) * 2, 40))); g.drawLine(i, 0, i + h, h); }
                    break;
                case 3:
                    g.setColor(mix(cBg, cAcc, 60));
                    for (int y = 0; y < h; y += 16)
                        for (int x = (y / 16 % 2) * 8; x < w; x += 16) g.fillArc(x, y, 4, 4, 0, 360);
                    break;
                case 4:
                    g.setColor(mix(cBg, cAcc, 40));
                    for (int x = 0; x < w; x += 20) g.drawLine(x, 0, x, h);
                    for (int y = 0; y < h; y += 20) g.drawLine(0, y, w, y);
                    break;
                case 5:
                    g.setColor(mix(cBg, cAcc, 50));
                    for (int k = 0; k < 7; k++)
                        for (int x = 0; x < w; x++) g.fillRect(x, k * h / 7 + tri(x * 2 + k * 40, 14), 1, 2);
                    break;
                case 6:
                    g.setColor(mix(cBg, cAcc, 28));
                    for (int y = 0; y < h; y += 14) g.fillRect(0, y, w, 7);
                    break;
            }
            return im;
        } catch (Throwable e) {
            return null;
        }
    }

    void drawHeader(Graphics g, String title, String pill) {
        if (headImg == null) {
            try {
                Image im = Image.createImage(w, headH);
                Graphics ig = im.getGraphics();
                for (int i = 0; i < headH; i++) { ig.setColor(mix(cBar, cBg, i * 130 / headH)); ig.drawLine(0, i, w, i); }
                ig.setColor(mix(cBar, cSub, 90));
                ig.drawLine(0, headH - 1, w, headH - 1);
                headImg = im;
            } catch (Throwable e) { headImg = null; }
        }
        if (headImg != null) g.drawImage(headImg, 0, 0, TL);
        else { g.setColor(cBar); g.fillRect(0, 0, w, headH); }
        g.setFont(fh);
        g.setColor(cTxt);
        g.drawString(title, 12, (headH - fh.getHeight()) / 2, TL);
        if (pill != null) {
            int pw = fb.stringWidth(pill) + 16, ph = fb.getHeight() + 4;
            g.setColor(cAcc);
            g.fillRoundRect(w - pw - 10, (headH - ph) / 2, pw, ph, ph, ph);
            g.setFont(fb);
            g.setColor(0xFFFFFF);
            g.drawString(pill, w - 10 - pw / 2, (headH - ph) / 2 + 2, TC);
        }
    }

    void drawBar(Graphics g, String l, String c, String r) {
        g.setColor(cBar);
        g.fillRect(0, h - barH, w, barH);
        g.setColor(mix(cBar, cSub, 90));
        g.drawLine(0, h - barH, w, h - barH);
        int y = h - barH + 5;
        g.setFont(fb);
        g.setColor(cAcc);
        g.drawString(l, 10, y, TL);
        g.drawString(r, w - 10, y, TR);
        g.setFont(fs);
        g.setColor(cSub);
        g.drawString(c, w / 2, y, TC);
    }

    void card(Graphics g, int x, int y, int cw, int ch, boolean sel) {
        if (sel) { g.setColor(cAcc); g.fillRoundRect(x - 2, y - 2, cw + 4, ch + 4, 16, 16); }
        g.setColor(cCard);
        g.fillRoundRect(x, y, cw, ch, 14, 14);
    }

    void banner(Graphics g, String a, String b) {
        int bw = Math.max(fb.stringWidth(a), fs.stringWidth(b)) + 40, bh = fb.getHeight() + fs.getHeight() + 28;
        int x = (w - bw) / 2, y = (h - bh) / 2;
        card(g, x, y, bw, bh, true);
        g.setFont(fb);
        g.setColor(cTxt);
        g.drawString(a, w / 2, y + 10, TC);
        g.setFont(fs);
        g.setColor(cSub);
        g.drawString(b, w / 2, y + 16 + fb.getHeight(), TC);
    }

    void drawToast(Graphics g) {
        if (toastMsg == null || toastOff <= 0) return;
        int tw = fb.stringWidth(toastMsg) + 28, th = fb.getHeight() + 12;
        int y = h - toastOff;
        g.setColor(cAcc);
        g.fillRoundRect((w - tw) / 2, y, tw, th, th, th);
        g.setFont(fb);
        g.setColor(0xFFFFFF);
        g.drawString(toastMsg, w / 2, y + 6, TC);
    }

    // ---------- icons (the "icon pack" is how the tile is drawn)
    void icon(Graphics g, int id, int cx, int cy, int size) {
        int col = APP_COLS[id], p = Cfg.pack, half = size / 2, fg, bg;
        if (p == 0) {
            g.setColor(col); g.fillRoundRect(cx - half, cy - half, size, size, size / 3, size / 3);
            fg = 0xFFFFFF; bg = col;
        } else if (p == 1) {
            g.setColor(col); g.fillArc(cx - half, cy - half, size, size, 0, 360);
            fg = 0xFFFFFF; bg = col;
        } else if (p == 2) {
            g.setColor(col); g.fillRoundRect(cx - half, cy - half, size, size, size / 3, size / 3);
            g.setColor(cCard); g.fillRoundRect(cx - half + 2, cy - half + 2, size - 4, size - 4, size / 3 - 2, size / 3 - 2);
            fg = col; bg = cCard;
        } else if (p == 3) {
            fg = col; bg = cBg;
        } else {
            g.setColor(cAcc); g.fillRoundRect(cx - half, cy - half, size, size, size / 3, size / 3);
            fg = 0xFFFFFF; bg = cAcc;
        }
        int u = size / 16;
        if (u < 1) u = 1;
        glyph(g, id, cx, cy, u, fg, bg);
    }

    void glyph(Graphics g, int id, int cx, int cy, int u, int fg, int bg) {
        g.setColor(fg);
        int t = u / 2 + 1;
        switch (id) {
            case 0:
                g.fillRoundRect(cx - 5 * u, cy - 7 * u, 10 * u, 14 * u, 2 * u, 2 * u);
                g.setColor(bg);
                g.fillRect(cx - 3 * u, cy - 3 * u, 6 * u, t);
                g.fillRect(cx - 3 * u, cy, 6 * u, t);
                g.fillRect(cx - 3 * u, cy + 3 * u, 4 * u, t);
                break;
            case 1:
                g.fillArc(cx - 6 * u, cy - 5 * u, 12 * u, 12 * u, 0, 360);
                g.fillArc(cx - 8 * u, cy - 7 * u, 5 * u, 5 * u, 0, 360);
                g.fillArc(cx + 3 * u, cy - 7 * u, 5 * u, 5 * u, 0, 360);
                g.setColor(bg);
                g.fillRect(cx, cy - 3 * u, t, 4 * u);
                g.fillRect(cx, cy + u, 3 * u, t);
                break;
            case 2:
                g.fillRoundRect(cx - 7 * u, cy - 4 * u, 14 * u, 10 * u, 2 * u, 2 * u);
                g.fillRect(cx - 3 * u, cy - 6 * u, 6 * u, 3 * u);
                g.setColor(bg);
                g.fillArc(cx - 3 * u, cy - 3 * u, 6 * u, 6 * u, 0, 360);
                g.setColor(fg);
                g.fillArc(cx - u, cy - u, 2 * u, 2 * u, 0, 360);
                break;
            case 3:
                g.fillRect(cx - 5 * u, cy - 6 * u, 10 * u, 2 * u);
                g.fillRect(cx - 5 * u, cy - 6 * u, 2 * u, 6 * u);
                g.fillRect(cx - 5 * u, cy - 2 * u, 10 * u, 2 * u);
                g.fillRect(cx + 3 * u, cy - 2 * u, 2 * u, 6 * u);
                g.fillRect(cx - 5 * u, cy + 2 * u, 10 * u, 2 * u);
                break;
            case 4:
                g.fillRoundRect(cx - 7 * u, cy - 7 * u, 14 * u, 14 * u, 3 * u, 3 * u);
                g.setColor(bg);
                g.setFont(fb);
                g.drawString("2K", cx, cy - fb.getHeight() / 2, TC);
                break;
            case 5:
                g.fillRect(cx - 7 * u, cy - 6 * u, 4 * u - 1, 2 * u);
                g.fillRect(cx - 2 * u, cy - 6 * u, 4 * u, 2 * u);
                g.fillRect(cx + 3 * u + 1, cy - 6 * u, 4 * u - 1, 2 * u);
                g.fillRect(cx - 4 * u, cy + 5 * u, 8 * u, 2 * u);
                g.fillArc(cx - u, cy, 2 * u, 2 * u, 0, 360);
                break;
            case 6:
                g.fillArc(cx - 4 * u, cy - 4 * u, 8 * u, 8 * u, 0, 360);
                g.fillRect(cx - 6 * u, cy, 12 * u, t);
                g.fillRect(cx, cy - 6 * u, t, 12 * u);
                g.drawLine(cx - 5 * u, cy - 5 * u, cx + 5 * u, cy + 5 * u);
                g.drawLine(cx + 5 * u, cy - 5 * u, cx - 5 * u, cy + 5 * u);
                g.setColor(bg);
                g.fillRect(cx - 2 * u, cy - 2 * u, u, u);
                break;
            case 7: {
                int b = 4 * u - 1;
                g.fillRect(cx - 6 * u, cy - 5 * u, b, b);
                g.fillRect(cx - 2 * u, cy - 5 * u, b, b);
                g.fillRect(cx + 2 * u, cy - 5 * u, b, b);
                g.fillRect(cx - 2 * u, cy - u, b, b);
                break;
            }
            case 8:
                g.fillArc(cx - 6 * u, cy - 5 * u, 12 * u, 12 * u, 0, 360);
                g.fillRect(cx - u, cy - 7 * u, 2 * u, 2 * u);
                g.setColor(bg);
                g.fillRect(cx, cy - 3 * u, t, 4 * u);
                g.fillRect(cx, cy + u, 3 * u, t);
                break;
            case 9:
                g.fillRoundRect(cx - 6 * u, cy - 6 * u, 12 * u, 12 * u, 2 * u, 2 * u);
                g.setColor(bg);
                g.drawLine(cx - 3 * u, cy, cx - u, cy + 3 * u);
                g.drawLine(cx - 3 * u, cy + 1, cx - u, cy + 3 * u + 1);
             
