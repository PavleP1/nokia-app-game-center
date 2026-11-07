import javax.microedition.lcdui.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.Control;
import javax.microedition.media.control.VideoControl;
import java.io.OutputStream;

final class CameraApp extends App {
    static final String[] FILTERS = {"Normal", "Mono", "Sepia", "Invert", "Warm", "Cool", "Vivid", "Fade"};
    Player player;
    VideoControl vc;
    boolean ok, pending = true;
    String err;
    int mode, fi;
    Image photo, filtered;

    boolean live() { return true; }
    boolean wall() { return false; }
    int tickMs() { return pending ? 100 : 0; }
    void enter(Hub h) { mode = 0; pending = true; }
    void leave(Hub h) { stop(); }
    void pause(Hub h) { stop(); }
    void resume(Hub h) { if (mode == 0 && !ok) pending = true; }
    boolean tick(Hub h, long now) {
        if (pending) { pending = false; start(h); return true; }
        return false;
    }

    void start(Hub h) {
        stop();
        try {
            player = Manager.createPlayer("capture://video");
            player.realize();
            Control c = player.getControl("VideoControl");
            if (c == null) c = player.getControl("javax.microedition.media.control.VideoControl");
            vc = (VideoControl) c;
            vc.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, h);
            vc.setDisplayLocation(0, h.headH);
            vc.setDisplaySize(h.w, h.h - h.headH - h.barH);
            vc.setVisible(true);
            player.start();
            ok = true;
            err = null;
        } catch (Throwable e) {
            stop();
            err = "Camera unavailable";
        }
    }

    void stop() {
        ok = false;
        try { if (vc != null) vc.setVisible(false); } catch (Throwable e) {}
        try { if (player != null) { player.stop(); player.close(); } } catch (Throwable e) {}
        player = null;
        vc = null;
    }

    void paint(Graphics g, Hub h) {
        int ah = h.h - h.headH - h.barH;
        if (mode == 0) {
            if (!ok) {
                g.setColor(0x000000);
                g.fillRect(0, h.headH, h.w, ah);
                g.setFont(h.fs); g.setColor(0xFFFFFF);
                g.drawString(err != null ? err : "Starting camera...", h.w / 2, h.h / 2, Hub.TC);
            }
            h.drawHeader(g, "Camera", "LIVE");
            h.drawBar(g, "Snap", "5 = Snap", "Back");
        } else {
            g.setColor(0x000000);
            g.fillRect(0, h.headH, h.w, ah);
            if (filtered != null) {
                g.setClip(0, h.headH, h.w, ah);
                g.drawImage(filtered, (h.w - filtered.getWidth()) / 2, h.headH + (ah - filtered.getHeight()) / 2, Hub.TL);
                g.setClip(0, 0, h.w, h.h);
            }
            h.drawHeader(g, "Photo", FILTERS[fi]);
            h.drawBar(g, "Save", "< 4   6 >  Filter", "Retake");
        }
    }

    void key(int k, Hub h) {
        if (mode == 0) {
            if (h.isRight(k)) { h.back(); return; }
            if (h.isLeft(k) || k == Canvas.KEY_NUM5 || h.act(k) == Canvas.FIRE) snapPhoto(h);
        } else {
            if (h.isLeft(k)) { save(h); return; }
            if (h.isRight(k)) { mode = 0; photo = null; filtered = null; pending = true; return; }
            int a = h.act(k);
            if (a == Canvas.LEFT || a == Canvas.RIGHT) {
                fi = (fi + (a == Canvas.RIGHT ? 1 : FILTERS.length - 1)) % FILTERS.length;
                try { filtered = filter(photo, fi); } catch (Throwable e) { filtered = photo; }
            }
        }
    }

    void snapPhoto(Hub h) {
        if (!ok || vc == null) return;
        String[] opts = {"encoding=jpeg&width=240&height=180", "encoding=png&width=240&height=180", null};
        byte[] raw = null;
        for (int i = 0; i < opts.length && raw == null; i++) {
            try { raw = vc.getSnapshot(opts[i]); } catch (Throwable e) { raw = null; }
        }
        if (raw == null) { h.toast("Capture failed"); return; }
        try { photo = Image.createImage(raw, 0, raw.length); }
        catch (Throwable e) { h.toast("Capture failed"); return; }
        raw = null;
        fi = 0;
        filtered = photo;
        stop();
        mode = 1;
    }

    static int cl(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    static int px(int p, int f) {
        int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
        int l = (r * 30 + g * 59 + b * 11) / 100;
        switch (f) {
            case 1: r = l; g = l; b = l; break;
            case 2: {
                int nr = (r * 393 + g * 769 + b * 189) / 1000;
                int ng = (r * 349 + g * 686 + b * 168) / 1000;
                int nb = (r * 272 + g * 534 + b * 131) / 1000;
                r = nr; g = ng; b = nb;
                break;
            }
            case 3: r = 255 - r; g = 255 - g; b = 255 - b; break;
            case 4: r += 26; b -= 26; break;
            case 5: r -= 26; b += 26; break;
            case 6: r = l + (r - l) * 16 / 10; g = l + (g - l) * 16 / 10; b = l + (b - l) * 16 / 10; break;
            case 7: r = r * 180 / 255 + 50; g = g * 180 / 255 + 50; b = b * 180 / 255 + 50; break;
        }
        return 0xFF000000 | (cl(r) << 16) | (cl(g) << 8) | cl(b);
    }

    static Image filter(Image src, int f) {
        if (f == 0) return src;
        int iw = src.getWidth(), ih = src.getHeight();
        Image out = Image.createImage(iw, ih);
        Graphics og = out.getGraphics();
        int[] row = new int[iw];
        for (int y = 0; y < ih; y++) {
            src.getRGB(row, 0, iw, 0, y, iw, 1);
            for (int x = 0; x < iw; x++) row[x] = px(row[x], f);
            og.drawRGB(row, 0, iw, 0, y, iw, 1, false);
        }
        return out;
    }

    void save(Hub h) {
        FileConnection fc = null;
        OutputStream os = null;
        try {
            String dir = System.getProperty("fileconn.dir.photos");
            if (dir == null || dir.length() == 0) dir = "file:///E:/";
            String name = "MH" + (System.currentTimeMillis() / 1000L) + ".png";
            fc = (FileConnection) Connector.open(dir + name, Connector.READ_WRITE);
            if (!fc.exists()) fc.create();
            os = fc.openOutputStream();
            Png.write(filtered, os);
            os.flush();
            h.toast("Saved " + name);
        } catch (Throwable e) {
            h.toast("Save failed");
        } finally {
            try { if (os != null) os.close(); } catch (Throwable e) {}
            try { if (fc != null) fc.close(); } catch (Throwable e) {}
        }
    }
}
