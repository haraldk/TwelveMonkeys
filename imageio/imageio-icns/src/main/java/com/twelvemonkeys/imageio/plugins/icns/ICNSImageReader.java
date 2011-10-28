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
 */
public final class ICNSImageReader extends ImageReaderBase {
    // TODO: Merge masks with icon in front + calculate image count based on this...

    private static final int HEADER_SIZE = 8;
    private List<IconHeader> iconHeaders = new ArrayList<IconHeader>();
    private int length;

    public ICNSImageReader() {
        this(new ICNSImageReaderSpi());
    }

    ICNSImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
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
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        IconHeader header = readIconHeader(imageIndex);

        List<ImageTypeSpecifier> specifiers = new ArrayList<ImageTypeSpecifier>();

        switch (header.depth()) {
            case 1:
                specifiers.add(ImageTypeSpecifier.createGrayscale(1, DataBuffer.TYPE_BYTE, false));
                // Fall through
            case 4:
                specifiers.add(ImageTypeSpecifier.createGrayscale(4, DataBuffer.TYPE_BYTE, false));
                // Fall through
            case 8:
                specifiers.add(ImageTypeSpecifier.createGrayscale(8, DataBuffer.TYPE_BYTE, false));
                // Fall through
            case 24:
                specifiers.add(ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{0, 1, 2}, DataBuffer.TYPE_BYTE, false, false));
            case 32:
                specifiers.add(ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, false));
                break;
            default:
                throw new IllegalStateException(String.format("Unknown bit depth: %d", header.depth()));
        }

        return specifiers.iterator();
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        assertInput();

        if (!allowSearch) {
            return -1;
        }

        int num = iconHeaders.size();
        while (true) {
            try {
                readIconHeader(num);
                num++;
            }
            catch (IndexOutOfBoundsException expected) {
                break;
            }
        }

        return num;
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        IconHeader header = readIconHeader(imageIndex);
//        System.err.println("header: " + header);

        imageInput.seek(header.start + HEADER_SIZE);

        // TODO: Extract in separate method/class
        // Special handling of PNG/JPEG 2000 icons
        if (header.isForeignFormat()) {
            ImageInputStream stream = ImageIO.createImageInputStream(IIOUtil.createStreamAdapter(imageInput, header.length));
            try {
                //                       1  2  3  4  5  6  7  8  9  10 11 12  13 14 15 16 17 18 19 20 21 22 23
                // JPEG2000 magic bytes: 00 00 00 0C 6A 50 20 20 0D 0A 87 0A  00 00 00 14 66 74 79 70 6A 70 32
                //                       00 00 00 0C 6A 50 20 20 0D 0A 87 0A
                //                                12  j  P sp sp \r \n
                byte[] magic = new byte[12];
                stream.readFully(magic);
//                System.out.println("magic: " + Arrays.toString(magic));

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

                stream.seek(0);

                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

                while (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(stream);

                    try {
                        return reader.read(0, param);
                    }
                    catch (IOException ignore) {
                    }

                    stream.seek(0);
                }

                // TODO: There's no JPEG 2000 reader installed in ImageIO by default (requires JAI ImageIO installed)
                // TODO: Return blank icon? We know the image dimensions, we just can't read the data... Return blank image? Pretend it's not in the stream? ;-)
                // TODO: Create JPEG 2000 reader..? :-P
                throw new IIOException(String.format(
                        "Cannot read %s format in type '%s' icon (no reader; installed: %s)",
                        format, ICNSUtil.intToStr(header.type), Arrays.toString(ImageIO.getReaderFormatNames())
                ));
            }
            finally {
                stream.close();
            }
        }

        Dimension size = header.size();
        int width = size.width;
        int height = size.height;

        BufferedImage image = getDestination(param, getImageTypes(imageIndex), width, height);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        checkReadParamBandSettings(param, rawType.getNumBands(), image.getSampleModel().getNumBands());

        final Rectangle source = new Rectangle();
        final Rectangle dest = new Rectangle();
        computeRegions(param, width, height, image, source, dest);

        // Read image data
        byte[] data;
        if (header.isPackbits()) {
            data = new byte[width * height * header.depth() / 8];

            int packedSize = header.length - HEADER_SIZE;
            if (width >= 128 && height >= 128) {
                imageInput.skipBytes(4);
                packedSize -= 4;
            }

            InputStream input = IIOUtil.createStreamAdapter(imageInput, packedSize);
            unpackbits(new DataInputStream(input), data, 0, data.length);
            input.close();
        }
        else {
            data = new byte[header.length - HEADER_SIZE];
            imageInput.readFully(data);
        }

        switch (header.depth()) {
            case 1:
                break;
            case 4:
                break;
            case 8:
                break;
            case 24:
                break;
            default:
                throw new IllegalStateException(String.format("Unknown bit depth for icon: %d", header.depth()));
        }

        if (header.depth() <= 8) {
            DataBufferByte buffer = new DataBufferByte(data, data.length);
            image.setData(Raster.createPackedRaster(buffer, width, height, header.depth(), null));
        }
        else {
//            System.err.println("image: " + image);
//            DataBufferByte buffer = new DataBufferByte(data, data.length);
//            WritableRaster raster = Raster.createInterleavedRaster(buffer, width, height, width * header.depth() / 8, header.depth() / 8, new int[]{0, 1, 2}, null);
//            WritableRaster raster = Raster.createInterleavedRaster(buffer, width, height, width * header.depth() / 8, header.depth() / 8, new int[]{0, 1, 2, 3}, null);
//            int bandLen = data.length / 4;
//            DataBufferByte buffer = new DataBufferByte(data, data.length);
//            WritableRaster raster = Raster.createBandedRaster(buffer, width, height, width, new int[]{0, 0, 0, 0}, new int[]{0, bandLen, bandLen * 2, bandLen * 3}, null);
            int bandLen = data.length / 3;
            DataBufferByte buffer = new DataBufferByte(data, data.length);
            WritableRaster raster = Raster.createBandedRaster(buffer, width, height, width, new int[]{0, 0, 0}, new int[]{0, bandLen, bandLen * 2}, null);
            ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

            BufferedImage temp = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
//            showIt(temp, "foo");

//            image.setData(raster);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.drawImage(temp, 0, 0, null);
            }
            finally {
                graphics.dispose();
            }
        }

        return image;
    }

    // TODO: Is this really packbits?! Don't think so, but it's very close...
    static void unpackbits(final DataInputStream input, final byte[] result, int offset, int length) throws IOException {
        int resultPos = offset;
        int remaining = length;

        while (remaining > 0) {
            byte run = input.readByte();
            int runLength;

            if ((run & 0x80) != 0) {
                // Repeated run
                runLength = run + 131; // Packbits says: -run + 1 and 0x80 should be no-op... This inverts the lengths, but allows longer runs...

                byte runData = input.readByte();
                for (int i = 0; i < runLength; i++) {
                    result[resultPos++] = runData;
                }
            }
            else {
                // Literal run
                runLength = run + 1;

                input.readFully(result, resultPos, runLength);
                resultPos += runLength;
            }

            remaining -= runLength;
        }
    }

    private IconHeader readIconHeader(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readeFileHeader();

        if (iconHeaders.size() <= imageIndex) {
            int lastReadIndex = iconHeaders.size() - 1;
            IconHeader lastRead = iconHeaders.isEmpty() ? null : iconHeaders.get(lastReadIndex);

            for (int i = lastReadIndex; i < imageIndex; i++) {
                imageInput.seek(lastRead == null ? HEADER_SIZE : lastRead.start + lastRead.length);

                if (imageInput.getStreamPosition() >= length) {
                    throw new IndexOutOfBoundsException();
                }

                lastRead = IconHeader.read(imageInput);
                iconHeaders.add(lastRead);
            }
        }

        return iconHeaders.get(imageIndex);
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
                    if (length == 128) {
                        return;
                    }
                case ICNS.ICN_:
                    if (length == 256) {
                        return;
                    }
                case ICNS.icm_:
                    if (length == 24) {
                        return;
                    }
                case ICNS.icm4:
                    if (length == 96) {
                        return;
                    }
                case ICNS.icm8:
                    if (length == 192) {
                        return;
                    }
                case ICNS.ics_:
                    if (length == 32) {
                        return;
                    }
                case ICNS.ics4:
                    if (length == 128) {
                        return;
                    }
                case ICNS.ics8:
                case ICNS.s8mk:
                    if (length == 256) {
                            return;
                    }
                case ICNS.icl4:
                    if (length == 512) {
                            return;
                    }
                case ICNS.icl8:
                case ICNS.l8mk:
                    if (length == 1024) {
                            return;
                    }
                case ICNS.ich_:
                    if (length == 288) {
                            return;
                    }
                case ICNS.ich4:
                    if (length == 1152) {
                            return;
                    }
                case ICNS.ich8:
                case ICNS.h8mk:
                    if (length == 2034) {
                            return;
                    }
                case ICNS.t8mk:
                    if (length == 16384) {
                            return;
                    }
                case ICNS.ih32:
                case ICNS.is32:
                case ICNS.il32:
                case ICNS.it32:
                case ICNS.ic08:
                case ICNS.ic09:
                case ICNS.ic10:
                    if (length > 0) {
                        return;
                    }
                    throw new IllegalArgumentException(String.format("Wrong combination of icon type '%s' and length: %d", ICNSUtil.intToStr(type), length));
                default:
                    throw new IllegalStateException(String.format("Unknown icon type: '%s'", ICNSUtil.intToStr(type)));
            }
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
                case ICNS.ICN_: // Specical case? Wikipedi say 1 bit + 1 bit mask
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
                    return 24;
                default:
                    throw new IllegalStateException(String.format("Unknown icon type: '%s'", ICNSUtil.intToStr(type)));
            }
        }

        public boolean isPackbits() {
            switch (type) {
                case ICNS.ih32:
                case ICNS.il32:
                case ICNS.is32:
                case ICNS.it32:
                    return true;
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
                System.err.println("image: " + image);
                showIt(image, String.format("%s - %d", input.getName(), i));
            }
            catch (IIOException e) {
                e.printStackTrace();
            }
        }
    }
}
