/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.image;

import java.awt.*;

/**
 * GraphicsUtil
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/GraphicsUtil.java#1 $
 */
public final class GraphicsUtil {

    /**
     * Enables anti-aliasing in the {@code Graphics} object.
     * <p>
     * Anti-aliasing is enabled by casting to {@code Graphics2D} and setting
     * the rendering hint {@code RenderingHints.KEY_ANTIALIASING} to
     * {@code RenderingHints.VALUE_ANTIALIAS_ON}.
     * </p>
     *
     * @param pGraphics the graphics object
     * @throws ClassCastException if {@code pGraphics} is not an instance of
     *         {@code Graphics2D}.
     *
     * @see java.awt.RenderingHints#KEY_ANTIALIASING
     */
    public static void enableAA(final Graphics pGraphics) {
        ((Graphics2D) pGraphics).setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );
    }

    /**
     * Sets the alpha in the {@code Graphics} object.
     * <p>
     * Alpha is set by casting to {@code Graphics2D} and setting the composite
     * to the rule {@code AlphaComposite.SRC_OVER} multiplied by the given
     * alpha.
     * </p>
     *
     * @param pGraphics the graphics object
     * @param pAlpha the alpha level, {@code alpha} must be a floating point
     *        number in the inclusive range [0.0,&nbsp;1.0].
     * @throws ClassCastException if {@code pGraphics} is not an instance of
     *         {@code Graphics2D}.
     *
     * @see java.awt.AlphaComposite#SRC_OVER
     * @see java.awt.AlphaComposite#getInstance(int, float)
     */
    public static void setAlpha(final Graphics pGraphics, final float pAlpha) {
        ((Graphics2D) pGraphics).setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pAlpha)
        );
    }
}
