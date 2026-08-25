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

import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;

import javax.imageio.IIOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CatalogTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public class CatalogTest {

    private static void writeHeader(final LittleEndianDataOutputStream out, final int thumbCount) throws IOException {
        out.writeShort(0); // reserved1
        out.writeShort(0); // reserved2
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
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LittleEndianDataOutputStream out = new LittleEndianDataOutputStream(bytes);
        writeHeader(out, 1);
        writeItem(out, itemId, name);
        out.flush();

        return new LittleEndianDataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    @Test
    public void testReadValidItem() throws IOException {
        Catalog catalog = Catalog.read(catalogWith(1, "3\\folder\\image.jpg"));

        assertEquals(1, catalog.getThumbnailCount());
        assertEquals("image.jpg", catalog.getItem(0).getName());
    }

    @Test
    public void testFilenameLongerThanBuffer() throws IOException {
        // Filename length is taken from the stream and was filled into a fixed 256 char buffer before
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            name.append('a');
        }

        Catalog catalog = Catalog.read(catalogWith(1, name.toString()));

        assertEquals(name.toString(), catalog.getItem(0).getName());
    }

    @Test
    public void testItemIdBelowRange() throws IOException {
        // itemId is used as items[itemId - 1] without a bounds check
        assertThrows(IIOException.class, () -> Catalog.read(catalogWith(0, "image.jpg")));
    }

    @Test
    public void testItemIdAboveRange() throws IOException {
        assertThrows(IIOException.class, () -> Catalog.read(catalogWith(9, "image.jpg")));
    }
}
