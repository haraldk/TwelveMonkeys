/*
 * Copyright (c) 2026, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jxl;

import com.twelvemonkeys.imageio.ImageReaderBase;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

/**
 * ImageReader for JPEG XL (JXL) images, backed by the JXLatte pure Java decoder.
 * <p>
 * NOTE: Work in progress, decoding is not yet implemented.
 * </p>
 */
final class JXLImageReader extends ImageReaderBase {

    JXLImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int getHeight(final int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
