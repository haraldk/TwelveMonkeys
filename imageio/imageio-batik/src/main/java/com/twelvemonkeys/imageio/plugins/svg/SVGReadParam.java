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

package com.twelvemonkeys.imageio.plugins.svg;

import javax.imageio.ImageReadParam;
import java.awt.*;

/**
 * Implementation of {@code IamgeReadParam} for SVG images.
 * SVG images allows for different source render sizes.
 *
 */
public class SVGReadParam extends ImageReadParam {
    private Paint background;
    private String baseURI;
    private boolean allowExternalResources;

    public SVGReadParam() {
        super();
        allowExternalResources = true;
    }

    public Paint getBackgroundColor() {
        return background;
    }

    public void setBackgroundColor(Paint pColor) {
        background = pColor;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String pBaseURI) {
        baseURI = pBaseURI;
    }

    public void allowExternalResources(boolean bAllow) {
        allowExternalResources = bAllow;
    }

    public boolean shouldAllowExternalResources() {
        return allowExternalResources;
    }

    @Override
    public boolean canSetSourceRenderSize() {
        return true;
    }
}
