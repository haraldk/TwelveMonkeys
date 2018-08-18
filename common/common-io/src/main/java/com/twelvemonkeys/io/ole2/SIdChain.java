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

package com.twelvemonkeys.io.ole2;

import java.util.NoSuchElementException;

/**
 * SIdChain
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/ole2/SIdChain.java#1 $
 */
final class SIdChain {
    int[] chain;
    int size = 0;
    int next = 0;

    public SIdChain() {
        chain = new int[16];
    }

    void addSID(int pSID) {
        ensureCapacity();
        chain[size++] = pSID;
    }

    private void ensureCapacity() {
        if (chain.length == size) {
            int[] temp = new int[size << 1];
            System.arraycopy(chain, 0, temp, 0, size);
            chain = temp;
        }
    }

    public int[] getChain() {
        int[] result = new int[size];
        System.arraycopy(chain, 0, result, 0, size);
        return result;
    }

    public void reset() {
        next = 0;
    }

    public boolean hasNext() {
        return next < size;
    }

    public int next() {
        if (next >= size) {
            throw new NoSuchElementException("No element");
        }
        return chain[next++];
    }

    public int get(final int pIndex) {
        return chain[pIndex];
    }

    public int length() {
        return size;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(size * 5);
        buf.append('[');
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                buf.append(',');
            }
            buf.append(chain[i]);
        }
        buf.append(']');

        return buf.toString();
    }
}
