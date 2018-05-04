/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
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

package com.twelvemonkeys.imageio.plugins.cr2;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * CR2ImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CR2ImageReaderTest.java,v 1.0 07.04.14 21:52 haraldk Exp$
 */
@Ignore
public class CR2ImageReaderTest extends ImageReaderAbstractTest {
    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(
                        getClassLoaderResource("/cr2/IMG_3483.CR2"), // Canon EOS 400D
                        new Dimension(3888, 2592) // This is what the TIFF structure says...
                        /* from http://lclevy.free.fr/dng/:
                        new Dimension(3888 / 4, 2592 / 4), // The image is supposed to be 1/4 of the size in the TIFF tags...
                        new Dimension(-1, -1), // "small version", no size information, only JPEG data (EXIF thumbnail)
                        new Dimension(384, 256), // according to http://lclevy.free.fr/dng/ this is not compressed, but TIFF structure says JPEG... Perhaps JPEG lossless?
                        new Dimension(3888, 2592) // Full size image, no size information
                        */
                )
                // TODO: EOS 7D sample
        );
    }

    @Override
    protected ImageReaderSpi createProvider() {
        return new CR2ImageReaderSpi();
    }

    @Override
    protected ImageReader createReader() {
        return new CR2ImageReader(createProvider());
    }

    @Override
    protected Class getReaderClass() {
        return CR2ImageReader.class;
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("cr2");
    }

    @Override
    protected List<String> getSuffixes() {
        return Arrays.asList("cr2");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList("image/x-canon-raw");
    }

    @Test
    @Ignore("Known issue: Subsampled reading not supported")
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        super.testReadWithSubsampleParamPixels();
    }

    @Test
    @Ignore("Known issue: Source region reading not supported")
    @Override
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        super.testReadWithSourceRegionParamEqualImage();
    }
}
