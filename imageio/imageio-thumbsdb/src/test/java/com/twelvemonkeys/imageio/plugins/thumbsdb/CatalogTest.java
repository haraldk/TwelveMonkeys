/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;
import com.twelvemonkeys.io.ole2.CompoundDocument;
import com.twelvemonkeys.io.ole2.Entry;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CatalogTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public class CatalogTest {

    private static void writeHeader(final LittleEndianDataOutputStream out, final int thumbCount) throws IOException {
        out.writeShort(16); // length? always 16 for real data, matches the size
        out.writeShort(7); // reserved2 (not sure what this is, but always 7 in sample files I've seen)
        out.writeInt(thumbCount);
        out.writeInt(96); // max width
        out.writeInt(96); // max height
    }

    private static void writeItem(final LittleEndianDataOutputStream out, final int itemId, final String name) throws IOException {
        // 16 byte fixed part (size + itemId + timestamp) + UTF-16LE name + NUL + 2 byte padding
        int size = 16 + (name.length() + 1) * 2 + 2;
        out.writeInt(size);
        out.writeInt(itemId);
        out.writeLong(0L); // last modified
        for (int i = 0; i < name.length(); i++) {
            out.writeChar(name.charAt(i));
        }
        out.writeChar(0); // NUL terminator
        out.writeShort(0); // padding
    }

    private static DataInput catalogWith(final int itemId, final String name) throws IOException {
        FastByteArrayOutputStream bytes = new FastByteArrayOutputStream(1024);
        LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(bytes);
        writeHeader(out, 1);
        writeItem(out, itemId, name);
        out.flush();

        return new LittleEndianDataInputStream(bytes.createInputStream());
    }

    @Test
    void testReadValidItem() throws IOException {
        Catalog catalog = Catalog.read(catalogWith(1, "3\\folder\\image.jpg"));

        assertEquals(1, catalog.getThumbnailCount());
        assertEquals("image.jpg", catalog.getItem(0).getName());
    }

    @Test
    void testFilenameLongerThanAllowed() throws IOException {
        // Creates a crafted Catalog with a single entry and file name above the limit of 260 chars
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            name.append('a');
        }

        assertThrows(IIOException.class, () -> Catalog.read(catalogWith(1, name.toString())));
    }

    @Test
    void testItemIdBelowRange() {
        // Creates a crafted Catalog with entry less than the minimum 1
        assertThrows(IIOException.class, () -> Catalog.read(catalogWith(0, "image.jpg")));
    }

    @Test
    void testItemIdAboveRange() {
        // Creates a crafted Catalog with entry above the range
        assertThrows(IIOException.class, () -> Catalog.read(catalogWith(9, "image.jpg")));
    }

    @Test
    void testRealInput() throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(getClass().getResourceAsStream("/thumbsdb/Thumbs.db"))) {
            input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            Entry root = new CompoundDocument(input).getRootEntry();

            Entry child = root.getChildEntry("Catalog");
            assertNotNull(child);

            Catalog catalog = Catalog.read(child.getInputStream());
            assertNotNull(catalog);
            assertEquals(9, catalog.getThumbnailCount());
            assertEquals("{A42CD7B6-E9B9-4D02-B7A6-288B71AD28BA}", catalog.getItem(0).getName());
            assertEquals("CoffeeBean.bmp", catalog.getItem(1).getName());
            assertEquals("JavaCup.ico", catalog.getItem(2).getName());
            assertEquals("office_05.gif", catalog.getItem(3).getName());
            assertEquals("sunflower.jpg", catalog.getItem(4).getName());
            assertEquals("test.jpg", catalog.getItem(5).getName());
            assertEquals("test.png", catalog.getItem(6).getName());
            assertEquals("test.tif", catalog.getItem(7).getName());
            assertEquals("test.wmf", catalog.getItem(8).getName());
        }
    }
}
