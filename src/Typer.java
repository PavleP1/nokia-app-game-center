import javax.microedition.lcdui.Canvas;

final class Typer {
    static final String[] KEYS = {" 0", ".,?!'-@1", "abc2", "def3", "ghi4", "jkl5", "mno6", "pqrs7", "tuv8", "wxyz9"};
    static final String[] MODES = {"abc", "Abc", "ABC", "123"};
    StringBuffer buf = new StringBuffer();
    int mode = 1, max = 2000;
    boolean multiline = true;
    private int lastKey = -1, keyIdx;
    private long lastTime;
    private boolean curUpper;

    void set(String s, int m) { buf = new StringBuffer(s); mode = m; lastKey = -1; }
    String text() { return buf.toString(); }

    boolean key(int k) {
        if (k >= Canvas.KEY_NUM0 && k <= Canvas.KEY_NUM9) { type(k - Canvas.KEY_NUM0); return true; }
        if (k == Canvas.KEY_POUND) { mode = (mode + 1) % 4; lastKey = -1; return true; }
        if (k == Canvas.KEY_STAR) { if (multiline) add('\n'); return true; }
        if (k == -8) { back(); return true; }
        return false;
    }

    void add(char c) {
        if (buf.length() >= max) return;
        buf.append(c);
        lastKey = -1;
    }

    void back() {
        if (buf.length() > 0) buf.setLength(buf.length() - 1);
        lastKey = -1;
    }

    private void type(int n) {
        if (mode == 3) { add((char) ('0' + n)); return; }
        String set = KEYS[n];
        long now = System.currentTimeMillis();
        if (n == lastKey && now - lastTime < 900 && buf.length() > 0) {
            keyIdx = (keyIdx + 1) % set.length();
            char c = set.charAt(keyIdx);
            buf.setCharAt(buf.length() - 1, curUpper ? Character.toUpperCase(c) : c);
        } else {
            if (buf.length() >= max) return;
            keyIdx = 0;
            curUpper = (mode == 1 || mode == 2);
            char c = set.charAt(0);
            buf.append(curUpper ? Character.toUpperCase(c) : c);
            if (mode == 1) mode = 0;
        }
        lastKey = n;
        lastTime = now;
    }
}
