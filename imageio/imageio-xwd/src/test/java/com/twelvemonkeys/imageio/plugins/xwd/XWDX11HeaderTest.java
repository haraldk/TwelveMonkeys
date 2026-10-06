package com.twelvemonkeys.imageio.plugins.xwd;

import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XWDX11HeaderTest {
    private static byte[] createXWD(int bitsPerPixel, int bytesPerLine, int visualClass, int bitsPerRGB, int colorMapEntries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);

        output.writeInt(X11.X11_HEADER_SIZE + 4);
        output.writeInt(X11.X11_HEADER_VERSION);
        output.writeInt(2);  // format
        output.writeInt(24); // depth
        output.writeInt(4);  // width
        output.writeInt(4);  // height
        output.writeInt(0);  // xOffset
        output.writeInt(1);  // byteOrder
        output.writeInt(32); // unit
        output.writeInt(1);  // bitOrder
        output.writeInt(32); // pad
        output.writeInt(bitsPerPixel);
        output.writeInt(bytesPerLine);
        output.writeInt(visualClass);
        output.writeInt(0xff0000);
        output.writeInt(0xff00);
        output.writeInt(0xff);
        output.writeInt(bitsPerRGB);
        output.writeInt(0); // numColors
        output.writeInt(colorMapEntries);
        output.writeInt(4); // windowWidth
        output.writeInt(4); // windowHeight
        output.writeInt(0); // windowX
        output.writeInt(0); // windowY
        output.writeInt(0); // windowBorderWidth
        output.write(new byte[4]); // windowName
        output.write(new byte[64 * 12]); // color map + pixel data

        return bytes.toByteArray();
    }

    private static XWDX11Header readHeader(byte[] data) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            return XWDX11Header.read(input);
        }
    }

    @Test
    void readValid() throws IOException {
        XWDX11Header header = readHeader(createXWD(32, 16, X11.VISUAL_CLASS_TRUE_COLOR, 8, 0));

        assertEquals(4, header.width);
        assertEquals(4, header.height);
        assertEquals(4, header.numComponents());
    }

    @Test
    void readNegativeColorMapEntries() {
        assertThrows(IIOException.class, () -> readHeader(createXWD(32, 16, X11.VISUAL_CLASS_TRUE_COLOR, 8, -1)));
    }

    @Test
    void readColorMapEntriesOverflow() {
        // 12 * 0x15555556 overflows to 8
        assertThrows(IIOException.class, () -> readHeader(createXWD(8, 4, X11.VISUAL_CLASS_PSEUDO_COLOR, 8, 0x15555556)));
    }

    @Test
    void readColorMapBitsPerRGBTooLarge() {
        assertThrows(IIOException.class, () -> readHeader(createXWD(8, 4, X11.VISUAL_CLASS_PSEUDO_COLOR, 24, 16)));
    }

    @Test
    void readZeroBitsPerRGB() {
        assertThrows(IIOException.class, () -> readHeader(createXWD(32, 16, X11.VISUAL_CLASS_TRUE_COLOR, 0, 0)));
    }

    @Test
    void readTooManyComponents() {
        assertThrows(IIOException.class, () -> readHeader(createXWD(32, 16, X11.VISUAL_CLASS_TRUE_COLOR, 1, 0)));
    }

    @Test
    void readNegativeBytesPerLine() {
        assertThrows(IIOException.class, () -> readHeader(createXWD(32, -1, X11.VISUAL_CLASS_TRUE_COLOR, 8, 0)));
    }

    @Test
    void readBytesPerLineTooSmall() {
        assertThrows(IIOException.class, () -> readHeader(createXWD(32, 4, X11.VISUAL_CLASS_TRUE_COLOR, 8, 0)));
    }
}
