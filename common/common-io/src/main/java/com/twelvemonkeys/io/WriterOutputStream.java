/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.DateUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Wraps a {@code Writer} in an {@code OutputStream}.
 * <p/>
 * <em>Instances of this class are not thread-safe.</em>
 * <p/>
 * <em>NOTE: This class is probably not the right way of solving your problem,
 * however it might prove useful in JSPs etc.
 * If possible, it's always better to use the {@code Writer}'s underlying
 * {@code OutputStream}, or wrap it's native backing.
 * </em>
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/WriterOutputStream.java#2 $
 */
public class WriterOutputStream extends OutputStream {
    protected Writer writer;
    final protected Decoder decoder;
    final ByteArrayOutputStream bufferStream = new FastByteArrayOutputStream(1024);

    private volatile boolean isFlushing = false; // Ugly but critical...

    private static final boolean NIO_AVAILABLE = isNIOAvailable();

    private static boolean isNIOAvailable() {
        try {
            Class.forName("java.nio.charset.Charset");
            return true;
        }
        catch (Throwable t) {
            // Ignore
        }

        return false;
    }

    public WriterOutputStream(final Writer pWriter, final String pCharset) {
        writer = pWriter;
        decoder = getDecoder(pCharset);
    }

    public WriterOutputStream(final Writer pWriter) {
        this(pWriter, null);
    }

    private static Decoder getDecoder(final String pCharset) {
        // NOTE: The CharsetDecoder is typically 10-20% faster than
        // StringDecoder according to my tests
        // StringEncoder is horribly slow on 1.2 systems, but there's no
        // alternative...
        if (NIO_AVAILABLE) {
            return new CharsetDecoder(pCharset);
        }

        return new StringDecoder(pCharset);
    }

    @Override
    public void close() throws IOException {
        flush();
        writer.close();
        writer = null;
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        writer.flush();
    }

    @Override
    public final void write(byte[] pBytes) throws IOException {
        if (pBytes == null) {
            throw new NullPointerException("bytes == null");
        }
        write(pBytes, 0, pBytes.length);
    }

    @Override
    public final void write(byte[] pBytes, int pOffset, int pLength) throws IOException {
        flushBuffer();
        decoder.decodeTo(writer, pBytes, pOffset, pLength);
    }

    @Override
    public final void write(int pByte)  {
        // TODO: Is it possible to know if this is a good place in the stream to
        // flush? It might be in the middle of a multi-byte encoded character..
        bufferStream.write(pByte);
    }

    private void flushBuffer() throws IOException {
        if (!isFlushing && bufferStream.size() > 0) {
            isFlushing = true;
            bufferStream.writeTo(this); // NOTE: Avoids cloning buffer array
            bufferStream.reset();
            isFlushing = false;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public static void main(String[] pArgs) throws IOException {
        int iterations = 1000000;

        byte[] bytes = "������ klashf lkash ljah lhaaklhghdfgu ksd".getBytes("UTF-8");

        Decoder d;
        long start;
        long time;
        Writer sink = new PrintWriter(new NullOutputStream());
        StringWriter writer;
        String str;

        d = new StringDecoder("UTF-8");
        for (int i = 0; i < 10000; i++) {
            d.decodeTo(sink, bytes, 0, bytes.length);
        }
        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            d.decodeTo(sink, bytes, 0, bytes.length);
        }
        time = DateUtil.delta(start);
        System.out.println("StringDecoder");
        System.out.println("time: " + time);

        writer = new StringWriter();
        d.decodeTo(writer, bytes, 0, bytes.length);
        str = writer.toString();
        System.out.println("str: \"" + str + "\"");
        System.out.println("chars.length: " + str.length());
        System.out.println();

        if (NIO_AVAILABLE) {
            d = new CharsetDecoder("UTF-8");
            for (int i = 0; i < 10000; i++) {
                d.decodeTo(sink, bytes, 0, bytes.length);
            }
            start = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                d.decodeTo(sink, bytes, 0, bytes.length);
            }
            time = DateUtil.delta(start);
            System.out.println("CharsetDecoder");
            System.out.println("time: " + time);
            writer = new StringWriter();
            d.decodeTo(writer, bytes, 0, bytes.length);
            str = writer.toString();
            System.out.println("str: \"" + str + "\"");
            System.out.println("chars.length: " + str.length());
            System.out.println();
        }

        OutputStream os = new WriterOutputStream(new PrintWriter(System.out), "UTF-8");
        os.write(bytes);
        os.flush();
        System.out.println();

        for (byte b : bytes) {
            os.write(b & 0xff);
        }
        os.flush();
    }

    ///////////////////////////////////////////////////////////////////////////
    private static interface Decoder {
        void decodeTo(Writer pWriter, byte[] pBytes, int pOffset, int pLength) throws IOException;
    }

    private static final class CharsetDecoder implements Decoder {
        final Charset mCharset;

        CharsetDecoder(String pCharset) {
            // Handle null-case, to get default charset
            String charset = pCharset != null ? pCharset :
                    System.getProperty("file.encoding", "ISO-8859-1");
            mCharset = Charset.forName(charset);
        }

        public void decodeTo(Writer pWriter, byte[] pBytes, int pOffset, int pLength) throws IOException {
            CharBuffer cb = mCharset.decode(ByteBuffer.wrap(pBytes, pOffset, pLength));
            pWriter.write(cb.array(), 0, cb.length());
        }
    }

    private static final class StringDecoder implements Decoder {
        final String mCharset;

        StringDecoder(String pCharset) {
            mCharset = pCharset;
        }

        public void decodeTo(Writer pWriter, byte[] pBytes, int pOffset, int pLength) throws IOException {
            String str = mCharset == null ?
                    new String(pBytes, pOffset, pLength) :
                    new String(pBytes, pOffset, pLength, mCharset);

            pWriter.write(str);
        }
    }
}