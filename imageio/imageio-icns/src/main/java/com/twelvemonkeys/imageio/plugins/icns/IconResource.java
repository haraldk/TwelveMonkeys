/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.lang.Validate;

import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * IconResource
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IconResource.java,v 1.0 23.11.11 13:35 haraldk Exp$
 */
final class IconResource {
    // TODO: Rewrite using subclasses/instances!

    final long start;
    final int type;
    final int length;

    private IconResource(long start, int type, int length) {
        validate(type, length);

        this.start = start;
        this.type = type;
        this.length = length;
    }

    static IconResource read(final ImageInputStream input) throws IOException {
        return read(input.getStreamPosition(), input);
    }

    static IconResource read(final long offset, final ImageInputStream input) throws IOException {
        return new IconResource(offset, input.readInt(), input.readInt());
    }

    private void validate(int type, int length) {
        switch (type) {
            case ICNS.ICON:
                validateLengthForType(type, length, 128);
                break;
            case ICNS.ICN_:
                validateLengthForType(type, length, 256);
                break;
            case ICNS.icm_:
                validateLengthForType(type, length, 48);
                break;
            case ICNS.icm4:
                validateLengthForType(type, length, 96);
                break;
            case ICNS.icm8:
                validateLengthForType(type, length, 192);
                break;
            case ICNS.ics_:
                validateLengthForType(type, length, 64);
                break;
            case ICNS.ics4:
                validateLengthForType(type, length, 128);
                break;
            case ICNS.ics8:
            case ICNS.s8mk:
                validateLengthForType(type, length, 256);
                break;
            case ICNS.icl4:
                validateLengthForType(type, length, 512);
                break;
            case ICNS.icl8:
            case ICNS.l8mk:
                validateLengthForType(type, length, 1024);
                break;
            case ICNS.ich_:
                validateLengthForType(type, length, 576);
                break;
            case ICNS.ich4:
                validateLengthForType(type, length, 1152);
                break;
            case ICNS.ich8:
            case ICNS.h8mk:
                validateLengthForType(type, length, 2304);
                break;
            case ICNS.t8mk:
                validateLengthForType(type, length, 16384);
                break;
            case ICNS.ih32:
            case ICNS.is32:
            case ICNS.il32:
            case ICNS.it32:
            case ICNS.icp4:
            case ICNS.icp5:
            case ICNS.icp6:
            case ICNS.ic07:
            case ICNS.ic08:
            case ICNS.ic09:
            case ICNS.ic10:
            case ICNS.ic11:
            case ICNS.ic12:
            case ICNS.ic13:
            case ICNS.ic14:
                if (length > ICNS.RESOURCE_HEADER_SIZE) {
                    break;
                }
                throw new IllegalArgumentException(String.format("Wrong combination of icon type '%s' and length: %d", ICNSUtil.intToStr(type), length));
            case ICNS.icnV:
                validateLengthForType(type, length, 4);
                break;
            case ICNS.TOC_:
            default:
                if (length > ICNS.RESOURCE_HEADER_SIZE) {
                    break;
                }
                throw new IllegalStateException(String.format("Unknown icon type: '%s' length: %d", ICNSUtil.intToStr(type), length));
        }
    }

    private void validateLengthForType(int type, int length, final int expectedLength) {
        Validate.isTrue(
                length == expectedLength + ICNS.RESOURCE_HEADER_SIZE, // Compute to make lengths more logical
                String.format(
                        "Wrong combination of icon type '%s' and length: %d (expected: %d)",
                        ICNSUtil.intToStr(type), length - ICNS.RESOURCE_HEADER_SIZE, expectedLength
                )
        );
    }

    Dimension size() {
        switch (type) {
            case ICNS.ICON:
            case ICNS.ICN_:
                return new Dimension(32, 32);
            case ICNS.icm_:
            case ICNS.icm4:
            case ICNS.icm8:
                return new Dimension(16, 12);
            case ICNS.ics_:
            case ICNS.ics4:
            case ICNS.ics8:
            case ICNS.is32:
            case ICNS.s8mk:
            case ICNS.icp4:
                return new Dimension(16, 16);
            case ICNS.icl4:
            case ICNS.icl8:
            case ICNS.il32:
            case ICNS.l8mk:
            case ICNS.icp5:
            case ICNS.ic11:
                return new Dimension(32, 32);
            case ICNS.ich_:
            case ICNS.ich4:
            case ICNS.ich8:
            case ICNS.ih32:
            case ICNS.h8mk:
                return new Dimension(48, 48);
            case ICNS.icp6:
            case ICNS.ic12:
                return new Dimension(64, 64);
            case ICNS.it32:
            case ICNS.t8mk:
            case ICNS.ic07:
                return new Dimension(128, 128);
            case ICNS.ic08:
            case ICNS.ic13:
                return new Dimension(256, 256);
            case ICNS.ic09:
            case ICNS.ic14:
                return new Dimension(512, 512);
            case ICNS.ic10:
                return new Dimension(1024, 1024);
            default:
                throw new IllegalStateException(String.format("Unknown icon type: '%s'", ICNSUtil.intToStr(type)));
        }
    }

    int depth() {
        switch (type) {
            case ICNS.ICON:
            case ICNS.ICN_:
            case ICNS.icm_:
            case ICNS.ics_:
            case ICNS.ich_:
                return 1;
            case ICNS.icm4:
            case ICNS.ics4:
            case ICNS.icl4:
            case ICNS.ich4:
                return 4;
            case ICNS.icm8:
            case ICNS.ics8:
            case ICNS.icl8:
            case ICNS.ich8:
            case ICNS.s8mk:
            case ICNS.l8mk:
            case ICNS.h8mk:
            case ICNS.t8mk:
                return 8;
            case ICNS.is32:
            case ICNS.il32:
            case ICNS.ih32:
            case ICNS.it32:
            case ICNS.icp4:
            case ICNS.icp5:
            case ICNS.icp6:
            case ICNS.ic07:
            case ICNS.ic08:
            case ICNS.ic09:
            case ICNS.ic10:
            case ICNS.ic11:
            case ICNS.ic12:
            case ICNS.ic13:
            case ICNS.ic14:
                return 32;
            default:
                throw new IllegalStateException(String.format("Unknown icon type: '%s'", ICNSUtil.intToStr(type)));
        }
    }

    boolean isUnknownType() {
        // Unknown types should simply be skipped when reading
        switch (type) {
            case ICNS.ICON:
            case ICNS.ICN_:
            case ICNS.icm_:
            case ICNS.ics_:
            case ICNS.ich_:
            case ICNS.icm4:
            case ICNS.ics4:
            case ICNS.icl4:
            case ICNS.ich4:
            case ICNS.icm8:
            case ICNS.ics8:
            case ICNS.icl8:
            case ICNS.ich8:
            case ICNS.s8mk:
            case ICNS.l8mk:
            case ICNS.h8mk:
            case ICNS.t8mk:
            case ICNS.is32:
            case ICNS.il32:
            case ICNS.ih32:
            case ICNS.it32:
            case ICNS.icp4:
            case ICNS.icp5:
            case ICNS.icp6:
            case ICNS.ic07:
            case ICNS.ic08:
            case ICNS.ic09:
            case ICNS.ic10:
            case ICNS.ic11:
            case ICNS.ic12:
            case ICNS.ic13:
            case ICNS.ic14:
                return false;
        }

        return true;
    }

    boolean hasMask() {
        switch (type) {
            case ICNS.ICN_:
            case ICNS.icm_:
            case ICNS.ics_:
            case ICNS.ich_:
                return true;
        }

        return false;
    }

    boolean isMaskType() {
        switch (type) {
            case ICNS.s8mk:
            case ICNS.l8mk:
            case ICNS.h8mk:
            case ICNS.t8mk:
                return true;
        }

        return false;
    }

    boolean isCompressed() {
        switch (type) {
            case ICNS.is32:
            case ICNS.il32:
            case ICNS.ih32:
            case ICNS.it32:
                // http://www.macdisk.com/maciconen.php
                // "One should check whether the data length corresponds to the theoretical length (width * height)."
                Dimension size = size();
                if (length != (size.width * size.height * depth() / 8 + ICNS.RESOURCE_HEADER_SIZE)) {
                    return true;
                }
        }

        return false;
    }

    boolean isForeignFormat() {
        // Recent entries contains full JPEG 2000 or PNG streams
        switch (type) {
            case ICNS.icp4:
            case ICNS.icp5:
            case ICNS.icp6:
            case ICNS.ic07:
            case ICNS.ic08:
            case ICNS.ic09:
            case ICNS.ic10:
            case ICNS.ic11:
            case ICNS.ic12:
            case ICNS.ic13:
            case ICNS.ic14:
                return true;
        }

        return false;
    }

    boolean isTOC() {
        return type == ICNS.TOC_;
    }

    @Override
    public int hashCode() {
        return (int) start ^ type;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other != null && other.getClass() == getClass() && isEqual((IconResource) other);
    }

    private boolean isEqual(IconResource other) {
        // This isn't strictly true, as resource must reside in same stream as well, but good enough for now
        return start == other.start && type == other.type && length == other.length;
    }

    @Override
    public String toString() {
        return String.format("%s['%s' start: %d, length: %d%s]", getClass().getSimpleName(), ICNSUtil.intToStr(type), start, length, isCompressed() ? " (compressed)" : "");
    }

    static int typeFromImage(final RenderedImage image, final String compression) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width == height) {
            switch (compression) {
                case "JPEG2000":
                case "PNG":
                    return typeFromWidthForeign(width);
                case "None":
                case "RLE":
                    return typeFromWidthNative(width);
                default:
                    throw new IllegalArgumentException("Unsupported compression for ICNS: " + compression);
            }
        }

        // Note: Strictly, the format supports an ancient 16x12 size, but I doubt we'll ever support that
        throw new IllegalArgumentException(String.format("Unsupported dimensions for ICNS, only square icons supported: %dx%d", width, height));
    }
    // NOTE: These also needs a mask, if there's an alpha channel

    private static int typeFromWidthNative(final int width) {
        switch (width) {
            case 16:
                return ICNS.is32;
            case 32:
                return ICNS.il32;
            case 48:
                return ICNS.ih32;
            case 128:
                return ICNS.it32;
            default:
                throw new IllegalArgumentException(String.format("Unsupported dimensions for ICNS, only 16, 32, 48 and 128 supported: %dx%d", width, width));
        }
    }

    private static int typeFromWidthForeign(final int width) {
        switch (width) {
            case 16:
                return ICNS.icp4;
            case 32:
                return ICNS.icp5;
            case 64:
                return ICNS.icp6;
            case 128:
                return ICNS.ic07;
            case 256:
                return ICNS.ic08;
            case 512:
                return ICNS.ic09;
            case 1024:
                return ICNS.ic10;
            default:
                throw new IllegalArgumentException(String.format("Unsupported dimensions for ICNS, only multiples of 2 from 16 to 1024 supported: %dx%d", width, width));
        }
    }
}
