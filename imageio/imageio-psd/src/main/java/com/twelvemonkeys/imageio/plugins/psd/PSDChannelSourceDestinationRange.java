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
 * PSDChannelSourceDestinationRange
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDChannelSourceDestinationRange.java,v 1.0 May 6, 2008 5:14:13 PM haraldk Exp$
 */
final class PSDChannelSourceDestinationRange {
    private String channel;
    private short sourceBlack;
    private short sourceWhite;
    private short destBlack;
    private short destWhite;

    public PSDChannelSourceDestinationRange(final ImageInputStream pInput, final String pChannel) throws IOException {
        channel = pChannel;
        sourceBlack = pInput.readShort();
        sourceWhite = pInput.readShort();
        destBlack = pInput.readShort();
        destWhite = pInput.readShort();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());

        builder.append("[(").append(channel);
        builder.append("): sourceBlack: ").append(Integer.toHexString(sourceBlack & 0xffff));
        builder.append(", sourceWhite: ").append(Integer.toHexString(sourceWhite & 0xffff));
        builder.append(", destBlack: ").append(Integer.toHexString(destBlack & 0xffff));
        builder.append(", destWhite: ").append(Integer.toHexString(destWhite & 0xffff));
        builder.append("]");

        return builder.toString();
    }
}
