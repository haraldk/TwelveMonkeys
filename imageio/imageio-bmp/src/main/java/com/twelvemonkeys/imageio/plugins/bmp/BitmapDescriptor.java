/*
 * Copyright (c) 2009, Harald Kuhr
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
package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.lang.Validate;

import java.awt.image.BufferedImage;

/**
 * Describes a bitmap structure.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: Bitmap.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
 */
abstract class BitmapDescriptor {
    protected final DirectoryEntry entry;
    protected final DIBHeader header;

    protected BufferedImage image;
    protected BitmapMask mask;

    public BitmapDescriptor(final DirectoryEntry pEntry, final DIBHeader pHeader) {
        Validate.notNull(pEntry, "entry");
        Validate.notNull(pHeader, "header");
        
        entry = pEntry;
        header = pHeader;
    }

    abstract public BufferedImage getImage();

    public final int getWidth() {
        return entry.getWidth();
    }

    public final int getHeight() {
        return entry.getHeight();
    }

    protected final int getColorCount() {
        return entry.getColorCount() != 0 ? entry.getColorCount() : 1 << getBitCount();
    }

    protected final int getBitCount() {
        return entry.getBitCount() != 0 ? entry.getBitCount() : header.getBitCount();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + entry + ", " + header + "]";
    }

    public final void setMask(final BitmapMask mask) {
        this.mask = mask;
    }

    public final boolean hasMask() {
        return header.getHeight() == getHeight() * 2;
    }
}
