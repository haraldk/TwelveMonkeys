/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormatImpl;

/**
 * TIFFMedataFormat.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFMedataFormat.java,v 1.0 17/04/15 harald.kuhr Exp$
 * @deprecated Use {@link TIFFImageMetadataFormat} instead.
 */
@Deprecated
public final class TIFFMedataFormat extends IIOMetadataFormatImpl {
    // TODO: Delete class in next version
    private static final TIFFMedataFormat INSTANCE = new TIFFMedataFormat();

    public static final String SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME = TIFFImageMetadataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;

    public TIFFMedataFormat() {
        super(SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME, CHILD_POLICY_SOME);
    }

    @Override
    public boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType) {
        return true;
    }

    public static TIFFMedataFormat getInstance() {
        return INSTANCE;
    }
}
