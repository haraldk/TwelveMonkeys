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

package com.twelvemonkeys.imageio.color;

import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;

/**
 * ComponentColorModel subclass that correctly handles full 16 bit {@code TYPE_SHORT} signed integral samples.
 **
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: UInt32ColorModel.java,v 1.0 24.01.11 17.51 haraldk Exp$
 */
public final class Int16ComponentColorModel extends ComponentColorModel {
    private final ComponentColorModel delegate;

    public Int16ComponentColorModel(final ColorSpace cs, final boolean hasAlpha, boolean isAlphaPremultiplied) {
        super(cs, hasAlpha, isAlphaPremultiplied, hasAlpha ? TRANSLUCENT : OPAQUE, DataBuffer.TYPE_SHORT);

        delegate = new ComponentColorModel(cs, hasAlpha, isAlphaPremultiplied, hasAlpha ? TRANSLUCENT : OPAQUE, DataBuffer.TYPE_USHORT);
    }

    private void remap(final short[] s, final int i) {
        // MIN ... -1 -> 0 ... MAX
        // 0 ... MAX  -> MIN ... -1
        short sample = s[i];

        if (sample < 0) {
            s[i] = (short) (sample - Short.MIN_VALUE);
        }
        else {
            s[i] = (short) (sample + Short.MIN_VALUE);
        }
    }

    @Override
    public int getRed(final Object inData) {
        remap((short[]) inData, 0);

        return delegate.getRed(inData);
    }

    @Override
    public int getGreen(final Object inData) {
        remap((short[]) inData, 1);

        return delegate.getGreen(inData);
    }

    @Override
    public int getBlue(final Object inData) {
        remap((short[]) inData, 2);

        return delegate.getBlue(inData);
    }
}
