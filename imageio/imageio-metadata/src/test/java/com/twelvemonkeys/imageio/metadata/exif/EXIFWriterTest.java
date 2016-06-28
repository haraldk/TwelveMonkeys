/*
 * Copyright (c) 2013, Harald Kuhr
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

import com.twelvemonkeys.imageio.metadata.*;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * EXIFWriterTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFWriterTest.java,v 1.0 18.07.13 09:53 haraldk Exp$
 */
public class EXIFWriterTest extends MetadataWriterAbstractTest {

    @Override
    protected InputStream getData() throws IOException {
        return getResource("/exif/exif-jpeg-segment.bin").openStream();
    }

    protected EXIFReader createReader() {
        return new EXIFReader();
    }

    @Override
    protected EXIFWriter createWriter() {
        return new EXIFWriter();
    }

    @Test
    public void testWriteReadSimple() throws IOException {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new EXIFEntry(TIFF.TAG_ORIENTATION, 1, TIFF.TYPE_SHORT));
        entries.add(new EXIFEntry(TIFF.TAG_IMAGE_WIDTH, 1600, TIFF.TYPE_SHORT));
        entries.add(new AbstractEntry(TIFF.TAG_IMAGE_HEIGHT, 900) {});
        entries.add(new EXIFEntry(TIFF.TAG_ARTIST, "Harald K.", TIFF.TYPE_ASCII));
        entries.add(new AbstractEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO") {});
        Directory directory = new AbstractDirectory(entries) {};

        ByteArrayOutputStream output = new FastByteArrayOutputStream(1024);
        ImageOutputStream imageStream = ImageIO.createImageOutputStream(output);
        new EXIFWriter().write(directory, imageStream);
        imageStream.flush();

        assertEquals(output.size(), imageStream.getStreamPosition());

        byte[] data = output.toByteArray();

        assertEquals(106, data.length);
        assertEquals('M', data[0]);
        assertEquals('M', data[1]);
        assertEquals(0, data[2]);
        assertEquals(42, data[3]);

        Directory read = new EXIFReader().read(new ByteArrayImageInputStream(data));

        assertNotNull(read);
        assertEquals(5, read.size());

        // TODO: Assert that the tags are written in ascending order (don't test the read directory, but the file structure)!

        assertNotNull(read.getEntryById(TIFF.TAG_SOFTWARE));
        assertEquals("TwelveMonkeys ImageIO", read.getEntryById(TIFF.TAG_SOFTWARE).getValue());

        assertNotNull(read.getEntryById(TIFF.TAG_IMAGE_WIDTH));
        assertEquals(1600, read.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue());

        assertNotNull(read.getEntryById(TIFF.TAG_IMAGE_HEIGHT));
        assertEquals(900, read.getEntryById(TIFF.TAG_IMAGE_HEIGHT).getValue());

        assertNotNull(read.getEntryById(TIFF.TAG_ORIENTATION));
        assertEquals(1, read.getEntryById(TIFF.TAG_ORIENTATION).getValue());

        assertNotNull(read.getEntryById(TIFF.TAG_ARTIST));
        assertEquals("Harald K.", read.getEntryById(TIFF.TAG_ARTIST).getValue());
    }

    @Test
    public void testWriteMotorola() throws IOException {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new AbstractEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO") {});
        entries.add(new EXIFEntry(TIFF.TAG_IMAGE_WIDTH, Integer.MAX_VALUE, TIFF.TYPE_LONG));
        Directory directory = new AbstractDirectory(entries) {};

        ByteArrayOutputStream output = new FastByteArrayOutputStream(1024);
        ImageOutputStream imageStream = ImageIO.createImageOutputStream(output);

        imageStream.setByteOrder(ByteOrder.BIG_ENDIAN); // BE = Motorola

        new EXIFWriter().write(directory, imageStream);
        imageStream.flush();

        assertEquals(output.size(), imageStream.getStreamPosition());

        byte[] data = output.toByteArray();

        assertEquals(60, data.length);
        assertEquals('M', data[0]);
        assertEquals('M', data[1]);
        assertEquals(0, data[2]);
        assertEquals(42, data[3]);

        Directory read = new EXIFReader().read(new ByteArrayImageInputStream(data));

        assertNotNull(read);
        assertEquals(2, read.size());
        assertNotNull(read.getEntryById(TIFF.TAG_SOFTWARE));
        assertEquals("TwelveMonkeys ImageIO", read.getEntryById(TIFF.TAG_SOFTWARE).getValue());
        assertNotNull(read.getEntryById(TIFF.TAG_IMAGE_WIDTH));
        assertEquals((long) Integer.MAX_VALUE, read.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue());
    }

    @Test
    public void testWriteIntel() throws IOException {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new AbstractEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO") {});
        entries.add(new EXIFEntry(TIFF.TAG_IMAGE_WIDTH, Integer.MAX_VALUE, TIFF.TYPE_LONG));
        Directory directory = new AbstractDirectory(entries) {};

        ByteArrayOutputStream output = new FastByteArrayOutputStream(1024);
        ImageOutputStream imageStream = ImageIO.createImageOutputStream(output);

        imageStream.setByteOrder(ByteOrder.LITTLE_ENDIAN); // LE = Intel

        new EXIFWriter().write(directory, imageStream);
        imageStream.flush();

        assertEquals(output.size(), imageStream.getStreamPosition());

        byte[] data = output.toByteArray();

        assertEquals(60, data.length);
        assertEquals('I', data[0]);
        assertEquals('I', data[1]);
        assertEquals(42, data[2]);
        assertEquals(0, data[3]);

        Directory read = new EXIFReader().read(new ByteArrayImageInputStream(data));

        assertNotNull(read);
        assertEquals(2, read.size());
        assertNotNull(read.getEntryById(TIFF.TAG_SOFTWARE));
        assertEquals("TwelveMonkeys ImageIO", read.getEntryById(TIFF.TAG_SOFTWARE).getValue());
        assertNotNull(read.getEntryById(TIFF.TAG_IMAGE_WIDTH));
        assertEquals((long) Integer.MAX_VALUE, read.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue());
    }

    @Test
    public void testNesting() throws IOException {
        EXIFEntry artist = new EXIFEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO", TIFF.TYPE_ASCII);

        EXIFEntry subSubSubSubIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(artist)), TIFF.TYPE_LONG);
        EXIFEntry subSubSubIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(subSubSubSubIFD)), TIFF.TYPE_LONG);
        EXIFEntry subSubIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(subSubSubIFD)), TIFF.TYPE_LONG);
        EXIFEntry subIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(subSubIFD)), TIFF.TYPE_LONG);

        Directory directory = new IFD(Collections.<Entry>singletonList(subIFD));

        ByteArrayOutputStream output = new FastByteArrayOutputStream(1024);
        ImageOutputStream imageStream = ImageIO.createImageOutputStream(output);

        new EXIFWriter().write(directory, imageStream);
        imageStream.flush();

        assertEquals(output.size(), imageStream.getStreamPosition());

        Directory read = new EXIFReader().read(new ByteArrayImageInputStream(output.toByteArray()));

        assertNotNull(read);
        assertEquals(1, read.size());
        assertEquals(subIFD, read.getEntryById(TIFF.TAG_SUB_IFD)); // Recursively tests content!
    }

    @Test
    public void testReadWriteRead() throws IOException {
        Directory original = createReader().read(getDataAsIIS());

        ByteArrayOutputStream output = new FastByteArrayOutputStream(256);
        ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output);

        try {
            createWriter().write(original, imageOutput);
        }
        finally {
            imageOutput.close();
        }

        Directory read = createReader().read(new ByteArrayImageInputStream(output.toByteArray()));

        assertEquals(original, read);
    }

    @Test
    public void testComputeIFDSize() throws IOException {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new EXIFEntry(TIFF.TAG_ORIENTATION, 1, TIFF.TYPE_SHORT));
        entries.add(new EXIFEntry(TIFF.TAG_IMAGE_WIDTH, 1600, TIFF.TYPE_SHORT));
        entries.add(new AbstractEntry(TIFF.TAG_IMAGE_HEIGHT, 900) {});
        entries.add(new EXIFEntry(TIFF.TAG_ARTIST, "Harald K.", TIFF.TYPE_ASCII));
        entries.add(new AbstractEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO") {});

        EXIFWriter writer = createWriter();

        ImageOutputStream stream = new NullImageOutputStream();
        writer.write(new IFD(entries), stream);

        assertEquals(stream.getStreamPosition(), writer.computeIFDSize(entries) + 12);
    }

    @Test
    public void testComputeIFDSizeNested() throws IOException {
        EXIFEntry artist = new EXIFEntry(TIFF.TAG_SOFTWARE, "TwelveMonkeys ImageIO", TIFF.TYPE_ASCII);

        EXIFEntry subSubSubSubIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(artist)), TIFF.TYPE_LONG);
        EXIFEntry subSubSubIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(subSubSubSubIFD)), TIFF.TYPE_LONG);
        EXIFEntry subSubIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(subSubSubIFD)), TIFF.TYPE_LONG);
        EXIFEntry subIFD = new EXIFEntry(TIFF.TAG_SUB_IFD, new IFD(Collections.singletonList(subSubIFD)), TIFF.TYPE_LONG);

        List<Entry> entries = Collections.<Entry>singletonList(subIFD);

        EXIFWriter writer = createWriter();

        ImageOutputStream stream = new NullImageOutputStream();
        writer.write(new IFD(entries), stream);

        assertEquals(stream.getStreamPosition(), writer.computeIFDSize(entries) + 12);
    }

    private static class NullImageOutputStream extends ImageOutputStreamImpl {
        @Override
        public void write(int b) throws IOException {
            streamPos++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            streamPos += len;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException("Method read not implemented");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException("Method read not implemented");
        }
    }
}
