/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.path;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * PathsTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PathsTest.java,v 1.0 12/12/14 harald.kuhr Exp$
 */
public class PathsTest {
    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadPathNull() throws IOException {
        Paths.readPath(null);
    }

    @Test
    public void testReadPathUnknown() throws IOException {
        assertNull(Paths.readPath(new ByteArrayImageInputStream(new byte[42])));
    }

    @Test
    public void testGrapeJPEG() throws IOException {
        ImageInputStream stream = resourceAsIIOStream("/jpeg/grape_with_path.jpg");

        Path2D path = Paths.readPath(stream);

        assertNotNull(path);
        assertPathEquals(readExpectedPath("/ser/grape-path.ser"), path);
    }

    @Test
    public void testGrapePSD() throws IOException {
        ImageInputStream stream = resourceAsIIOStream("/psd/grape_with_path.psd");

        Path2D path = Paths.readPath(stream);

        assertNotNull(path);
        assertPathEquals(readExpectedPath("/ser/grape-path.ser"), path);

    }

    @Test
    public void testGrapeTIFF() throws IOException {
        ImageInputStream stream = resourceAsIIOStream("/tiff/little-endian-grape_with_path.tif");

        Path2D path = Paths.readPath(stream);

        assertNotNull(path);
        assertPathEquals(readExpectedPath("/ser/grape-path.ser"), path);
    }

    @Test
    public void testMultipleTIFF() throws IOException {
        ImageInputStream stream = resourceAsIIOStream("/tiff/big-endian-multiple-clips.tif");

        Shape path = Paths.readPath(stream);

        assertNotNull(path);
    }

    @Test
    public void testGrape8BIM() throws IOException {
        ImageInputStream stream = resourceAsIIOStream("/psd/grape_with_path.psd");

        // PSD image resources from position 34, length 32598
        stream.seek(34);
        stream = new SubImageInputStream(stream, 32598);

        Path2D path = Paths.readPath(stream);

        assertNotNull(path);
        assertPathEquals(readExpectedPath("/ser/grape-path.ser"), path);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplyClippingPathNullPath() {
        Paths.applyClippingPath(null, new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApplyClippingPathNullSource() {
        Paths.applyClippingPath(new GeneralPath(), null);
    }

    @Test
    public void testApplyClippingPath() throws IOException {
        BufferedImage source = new BufferedImage(20, 20, BufferedImage.TYPE_3BYTE_BGR);

        Path2D path = readExpectedPath("/ser/grape-path.ser");

        BufferedImage image = Paths.applyClippingPath(path, source);

        assertNotNull(image);
        // Same dimensions as original
        assertEquals(source.getWidth(), image.getWidth());
        assertEquals(source.getHeight(), image.getHeight());
        // Transparent
        assertEquals(Transparency.TRANSLUCENT, image.getColorModel().getTransparency());

        // Corners (at least) should be transparent
        assertEquals(0, image.getRGB(0, 0));
        assertEquals(0, image.getRGB(source.getWidth() - 1, 0));
        assertEquals(0, image.getRGB(0, source.getHeight() - 1));
        assertEquals(0, image.getRGB(source.getWidth() - 1, source.getHeight() - 1));

        // Center opaque
        assertEquals(0xff, image.getRGB(source.getWidth() / 2, source.getHeight() / 2) >>> 24);

        // TODO: Mor sophisticated test that tests all pixels outside path...
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalArgumentException.class)
    public void testApplyClippingPathNullDestination() {
        Paths.applyClippingPath(new GeneralPath(), new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY), null);
    }

    @Test
    public void testApplyClippingPathCustomDestination() throws IOException {
        BufferedImage source = new BufferedImage(20, 20, BufferedImage.TYPE_3BYTE_BGR);

        Path2D path = readExpectedPath("/ser/grape-path.ser");

        // Destination is intentionally larger than source
        BufferedImage destination = new BufferedImage(30, 30, BufferedImage.TYPE_4BYTE_ABGR);
        BufferedImage image = Paths.applyClippingPath(path, source, destination);

        assertSame(destination, image);

        // Corners (at least) should be transparent
        assertEquals(0, image.getRGB(0, 0));
        assertEquals(0, image.getRGB(image.getWidth() - 1, 0));
        assertEquals(0, image.getRGB(0, image.getHeight() - 1));
        assertEquals(0, image.getRGB(image.getWidth() - 1, image.getHeight() - 1));

        // "inner" corners
        assertEquals(0, image.getRGB(source.getWidth() - 1, 0));
        assertEquals(0, image.getRGB(0, source.getHeight() - 1));
        assertEquals(0, image.getRGB(source.getWidth() - 1, source.getHeight() - 1));

        // Center opaque
        assertEquals(0xff, image.getRGB(source.getWidth() / 2, source.getHeight() / 2) >>> 24);

        // TODO: Mor sophisticated test that tests all pixels outside path...
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadClippedNull() throws IOException {
        Paths.readClipped(null);
    }

    @Test
    public void testReadClipped() throws IOException {
        BufferedImage image = Paths.readClipped(resourceAsIIOStream("/jpeg/grape_with_path.jpg"));

        assertNotNull(image);
        // Same dimensions as original
        assertEquals(857, image.getWidth());
        assertEquals(1800, image.getHeight());
        // Transparent
        assertEquals(Transparency.TRANSLUCENT, image.getColorModel().getTransparency());

        // Corners (at least) should be transparent
        assertEquals(0, image.getRGB(0, 0));
        assertEquals(0, image.getRGB(image.getWidth() - 1, 0));
        assertEquals(0, image.getRGB(0, image.getHeight() - 1));
        assertEquals(0, image.getRGB(image.getWidth() - 1, image.getHeight() - 1));

        // Center opaque
        assertEquals(0xff, image.getRGB(image.getWidth() / 2, image.getHeight() / 2) >>> 24);

        // TODO: Mor sophisticated test that tests all pixels outside path...
    }

    // TODO: Test read image without path, as no-op

    static ImageInputStream resourceAsIIOStream(String name) throws IOException {
        return ImageIO.createImageInputStream(PathsTest.class.getResource(name));
    }

    static Path2D readExpectedPath(final String resource) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(PathsTest.class.getResourceAsStream(resource))) {
            return (Path2D) ois.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    static void assertPathEquals(final Path2D expectedPath, final Path2D actualPath) {
        assertNotNull("Expected path is null, check your tests...", expectedPath);
        assertNotNull(actualPath);

        PathIterator expectedIterator = expectedPath.getPathIterator(null);
        PathIterator actualIterator = actualPath.getPathIterator(null);

        float[] expectedCoords = new float[6];
        float[] actualCoords = new float[6];

        while(!expectedIterator.isDone()) {
            assertFalse("Less points than expected", actualIterator.isDone());

            int expectedType = expectedIterator.currentSegment(expectedCoords);
            int actualType = actualIterator.currentSegment(actualCoords);

            assertEquals("Unexpected segment type", expectedType, actualType);
            assertArrayEquals("Unexpected coordinates", expectedCoords, actualCoords, 0);

            actualIterator.next();
            expectedIterator.next();
        }

        assertTrue("More points than expected", actualIterator.isDone());
    }

    @Test
    public void testWriteJPEG() throws IOException {
        Path2D originalPath = readExpectedPath("/ser/multiple-clips.ser");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(bytes)) {
            boolean written = Paths.writeClipped(image, originalPath, "JPEG", stream);
            assertTrue(written);
        }
        assertTrue(bytes.size() > 1024); // Actual size may be plugin specific...

        Path2D actualPath = Paths.readPath(new ByteArrayImageInputStream(bytes.toByteArray()));
        assertPathEquals(originalPath, actualPath);
    }

    @Test
    public void testWriteTIFF() throws IOException {
        Path2D originalPath = readExpectedPath("/ser/grape-path.ser");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(bytes)) {
            boolean written = Paths.writeClipped(image, originalPath, "TIFF", stream);
            assumeTrue(written); // TIFF support is optional
        }

        assertTrue(bytes.size() > 1024); // Actual size may be plugin specific...

        Path2D actualPath = Paths.readPath(new ByteArrayImageInputStream(bytes.toByteArray()));
        assertPathEquals(originalPath, actualPath);
    }
}
