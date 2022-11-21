/*
 * Copyright (c) 2014, Harald Kuhr
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

import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.IIOImage;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

final class PAMHeaderWriter extends HeaderWriter {
    public PAMHeaderWriter(final ImageOutputStream imageOutput) {
        super(imageOutput);
    }

    @Override
    public void writeHeader(final IIOImage image, final ImageWriterSpi provider) throws IOException {
        TupleType tupleType = tupleType(image);

        // Write PAM magic
        imageOutput.writeShort(PNM.PAM);
        imageOutput.write('\n');
        // Comments
        writeComments(image.getMetadata(), provider);
        // Write width/height and number of channels
        imageOutput.write(String.format("WIDTH %s\nHEIGHT %s\n", getWidth(image), getHeight(image)).getBytes(UTF_8));
        imageOutput.write(String.format("DEPTH %s\n", getNumBands(image)).getBytes(UTF_8));
        imageOutput.write(String.format("MAXVAL %s\n", getMaxVal(image)).getBytes(UTF_8));
        imageOutput.write(String.format("TUPLTYPE %s\nENDHDR\n", tupleType).getBytes(UTF_8));
    }

    private static TupleType tupleType(IIOImage image) {
        TupleType tupleType = image.hasRaster()
                ? TupleType.forPAM(image.getRaster())
                : TupleType.forPAM(ImageTypeSpecifiers.createFromRenderedImage(image.getRenderedImage()));

        if (tupleType == null) {
            throw new IllegalArgumentException("Unknown TupleType for " + (image.hasRaster() ? image.getRaster() : image.getRenderedImage()));
        }

        return tupleType;
    }
}
