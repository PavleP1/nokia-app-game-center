import javax.microedition.lcdui.*;
import java.util.Calendar;

final class HomeApp extends App {
    static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    int sel, scroll, lastMin = -1;

    int cols() { return Cfg.layout == 0 ? 3 : 1; }
    int isz(Hub h) { int s = h.w / 6; if (s < 28) s = 28; if (s > 44) s = 44; return s; }
    int rowH(Hub h) { return Cfg.layout == 0 ? isz(h) + h.fs.getHeight() + 20 : isz(h) + 14; }
    int area0(Hub h) { return h.headH + 8 + h.fbig.getHeight() + 10; }

    boolean repeat() { return true; }
    int tickMs() { return 1000; }
    boolean tick(Hub h, long now) {
        int m = Calendar.getInstance().get(Calendar.MINUTE);
        if (m != lastMin) { lastMin = m; return true; }
        return false;
    }
    void resume(Hub h) { ensure(h); }

    void ensure(Hub h) {
        int rh = rowH(h), row = sel / cols();
        int view = h.h - h.barH - area0(h) - 4;
        int rt = row * rh;
        if (rt < scroll) scroll = rt;
        if (rt + rh > scroll + view) scroll = rt + rh - view;
        if (scroll < 0) scroll = 0;
    }

    void paint(Graphics g, Hub h) {
        Calendar c = Calendar.getInstance();
        int top = h.headH + 8;
        g.setFont(h.fbig);
        g.setColor(h.cTxt);
        g.drawString(Hub.two(c.get(Calendar.HOUR_OF_DAY)) + ":" + Hub.two(c.get(Calendar.MINUTE)), 14, top, Hub.TL);
        g.setFont(h.fs);
        g.setColor(h.cSub);
        g.drawString(DAYS[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)],
                h.w - 14, top + (h.fbig.getHeight() - h.fs.getHeight()) / 2, Hub.TR);
        int cs = cols(), rh = rowH(h), sz = isz(h), a0 = area0(h), cw = h.w / cs;
        for (int i = 0; i < Hub.NAPPS; i++) {
            int x = (i % cs) * cw, y = a0 + (i / cs) * rh - scroll;
            if (y + rh < a0 || y > h.h - h.barH) continue;
            if (i == sel) h.card(g, x + 6, y, cw - 12, rh - 4, true);
            if (cs == 3) {
                h.icon(g, i, x + cw / 2, y + 6 + sz / 2, sz);
                g.setFont(h.fs);
                g.setColor(h.cTxt);
                g.drawString(h.fit(Hub.APP_NAMES[i], h.fs, cw - 6), x + cw / 2, y + 6 + sz + 4, Hub.TC);
            } else {
                h.icon(g, i, 18 + sz / 2, y + (rh - 4) / 2, sz);
                g.setFont(h.fb);
                g.setColor(h.cTxt);
                g.drawString(Hub.APP_NAMES[i], 30 + sz, y + (rh - 4 - h.fb.getHeight()) / 2, Hub.TL);
            }
        }
        h.drawHeader(g, "Modern Hub", null);
        h.drawBar(g, "Open", "", "Exit");
    }

    void key(int k, Hub h) {
        if (k == Canvas.KEY_POUND || h.isRight(k)) { h.midlet.exit(); return; }
        int a = h.act(k), cs = cols();
        if (h.isLeft(k) || a == Canvas.FIRE) { h.open(h.make(sel)); return; }
        if (a == Canvas.LEFT && sel > 0) sel--;
        else if (a == Canvas.RIGHT && sel < Hub.NAPPS - 1) sel++;
        else if (a == Canvas.UP && sel - cs >= 0) sel -= cs;
        else if (a == Canvas.DOWN && sel < Hub.NAPPS - 1) sel = Math.min(sel + cs, Hub.NAPPS - 1);
        ensure(h);
    }
}
