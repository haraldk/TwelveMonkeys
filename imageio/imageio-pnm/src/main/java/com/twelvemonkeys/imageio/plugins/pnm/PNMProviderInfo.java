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

package com.twelvemonkeys.imageio.plugins.pnm;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

/**
 * PNMProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PNMProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
class PNMProviderInfo extends ReaderWriterProviderInfo {
    public PNMProviderInfo() {
        super(PNMProviderInfo.class,
                new String[] {
                        "pnm", "pbm", "pgm", "ppm","pfm",
                        "PNM", "PBM", "PGM", "PPM", "PFM"
                },
                new String[] {"pbm", "pgm", "ppm", "pfm"},
                new String[] {
                        // No official IANA record exists, these are conventional
                        "image/x-portable-pixmap",
                        "image/x-portable-anymap",
                },
                "com.twelvemonkeys.imageio.plugins.pnm.PNMImageReader",
                new String[] {
                        "com.twelvemonkeys.imageio.plugins.pnm.PNMImageReaderSpi",
                        "com.twelvemonkeys.imageio.plugins.pnm.PAMImageReaderSpi"
                },
                "com.twelvemonkeys.imageio.plugins.pnm.PNMImageWriter",
                new String[] {
                        "com.twelvemonkeys.imageio.plugins.pnm.PNMImageWriterSpi",
                        "com.twelvemonkeys.imageio.plugins.pnm.PAMImageWriterSpi"
                },
                true, // supports standard stream metadata
                null, null, // native stream format name and class
                null, null, // extra stream formats
                true, // supports standard image metadata
                null, null,
                null, null // extra image metadata formats
        );
    }
}
