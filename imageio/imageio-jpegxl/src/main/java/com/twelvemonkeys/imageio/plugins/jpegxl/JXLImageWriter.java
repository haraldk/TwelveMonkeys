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

import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ImageWriter for JPEG XL, encoded by converting to PNG and then to
 * JPEG XL with the external libjxl {@code cjxl} tool.
 */
public final class JXLImageWriter extends ImageWriter {

    JXLImageWriter(final ImageWriterSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image,
                      final ImageWriteParam param) throws IOException {
        if (!(output instanceof ImageOutputStream)) {
            throw new IllegalStateException("Output must be an ImageOutputStream");
        }

        BufferedImage buffered = toBufferedImage(image);
        Path dir = JXLTool.createTempDir();
        try {
            Path png = dir.resolve("image.png");
            Path jxl = dir.resolve("image.jxl");
            if (!javax.imageio.ImageIO.write(buffered, "png", png.toFile())) {
                throw new IOException("No PNG writer available for JPEG XL intermediate");
            }

            JXLTool.run("cjxl", png, jxl);
            ((ImageOutputStream) output).write(Files.readAllBytes(jxl));
        }
        finally {
            JXLTool.deleteRecursively(dir);
        }
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(final ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType,
                                               final ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertStreamMetadata(final IIOMetadata inData, final ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType,
                                            final ImageWriteParam param) {
        return null;
    }

    private static BufferedImage toBufferedImage(final IIOImage image) {
        if (image.getRenderedImage() instanceof BufferedImage) {
            return (BufferedImage) image.getRenderedImage();
        }

        BufferedImage buffered = new BufferedImage(
                image.getRenderedImage().getWidth(),
                image.getRenderedImage().getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = buffered.createGraphics();
        try {
            graphics.drawRenderedImage(image.getRenderedImage(), null);
        }
        finally {
            graphics.dispose();
        }
        return buffered;
    }
}
