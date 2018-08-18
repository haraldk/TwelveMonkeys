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

package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDPrintFlagsInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDPrintFlagsInfo.java,v 1.0 Jul 28, 2009 5:16:27 PM haraldk Exp$
 */
final class PSDPrintFlags extends PSDImageResource {
    boolean labels;
    boolean cropMasks;
    boolean colorBars;
    boolean registrationMarks;
    boolean negative;
    boolean flip;
    boolean interpolate;
    boolean caption;

    PSDPrintFlags(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        labels = pInput.readBoolean();
        cropMasks = pInput.readBoolean();
        colorBars = pInput.readBoolean();
        registrationMarks = pInput.readBoolean();
        negative = pInput.readBoolean();
        flip = pInput.readBoolean();
        interpolate = pInput.readBoolean();

        // Photoshop 2.5 and before has shorter struct
        if (size > 7) {
            caption = pInput.readBoolean();

            pInput.skipBytes(size - 8);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", labels: ").append(labels);
        builder.append(", crop masks: ").append(cropMasks);
        builder.append(", color bars: ").append(colorBars);
        builder.append(", registration marks: ").append(registrationMarks);
        builder.append(", negative: ").append(negative);
        builder.append(", flip: ").append(flip);
        builder.append(", interpolate: ").append(interpolate);
        builder.append(", caption: ").append(caption);

        builder.append("]");

        return builder.toString();
    }
}
