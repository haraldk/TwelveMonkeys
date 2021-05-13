/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.tiff;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.junit.Test;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.MetadataReaderAbstractTest;
import com.twelvemonkeys.imageio.metadata.exif.EXIF;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

/**
 * TIFFReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFReaderTest.java,v 1.0 23.12.11 13:50 haraldk Exp$
 */
public class TIFFReaderTest extends MetadataReaderAbstractTest {
    @Override
    protected InputStream getData() throws IOException {
        return getResource("/exif/exif-jpeg-segment.bin").openStream();
    }

    @Override
    protected TIFFReader createReader() {
        return new TIFFReader();
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

    @Test
    public void testReadExifJPEGWithInteropSubDirR98() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-R98.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 1360));
        assertEquals(17, directory.size());
        assertEquals(2, directory.directoryCount());

        Directory exif = (Directory) directory.getEntryById(TIFF.TAG_EXIF_IFD).getValue();
        assertNotNull(exif);
        assertEquals(23, exif.size());

        // The interop IFD with two values
        Directory interop = (Directory) exif.getEntryById(TIFF.TAG_INTEROP_IFD).getValue();
        assertNotNull(interop);
        assertEquals(2, interop.size());

        assertNotNull(interop.getEntryById(1)); // InteropIndex
        assertEquals("ASCII", interop.getEntryById(1).getTypeName());
        assertEquals("R98", interop.getEntryById(1).getValue());  // Known values: R98, THM or R03

        assertNotNull(interop.getEntryById(2)); // InteropVersion
        assertEquals("UNDEFINED", interop.getEntryById(2).getTypeName());
        assertArrayEquals(new byte[] {'0', '1', '0', '0'}, (byte[]) interop.getEntryById(2).getValue());
    }

    @Test
    public void testReadExifJPEGWithInteropSubDirEmpty() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-empty.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 1360));
        assertEquals(11, directory.size());
        assertEquals(1, directory.directoryCount());

        Directory exif = (Directory) directory.getEntryById(TIFF.TAG_EXIF_IFD).getValue();
        assertNotNull(exif);
        assertEquals(24, exif.size());

        // The interop IFD is empty (entry count is 0)
        Directory interop = (Directory) exif.getEntryById(TIFF.TAG_INTEROP_IFD).getValue();
        assertNotNull(interop);
        assertEquals(0, interop.size());
    }

    @Test
    public void testReadExifJPEGWithInteropSubDirEOF() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-eof.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 236));
        assertEquals(8, directory.size());
        assertEquals(1, directory.directoryCount());

        Directory exif = (Directory) directory.getEntryById(TIFF.TAG_EXIF_IFD).getValue();
        assertNotNull(exif);
        assertEquals(5, exif.size());

        // The interop IFD isn't there (offset points to outside the TIFF structure)...
        // Have double-checked using ExifTool, which says "Warning : Bad InteropOffset SubDirectory start"
        Object interop = exif.getEntryById(TIFF.TAG_INTEROP_IFD).getValue();
        assertNotNull(interop);
        assertEquals(240L, interop);
    }

    @Test
    public void testReadExifJPEGWithInteropSubDirBad() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-bad.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 12185));
        assertEquals(16, directory.size());
        assertEquals(2, directory.directoryCount());

        Directory exif = (Directory) directory.getEntryById(TIFF.TAG_EXIF_IFD).getValue();
        assertNotNull(exif);
        assertEquals(26, exif.size());

        // JPEG starts at offset 1666 and length 10519, interop IFD points to offset 1900...
        // Have double-checked using ExifTool, which says "Warning : Bad InteropIFD directory"
        Directory interop = (Directory) exif.getEntryById(TIFF.TAG_INTEROP_IFD).getValue();
        assertNotNull(interop);
        assertEquals(0, interop.size());
    }

    @Test
    public void testReadExifWithMissingEOFMarker() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/exif/noeof.tif"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(15, directory.size());
            assertEquals(1, directory.directoryCount());
        }
    }

    @Test
    public void testReadExifWithEmptyTag() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/exif/emptyexiftag.tif"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(3, directory.directoryCount());
        }
    }

    @Test
    public void testReadValueBeyondEOF() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/exif/value-beyond-eof.tif"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(1, directory.directoryCount());
            assertEquals(5, directory.size());

            assertEquals(1, directory.getEntryById(TIFF.TAG_PHOTOMETRIC_INTERPRETATION).getValue());
            assertEquals(10, directory.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue());
            assertEquals(10, directory.getEntryById(TIFF.TAG_IMAGE_HEIGHT).getValue());
            assertEquals(42L, directory.getEntryById(32935).getValue());
            // NOTE: Assumes current implementation, could possibly change in the future.
            assertTrue(directory.getEntryById(32934).getValue() instanceof EOFException);
        }
    }

    @Test
    public void testReadIDFPointerBeyondEOF() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/tiff/ifd-end-pointer.tif"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(1, directory.directoryCount());
            assertEquals(15, directory.size());
        }
    }

    @Test
    public void testReadNestedExifWithoutOOME() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-nested-exif.jpg"))) {
            stream.seek(30);
            CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 886));
            assertEquals(1, directory.directoryCount());
            assertEquals(10, directory.size());
        }
    }

    @Test
    public void testReadExifBogusCountWithoutOOME() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-oome-bogus-count.jpg"))) {
            stream.seek(30);
            CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 3503));
            assertEquals(2, directory.directoryCount());
            assertEquals(12, directory.size());
            assertEquals(9, directory.getDirectory(0).size());
            assertEquals(3, directory.getDirectory(1).size());
        }
    }

    @Test
    public void testReadWithoutOOME() throws IOException {
        // This EXIF segment from a JPEG contains an Interop IFD, containing a weird value that could be interpreted
        // as a huge SLONG8 field (valid for BigTIFF only).
        // OutOfMemoryError would only happen if length of stream is not known (ie. reading from underlying stream).
        try (InputStream stream = getResource("/exif/exif-bad-interop-oome.bin").openStream()) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(new MemoryCacheImageInputStream(stream));
            assertEquals(2, directory.directoryCount());
            assertEquals(11, directory.getDirectory(0).size());
            assertEquals(6, directory.getDirectory(1).size());
            assertEquals("Picasa", directory.getDirectory(0).getEntryById(TIFF.TAG_SOFTWARE).getValue());
            assertEquals("2020:11:17 16:05:37", directory.getDirectory(0).getEntryById(TIFF.TAG_DATE_TIME).getValueAsString());
        }
    }

    @Test(timeout = 200)
    public void testReadCyclicExifWithoutLoopOrOOME() throws IOException {
        // This EXIF segment has an interesting bug...
        // The bits per sample value (0x 0008 0008 0008) overwrites half the IFD1 link offset (should be 0x00000000),
        // effectively making it a loop back to the IFD0 at offset 0x0000008...
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/exif/exif-loop.bin"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(1, directory.directoryCount());
            assertEquals(12, directory.getDirectory(0).size());
            assertEquals("Polarr Photo Editor", directory.getDirectory(0).getEntryById(TIFF.TAG_SOFTWARE).getValue());
            assertEquals("2019:02:27 09:22:59", directory.getDirectory(0).getEntryById(TIFF.TAG_DATE_TIME).getValueAsString());
        }
    }

    @Test(timeout = 100)
    public void testIFDLoop() throws IOException {
        byte[] looping = new byte[] {
                'M', 'M', 0, 42,
                0, 0, 0, 8,     // IFD0 pointer
                0, 1,           // entry count
                0, (byte) 259,  // compression
                0, 3,           // SHORT
                0, 0, 0, 1,     // count
                0, 0, 0, 0,     //
                0, 0, 0, 8,     // IFD1 pointer
        };

        try (ImageInputStream stream = new ByteArrayImageInputStream(looping)) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);

            assertEquals(1, directory.directoryCount());
            assertEquals(1, directory.size());
        }
    }

    @Test(timeout = 100)
    public void testIFDLoopNested() throws IOException {
        byte[] looping = new byte[] {
                'M', 'M', 0, 42,
                0, 0, 0, 8,     // IFD0 pointer
                0, 1,           // entry count
                1, 74,          // sub IFD
                0, 4,           // LONG
                0, 0, 0, 1,     // count
                0, 0, 0, 8,     // sub IFD pointer -> IFD0
                0, 0, 0, 0,     // End of IFD chain
        };

        try (ImageInputStream stream = new ByteArrayImageInputStream(looping)) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);

            assertEquals(1, directory.directoryCount());
            assertEquals(1, directory.size());
        }
    }

    @Test(timeout = 100)
    public void testSubIFDLoop() throws IOException {
        byte[] looping = new byte[] {
                'M', 'M', 0, 42,
                0, 0, 0, 8,     // IFD0 pointer
                0, 1,           // entry count
                1, 74,          // sub IFD
                0, 4,           // LONG
                0, 0, 0, 1,     // count
                0, 0, 0, 26,    // SubIFD pointer
                0, 0, 0, 0,     // End of IFD chain
                // --- sub IFD
                0, 1,           // entry count
                1, 74,          // sub IFD
                0, 4,           // LONG
                0, 0, 0, 1,     // count
                0, 0, 0, 26,    // sub IFD pointer -> sub IFD
        };

        try (ImageInputStream stream = new ByteArrayImageInputStream(looping)) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);

            assertEquals(1, directory.directoryCount());
            assertEquals(1, directory.size());
        }
    }

    @Test(timeout = 100)
    public void testSubIFDLoopNested() throws IOException {
        byte[] looping = new byte[] {
                'M', 'M', 0, 42,
                0, 0, 0, 8,     // IFD0 pointer
                0, 1,           // entry count
                1, 74,          // sub IFD
                0, 4,           // LONG
                0, 0, 0, 1,     // count
                0, 0, 0, 26,    // SubIFD pointer
                0, 0, 0, 0,     // End of IFD chain
                // --- sub IFD
                0, 1,           // entry count
                1, 74,          // sub IFD
                0, 4,           // LONG
                0, 0, 0, 1,     // count
                0, 0, 0, 8,     // sub IFD pointer -> IFD0
        };

        try (ImageInputStream stream = new ByteArrayImageInputStream(looping)) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);

            assertEquals(1, directory.directoryCount());
            assertEquals(1, directory.size());
        }
    }
}
