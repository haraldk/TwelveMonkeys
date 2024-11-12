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

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JFXXThumbnailReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JFXXThumbnailReaderTest.java,v 1.0 04.05.12 15:56 haraldk Exp$
 */
public class JFXXThumbnailReaderTest extends AbstractThumbnailReaderTest {
    private final ImageReader thumbnailReader = ImageIO.getImageReadersByFormatName("jpeg").next();

    @Override
    protected ThumbnailReader createReader(final ImageInputStream stream) throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(stream, JPEG.APP0, "JFXX");
        stream.close();

        assertNotNull(segments);
        assertFalse(segments.isEmpty());

        JPEGSegment jfxx = segments.get(0);
        return JFXXThumbnail.from(JFXX.read(new DataInputStream(jfxx.segmentData()), jfxx.length()), thumbnailReader);
    }

    @AfterEach
    public void tearDown() {
        thumbnailReader.dispose();
    }

    @Test
    public void testFromNull() throws IOException {
        assertNull(JFXXThumbnail.from(null, thumbnailReader));
    }

    @Test
    public void testFromNullThumbnail() throws IOException {
        assertThrows(IIOException.class, () -> JFXXThumbnail.from(new JFXX(JFXX.JPEG, null), thumbnailReader));
    }

    @Test
    public void testFromEmpty() throws IOException {
        assertThrows(IIOException.class, () -> JFXXThumbnail.from(new JFXX(JFXX.JPEG, new byte[0]), thumbnailReader));
    }

    @Test
    public void testFromTruncatedJPEG() throws IOException {
        assertThrows(IIOException.class, () -> JFXXThumbnail.from(new JFXX(JFXX.JPEG, new byte[99]), thumbnailReader));
    }

    @Test
    public void testFromTruncatedRGB() throws IOException {
        byte[] thumbnail = new byte[765];
        thumbnail[0] = (byte) 160;
        thumbnail[1] = 90;

        assertThrows(IIOException.class, () -> JFXXThumbnail.from(new JFXX(JFXX.RGB, thumbnail), thumbnailReader));
    }

    @Test
    public void testFromTruncatedIndexed() throws IOException {
        byte[] thumbnail = new byte[365];
        thumbnail[0] = (byte) 160;
        thumbnail[1] = 90;

        assertThrows(IIOException.class, () -> JFXXThumbnail.from(new JFXX(JFXX.INDEXED, thumbnail), thumbnailReader));
    }

    @Test
    public void testFromValid() throws IOException {
        byte[] thumbnail = new byte[14];
        thumbnail[0] = 2;
        thumbnail[1] = 2;
        ThumbnailReader reader = JFXXThumbnail.from(new JFXX(JFXX.RGB, thumbnail), thumbnailReader);
        assertNotNull(reader);

        // Sanity check below
        assertEquals(2, reader.getWidth());
        assertEquals(2, reader.getHeight());
        assertNotNull(reader.read());
    }

    @Test
    public void testReadJPEG() throws IOException {
        ThumbnailReader reader = createReader(createStream("/jpeg/jfif-jfxx-thumbnail-olympus-d320l.jpg"));

        assertEquals(80, reader.getWidth());
        assertEquals(60, reader.getHeight());

        BufferedImage thumbnail = reader.read();
        assertNotNull(thumbnail);
        assertEquals(80, thumbnail.getWidth());
        assertEquals(60, thumbnail.getHeight());
    }

    // TODO: Test JFXX indexed thumbnail
    // TODO: Test JFXX RGB thumbnail
}
