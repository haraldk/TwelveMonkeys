/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff.jaiinterop;

import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests our TIFFImageMetadata works with JAI TIFFImageWriter.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReaderJDKJPEGInteroperabilityTest.java,v 1.0 08.05.12 15:25 haraldk Exp$
 */
public class TIFFImageMetadataJAInteroperabilityTest {
    private static final String JAI_TIFF_PROVIDER_CLASS_NAME = "com.github.jaiimageio.impl.plugins.tiff.TIFFImageWriterSpi";

    private ImageWriter createImageWriter() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");

        while (writers.hasNext()) {
            ImageWriter writer = writers.next();

            if (JAI_TIFF_PROVIDER_CLASS_NAME.equals(writer.getOriginatingProvider().getClass().getName())) {
                return writer;
            }
        }

        throw new AssertionError("Expected Spi not found (dependency issue?): " + JAI_TIFF_PROVIDER_CLASS_NAME);
    }


    @Test
    public void testRationalNeedsDenominator() {
        // Set the resolution to 200 dpi
        IIOMetadata ourMetadata = new TIFFImageMetadata(Arrays.asList(new TIFFEntry(TIFF.TAG_RESOLUTION_UNIT, 2), // Unit DPI (default)
                                                                      new TIFFEntry(TIFF.TAG_X_RESOLUTION, new Rational(200)),
                                                                      new TIFFEntry(TIFF.TAG_Y_RESOLUTION, new Rational(200))));

        ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
        ImageWriter writer = createImageWriter();
        IIOMetadata converted = writer.convertImageMetadata(ourMetadata, type, null);

        assertNotNull(converted);

        // Make sure we have x/y resolution in converted metadata
        IIOMetadataNode standardTree = (IIOMetadataNode) converted.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        String horizontalPixelSize = ((IIOMetadataNode) standardTree.getElementsByTagName("HorizontalPixelSize").item(0)).getAttribute("value");
        String verticalPixelSize = ((IIOMetadataNode) standardTree.getElementsByTagName("VerticalPixelSize").item(0)).getAttribute("value");

        // For some reason this is *pixel size* in *mm*...
        String expected = String.valueOf(2.54 / 200 * 10);
        assertEquals(expected, horizontalPixelSize);
        assertEquals(expected, verticalPixelSize);
    }
}
