/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.IndexedImageTypeSpecifier;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.*;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * ICNSImageReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ICNSImageReader.java,v 1.0 25.10.11 18:42 haraldk Exp$
 *
 * @see <a href="http://www.macdisk.com/maciconen.php">Macintosh Icons</a>
 * @see <a href="http://en.wikipedia.org/wiki/Apple_Icon_Image_format">Apple Icon Image format (Wikipedia)</a>
 */
public final class ICNSImageReader extends ImageReaderBase {
    // TODO: Support ToC resource for faster parsing/faster determine number of icons?
    // TODO: Subsampled reading for completeness, even if never used?

    private static final int RESOURCE_HEADER_SIZE = 8;

    private List<IconResource> icons = new ArrayList<IconResource>();
    private List<IconResource> masks = new ArrayList<IconResource>();
    private IconResource lastResourceRead;

    private int length;

    public ICNSImageReader() {
        this(new ICNSImageReaderSpi());
    }

    ICNSImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        length = 0;

        lastResourceRead = null;
        icons.clear();
        masks.clear();
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        return readIconResource(imageIndex).size().width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        return readIconResource(imageIndex).size().height;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        IconResource resource = readIconResource(imageIndex);

        switch (resource.depth()) {
            case 1:
                return IndexedImageTypeSpecifier.createFromIndexColorModel(ICNS1BitColorModel.INSTANCE);
            case 4:
                return IndexedImageTypeSpecifier.createFromIndexColorModel(ICNS4BitColorModel.INSTANCE);
            case 8:
                return IndexedImageTypeSpecifier.createFromIndexColorModel(ICNS8BitColorModel.INSTANCE);
            case 32:
                if (resource.isCompressed()) {
                    return ImageTypeSpecifier.createBanded(
                            ColorSpace.getInstance(ColorSpace.CS_sRGB),
                            new int[]{0, 1, 2, 3}, createBandOffsets(resource.size().width * resource.size().height),
                            DataBuffer.TYPE_BYTE, true, false
                    );
                }
                else {
                    return ImageTypeSpecifier.createInterleaved(
                            ColorSpace.getInstance(ColorSpace.CS_sRGB),
                            new int[]{1, 2, 3, 0},
                            DataBuffer.TYPE_BYTE, true, false
                    );
                }
            default:
                throw new IllegalStateException(String.format("Unknown bit depth: %d", resource.depth()));
        }
    }

    private static int[] createBandOffsets(int bandLen) {
        return new int[]{0, bandLen, 2 * bandLen, 3 * bandLen};
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        IconResource resource = readIconResource(imageIndex);

        List<ImageTypeSpecifier> specifiers = new ArrayList<ImageTypeSpecifier>();

        switch (resource.depth()) {
            case 1:
            case 4:
            case 8:
                // Fall through & convert during read
            case 32:
                specifiers.add(ImageTypeSpecifier.createPacked(ColorSpace.getInstance(ColorSpace.CS_sRGB), 0xff0000, 0x00ff00, 0x0000ff, 0xff000000, DataBuffer.TYPE_INT, false));
                specifiers.add(ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{3, 2, 1, 0}, DataBuffer.TYPE_BYTE, true, false));
                break;
            default:
                throw new IllegalStateException(String.format("Unknown bit depth: %d", resource.depth()));
        }

        specifiers.add(rawType);

        return specifiers.iterator();
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        assertInput();

        if (!allowSearch) {
            // Return icons.size if we know we have read all?
            return -1;
        }

        int num = icons.size();
        while (true) {
            try {
                readIconResource(num++);
            }
            catch (IndexOutOfBoundsException expected) {
                break;
            }
        }

        return icons.size();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        IconResource resource = readIconResource(imageIndex);

        imageInput.seek(resource.start + RESOURCE_HEADER_SIZE);

        // Special handling of PNG/JPEG 2000 icons
        if (resource.isForeignFormat()) {
            return readForeignFormat(param, resource);
        }

        return readICNSFormat(imageIndex, param, resource);
    }

    private BufferedImage readICNSFormat(final int imageIndex, final ImageReadParam param, final IconResource resource) throws IOException {
        Dimension size = resource.size();

        int width = size.width;
        int height = size.height;

        BufferedImage image = getDestination(param, getImageTypes(imageIndex), width, height);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);

        if (rawType instanceof IndexedImageTypeSpecifier && rawType.getBufferedImageType() != image.getType()) {
            checkReadParamBandSettings(param, 4, image.getSampleModel().getNumBands());
        }
        else {
            checkReadParamBandSettings(param, rawType.getNumBands(), image.getSampleModel().getNumBands());
        }

        final Rectangle source = new Rectangle();
        final Rectangle dest = new Rectangle();
        computeRegions(param, width, height, image, source, dest);

        processImageStarted(imageIndex);

        // Read image data
        byte[] data;
        if (resource.isCompressed()) {
            // Only 32 bit icons may be compressed
            data = new byte[width * height * resource.depth() / 8];

            int packedSize = resource.length - RESOURCE_HEADER_SIZE;

            if (width >= 128 && height >= 128) {
                // http://www.macdisk.com/maciconen.php:
                // "In some icon sizes, there is a 32bit integer at the beginning of the run, whose role remains unknown."
                imageInput.skipBytes(4); // Seems to be 4 byte 0-pad
                packedSize -= 4;
            }

            InputStream input = IIOUtil.createStreamAdapter(imageInput, packedSize);

            try {
                decompress(new DataInputStream(input), data, 0, (data.length * 24) / 32); // 24 bit data
            }
            finally {
                input.close();
            }
        }
        else {
            data = new byte[resource.length - RESOURCE_HEADER_SIZE];
            imageInput.readFully(data);
        }

        if (resource.depth() == 1) {
            // Binary
            DataBufferByte buffer = new DataBufferByte(data, data.length / 2, 0);
            WritableRaster raster = Raster.createPackedRaster(buffer, width, height, resource.depth(), null);

            if (image.getType() == rawType.getBufferedImageType() && ((IndexColorModel) image.getColorModel()).getMapSize() == 2) {
                // Preserve raw data as read (binary), discard mask
                image.setData(raster);
            }
            else {
                // Convert to 32 bit ARGB
                DataBufferByte maskBuffer = new DataBufferByte(data, data.length / 2, data.length / 2);
                WritableRaster mask = Raster.createPackedRaster(maskBuffer, width, height, resource.depth(), null);

                Graphics2D graphics = image.createGraphics();

                try {
                    // Apply image data
                    BufferedImage temp = new BufferedImage(rawType.getColorModel(), raster, false, null);
                    graphics.drawImage(temp, 0, 0, null);

                    // Apply mask
                    temp = new BufferedImage(ICNSBitMaskColorModel.INSTANCE, mask, false, null);
                    temp.setData(mask);
                    graphics.setComposite(AlphaComposite.DstIn);
                    graphics.drawImage(temp, 0, 0, null);
                }
                finally {
                    graphics.dispose();
                }
            }
        }
        else if (resource.depth() <= 8) {
            // Indexed
            DataBufferByte buffer = new DataBufferByte(data, data.length);
            WritableRaster raster = Raster.createPackedRaster(buffer, width, height, resource.depth(), null);
            
            if (image.getType() == rawType.getBufferedImageType()) {
                // Preserve raw data as read (indexed), discard mask
                image.setData(raster);
            }
            else {
                // Convert to 32 bit ARGB
                Graphics2D graphics = image.createGraphics();

                try {
                    BufferedImage temp = new BufferedImage(rawType.getColorModel(), raster, false, null);
                    graphics.drawImage(temp, 0, 0, null);
                }
                finally {
                    graphics.dispose();
                }

                processImageProgress(50f);

                // Read mask and apply
                IconResource maskResource = findMaskResource(resource);

                if (maskResource != null) {
                    Raster mask = readMask(maskResource);
                    image.getAlphaRaster().setRect(mask);
                }
            }
        }
        else {
            // 32 bit ARGB (true color)
            int bandLen = data.length / 4;

            DataBufferByte buffer = new DataBufferByte(data, data.length);

            WritableRaster raster;

            if (resource.isCompressed()) {
                raster = Raster.createBandedRaster(buffer, width, height, width, new int[]{0, 0, 0, 0}, createBandOffsets(bandLen), null);
            }
            else {
                // NOTE: Uncompressed 32bit is interleaved RGBA, not banded...
                raster = Raster.createInterleavedRaster(buffer, width, height, width * 4, 4, new int[]{1, 2, 3, 0}, null);
            }

            image.setData(raster);

            processImageProgress(75f);

            // Read mask and apply
            IconResource maskResource = findMaskResource(resource);

            if (maskResource != null) {
                Raster mask = readMask(maskResource);
                image.getAlphaRaster().setRect(mask);
            }
            else {
                // TODO: This is simply stupid. Rewrite to use no alpha instead?
                byte[] solid = new byte[width * height];
                Arrays.fill(solid, (byte) -1);
                WritableRaster mask = Raster.createBandedRaster(new DataBufferByte(solid, solid.length), width, height, width, new int[]{0}, new int[]{0}, null);
                image.getAlphaRaster().setRect(mask);
            }
        }

        // For now: Make listener tests happy
        // TODO: Implement more sophisticated reading
        processImageProgress(100f);

        if (abortRequested()) {
            processReadAborted();
        }
        else {
            processImageComplete();
        }

        return image;
    }

    private Raster readMask(final IconResource resource) throws IOException {
        Dimension size = resource.size();

        int width = size.width;
        int height = size.height;

        byte[] mask = new byte[width * height];
        imageInput.seek(resource.start + RESOURCE_HEADER_SIZE);

        if (resource.isMaskType()) {
            // 8 bit mask
            imageInput.readFully(mask, 0, resource.length - RESOURCE_HEADER_SIZE);
        }
        else if (resource.hasMask()) {
            // Embedded 1bit mask
            byte[] maskData = new byte[(resource.length - RESOURCE_HEADER_SIZE) / 2];
            imageInput.skipBytes(maskData.length); // Skip the 1 bit image data
            imageInput.readFully(maskData);

            // Unpack 1bit mask to 8 bit
            int bitPos = 0x80;

            for (int i = 0, maskLength = mask.length; i < maskLength; i++) {
                mask[i] = (byte) ((maskData[i / 8] & bitPos) != 0 ? 0xff : 0x00);
                
                if ((bitPos >>= 1) == 0) {
                    bitPos = 0x80;
                }
            }
        }
        else {
            throw new IllegalArgumentException(String.format("Not a mask resource: %s", resource));
        }

        return Raster.createBandedRaster(new DataBufferByte(mask, mask.length), width, height, width, new int[]{0}, new int[]{0}, null);
    }

    private IconResource findMaskResource(final IconResource iconResource) throws IOException {
        // Find 8 bit mask
        try {
            int i = 0;

            while (true) {
                IconResource mask = i < masks.size() ? masks.get(i++) : readNextIconResource();

                if (mask.isMaskType() && mask.size().equals(iconResource.size())) {
                    return mask;
                }
            }
        }
        catch (IndexOutOfBoundsException ignore) {
        }

        // Fall back to mask from 1 bit resource if no 8 bit mask
        for (IconResource resource : icons) {
            if (resource.hasMask() && resource.size().equals(iconResource.size())) {
                return resource;
            }
        }

        return null;
    }

    private BufferedImage readForeignFormat(final ImageReadParam param, final IconResource resource) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(IIOUtil.createStreamAdapter(imageInput, resource.length));

        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(stream);

                try {
                    return reader.read(0, param);
                }
                catch (IOException ignore) {
                    if (stream.getFlushedPosition() <= 0) {
                        stream.seek(0);
                    }
                    else {
                        stream = ImageIO.createImageInputStream(IIOUtil.createStreamAdapter(imageInput, resource.length));
                    }
                }
            }

            // There's no JPEG 2000 reader installed in ImageIO by default (requires JAI ImageIO installed).
            // The current implementation is correct, but a bit harsh maybe..? Other options:
            // TODO: Return blank icon + issue warning? We know the image dimensions, we just can't read the data...
            // TODO: Pretend it's not in the stream + issue warning?
            // TODO: Create JPEG 2000 reader..? :-P
            throw new IIOException(String.format(
                    "Cannot read %s format in type '%s' icon (no reader; installed: %s)",
                    getForeignFormat(stream), ICNSUtil.intToStr(resource.type), Arrays.toString(ImageIO.getReaderFormatNames())
            ));
        }
        finally {
            stream.close();
        }
    }

    private String getForeignFormat(final ImageInputStream stream) throws IOException {
        byte[] magic = new byte[12]; // Length of JPEG 2000 magic
        try {
            stream.readFully(magic);
        }
        finally {
            stream.seek(0);
        }

        String format;
        if (Arrays.equals(ICNS.PNG_MAGIC, magic)) {
            format = "PNG";
        }
        else if (Arrays.equals(ICNS.JPEG_2000_MAGIC, magic)) {
            format = "JPEG 2000";
        }
        else {
            format = "unknown";
        }

        return format;
    }

    /*
     * http://www.macdisk.com/maciconen.php:
     * "For [...] (width * height of the icon), read a byte.
     * if bit 8 of the byte is set:
     *   This is a compressed run, for some value (next byte).
     *   The length is byte - 125. (*
     *   Put so many copies of the byte in the current color channel.
     * Else:
     *   This is an uncompressed run, whose values follow.
     *   The length is byte + 1.
     *   Read the bytes and put them in the current color channel."
     *
     *   *): With signed bytes, byte is always negative in this case, so it's actually -byte - 125,
     *       which is the same as byte + 131.
     */
    // NOTE: This is very close to PackBits (as described by the Wikipedia article), but it is not PackBits!
    static void decompress(final DataInputStream input, final byte[] result, int offset, int length) throws IOException {
        int resultPos = offset;
        int remaining = length;

        while (remaining > 0) {
            byte run = input.readByte();
            int runLength;

            if ((run & 0x80) != 0) {
                // Compressed run
                runLength = run + 131; // PackBits: -run + 1 and run == 0x80 is no-op... This allows 1 byte longer runs...

                byte runData = input.readByte();

                for (int i = 0; i < runLength; i++) {
                    result[resultPos++] = runData;
                }
            }
            else {
                // Uncompressed run
                runLength = run + 1;

                input.readFully(result, resultPos, runLength);
                resultPos += runLength;
            }

            remaining -= runLength;
        }
    }

    private IconResource readIconResource(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readeFileHeader();

        while (icons.size() <= imageIndex) {
            readNextIconResource();
        }

        return icons.get(imageIndex);
    }

    private IconResource readNextIconResource() throws IOException {
        long lastReadPos = lastResourceRead == null ? RESOURCE_HEADER_SIZE : lastResourceRead.start + lastResourceRead.length;

        imageInput.seek(lastReadPos);

        if (imageInput.getStreamPosition() >= length) {
            throw new IndexOutOfBoundsException();
        }

        IconResource resource = IconResource.read(imageInput);
//        System.err.println("resource: " + resource);

        lastResourceRead = resource;

        // Filter out special cases like 'icnV' or 'TOC ' resources
        if (resource.isMaskType()) {
            masks.add(resource);
        }
        else if (!resource.isUnknownType()) {
            icons.add(resource);
        }

        return resource;
    }

    private void readeFileHeader() throws IOException {
        assertInput();

        if (length <= 0) {
            imageInput.seek(0);

            if (imageInput.readInt() != ICNS.MAGIC) {
                throw new IIOException("Not an Apple Icon Image");
            }

            length = imageInput.readInt();
        }
    }

    // TODO: Rewrite using subclasses/instances!
    static final class IconResource {
        private final long start;
        private final int type;
        private final int length;

        IconResource(long start, int type, int length) {
            validate(type, length);

            this.start = start;
            this.type = type;
            this.length = length;
        }

        public static IconResource read(ImageInputStream input) throws IOException {
            return new IconResource(input.getStreamPosition(), input.readInt(), input.readInt());
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
                case ICNS.ic08:
                case ICNS.ic09:
                case ICNS.ic10:
                    if (length > RESOURCE_HEADER_SIZE) {
                        break;
                    }
                    throw new IllegalArgumentException(String.format("Wrong combination of icon type '%s' and length: %d", ICNSUtil.intToStr(type), length));
                case ICNS.icnV:
                    validateLengthForType(type, length, 4);
                    break;
                case ICNS.TOC_:
                default:
                    if (length > RESOURCE_HEADER_SIZE) {
                        break;
                    }
                    throw new IllegalStateException(String.format("Unknown icon type: '%s' length: %d", ICNSUtil.intToStr(type), length));
            }
        }

        private void validateLengthForType(int type, int length, final int expectedLength) {
            Validate.isTrue(
                    length == expectedLength + RESOURCE_HEADER_SIZE, // Compute to make lengths more logical
                    String.format(
                            "Wrong combination of icon type '%s' and length: %d (expected: %d)",
                            ICNSUtil.intToStr(type), length - RESOURCE_HEADER_SIZE, expectedLength
                    )
            );
        }

        public Dimension size() {
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
                    return new Dimension(16, 16);
                case ICNS.icl4:
                case ICNS.icl8:
                case ICNS.il32:
                case ICNS.l8mk:
                    return new Dimension(32, 32);
                case ICNS.ich_:
                case ICNS.ich4:
                case ICNS.ich8:
                case ICNS.ih32:
                case ICNS.h8mk:
                    return new Dimension(48, 48);
                case ICNS.it32:
                case ICNS.t8mk:
                    return new Dimension(128, 128);
                case ICNS.ic08:
                    return new Dimension(256, 256);
                case ICNS.ic09:
                    return new Dimension(512, 512);
                case ICNS.ic10:
                    return new Dimension(1024, 1024);
                default:
                    throw new IllegalStateException(String.format("Unknown icon type: '%s'", ICNSUtil.intToStr(type)));
            }
        }

        public int depth() {
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
                case ICNS.ic08:
                case ICNS.ic09:
                case ICNS.ic10:
                    return 32;
                default:
                    throw new IllegalStateException(String.format("Unknown icon type: '%s'", ICNSUtil.intToStr(type)));
            }
        }

        public boolean isUnknownType() {
            // These should simply be skipped
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
                case ICNS.ic08:
                case ICNS.ic09:
                case ICNS.ic10:
                    return false;
            }

            return true;
        }

        public boolean hasMask() {
            switch (type) {
                case ICNS.ICN_:
                case ICNS.icm_:
                case ICNS.ics_:
                case ICNS.ich_:
                    return true;
            }

            return false;
        }

        public boolean isMaskType() {
            switch (type) {
                case ICNS.s8mk:
                case ICNS.l8mk:
                case ICNS.h8mk:
                case ICNS.t8mk:
                    return true;
            }

            return false;
        }

        public boolean isCompressed() {
            switch (type) {
                case ICNS.is32:
                case ICNS.il32:
                case ICNS.ih32:
                case ICNS.it32:
                    // http://www.macdisk.com/maciconen.php
                    // "One should check whether the data length corresponds to the theoretical length (width * height)."
                    Dimension size = size();
                    if (length != (size.width * size.height * depth() / 8 + RESOURCE_HEADER_SIZE)) {
                        return true;
                    }
            }

            return false;
        }

        public boolean isForeignFormat() {
            switch (type) {
                case ICNS.ic08:
                case ICNS.ic09:
                case ICNS.ic10:
                    return true;
            }
            return false;
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
            return start == other.start && type == other.type && length == other.length;
        }

        @Override
        public String toString() {
            return String.format("%s['%s' start: %d, length: %d%s]", getClass().getSimpleName(), ICNSUtil.intToStr(type), start, length, isCompressed() ? " (compressed)" : "");
        }
    }

    @SuppressWarnings({"UnusedAssignment"})
    public static void main(String[] args) throws IOException {
        int argIndex = 0;

        int requested = -1;
        if (args[argIndex].charAt(0) == '-') {
            argIndex++;
            requested = Integer.parseInt(args[argIndex++]);
        }

        int imagesRead = 0;
        int imagesSkipped = 0;
        ImageReader reader = new ICNSImageReader();

        while(argIndex < args.length) {
            File input = new File(args[argIndex++]);
            ImageInputStream stream = ImageIO.createImageInputStream(input);

            if (stream == null) {
                System.err.printf("Cannot read: %s\n", input.getAbsolutePath());
                continue;
            }

            try {
                reader.setInput(stream);

                int start = requested != -1 ? requested : 0;
                int numImages = requested != -1 ? requested + 1 : reader.getNumImages(true);
                for (int i = start; i < numImages; i++) {
                    try {
                        BufferedImage image = reader.read(i);
                        imagesRead++;
//                        System.err.println("image: " + image);
                        showIt(image, String.format("%s - %d", input.getName(), i));
                    }
                    catch (IOException e) {
                        imagesSkipped++;
                        if (e.getMessage().contains("JPEG 2000")) {
//                            System.err.printf("%s: %s\n", input, e.getMessage());
                        }
                        else {
                            System.err.printf("%s: ", input);
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (Exception e) {
                System.err.printf("%s: ", input);
                e.printStackTrace();
            }
        }

        System.err.printf("Read %s images (%d skipped) in %d files\n", imagesRead, imagesSkipped, args.length);
    }

    private static final class ICNSBitMaskColorModel extends IndexColorModel {
        static final IndexColorModel INSTANCE = new ICNSBitMaskColorModel();

        private ICNSBitMaskColorModel() {
            super(1, 2, new int[]{0, 0xffffffff}, 0, true, 0, DataBuffer.TYPE_BYTE);
        }
    }
}
