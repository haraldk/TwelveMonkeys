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

import java.awt.image.DataBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

final class PNMHeader {
    private final short fileType;
    private final TupleType tupleType;
    private final int width;
    private final int height;
    private final int maxSample;

    private final List<String> comments;
    private final ByteOrder byteOrder;

    public PNMHeader(final short fileType, final TupleType tupleType, final int width, final int height, final int depth, final int maxSample, final Collection<String> comments) {
        this.fileType = isTrue(isValidFileType(fileType), fileType, String.format("Illegal type: %s", PNMImageReader.asASCII(fileType)));
        this.tupleType = notNull(tupleType, "tuple type may not be null");
        this.width = isTrue(width > 0, width, "width must be greater than 0: %d");
        this.height = isTrue(height > 0, height, "height must be greater than: %d");
        isTrue(depth == tupleType.getSamplesPerPixel(), depth, String.format("incorrect depth for %s, expected %d: %d", tupleType, tupleType.getSamplesPerPixel(), depth));
        this.maxSample = isTrue(tupleType.isValidMaxSample(maxSample), maxSample, "maxSample out of range: %d");

        this.comments = Collections.unmodifiableList(new ArrayList<String>(comments));

        byteOrder = ByteOrder.BIG_ENDIAN;
    }

    public PNMHeader(final short fileType, final TupleType tupleType, final int width, final int height, final int depth, final ByteOrder byteOrder, final Collection<String> comments) {
        this.fileType = isTrue(isValidFileType(fileType), fileType, String.format("Illegal type: %s", PNMImageReader.asASCII(fileType)));
        this.tupleType = notNull(tupleType, "tuple type may not be null");
        this.width = isTrue(width > 0, width, "width must be greater than 0: %d");
        this.height = isTrue(height > 0, height, "height must be greater than: %d");
        isTrue(depth == tupleType.getSamplesPerPixel(), depth, String.format("incorrect depth for %s, expected %d: %d", tupleType, tupleType.getSamplesPerPixel(), depth));

        this.maxSample = -1;
        this.byteOrder = byteOrder;

        this.comments = Collections.unmodifiableList(new ArrayList<String>(comments));
    }

    private boolean isValidFileType(final short fileType) {
        return (fileType >= PNM.PBM_PLAIN && fileType <= PNM.PAM || fileType == PNM.PFM_GRAY || fileType == PNM.PFM_RGB);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TupleType getTupleType() {
        return tupleType;
    }

    public int getMaxSample() {
        return maxSample;
    }

    public int getTransparency() {
        return tupleType.getTransparency();
    }

    public int getSamplesPerPixel() {
        return tupleType.getSamplesPerPixel();
    }

    public int getBitsPerSample() {
        if (fileType == PNM.PFM_GRAY || fileType == PNM.PFM_RGB) {
            return 32;
        }
        if (tupleType == TupleType.BLACKANDWHITE_WHITE_IS_ZERO) {
            // Special case for PBM, PAM B/W uses 8 bits per sample for some reason
            return 1;
        }
        if (maxSample <= PNM.MAX_VAL_8BIT) {
            return 8;
        }
        if (maxSample <= PNM.MAX_VAL_16BIT) {
            return 16;
        }
        if ((maxSample & 0xffffffffL) <= PNM.MAX_VAL_32BIT) {
            return 32;
        }

        throw new AssertionError("maxSample exceeds 32 bit");
    }

    public int getTransferType() {
        if (fileType == PNM.PFM_GRAY || fileType == PNM.PFM_RGB) {
            return DataBuffer.TYPE_FLOAT;
        }
        if (maxSample <= PNM.MAX_VAL_8BIT) {
            return DataBuffer.TYPE_BYTE;
        }
        if (maxSample <= PNM.MAX_VAL_16BIT) {
            return DataBuffer.TYPE_USHORT;
        }
        if ((maxSample & 0xffffffffL) <= PNM.MAX_VAL_32BIT) {
            return DataBuffer.TYPE_INT;
        }

        throw new AssertionError("maxSample exceeds 32 bit");
    }

    public List<String> getComments() {
        return comments;
    }

    public short getFileType() {
        return fileType;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    @Override
    public String toString() {
        return "PNMHeader{" +
                "fileType=" + PNMImageReader.asASCII(fileType) +
                ", tupleType=" + tupleType +
                ", width=" + width +
                ", height=" + height +
                (getTransferType() == DataBuffer.TYPE_FLOAT ? ", byteOrder=" + byteOrder : ", maxSample=" + maxSample) +
                ", comments=" + comments +
                '}';
    }
}
