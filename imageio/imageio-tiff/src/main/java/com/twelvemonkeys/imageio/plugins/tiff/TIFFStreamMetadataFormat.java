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
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * TIFFStreamMetadataFormat.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFStreamMetadataFormat.java,v 1.0 17/04/15 harald.kuhr Exp$
 */
public final class TIFFStreamMetadataFormat extends IIOMetadataFormatImpl {
    private static final TIFFStreamMetadataFormat INSTANCE = new TIFFStreamMetadataFormat();

    // We'll reuse the metadata formats defined for JAI
    public static final String SUN_NATIVE_STREAM_METADATA_FORMAT_NAME = "com_sun_media_imageio_plugins_tiff_stream_1.0";

    private TIFFStreamMetadataFormat() {
        super(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, CHILD_POLICY_ALL);

        addElement("ByteOrder", SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, CHILD_POLICY_EMPTY);
        addAttribute("ByteOrder", "value", DATATYPE_STRING, true, ByteOrder.BIG_ENDIAN.toString(), Arrays.asList(ByteOrder.BIG_ENDIAN.toString(), ByteOrder.LITTLE_ENDIAN.toString()));
    }

    @Override
    public boolean canNodeAppear(final String elementName, final ImageTypeSpecifier imageType) {
        return true;
    }

    public static TIFFStreamMetadataFormat getInstance() {
        return INSTANCE;
    }
}
