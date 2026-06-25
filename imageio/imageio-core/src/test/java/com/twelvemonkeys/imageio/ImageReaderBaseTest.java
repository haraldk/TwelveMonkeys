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

package com.twelvemonkeys.imageio;


import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;

import org.junit.jupiter.api.Test;

/**
 * ImageReaderBaseTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageReaderBaseTest.java,v 1.0 23.05.12 09:50 haraldk Exp$
 */
public class ImageReaderBaseTest {

    private static final List<ImageTypeSpecifier> TYPES = Arrays.asList(
            ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB),
            ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)
    );

    @Test
    public void testGetDestinationZeroWidth() throws IIOException {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 0, 42));
    }

    @Test
    public void testGetDestinationNegativeWidth() throws IIOException {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), -1, 42));

    }

    @Test
    public void testGetDestinationZeroHeight() throws IIOException {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 42, 0));

    }

    @Test
    public void testGetDestinationNegativeHeight() throws IIOException {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 42, -1));
    }

    @Test
    public void testGetDestinationNullTypes() throws IIOException {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, null, 42, 42));
    }

    @Test
    public void testGetDestinationNoTypes() throws IIOException {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, Collections.<ImageTypeSpecifier>emptyList().iterator(), 42, 42));
    }

    @Test
    public void testGetDestinationParamSourceRegionWider() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(42, 1));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(3, destination.getWidth());
        assertEquals(1, destination.getHeight());
        assertEquals(TYPES.get(0).getBufferedImageType(), destination.getType());
    }

    @Test
    public void testGetDestinationParamSourceRegionTaller() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(1, 42));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(1, destination.getWidth());
        assertEquals(3, destination.getHeight());
        assertEquals(TYPES.get(0).getBufferedImageType(), destination.getType());
    }

    @Test
    public void testGetDestinationParamDestinationWider() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(42, 1, BufferedImage.TYPE_INT_RGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(42, destination.getWidth());
        assertEquals(1, destination.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, destination.getType());
    }

    @Test
    public void testGetDestinationParamDestinationTaller() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(1, 42, BufferedImage.TYPE_INT_ARGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(1, destination.getWidth());
        assertEquals(42, destination.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, destination.getType());
    }

    @Test
    public void testGetDestinationNoParam() throws IIOException {
        BufferedImage destination = ImageReaderBase.getDestination(null, TYPES.iterator(), 42, 1);
        assertEquals(BufferedImage.TYPE_INT_RGB, destination.getType());
        assertEquals(42, destination.getWidth());
        assertEquals(1, destination.getHeight());
    }

    @Test
    public void testGetDestinationParamNoDestination() throws IIOException {
        BufferedImage destination = ImageReaderBase.getDestination(new ImageReadParam(), TYPES.iterator(), 42, 1);
        assertEquals(BufferedImage.TYPE_INT_RGB, destination.getType());
        assertEquals(42, destination.getWidth());
        assertEquals(1, destination.getHeight());
    }

    @Test
    public void testGetDestinationParamGoodDestination() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(21, 1, BufferedImage.TYPE_INT_ARGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 42, 1);
        assertEquals(BufferedImage.TYPE_INT_ARGB, destination.getType());
        assertEquals(21, destination.getWidth());
        assertEquals(1, destination.getHeight());
    }

    @Test
    public void testGetDestinationParamIllegalDestination() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(21, 1, BufferedImage.TYPE_USHORT_565_RGB));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 42, 1));
    }

    @Test
    public void testGetDestinationParamGoodDestinationType() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 6, 7);
        assertEquals(BufferedImage.TYPE_INT_ARGB, destination.getType());
        assertEquals(6, destination.getWidth());
        assertEquals(7, destination.getHeight());
    }

    @Test
    public void testGetDestinationParamGoodDestinationTypeAlt() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        // In essence, this is the same as TYPE_INT_ARGB
        ImageTypeSpecifier type = ImageTypeSpecifier.createPacked(ColorSpace.getInstance(ColorSpace.CS_sRGB), 0xff0000, 0xff00, 0xff, 0xff000000, DataBuffer.TYPE_INT, false);
        param.setDestinationType(type);
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 6, 7);
        assertEquals(BufferedImage.TYPE_INT_ARGB, destination.getType());
        assertEquals(6, destination.getWidth());
        assertEquals(7, destination.getHeight());
    }

    @Test
    public void testGetDestinationParamIllegalDestinationType() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 6, 7));
    }

    @Test
    public void testGetDestinationParamIllegalDestinationTypeAlt() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 6, 7));
    }

    @Test
    public void testGetDestinationSourceExceedsIntegerMax() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(42, 7));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), Integer.MAX_VALUE, 42);// 90 194 313 174 pixels
        assertEquals(42, destination.getWidth());
        assertEquals(7, destination.getHeight());
        assertEquals(TYPES.get(0).getBufferedImageType(), destination.getType());
    }

    @Test
    public void testGetDestinationParamDestinationExceedsIntegerMax() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(3 * Short.MAX_VALUE, 2 * Short.MAX_VALUE)); // 6 442 057 734 pixels
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 6 * Short.MAX_VALUE, 4 * Short.MAX_VALUE)); // 25 768 230 936 pixels
    }

    @Test
    public void testGetDestinationDimensionExceedsIntegerMax() throws IIOException {
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 3 * Short.MAX_VALUE, 2 * Short.MAX_VALUE)); // 6 442 057 734 pixels
    }

    @Test
    public void testGetDestinationStorageExceedsIntegerMax() throws IIOException {
        Set<ImageTypeSpecifier> byteTypes = singleton(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(null, byteTypes.iterator(), Short.MAX_VALUE,  Short.MAX_VALUE)); // 1 073 676 289 pixels
        // => 3 221 028 867 bytes needed in continuous array, not possible
    }

    @Test
    public void testHasExplicitDestinationNull() {
        assertFalse(ImageReaderBase.hasExplicitDestination(null));

    }

    @Test
    public void testHasExplicitDestinationDefaultParam() {
        assertFalse(ImageReaderBase.hasExplicitDestination(new ImageReadParam()));
    }

    @Test
    public void testHasExplicitDestinationParamWithDestination() {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY));
        assertTrue(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    public void testHasExplicitDestinationParamWithDestinationType() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
        assertTrue(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    public void testHasExplicitDestinationParamWithDestinationOffset() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationOffset(new Point(42, 42));
        assertTrue(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    public void testHasExplicitDestinationParamWithDestinationOffsetUnspecified() {
        ImageReadParam param = new ImageReadParam();
        // getDestinationOffset should now return new Point(0, 0)
        assertFalse(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    public void testHasExplicitDestinationParamWithDestinationOffsetOrigin() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationOffset(new Point(0, 0));
        assertFalse(ImageReaderBase.hasExplicitDestination(param));
    }

    // Destination allocation size guard (GHSA-7c3w-m9qh-j2vj). TYPES is INT_RGB/INT_ARGB, i.e. 4 bytes/pixel.

    @Test
    public void testGetDestinationWithinExpansionRatioIsAllowed() throws IIOException {
        // 100 x 100 x 4 = 40000 bytes; with 40000 bytes of input the limit is 40000 * 16, so it must not be rejected.
        BufferedImage destination = ImageReaderBase.getDestination(null, TYPES.iterator(), 100, 100, 40000L, 16);
        assertNotNull(destination);
        assertEquals(100, destination.getWidth());
        assertEquals(100, destination.getHeight());
    }

    @Test
    public void testGetDestinationExceedingExpansionRatioIsRejected() {
        // A 100-byte input must not be able to force a ~1.6 GB raster (20000 x 20000 x 4).
        IIOException exception = assertThrows(IIOException.class,
                () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 20000, 20000, 100L, 16));
        assertTrue(exception.getMessage().contains("exceeding"), exception.getMessage());
    }

    @Test
    public void testGetDestinationUnknownLengthRejectedByMaxImageBytes() {
        // Unknown length (-1) uses the MAX_IMAGE_BYTES ceiling; override it low to assert deterministic rejection.
        System.setProperty(ImageReaderBase.MAX_IMAGE_BYTES_PROPERTY, String.valueOf(1024 * 1024));
        try {
            // 1000 x 1000 x 4 = 4 MB > 1 MB ceiling.
            IIOException exception = assertThrows(IIOException.class,
                    () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 1000, 1000, -1L, 16));
            assertTrue(exception.getMessage().contains("unknown length"), exception.getMessage());
        }
        finally {
            System.clearProperty(ImageReaderBase.MAX_IMAGE_BYTES_PROPERTY);
        }
    }

    @Test
    public void testGetDestinationUnknownLengthWithinMaxImageBytesIsAllowed() throws IIOException {
        System.setProperty(ImageReaderBase.MAX_IMAGE_BYTES_PROPERTY, String.valueOf(64 * 1024 * 1024));
        try {
            // 1000 x 1000 x 4 = 4 MB < 64 MB ceiling.
            BufferedImage destination = ImageReaderBase.getDestination(null, TYPES.iterator(), 1000, 1000, -1L, 16);
            assertNotNull(destination);
        }
        finally {
            System.clearProperty(ImageReaderBase.MAX_IMAGE_BYTES_PROPERTY);
        }
    }

    @Test
    public void testGetDestinationWithoutRatioIsNotGuarded() throws IIOException {
        // A non-positive ratio (and the legacy four-argument overload) disables the guard: a tiny declared input
        // length must not cause rejection. 1000 x 1000 x 4 = 4 MB is a safe allocation to prove this.
        assertNotNull(ImageReaderBase.getDestination(null, TYPES.iterator(), 1000, 1000, 1L, 0));
        assertNotNull(ImageReaderBase.getDestination(null, TYPES.iterator(), 1000, 1000));
    }
}
