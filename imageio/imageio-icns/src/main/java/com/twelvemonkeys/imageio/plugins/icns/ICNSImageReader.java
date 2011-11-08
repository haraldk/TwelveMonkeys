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
    private static final int HEADER_SIZE = 8;
    private List<IconHeader> icons = new ArrayList<IconHeader>();
    private List<IconHeader> masks = new ArrayList<IconHeader>();

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
        
        icons.clear();
        masks.clear();
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        return readIconHeader(imageIndex).size().width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        return readIconHeader(imageIndex).size().height;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        IconHeader header = readIconHeader(imageIndex);

        switch (header.depth()) {
            case 1:
                return IndexedImageTypeSpecifier.createFromIndexColorModel(ICNS1BitColorModel.INSTANCE);
            case 4:
                return IndexedImageTypeSpecifier.createFromIndexColorModel(ICNS4BitColorModel.INSTANCE);
            case 8:
                return IndexedImageTypeSpecifier.createFromIndexColorModel(ICNS8BitColorModel.INSTANCE);
            case 32:
                return ImageTypeSpecifier.createBanded(
                        ColorSpace.getInstance(ColorSpace.CS_sRGB),
                        new int[]{0, 1, 2, 3}, createBandOffsets(header.size().width * header.size().height),
                        DataBuffer.TYPE_BYTE, true, false
                );
            default:
                throw new IllegalStateException(String.format("Unknown bit depth: %d", header.depth()));
        }
    }

    private static int[] createBandOffsets(int bandLen) {
        return new int[]{0, bandLen, 2 * bandLen, 3 * bandLen};
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        IconHeader header = readIconHeader(imageIndex);

        List<ImageTypeSpecifier> specifiers = new ArrayList<ImageTypeSpecifier>();

        switch (header.depth()) {
            case 1:
            case 4:
            case 8:
                // Fall through & convert during read
            case 32:
                specifiers.add(ImageTypeSpecifier.createPacked(ColorSpace.getInstance(ColorSpace.CS_sRGB), 0xff0000, 0x00ff00, 0x0000ff, 0xff000000, DataBuffer.TYPE_INT, false));
                specifiers.add(ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{3, 2, 1, 0}, DataBuffer.TYPE_BYTE, true, false));
                break;
            default:
                throw new IllegalStateException(String.format("Unknown bit depth: %d", header.depth()));
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
                readIconHeader(num++);
            }
            catch (IndexOutOfBoundsException expected) {
                break;
            }
        }

        return icons.size();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        IconHeader header = readIconHeader(imageIndex);

        imageInput.seek(header.start + HEADER_SIZE);

        // Special handling of PNG/JPEG 2000 icons
        if (header.isForeignFormat()) {
            return readForeignFormat(param, header);
        }

        return readICNSFormat(imageIndex, param, header);
    }

    private BufferedImage readICNSFormat(final int imageIndex, final ImageReadParam param, final IconHeader header) throws IOException {
        Dimension size = header.size();

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
        if (header.isCompressed()) {
            // Only 32 bit icons may be compressed
            data = new byte[width * height * header.depth() / 8];

            int packedSize = header.length - HEADER_SIZE;

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
            data = new byte[header.length - HEADER_SIZE];
            imageInput.readFully(data);
        }

        if (header.depth() == 1) {
            // Binary
            DataBufferByte buffer = new DataBufferByte(data, data.length / 2, 0);
            WritableRaster raster = Raster.createPackedRaster(buffer, width, height, header.depth(), null);

            if (image.getType() == rawType.getBufferedImageType() && ((IndexColorModel) image.getColorModel()).getMapSize() == 2) {
                // Preserve raw data as read (binary), discard mask
                image.setData(raster);
            }
            else {
                // Convert to 32 bit ARGB
                DataBufferByte maskBuffer = new DataBufferByte(data, data.length / 2, data.length / 2);
                WritableRaster mask = Raster.createPackedRaster(maskBuffer, width, height, header.depth(), null);

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
        else if (header.depth() <= 8) {
            // Indexed
            DataBufferByte buffer = new DataBufferByte(data, data.length);
            WritableRaster raster = Raster.createPackedRaster(buffer, width, height, header.depth(), null);
            
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
                Raster mask = readMask(findMask(header));
                image.getAlphaRaster().setRect(mask);
            }
        }
        else {
            // 32 bit ARGB (true color)
            int bandLen = data.length / 4;

            DataBufferByte buffer = new DataBufferByte(data, data.length);
            WritableRaster raster = Raster.createBandedRaster(buffer, width, height, width, new int[]{0, 0, 0, 0}, new int[]{0, bandLen, bandLen * 2, bandLen * 3}, null);
            image.setData(raster);

            processImageProgress(75f);

            // Read mask and apply
            Raster mask = readMask(findMask(header));
            image.getAlphaRaster().setRect(mask);
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

    private Raster readMask(IconHeader header) throws IOException {
        Dimension size = header.size();

        int width = size.width;
        int height = size.height;

        byte[] alpha = new byte[header.length - HEADER_SIZE];

        imageInput.seek(header.start + HEADER_SIZE);
        imageInput.readFully(alpha);

        return Raster.createBandedRaster(new DataBufferByte(alpha, alpha.length), width, height, width, new int[]{0}, new int[]{0}, null);
    }

    private IconHeader findMask(final IconHeader icon) throws IOException {
        try {
            int i = 0;

            while (true) {
                IconHeader mask = i < masks.size() ? masks.get(i++) : readNextIconHeader();

                if (mask.isMask() && mask.size().equals(icon.size())) {
                    return mask;
                }
            }
        }
        catch (IndexOutOfBoundsException ignore) {
        }

        throw new IIOException(String.format("No mask for icon: %s", icon));
    }

    private BufferedImage readForeignFormat(final ImageReadParam param, final IconHeader header) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(IIOUtil.createStreamAdapter(imageInput, header.length));

        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(stream);

                try {
                    return reader.read(0, param);
                }
                catch (IOException ignore) {
                }
                finally {
                    stream.seek(0);
                }
            }

            // There's no JPEG 2000 reader installed in ImageIO by default (requires JAI ImageIO installed).
            // The current implementation is correct, but a bit harsh maybe..? Other options:
            // TODO: Return blank icon + issue warning? We know the image dimensions, we just can't read the data...
            // TODO: Pretend it's not in the stream + issue warning?
            // TODO: Create JPEG 2000 reader..? :-P
            throw new IIOException(String.format(
                    "Cannot read %s format in type '%s' icon (no reader; installed: %s)",
                    getForeignFormat(stream), ICNSUtil.intToStr(header.type), Arrays.toString(ImageIO.getReaderFormatNames())
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

    private IconHeader readIconHeader(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readeFileHeader();

        while (icons.size() <= imageIndex) {
            readNextIconHeader();
        }

        return icons.get(imageIndex);
    }

    private IconHeader readNextIconHeader() throws IOException {
        IconHeader lastIcon = icons.isEmpty() ? null : icons.get(icons.size() - 1);
        IconHeader lastMask = masks.isEmpty() ? null : masks.get(masks.size() - 1);

        long lastReadPos = Math.max(
                lastIcon == null ? HEADER_SIZE : lastIcon.start + lastIcon.length,
                lastMask == null ? HEADER_SIZE : lastMask.start + lastMask.length
        );

        imageInput.seek(lastReadPos);

        if (imageInput.getStreamPosition() >= length) {
            throw new IndexOutOfBoundsException();
        }

        IconHeader header = IconHeader.read(imageInput);

        // Filter out special case icnV (version?), as this isn't really an icon..
        if (header.isMask() || header.type == ICNS.icnV) {
            masks.add(header);
        }
        else {
            icons.add(header);
        }

        return header;
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

    // TODO: Rewrite using subclasses!
    static final class IconHeader {
        private final long start;
        private final int type;
        private final int length;

        IconHeader(long start, int type, int length) {
            validate(type, length);

            this.start = start;
            this.type = type;
            this.length = length;
        }

        public static IconHeader read(ImageInputStream input) throws IOException {
            return new IconHeader(input.getStreamPosition(), input.readInt(), input.readInt());
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
                    validateLengthForType(type, length, 24);
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
                    if (length > 0) {
                        break;
                    }
                    throw new IllegalArgumentException(String.format("Wrong combination of icon type '%s' and length: %d", ICNSUtil.intToStr(type), length));
                case ICNS.icnV:
                    validateLengthForType(type, length, 4);
                    break;
                default:
                    throw new IllegalStateException(String.format("Unknown icon type: '%s'", ICNSUtil.intToStr(type)));
            }

        }

        private void validateLengthForType(int type, int length, final int expectedLength) {
            Validate.isTrue(
                    length == expectedLength + HEADER_SIZE, // Compute to make lengths more logical
                    String.format(
                            "Wrong combination of icon type '%s' and length: %d (expected: %d)",
                            ICNSUtil.intToStr(type), length - HEADER_SIZE, expectedLength
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

        public boolean isMask() {
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
                    if (length != size.width * size.height) {
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
            return other == this || other != null && other.getClass() == getClass() && isEqual((IconHeader) other);
        }

        private boolean isEqual(IconHeader other) {
            return start == other.start && type == other.type && length == other.length;
        }

        @Override
        public String toString() {
            return String.format("%s['%s' start: %d, length: %d]", getClass().getSimpleName(), ICNSUtil.intToStr(type), start, length);
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

        File input = new File(args[argIndex++]);
        ImageReader reader = new ICNSImageReader();
        reader.setInput(ImageIO.createImageInputStream(input));

        int start = requested != -1 ? requested : 0;
        int numImages = requested != -1 ? requested + 1 : reader.getNumImages(true);
        for (int i = start; i < numImages; i++) {
            try {
                BufferedImage image = reader.read(i);
//                System.err.println("image: " + image);
                showIt(image, String.format("%s - %d", input.getName(), i));
            }
            catch (IIOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final class ICNSBitMaskColorModel extends IndexColorModel {
        static final IndexColorModel INSTANCE = new ICNSBitMaskColorModel();

        private ICNSBitMaskColorModel() {
            super(1, 2, new int[]{0, 0xffffffff}, 0, true, 0, DataBuffer.TYPE_BYTE);
        }
    }
}
