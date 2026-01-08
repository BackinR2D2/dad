package ro.ase.dad.c03;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BmpSplitMerge {

    static class Parsed {
        byte[] header;
        int dataOffset;
        int width;
        int height;        // signed
        int absHeight;
        boolean topDown;
        int bpp;
        int compression;
        int rowStride;
        byte[] pixels;
    }

    static Parsed parse(byte[] bmpBytes) {
        if (bmpBytes == null || bmpBytes.length < 54) throw new IllegalArgumentException("Invalid BMP");
        if (bmpBytes[0] != 'B' || bmpBytes[1] != 'M') throw new IllegalArgumentException("Not a BMP");

        ByteBuffer bb = ByteBuffer.wrap(bmpBytes).order(ByteOrder.LITTLE_ENDIAN);

        int dataOffset = bb.getInt(10);
        int dibSize = bb.getInt(14);
        if (dibSize < 40) throw new IllegalArgumentException("Unsupported DIB: " + dibSize);

        int width = bb.getInt(18);
        int height = bb.getInt(22);
        short planes = bb.getShort(26);
        short bpp = bb.getShort(28);
        int compression = bb.getInt(30);

        if (planes != 1) throw new IllegalArgumentException("Invalid planes");
        if (!(bpp == 24 || bpp == 32)) throw new IllegalArgumentException("Unsupported bpp " + bpp);
        if (compression != 0) throw new IllegalArgumentException("Unsupported compression " + compression);

        boolean topDown = height < 0;
        int absHeight = Math.abs(height);

        int bytesPerPixel = bpp / 8;
        int rowNoPad = width * bytesPerPixel;
        int rowStride = ((rowNoPad + 3) / 4) * 4;

        int pixelsLen = rowStride * absHeight;
        if (dataOffset + pixelsLen > bmpBytes.length) throw new IllegalArgumentException("Pixel data out of range");

        Parsed p = new Parsed();
        p.dataOffset = dataOffset;
        p.width = width;
        p.height = height;
        p.absHeight = absHeight;
        p.topDown = topDown;
        p.bpp = bpp;
        p.compression = compression;
        p.rowStride = rowStride;

        p.header = new byte[dataOffset];
        System.arraycopy(bmpBytes, 0, p.header, 0, dataOffset);

        p.pixels = new byte[pixelsLen];
        System.arraycopy(bmpBytes, dataOffset, p.pixels, 0, pixelsLen);

        return p;
    }

    static byte[] build(Parsed base, int newAbsHeight, boolean topDown, byte[] newPixels) {
        byte[] header = base.header.clone();
        ByteBuffer hb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

        int signedH = topDown ? -newAbsHeight : newAbsHeight;
        int newImageSize = newPixels.length;
        int newFileSize = header.length + newImageSize;

        hb.putInt(2, newFileSize);
        hb.putInt(18, base.width);
        hb.putInt(22, signedH);
        hb.putInt(34, newImageSize);

        byte[] out = new byte[newFileSize];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(newPixels, 0, out, header.length, newPixels.length);
        return out;
    }

    public static class Split {
        public byte[] topBmp;
        public byte[] bottomBmp;
    }

    public static Split splitHalf(byte[] bmpBytes) {
        Parsed p = parse(bmpBytes);

        int topH = p.absHeight / 2;
        int bottomH = p.absHeight - topH;

        byte[] topPixels = new byte[p.rowStride * topH];
        byte[] bottomPixels = new byte[p.rowStride * bottomH];

        for (int visualY = 0; visualY < p.absHeight; visualY++) {
            int srcRowIndexInFile = p.topDown ? visualY : (p.absHeight - 1 - visualY);
            int srcBase = srcRowIndexInFile * p.rowStride;

            if (visualY < topH) {
                int dstVisualY = visualY;
                int dstRowIndexInFile = p.topDown ? dstVisualY : (topH - 1 - dstVisualY);
                System.arraycopy(p.pixels, srcBase, topPixels, dstRowIndexInFile * p.rowStride, p.rowStride);
            } else {
                int dstVisualY = visualY - topH;
                int dstRowIndexInFile = p.topDown ? dstVisualY : (bottomH - 1 - dstVisualY);
                System.arraycopy(p.pixels, srcBase, bottomPixels, dstRowIndexInFile * p.rowStride, p.rowStride);
            }
        }

        Split s = new Split();
        s.topBmp = build(p, topH, p.topDown, topPixels);
        s.bottomBmp = build(p, bottomH, p.topDown, bottomPixels);
        return s;
    }

    public static byte[] mergeVertical(byte[] topBmp, byte[] bottomBmp) {
        Parsed t = parse(topBmp);
        Parsed b = parse(bottomBmp);

        if (t.width != b.width) throw new IllegalArgumentException("Cannot merge: different widths");
        if (t.bpp != b.bpp) throw new IllegalArgumentException("Cannot merge: different bpp");
        if (t.rowStride != b.rowStride) throw new IllegalArgumentException("Cannot merge: different rowStride");
        if (t.topDown != b.topDown) throw new IllegalArgumentException("Cannot merge: different orientation");

        int mergedH = t.absHeight + b.absHeight;
        byte[] mergedPixels = new byte[t.rowStride * mergedH];

        for (int visualY = 0; visualY < mergedH; visualY++) {
            boolean fromTop = visualY < t.absHeight;
            Parsed src = fromTop ? t : b;
            int srcVisualY = fromTop ? visualY : (visualY - t.absHeight);

            int srcRowIndexInFile = src.topDown ? srcVisualY : (src.absHeight - 1 - srcVisualY);
            int srcBase = srcRowIndexInFile * src.rowStride;

            int dstRowIndexInFile = t.topDown ? visualY : (mergedH - 1 - visualY);
            int dstBase = dstRowIndexInFile * t.rowStride;

            System.arraycopy(src.pixels, srcBase, mergedPixels, dstBase, src.rowStride);
        }

        return build(t, mergedH, t.topDown, mergedPixels);
    }
}
