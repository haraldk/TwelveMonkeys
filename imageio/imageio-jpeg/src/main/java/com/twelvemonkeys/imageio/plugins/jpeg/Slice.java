/*
 * Copyright (c) 2018, Oleg Ermolaev
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

import javax.imageio.IIOException;
import java.util.Arrays;

/**
 * For CR2 RAW image.
 *
 * @author Oleg Ermolaev Date: 05.05.2018 2:04
 */
public class Slice {
    public static final int FIRST_WIDTH_COUNT_INDEX = 0;
    public static final int FIRST_WIDTH_INDEX = 1;
    public static final int LAST_WIDTH_INDEX = 2;

    private final int firstWidthCount;
    private final int firstWidth;
    private final int lastWidth;

    public Slice(int firstWidthCount, int firstWidth, int lastWidth) {
        this.firstWidthCount = firstWidthCount;
        this.firstWidth = firstWidth;
        this.lastWidth = lastWidth;
    }

    public static Slice createSlice(long[] values) throws IIOException {
        if (values == null || values.length != 3) {
            throw new IIOException("Unexpected slices array: " + Arrays.toString(values));
        }
        final long firstWidthCount = values[FIRST_WIDTH_COUNT_INDEX];
        final long firstWidth = values[FIRST_WIDTH_INDEX];
        final long lastWidth = values[LAST_WIDTH_INDEX];
        if (!(0 < firstWidthCount && firstWidthCount <= Integer.MAX_VALUE) ||
                !(0 < firstWidth && firstWidth <= Integer.MAX_VALUE) ||
                !(0 < lastWidth && lastWidth <= Integer.MAX_VALUE) ||
                firstWidthCount * firstWidth + lastWidth > Integer.MAX_VALUE) {
            throw new IIOException("Unexpected slices array: " + Arrays.toString(values));
        }
        return new Slice((int) firstWidthCount, (int) firstWidth, (int) lastWidth);
    }

    public int getFirstWidthCount() {
        return firstWidthCount;
    }

    public int getFirstWidth() {
        return firstWidth;
    }

    public int getLastWidth() {
        return lastWidth;
    }

    private int getWidth() {
        return firstWidthCount * firstWidth + lastWidth;
    }

    public int[] unslice(int[][] data, int componentCount, int height) throws IIOException {
        final int width = getWidth();
        final int[] result = new int[width * height];

        for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
            if (result.length != data[componentIndex].length * componentCount) {
                throw new IIOException(String.format("Invalid array size for component #%d", componentIndex));
            }
        }

        int position = 0;
        int currentWidth = firstWidth / componentCount;
        for (int sliceIndex = 0; sliceIndex < firstWidthCount + 1; ++sliceIndex) {
            if (sliceIndex == firstWidthCount) {
                currentWidth = lastWidth / componentCount;
            }
            final int sliceOffset = sliceIndex * firstWidth;
            for (int y = 0; y < height; ++y) {
                final int yOffset = y * width;
                for (int x = 0; x < currentWidth; ++x) {
                    final int xOffset = x * componentCount;
                    for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
                        result[sliceOffset + yOffset + xOffset + componentIndex] = data[componentIndex][position];
                    }
                    position++;
                }
            }
        }

        return result;
    }
}
