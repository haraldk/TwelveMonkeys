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

package com.twelvemonkeys.imageio.metadata.exif;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.MetadataReaderAbstractTest;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * EXIFReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFReaderTest.java,v 1.0 23.12.11 13:50 haraldk Exp$
 */
@SuppressWarnings("deprecation")
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

    @Test
    public void testDirectoryOutOfBounds() throws IOException {
        InputStream data = getData();

        CompoundDirectory exif = (CompoundDirectory) createReader().read(ImageIO.createImageInputStream(data));

        assertEquals(2, exif.directoryCount());
        assertThrows(IndexOutOfBoundsException.class, () -> {
            exif.getDirectory(exif.directoryCount());
        });
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
}
