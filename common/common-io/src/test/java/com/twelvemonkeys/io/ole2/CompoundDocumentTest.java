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

package com.twelvemonkeys.io.ole2;

import com.twelvemonkeys.io.MemoryCacheSeekableStream;
import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * CompoundDocumentTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/ole2/CompoundDocumentTestCase.java#1 $
 */
public class CompoundDocumentTest {

    private static final String SAMPLE_DATA = "/Thumbs-camera.db";

    protected final CompoundDocument createTestDocument() throws IOException {
        URL input = getClass().getResource(SAMPLE_DATA);

        assertNotNull("Missing test resource!", input);
        assertEquals("Test resource not a file:// resource", "file", input.getProtocol());

        try {
            return new CompoundDocument(new File(input.toURI()));
        }
        catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testRoot() throws IOException {
        try (CompoundDocument document = createTestDocument()) {
            Entry root = document.getRootEntry();

            assertNotNull(root);
            assertEquals("Root Entry", root.getName());
            assertTrue(root.isRoot());
            assertFalse(root.isFile());
            assertFalse(root.isDirectory());
            assertEquals(0, root.length());
            assertNull(root.getInputStream());
        }
    }

    @Test
    public void testContents() throws IOException {
        try (CompoundDocument document = createTestDocument()) {
            Entry root = document.getRootEntry();

            assertNotNull(root);

            SortedSet<Entry> children = new TreeSet<Entry>(root.getChildEntries());
            assertEquals(25, children.size());

            // Weirdness in the file format, name is *written backwards* 1-24 + Catalog
            for (String name : "1,2,3,4,5,6,7,8,9,01,02,11,12,21,22,31,32,41,42,51,61,71,81,91,Catalog".split(",")) {
                assertEquals(name, children.first().getName());
                children.remove(children.first());
            }
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testChildEntriesUnmodifiable() throws IOException {
        try (CompoundDocument document = createTestDocument()) {
            Entry root = document.getRootEntry();

            assertNotNull(root);

            SortedSet<Entry> children = root.getChildEntries();

            // Should not be allowed, as it modifies the internal structure
            children.remove(children.first());
        }
    }

    @Test
    public void testReadThumbsCatalogFile() throws IOException {
        try (CompoundDocument document = createTestDocument()) {
            Entry root = document.getRootEntry();

            assertNotNull(root);
            assertEquals(25, root.getChildEntries().size());

            Entry catalog = root.getChildEntry("Catalog");

            assertNotNull(catalog);
            assertNotNull("Input stream may not be null", catalog.getInputStream());
        }
    }

    @Test
    public void testReadCatalogInputStream() throws IOException {
        InputStream input = getClass().getResourceAsStream(SAMPLE_DATA);

        assertNotNull("Missing test resource!", input);

        CompoundDocument document = new CompoundDocument(input);
        Entry root = document.getRootEntry();
        assertNotNull(root);
        assertEquals(25, root.getChildEntries().size());

        Entry catalog = root.getChildEntry("Catalog");
        assertNotNull(catalog);
        assertNotNull("Input stream may not be null", catalog.getInputStream());
    }

    @Test
    public void testReadCatalogSeekableStream() throws IOException {
        InputStream input = getClass().getResourceAsStream(SAMPLE_DATA);

        assertNotNull("Missing test resource!", input);

        CompoundDocument document = new CompoundDocument(new MemoryCacheSeekableStream(input));
        Entry root = document.getRootEntry();
        assertNotNull(root);
        assertEquals(25, root.getChildEntries().size());

        Entry catalog = root.getChildEntry("Catalog");
        assertNotNull(catalog);
        assertNotNull("Input stream may not be null", catalog.getInputStream());
    }

    @Test
    public void testReadCatalogImageInputStream() throws IOException {
        InputStream input = getClass().getResourceAsStream(SAMPLE_DATA);

        assertNotNull("Missing test resource!", input);

        MemoryCacheImageInputStream stream = new MemoryCacheImageInputStream(input);
        stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        CompoundDocument document = new CompoundDocument(stream);

        Entry root = document.getRootEntry();

        assertNotNull(root);
        assertEquals(25, root.getChildEntries().size());

        Entry catalog = root.getChildEntry("Catalog");

        assertNotNull(catalog);
        assertNotNull("Input stream may not be null", catalog.getInputStream());
    }
}
