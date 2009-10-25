/*
 * Copyright (c) 2008, Harald Kuhr
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
class PSDLayerInfo {
    final int mTop;
    final int mLeft;
    final int mBottom;
    final int mRight;

    final PSDChannelInfo[] mChannelInfo;
    final PSDLayerBlendMode mBlendMode;
    final PSDLayerMaskData mLayerMaskData;
    final PSDChannelSourceDestinationRange[] mRanges;
    final String mLayerName;

    PSDLayerInfo(ImageInputStream pInput) throws IOException {
        mTop = pInput.readInt();
        mLeft = pInput.readInt();
        mBottom = pInput.readInt();
        mRight = pInput.readInt();

        int channels = pInput.readUnsignedShort();
        
        mChannelInfo = new PSDChannelInfo[channels];
        for (int i = 0; i < channels; i++) {
            short channelId = pInput.readShort();
            long length = pInput.readUnsignedInt();

            mChannelInfo[i] = new PSDChannelInfo(channelId, length);
        }

        mBlendMode = new PSDLayerBlendMode(pInput);

        // Lenght of layer mask data
        long extraDataSize = pInput.readUnsignedInt();
        // TODO: Allow skipping the rest here?
        // pInput.skipBytes(extraDataSize);

        // Layer mask/adjustment layer data
        int layerMaskDataSize = pInput.readInt(); // May be 0, 20 or 36 bytes...
        if (layerMaskDataSize != 0) {
            mLayerMaskData = new PSDLayerMaskData(pInput, layerMaskDataSize);
        }
        else {
            mLayerMaskData = null;
        }

        int layerBlendingDataSize = pInput.readInt();
        if (layerBlendingDataSize % 8 != 0) {
            throw new IIOException("Illegal PSD Layer Blending Data size: " + layerBlendingDataSize + ", expected multiple of 8");
        }

        mRanges = new PSDChannelSourceDestinationRange[layerBlendingDataSize / 8];
        for (int i = 0; i < mRanges.length; i++) {
            mRanges[i] = new PSDChannelSourceDestinationRange(pInput, (i == 0 ? "Gray" : "Channel " + (i - 1)));
        }


        mLayerName = PSDUtil.readPascalString(pInput);

        int layerNameSize = mLayerName.length() + 1;
        // readPascalString has already read pad byte for word alignment
        if (layerNameSize % 2 != 0) {
            layerNameSize++;
        }
        // Skip two more pad bytes if needed
        if (layerNameSize % 4 != 0) {
            pInput.skipBytes(2);
            layerNameSize += 2;
        }

        // TODO: There's some data skipped here...
        // Adjustment layer info etc...
        pInput.skipBytes(extraDataSize - layerMaskDataSize - 4 - layerBlendingDataSize - 4 - layerNameSize);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append("top: ").append(mTop);
        builder.append(", left: ").append(mLeft);
        builder.append(", bottom: ").append(mBottom);
        builder.append(", right: ").append(mRight);

        builder.append(", channels: ").append(Arrays.toString(mChannelInfo));
        builder.append(", blend mode: ").append(mBlendMode);
        if (mLayerMaskData != null) {
            builder.append(", layer mask data: ").append(mLayerMaskData);
        }
        builder.append(", ranges: ").append(Arrays.toString(mRanges));
        builder.append(", layer name: \"").append(mLayerName).append("\"");

        builder.append("]");
        return builder.toString();
    }
}
