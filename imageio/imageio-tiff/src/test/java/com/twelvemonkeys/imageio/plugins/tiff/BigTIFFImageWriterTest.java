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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.twelvemonkeys.imageio.util.ImageReaderAbstractTest.assertRGBEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 * BigTIFFImageWriterTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BigTIFFImageWriterTest.java,v 1.0 19.09.13 13:22 haraldk Exp$
 */
public class BigTIFFImageWriterTest extends ImageWriterAbstractTest<TIFFImageWriter> {
    @Override
    protected ImageWriterSpi createProvider() {
        return new BigTIFFImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return Arrays.asList(
                new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB),
                new BufferedImage(301, 199, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(299, 201, BufferedImage.TYPE_3BYTE_BGR),
                new BufferedImage(160, 90, BufferedImage.TYPE_4BYTE_ABGR),
                new BufferedImage(90, 160, BufferedImage.TYPE_BYTE_GRAY),
                new BufferedImage(30, 20, BufferedImage.TYPE_USHORT_GRAY)
        );
    }

    @Test
    public void roundrtip() throws IOException {
        TIFFImageWriter writer = createWriter();
        ImageReader reader = ImageIO.getImageReader(writer);

        try (ImageInputStream input = ImageIO.createImageInputStream(getClassLoaderResource("/bigtiff/BigTIFF.tif"))) {
            reader.setInput(input);
            BufferedImage image = reader.read(0);

            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            try (ImageOutputStream output = ImageIO.createImageOutputStream(temp)) {
                writer.setOutput(output);
                writer.write(image);
            }
            finally {
                writer.dispose();
            }

            // Validate we actually write BigTIFF
            byte[] data = temp.toByteArray();
            assertArrayEquals(new byte[] { 'M', 'M', 0, TIFF.BIGTIFF_MAGIC}, Arrays.copyOf(data, 4));

            // Read image back and see that it is the same
            try (ImageInputStream stream = new ByteArrayImageInputStream(data)) {
                reader.setInput(stream);
                BufferedImage after = reader.read(0);

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        assertRGBEquals("Pixel values differ: ", image.getRGB(x, y), after.getRGB(x, y), 0);
                    }
                }
            }
        }
        finally {
            reader.dispose();
        }
    }
}
