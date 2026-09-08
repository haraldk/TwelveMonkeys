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
import java.awt.image.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;

import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import org.junit.jupiter.api.Test;

/**
 * ImageReaderBaseTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageReaderBaseTest.java,v 1.0 23.05.12 09:50 haraldk Exp$
 */
class ImageReaderBaseTest {

    private static final List<ImageTypeSpecifier> TYPES = Arrays.asList(
            ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB),
            ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)
    );

    @Test
    void testGetDestinationZeroWidth() {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 0, 42));
    }

    @Test
    void testGetDestinationNegativeWidth() {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), -1, 42));

    }

    @Test
    void testGetDestinationZeroHeight() {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 42, 0));

    }

    @Test
    void testGetDestinationNegativeHeight() {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 42, -1));
    }

    @Test
    void testGetDestinationNullTypes() {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, null, 42, 42));
    }

    @Test
    void testGetDestinationNoTypes() {
        assertThrows(IllegalArgumentException.class, () -> ImageReaderBase.getDestination(null, Collections.emptyIterator(), 42, 42));
    }

    @Test
    void testGetDestinationParamSourceRegionWider() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(42, 1));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(3, destination.getWidth());
        assertEquals(1, destination.getHeight());
        assertEquals(TYPES.get(0).getBufferedImageType(), destination.getType());
    }

    @Test
    void testGetDestinationParamSourceRegionTaller() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(1, 42));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(1, destination.getWidth());
        assertEquals(3, destination.getHeight());
        assertEquals(TYPES.get(0).getBufferedImageType(), destination.getType());
    }

    @Test
    void testGetDestinationParamDestinationWider() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(42, 1, BufferedImage.TYPE_INT_RGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(42, destination.getWidth());
        assertEquals(1, destination.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, destination.getType());
    }

    @Test
    void testGetDestinationParamDestinationTaller() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(1, 42, BufferedImage.TYPE_INT_ARGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 3, 3);
        assertEquals(1, destination.getWidth());
        assertEquals(42, destination.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, destination.getType());
    }

    @Test
    void testGetDestinationNoParam() throws IIOException {
        BufferedImage destination = ImageReaderBase.getDestination(null, TYPES.iterator(), 42, 1);
        assertEquals(BufferedImage.TYPE_INT_RGB, destination.getType());
        assertEquals(42, destination.getWidth());
        assertEquals(1, destination.getHeight());
    }

    @Test
    void testGetDestinationParamNoDestination() throws IIOException {
        BufferedImage destination = ImageReaderBase.getDestination(new ImageReadParam(), TYPES.iterator(), 42, 1);
        assertEquals(BufferedImage.TYPE_INT_RGB, destination.getType());
        assertEquals(42, destination.getWidth());
        assertEquals(1, destination.getHeight());
    }

    @Test
    void testGetDestinationParamGoodDestination() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(21, 1, BufferedImage.TYPE_INT_ARGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 42, 1);
        assertEquals(BufferedImage.TYPE_INT_ARGB, destination.getType());
        assertEquals(21, destination.getWidth());
        assertEquals(1, destination.getHeight());
    }

    @Test
    void testGetDestinationParamIllegalDestination() {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(21, 1, BufferedImage.TYPE_USHORT_565_RGB));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 42, 1));
    }

    @Test
    void testGetDestinationParamGoodDestinationType() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), 6, 7);
        assertEquals(BufferedImage.TYPE_INT_ARGB, destination.getType());
        assertEquals(6, destination.getWidth());
        assertEquals(7, destination.getHeight());
    }

    @Test
    void testGetDestinationParamGoodDestinationTypeAlt() throws IIOException {
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
    void testGetDestinationParamIllegalDestinationType() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 6, 7));
    }

    @Test
    void testGetDestinationParamIllegalDestinationTypeAlt() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 6, 7));
    }

    @Test
    void testGetDestinationSourceExceedsIntegerMax() throws IIOException {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(42, 7));
        BufferedImage destination = ImageReaderBase.getDestination(param, TYPES.iterator(), Integer.MAX_VALUE, 42);// 90 194 313 174 pixels
        assertEquals(42, destination.getWidth());
        assertEquals(7, destination.getHeight());
        assertEquals(TYPES.get(0).getBufferedImageType(), destination.getType());
    }

    @Test
    void testGetDestinationParamDestinationExceedsIntegerMax() {
        ImageReadParam param = new ImageReadParam();
        param.setSourceRegion(new Rectangle(3 * Short.MAX_VALUE, 2 * Short.MAX_VALUE)); // 6 442 057 734 pixels
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(param, TYPES.iterator(), 6 * Short.MAX_VALUE, 4 * Short.MAX_VALUE)); // 25 768 230 936 pixels
    }

    @Test
    void testGetDestinationDimensionExceedsIntegerMax() {
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(null, TYPES.iterator(), 3 * Short.MAX_VALUE, 2 * Short.MAX_VALUE)); // 6 442 057 734 pixels
    }

    @Test
    void testGetDestinationStorageExceedsIntegerMax() {
        Set<ImageTypeSpecifier> byteTypes = singleton(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
        assertThrows(IIOException.class, () -> ImageReaderBase.getDestination(null, byteTypes.iterator(), Short.MAX_VALUE,  Short.MAX_VALUE)); // 1 073 676 289 pixels
        // => 3 221 028 867 bytes needed in continuous array, not possible
    }

    @Test
    void testHasExplicitDestinationNull() {
        assertFalse(ImageReaderBase.hasExplicitDestination(null));

    }

    @Test
    void testHasExplicitDestinationDefaultParam() {
        assertFalse(ImageReaderBase.hasExplicitDestination(new ImageReadParam()));
    }

    @Test
    void testHasExplicitDestinationParamWithDestination() {
        ImageReadParam param = new ImageReadParam();
        param.setDestination(new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY));
        assertTrue(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    void testHasExplicitDestinationParamWithDestinationType() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationType(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
        assertTrue(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    void testHasExplicitDestinationParamWithDestinationOffset() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationOffset(new Point(42, 42));
        assertTrue(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    void testHasExplicitDestinationParamWithDestinationOffsetUnspecified() {
        ImageReadParam param = new ImageReadParam();
        // getDestinationOffset should now return new Point(0, 0)
        assertFalse(ImageReaderBase.hasExplicitDestination(param));
    }

    @Test
    void testHasExplicitDestinationParamWithDestinationOffsetOrigin() {
        ImageReadParam param = new ImageReadParam();
        param.setDestinationOffset(new Point(0, 0));
        assertFalse(ImageReaderBase.hasExplicitDestination(param));
    }

    // Destination allocation size guard (GHSA-7c3w-m9qh-j2vj). TYPES is INT_RGB/INT_ARGB, i.e. 4 bytes/pixel.

    @Test
    void testValidateSourceSizeWithinExpansionRatioIsAllowed() {
        // 100 x 100 x 4 = 40000 bytes; with 40000 bytes of input the limit is 40000 * 16, so it must not be rejected.
        assertDoesNotThrow(() -> ImageReaderBase.validateSourceSize(TYPES.iterator().next(), 100, 100, 40000L, 16));
    }

    @Test
    void testValidateSourceSizeExceedingExpansionRatioIsRejected() {
        // A 100-byte input must not be able to force a ~1.6 GB raster (20000 x 20000 x 4).
        IIOException exception = assertThrows(IIOException.class,
                () -> ImageReaderBase.validateSourceSize(TYPES.iterator().next(), 20000, 20000, 100L, 16));
        assertTrue(exception.getMessage().contains("exceeding"), exception.getMessage());
    }

    @Test
    void testValidateSourceSizeUnknownLengthRejectedByMaxImageBytes() {
        // Unknown length (-1) uses the injected ceiling; 4 MB declared against a 1 MB ceiling must be rejected.
        IIOException exception = assertThrows(IIOException.class,
                () -> ImageReaderBase.validateSourceSize(4L * 1024 * 1024, -1L, 16, 1024 * 1024));
        assertTrue(exception.getMessage().contains("unknown length"), exception.getMessage());
    }

    @Test
    void testValidateSourceSizeUnknownLengthWithinMaxImageBytesIsAllowed() {
        // 4 MB declared against a 64 MB ceiling must be allowed (no exception).
        assertDoesNotThrow(() -> ImageReaderBase.validateSourceSize(4L * 1024 * 1024, -1L, 16, 64L * 1024 * 1024));
    }

    @Test
    void testDefaultMaxImageBytesIsCappedAndFloored() {
        // No property set: default is half the heap, capped at 512 MB, floored at 64 MB.
        long cap = 512L * 1024 * 1024;
        long floor = 64L * 1024 * 1024;

        long maxHeap = Runtime.getRuntime().maxMemory();

        if (maxHeap < floor) {
            assertEquals(floor, ImageReaderBase.defaultMaxImageBytes());
        }
        else if (maxHeap > cap) {
            assertEquals(cap, ImageReaderBase.defaultMaxImageBytes());
        }
        else {
            assertEquals(maxHeap / 2, ImageReaderBase.defaultMaxImageBytes());
        }
    }

    @Test
    void testComputeSizeBinary() {
        ImageTypeSpecifier binarySpec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);
        assertEquals(((DataBufferByte) binarySpec.getSampleModel(8, 10).createDataBuffer()).getData().length, ImageReaderBase.computeByteSize(binarySpec, 8, 10));
        assertEquals(((DataBufferByte) binarySpec.getSampleModel(10, 10).createDataBuffer()).getData().length, ImageReaderBase.computeByteSize(binarySpec, 10, 10));
        assertEquals(10, ImageReaderBase.computeByteSize(binarySpec, 8, 10)); // 8 bits / 1 byte per row
        assertEquals(20, ImageReaderBase.computeByteSize(binarySpec, 10, 10)); // 10 bits / 2 bytes per row

        // Special case, SampleModel overflow
        assertEquals(10L * ((Integer.MAX_VALUE + 7L) / 8), ImageReaderBase.computeByteSize(binarySpec, Integer.MAX_VALUE, 10));
        assertEquals(2L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(binarySpec, 9, Integer.MAX_VALUE));
    }

    @Test
    void testComputeSizeIndexed() {
        // indexed2 is basically same as binary
        ImageTypeSpecifier indexed2Spec = ImageTypeSpecifiers.createIndexed(new int[2], false, -1, 1, DataBuffer.TYPE_BYTE);
        assertEquals(10, ImageReaderBase.computeByteSize(indexed2Spec, 8, 10));
        assertEquals(20, ImageReaderBase.computeByteSize(indexed2Spec, 10, 10));

        ImageTypeSpecifier indexed4Spec = ImageTypeSpecifiers.createIndexed(new int[4], false, -1, 2, DataBuffer.TYPE_BYTE);
        assertEquals(10, ImageReaderBase.computeByteSize(indexed4Spec, 4, 10));
        assertEquals(30, ImageReaderBase.computeByteSize(indexed4Spec, 10, 10));

        // 3 bits/pixel could be packed but it's impractical, so reverts to 1 byte/sample
        ImageTypeSpecifier indexed8Spec = ImageTypeSpecifiers.createIndexed(new int[8], false, -1, 3, DataBuffer.TYPE_BYTE);
        assertEquals(((DataBufferByte) indexed8Spec.getSampleModel(10, 10).createDataBuffer()).getData().length, ImageReaderBase.computeByteSize(indexed8Spec, 10, 10));
        assertEquals(100, ImageReaderBase.computeByteSize(indexed8Spec, 10, 10));

        ImageTypeSpecifier indexed16Spec = ImageTypeSpecifiers.createIndexed(new int[16], false, -1, 4, DataBuffer.TYPE_BYTE);
        assertEquals(10, ImageReaderBase.computeByteSize(indexed16Spec, 2, 10));
        assertEquals(50, ImageReaderBase.computeByteSize(indexed16Spec, 10, 10));

        // Anything above 4 bits is impractical to pack, so we have 1 byte/sample
        ImageTypeSpecifier indexed32Spec = ImageTypeSpecifiers.createIndexed(new int[32], false, -1, 5, DataBuffer.TYPE_BYTE);
        assertEquals(100, ImageReaderBase.computeByteSize(indexed32Spec, 10, 10));

        ImageTypeSpecifier indexed64Spec = ImageTypeSpecifiers.createIndexed(new int[64], false, -1, 6, DataBuffer.TYPE_BYTE);
        assertEquals(100, ImageReaderBase.computeByteSize(indexed64Spec, 10, 10));

        ImageTypeSpecifier indexed128Spec = ImageTypeSpecifiers.createIndexed(new int[128], false, -1, 7, DataBuffer.TYPE_BYTE);
        assertEquals(100, ImageReaderBase.computeByteSize(indexed128Spec, 10, 10));

        ImageTypeSpecifier indexed256Spec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_INDEXED);
        assertEquals(100, ImageReaderBase.computeByteSize(indexed256Spec, 10, 10));

        // Trying to pack two 8 bit samples into a 16 bit sample won't work...
        // IndexColorModel 8 bits/TYPE_USHORT will create a sample model with TYPE_USHORT but a raster with TYPE_BYTE,
        // which is incompatible.
        //ImageTypeSpecifier indexed256UshortSpec = ImageTypeSpecifiers.createIndexed(new int[256], false, -1, 8, DataBuffer.TYPE_USHORT);
        //assertEquals(100, ImageReaderBase.computeByteSize(indexed256UshortSpec, 10, 10)); // Will fail, actual 200

        // Custom setup that creates the model we intended to create above...
        int[] colors = new int[256];
        IndexColorModel indexColorModel = new IndexColorModel(8, colors.length, colors, 0, false, -1, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = new MultiPixelPackedSampleModel(DataBuffer.TYPE_USHORT, 1, 1, 8);
        ImageTypeSpecifier indexed256UshortCustomSpec = new ImageTypeSpecifier(indexColorModel, sampleModel);
        assertEquals(100, ImageReaderBase.computeByteSize(indexed256UshortCustomSpec, 10, 10));
        // Extra verification that it actually would work
        BufferedImage bufferedImage = indexed256UshortCustomSpec.createBufferedImage(10, 10);
        assertEquals(2L * ((DataBufferUShort) bufferedImage.getRaster().getDataBuffer()).getData().length, ImageReaderBase.computeByteSize(indexed256UshortCustomSpec, 10, 10));

        // Not sure why creating a 10 bit IndexColorModel with TYPE_BYTE is allowed?
        //ImageTypeSpecifier indexed1024BadByteSpec = ImageTypeSpecifiers.createIndexed(new int[1024], false, -1, 10, DataBuffer.TYPE_BYTE);
        //assertEquals(2 * 100, ImageReaderBase.computeByteSize(indexed1024BadByteSpec, 10, 10)); // Will fail, actual 100

        ImageTypeSpecifier indexed1024Spec = ImageTypeSpecifiers.createIndexed(new int[1024], false, -1, 10, DataBuffer.TYPE_USHORT);
        assertEquals(2 * 100, ImageReaderBase.computeByteSize(indexed1024Spec, 10, 10));

        // Special case, SampleModel overflow
        assertEquals(10L * ((Integer.MAX_VALUE + 7L) / 8), ImageReaderBase.computeByteSize(indexed2Spec, Integer.MAX_VALUE, 10));
        assertEquals(2L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(indexed2Spec, 10, Integer.MAX_VALUE));
        assertEquals(10L * ((Integer.MAX_VALUE + 3L) / 4), ImageReaderBase.computeByteSize(indexed4Spec, Integer.MAX_VALUE, 10));
        assertEquals(3L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(indexed4Spec, 10, Integer.MAX_VALUE));
        assertEquals(10L * ((Integer.MAX_VALUE + 1L) / 2), ImageReaderBase.computeByteSize(indexed16Spec, Integer.MAX_VALUE, 10));
        assertEquals(5L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(indexed16Spec, 10, Integer.MAX_VALUE));
        assertEquals(10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(indexed256Spec, 10, Integer.MAX_VALUE));
    }

    @Test
    void testComputeSizeGray() {
        ColorSpace gray = ColorSpace.getInstance(ColorSpace.CS_GRAY);

        ImageTypeSpecifier byteGraySpec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
        assertEquals(100, ImageReaderBase.computeByteSize(byteGraySpec, 10, 10));
        ImageTypeSpecifier byteGrayASpec = ImageTypeSpecifiers.createInterleaved(gray, new int[]{0, 1}, DataBuffer.TYPE_BYTE, true, false);
        assertEquals(((DataBufferByte) byteGrayASpec.getSampleModel(10, 10).createDataBuffer()).getData().length, ImageReaderBase.computeByteSize(byteGrayASpec, 10, 10));
        assertEquals(2 * 100, ImageReaderBase.computeByteSize(byteGrayASpec, 10, 10));

        ImageTypeSpecifier ushortGraySpec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_GRAY);
        assertEquals(((DataBufferUShort) ushortGraySpec.getSampleModel(10, 10).createDataBuffer()).getData().length * 2L, ImageReaderBase.computeByteSize(ushortGraySpec, 10, 10));
        assertEquals(2 * 100, ImageReaderBase.computeByteSize(ushortGraySpec, 10, 10));
        ImageTypeSpecifier ushortGrayASpec = ImageTypeSpecifiers.createInterleaved(gray, new int[]{0, 1}, DataBuffer.TYPE_USHORT, true, false);
        assertEquals(2 * 2 * 100, ImageReaderBase.computeByteSize(ushortGrayASpec, 10, 10));

        ImageTypeSpecifier intGraySpec = ImageTypeSpecifiers.createInterleaved(gray, new int[]{0}, DataBuffer.TYPE_INT, false, false);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(intGraySpec, 10, 10));
        ImageTypeSpecifier intGrayASpec = ImageTypeSpecifiers.createInterleaved(gray, new int[]{0, 1}, DataBuffer.TYPE_INT, true, false);
        assertEquals(4 * 2 * 100, ImageReaderBase.computeByteSize(intGrayASpec, 10, 10));

        ImageTypeSpecifier packedGray1 = ImageTypeSpecifiers.createPackedGrayscale(gray, 1, DataBuffer.TYPE_BYTE);
        assertEquals(20, ImageReaderBase.computeByteSize(packedGray1, 10, 10)); // 10 bits / 2 bytes per row
        ImageTypeSpecifier packedGray2 = ImageTypeSpecifiers.createPackedGrayscale(gray, 2, DataBuffer.TYPE_BYTE);
        assertEquals(30, ImageReaderBase.computeByteSize(packedGray2, 10, 10)); // 20 bits / 3 bytes per row
        ImageTypeSpecifier packedGray4 = ImageTypeSpecifiers.createPackedGrayscale(gray, 4, DataBuffer.TYPE_BYTE);
        assertEquals(50, ImageReaderBase.computeByteSize(packedGray4, 10, 10)); // 40 bits / 5 bytes per row

        // Special case, SampleModel overflow
        assertEquals(2L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(byteGrayASpec, Integer.MAX_VALUE, 10));
        assertEquals(2L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(ushortGraySpec, 10, Integer.MAX_VALUE));
    }

    @Test
    void testComputeSizeRGB() {
        ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

        ImageTypeSpecifier ushortRGBPacked555Spec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_555_RGB);
        assertEquals(((DataBufferUShort) ushortRGBPacked555Spec.getSampleModel(10, 10).createDataBuffer()).getData().length * 2L, ImageReaderBase.computeByteSize(ushortRGBPacked555Spec, 10, 10));
        assertEquals(2 * 100, ImageReaderBase.computeByteSize(ushortRGBPacked555Spec, 10, 10));
        ImageTypeSpecifier ushortRGBPacked565Spec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_565_RGB);
        assertEquals(2 * 100, ImageReaderBase.computeByteSize(ushortRGBPacked565Spec, 10, 10));
        ImageTypeSpecifier ushortARGBPacked4444Spec = ImageTypeSpecifiers.createPacked(sRGB, 0xF00, 0xF0, 0xF, 0xF000, DataBuffer.TYPE_USHORT, false);
        assertEquals(2 * 100, ImageReaderBase.computeByteSize(ushortARGBPacked4444Spec, 10, 10));

        ImageTypeSpecifier intRGBPackedSpec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(intRGBPackedSpec, 10, 10));
        ImageTypeSpecifier intARGBPackedSpec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(intARGBPackedSpec, 10, 10));
        ImageTypeSpecifier intBGRPackedSpec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(intBGRPackedSpec, 10, 10));
        ImageTypeSpecifier intARGBPrePackedSpec = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(intARGBPrePackedSpec, 10, 10));

        ImageTypeSpecifier byteRGBInterleavedSpec = ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false);
        assertEquals(3 * 100, ImageReaderBase.computeByteSize(byteRGBInterleavedSpec, 10, 10));
        ImageTypeSpecifier byteRGBAInterleavedSpec = ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, false);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(byteRGBAInterleavedSpec, 10, 10));

        ImageTypeSpecifier byteRGBBandedSpec = ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[3], DataBuffer.TYPE_BYTE, false, false);
        assertEquals(3 * 100, ImageReaderBase.computeByteSize(byteRGBBandedSpec, 10, 10));
        ImageTypeSpecifier byteRGBABandedSpec = ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[4], DataBuffer.TYPE_BYTE, true, false);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(byteRGBABandedSpec, 10, 10));

        ImageTypeSpecifier ushortRGBInterleavedSpec = ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false);
        assertEquals(3 * 2 * 100, ImageReaderBase.computeByteSize(ushortRGBInterleavedSpec, 10, 10));
        ImageTypeSpecifier ushortRGBAInterleavedSpec = ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_USHORT, true, false);
        assertEquals(4 * 2 * 100, ImageReaderBase.computeByteSize(ushortRGBAInterleavedSpec, 10, 10));

        ImageTypeSpecifier ushortRGBBandedSpec = ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[3], DataBuffer.TYPE_USHORT, false, false);
        assertEquals(3 * 2 * 100, ImageReaderBase.computeByteSize(ushortRGBBandedSpec, 10, 10));
        ImageTypeSpecifier ushortRGBABandedSpec = ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 3}, new int[4], DataBuffer.TYPE_USHORT, true, false);
        assertEquals(4 * 2 * 100, ImageReaderBase.computeByteSize(ushortRGBABandedSpec, 10, 10));

        ImageTypeSpecifier intRGBInterleavedSpec = ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_INT, false, false);
        assertEquals(3 * 4 * 100, ImageReaderBase.computeByteSize(intRGBInterleavedSpec, 10, 10));
        ImageTypeSpecifier intRGBAInterleavedSpec = ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2, 4}, DataBuffer.TYPE_INT, true, false);
        assertEquals(4 * 4 * 100, ImageReaderBase.computeByteSize(intRGBAInterleavedSpec, 10, 10));

        ImageTypeSpecifier intRGBBandedSpec = ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2}, new int[3], DataBuffer.TYPE_INT, false, false);
        assertEquals(3 * 4 * 100, ImageReaderBase.computeByteSize(intRGBBandedSpec, 10, 10));
        ImageTypeSpecifier intRGBABandedSpec = ImageTypeSpecifiers.createBanded(sRGB, new int[] {0, 1, 2, 4}, new int[4], DataBuffer.TYPE_INT, true, false);
        assertEquals(4 * 4 * 100, ImageReaderBase.computeByteSize(intRGBABandedSpec, 10, 10));

        // Special case, SampleModel overflow
        assertEquals(3L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(byteRGBInterleavedSpec, Integer.MAX_VALUE, 10));
        assertEquals(4L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(byteRGBABandedSpec, 10, Integer.MAX_VALUE));
        assertEquals(3L * 2L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(ushortRGBBandedSpec, Integer.MAX_VALUE, 10));
        assertEquals(4L * 2L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(ushortRGBAInterleavedSpec, 10, Integer.MAX_VALUE));
        assertEquals(3L * 4L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(intRGBInterleavedSpec, Integer.MAX_VALUE, 10));
        assertEquals(4L * 4L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(intRGBABandedSpec, 10, Integer.MAX_VALUE));

        assertEquals(2L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(ushortARGBPacked4444Spec, Integer.MAX_VALUE, 10));
        assertEquals(4L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(intRGBPackedSpec, 10, Integer.MAX_VALUE));
        assertEquals(4L * 10L * Integer.MAX_VALUE, ImageReaderBase.computeByteSize(intARGBPackedSpec, Integer.MAX_VALUE, 10));
    }

    @Test
    void testComputeSizeCMYK() {
        ColorSpace cmyk = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);

        ImageTypeSpecifier byteCMYKInterleavedSpec = ImageTypeSpecifiers.createInterleaved(cmyk, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, false, false);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(byteCMYKInterleavedSpec, 10, 10));
        ImageTypeSpecifier byteCMYKAInterleavedSpec = ImageTypeSpecifiers.createInterleaved(cmyk, new int[] {0, 1, 2, 3, 4}, DataBuffer.TYPE_BYTE, true, false);
        assertEquals(5 * 100, ImageReaderBase.computeByteSize(byteCMYKAInterleavedSpec, 10, 10));

        ImageTypeSpecifier byteCMYKBandedSpec = ImageTypeSpecifiers.createBanded(cmyk, new int[] {0, 1, 2, 3}, new int[4], DataBuffer.TYPE_BYTE, false, false);
        assertEquals(4 * 100, ImageReaderBase.computeByteSize(byteCMYKBandedSpec, 10, 10));
        ImageTypeSpecifier byteCMYKABandedSpec = ImageTypeSpecifiers.createBanded(cmyk, new int[] {0, 1, 2, 3, 4}, new int[5], DataBuffer.TYPE_BYTE, true, false);
        assertEquals(5 * 100, ImageReaderBase.computeByteSize(byteCMYKABandedSpec, 10, 10));

        ImageTypeSpecifier ushortCMYKInterleavedSpec = ImageTypeSpecifiers.createInterleaved(cmyk, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_USHORT, false, false);
        assertEquals(4 * 2 * 100, ImageReaderBase.computeByteSize(ushortCMYKInterleavedSpec, 10, 10));
        ImageTypeSpecifier ushortCMYKAInterleavedSpec = ImageTypeSpecifiers.createInterleaved(cmyk, new int[] {0, 1, 2, 3, 5}, DataBuffer.TYPE_USHORT, true, false);
        assertEquals(5 * 2 * 100, ImageReaderBase.computeByteSize(ushortCMYKAInterleavedSpec, 10, 10));

        ImageTypeSpecifier ushortCMYKBandedSpec = ImageTypeSpecifiers.createBanded(cmyk, new int[] {0, 1, 2, 3}, new int[4], DataBuffer.TYPE_USHORT, false, false);
        assertEquals(4 * 2 * 100, ImageReaderBase.computeByteSize(ushortCMYKBandedSpec, 10, 10));
        ImageTypeSpecifier ushortCMYKABandedSpec = ImageTypeSpecifiers.createBanded(cmyk, new int[] {0, 1, 2, 3, 5}, new int[5], DataBuffer.TYPE_USHORT, true, false);
        assertEquals(5 * 2 * 100, ImageReaderBase.computeByteSize(ushortCMYKABandedSpec, 10, 10));

        ImageTypeSpecifier intCMYKInterleavedSpec = ImageTypeSpecifiers.createInterleaved(cmyk, new int[] {0, 1, 2, 3}, DataBuffer.TYPE_INT, false, false);
        assertEquals(4 * 4 * 100, ImageReaderBase.computeByteSize(intCMYKInterleavedSpec, 10, 10));
        ImageTypeSpecifier intCMYKAInterleavedSpec = ImageTypeSpecifiers.createInterleaved(cmyk, new int[] {0, 1, 2, 3, 4}, DataBuffer.TYPE_INT, true, false);
        assertEquals(5 * 4 * 100, ImageReaderBase.computeByteSize(intCMYKAInterleavedSpec, 10, 10));

        ImageTypeSpecifier intCMYKBandedSpec = ImageTypeSpecifiers.createBanded(cmyk, new int[] {0, 1, 2, 3}, new int[4], DataBuffer.TYPE_INT, false, false);
        assertEquals(4 * 4 * 100, ImageReaderBase.computeByteSize(intCMYKBandedSpec, 10, 10));
        ImageTypeSpecifier intCMYKABandedSpec = ImageTypeSpecifiers.createBanded(cmyk, new int[] {0, 1, 2, 3, 4}, new int[5], DataBuffer.TYPE_INT, true, false);
        assertEquals(5 * 4 * 100, ImageReaderBase.computeByteSize(intCMYKABandedSpec, 10, 10));

        // No need to test the overflow cases here, should be equivalent to the RGB specs
    }
}
