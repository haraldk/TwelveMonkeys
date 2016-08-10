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

public class ScanHeader {

    private int ah;
    private int al;
    private int numComp; // Number of components in the scan
    private int selection; // Start of spectral or predictor selection
    private int spectralEnd; // End of spectral selection

    protected ScanComponent components[];

    public int getAh() {
        return ah;
    }

    public int getAl() {
        return al;
    }

    public int getNumComponents() {
        return numComp;
    }

    public int getSelection() {
        return selection;
    }

    public int getSpectralEnd() {
        return spectralEnd;
    }

    public void setAh(final int ah) {
        this.ah = ah;
    }

    public void setAl(final int al) {
        this.al = al;
    }

    public void setSelection(final int selection) {
        this.selection = selection;
    }

    public void setSpectralEnd(final int spectralEnd) {
        this.spectralEnd = spectralEnd;
    }

    protected int read(final ImageInputStream data) throws IOException {
        int count = 0;
        final int length = data.readUnsignedShort();
        count += 2;

        numComp = data.readUnsignedByte();
        count++;

        components = new ScanComponent[numComp];

        for (int i = 0; i < numComp; i++) {
            components[i] = new ScanComponent();

            if (count > length) {
                throw new IOException("ERROR: scan header format error");
            }

            components[i].setScanCompSel(data.readUnsignedByte());
            count++;

            final int temp = data.readUnsignedByte();
            count++;

            components[i].setDcTabSel(temp >> 4);
            components[i].setAcTabSel(temp & 0x0F);
        }

        setSelection(data.readUnsignedByte());
        count++;

        setSpectralEnd(data.readUnsignedByte());
        count++;

        final int temp = data.readUnsignedByte();
        setAh(temp >> 4);
        setAl(temp & 0x0F);
        count++;

        if (count != length) {
            throw new IOException("ERROR: scan header format error [count!=Ns]");
        }

        return 1;
    }
}
