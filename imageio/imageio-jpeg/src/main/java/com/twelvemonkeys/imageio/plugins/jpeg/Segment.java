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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.lang.Validate;

import java.io.DataInput;
import java.io.IOException;

/**
 * Segment.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: Segment.java,v 1.0 22/08/16 harald.kuhr Exp$
 */
abstract class Segment {
    final int marker;

    protected Segment(final int marker) {
        this.marker = Validate.isTrue(marker >> 8 == 0xFF, marker, "Unknown JPEG marker: 0x%04x");
    }

    static Segment read(int marker, String identifier, int length, DataInput data) throws IOException {
        switch (marker) {
            case JPEG.DHT:
                return HuffmanTable.read(data, length);
            case JPEG.DQT:
                return QuantizationTable.read(data, length);
            case JPEG.SOF0:
            case JPEG.SOF1:
            case JPEG.SOF2:
            case JPEG.SOF3:
            case JPEG.SOF5:
            case JPEG.SOF6:
            case JPEG.SOF7:
            case JPEG.SOF9:
            case JPEG.SOF10:
            case JPEG.SOF11:
            case JPEG.SOF13:
            case JPEG.SOF14:
            case JPEG.SOF15:
                return Frame.read(marker, data, length);
            case JPEG.SOS:
                return Scan.read(data, length);
            case JPEG.COM:
                return Comment.read(data, length);
            // TODO: JPEG.DAC
            case JPEG.DRI:
                return RestartInterval.read(data, length);
            case JPEG.APP0:
            case JPEG.APP1:
            case JPEG.APP2:
            case JPEG.APP3:
            case JPEG.APP4:
            case JPEG.APP5:
            case JPEG.APP6:
            case JPEG.APP7:
            case JPEG.APP8:
            case JPEG.APP9:
            case JPEG.APP10:
            case JPEG.APP11:
            case JPEG.APP12:
            case JPEG.APP13:
            case JPEG.APP14:
            case JPEG.APP15:
                return Application.read(marker, identifier, data, length);
            default:
                return Unknown.read(marker, length, data);
        }
    }
}
