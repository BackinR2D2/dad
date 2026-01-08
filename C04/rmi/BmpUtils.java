import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BmpUtils {

    public static class Bmp {
        public byte[] header;      
        public int dataOffset;     
        public int width;
        public int height;         // pozitiv => bottom-up, negativ => top-down
        public int absHeight;
        public int bpp;            // 24 sau 32
        public int compression;    // 0 = BI_RGB
        public int rowStride;      // bytes per row incl. padding
        public boolean topDown;    // height < 0

        public byte[] pixels;      
    }

    public static Bmp parse(byte[] bmpBytes) {
        if (bmpBytes == null || bmpBytes.length < 54) {
            throw new IllegalArgumentException("Invalid BMP: too small");
        }
        if (bmpBytes[0] != 'B' || bmpBytes[1] != 'M') {
            throw new IllegalArgumentException("Invalid BMP: missing BM signature");
        }

        ByteBuffer bb = ByteBuffer.wrap(bmpBytes).order(ByteOrder.LITTLE_ENDIAN);

        int fileSize = bb.getInt(2);
        int dataOffset = bb.getInt(10);

        int dibSize = bb.getInt(14);
        if (dibSize < 40) {
            throw new IllegalArgumentException("Unsupported DIB header size: " + dibSize);
        }

        int width = bb.getInt(18);
        int height = bb.getInt(22);
        short planes = bb.getShort(26);
        short bpp = bb.getShort(28);
        int compression = bb.getInt(30);

        if (planes != 1) throw new IllegalArgumentException("Invalid BMP planes: " + planes);
        if (!(bpp == 24 || bpp == 32)) throw new IllegalArgumentException("Unsupported BMP bpp: " + bpp);
        if (compression != 0) throw new IllegalArgumentException("Unsupported BMP compression: " + compression);

        boolean topDown = height < 0;
        int absHeight = Math.abs(height);

        int bytesPerPixel = bpp / 8;
        int rowNoPad = width * bytesPerPixel;
        int rowStride = ((rowNoPad + 3) / 4) * 4;

        if (dataOffset <= 0 || dataOffset >= bmpBytes.length) {
            throw new IllegalArgumentException("Invalid BMP data offset: " + dataOffset);
        }
        int expectedPixelsLen = rowStride * absHeight;
        if (dataOffset + expectedPixelsLen > bmpBytes.length) {
            throw new IllegalArgumentException("Invalid BMP: pixel data out of range");
        }

        Bmp bmp = new Bmp();
        bmp.dataOffset = dataOffset;
        bmp.width = width;
        bmp.height = height;
        bmp.absHeight = absHeight;
        bmp.topDown = topDown;
        bmp.bpp = bpp;
        bmp.compression = compression;
        bmp.rowStride = rowStride;

        bmp.header = new byte[dataOffset];
        System.arraycopy(bmpBytes, 0, bmp.header, 0, dataOffset);

        bmp.pixels = new byte[expectedPixelsLen];
        System.arraycopy(bmpBytes, dataOffset, bmp.pixels, 0, expectedPixelsLen);

        return bmp;
    }

    public static byte[] build(Bmp bmp, int newWidth, int newHeightAbs, boolean newTopDown, byte[] newPixels, int newRowStride) {
        // Clone header then patch needed fields: file size, width, height, image size
        byte[] header = bmp.header.clone();
        ByteBuffer hb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

        int newHeightSigned = newTopDown ? -newHeightAbs : newHeightAbs;
        int newImageSize = newRowStride * newHeightAbs;
        int newFileSize = header.length + newImageSize;

        hb.putInt(2, newFileSize);         // bfSize
        hb.putInt(18, newWidth);           // biWidth
        hb.putInt(22, newHeightSigned);    // biHeight
        hb.putInt(34, newImageSize);       

        byte[] out = new byte[newFileSize];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(newPixels, 0, out, header.length, newPixels.length);
        return out;
    }

    public static byte[] zoomNearest(byte[] bmpBytes, int percent, boolean zoomIn) {
        if (percent <= 0) throw new IllegalArgumentException("percent must be > 0");

        Bmp src = parse(bmpBytes);

        double factor = zoomIn ? (1.0 + percent / 100.0) : (1.0 - percent / 100.0);
        if (factor <= 0.05) factor = 0.05; // safety

        int dstW = Math.max(1, (int) Math.round(src.width * factor));
        int dstH = Math.max(1, (int) Math.round(src.absHeight * factor));

        int bpp = src.bpp;
        int bytesPerPixel = bpp / 8;

        int dstRowNoPad = dstW * bytesPerPixel;
        int dstRowStride = ((dstRowNoPad + 3) / 4) * 4;

        byte[] dstPixels = new byte[dstRowStride * dstH];

        // helper: map dst(x,y) -> src(sx,sy)
        for (int y = 0; y < dstH; y++) {
            int sy = (int) Math.floor(y / factor);
            if (sy >= src.absHeight) sy = src.absHeight - 1;

            // BMP bottom-up means row0 in file is bottom row
            int srcRowIndex = src.topDown ? sy : (src.absHeight - 1 - sy);
            int srcRowBase = srcRowIndex * src.rowStride;

            int dstRowIndex = src.topDown ? y : (dstH - 1 - y); // keep same orientation as source
            int dstRowBase = dstRowIndex * dstRowStride;

            for (int x = 0; x < dstW; x++) {
                int sx = (int) Math.floor(x / factor);
                if (sx >= src.width) sx = src.width - 1;

                int srcPix = srcRowBase + sx * bytesPerPixel;
                int dstPix = dstRowBase + x * bytesPerPixel;

                // copy B,G,R,(A)
                for (int k = 0; k < bytesPerPixel; k++) {
                    dstPixels[dstPix + k] = src.pixels[srcPix + k];
                }
            }
            // padding is already 0 (default) => ok
        }

        return build(src, dstW, dstH, src.topDown, dstPixels, dstRowStride);
    }
}
