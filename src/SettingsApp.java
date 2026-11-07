import javax.microedition.lcdui.*;

final class SettingsApp extends App {
    static final String[] ROWS = {"Theme", "Accent color", "Wallpaper", "Icon pack", "Home layout", "Music", "Sound effects", "Volume", "Reset high scores", "About"};
    int sel, scroll;

    boolean repeat() { return true; }
    void leave(Hub h) { Cfg.save(); }

    String val(Hub h, int i) {
        switch (i) {
            case 0: return Hub.THEME_NAMES[Cfg.theme];
            case 1: return Hub.ACC_NAMES[Cfg.accent];
            case 2: return Hub.WALL_NAMES[Cfg.wall];
            case 3: return Hub.PACK_NAMES[Cfg.pack];
            case 4: return Cfg.layout == 0 ? "Grid" : "List";
            case 5: return Cfg.track == 0 ? "Off" : Midi.NAMES[Cfg.track - 1];
            case 6: return Cfg.sfxOff == 0 ? "On" : "Off";
            case 7: return (100 - Cfg.quiet * 20) + "%";
            case 8: return "FIRE";
            default: String v = h.midlet.prop("MIDlet-Version"); return "v" + (v == null ? "2.1" : v);
        }
    }

    void change(Hub h, int i, int d) {
        if (i == 0) Cfg.theme = (Cfg.theme + d + Hub.THEMES.length) % Hub.THEMES.length;
        else if (i == 1) Cfg.accent = (Cfg.accent + d + Hub.ACCENTS.length) % Hub.ACCENTS.length;
        else if (i == 2) Cfg.wall = (Cfg.wall + d + Hub.WALL_NAMES.length) % Hub.WALL_NAMES.length;
        else if (i == 3) Cfg.pack = (Cfg.pack + d + Hub.PACK_NAMES.length) % Hub.PACK_NAMES.length;
        else if (i == 4) Cfg.layout = 1 - Cfg.layout;
        else if (i == 5) {
            int dd = d == 0 ? 1 : d, n = Midi.NAMES.length + 1;
            Cfg.track = (Cfg.track + dd + n) % n;
            Snd.wake();
        } else if (i == 6) {
            Cfg.sfxOff = 1 - Cfg.sfxOff;
            if (Cfg.sfxOff == 0) Snd.sfx(Snd.BLIP);
        } else if (i == 7) {
            if (d == 0) Cfg.quiet = (Cfg.quiet + 4) % 5;
            else Cfg.quiet -= d;
            if (Cfg.quiet < 0) Cfg.quiet = 0;
            if (Cfg.quiet > 4) Cfg.quiet = 4;
            Snd.wake();
            Snd.sfx(Snd.BLIP);
        } else if (i == 8 && d == 0) {
            for (int j = 0; j < Cfg.hi.length; j++) Cfg.hi[j] = 0;
            h.toast("Scores reset");
        } else if (i == 9 && d == 0) {
            h.toast("Open a new .jar to update");
        } else return;
        Cfg.save();
        h.refresh();
    }

    void ensure(Hub h) {
        int ch = h.fb.getHeight() + 10, rh = ch + 4;
        int view = h.h - h.barH - h.headH - 12;
        int top = sel * rh;
        if (top < scroll) scroll = top;
        if (top + ch > scroll + view) scroll = top + ch - view;
        if (scroll < 0) scroll = 0;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        int a = h.act(k);
        if (a == Canvas.UP && sel > 0) { sel--; ensure(h); }
        else if (a == Canvas.DOWN && sel < ROWS.length - 1) { sel++; ensure(h); }
        else if (a == Canvas.LEFT) change(h, sel, -1);
        else if (a == Canvas.RIGHT) change(h, sel, 1);
        else if (a == Canvas.FIRE || h.isLeft(k)) change(h, sel, 0);
    }

    void paint(Graphics g, Hub h) {
        int ch = h.fb.getHeight() + 10, rh = ch + 4;
        for (int i = 0; i < ROWS.length; i++) {
            int y = h.headH + 8 + i * rh - scroll;
            if (y + ch < h.headH || y > h.h - h.barH) continue;
            h.card(g, 10, y, h.w - 20, ch, i == sel);
            g.setFont(h.fb);
            g.setColor(h.cTxt);
            g.drawString(ROWS[i], 20, y + 5, Hub.TL);
            g.setColor(h.cAcc);
            g.drawString(i < 8 ? "< " + val(h, i) + " >" : val(h, i), h.w - 20, y + 5, Hub.TR);
        }
        h.drawHeader(g, "Settings", null);
        h.drawBar(g, "Change", "< > values", "Back");
    }
}a
