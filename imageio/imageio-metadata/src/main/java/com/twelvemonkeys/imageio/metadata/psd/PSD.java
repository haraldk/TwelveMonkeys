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

package com.twelvemonkeys.imageio.metadata.psd;

/**
 * PSD
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSD.java,v 1.0 24.01.12 16:51 haraldk Exp$
 */
public interface PSD {
    /** PSD 2+ Native format (.PSD) identifier "8BPS" */
    int SIGNATURE_8BPS = ('8' << 24) + ('B' << 16) + ('P' << 8) + 'S';

    /** PSD image resource marker "8BIM". */
    int RESOURCE_TYPE = ('8' << 24) + ('B' << 16) + ('I' << 8) + 'M';

    // http://fileformats.archiveteam.org/wiki/Photoshop_Image_Resources
    int RESOURCE_TYPE_IMAGEREADY = ('M' << 24) + ('e' << 16) + ('S' << 8) + 'a';
    int RESOURCE_TYPE_PHOTODELUXE = ('P' << 24) + ('H' << 16) + ('U' << 8) + 'T';
    int RESOURCE_TYPE_LIGHTROOM = ('A' << 24) + ('g' << 16) + ('H' << 8) + 'g';
    int RESOURCE_TYPE_DCSR = ('D' << 24) + ('C' << 16) + ('S' << 8) + 'R';

    /** IPTC image resource id. */
    int RES_IPTC_NAA = 0x0404;

    /** ICC profile image resource id. */
    int RES_ICC_PROFILE = 0x040f;

    /** PSD Path resource id. */
    int RES_CLIPPING_PATH = 0x07d0;
}
