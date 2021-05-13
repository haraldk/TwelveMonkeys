/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class PercentAreaOfInterest extends DefaultAreaOfInterest {

    public PercentAreaOfInterest(Dimension pOriginalDimension) {
        super(pOriginalDimension);
    }

    public PercentAreaOfInterest(int pOriginalWidth, int pOriginalHeight) {
        super(pOriginalWidth, pOriginalHeight);
    }

    public Dimension getCrop(Dimension pOriginalDimension, final Rectangle pCrop) {
        int cropWidth = pCrop.width;
        int cropHeight = pCrop.height;
        float ratio;

        if (cropWidth >= 0 && cropHeight >= 0) {
            // Non-uniform
            cropWidth = Math.round((float) pOriginalDimension.width * (float) pCrop.width / 100f);
            cropHeight = Math.round((float) pOriginalDimension.height * (float) pCrop.height / 100f);
        }
        else if (cropWidth >= 0) {
            // Find ratio from pWidth
            ratio = (float) cropWidth / 100f;
            cropWidth = Math.round((float) pOriginalDimension.width * ratio);
            cropHeight = Math.round((float) pOriginalDimension.height * ratio);

        }
        else if (cropHeight >= 0) {
            // Find ratio from pHeight
            ratio = (float) cropHeight / 100f;
            cropWidth = Math.round((float) pOriginalDimension.width * ratio);
            cropHeight = Math.round((float) pOriginalDimension.height * ratio);
        }
        // Else: No crop

        return new Dimension(cropWidth, cropHeight);
    }

}
