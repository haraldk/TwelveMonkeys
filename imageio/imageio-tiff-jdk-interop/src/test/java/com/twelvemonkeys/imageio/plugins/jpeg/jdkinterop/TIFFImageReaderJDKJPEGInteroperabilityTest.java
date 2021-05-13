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

package com.twelvemonkeys.imageio.plugins.jpeg.jdkinterop;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReader;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;
import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Tests our TIFFImageReader delegating to the JDK JPEGImageReader.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReaderJDKJPEGInteroperabilityTest.java,v 1.0 08.05.12 15:25 haraldk Exp$
 */
public class TIFFImageReaderJDKJPEGInteroperabilityTest extends ImageReaderAbstractTest<TIFFImageReader> {
    private static final String JDK_JPEG_PROVIDER_CLASS_NAME = "com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi";

    @Override
    protected ImageReaderSpi createProvider() {
        return new TIFFImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/tiff/foto_0001.tif"), new Dimension(1663, 2338)), // Little endian, Old JPEG
                new TestData(getClassLoaderResource("/tiff/quad-jpeg.tif"), new Dimension(512, 384)), // YCbCr, JPEG compressed, striped
                new TestData(getClassLoaderResource("/tiff/cmyk_jpeg.tif"), new Dimension(100, 100)) // CMYK, JPEG compressed, with ICC profile
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

    @Ignore("Fails in TIFFImageReader")
    @Override
    public void testSetDestinationIllegal() {
    }

    @Test
    public void testReallyUsingJDKJPEGImageReader() {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");

        if (readers.hasNext()) {
            ImageReader reader = readers.next();

            if (JDK_JPEG_PROVIDER_CLASS_NAME.equals(reader.getOriginatingProvider().getClass().getName())) {
                return;
            }
        }

        fail("Expected Spi not in use (dependency issue?): " + JDK_JPEG_PROVIDER_CLASS_NAME);
    }
}
