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
public class CompoundDocumentTestCase {

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
        CompoundDocument document = createTestDocument();

        Entry root = document.getRootEntry();

        assertNotNull(root);
        assertEquals("Root Entry", root.getName());
        assertTrue(root.isRoot());
        assertFalse(root.isFile());
        assertFalse(root.isDirectory());
        assertEquals(0, root.length());
        assertNull(root.getInputStream());
    }

    @Test
    public void testContents() throws IOException {
        CompoundDocument document = createTestDocument();

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

    @Test(expected = UnsupportedOperationException.class)
    public void testChildEntriesUnmodifiable() throws IOException {
        CompoundDocument document = createTestDocument();

        Entry root = document.getRootEntry();

        assertNotNull(root);

        SortedSet<Entry> children = root.getChildEntries();

        // Should not be allowed, as it modifies the internal structure
        children.remove(children.first());
    }

    @Test
    public void testReadThumbsCatalogFile() throws IOException {
        CompoundDocument document = createTestDocument();

        Entry root = document.getRootEntry();

        assertNotNull(root);
        assertEquals(25, root.getChildEntries().size());

        Entry catalog = root.getChildEntry("Catalog");

        assertNotNull(catalog);
        assertNotNull("Input stream may not be null", catalog.getInputStream());
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
