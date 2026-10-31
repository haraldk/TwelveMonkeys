/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;

import javax.imageio.IIOException;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ICOImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICOImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class ICOImageReaderTest extends ImageReaderAbstractTest<ICOImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new ICOImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(
                        getClassLoaderResource("/ico/JavaCup.ico"),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16),
                        new Dimension(48, 48), new Dimension(32, 32), new Dimension(16, 16)
                ),
                new TestData(getClassLoaderResource("/ico/favicon.ico"), new Dimension(32, 32)),
                new TestData(
                        getClassLoaderResource("/ico/joypad.ico"),
                        new Dimension(16, 16), new Dimension(24, 24), new Dimension(32, 32), new Dimension(48, 48),
                        new Dimension(16, 16), new Dimension(24, 24), new Dimension(32, 32), new Dimension(48, 48)
                ),
                // Windows Vista icon, PNG encoded for 256x256 sizes
                new TestData(
                        getClassLoaderResource("/ico/down.ico"),
                        new Dimension(16, 16), new Dimension(16, 16), new Dimension(32, 32), new Dimension(32, 32),
                        new Dimension(48, 48), new Dimension(48, 48), new Dimension(256, 256), new Dimension(256, 256),
                        new Dimension(16, 16), new Dimension(32, 32), new Dimension(48, 48), new Dimension(256, 256)
                ),
                // Problematic icon that reports 24 bit in the descriptor, but has separate 1 bit ''mask (height 2 x icon height)!
                new TestData(getClassLoaderResource("/ico/rgb24bitmask.ico"), new Dimension(32, 32))
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Collections.singletonList("ico");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("ico");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList("image/vnd.microsoft.icon", "image/ico", "image/x-icon");
    }

    @Test
    @Disabled("Known issue")
    @Override
    public void testNotBadCaching() throws IOException {
        super.testNotBadCaching();
    }

    @Test
    @Disabled("Known issue: Subsampled reading currently not supported")
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        super.testReadWithSubsampleParamPixels();
    }

    @Test
    public void testRejectsEntryBitCountOver8ForIndexedBitmap() throws IOException {
        // Entry bit count 31 makes the palette size 1 << 31 (negative), the DIB header says 8 bit indexed
        byte[] data = createICO(31, 40 + 256 * 4 + 8, 22, createDIBHeader(8));

        assertReadThrowsIIOException(data);
    }

    @Test
    public void testReadsRGBWithMaskAndEntryBitCountOver8() throws IOException {
        // The 1 bit mask of a 24 bit bitmap shares the directory entry, bit count 31 must not size a palette
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(createDIBHeader(24));
        bytes.write(new byte[] {(byte) 0xff, 0, 0, 0}); // 1 BGR pixel, padded to 4 bytes
        bytes.write(new byte[] {0, 0, 0, 0});           // 1 bit mask row, padded to 4 bytes
        byte[] data = createICO(31, bytes.size(), 22, bytes.toByteArray());

        ICOImageReader reader = createReader();

        try (ImageInputStream stream = new ByteArrayImageInputStream(data)) {
            reader.setInput(stream);

            BufferedImage image = reader.read(0);
            assertEquals(0xff0000ff, image.getRGB(0, 0));
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testRejectsNegativeEntryOffset() throws IOException {
        byte[] data = createICO(8, 40 + 256 * 4 + 8, Integer.MIN_VALUE, createDIBHeader(8));

        assertReadThrowsIIOException(data);
    }

    @Test
    public void testRejectsNegativeEntrySize() throws IOException {
        // PNG magic at the entry offset routes to the PNG reader, using the entry size as stream length
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
        byte[] data = createICO(32, -1, 22, png);

        assertReadThrowsIIOException(data);
    }

    private void assertReadThrowsIIOException(byte[] data) throws IOException {
        ICOImageReader reader = createReader();

        try (ImageInputStream stream = new ByteArrayImageInputStream(data)) {
            reader.setInput(stream);

            assertThrows(IIOException.class, () -> reader.read(0));
        }
        finally {
            reader.dispose();
        }
    }

    private static byte[] createICO(int bitCount, int size, int offset, byte[] image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(bytes)) {
            out.writeShort(0);          // Reserved
            out.writeShort(1);          // Type: ICO
            out.writeShort(1);          // Image count

            out.writeByte(1);           // Width
            out.writeByte(1);           // Height
            out.writeByte(0);           // Color count (0: 1 << bit count)
            out.writeByte(0);           // Reserved
            out.writeShort(1);          // Planes
            out.writeShort(bitCount);   // Bit count
            out.writeInt(size);         // Size of image data
            out.writeInt(offset);       // Offset of image data

            out.write(image);
        }

        return bytes.toByteArray();
    }

    private static byte[] createDIBHeader(int bitCount) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(bytes)) {
            out.writeInt(40);           // BITMAPINFOHEADER size
            out.writeInt(1);            // Width
            out.writeInt(2);            // Height (image + mask)
            out.writeShort(1);          // Planes
            out.writeShort(bitCount);   // Bit count
            out.writeInt(0);            // Compression: BI_RGB
            out.writeInt(0);            // Image size
            out.writeInt(0);            // X pixels per meter
            out.writeInt(0);            // Y pixels per meter
            out.writeInt(0);            // Colors used
            out.writeInt(0);            // Colors important
        }

        return bytes.toByteArray();
    }
}