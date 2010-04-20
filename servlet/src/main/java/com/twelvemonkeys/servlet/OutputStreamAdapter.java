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

package com.twelvemonkeys.servlet;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@code ServletOutputStream} implementation backed by a
 * {@link java.io.OutputStream}. For filters that need to buffer the
 * response and do post filtering, it may be used like this:<pre>
 * ByteArrayOutputStream buffer = new ByteArraOutputStream();
 * ServletOutputStream adapter = new OutputStreamAdapter(buffer);
 * </pre>
 * <p/>
 * As a {@code ServletOutputStream} is itself an {@code OutputStream}, this
 * class may also be used as a superclass for wrappers of other
 * {@code ServletOutputStream}s, like this:<pre>
 * class FilterServletOutputStream extends OutputStreamAdapter {
 *    public FilterServletOutputStream(ServletOutputStream out) {
 *       super(out);
 *    }
 *
 *    public void write(int abyte) {
 *       // do filtering...
 *       super.write(...);
 *    }
 * }
 *
 * ...
 *
 * ServletOutputStream original = response.getOutputStream();
 * ServletOutputStream wrapper = new FilterServletOutputStream(original);
 * </pre>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/OutputStreamAdapter.java#1 $
 *
 */
public class OutputStreamAdapter extends ServletOutputStream {

    /** The wrapped {@code OutputStream}. */
    protected final OutputStream mOut;

    /**
     * Creates an {@code OutputStreamAdapter}.
     *
     * @param pOut the wrapped {@code OutputStream}
     *
     * @throws IllegalArgumentException if {@code pOut} is {@code null}.
     */
    public OutputStreamAdapter(OutputStream pOut) {
        if (pOut == null) {
            throw new IllegalArgumentException("out == null");
        }
        mOut = pOut;
    }

    /**
     * Returns the wrapped {@code OutputStream}.
     *
     * @return the wrapped {@code OutputStream}.
     */
    public OutputStream getOutputStream() {
        return mOut;
    }

    public String toString() {
        return "ServletOutputStream adapted from " + mOut.toString();
    }

    /**
     * Writes a byte to the underlying stream.
     *
     * @param pByte the byte to write.
     *
     * @throws IOException if an error occurs during writing
     */
    public void write(int pByte)
            throws IOException {
        mOut.write(pByte);
    }

    // Overide for efficiency
    public void write(byte pBytes[])
            throws IOException {
        mOut.write(pBytes);
    }

    // Overide for efficiency
    public void write(byte pBytes[], int pOff, int pLen)
            throws IOException {
        mOut.write(pBytes, pOff, pLen);
    }
}
