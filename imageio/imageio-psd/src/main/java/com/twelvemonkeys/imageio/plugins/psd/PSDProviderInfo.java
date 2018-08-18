/*
 * Copyright (c) 2015, Harald Kuhr
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

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * PSDProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PSDProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
final class PSDProviderInfo extends ReaderWriterProviderInfo {
    protected PSDProviderInfo() {
        super(
                PSDProviderInfo.class,
                new String[] {"psd", "PSD"},
                new String[] {"psd"},
                new String[] {
                        "image/vnd.adobe.photoshop",        // Official, IANA registered
                        "application/vnd.adobe.photoshop",  // Used in XMP
                        "image/x-psd",
                        "application/x-photoshop",
                        "image/x-photoshop"
                },
                "com.twelvemonkeys.imageio.plugins.psd.PSDImageReader",
                new String[] {"com.twelvemonkeys.imageio.plugins.psd.PSDImageReaderSpi"},
                null,
                null, // new String[] {"com.twelvemonkeys.imageio.plugins.psd.PSDImageWriterSpi"},
                false, null, null, null, null,
                true, PSDMetadata.NATIVE_METADATA_FORMAT_NAME, PSDMetadata.NATIVE_METADATA_FORMAT_CLASS_NAME, null, null
        );
    }
}
