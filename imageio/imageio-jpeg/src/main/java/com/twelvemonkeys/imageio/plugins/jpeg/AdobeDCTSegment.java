/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

/**
 * AdobeDCTSegment
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AdobeDCTSegment.java,v 1.0 23.04.12 16:55 haraldk Exp$
 */
class AdobeDCTSegment {
    public static final int Unknown = 0;
    public static final int YCC = 1;
    public static final int YCCK = 2;

    final int version;
    final int flags0;
    final int flags1;
    final int transform;

    AdobeDCTSegment(int version, int flags0, int flags1, int transform) {
        this.version = version; // 100 or 101
        this.flags0 = flags0;
        this.flags1 = flags1;
        this.transform = transform;
    }

    public int getVersion() {
        return version;
    }

    public int getFlags0() {
        return flags0;
    }

    public int getFlags1() {
        return flags1;
    }

    public int getTransform() {
        return transform;
    }

    @Override
    public String toString() {
        return String.format(
                "AdobeDCT[ver: %d.%02d, flags: %s %s, transform: %d]",
                getVersion() / 100, getVersion() % 100, Integer.toBinaryString(getFlags0()), Integer.toBinaryString(getFlags1()), getTransform()
        );
    }
}
