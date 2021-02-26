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

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * EXIFThumbnailReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFThumbnailReaderTest.java,v 1.0 04.05.12 15:55 haraldk Exp$
 */
public class EXIFThumbnailReaderTest extends AbstractThumbnailReaderTest {

    private final ImageReader thumbnailReader = ImageIO.getImageReadersByFormatName("jpeg").next();

    @Override
    protected ThumbnailReader createReader(final ImageInputStream stream) throws IOException {
        List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(stream, JPEG.APP1, "Exif");
        stream.close();

        assertNotNull(segments);
        assertFalse(segments.isEmpty());

        JPEGSegment exifSegment = segments.get(0);
        InputStream data = exifSegment.segmentData();
        byte[] exifData = new byte[exifSegment.segmentLength() - 2];
        new DataInputStream(data).readFully(exifData);

        EXIF exif = new EXIF(exifData);
        return EXIFThumbnail.from(exif, (CompoundDirectory) new TIFFReader().read(exif.exifData()), thumbnailReader, listener);
    }

    @Test
    public void testReadJPEG() throws IOException {
        ThumbnailReader reader = createReader(createStream("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"));

        assertEquals(114, reader.getWidth());
        assertEquals(160, reader.getHeight());

        BufferedImage thumbnail = reader.read();
        assertNotNull(thumbnail);
        assertEquals(114, thumbnail.getWidth());
        assertEquals(160, thumbnail.getHeight());
    }

    @Test
    public void testReadRaw() throws IOException {
        ThumbnailReader reader = createReader(createStream("/jpeg/exif-rgb-thumbnail-sony-d700.jpg"));

        assertEquals(80, reader.getWidth());
        assertEquals(60, reader.getHeight());

        BufferedImage thumbnail = reader.read();
        assertNotNull(thumbnail);
        assertEquals(80, thumbnail.getWidth());
        assertEquals(60, thumbnail.getHeight());
    }
}
