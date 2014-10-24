/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;

/**
 * ImageReader for Microsoft Windows CUR (cursor) format.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CURImageReader.java,v 1.0 Apr 20, 2009 11:54:28 AM haraldk Exp$
 *
 * @see ICOImageReader
 */
public final class CURImageReader extends DIBImageReader {
    public CURImageReader() {
        super(new CURImageReaderSpi());
    }

    protected CURImageReader(final ImageReaderSpi pProvider) {
        super(pProvider);
    }

    /**
     * Returns the hot spot location for the cursor.
     *
     * @param pImageIndex the index of the cursor in the current input.
     * @return the hot spot location for the cursor
     *
     * @throws java.io.IOException if an I/O exception occurs during reading of image meta data
     * @throws IndexOutOfBoundsException if {@code pImageIndex} is less than {@code 0} or greater than/equal to
     *         the number of cursors in the file
     */
    public final Point getHotSpot(final int pImageIndex) throws IOException {
        DirectoryEntry.CUREntry entry = (DirectoryEntry.CUREntry) getEntry(pImageIndex);
        return entry.getHotspot();
    }
}
