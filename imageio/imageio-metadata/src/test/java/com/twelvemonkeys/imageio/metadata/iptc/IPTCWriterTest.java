package com.twelvemonkeys.imageio.metadata.iptc;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.MetadataWriter;
import com.twelvemonkeys.imageio.metadata.MetadataWriterAbstractTest;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNotNull;

/**
 * IPTCWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: IPTCWriterTest.java,v 1.0 05/06/15 harald.kuhr Exp$
 */
public class IPTCWriterTest extends MetadataWriterAbstractTest {
    @Override
    protected InputStream getData() throws IOException {
        return getResource("/iptc/iptc-jpeg-segment.bin").openStream();
    }

    @Override
    protected MetadataWriter createWriter() {
        return new IPTCWriter();
    }

    private IPTCReader createReader() {
        return new IPTCReader();
    }

    @Test
    public void testRewriteExisting() throws IOException {
        IPTCReader reader = createReader();
        Directory iptc = reader.read(getDataAsIIS());
        assumeNotNull(iptc);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(bytes);
        createWriter().write(iptc, stream);
        stream.close();

        Directory written = reader.read(new ByteArrayImageInputStream(bytes.toByteArray()));
        assertEquals(iptc, written);
    }

    @Test
    public void testWrite() throws IOException {
        List<IPTCEntry> entries = new ArrayList<>();
        entries.add(new IPTCEntry(IPTC.TAG_KEYWORDS, new String[] {"Uno", "Due", "Tre"}));

        Directory iptc = new IPTCDirectory(entries);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(bytes);
        createWriter().write(iptc, stream);
        stream.close();

        Directory written = createReader().read(new ByteArrayImageInputStream(bytes.toByteArray()));
        assertEquals(iptc, written);
    }
}