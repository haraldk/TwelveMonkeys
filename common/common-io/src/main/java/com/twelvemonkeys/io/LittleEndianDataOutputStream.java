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
 * A little endian output stream writes primitive Java numbers
 * and characters to an output stream in a little endian format.
 * <p/>
 * The standard {@code java.io.DataOutputStream} class which this class
 * imitates uses big endian integers.
 * <p/>
 * <em>Warning:
 * The {@code DataInput} and {@code DataOutput} interfaces
 * specifies big endian byte order in their documentation.
 * This means that this class is, strictly speaking, not a proper
 * implementation. However, I don't see a reason for the these interfaces to
 * specify the byte order of their underlying representations.
 * </em>
 *
 * @see com.twelvemonkeys.io.LittleEndianRandomAccessFile
 * @see java.io.DataOutputStream
 * @see java.io.DataInput
 * @see java.io.DataOutput
 *
 * @author Elliotte Rusty Harold
 * @version 1.0.1, 19 May 1999
 */
public class LittleEndianDataOutputStream extends FilterOutputStream implements DataOutput {

    /**
     * The number of bytes written so far to the little endian output stream.
     */
    protected int bytesWritten;

    /**
     * Creates a new little endian output stream and chains it to the
     * output stream specified by the {@code pStream} argument.
     *
     * @param pStream the underlying output stream.
     * @see java.io.FilterOutputStream#out
     */
    public LittleEndianDataOutputStream(OutputStream pStream) {
        super(Validate.notNull(pStream, "stream"));
    }

    /**
     * Writes the specified byte value to the underlying output stream.
     *
     * @param pByte the {@code byte} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public synchronized void write(int pByte) throws IOException {
        out.write(pByte);
        bytesWritten++;
    }

    /**
     * Writes {@code pLength} bytes from the specified byte array
     * starting at {@code pOffset} to the underlying output stream.
     *
     * @param pBytes   the data.
     * @param pOffset the start offset in the data.
     * @param pLength the number of bytes to write.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public synchronized void write(byte[] pBytes, int pOffset, int pLength) throws IOException {
        out.write(pBytes, pOffset, pLength);
        bytesWritten += pLength;
    }


    /**
     * Writes a {@code boolean} to the underlying output stream as
     * a single byte. If the argument is true, the byte value 1 is written.
     * If the argument is false, the byte value {@code 0} in written.
     *
     * @param pBoolean the {@code boolean} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeBoolean(boolean pBoolean) throws IOException {
        if (pBoolean) {
            write(1);
        }
        else {
            write(0);
        }
    }

    /**
     * Writes out a {@code byte} to the underlying output stream
     *
     * @param pByte the {@code byte} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeByte(int pByte) throws IOException {
        out.write(pByte);
        bytesWritten++;
    }

    /**
     * Writes a two byte {@code short} to the underlying output stream in
     * little endian order, low byte first.
     *
     * @param pShort the {@code short} to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeShort(int pShort) throws IOException {
        out.write(pShort & 0xFF);
        out.write((pShort >>> 8) & 0xFF);
        bytesWritten += 2;
    }

    /**
     * Writes a two byte {@code char} to the underlying output stream
     * in little endian order, low byte first.
     *
     * @param pChar the {@code char} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeChar(int pChar) throws IOException {
        out.write(pChar & 0xFF);
        out.write((pChar >>> 8) & 0xFF);
        bytesWritten += 2;
    }

    /**
     * Writes a four-byte {@code int} to the underlying output stream
     * in little endian order, low byte first, high byte last
     *
     * @param pInt the {@code int} to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeInt(int pInt) throws IOException {
        out.write(pInt & 0xFF);
        out.write((pInt >>> 8) & 0xFF);
        out.write((pInt >>> 16) & 0xFF);
        out.write((pInt >>> 24) & 0xFF);
        bytesWritten += 4;

    }

    /**
     * Writes an eight-byte {@code long} to the underlying output stream
     * in little endian order, low byte first, high byte last
     *
     * @param pLong the {@code long} to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeLong(long pLong) throws IOException {
        out.write((int) pLong & 0xFF);
        out.write((int) (pLong >>> 8) & 0xFF);
        out.write((int) (pLong >>> 16) & 0xFF);
        out.write((int) (pLong >>> 24) & 0xFF);
        out.write((int) (pLong >>> 32) & 0xFF);
        out.write((int) (pLong >>> 40) & 0xFF);
        out.write((int) (pLong >>> 48) & 0xFF);
        out.write((int) (pLong >>> 56) & 0xFF);
        bytesWritten += 8;
    }

    /**
     * Writes a 4 byte Java float to the underlying output stream in
     * little endian order.
     *
     * @param f the {@code float} value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeFloat(float f) throws IOException {
        writeInt(Float.floatToIntBits(f));
    }

    /**
     * Writes an 8 byte Java double to the underlying output stream in
     * little endian order.
     *
     * @param d the {@code double} value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeDouble(double d) throws IOException {
        writeLong(Double.doubleToLongBits(d));
    }

    /**
     * Writes a string to the underlying output stream as a sequence of
     * bytes. Each character is written to the data output stream as
     * if by the {@link #writeByte(int)} method.
     *
     * @param pString the {@code String} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     * @see #writeByte(int)
     * @see #out
     */
    public void writeBytes(String pString) throws IOException {
        int length = pString.length();

        for (int i = 0; i < length; i++) {
            out.write((byte) pString.charAt(i));
        }

        bytesWritten += length;
    }

    /**
     * Writes a string to the underlying output stream as a sequence of
     * characters. Each character is written to the data output stream as
     * if by the {@code writeChar} method.
     *
     * @param pString a {@code String} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     * @see #writeChar(int)
     * @see #out
     */
    public void writeChars(String pString) throws IOException {
        int length = pString.length();

        for (int i = 0; i < length; i++) {
            int c = pString.charAt(i);
            out.write(c & 0xFF);
            out.write((c >>> 8) & 0xFF);
        }

        bytesWritten += length * 2;
    }

    /**
     * Writes a string of no more than 65,535 characters
     * to the underlying output stream using UTF-8
     * encoding. This method first writes a two byte short
     * in <b>big</b> endian order as required by the
     * UTF-8 specification. This gives the number of bytes in the
     * UTF-8 encoded version of the string, not the number of characters
     * in the string. Next each character of the string is written
     * using the UTF-8 encoding for the character.
     *
     * @param pString the string to be written.
     * @throws UTFDataFormatException if the string is longer than
     *                                65,535 characters.
     * @throws IOException            if the underlying stream throws an IOException.
     */
    public void writeUTF(String pString) throws IOException {
        int numchars = pString.length();
        int numbytes = 0;

        for (int i = 0; i < numchars; i++) {
            int c = pString.charAt(i);

            if ((c >= 0x0001) && (c <= 0x007F)) {
                numbytes++;
            }
            else if (c > 0x07FF) {
                numbytes += 3;
            }
            else {
                numbytes += 2;
            }
        }

        if (numbytes > 65535) {
            throw new UTFDataFormatException();
        }

        out.write((numbytes >>> 8) & 0xFF);
        out.write(numbytes & 0xFF);

        for (int i = 0; i < numchars; i++) {
            int c = pString.charAt(i);

            if ((c >= 0x0001) && (c <= 0x007F)) {
                out.write(c);
            }
            else if (c > 0x07FF) {
                out.write(0xE0 | ((c >> 12) & 0x0F));
                out.write(0x80 | ((c >> 6) & 0x3F));
                out.write(0x80 | (c & 0x3F));
                bytesWritten += 2;
            }
            else {
                out.write(0xC0 | ((c >> 6) & 0x1F));
                out.write(0x80 | (c & 0x3F));
                bytesWritten += 1;
            }
        }

        bytesWritten += numchars + 2;
    }

    /**
     * Returns the number of bytes written to this little endian output stream.
     * (This class is not thread-safe with respect to this method. It is
     * possible that this number is temporarily less than the actual
     * number of bytes written.)
     * @return the value of the {@code written} field.
     * @see     #bytesWritten
     */
    public int size() {
        return bytesWritten;
  }
}