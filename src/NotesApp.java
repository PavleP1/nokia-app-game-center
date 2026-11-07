import javax.microedition.lcdui.*;
import java.io.*;
import java.util.Vector;

final class NotesApp extends App {
    static Vector notes;
    static int focus = -1;
    int sel, scroll;
    boolean delArmed;
    long delTime;

    static void load() {
        if (notes != null) return;
        notes = new Vector();
        byte[] d = Cfg.read("mnotes");
        if (d != null) {
            try {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(d));
                in.readInt();
                int n = in.readInt();
                for (int i = 0; i < n; i++) notes.addElement(in.readUTF());
            } catch (Exception e) {}
        }
        if (notes.size() == 0) notes.addElement("Welcome to Modern Hub\nNotes, games and tools in one app.\nNotes are saved automatically.");
    }

    static void save() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeInt(Cfg.theme);
            o.writeInt(notes.size());
            for (int i = 0; i < notes.size(); i++) o.writeUTF((String) notes.elementAt(i));
            o.flush();
            Cfg.write("mnotes", bo.toByteArray());
        } catch (Exception e) {}
    }

    boolean repeat() { return true; }
    void enter(Hub h) { load(); }
    void resume(Hub h) {
        if (focus >= 0) { sel = focus; focus = -1; }
        if (sel >= notes.size()) sel = notes.size() - 1;
        if (sel < 0) sel = 0;
        ensure(h);
    }

    int ch(Hub h) { return h.fb.getHeight() + h.fs.getHeight() + 16; }

    void ensure(Hub h) {
        int gap = 8, view = h.h - h.barH - h.headH - gap * 2;
        int selY = sel * (ch(h) + gap);
        if (selY < scroll) scroll = selY;
        if (selY + ch(h) > scroll + view) scroll = selY + ch(h) - view;
        if (scroll < 0) scroll = 0;
    }

    void paint(Graphics g, Hub h) {
        int gap = 8, top = h.headH + gap, ch = ch(h);
        if (notes.size() == 0) {
            g.setFont(h.fs); g.setColor(h.cSub);
            g.drawString("No notes yet", h.w / 2, h.h / 2, Hub.TC);
        }
        for (int i = 0; i < notes.size(); i++) {
            int y = top + i * (ch + gap) - scroll;
            if (y + ch < h.headH || y > h.h - h.barH) continue;
            String s = (String) notes.elementAt(i);
            int nl = s.indexOf('\n');
            String title = nl < 0 ? s : s.substring(0, nl);
            String prev = nl < 0 ? "" : s.substring(nl + 1).replace('\n', ' ').trim();
            if (title.length() == 0) title = "Untitled";
            if (prev.length() == 0) prev = "No additional text";
            h.card(g, 10, y, h.w - 20, ch, i == sel);
            g.setFont(h.fb); g.setColor(h.cTxt);
            g.drawString(h.fit(title, h.fb, h.w - 44), 20, y + 6, Hub.TL);
            g.setFont(h.fs); g.setColor(h.cSub);
            g.drawString(h.fit(prev, h.fs, h.w - 44), 20, y + 8 + h.fb.getHeight(), Hub.TL);
        }
        h.drawHeader(g, "Notes", notes.size() + "");
        h.drawBar(g, "New", "C/0 Delete", "Back");
    }

    void key(int k, Hub h) {
        if (h.isLeft(k)) { h.open(new NoteEdit(-1)); return; }
        if (h.isRight(k)) { h.back(); return; }
        if (h.isClear(k) || k == Canvas.KEY_NUM0) { del(h); return; }
        int a = h.act(k);
        if (a == Canvas.UP && sel > 0) { sel--; delArmed = false; }
        else if (a == Canvas.DOWN && sel < notes.size() - 1) { sel++; delArmed = false; }
        else if (a == Canvas.FIRE) { h.open(new NoteEdit(notes.size() > 0 ? sel : -1)); return; }
        ensure(h);
    }

    void del(Hub h) {
        if (notes.size() == 0) return;
        long now = System.currentTimeMillis();
        if (delArmed && now - delTime < 3000) {
            notes.removeElementAt(sel);
            if (sel >= notes.size()) sel = notes.size() - 1;
            if (sel < 0) sel = 0;
            delArmed = false;
            save();
            ensure(h);
            h.toast("Deleted");
        } else {
            delArmed = true; delTime = now;
            h.toast("Press again to delete");
        }
    }
}

final class NoteEdit extends App {
    final Typer t = new Typer();
    int idx, titleLines, edScroll;
    Vector lines;
    boolean blink = true;
    long blinkTime;

    NoteEdit(int index) { idx = index; }

    boolean repeat() { return true; }
    int tickMs() { return 100; }
    boolean tick(Hub h, long now) {
        if (now - blinkTime > 500) { blink = !blink; blinkTime = now; return true; }
        return false;
    }

    void enter(Hub h) {
        NotesApp.load();
        t.set(idx >= 0 ? (String) NotesApp.notes.elementAt(idx) : "", idx >= 0 ? 0 : 1);
        changed();
    }
    void leave(Hub h) { commit(); NotesApp.save(); }
    void pause(Hub h) { commit(); NotesApp.save(); }

    void changed() {
        lines = null;
        edScroll = 100000;
        blink = true;
        blinkTime = System.currentTimeMillis();
    }

    void commit() {
        String s = t.text();
        Vector n = NotesApp.notes;
        if (s.trim().length() == 0) {
            if (idx >= 0) n.removeElementAt(idx);
            idx = -1;
        } else if (idx >= 0) {
            n.setElementAt(s, idx);
        } else {
            n.insertElementAt(s, 0);
            idx = 0;
        }
        NotesApp.focus = idx;
    }

    void rewrap(Hub h) {
        lines = new Vector();
        String s = t.text();
        Font f = h.fb;
        int start = 0, lastSpace = -1, maxW = h.w - 24;
        boolean seenNl = false;
        titleLines = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                lines.addElement(s.substring(start, i));
                start = i + 1;
                lastSpace = -1;
                if (!seenNl) { seenNl = true; titleLines = lines.size(); f = h.fs; }
            } else {
                if (c == ' ') lastSpace = i;
                if (f.substringWidth(s, start, i - start + 1) > maxW) {
                    int brk = lastSpace > start ? lastSpace + 1 : (i > start ? i : i + 1);
                    lines.addElement(s.substring(start, brk));
                    start = brk;
                    lastSpace = -1;
                }
            }
        }
        lines.addElement(s.substring(start));
        if (!seenNl) titleLines = lines.size();
    }

    void paint(Graphics g, Hub h) {
        if (lines == null) rewrap(h);
        int lh = h.fs.getHeight() + 3, top = h.headH + 8;
        int bottom = h.h - h.barH - h.fs.getHeight() - 8;
        int vis = (bottom - top) / lh, n = lines.size();
        int maxScroll = n > vis ? n - vis : 0;
        if (edScroll > maxScroll) edScroll = maxScroll;
        if (edScroll < 0) edScroll = 0;
        int y = top;
        for (int i = edScroll; i < n && i < edScroll + vis; i++) {
            g.setFont(i < titleLines ? h.fb : h.fs);
            g.setColor(h.cTxt);
            g.drawString((String) lines.elementAt(i), 12, y, Hub.TL);
            y += lh;
        }
        int last = n - 1;
        if (blink && last >= edScroll && last < edScroll + vis) {
            Font f = last < titleLines ? h.fb : h.fs;
            g.setColor(h.cAcc);
            g.fillRect(13 + f.stringWidth((String) lines.elementAt(last)), top + (last - edScroll) * lh, 2, h.fs.getHeight());
        }
        g.setFont(h.fs);
        g.setColor(h.cSub);
        g.drawString(t.buf.length() + "/" + t.max, h.w - 10, h.h - h.barH - h.fs.getHeight() - 3, Hub.TR);
        h.drawHeader(g, "Edit", Typer.MODES[t.mode]);
        h.drawBar(g, "Save", "# Mode  * Enter", "Del");
    }

    void key(int k, Hub h) {
        if (t.key(k)) { changed(); return; }
        if (h.isLeft(k)) { h.back(); return; }
        if (h.isRight(k)) { t.back(); changed(); return; }
        int a = h.act(k);
        if (a == Canvas.FIRE) { t.add('\n'); changed(); }
        else if (a == Canvas.UP && edScroll > 0) edScroll--;
        else if (a == Canvas.DOWN) edScroll++;
    }
}
