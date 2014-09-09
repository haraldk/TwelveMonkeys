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

package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.IIOException;
import java.io.IOException;
import java.util.Arrays;

/**
 * PSDLayerInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDLayerInfo.java,v 1.0 Apr 29, 2008 6:01:12 PM haraldk Exp$
 */
final class PSDLayerInfo {
    final int top;
    final int left;
    final int bottom;
    final int right;

    final PSDChannelInfo[] channelInfo;
    final PSDLayerBlendMode blendMode;
    final PSDLayerMaskData layerMaskData;
    final PSDChannelSourceDestinationRange[] ranges;
    final String layerName;

    PSDLayerInfo(final boolean largeFormat, final ImageInputStream pInput) throws IOException {
        top = pInput.readInt();
        left = pInput.readInt();
        bottom = pInput.readInt();
        right = pInput.readInt();

        int channels = pInput.readUnsignedShort();
        
        channelInfo = new PSDChannelInfo[channels];
        for (int i = 0; i < channels; i++) {
            short channelId = pInput.readShort();
            long length = largeFormat ? pInput.readLong() : pInput.readUnsignedInt();

            channelInfo[i] = new PSDChannelInfo(channelId, length);
        }

        blendMode = new PSDLayerBlendMode(pInput);

        // Lenght of layer mask data
        long extraDataSize = pInput.readUnsignedInt();
        // TODO: Allow skipping the rest here?
        // pInput.skipBytes(extraDataSize);

        // Layer mask/adjustment layer data
        int layerMaskDataSize = pInput.readInt(); // May be 0, 20 or 36 bytes...
        if (layerMaskDataSize != 0) {
            layerMaskData = new PSDLayerMaskData(pInput, layerMaskDataSize);
        }
        else {
            layerMaskData = null;
        }

        int layerBlendingDataSize = pInput.readInt();
        if (layerBlendingDataSize % 8 != 0) {
            throw new IIOException("Illegal PSD Layer Blending Data size: " + layerBlendingDataSize + ", expected multiple of 8");
        }

        ranges = new PSDChannelSourceDestinationRange[layerBlendingDataSize / 8];
        for (int i = 0; i < ranges.length; i++) {
            ranges[i] = new PSDChannelSourceDestinationRange(pInput, (i == 0 ? "Gray" : "Channel " + (i - 1)));
        }


        layerName = PSDUtil.readPascalString(pInput);

        int layerNameSize = layerName.length() + 1;

        // Skip pad bytes for long word alignment
        if (layerNameSize % 4 != 0) {
            int skip = layerNameSize % 4;
            pInput.skipBytes(skip);
            layerNameSize += skip;
        }

        // TODO: There's some data skipped here...
        // Adjustment layer info etc...
        pInput.skipBytes(extraDataSize - layerMaskDataSize - 4 - layerBlendingDataSize - 4 - layerNameSize);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append("top: ").append(top);
        builder.append(", left: ").append(left);
        builder.append(", bottom: ").append(bottom);
        builder.append(", right: ").append(right);

        builder.append(", channels: ").append(Arrays.toString(channelInfo));
        builder.append(", blend mode: ").append(blendMode);
        if (layerMaskData != null) {
            builder.append(", layer mask data: ").append(layerMaskData);
        }
        builder.append(", ranges: ").append(Arrays.toString(ranges));
        builder.append(", layer name: \"").append(layerName).append("\"");

        builder.append("]");
        return builder.toString();
    }
}
