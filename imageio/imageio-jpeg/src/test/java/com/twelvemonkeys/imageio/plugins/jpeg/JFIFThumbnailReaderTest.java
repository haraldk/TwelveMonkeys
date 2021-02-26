/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;

import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * JFIFThumbnailReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JFIFThumbnailReaderTest.java,v 1.0 04.05.12 15:56 haraldk Exp$
 */
public class JFIFThumbnailReaderTest extends AbstractThumbnailReaderTest {

    @Override
    protected ThumbnailReader createReader(ImageInputStream stream) throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(stream, JPEG.APP0, "JFIF");
        stream.close();

        assertNotNull(segments);
        assertFalse(segments.isEmpty());

        JPEGSegment segment = segments.get(0);

        return JFIFThumbnail.from(JFIF.read(new DataInputStream(segment.segmentData()), segment.segmentLength()), listener);
    }

    @Test
    public void testFromNull() {
        assertNull(JFIFThumbnail.from(null, listener));

        verify(listener, never()).warningOccurred(anyString());
    }

    @Test
    public void testFromNullThumbnail() {
        assertNull(JFIFThumbnail.from(new JFIF(1, 1, 0, 1, 1, 0, 0, null), listener));

        verify(listener, never()).warningOccurred(anyString());
    }

    @Test
    public void testFromEmpty() {
        assertNull(JFIFThumbnail.from(new JFIF(1, 1, 0, 1, 1, 0, 0, new byte[0]), listener));

        verify(listener, never()).warningOccurred(anyString());
    }

    @Test
    public void testFromTruncated() {
        assertNull(JFIFThumbnail.from(new JFIF(1, 1, 0, 1, 1, 255, 170, new byte[99]), listener));

        verify(listener, only()).warningOccurred(anyString());
    }

    @Test
    public void testFromValid() throws IOException {
        ThumbnailReader reader = JFIFThumbnail.from(new JFIF(1, 1, 0, 1, 1, 30, 20, new byte[30 * 20 * 3]), listener);
        assertNotNull(reader);

        verify(listener, never()).warningOccurred(anyString());

        // Sanity check below
        assertEquals(30, reader.getWidth());
        assertEquals(20, reader.getHeight());
        assertNotNull(reader.read());
    }

    @Test
    public void testReadRaw() throws IOException {
        ThumbnailReader reader = createReader(createStream("/jpeg/jfif-jfif-and-exif-thumbnail-sharpshot-iphone.jpg"));

        assertEquals(131, reader.getWidth());
        assertEquals(122, reader.getHeight());

        BufferedImage thumbnail = reader.read();
        assertNotNull(thumbnail);
        assertEquals(131, thumbnail.getWidth());
        assertEquals(122, thumbnail.getHeight());
    }

    @Test
    public void testReadNonSpecGray() throws IOException {
        ThumbnailReader reader = createReader(createStream("/jpeg/jfif-grayscale-thumbnail.jpg"));

        assertEquals(127, reader.getWidth());
        assertEquals(76, reader.getHeight());

        BufferedImage thumbnail = reader.read();
        assertNotNull(thumbnail);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, thumbnail.getType());
        assertEquals(127, thumbnail.getWidth());
        assertEquals(76, thumbnail.getHeight());
    }
}
