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

package com.twelvemonkeys.imageio.plugins.jpeg.jep262interop;

import com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageReaderSpi;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Tests the JEP 262 TIFFImageReader delegating to our JPEGImageReader.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReaderTest.java,v 1.0 08.05.12 15:25 haraldk Exp$
 */
public class JEP262TIFFImageReaderInteroperabilityTest extends ImageReaderAbstractTest<ImageReader> {
    private static final String JEP_262_PROVIDER_CLASS_NAME = "com.sun.imageio.plugins.tiff.TIFFImageReaderSpi";

    @Override
    protected ImageReaderSpi createProvider() {
        Iterator<ImageReaderSpi> providers = IIORegistry.getDefaultInstance().getServiceProviders(ImageReaderSpi.class, new ServiceRegistry.Filter() {
            @Override
            public boolean filter(final Object provider) {
                return JEP_262_PROVIDER_CLASS_NAME.equals(provider.getClass().getName()) && ((ImageReaderSpi) provider).getVendorName().startsWith("Oracle");
            }
        }, true);

        if (providers.hasNext()) {
            return providers.next();
        }

        // Skip tests if we have no Spi (ie. pre JDK 9)
        assumeTrue(providers.hasNext(), "Provider " + JEP_262_PROVIDER_CLASS_NAME + " not found");
        return providers.next();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/tiff/foto_0001.tif"), new Dimension(1663, 2338), new Dimension(1663, 2337)), // Little endian, Old JPEG
                new TestData(getClassLoaderResource("/tiff/cmyk_jpeg.tif"), new Dimension(100, 100)), // CMYK, JPEG compressed, with ICC profile
                new TestData(getClassLoaderResource("/tiff/jpeg-lossless-8bit-gray.tif"), new Dimension(512, 512))  // Lossless JPEG Gray, 8 bit/sample
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.emptyList();
    }

    @Disabled("Fails in TIFFImageReader")
    @Override
    public void testSetDestinationIllegal() {
    }

    @Test
    public void testReallyUsingOurJPEGImageReader() {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");

        if (readers.hasNext()) {
            ImageReader reader = readers.next();

            if ((reader.getOriginatingProvider() instanceof JPEGImageReaderSpi)) {
                return;
            }
        }

        fail("Expected Spi not registered (dependency issue?): " + JPEGImageReaderSpi.class);
    }
}
