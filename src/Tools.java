import javax.microedition.lcdui.*;

final class Num {
    static long parse(String s) {
        boolean neg = false;
        int i = 0;
        if (s.length() > 0 && s.charAt(0) == '-') { neg = true; i = 1; }
        long ip = 0, fp = 0;
        int fd = 0;
        boolean dot = false;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') dot = true;
            else if (c >= '0' && c <= '9') {
                if (!dot) ip = ip * 10 + (c - '0');
                else if (fd < 3) { fp = fp * 10 + (c - '0'); fd++; }
            }
        }
        while (fd < 3) { fp *= 10; fd++; }
        long v = ip * 1000 + fp;
        return neg ? -v : v;
    }

    static String fmt(long v) {
        boolean neg = v < 0;
        if (neg) v = -v;
        long ip = v / 1000, fp = v % 1000;
        String f = (fp < 10 ? "00" : (fp < 100 ? "0" : "")) + fp;
        int e = 3;
        while (e > 0 && f.charAt(e - 1) == '0') e--;
        String r = ip + (e > 0 ? "." + f.substring(0, e) : "");
        return neg ? "-" + r : r;
    }
}

final class ToolsApp extends App {
    static final String[] N = {"Stopwatch", "Timer", "Calculator", "Unit converter"};
    int sel;

    boolean repeat() { return true; }

    void paint(Graphics g, Hub h) {
        int ch = h.fb.getHeight() + 18;
        for (int i = 0; i < N.length; i++) {
            int y = h.headH + 10 + i * (ch + 8);
            h.card(g, 12, y, h.w - 24, ch, i == sel);
            g.setFont(h.fb); g.setColor(h.cTxt);
            g.drawString(N[i], 24, y + 9, Hub.TL);
        }
        h.drawHeader(g, "Tools", null);
        h.drawBar(g, "Open", "", "Back");
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        int a = h.act(k);
        if (a == Canvas.UP && sel > 0) sel--;
        else if (a == Canvas.DOWN && sel < N.length - 1) sel++;
        else if (h.isLeft(k) || a == Canvas.FIRE) {
            if (sel == 0) h.open(new StopwatchApp());
            else if (sel == 1) h.open(new TimerApp());
            else if (sel == 2) h.open(new CalcApp());
            else h.open(new ConvApp());
        }
    }
}

final class StopwatchApp extends App {
    boolean run;
    long start, acc;
    long[] laps = new long[6];
    int nl;

    int tickMs() { return run ? 50 : 0; }
    boolean tick(Hub h, long now) { return run; }
    long el() { return run ? acc + System.currentTimeMillis() - start : acc; }
    static String tf(long ms) {
        return Hub.two((int) (ms / 60000)) + ":" + Hub.two((int) ((ms / 1000) % 60)) + "." + Hub.two((int) ((ms / 10) % 100));
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) {
            if (run) {
                if (nl == 6) { for (int i = 0; i < 5; i++) laps[i] = laps[i + 1]; nl = 5; }
                laps[nl++] = el();
            } else { acc = 0; nl = 0; }
            return;
        }
        if (h.act(k) == Canvas.FIRE) {
            if (run) { acc = el(); run = false; }
            else { start = System.currentTimeMillis(); run = true; }
        }
    }

    void paint(Graphics g, Hub h) {
        g.setFont(h.fbig); g.setColor(h.cTxt);
        g.drawString(tf(el()), h.w / 2, h.headH + 24, Hub.TC);
        g.setFont(h.fs);
        for (int i = 0; i < nl; i++) {
            g.setColor(h.cSub);
            g.drawString("Lap " + (i + 1) + "   " + tf(laps[i]), h.w / 2, h.headH + 24 + h.fbig.getHeight() + 14 + i * (h.fs.getHeight() + 4), Hub.TC);
        }
        h.drawHeader(g, "Stopwatch", run ? "RUN" : "STOP");
        h.drawBar(g, run ? "Lap" : "Reset", "FIRE: start/stop", "Back");
    }
}

final class TimerApp extends App {
    int secs = 300, rem, beeps = 6;
    boolean run, done;
    long end, lastBeep;

    int tickMs() { return (run || (done && beeps < 6)) ? 100 : 0; }

    boolean tick(Hub h, long now) {
        if (run) {
            rem = (int) ((end - now + 999) / 1000);
            if (rem <= 0) { rem = 0; run = false; done = true; beeps = 0; lastBeep = 0; }
            return true;
        }
        if (done && beeps < 6) {
            if (now - lastBeep > 1500) { lastBeep = now; beeps++; h.alert(); }
            return true;
        }
        return false;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { run = false; done = false; beeps = 6; return; }
        if (k >= Canvas.KEY_NUM1 && k <= Canvas.KEY_NUM9 && !run) { secs = (k - Canvas.KEY_NUM0) * 60; done = false; return; }
        int a = h.act(k);
        if (!run && !done) {
            if (a == Canvas.UP) secs += 60;
            else if (a == Canvas.DOWN && secs > 60) secs -= 60;
            else if (a == Canvas.RIGHT) secs += 10;
            else if (a == Canvas.LEFT && secs > 10) secs -= 10;
        }
        if (a == Canvas.FIRE) {
            if (done) { done = false; beeps = 6; }
            else if (run) { secs = rem; run = false; }
            else { run = true; end = System.currentTimeMillis() + secs * 1000L; rem = secs; }
        }
    }

    void paint(Graphics g, Hub h) {
        int s = run ? rem : secs;
        g.setFont(h.fbig); g.setColor(done ? 0xFF453A : h.cTxt);
        g.drawString(Hub.two(s / 60) + ":" + Hub.two(s % 60), h.w / 2, h.headH + 30, Hub.TC);
        g.setFont(h.fs); g.setColor(h.cSub);
        g.drawString(done ? "Time's up! FIRE to stop" : (run ? "Running" : "2/8 min, 4/6 sec, 1-9 = minutes"), h.w / 2, h.headH + 30 + h.fbig.getHeight() + 14, Hub.TC);
        h.drawHeader(g, "Timer", run ? "RUN" : "SET");
        h.drawBar(g, "Reset", "FIRE: start", "Back");
    }
}

final class CalcApp extends App {
    static final String[] B = {"7", "8", "9", "/", "4", "5", "6", "x", "1", "2", "3", "-", "0", ".", "=", "+"};
    long acc;
    int op, cc = 12;
    StringBuffer inp = new StringBuffer("0");
    boolean fresh = true;
    String err;

    boolean repeat() { return true; }

    void clear() { acc = 0; op = 0; inp = new StringBuffer("0"); fresh = true; err = null; }

    void apply() {
        long b = Num.parse(inp.toString());
        if (op == 1) acc += b;
        else if (op == 2) acc -= b;
        else if (op == 3) acc = acc * b / 1000;
        else if (op == 4) {
            if (b == 0) { clear(); err = "Divide by 0"; return; }
            acc = acc * 1000 / b;
        }
        inp = new StringBuffer(Num.fmt(acc));
        fresh = true;
    }

    void press(String s) {
        char c = s.charAt(0);
        err = null;
        if ((c >= '0' && c <= '9') || c == '.') {
            if (fresh) { inp.setLength(0); fresh = false; }
            String cs = inp.toString();
            int dot = cs.indexOf('.');
            if (c == '.') { if (dot >= 0) return; if (cs.length() == 0) inp.append('0'); }
            else if (cs.equals("0")) inp.setLength(0);
            else if (dot >= 0 && cs.length() - dot > 3) return;
            if (inp.length() >= 12) return;
            inp.append(c);
            return;
        }
        if (c == '=') {
            if (op != 0 && !fresh) { apply(); op = 0; }
            return;
        }
        int o = c == '+' ? 1 : c == '-' ? 2 : c == 'x' ? 3 : 4;
        if (op != 0 && !fresh) { apply(); if (err != null) return; }
        else acc = Num.parse(inp.toString());
        op = o;
        fresh = true;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { clear(); return; }
        if (k >= Canvas.KEY_NUM0 && k <= Canvas.KEY_NUM9) { press("" + (char) ('0' + k - Canvas.KEY_NUM0)); return; }
        if (k == Canvas.KEY_STAR) { press("."); return; }
        if (k == Canvas.KEY_POUND) { press("="); return; }
        if (h.isClear(k)) {
            if (!fresh && inp.length() > 0) { inp.setLength(inp.length() - 1); if (inp.length() == 0) inp.append('0'); }
            return;
        }
        int a = h.act(k), col = cc % 4, row = cc / 4;
        if (a == Canvas.LEFT && col > 0) cc--;
        else if (a == Canvas.RIGHT && col < 3) cc++;
        else if (a == Canvas.UP && row > 0) cc -= 4;
        else if (a == Canvas.DOWN && row < 3) cc += 4;
        else if (a == Canvas.FIRE) press(B[cc]);
    }

    void paint(Graphics g, Hub h) {
        int dh = h.fbig.getHeight() + h.fs.getHeight() + 14;
        h.card(g, 10, h.headH + 8, h.w - 20, dh, false);
        g.setFont(h.fs); g.setColor(h.cSub);
        String top = op == 0 ? "" : Num.fmt(acc) + " " + B[op == 1 ? 15 : op == 2 ? 11 : op == 3 ? 7 : 3];
        g.drawString(top, h.w - 20, h.headH + 12, Hub.TR);
        g.setFont(h.fbig); g.setColor(err != null ? 0xFF453A : h.cTxt);
        g.drawString(h.fit(err != null ? err : inp.toString(), h.fbig, h.w - 40), h.w - 20, h.headH + 12 + h.fs.getHeight(), Hub.TR);
        int gap = 6, gy = h.headH + dh + 18;
        int bw = (h.w - gap * 5) / 4, bh = (h.h - h.barH - gy - gap * 4) / 4;
        for (int i = 0; i < 16; i++) {
            int x = gap + (i % 4) * (bw + gap), y = gy + (i / 4) * (bh + gap);
            boolean opk = i % 4 == 3 || i == 14;
            if (i == cc) { g.setColor(h.cAcc); g.fillRoundRect(x - 2, y - 2, bw + 4, bh + 4, 14, 14); }
            g.setColor(opk ? Hub.mix(h.cCard, h.cAcc, 90) : h.cCard);
            g.fillRoundRect(x, y, bw, bh, 12, 12);
            g.setFont(h.fb); g.setColor(h.cTxt);
            g.drawString(B[i], x + bw / 2, y + (bh - h.fb.getHeight()) / 2, Hub.TC);
        }
        h.drawHeader(g, "Calculator", null);
        h.drawBar(g, "Clear", "C = back", "Back");
    }
}

final class ConvApp extends App {
    static final String[] N = {"km > mi", "mi > km", "m > ft", "ft > m", "cm > in", "in > cm", "kg > lb", "lb > kg", "L > gal", "gal > L", "C > F", "F > C"};
    static final int[] F = {6214, 16093, 32808, 3048, 3937, 25400, 22046, 4536, 2642, 37854, 0, 0};
    int p;
    StringBuffer inp = new StringBuffer();

    boolean repeat() { return true; }

    long conv(long v) {
        if (p == 10) return v * 9 / 5 + 32000;
        if (p == 11) return (v - 32000) * 5 / 9;
        return v * F[p] / 10000;
    }

    void key(int k, Hub h) {
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { inp.setLength(0); return; }
        if (k >= Canvas.KEY_NUM0 && k <= Canvas.KEY_NUM9) { if (inp.length() < 10) inp.append((char) ('0' + k - Canvas.KEY_NUM0)); return; }
        if (k == Canvas.KEY_STAR) { if (inp.toString().indexOf('.') < 0 && inp.length() < 10) inp.append(inp.length() == 0 ? "0." : "."); return; }
        if (k == Canvas.KEY_POUND) {
            if (inp.length() > 0 && inp.charAt(0) == '-') inp.deleteCharAt(0); else inp.insert(0, '-');
            return;
        }
        if (h.isClear(k)) { if (inp.length() > 0) inp.setLength(inp.length() - 1); return; }
        int a = h.act(k);
        if (a == Canvas.UP || a == Canvas.LEFT) p = (p + N.length - 1) % N.length;
        else if (a == Canvas.DOWN || a == Canvas.RIGHT) p = (p + 1) % N.length;
    }

    void paint(Graphics g, Hub h) {
        int ch = h.fb.getHeight() + h.fs.getHeight() + 18;
        int y = h.headH + 10;
        h.card(g, 12, y, h.w - 24, ch, true);
        g.setFont(h.fs); g.setColor(h.cSub);
        g.drawString("Convert (up/down changes)", 24, y + 6, Hub.TL);
        g.setFont(h.fb); g.setColor(h.cTxt);
        g.drawString("< " + N[p] + " >", 24, y + 8 + h.fs.getHeight(), Hub.TL);
        y += ch + 10;
        h.card(g, 12, y, h.w - 24, ch, false);
        g.setFont(h.fs); g.setColor(h.cSub);
        g.drawString("Value", 24, y + 6, Hub.TL);
        g.setFont(h.fb); g.setColor(h.cTxt);
        g.drawString(inp.length() == 0 ? "0" : inp.toString(), 24, y + 8 + h.fs.getHeight(), Hub.TL);
        y += ch + 10;
        h.card(g, 12, y, h.w - 24, ch, false);
        g.setFont(h.fs); g.setColor(h.cSub);
        g.drawString("Result", 24, y + 6, Hub.TL);
        g.setFont(h.fb); g.setColor(h.cAcc);
        g.drawString(Num.fmt(conv(Num.parse(inp.toString()))), 24, y + 8 + h.fs.getHeight(), Hub.TL);
        h.drawHeader(g, "Converter", null);
        h.drawBar(g, "Clear", "* dot  # +/-", "Back");
    }
}
