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

package com.twelvemonkeys.imageio.plugins.tiff;

/**
 * TIFFCustom
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFCustom.java,v 1.0 10.05.12 17:35 haraldk Exp$
 */
interface TIFFCustom {
    int COMPRESSION_NEXT = 32766;
    int COMPRESSION_CCITTRLEW = 32771;
    int COMPRESSION_THUNDERSCAN = 32809;
    int COMPRESSION_IT8CTPAD = 32895;
    int COMPRESSION_IT8LW = 32896;
    int COMPRESSION_IT8MP = 32897;
    int COMPRESSION_IT8BL = 32898;
    int COMPRESSION_PIXARFILM = 32908;
    int COMPRESSION_PIXARLOG = 32909;
    int COMPRESSION_DCS = 32947;
    int COMPRESSION_JBIG = 34661;
    int COMPRESSION_SGILOG = 34676;
    int COMPRESSION_SGILOG24 = 34677;
    int COMPRESSION_JPEG2000 = 34712;
    // TODO: Aperio SVS JPEG2000: 33003 (YCbCr) and 33005 (RGB), see http://openslide.org/formats/aperio/

    // PIXTIFF aka DELL PixTools, see https://community.emc.com/message/515755#515755
    /** PIXTIFF proprietary ZIP compression, identical to Deflate/ZLib. */
    int COMPRESSION_PIXTIFF_ZIP = 50013;

    int PHOTOMETRIC_LOGL = 32844;
    int PHOTOMETRIC_LOGLUV = 32845;

    /** DNG: CFA (Color Filter Array). */
    int PHOTOMETRIC_CFA = 32803;
    /** DNG: LinearRaw. */
    int PHOTOMETRIC_LINEAR_RAW = 34892;

    int SAMPLEFORMAT_COMPLEX_INT = 5;
    int SAMPLEFORMAT_COMPLEX_IEEE_FP = 6;
}
