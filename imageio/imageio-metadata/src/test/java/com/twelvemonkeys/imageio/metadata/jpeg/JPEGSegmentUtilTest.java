/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.jpeg;

import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;
import org.junit.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ICC_Profile;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JPEGSegmentUtilTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGSegmentUtilTest.java,v 1.0 01.03.11 16.22 haraldk Exp$
 */
public class JPEGSegmentUtilTest {
    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
    }

    protected ImageInputStream getData(final String name) throws IOException {
        return ImageIO.createImageInputStream(getClass().getResource(name));
    }

    @Test
    public void testReadAPP0JFIF() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/9788245605525.jpg"), JPEG.APP0, "JFIF");

        assertEquals(1, segments.size());
        JPEGSegment segment = segments.get(0);
        assertEquals(JPEG.APP0, segment.marker());
        assertEquals("JFIF", segment.identifier());
        assertEquals(16, segment.segmentLength());
    }

    @Test
    public void testReadAPP1Exif() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/ts_open_300dpi.jpg"), JPEG.APP1, "Exif");

        assertEquals(1, segments.size());
        JPEGSegment segment = segments.get(0);
        assertEquals(JPEG.APP1, segment.marker());
        assertEquals("Exif", segment.identifier());
    }

    @Test
    public void testReadAPP1XMP() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/ts_open_300dpi.jpg"), JPEG.APP1, "http://ns.adobe.com/xap/1.0/");

        assertEquals(1, segments.size());
        JPEGSegment segment = segments.get(0);
        assertEquals(JPEG.APP1, segment.marker());
        assertEquals("http://ns.adobe.com/xap/1.0/", segment.identifier());
    }

    @Test
    public void testReadAPP13Photoshop() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/ts_open_300dpi.jpg"), JPEG.APP13, "Photoshop 3.0");

        assertEquals(1, segments.size());
        JPEGSegment segment = segments.get(0);
        assertEquals(0xFFED, segment.marker());
        assertEquals("Photoshop 3.0", segment.identifier());
    }

    @Test
    public void testReadAPP14Adobe() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/9788245605525.jpg"), JPEG.APP14, "Adobe");

        assertEquals(1, segments.size());
        JPEGSegment segment = segments.get(0);
        assertEquals(JPEG.APP14, segment.marker());
        assertEquals("Adobe", segment.identifier());
        assertEquals(14, segment.segmentLength());
    }

    @Test
    public void testReadAPP2ICC_PROFILE() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/ts_open_300dpi.jpg"), JPEG.APP2, "ICC_PROFILE");

        assertEquals(18, segments.size());

        for (JPEGSegment segment : segments) {
            assertEquals(JPEG.APP2, segment.marker());
            assertEquals("ICC_PROFILE", segment.identifier());
        }

        // Test that we can actually read the chunked ICC profile
        DataInputStream stream = new DataInputStream(segments.get(0).data());
        int chunkNumber = stream.readUnsignedByte();
        int chunkCount = stream.readUnsignedByte();

        InputStream[] streams = new InputStream[chunkCount];
        streams[chunkNumber - 1] = stream;

        for (int i = 1; i < chunkCount; i++) {
            stream = new DataInputStream(segments.get(i).data());

            chunkNumber = stream.readUnsignedByte();
            if (stream.readUnsignedByte() != chunkCount) {
                throw new IIOException(String.format("Bad number of 'ICC_PROFILE' chunks."));
            }

            streams[chunkNumber - 1] = stream;
        }

        ICC_Profile profile = ICC_Profile.getInstance(new SequenceInputStream(Collections.enumeration(Arrays.asList(streams))));
        assertNotNull("Profile could not be read, probably bad data", profile);
    }

    @Test
    public void testReadAll() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/9788245605525.jpg"), JPEGSegmentUtil.ALL_SEGMENTS);
        assertEquals(7, segments.size());

        assertEquals(segments.toString(), JPEG.SOF0, segments.get(3).marker());
        assertEquals(segments.toString(), null, segments.get(3).identifier());
        assertEquals(segments.toString(), JPEG.SOS, segments.get(segments.size() - 1).marker());
    }

    @Test
    public void testReadAllAlt() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/ts_open_300dpi.jpg"), JPEGSegmentUtil.ALL_SEGMENTS);
        assertEquals(27, segments.size());

        assertEquals(segments.toString(), JPEG.SOF0, segments.get(23).marker());
        assertEquals(segments.toString(), null, segments.get(23).identifier());
        assertEquals(segments.toString(), JPEG.SOS, segments.get(segments.size() - 1).marker());
    }

    @Test
    public void testReadAppMarkers() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/9788245605525.jpg"), JPEGSegmentUtil.APP_SEGMENTS);
        assertEquals(2, segments.size());

        assertEquals(JPEG.APP0, segments.get(0).marker());
        assertEquals("JFIF", segments.get(0).identifier());
        assertEquals(JPEG.APP14, segments.get(1).marker());
        assertEquals("Adobe", segments.get(1).identifier());
    }

    @Test
    public void testReadAppMarkersAlt() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/ts_open_300dpi.jpg"), JPEGSegmentUtil.APP_SEGMENTS);
        assertEquals(22, segments.size());

        assertEquals(JPEG.APP1, segments.get(0).marker());
        assertEquals("Exif", segments.get(0).identifier());
        assertEquals(0xFFED, segments.get(1).marker());
        assertEquals("Photoshop 3.0", segments.get(1).identifier());
        assertEquals(JPEG.APP1, segments.get(2).marker());
        assertEquals("http://ns.adobe.com/xap/1.0/", segments.get(2).identifier());
        assertEquals(JPEG.APP2, segments.get(3).marker());
        assertEquals("ICC_PROFILE", segments.get(3).identifier());
        // ...
        assertEquals(JPEG.APP14, segments.get(21).marker());
        assertEquals("Adobe", segments.get(21).identifier());
    }

    @Test
    public void testReadPaddedSegments() throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(getData("/jpeg/jfif-padded-segments.jpg"), JPEGSegmentUtil.APP_SEGMENTS);
        assertEquals(3, segments.size());

        assertEquals(JPEG.APP0, segments.get(0).marker());
        assertEquals("JFIF", segments.get(0).identifier());
        assertEquals(JPEG.APP2, segments.get(1).marker());
        assertEquals("ICC_PROFILE", segments.get(1).identifier());
        assertEquals(JPEG.APP1, segments.get(2).marker());
        assertEquals("Exif", segments.get(2).identifier());
    }
}
