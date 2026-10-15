package com.twelvemonkeys.imageio.plugins.jpegxl;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class JXLImageIOTest {

    private static boolean toolOnPath(final String tool) {
        try {
            return new ProcessBuilder(tool, "--version").start().waitFor() == 0
                    || true; // Started: the tool exists on PATH
        }
        catch (Exception e) {
            return false;
        }
    }

    @Test
    public void testReaderSpiRegistered() {
        Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("jxl");
        assertTrue(readers.hasNext(), "JPEG XL ImageReader should be registered");
    }

    @Test
    public void testWriterSpiRegistered() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("jxl");
        assertTrue(writers.hasNext(), "JPEG XL ImageWriter should be registered");
    }

    @Test
    public void testWriteReadRoundTrip() throws Exception {
        assumeTrue(toolOnPath("cjxl"), "cjxl not on PATH");
        assumeTrue(toolOnPath("djxl"), "djxl not on PATH");

        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jxlOut = new ByteArrayOutputStream();
        try (var stream = ImageIO.createImageOutputStream(jxlOut)) {
            ImageWriter writer = ImageIO.getImageWritersBySuffix("jxl").next();
            try {
                writer.setOutput(stream);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), null);
            }
            finally {
                writer.dispose();
            }
        }

        byte[] jxl = jxlOut.toByteArray();
        assertTrue(jxl.length > 2 && jxl[0] == (byte) 0xFF && jxl[1] == 0x0A,
                "Output should start with the JPEG XL codestream signature");

        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(jxl))) {
            ImageReader reader = ImageIO.getImageReadersBySuffix("jxl").next();
            try {
                reader.setInput(in);
                BufferedImage read = reader.read(0);
                assertEquals(16, read.getWidth());
                assertEquals(16, read.getHeight());
            }
            finally {
                reader.dispose();
            }
        }
    }
}
