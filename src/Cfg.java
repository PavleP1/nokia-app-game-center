import javax.microedition.rms.RecordStore;
import java.io.*;

final class Cfg {
    static int theme, accent, wall, pack, layout, track, sfxOff, quiet;
    static final int[] hi = new int[8];   // 0 snake, 1 2048, 2 breakout, 3 mines(best secs), 4 tetris

    static byte[] read(String name) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(name, true);
            if (rs.getNumRecords() > 0) return rs.getRecord(1);
        } catch (Exception e) {
        } finally {
            if (rs != null) try { rs.closeRecordStore(); } catch (Exception e) {}
        }
        return null;
    }

    static void write(String name, byte[] d) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(name, true);
            if (rs.getNumRecords() == 0) rs.addRecord(d, 0, d.length);
            else rs.setRecord(1, d, 0, d.length);
        } catch (Exception e) {
        } finally {
            if (rs != null) try { rs.closeRecordStore(); } catch (Exception e) {}
        }
    }

    static void load() {
        byte[] d = read("mhub");
        if (d != null) {
            try {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(d));
                theme = in.readInt(); accent = in.readInt(); wall = in.readInt();
                pack = in.readInt(); layout = in.readInt();
                for (int i = 0; i < hi.length; i++) hi[i] = in.readInt();
                track = in.readInt(); sfxOff = in.readInt(); quiet = in.readInt();
            } catch (Exception e) {}
        }
        if (theme < 0 || theme >= Hub.THEMES.length) theme = 0;
        if (accent < 0 || accent >= Hub.ACCENTS.length) accent = 0;
        if (wall < 0 || wall >= Hub.WALL_NAMES.length) wall = 0;
        if (pack < 0 || pack >= Hub.PACK_NAMES.length) pack = 0;
        if (layout < 0 || layout > 1) layout = 0;
        if (track < 0 || track > Midi.NAMES.length) track = 0;
        if (sfxOff < 0 || sfxOff > 1) sfxOff = 0;
        if (quiet < 0 || quiet > 4) quiet = 0;
    }

    static void save() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeInt(theme); o.writeInt(accent); o.writeInt(wall);
            o.writeInt(pack); o.writeInt(layout);
            for (int i = 0; i < hi.length; i++) o.writeInt(hi[i]);
            o.writeInt(track); o.writeInt(sfxOff); o.writeInt(quiet);
            o.flush();
            write("mhub", bo.toByteArray());
        } catch (Exception e) {}
    }
}
