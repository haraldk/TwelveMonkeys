/*
 * Copyright (c) 2010, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.tiff;

/**
 * Unknown
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Unknown.java,v 1.0 Oct 8, 2010 3:38:45 PM haraldk Exp$
 * @see <a href="http://en.wikipedia.org/wiki/There_are_known_knowns">We also know there are known unknowns</a>
 */
final class Unknown {
    private final short type;
    private final int count;
    private final long pos;

    public Unknown(final short type, final int count, final long pos) {
        this.type = type;
        this.count = count;
        this.pos = pos;
    }

    @Override
    public int hashCode() {
        return (int) (pos ^ (pos >>> 32)) + count * 37 + type * 97;
    }

    @Override
    public boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            Unknown unknown = (Unknown) other;
            return pos == unknown.pos && type == unknown.type && count == unknown.count;
        }

        return false;
    }

    @Override
    public String toString() {
        if (count == 1) {
            return String.format("Unknown(%d)@%08x", type, pos);
        }
        else {
            return String.format("Unknown(%d)[%d]@%08x", type, count, pos);
        }
    }
}
