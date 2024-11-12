/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.svg;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SVGImageReaderSpiTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: SVGImageReaderSpiTest.java,v 1.0 08/08/16 harald.kuhr Exp$
 */
public class SVGImageReaderSpiTest {

    private static final String[] VALID_INPUTS = {
            "/svg/Android_robot.svg", // Minimal, no xml dec, no namespace
            "/svg/batikLogo.svg",     // xml dec, comments, namespace
            "/svg/blue-square.svg",   // xml dec, namespace
            "/svg/red-square.svg",
    };

    private static final String[] INVALID_INPUTS = {
        "<xml>",
        "<",
        "<?",
        "<?1",
        "<?12",
        "<?123", // #275 Infinite loop issue
        "<!--",
        "<!-- ", // #275 Infinite loop issue
        "<?123?>", // #275 Infinite loop issue
        "<svg",
    };

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    private final ImageReaderSpi provider = new SVGImageReaderSpi();

    @Test
    public void canDecodeInput() throws Exception {
        for (String validInput : VALID_INPUTS) {
            try (ImageInputStream input = ImageIO.createImageInputStream(getClass().getResource(validInput))) {
                assertTrue(provider.canDecodeInput(input), "Can't read valid input: " + validInput);
            }
        }
    }

    // Test will time out, if EOFs are not properly detected, see #275
    @Test
    public void canDecodeInputInvalid() throws Exception {
        assertTimeoutPreemptively(Duration.ofMillis(5000), () -> {
            for (String invalidInput : INVALID_INPUTS) {
                try (ImageInputStream input = new ByteArrayImageInputStream(invalidInput.getBytes(StandardCharsets.UTF_8))) {
                    assertFalse(provider.canDecodeInput(input), "Claims to read invalid input:" + invalidInput);
                }
            }
        });
    }
}