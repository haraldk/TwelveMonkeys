/*
 * Copyright (c) 2011, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  Neither the name "TwelveMonkeys" nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTestCase;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JPEGImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReaderTest.java,v 1.0 24.01.11 22.04 haraldk Exp$
 */
public class JPEGImageReaderTest extends ImageReaderAbstractTestCase<JPEGImageReader> {

    private static final JPEGImageReaderSpi SPI = new JPEGImageReaderSpi(lookupDelegateProvider());

    private static ImageReaderSpi lookupDelegateProvider() {
        return JPEGImageReaderSpi.lookupDelegateProvider(IIORegistry.getDefaultInstance());
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/jpeg/cmm-exception-adobe-rgb.jpg"), new Dimension(626, 76)),
                new TestData(getClassLoaderResource("/jpeg/cmm-exception-srgb.jpg"), new Dimension(1800, 1200)),
                new TestData(getClassLoaderResource("/jpeg/gray-sample.jpg"), new Dimension(386, 396)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample.jpg"), new Dimension(160, 227)),
                new TestData(getClassLoaderResource("/jpeg/cmyk-sample-multiple-chunk-icc.jpg"), new Dimension(2707, 3804))
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return SPI;
    }

    @Override
    protected JPEGImageReader createReader() {
        try {
            return (JPEGImageReader) SPI.createReaderInstance();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    protected Class<JPEGImageReader> getReaderClass() {
        return JPEGImageReader.class;
    }

    @Override
    protected boolean allowsNullRawImageType() {
        return true;
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("JPEG", "jpeg", "JPG", "jpg");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("jpeg", "jpg");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList("image/jpeg");
    }

    /*
    @Ignore("TODO: This method currently fails, fix it")
    @Override
    public void testSetDestinationType() throws IOException {
        // TODO: This method currently fails, fix it
        super.testSetDestinationType();
    }
*/
    // TODO: Test that subsampling is actually reading something

    @Test
    public void testICCProfileClassOutputColors() throws IOException {
        // Make sure ICC profile with class output isn't converted to too bright values
        JPEGImageReader reader = createReader();
        reader.setInput(ImageIO.createImageInputStream(getClassLoaderResource("/jpeg/cmyk-sample-custom-icc-bright.jpg")));

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(new Rectangle(800, 800, 64, 8));
        param.setSourceSubsampling(8, 8, 1, 1);

        BufferedImage image = reader.read(0, param);
        assertNotNull(image);

        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        byte[] expectedData = {34, 37, 34, 47, 47, 44, 22, 26, 28, 23, 26, 28, 20, 23, 26, 20, 22, 25, 22, 25, 27, 18, 21, 24};

        assertEquals(expectedData.length, data.length);
        
        for (int i = 0; i < expectedData.length; i++) {
            assertEquals(expectedData[i], data[i], 5);
        }
    }
}
