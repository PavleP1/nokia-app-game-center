import javax.microedition.lcdui.*;
import java.io.*;
import java.util.TimeZone;
import java.util.Vector;

final class Task {
    String name;
    int streak;
    long last;
}

final class TodoApp extends App {
    final Vector tasks = new Vector();
    int sel, scroll;
    boolean adding, delArmed;
    long delTime;
    final Typer t = new Typer();

    boolean repeat() { return true; }

    long today() { return (System.currentTimeMillis() + TimeZone.getDefault().getRawOffset()) / 86400000L; }

    void enter(Hub h) {
        t.multiline = false;
        t.max = 24;
        byte[] d = Cfg.read("mtodo");
        if (d == null) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(d));
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                Task k = new Task();
                k.name = in.readUTF(); k.streak = in.readInt(); k.last = in.readLong();
                tasks.addElement(k);
            }
        } catch (Exception e) {}
    }

    void save() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeInt(tasks.size());
            for (int i = 0; i < tasks.size(); i++) {
                Task k = (Task) tasks.elementAt(i);
                o.writeUTF(k.name); o.writeInt(k.streak); o.writeLong(k.last);
            }
            o.flush();
            Cfg.write("mtodo", bo.toByteArray());
        } catch (Exception e) {}
    }

    int ch(Hub h) { return h.fb.getHeight() + h.fs.getHeight() + 14; }

    void ensure(Hub h) {
        int gap = 8, view = h.h - h.barH - h.headH - gap * 2;
        int selY = sel * (ch(h) + gap);
        if (selY < scroll) scroll = selY;
        if (selY + ch(h) > scroll + view) scroll = selY + ch(h) - view;
        if (scroll < 0) scroll = 0;
    }

    void paint(Graphics g, Hub h) {
        long td = today();
        int gap = 8, ch = ch(h), done = 0;
        for (int i = 0; i < tasks.size(); i++) {
            Task k = (Task) tasks.elementAt(i);
            boolean dn = k.last == td;
            if (dn) done++;
            int y = h.headH + gap + i * (ch + gap) - scroll;
            if (y + ch < h.headH || y > h.h - h.barH) continue;
            h.card(g, 10, y, h.w - 20, ch, i == sel && !adding);
            int bx = 20, by = y + (ch - 16) / 2;
            g.setColor(dn ? 0x30D158 : h.cSub);
            g.drawRoundRect(bx, by, 16, 16, 5, 5);
            if (dn) { g.fillRoundRect(bx + 3, by + 3, 11, 11, 3, 3); }
            g.setFont(h.fb); g.setColor(h.cTxt);
            g.drawString(h.fit(k.name, h.fb, h.w - 80), 46, y + 5, Hub.TL);
            g.setFont(h.fs); g.setColor(h.cSub);
            g.drawString("Streak " + k.streak + (dn ? "  done today" : ""), 46, y + 7 + h.fb.getHeight(), Hub.TL);
        }
        if (tasks.size() == 0) {
            g.setFont(h.fs); g.setColor(h.cSub);
            g.drawString("No tasks. Press New.", h.w / 2, h.h / 2, Hub.TC);
        }
        if (adding) {
            int bh = h.fb.getHeight() + h.fs.getHeight() + 26, y = h.h / 2 - bh / 2;
            h.card(g, 14, y, h.w - 28, bh, true);
            g.setFont(h.fs); g.setColor(h.cSub);
            g.drawString("New task  " + Typer.MODES[t.mode], 26, y + 8, Hub.TL);
            g.setFont(h.fb); g.setColor(h.cTxt);
            g.drawString(h.fit(t.text() + "_", h.fb, h.w - 56), 26, y + 12 + h.fs.getHeight(), Hub.TL);
        }
        h.drawHeader(g, "To-do", done + "/" + tasks.size());
        if (adding) h.drawBar(g, "Add", "# Mode", "Cancel");
        else h.drawBar(g, "New", "FIRE: done", "Back");
    }

    void key(int k, Hub h) {
        if (adding) {
            if (h.isLeft(k)) {
                String s = t.text().trim();
                if (s.length() > 0) { Task n = new Task(); n.name = s; tasks.addElement(n); sel = tasks.size() - 1; save(); ensure(h); }
                adding = false;
            } else if (h.isRight(k)) adding = false;
            else t.key(k);
            return;
        }
        if (h.isRight(k)) { h.back(); return; }
        if (h.isLeft(k)) { t.set("", 1); adding = true; return; }
        if (h.isClear(k) || k == Canvas.KEY_NUM0) {
            if (tasks.size() == 0) return;
            long now = System.currentTimeMillis();
            if (delArmed && now - delTime < 3000) {
                tasks.removeElementAt(sel);
                if (sel >= tasks.size()) sel = tasks.size() - 1;
                if (sel < 0) sel = 0;
                delArmed = false; save(); ensure(h); h.toast("Deleted");
            } else { delArmed = true; delTime = now; h.toast("Press again to delete"); }
            return;
        }
        int a = h.act(k);
        if (a == Canvas.UP && sel > 0) { sel--; delArmed = false; }
        else if (a == Canvas.DOWN && sel < tasks.size() - 1) { sel++; delArmed = false; }
        else if (a == Canvas.FIRE && tasks.size() > 0) {
            Task tk = (Task) tasks.elementAt(sel);
            long td = today();
            if (tk.last == td) { tk.streak--; tk.last = tk.streak > 0 ? td - 1 : 0; }
            else { tk.streak = (tk.last == td - 1) ? tk.streak + 1 : 1; tk.last = td; }
            save();
        }
        ensure(h);
    }
}
