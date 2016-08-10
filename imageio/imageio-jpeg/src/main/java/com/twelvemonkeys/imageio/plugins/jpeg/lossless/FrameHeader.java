/*
 * Copyright (C) 2015 Michael Martinez
 * Changes: Added support for selection values 2-7, fixed minor bugs &
 * warnings, split into multiple class files, and general clean up.
 *
 * 08-25-2015: Helmut Dersch agreed to a license change from LGPL to MIT.
 */

/*
 * Copyright (C) Helmut Dersch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.twelvemonkeys.imageio.plugins.jpeg.lossless;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public class FrameHeader {

    private ComponentSpec components[]; // Components
    private int dimX; // Number of samples per line
    private int dimY; // Number of lines
    private int numComp; // Number of component in the frame
    private int precision; // Sample Precision (from the original image)

    public ComponentSpec[] getComponents() {
        return components.clone();
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getNumComponents() {
        return numComp;
    }

    public int getPrecision() {
        return precision;
    }

    protected int read(final ImageInputStream data) throws IOException {
        int count = 0;

        final int length = data.readUnsignedShort();
        count += 2;

        precision = data.readUnsignedByte();
        count++;

        dimY = data.readUnsignedShort();
        count += 2;

        dimX = data.readUnsignedShort();
        count += 2;

        numComp = data.readUnsignedByte();
        count++;

        //components = new ComponentSpec[numComp]; // some image exceed this range...
        components = new ComponentSpec[256]; // setting to 256 -- not sure what it should be.

        for (int i = 1; i <= numComp; i++) {
            if (count > length) {
                throw new IOException("ERROR: frame format error");
            }

            final int c = data.readUnsignedByte();
            count++;

            if (count >= length) {
                throw new IOException("ERROR: frame format error [c>=Lf]");
            }

            final int temp = data.readUnsignedByte();
            count++;

            if (components[c] == null) {
                components[c] = new ComponentSpec();
            }

            components[c].hSamp = temp >> 4;
            components[c].vSamp = temp & 0x0F;
            components[c].quantTableSel = data.readUnsignedByte();
            count++;
        }

        if (count != length) {
            throw new IOException("ERROR: frame format error [Lf!=count]");
        }

        return 1;
    }
}
