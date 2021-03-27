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

import com.twelvemonkeys.imageio.metadata.AbstractCompoundDirectory;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.metadata.tiff.IFD;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;

import org.junit.After;
import org.junit.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
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

    @After
    public void tearDown() {
        thumbnailReader.dispose();
    }

    @Test
    public void testFromNullSegment() throws IOException {
        assertNull(EXIFThumbnail.from(null, null, thumbnailReader));
    }

    @Test
    public void testFromNullIFD() throws IOException {
        assertNull(EXIFThumbnail.from(new EXIF(new byte[0]), null, thumbnailReader));
    }

    @Test
    public void testFromEmptyIFD() throws IOException {
        assertNull(EXIFThumbnail.from(new EXIF(new byte[0]), new EXIFDirectory(), thumbnailReader));
    }

    @Test
    public void testFromSingleIFD() throws IOException {
        assertNull(EXIFThumbnail.from(new EXIF(new byte[42]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList())), thumbnailReader));
    }

    @Test(expected = IIOException.class)
    public void testFromMissingThumbnail() throws IOException {
        EXIFThumbnail.from(new EXIF(new byte[42]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(Collections.<Entry>emptyList())), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromUnsupportedThumbnailCompression() throws IOException {
        List<TIFFEntry> entries = Collections.singletonList(new TIFFEntry(TIFF.TAG_COMPRESSION, 42));
        EXIFThumbnail.from(new EXIF(new byte[42]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromMissingOffsetUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, 16),
                new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, 9)
        );
        EXIFThumbnail.from(new EXIF(new byte[6 + 16 * 9 * 3]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromMissingWidthUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, 0),
                new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, 9)
        );
        EXIFThumbnail.from(new EXIF(new byte[6 + 16 * 9 * 3]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromMissingHeightUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, 0),
                new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, 16)
        );
        EXIFThumbnail.from(new EXIF(new byte[6 + 16 * 9 * 3]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromUnsupportedPhotometricUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, 0),
                new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, 16),
                new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, 9),
                new TIFFEntry(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, 42)
        );
        EXIFThumbnail.from(new EXIF(new byte[6 + 16 * 9 * 3]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromUnsupportedBitsPerSampleUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, 0),
                new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, 16),
                new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, 9),
                new TIFFEntry(TIFF.TAG_BITS_PER_SAMPLE, new int[]{5, 6, 5})
        );
        EXIFThumbnail.from(new EXIF(new byte[6 + 16 * 9 * 3]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromUnsupportedSamplesPerPixelUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, 0),
                new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, 160),
                new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, 90),
                new TIFFEntry(TIFF.TAG_SAMPLES_PER_PIXEL, 1)
        );
        EXIFThumbnail.from(new EXIF(new byte[6 + 16 * 9]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromTruncatedUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, 0),
                new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, 160),
                new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, 90)
        );
        EXIFThumbnail.from(new EXIF(new byte[42]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test
    public void testValidUncompressed() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 1),
                new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, 0),
                new TIFFEntry(TIFF.TAG_IMAGE_WIDTH, 16),
                new TIFFEntry(TIFF.TAG_IMAGE_HEIGHT, 9)
        );

        ThumbnailReader reader = EXIFThumbnail.from(new EXIF(new byte[6 + 16 * 9 * 3]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
        assertNotNull(reader);

        // Sanity check below
        assertEquals(16, reader.getWidth());
        assertEquals(9, reader.getHeight());
        assertNotNull(reader.read());
    }

    @Test(expected = IIOException.class)
    public void testFromMissingOffsetJPEG() throws IOException {
        List<TIFFEntry> entries = Collections.singletonList(new TIFFEntry(TIFF.TAG_COMPRESSION, 6));
        EXIFThumbnail.from(new EXIF(new byte[42]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }

    @Test(expected = IIOException.class)
    public void testFromTruncatedJPEG() throws IOException {
        List<TIFFEntry> entries = Arrays.asList(
                new TIFFEntry(TIFF.TAG_COMPRESSION, 6),
                new TIFFEntry(TIFF.TAG_JPEG_INTERCHANGE_FORMAT, 0)
        );
        EXIFThumbnail.from(new EXIF(new byte[42]), new EXIFDirectory(new IFD(Collections.<Entry>emptyList()), new IFD(entries)), thumbnailReader);
    }


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
        return EXIFThumbnail.from(exif, (CompoundDirectory) new TIFFReader().read(exif.exifData()), thumbnailReader);
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

    private static class EXIFDirectory extends AbstractCompoundDirectory {
        public EXIFDirectory(IFD... ifds) {
            super(Arrays.asList(ifds));
        }
    }
}
