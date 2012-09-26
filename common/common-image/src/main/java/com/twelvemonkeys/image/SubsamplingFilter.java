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

package com.twelvemonkeys.image;

import java.awt.image.ReplicateScaleFilter;

/**
 * An {@code ImageFilter} class for subsampling images.
 * <p/>
 * It is meant to be used in conjunction with a {@code FilteredImageSource}
 * object to produce subsampled versions of existing images.
 * 
 * @see java.awt.image.FilteredImageSource
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/SubsamplingFilter.java#1 $
 */
public class SubsamplingFilter extends ReplicateScaleFilter {
    private int xSub;
    private int ySub;

    /**
     * Creates a {@code SubsamplingFilter}.
     *
     * @param pXSub
     * @param pYSub
     *
     * @throws IllegalArgumentException if {@code pXSub} or {@code pYSub} is
     * less than 1.
     */
    public SubsamplingFilter(int pXSub, int pYSub) {
        super(1, 1); // These are NOT REAL values, but we have to defer setting
                     // until w/h is available, in setDimensions below

        if (pXSub < 1 || pYSub < 1) {
            throw new IllegalArgumentException("Subsampling factors must be positive.");
        }

        xSub = pXSub;
        ySub = pYSub;
    }

    /** {@code ImageFilter} implementation, do not invoke. */
    public void setDimensions(int pWidth, int pHeight) {
        destWidth = (pWidth + xSub - 1) / xSub;
        destHeight = (pHeight + ySub - 1) / ySub;

        //System.out.println("Subsampling: " + xSub + "," + ySub + "-> " + destWidth + ", " + destHeight);
        super.setDimensions(pWidth, pHeight);
    }
}
