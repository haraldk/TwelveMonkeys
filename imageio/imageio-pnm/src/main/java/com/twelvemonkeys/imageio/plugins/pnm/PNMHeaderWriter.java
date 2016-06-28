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

package com.twelvemonkeys.imageio.plugins.pnm;

import javax.imageio.IIOImage;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

final class PNMHeaderWriter extends HeaderWriter {
    public PNMHeaderWriter(final ImageOutputStream imageOutput) {
        super(imageOutput);
    }

    @Override public void writeHeader(final IIOImage image, final ImageWriterSpi provider) throws IOException {
        // Write P4/P5/P6 magic (Support only RAW formats for now; if we are to support PLAIN formats, pass parameter)
        // TODO: Determine PBM, PBM or PPM based on input color model and image data?
        short type = PNM.PPM;
        imageOutput.writeShort(type);
        imageOutput.write('\n');

        // Comments
        writeComments(image.getMetadata(), provider);

        // Dimensions (width/height)
        imageOutput.write(String.format("%s %s\n", getWidth(image), getHeight(image)).getBytes(HeaderWriter.UTF8));

        // MaxSample
        if (type != PNM.PBM) {
            imageOutput.write(String.format("%s\n", getMaxVal(image)).getBytes(HeaderWriter.UTF8));
        }
    }
}
