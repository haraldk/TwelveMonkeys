package com.twelvemonkeys.imageio.plugins.dds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DDSImageWriterTest extends ImageWriterAbstractTest<DDSImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new DDSImageWriterSpi();
    }

    @Override
    protected List<BufferedImage> getTestData() {
        return Arrays.asList(
            new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB),
            new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB),
            new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB),
            new BufferedImage(32, 32, BufferedImage.TYPE_4BYTE_ABGR),
            new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR)
        );
    }

    @Test
    void writeRasters() throws IOException {
        ImageWriter writer = createWriter();

        assertTrue(writer.canWriteRasters());

        // Full tests in super class
    }

    @Test
    void writeMipmap() throws IOException {
        ImageWriter writer = createWriter();

        try {
            assertTrue(writer.canWriteSequence());

            List<BufferedImage> testData = getTestData();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int previousSize = 0;

            try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
                writer.setOutput(stream);
                writer.prepareWriteSequence(null);
                ImageWriteParam param = writer.getDefaultWriteParam();

                assertTrue(buffer.size() > previousSize);
                previousSize = buffer.size();

                for (BufferedImage image : testData) {
                    writer.writeToSequence(new IIOImage(drawSomething(image), null, null), param);
                }

                writer.endWriteSequence();
                assertTrue(buffer.size() > previousSize, "No image data written");
            }
            catch (IOException e) {
                throw new AssertionError(e.getMessage(), e);
            }

            // Verify that we can read the file back...
            ImageReader reader = ImageIO.getImageReader(writer);

            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                stream.seek(0);
                reader.setInput(stream);

                assertEquals(testData.size(), reader.getNumImages(false));

                for (int i = 0; i < testData.size(); i++) {
                    BufferedImage image = reader.read(i, null);

                    assertNotNull(image);
                    assertEquals(testData.get(i).getWidth(), image.getWidth());
                    assertEquals(testData.get(i).getHeight(), image.getHeight());
                }
            }
            finally {
                reader.dispose();
            }

        }
        finally {
            writer.dispose();
        }
    }

    @Test
    void writeMipmapDifferentCompression() throws IOException {
        ImageWriter writer = createWriter();

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
                IIOWriteWarningListener listener = mock();
                writer.addIIOWriteWarningListener(listener);
                writer.setOutput(stream);

                writer.prepareWriteSequence(null);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionType("DXT2");

                // Write first with DXT2
                List<BufferedImage> testData = getTestData();
                writer.writeToSequence(new IIOImage(drawSomething(testData.get(0)), null, null), param);

                // Repeat with different type
                IIOImage image = new IIOImage(drawSomething(testData.get(1)), null, null);
                param.setCompressionType("DXT1");

                writer.writeToSequence(image, param);

                // Verify warning is issued
                verify(listener).warningOccurred(eq(writer), eq(1), anyString());
                verifyNoMoreInteractions(listener);
            }
            catch (IOException e) {
                throw new AssertionError(e.getMessage(), e);
            }
        }
        finally {
            writer.dispose();
        }
    }

    @Test
    void writeMipmapUnexpectedSize() throws IOException {
        ImageWriter writer = createWriter();

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try (ImageOutputStream stream = ImageIO.createImageOutputStream(buffer)) {
                writer.setOutput(stream);

                writer.prepareWriteSequence(null);
                ImageWriteParam param = writer.getDefaultWriteParam();
                BufferedImage testData = getTestData().get(0);

                IIOImage image = new IIOImage(drawSomething(testData), null, null);
                writer.writeToSequence(image, param);

                // Repeat with same size... boom.
                assertThrows(IIOException.class, () -> writer.writeToSequence(image, param));
            }
            catch (IOException e) {
                throw new AssertionError(e.getMessage(), e);
            }
        }
        finally {
            writer.dispose();
        }
    }
}
