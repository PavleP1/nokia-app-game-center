import javax.microedition.lcdui.*;

final class SettingsApp extends App {
    static final String[] ROWS = {"Theme", "Accent color", "Wallpaper", "Icon pack", "Home layout", "Reset high scores", "About"};
    int sel;

    boolean repeat() { return true; }
    void leave(Hub h) { Cfg.save(); }

    String val(Hub h, int i) {
        switch (i) {
            case 0: return Hub.THEME_NAMES[Cfg.theme];
            case 1: return Hub.ACC_NAMES[Cfg.accent];
            case 2: return Hub.WALL_NAMES[Cfg.wall];
            case 3: return Hub.PACK_NAMES[Cfg.pack];
            case 4: return Cfg.layout == 0 ? "Grid" : "List";
            case 5: return "FIRE";
            default: String v = h.midlet.prop("MIDlet-Version"); return "v" + (v == null ? "2.0" : v);
        }
    }

    void change(Hub h, int i, int d) {
        if (i == 0) Cfg.theme = (Cfg.theme + d + Hub.THEMES.length) % Hub.THEMES.length;
        else if (i == 1) Cfg.accent = (Cfg.accent + d + Hub.ACCENTS.length) % Hub.ACCENTS.length;
        else if (i == 2) Cfg.wall = (Cfg.wall + d + Hub.WALL_NAMES.length) % Hub.WALL_NAMES.length;
        else if (i == 3) Cfg.pack = (Cfg.pack + d + Hub.PACK_NAMES.length) % Hub.PACK_NAMES.length;
        else if (i == 4) Cfg.layout = 1 - Cfg.layout;
        else if (i == 5 && d == 0) { for (int j = 0; j < Cfg.hi.length; j++) Cfg.hi[j] = 0; h.toast("Scores reset"); }
        else if (i == 6 && d == 0) h.toast("Open a new .jar to update");
        else return;
        Cfg.save();
        h.refresh();
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        int a = h.act(k);
        if (a == Canvas.UP && sel > 0) sel--;
        else if (a == Canvas.DOWN && sel < ROWS.length - 1) sel++;
        else if (a == Canvas.LEFT) change(h, sel, -1);
        else if (a == Canvas.RIGHT) change(h, sel, 1);
        else if (a == Canvas.FIRE || h.isLeft(k)) change(h, sel, 0);
    }

    void paint(Graphics g, Hub h) {
        int ch = h.fb.getHeight() + 10, gap = 4;
        for (int i = 0; i < ROWS.length; i++) {
            int y = h.headH + 8 + i * (ch + gap);
            h.card(g, 10, y, h.w - 20, ch, i == sel);
            g.setFont(h.fb); g.setColor(h.cTxt);
            g.drawString(ROWS[i], 20, y + 5, Hub.TL);
            g.setColor(h.cAcc);
            g.drawString(i < 5 ? "< " + val(h, i) + " >" : val(h, i), h.w - 20, y + 5, Hub.TR);
        }
        int py = h.headH + 8 + ROWS.length * (ch + gap) + 12, sz = 34;
        int[] ids = {0, 3, 5, 8};
        for (int i = 0; i < 4; i++) h.icon(g, ids[i], h.w * (2 * i + 1) / 8, py + sz / 2, sz);
        h.drawHeader(g, "Settings", null);
        h.drawBar(g, "Change", "< > values", "Back");
    }
}
