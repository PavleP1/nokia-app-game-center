import javax.microedition.lcdui.*;

final class AlarmApp extends App {
    int field;

    boolean repeat() { return true; }

    void paint(Graphics g, Hub h) {
        int bw = h.fbig.stringWidth("00") + 20, bh = h.fbig.getHeight() + 16, sepW = h.fbig.stringWidth(":") + 10;
        int x0 = (h.w - (bw * 2 + sepW)) / 2, y0 = h.headH + 24;
        for (int i = 0; i < 2; i++) {
            int x = x0 + i * (bw + sepW);
            h.card(g, x, y0, bw, bh, field == i);
            g.setFont(h.fbig); g.setColor(h.cTxt);
            g.drawString(Hub.two(i == 0 ? h.alH : h.alM), x + bw / 2, y0 + 8, Hub.TC);
        }
        g.setFont(h.fbig); g.setColor(h.cTxt);
        g.drawString(":", x0 + bw + sepW / 2, y0 + 8, Hub.TC);
        int py = y0 + bh + 20, ph = h.fb.getHeight() + 14;
        for (int i = 0; i < 2; i++) {
            int y = py + i * (ph + 8);
            boolean on = i == 0 ? h.alOn : h.alDaily;
            h.card(g, 12, y, h.w - 24, ph, field == i + 2);
            g.setFont(h.fb); g.setColor(h.cTxt);
            g.drawString(i == 0 ? "Alarm" : "Repeat daily", 24, y + 7, Hub.TL);
            g.setColor(on ? 0x30D158 : h.cSub);
            g.drawString(on ? "ON" : "OFF", h.w - 24, y + 7, Hub.TR);
        }
        g.setFont(h.fs); g.setColor(h.cSub);
        String info = "2/8 change, 4/6 move";
        if (h.alOn && h.alTime > 0) {
            long m = (h.alTime - System.currentTimeMillis()) / 60000L;
            if (m < 0) m = 0;
            info = "Rings in " + (m / 60) + "h " + (m % 60) + "m";
        }
        g.drawString(info, h.w / 2, py + 2 * (ph + 8) + 6, Hub.TC);
        h.drawHeader(g, "Alarm", h.alOn ? "ON" : "OFF");
        h.drawBar(g, "Save", "", "Back");
    }

    void key(int k, Hub h) {
        if (h.isLeft(k) || h.isRight(k)) { h.armAlarm(true); h.back(); return; }
        int a = h.act(k);
        if (a == Canvas.LEFT && field > 0) field--;
        else if (a == Canvas.RIGHT && field < 3) field++;
        else if (a == Canvas.UP || a == Canvas.DOWN) {
            int d = (a == Canvas.UP) ? 1 : -1;
            if (field == 0) h.alH = (h.alH + d + 24) % 24;
            else if (field == 1) h.alM = (h.alM + d + 60) % 60;
            else if (field == 2) h.alOn = !h.alOn;
            else h.alDaily = !h.alDaily;
        } else if (a == Canvas.FIRE) {
            if (field == 2) h.alOn = !h.alOn;
            else if (field == 3) h.alDaily = !h.alDaily;
        }
    }
}
