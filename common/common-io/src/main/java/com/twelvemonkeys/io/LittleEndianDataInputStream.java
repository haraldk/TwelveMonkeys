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
/*
 * From http://www.cafeaulait.org/books/javaio/ioexamples/index.html:
 *
 * Please feel free to use any fragment of this code you need in your own work.
 * As far as I am concerned, it's in the public domain. No permission is necessary
 * or required.  Credit is always appreciated if you use a large chunk or base a
 * significant product on one of my examples, but that's not required either.
 *
 * Elliotte Rusty Harold
 */

package com.twelvemonkeys.io;

import com.twelvemonkeys.lang.Validate;

import java.io.*;

/**
 * A little endian input stream reads two's complement,
 * little endian integers, floating point numbers, and characters
 * and returns them as Java primitive types.
 * <p>
 * The standard {@code java.io.DataInputStream} class
 * which this class imitates reads big endian quantities.
 * </p>
 * <p>
 * <em>Warning:
 * The {@code DataInput} and {@code DataOutput} interfaces
 * specifies big endian byte order in their documentation.
 * This means that this class is, strictly speaking, not a proper
 * implementation. However, I don't see a reason for the these interfaces to
 * specify the byte order of their underlying representations.
 * </em>
 * </p>
 *
 * @see com.twelvemonkeys.io.LittleEndianRandomAccessFile
 * @see java.io.DataInputStream
 * @see java.io.DataInput
 * @see java.io.DataOutput
 *
 * @author Elliotte Rusty Harold
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version 2
 */
public class LittleEndianDataInputStream extends FilterInputStream implements DataInput {
    // TODO: Optimize by reading into a fixed size (8 bytes) buffer instead of individual read operations?
    /**
     * Creates a new little endian input stream and chains it to the
     * input stream specified by the {@code pStream} argument.
     *
     * @param pStream the underlying input stream.
     * @see java.io.FilterInputStream#in
     */
    public LittleEndianDataInputStream(final InputStream pStream) {
        super(Validate.notNull(pStream, "stream"));
    }

    /**
     * Reads a {@code boolean} from the underlying input stream by
     * reading a single byte. If the byte is zero, false is returned.
     * If the byte is positive, true is returned.
     *
     * @return the {@code boolean} value read.
     * @throws EOFException if the end of the underlying input stream
     *                      has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public boolean readBoolean() throws IOException {
        int b = in.read();

        if (b < 0) {
            throw new EOFException();
        }

        return b != 0;
    }

    /**
     * Reads a signed {@code byte} from the underlying input stream
     * with value between -128 and 127
     *
     * @return the {@code byte} value read.
     * @throws EOFException if the end of the underlying input stream
     *                      has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public byte readByte() throws IOException {
        int b = in.read();

        if (b < 0) {
            throw new EOFException();
        }

        return (byte) b;

    }

    /**
     * Reads an unsigned {@code byte} from the underlying
     * input stream with value between 0 and 255
     *
     * @return the {@code byte} value read.
     * @throws EOFException if the end of the underlying input
     *                      stream has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public int readUnsignedByte() throws IOException {
        int b = in.read();

        if (b < 0) {
            throw new EOFException();
        }

        return b;
    }

    /**
     * Reads a two byte signed {@code short} from the underlying
     * input stream in little endian order, low byte first.
     *
     * @return the {@code short} read.
     * @throws EOFException if the end of the underlying input stream
     *                      has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public short readShort() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();

        // only need to test last byte read
        // if byte1 is -1 so is byte2
        if (byte2 < 0) {
            throw new EOFException();
        }

        return (short) (((byte2 << 24) >>> 16) | (byte1 << 24) >>> 24);
    }

    /**
     * Reads a two byte unsigned {@code short} from the underlying
     * input stream in little endian order, low byte first.
     *
     * @return the int value of the unsigned short read.
     * @throws EOFException if the end of the underlying input stream
     *                      has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public int readUnsignedShort() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();

        if (byte2 < 0) {
            throw new EOFException();
        }

        return (byte2 << 8) + byte1;
    }

    /**
     * Reads a two byte Unicode {@code char} from the underlying
     * input stream in little endian order, low byte first.
     *
     * @return the int value of the unsigned short read.
     * @throws EOFException if the end of the underlying input stream
     *                      has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public char readChar() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();

        if (byte2 < 0) {
            throw new EOFException();
        }

        return (char) (((byte2 << 24) >>> 16) | ((byte1 << 24) >>> 24));
    }


    /**
     * Reads a four byte signed {@code int} from the underlying
     * input stream in little endian order, low byte first.
     *
     * @return the {@code int} read.
     * @throws EOFException if the end of the underlying input stream
     *                      has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public int readInt() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();
        int byte3 = in.read();
        int byte4 = in.read();

        if (byte4 < 0) {
            throw new EOFException();
        }

        return (byte4 << 24) | ((byte3 << 24) >>> 8)
                | ((byte2 << 24) >>> 16) | ((byte1 << 24) >>> 24);
    }

    /**
     * Reads an eight byte signed {@code int} from the underlying
     * input stream in little endian order, low byte first.
     *
     * @return the {@code int} read.
     * @throws EOFException if the end of the underlying input stream
     *                      has been reached
     * @throws IOException  if the underlying stream throws an IOException.
     */
    public long readLong() throws IOException {
        long byte1 = in.read();
        long byte2 = in.read();
        long byte3 = in.read();
        long byte4 = in.read();
        long byte5 = in.read();
        long byte6 = in.read();
        long byte7 = in.read();
        long byte8 = in.read();

        if (byte8 < 0) {
            throw new EOFException();
        }

        return (byte8 << 56) | ((byte7 << 56) >>> 8)
                | ((byte6 << 56) >>> 16) | ((byte5 << 56) >>> 24)
                | ((byte4 << 56) >>> 32) | ((byte3 << 56) >>> 40)
                | ((byte2 << 56) >>> 48) | ((byte1 << 56) >>> 56);
    }

    /**
     * Reads a string of no more than 65,535 characters
     * from the underlying input stream using UTF-8
     * encoding. This method first reads a two byte short
     * in <b>big</b> endian order as required by the
     * UTF-8 specification. This gives the number of bytes in
     * the UTF-8 encoded version of the string.
     * Next this many bytes are read and decoded as UTF-8
     * encoded characters.
     *
     * @return the decoded string
     * @throws UTFDataFormatException if the string cannot be decoded
     * @throws IOException            if the underlying stream throws an IOException.
     */
    public String readUTF() throws IOException {
        int byte1 = in.read();
        int byte2 = in.read();

        if (byte2 < 0) {
            throw new EOFException();
        }

        int numbytes = (byte1 << 8) + byte2;
        char result[] = new char[numbytes];
        int numread = 0;
        int numchars = 0;

        while (numread < numbytes) {
            int c1 = readUnsignedByte();
            int c2, c3;

            // The first four bits of c1 determine how many bytes are in this char
            int test = c1 >> 4;
            if (test < 8) {  // one byte
                numread++;
                result[numchars++] = (char) c1;
            }
            else if (test == 12 || test == 13) { // two bytes
                numread += 2;

                if (numread > numbytes) {
                    throw new UTFDataFormatException();
                }

                c2 = readUnsignedByte();

                if ((c2 & 0xC0) != 0x80) {
                    throw new UTFDataFormatException();
                }

                result[numchars++] = (char) (((c1 & 0x1F) << 6) | (c2 & 0x3F));
            }
            else if (test == 14) { // three bytes
                numread += 3;

                if (numread > numbytes) {
                    throw new UTFDataFormatException();
                }

                c2 = readUnsignedByte();
                c3 = readUnsignedByte();

                if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80)) {
                    throw new UTFDataFormatException();
                }

                result[numchars++] = (char) (((c1 & 0x0F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F));
            }
            else { // malformed
                throw new UTFDataFormatException();
            }

        }  // end while

        return new String(result, 0, numchars);

    }

    /**
     * @return the next eight bytes of this input stream, interpreted as a
     *         little endian {@code double}.
     * @throws EOFException if end of stream occurs before eight bytes
     *                      have been read.
     * @throws IOException  if an I/O error occurs.
     */
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * @return the next four bytes of this input stream, interpreted as a
     *         little endian {@code int}.
     * @throws EOFException if end of stream occurs before four bytes
     *                      have been read.
     * @throws IOException  if an I/O error occurs.
     */
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * See the general contract of the {@code skipBytes}
     * method of {@code DataInput}.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @param pLength the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @exception IOException if an I/O error occurs.
     */
    public final int skipBytes(int pLength) throws IOException {
        // NOTE: There was probably a bug in ERH's original code here, as skip
        // never returns -1, but returns 0 if no more bytes can be skipped...
        int total = 0;
        int skipped;

        while ((total < pLength) && ((skipped = (int) in.skip(pLength - total)) > 0)) {
            total += skipped;
        }

        return total;
    }

    /**
     * See the general contract of the {@code readFully} method of {@code DataInput}.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     * </p>
     *
     * @param pBytes the buffer into which the data is read.
     * @throws EOFException if this input stream reaches the end before
     *                      reading all the bytes.
     * @throws IOException  if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    public final void readFully(byte pBytes[]) throws IOException {
        readFully(pBytes, 0, pBytes.length);
    }

    /**
     * See the general contract of the {@code readFully} method of {@code DataInput}.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     * </p>
     *
     * @param pBytes   the buffer into which the data is read.
     * @param pOffset the start offset of the data.
     * @param pLength the number of bytes to read.
     * @throws EOFException if this input stream reaches the end before
     *                      reading all the bytes.
     * @throws IOException  if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    public final void readFully(byte pBytes[], int pOffset, int pLength) throws IOException {
        if (pLength < 0) {
            throw new IndexOutOfBoundsException();
        }

        int count = 0;

        while (count < pLength) {
            int read = in.read(pBytes, pOffset + count, pLength - count);

            if (read < 0) {
                throw new EOFException();
            }

            count += read;
        }
    }

    /**
     * See the general contract of the {@code readLine}
     * method of {@code DataInput}.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @deprecated This method does not properly convert bytes to characters.
     *
     * @return     the next line of text from this input stream.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.BufferedReader#readLine()
     * @see        java.io.DataInputStream#readLine()
     */
    public String readLine() throws IOException {
        DataInputStream ds = new DataInputStream(in);
        return ds.readLine();
    }
}
