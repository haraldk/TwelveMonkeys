/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;
import org.junit.Test;
import org.mockito.internal.matchers.LessOrEqual;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JPEGSegmentImageInputStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegmentImageInputStreamTest.java,v 1.0 30.01.12 22:14 haraldk Exp$
 */
public class JPEGSegmentImageInputStreamTest {
    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    private URL getClassLoaderResource(final String pName) {
        return getClass().getResource(pName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNull() {
        new JPEGSegmentImageInputStream(null);
    }

    @Test(expected = IIOException.class)
    public void testStreamNonJPEG() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(new ByteArrayInputStream(new byte[] {42, 42, 0, 0, 77, 99})));
        stream.read();
    }

    @Test(expected = IIOException.class)
    public void testStreamNonJPEGArray() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(new ByteArrayInputStream(new byte[] {42, 42, 0, 0, 77, 99})));
        stream.readFully(new byte[1]);
    }

    @Test(expected = IIOException.class)
    public void testStreamEmpty() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(new ByteArrayInputStream(new byte[0])));
        stream.read();
    }

    @Test(expected = IIOException.class)
    public void testStreamEmptyArray() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(new ByteArrayInputStream(new byte[0])));
        stream.readFully(new byte[1]);
    }

    @Test
    public void testStreamRealData() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/invalid-icc-duplicate-sequence-numbers-rgb-internal-kodak-srgb-jfif.jpg")));
        assertEquals(JPEG.SOI, stream.readUnsignedShort());
        assertEquals(JPEG.DQT, stream.readUnsignedShort());
    }

    @Test
    public void testStreamRealDataArray() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/invalid-icc-duplicate-sequence-numbers-rgb-internal-kodak-srgb-jfif.jpg")));
        byte[] bytes = new byte[20];

        // NOTE: read(byte[], int, int) must always read len bytes (or until EOF), due to known bug in Sun code
        assertEquals(20, stream.read(bytes, 0, 20));

        assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB, 0x0, 0x43, 0x0, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1}, bytes);
    }

    @Test
    public void testStreamRealDataLength() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/cmm-exception-adobe-rgb.jpg")));

        long length = 0;
        while (stream.read() != -1) {
            length++;
        }

        assertThat(length, new LessOrEqual<>(10203L)); // In no case should length increase

        assertEquals(9607L, length); // May change, if more chunks are passed to reader...
    }

    @Test
    public void testAppSegmentsFiltering() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/no-image-types-rgb-us-web-coated-v2-ms-photogallery-exif.jpg")));
        List<JPEGSegment> appSegments = JPEGSegmentUtil.readSegments(stream, JPEGSegmentUtil.APP_SEGMENTS);

        assertEquals(2, appSegments.size());

        assertEquals(JPEG.APP1, appSegments.get(0).marker());
        assertEquals("Exif", appSegments.get(0).identifier());

        assertEquals(JPEG.APP14, appSegments.get(1).marker());
        assertEquals("Adobe", appSegments.get(1).identifier());

        // And thus, no JFIF, no XMP, no ICC_PROFILE or other segments
    }

    @Test
    public void testEOFSOSSegmentBug() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/eof-sos-segment-bug.jpg")));

        long length = 0;
        while (stream.read() != -1) {
            length++;
        }

        assertEquals(9281L, length); // Sanity check: same as file size, except..?
    }

    @Test
    public void testReadPaddedSegmentsBug() throws IOException {
        ImageInputStream stream = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/jfif-padded-segments.jpg")));

        List<JPEGSegment> appSegments = JPEGSegmentUtil.readSegments(stream, JPEGSegmentUtil.APP_SEGMENTS);
        assertEquals(1, appSegments.size());

        assertEquals(JPEG.APP1, appSegments.get(0).marker());
        assertEquals("Exif", appSegments.get(0).identifier());

        stream.seek(0L);

        long length = 0;
        while (stream.read() != -1) {
            length++;
        }

        assertEquals(1061L, length); // Sanity check: same as file size, except padding and the filtered ICC_PROFILE segment
    }

    @Test
    public void testEOFExceptionInSegmentParsingShouldNotCreateBadState2() throws IOException {
        ImageInputStream iis = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/broken-jpeg/51432b02-02a8-11e7-9203-b42b1c43c0c3.jpg")));

        byte[] buffer = new byte[4096];

        // NOTE: This is a simulation of how the native parts of com.sun...JPEGImageReader would read the image...
        assertEquals(2, iis.read(buffer, 0, buffer.length));
        assertEquals(2, iis.getStreamPosition());

        iis.seek(2000); // Just a random postion beyond EOF
        assertEquals(2000, iis.getStreamPosition());

        // So far, so good (but stream position is now really beyond EOF)...

        // This however, will blow up with an EOFException internally (but we'll return -1 to be good)
        assertEquals(-1, iis.read(buffer, 0, buffer.length));
        assertEquals(-1, iis.read());
        assertEquals(2000, iis.getStreamPosition());

        // Again, should just continue returning -1 for ever
        assertEquals(-1, iis.read());
        assertEquals(-1, iis.read(buffer, 0, buffer.length));
        assertEquals(2000, iis.getStreamPosition());
    }

    @Test
    public void testEOFExceptionInSegmentParsingShouldNotCreateBadState() throws IOException {
        ImageInputStream iis = new JPEGSegmentImageInputStream(ImageIO.createImageInputStream(getClassLoaderResource("/broken-jpeg/broken-no-sof-ascii-transfer-mode.jpg")));

        byte[] buffer = new byte[4096];

        // NOTE: This is a simulation of how the native parts of com.sun...JPEGImageReader would read the image...
        assertEquals(2, iis.read(buffer, 0, buffer.length));
        assertEquals(2, iis.getStreamPosition());

        iis.seek(0x2012); // bad segment length, should have been 0x0012, not 0x2012
        assertEquals(0x2012, iis.getStreamPosition());

        // So far, so good (but stream position is now really beyond EOF)...

        // This however, will blow up with an EOFException internally (but we'll return -1 to be good)
        assertEquals(-1, iis.read(buffer, 0, buffer.length));
        assertEquals(-1, iis.read());
        assertEquals(0x2012, iis.getStreamPosition());

        // Again, should just continue returning -1 for ever
        assertEquals(-1, iis.read(buffer, 0, buffer.length));
        assertEquals(-1, iis.read());
        assertEquals(0x2012, iis.getStreamPosition());
    }
}
