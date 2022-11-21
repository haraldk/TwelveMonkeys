/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.color.ColorSpaces;

import org.junit.Test;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;

import static com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest.assertClassExists;
import static com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest.assertClassesExist;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * PAMImageWriterSpiTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PAMImageWriterSpiTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class PAMImageWriterSpiTest {

    private final ImageWriterSpi spi = new PAMImageWriterSpi();

    @Test
    public void getPluginClassName() {
        assertClassExists(spi.getPluginClassName(), ImageWriter.class);
    }

    @Test
    public void getImageReaderSpiNames() {
        assertClassesExist(spi.getImageReaderSpiNames(), ImageReaderSpi.class);
    }

    @Test
    public void getOutputTypes() {
        assertNotNull(spi.getOutputTypes());
    }

    @Test
    public void canEncodeImageBinary() {
        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY)));
    }

    @Test
    public void canNotEncodeImageIndexed() {
        assertFalse(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED)));
        assertFalse(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_INDEXED)));
    }

    @Test
    public void canEncodeImageGray() {
        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY)));

        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_USHORT_GRAY)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_USHORT_GRAY)));
    }

    @Test
    public void canEncodeImageGrayAlpha() {
        ComponentColorModel grayAlphaByte = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        assertTrue(spi.canEncodeImage(new ImageTypeSpecifier(grayAlphaByte, grayAlphaByte.createCompatibleSampleModel(1, 1))));

        ComponentColorModel grayAlphaUShort = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_USHORT);
        assertTrue(spi.canEncodeImage(new ImageTypeSpecifier(grayAlphaUShort, grayAlphaUShort.createCompatibleSampleModel(1, 1))));
    }

    @Test
    public void canEncodeImageRGB() {
        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR)));

        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB)));

        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR)));
    }

    @Test
    public void canEncodeImageARGB() {
        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR)));

        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR_PRE)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE)));

        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)));

        assertTrue(spi.canEncodeImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE)));
        assertTrue(spi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE)));
    }

    @Test
    public void canEncodeImageCMYK() {
        ComponentColorModel cmykByte = new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        assertTrue(spi.canEncodeImage(new ImageTypeSpecifier(cmykByte, cmykByte.createCompatibleSampleModel(1, 1))));

        ComponentColorModel cmykUShort = new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        assertTrue(spi.canEncodeImage(new ImageTypeSpecifier(cmykUShort, cmykUShort.createCompatibleSampleModel(1, 1))));

        ComponentColorModel cmykAlphaByte = new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        assertTrue(spi.canEncodeImage(new ImageTypeSpecifier(cmykAlphaByte, cmykAlphaByte.createCompatibleSampleModel(1, 1))));

        ComponentColorModel cmykAlphaUShort = new ComponentColorModel(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_USHORT);
        assertTrue(spi.canEncodeImage(new ImageTypeSpecifier(cmykAlphaUShort, cmykAlphaUShort.createCompatibleSampleModel(1, 1))));
    }

    @Test
    public void canNotEncodeImageNonGrayOrRGB() {
        ComponentColorModel xyzByte = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_CIEXYZ), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        assertFalse(spi.canEncodeImage(new ImageTypeSpecifier(xyzByte, xyzByte.createCompatibleSampleModel(1, 1))));

        ComponentColorModel xyzUshort = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_CIEXYZ), false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        assertFalse(spi.canEncodeImage(new ImageTypeSpecifier(xyzUshort, xyzUshort.createCompatibleSampleModel(1, 1))));
    }
}