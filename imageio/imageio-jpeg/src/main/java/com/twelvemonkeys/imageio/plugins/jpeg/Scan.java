/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

final class Scan extends Segment {
    final int spectralSelStart; // Start of spectral or predictor selection
    final int spectralSelEnd; // End of spectral selection
    final int approxHigh;
    final int approxLow;

    final Component[] components;

    Scan(final Component[] components, final int spectralStart, final int spectralSelEnd, final int approxHigh, final int approxLow) {
        super(JPEG.SOS);

        this.components = components;
        this.spectralSelStart = spectralStart;
        this.spectralSelEnd = spectralSelEnd;
        this.approxHigh = approxHigh;
        this.approxLow = approxLow;
    }

    @Override
    public String toString() {
        return String.format(
                "SOS[spectralSelStart: %d, spectralSelEnd: %d, approxHigh: %d, approxLow: %d, components: %s]",
                spectralSelStart, spectralSelEnd, approxHigh, approxLow, Arrays.toString(components)
        );
    }

    public static Scan read(final ImageInputStream data) throws IOException {
        int length = data.readUnsignedShort();

        return read(new SubImageInputStream(data, length), length);
    }

    public static Scan read(final DataInput data, final int length) throws IOException {
        int numComp = data.readUnsignedByte();

        int expected = 6 + numComp * 2;
        if (expected != length) {
            throw new IIOException(String.format("Unexpected SOS length: %d != %d", length, expected));
        }

        Component[] components = new Component[numComp];

        for (int i = 0; i < numComp; i++) {
            int scanCompSel = data.readUnsignedByte();
            final int temp = data.readUnsignedByte();

            components[i] = new Component(scanCompSel, temp & 0x0F, temp >> 4);
        }

        int selection = data.readUnsignedByte();
        int spectralEnd = data.readUnsignedByte();
        int temp = data.readUnsignedByte();

        return new Scan(components, selection, spectralEnd, temp >> 4, temp & 0x0F);
    }

    public final static class Component {
        final int scanCompSel; // Scan component selector
        final int acTabSel; // AC table selector
        final int dcTabSel; // DC table selector

        Component(final int scanCompSel, final int acTabSel, final int dcTabSel) {
            this.scanCompSel = scanCompSel;
            this.acTabSel = acTabSel;
            this.dcTabSel = dcTabSel;
        }

        @Override
        public String toString() {
            return String.format("scanCompSel: %d, acTabSel: %d, dcTabSel: %d", scanCompSel, acTabSel, dcTabSel);
        }
    }
}
