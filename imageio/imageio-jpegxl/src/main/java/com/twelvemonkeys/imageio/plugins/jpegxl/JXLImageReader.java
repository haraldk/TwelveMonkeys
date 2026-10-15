/*
 * Copyright (c) 2026, TwelveMonkeys contributors
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jpegxl;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

/**
 * ImageReader for JPEG XL, decoded by converting to PNG with the
 * external libjxl {@code djxl} tool and reading the PNG result.
 */
public final class JXLImageReader extends ImageReader {
    private BufferedImage decoded;

    JXLImageReader(final ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    private BufferedImage decode() throws IOException {
        if (decoded != null) {
            return decoded;
        }
        if (!(input instanceof ImageInputStream)) {
            throw new IIOException("Expected ImageInputStream input");
        }

        ImageInputStream stream = (ImageInputStream) input;
        stream.seek(0);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }

        Path dir = JXLTool.createTempDir();
        try {
            Path jxl = dir.resolve("image.jxl");
            Path png = dir.resolve("image.png");
            Files.write(jxl, buffer.toByteArray());
            JXLTool.run("djxl", jxl, png);
            decoded = javax.imageio.ImageIO.read(png.toFile());
            if (decoded == null) {
                throw new IIOException("djxl produced no readable PNG output");
            }
            return decoded;
        }
        finally {
            JXLTool.deleteRecursively(dir);
        }
    }

    @Override
    public int getNumImages(final boolean allowSearch) {
        return 1;
    }

    @Override
    public int getWidth(final int imageIndex) throws IOException {
        checkIndex(imageIndex);
        return decode().getWidth();
    }

    @Override
    public int getHeight(final int imageIndex) throws IOException {
        checkIndex(imageIndex);
        return decode().getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        checkIndex(imageIndex);
        return Collections.singleton(ImageTypeSpecifier.createFromRenderedImage(decode())).iterator();
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        checkIndex(imageIndex);
        return decode();
    }

    @Override
    public IIOMetadata getStreamMetadata() {
        return null;
    }

    @Override
    public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        checkIndex(imageIndex);
        return null;
    }

    private void checkIndex(final int imageIndex) throws IOException {
        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("imageIndex must be 0: " + imageIndex);
        }
    }
}
