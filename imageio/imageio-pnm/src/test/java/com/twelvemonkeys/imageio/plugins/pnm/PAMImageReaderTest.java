/*
 * Copyright (c) 2020, Harald Kuhr
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

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import org.junit.Test;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PAMImageReaderTest extends ImageReaderAbstractTest<PNMImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new PAMImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/pam/snail2.pam"), new Dimension(256, 256)),   // P7 RGB
                new TestData(getClassLoaderResource("/pam/rgba.pam"), new Dimension(4, 2))          // P7 RGB_ALPHA
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList(
                "pam",
                "PAM"
        );
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList(
                "pam"
        );
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList(
                "image/x-portable-arbitrarymap"
        );
    }

    @Test
    public void testRGBAVsReference() throws IOException {
        ImageReader reader = createReader();
        TestData data = new TestData(getClassLoaderResource("/pam/rgba.pam"), new Dimension(4, 2));
        reader.setInput(data.getInputStream());

        BufferedImage expected = new BufferedImage(4, 2, BufferedImage.TYPE_4BYTE_ABGR);

        expected.setRGB(0, 0, new Color(0, 0, 255).getRGB());
        expected.setRGB(1, 0, new Color(0, 255, 0).getRGB());
        expected.setRGB(2, 0, new Color(255, 0, 0).getRGB());
        expected.setRGB(3, 0, new Color(255, 255, 255).getRGB());

        expected.setRGB(0, 1, new Color(0, 0, 255, 127).getRGB());
        expected.setRGB(1, 1, new Color(0, 255, 0, 127).getRGB());
        expected.setRGB(2, 1, new Color(255, 0, 0, 127).getRGB());
        expected.setRGB(3, 1, new Color(255, 255, 255, 127).getRGB());

        assertImageDataEquals("Images differ from reference", expected, reader.read(0));
    }

    @Test
    public void testXVThumbNotIncorrectlyRecognizedAsPAM() throws IOException {
        assertTrue("Should recognize PAM format", provider.canDecodeInput(new TestData(getClassLoaderResource("/pam/rgba.pam"), new Dimension()).getInputStream())); // Sanity
        assertFalse("Should distinguish xv-thumbs from PAM format", provider.canDecodeInput(new TestData(getClassLoaderResource("/xv-thumb/xv-thumb.xvt"), new Dimension()).getInputStream()));
    }
}
