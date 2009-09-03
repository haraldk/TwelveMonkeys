package com.twelvemonkeys.io.ole2;

import junit.framework.TestCase;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteOrder;

/**
 * CompoundDocumentTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/ole2/CompoundDocumentTestCase.java#1 $
 */
public class CompoundDocumentTestCase extends TestCase {
    public void testReadCatalogInputStream() throws IOException {
        InputStream input = getClass().getResourceAsStream("/Thumbs-camera.db");

        assertNotNull("Missing test resource!", input);

        CompoundDocument document = new CompoundDocument(input);
        Entry root = document.getRootEntry();
        assertNotNull(root);
        assertEquals(25, root.getChildEntries().size());

        Entry catalog = root.getChildEntry("Catalog");
        assertNotNull(catalog);
        assertNotNull("Input stream may not be null", catalog.getInputStream());
    }

    public void testReadCatalogImageInputStream() throws IOException {
        InputStream input = getClass().getResourceAsStream("/Thumbs-camera.db");

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

    public void testReadThumbsCatalogFile() throws IOException, URISyntaxException {
        URL input = getClass().getResource("/Thumbs-camera.db");

        assertNotNull("Missing test resource!", input);
        assertEquals("Test resource not a file:// resource", "file", input.getProtocol());

        File file = new File(input.toURI());

        CompoundDocument document = new CompoundDocument(file);

        Entry root = document.getRootEntry();

        assertNotNull(root);
        assertEquals(25, root.getChildEntries().size());

        Entry catalog = root.getChildEntry("Catalog");

        assertNotNull(catalog);
        assertNotNull("Input stream may not be null", catalog.getInputStream());
    }
}
