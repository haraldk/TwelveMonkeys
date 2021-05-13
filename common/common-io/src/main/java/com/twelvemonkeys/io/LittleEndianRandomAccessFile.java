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

package com.twelvemonkeys.io;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * A replacement for {@link java.io.RandomAccessFile} that is capable of reading
 * and writing data in little endian byte order.
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
 * @see com.twelvemonkeys.io.LittleEndianDataInputStream
 * @see com.twelvemonkeys.io.LittleEndianDataOutputStream
 * @see java.io.RandomAccessFile
 * @see java.io.DataInput
 * @see java.io.DataOutput
 *
 * @author Elliotte Rusty Harold
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/LittleEndianRandomAccessFile.java#1 $
 */
public class LittleEndianRandomAccessFile implements DataInput, DataOutput {
    private RandomAccessFile file;

    public LittleEndianRandomAccessFile(final String pName, final String pMode) throws FileNotFoundException {
        this(FileUtil.resolve(pName), pMode);
    }

    public LittleEndianRandomAccessFile(final File pFile, final String pMode) throws FileNotFoundException {
        file = new RandomAccessFile(pFile, pMode);
    }

    public void close() throws IOException {
        file.close();
    }

    public FileChannel getChannel() {
        return file.getChannel();
    }

    public FileDescriptor getFD() throws IOException {
        return file.getFD();
    }

    public long getFilePointer() throws IOException {
        return file.getFilePointer();
    }

    public long length() throws IOException {
        return file.length();
    }

    public int read() throws IOException {
        return file.read();
    }

    public int read(final byte[] b) throws IOException {
        return file.read(b);
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        return file.read(b, off, len);
    }

    public void readFully(final byte[] b) throws IOException {
        file.readFully(b);
    }

    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        file.readFully(b, off, len);
    }

    public String readLine() throws IOException {
        return file.readLine();
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
        int b = file.read();

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
        int b = file.read();

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
        int b = file.read();

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
        int byte1 = file.read();
        int byte2 = file.read();

        // only need to test last byte read
        // if byte1 is -1 so is byte2
        if (byte2 < 0) {
            throw new EOFException();
        }

        return (short) (((byte2 << 24) >>> 16) + (byte1 << 24) >>> 24);
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
        int byte1 = file.read();
        int byte2 = file.read();

        if (byte2 < 0) {
            throw new EOFException();
        }

        //return ((byte2 << 24) >> 16) + ((byte1 << 24) >> 24);
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
        int byte1 = file.read();
        int byte2 = file.read();

        if (byte2 < 0) {
            throw new EOFException();
        }

        return (char) (((byte2 << 24) >>> 16) + ((byte1 << 24) >>> 24));
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
        int byte1 = file.read();
        int byte2 = file.read();
        int byte3 = file.read();
        int byte4 = file.read();

        if (byte4 < 0) {
            throw new EOFException();
        }

        return (byte4 << 24) + ((byte3 << 24) >>> 8) + ((byte2 << 24) >>> 16) + ((byte1 << 24) >>> 24);
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
        long byte1 = file.read();
        long byte2 = file.read();
        long byte3 = file.read();
        long byte4 = file.read();
        long byte5 = file.read();
        long byte6 = file.read();
        long byte7 = file.read();
        long byte8 = file.read();

        if (byte8 < 0) {
            throw new EOFException();
        }

        return (byte8 << 56) + ((byte7 << 56) >>> 8)
                + ((byte6 << 56) >>> 16) + ((byte5 << 56) >>> 24)
                + ((byte4 << 56) >>> 32) + ((byte3 << 56) >>> 40)
                + ((byte2 << 56) >>> 48) + ((byte1 << 56) >>> 56);

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
        int byte1 = file.read();
        int byte2 = file.read();

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
     * Sets the file-pointer offset, measured from the beginning of this
     * file, at which the next read or write occurs.  The offset may be 
     * set beyond the end of the file. Setting the offset beyond the end
     * of the file does not change the file length.  The file length will
     * change only by writing after the offset has been set beyond the end
     * of the file.
     *
     * @param      pos   the offset position, measured in bytes from the
     *                   beginning of the file, at which to set the file
     *                   pointer.
     * @exception  IOException  if {@code pos} is less than
     *                          {@code 0} or if an I/O error occurs.
     */
    public void seek(final long pos) throws IOException {
        file.seek(pos);
    }

    public void setLength(final long newLength) throws IOException {
        file.setLength(newLength);
    }

    public int skipBytes(final int n) throws IOException {
        return file.skipBytes(n);
    }

    public void write(final byte[] b) throws IOException {
        file.write(b);
    }

    public void write(final byte[] b, final int off, final int len) throws IOException {
        file.write(b, off, len);
    }

    public void write(final int b) throws IOException {
        file.write(b);
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
        file.write(pByte);
    }

    /**
     * Writes a two byte {@code short} to the underlying output stream in
     * little endian order, low byte first.
     *
     * @param pShort the {@code short} to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeShort(int pShort) throws IOException {
        file.write(pShort & 0xFF);
        file.write((pShort >>> 8) & 0xFF);
    }

    /**
     * Writes a two byte {@code char} to the underlying output stream
     * in little endian order, low byte first.
     *
     * @param pChar the {@code char} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeChar(int pChar) throws IOException {
        file.write(pChar & 0xFF);
        file.write((pChar >>> 8) & 0xFF);
    }

    /**
     * Writes a four-byte {@code int} to the underlying output stream
     * in little endian order, low byte first, high byte last
     *
     * @param pInt the {@code int} to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeInt(int pInt) throws IOException {
        file.write(pInt & 0xFF);
        file.write((pInt >>> 8) & 0xFF);
        file.write((pInt >>> 16) & 0xFF);
        file.write((pInt >>> 24) & 0xFF);
    }

    /**
     * Writes an eight-byte {@code long} to the underlying output stream
     * in little endian order, low byte first, high byte last
     *
     * @param pLong the {@code long} to be written.
     * @throws IOException if the underlying stream throws an IOException.
     */
    public void writeLong(long pLong) throws IOException {
        file.write((int) pLong & 0xFF);
        file.write((int) (pLong >>> 8) & 0xFF);
        file.write((int) (pLong >>> 16) & 0xFF);
        file.write((int) (pLong >>> 24) & 0xFF);
        file.write((int) (pLong >>> 32) & 0xFF);
        file.write((int) (pLong >>> 40) & 0xFF);
        file.write((int) (pLong >>> 48) & 0xFF);
        file.write((int) (pLong >>> 56) & 0xFF);
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
     * if by the {@code writeByte()} method.
     *
     * @param pString the {@code String} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     * @see #writeByte(int)
     * @see #file
     */
    public void writeBytes(String pString) throws IOException {
        int length = pString.length();

        for (int i = 0; i < length; i++) {
            file.write((byte) pString.charAt(i));
        }
    }

    /**
     * Writes a string to the underlying output stream as a sequence of
     * characters. Each character is written to the data output stream as
     * if by the {@code writeChar} method.
     *
     * @param pString a {@code String} value to be written.
     * @throws IOException if the underlying stream throws an IOException.
     * @see #writeChar(int)
     * @see #file
     */
    public void writeChars(String pString) throws IOException {
        int length = pString.length();

        for (int i = 0; i < length; i++) {
            int c = pString.charAt(i);
            file.write(c & 0xFF);
            file.write((c >>> 8) & 0xFF);
        }
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

        file.write((numbytes >>> 8) & 0xFF);
        file.write(numbytes & 0xFF);

        for (int i = 0; i < numchars; i++) {
            int c = pString.charAt(i);

            if ((c >= 0x0001) && (c <= 0x007F)) {
                file.write(c);
            }
            else if (c > 0x07FF) {
                file.write(0xE0 | ((c >> 12) & 0x0F));
                file.write(0x80 | ((c >> 6) & 0x3F));
                file.write(0x80 | (c & 0x3F));
            }
            else {
                file.write(0xC0 | ((c >> 6) & 0x1F));
                file.write(0x80 | (c & 0x3F));
            }
        }
    }
}
