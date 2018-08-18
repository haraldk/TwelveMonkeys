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

package com.twelvemonkeys.imageio.util;

import javax.imageio.ImageTypeSpecifier;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * IndexedImageTypeSpecifier
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IndexedImageTypeSpecifier.java,v 1.0 May 19, 2008 11:04:28 AM haraldk Exp$
 */
final class IndexedImageTypeSpecifier  {
    private IndexedImageTypeSpecifier() {}

    static ImageTypeSpecifier createFromIndexColorModel(final IndexColorModel pColorModel) {
        // For some reason, we need a sample model
        return new ImageTypeSpecifier(notNull(pColorModel, "colorModel"), pColorModel.createCompatibleSampleModel(1, 1)) {

            @Override
            public final BufferedImage createBufferedImage(final int pWidth, final int pHeight) {
                try {
                    // This is a fix for the super-method, that first creates a sample model, and then
                    // creates a raster from it, using Raster.createWritableRaster. The problem with
                    // that approach, is that it always creates a TYPE_CUSTOM BufferedImage for indexed images.
                    WritableRaster raster = colorModel.createCompatibleWritableRaster(pWidth, pHeight);
                    return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), new Hashtable());
                }
                catch (NegativeArraySizeException e) {
                    // Exception most likely thrown from a DataBuffer constructor
                    throw new IllegalArgumentException("Array size > Integer.MAX_VALUE!");
                }
            }
        };
    }
}
