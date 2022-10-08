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

package com.twelvemonkeys.imageio.plugins.pntg;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Locale;

/**
 * PNTGImageReaderSpi.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PNTGImageReaderSpi.java,v 1.0 23/03/2021 haraldk Exp$
 */
public final class PNTGImageReaderSpi extends ImageReaderSpiBase {
    public PNTGImageReaderSpi() {
        super(new PNTGProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;
        stream.mark();

        try {
            // TODO: Figure out how to read the files without the MacBinary header...
            //   Probably not possible, as it's just 512 bytes of nulls OR pattern information
            return isMacBinaryPNTG(stream);
        }
        catch (EOFException ignore) {
            return false;
        }
        finally {
            stream.reset();
        }
    }

    static boolean isMacBinaryPNTG(final ImageInputStream stream) throws IOException {
        stream.seek(0);

        if (stream.readByte() != 0) {
            return false;
        }

        byte nameLen = stream.readByte();
        if (nameLen < 0 || nameLen > 63) {
            return false;
        }

        stream.skipBytes(63);

        // Validate that type is PNTG and that next 4 bytes are all within the ASCII range, typically 'MPNT'
        return stream.readInt() == ('P' << 24 | 'N' << 16 | 'T' << 8 | 'G') && (stream.readInt() & 0x80808080) == 0;
    }

    @Override
    public PNTGImageReader createReaderInstance(final Object extension) {
        return new PNTGImageReader(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "Apple MacPaint Painting (PNTG) image reader";
    }
}
