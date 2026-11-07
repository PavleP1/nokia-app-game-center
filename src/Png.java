import java.io.*;
import javax.microedition.lcdui.Image;

final class Png {
    private static int crc;
    private static int[] tab;

    private static void wInt(OutputStream o, int v) throws IOException {
        o.write((v >>> 24) & 255); o.write((v >>> 16) & 255); o.write((v >>> 8) & 255); o.write(v & 255);
    }
    private static void begin(OutputStream o, String type, int len) throws IOException {
        wInt(o, len);
        crc = -1;
        byte[] tb = new byte[4];
        for (int i = 0; i < 4; i++) tb[i] = (byte) type.charAt(i);
        put(o, tb, 0, 4);
    }
    private static void put(OutputStream o, byte[] b, int off, int len) throws IOException {
        o.write(b, off, len);
        if (tab == null) {
            tab = new int[256];
            for (int n = 0; n < 256; n++) {
                int c = n;
                for (int k = 0; k < 8; k++) c = ((c & 1) != 0) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
                tab[n] = c;
            }
        }
        int c2 = crc;
        for (int i = 0; i < len; i++) c2 = tab[(c2 ^ b[off + i]) & 255] ^ (c2 >>> 8);
        crc = c2;
    }
    private static void end(OutputStream o) throws IOException { wInt(o, ~crc); }

    static void write(Image img, OutputStream o) throws IOException {
        int iw = img.getWidth(), ih = img.getHeight();
        byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        o.write(sig, 0, 8);
        byte[] hd = new byte[13];
        hd[0] = (byte) (iw >> 24); hd[1] = (byte) (iw >> 16); hd[2] = (byte) (iw >> 8); hd[3] = (byte) iw;
        hd[4] = (byte) (ih >> 24); hd[5] = (byte) (ih >> 16); hd[6] = (byte) (ih >> 8); hd[7] = (byte) ih;
        hd[8] = 8; hd[9] = 2;
        begin(o, "IHDR", 13); put(o, hd, 0, 13); end(o);
        int rb = 1 + 3 * iw;
        begin(o, "IDAT", 2 + ih * (5 + rb) + 4);
        byte[] zh = {0x78, 0x01};
        put(o, zh, 0, 2);
        int a = 1, b = 0;
        int[] pix = new int[iw];
        byte[] row = new byte[rb];
        byte[] bh = new byte[5];
        for (int y = 0; y < ih; y++) {
            img.getRGB(pix, 0, iw, 0, y, iw, 1);
            row[0] = 0;
            for (int x = 0; x < iw; x++) {
                int p = pix[x];
                row[1 + 3 * x] = (byte) (p >> 16);
                row[2 + 3 * x] = (byte) (p >> 8);
                row[3 + 3 * x] = (byte) p;
            }
            bh[0] = (byte) (y == ih - 1 ? 1 : 0);
            bh[1] = (byte) (rb & 255); bh[2] = (byte) ((rb >> 8) & 255);
            bh[3] = (byte) (~rb & 255); bh[4] = (byte) ((~rb >> 8) & 255);
            put(o, bh, 0, 5);
            put(o, row, 0, rb);
            for (int i = 0; i < rb; i++) {
                a = (a + (row[i] & 255)) % 65521;
                b = (b + a) % 65521;
            }
        }
        byte[] ad = {(byte) (b >> 8), (byte) b, (byte) (a >> 8), (byte) a};
        put(o, ad, 0, 4);
        end(o);
        begin(o, "IEND", 0); end(o);
    }
}
