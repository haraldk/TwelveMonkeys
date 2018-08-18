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
 * ComponentColorModel subclass that correctly handles full 32 bit {@code TYPE_INT} unsigned integral samples.
 *
 * @see <a href="https://bugs.openjdk.java.net/browse/JDK-6193686">
 *     ComponentColorModel.getNormalizedComponents() does not handle 32-bit TYPE_INT</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: UInt32ColorModel.java,v 1.0 24.01.11 17.51 haraldk Exp$
 */
public final class UInt32ColorModel extends ComponentColorModel {
    public UInt32ColorModel(final ColorSpace cs, final boolean hasAlpha, boolean isAlphaPremultiplied) {
        super(cs, hasAlpha, isAlphaPremultiplied, hasAlpha ? TRANSLUCENT : OPAQUE, DataBuffer.TYPE_INT);
    }

    @Override
    public float[] getNormalizedComponents(final Object pixel, float[] normComponents, final int normOffset) {
        // Implementation borrowed from super class, with modifications to allow 32 bit shifts and unsigned values.
        int numComponents = getNumComponents();

        if (normComponents == null) {
            normComponents = new float[numComponents + normOffset];
        }

        // This class only supports DataBuffer.TYPE_INT, cast is safe
        int[] ipixel = (int[]) pixel;
        for (int c = 0, nc = normOffset; c < numComponents; c++, nc++) {
            normComponents[nc] = ((float) (ipixel[c] & 0xffffffffl)) / ((float) ((1l << getComponentSize(c)) - 1));
        }

        int numColorComponents = getNumColorComponents();

        if (hasAlpha() && isAlphaPremultiplied()) {
            float alpha = normComponents[numColorComponents + normOffset];

            if (alpha != 0.0f) {
                float invAlpha = 1.0f / alpha;

                for (int c = normOffset; c < numColorComponents + normOffset; c++) {
                    normComponents[c] *= invAlpha;
                }
            }
        }

        // TODO: We don't currently support color spaces that has min and max other than 0.0f and 1.0f respectively.

        return normComponents;
    }
}
