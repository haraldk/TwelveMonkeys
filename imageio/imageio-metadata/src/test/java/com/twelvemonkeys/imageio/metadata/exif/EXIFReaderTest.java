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

package com.twelvemonkeys.imageio.metadata.exif;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReaderAbstractTest;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * EXIFReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFReaderTest.java,v 1.0 23.12.11 13:50 haraldk Exp$
 */
public class EXIFReaderTest extends MetadataReaderAbstractTest {
    @Override
    protected InputStream getData() throws IOException {
        return getResource("/exif/exif-jpeg-segment.bin").openStream();
    }

    @Override
    protected EXIFReader createReader() {
        return new EXIFReader();
    }

    @Test
    public void testIsCompoundDirectory() throws IOException {
        Directory exif = createReader().read(getDataAsIIS());
        assertThat(exif, instanceOf(CompoundDirectory.class));
    }

    @Test
    public void testDirectory() throws IOException {
        CompoundDirectory exif = (CompoundDirectory) createReader().read(getDataAsIIS());

        assertEquals(2, exif.directoryCount());
        assertNotNull(exif.getDirectory(0));
        assertNotNull(exif.getDirectory(1));
        assertEquals(exif.size(), exif.getDirectory(0).size() + exif.getDirectory(1).size());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testDirectoryOutOfBounds() throws IOException {
        InputStream data = getData();

        CompoundDirectory exif = (CompoundDirectory) createReader().read(ImageIO.createImageInputStream(data));

        assertEquals(2, exif.directoryCount());
        assertNotNull(exif.getDirectory(exif.directoryCount()));
    }

    @Test
    public void testEntries() throws IOException {
        CompoundDirectory exif = (CompoundDirectory) createReader().read(getDataAsIIS());

        // From IFD0
        assertNotNull(exif.getEntryById(TIFF.TAG_SOFTWARE));
        assertEquals("Adobe Photoshop CS2 Macintosh", exif.getEntryById(TIFF.TAG_SOFTWARE).getValue());
        assertEquals(exif.getEntryById(TIFF.TAG_SOFTWARE), exif.getEntryByFieldName("Software"));

        // From IFD1
        assertNotNull(exif.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT));
        assertEquals((long) 418, exif.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT).getValue());
        assertEquals(exif.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT), exif.getEntryByFieldName("JPEGInterchangeFormat"));
    }

    @Test
    public void testIFD0() throws IOException {
        CompoundDirectory exif = (CompoundDirectory) createReader().read(getDataAsIIS());

        Directory ifd0 = exif.getDirectory(0);
        assertNotNull(ifd0);

        assertNotNull(ifd0.getEntryById(TIFF.TAG_IMAGE_WIDTH));
        assertEquals(3601, ifd0.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue());

        assertNotNull(ifd0.getEntryById(TIFF.TAG_IMAGE_HEIGHT));
        assertEquals(4176, ifd0.getEntryById(TIFF.TAG_IMAGE_HEIGHT).getValue());

        // Assert 'uncompressed' (there's no TIFF image here, really)
        assertNotNull(ifd0.getEntryById(TIFF.TAG_COMPRESSION));
        assertEquals(1, ifd0.getEntryById(TIFF.TAG_COMPRESSION).getValue());
    }

    @Test
    public void testIFD1() throws IOException {
        CompoundDirectory exif = (CompoundDirectory) createReader().read(getDataAsIIS());

        Directory ifd1 = exif.getDirectory(1);
        assertNotNull(ifd1);

        // Assert 'JPEG compression' (thumbnail only)
        assertNotNull(ifd1.getEntryById(TIFF.TAG_COMPRESSION));
        assertEquals(6, ifd1.getEntryById(TIFF.TAG_COMPRESSION).getValue());

        assertNull(ifd1.getEntryById(TIFF.TAG_IMAGE_WIDTH));
        assertNull(ifd1.getEntryById(TIFF.TAG_IMAGE_HEIGHT));
    }

    @Test
    public void testReadBadDataZeroCount() throws IOException {
        // This image seems to contain bad Exif. But as other tools are able to read, so should we..
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-rgb-thumbnail-bad-exif-kodak-dc210.jpg"));
        stream.seek(12);
        Directory directory = createReader().read(new SubImageInputStream(stream, 21674));

        assertEquals(22, directory.size());

        // Special case: Ascii string with count == 0, not ok according to spec (?), but we'll let it pass
        assertEquals("", directory.getEntryById(TIFF.TAG_IMAGE_DESCRIPTION).getValue());
    }

    @Test
    public void testReadBadDataRationalZeroDenominator() throws IOException {
        // This image seems to contain bad Exif. But as other tools are able to read, so should we..
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-rgb-thumbnail-bad-exif-kodak-dc210.jpg"));
        stream.seek(12);
        Directory directory = createReader().read(new SubImageInputStream(stream, 21674));

        // Special case: Rational with zero-denominator inside EXIF data
        Directory exif = (Directory) directory.getEntryById(TIFF.TAG_EXIF_IFD).getValue();
        Entry entry = exif.getEntryById(EXIF.TAG_COMPRESSED_BITS_PER_PIXEL);
        assertNotNull(entry);
        assertEquals(Rational.NaN, entry.getValue());
    }

    @Test
    public void testReadBadDirectoryCount() throws IOException {
        // This image seems to contain bad Exif. But as other tools are able to read, so should we..
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-bad-directory-entry-count.jpg"));
        stream.seek(4424 + 10);

        Directory directory = createReader().read(new SubImageInputStream(stream, 214 - 6));
        assertEquals(7, directory.size()); // TIFF structure says 8, but the last entry isn't there

        Directory exif = (Directory) directory.getEntryById(TIFF.TAG_EXIF_IFD).getValue();
        assertNotNull(exif);
        assertEquals(3, exif.size());
    }

    @Test
    public void testTIFFWithBadExifIFD() throws IOException {
        // This image seems to contain bad TIFF data. But as other tools are able to read, so should we..
        // It seems that the EXIF data (at offset 494196 or 0x78a74) overlaps with a custom
        // Microsoft 'OLE Property Set' entry at 0x78a70 (UNDEFINED, count 5632)...
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/tiff/chifley_logo.tif"));
        Directory directory = createReader().read(stream);
        assertEquals(22, directory.size());

        // Some (all?) of the EXIF data is duplicated in the XMP, meaning PhotoShop can probably re-create it
        Directory exif = (Directory) directory.getEntryById(TIFF.TAG_EXIF_IFD).getValue();
        assertNotNull(exif);
        assertEquals(0, exif.size()); // EXIFTool reports "Warning: Bad ExifIFD directory"
    }
}
