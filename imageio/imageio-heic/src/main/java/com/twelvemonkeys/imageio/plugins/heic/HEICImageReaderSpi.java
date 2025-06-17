/*
 * Copyright (c) 2025, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.heic;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;
import openize.heic.decoder.HeicImage;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * HEICImageReaderSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: HEICImageReaderSpi.java,v 1.0 25.10.11 18:41 haraldk Exp$
 */
public final class HEICImageReaderSpi extends ImageReaderSpiBase {
    public HEICImageReaderSpi() {
        super(new HEICProviderInfo());
    }

    @Override
    public boolean canDecodeInput(Object source) throws IOException {
        return source instanceof ImageInputStream && canDecode((ImageInputStream) source);
    }

    private static boolean canDecode(ImageInputStream input) throws IOException {
        input.mark();

        try {
            // NOTE: Assumes the input to be ISO BMFF, only tests for "heic", other formats may throw exception
            return HeicImage.canLoad(new ImageInputStreamIOStreamAdapter(input));
        }
        catch (openize.io.IOException rtioe) {
            throw new IOException(rtioe.getMessage(), rtioe);
        }
        catch (Exception e) {
            return false;
        }
        finally {
            input.reset();
        }
    }

    @Override
    public ImageReader createReaderInstance(Object extension) throws IOException {
        return new HEICImageReader(this);
    }

    @Override
    public String getDescription(Locale locale) {
        return "ISO/IEC 23008-12:2017 HEIF (HEIC) format Reader";
    }

}
