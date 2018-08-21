/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.cr2;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

//import com.twelvemonkeys.imageio.plugins.jpeg.LosslessJPEGDecoder;

/**
 * Canon CR2 RAW ImageReader.
 * <p/>
 * Acknowledgement:
 * This ImageReader is based on the excellent work of Laurent Clevy, and would probably not exist without it.
 *
 * @see <a href="http://lclevy.free.fr/dng/">Understanding What is stored in a Canon RAW .CR2 file, How and Why</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CR2ImageReader.java,v 1.0 07.04.14 21:31 haraldk Exp$
 */
public final class CR2ImageReader extends ImageReaderBase {
    // See http://lclevy.free.fr/dng/
    // TODO: Avoid duped code from TIFFImageReader
    // TODO: Probably a good idea to move some of the getAsShort/Int/Long/Array to TIFF/EXIF metadata module
    // TODO: Automatic EXIF rotation, if we find a good way to do that for JPEG/EXIF/TIFF and keeping the metadata sane...

    final static boolean DEBUG =  true; //"true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.dng.debug"));

    // Thumbnail is in IFD1 (2nd entry)
    private final static int THUMBNAIL_IFD = 1;

    private CompoundDirectory IFDs;
    private Directory currentIFD;

    CR2ImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        IFDs = null;
        currentIFD = null;
    }

    private void readMetadata() throws IOException {
        if (imageInput == null) {
            throw new IllegalStateException("input not set");
        }

        if (IFDs == null) {
            // We'll validate the TIFF structure later, for now just see if we have 'CR'0x0200 at the right place

            imageInput.skipBytes(8); // TIFF byte order mark + magic + IFD0 offset

            if (imageInput.readByte() != 'C' || imageInput.readByte() != 'R') {
                throw new IIOException("Not a valid CR2 structure: Missing CR magic ('CR')");
            }

            int version = imageInput.readUnsignedByte();
            int revision = imageInput.readUnsignedByte();

            // TODO: Choke on anything but 2.0? All sample data from 400D until 5D mk III has 2.0 version...

            imageInput.seek(0);

            IFDs = (CompoundDirectory) new TIFFReader().read(imageInput); // NOTE: Sets byte order as a side effect

            if (DEBUG) {
                System.err.println("Byte order: " + imageInput.getByteOrder());
                System.err.println("Version: " + version + "." + revision);
                System.err.println("Number of IFDs: " + IFDs.directoryCount());

                for (int i = 0; i < IFDs.directoryCount(); i++) {
                    System.err.printf("IFD %d: %s\n", i, IFDs.getDirectory(i));
                }
            }
        }
    }

    private void readIFD(final int ifdIndex) throws IOException {
        readMetadata();

        if (ifdIndex < 0) {
            throw new IndexOutOfBoundsException("index < minIndex");
        }
        else if (ifdIndex >= IFDs.directoryCount()) {
            throw new IndexOutOfBoundsException("index >= numImages (" + ifdIndex + " >= " + IFDs.directoryCount() + ")");
        }

        currentIFD = IFDs.getDirectory(ifdIndex);
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        readMetadata();

        // This validation is maybe a little too restrictive, but ok for now
        if (IFDs.directoryCount() != 4) {
            throw new IIOException("Unexpected number of IFDs in CR2: " + IFDs.directoryCount());
        }

        return IFDs.directoryCount() - 1;
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        return imageIndex == 0 ? 1 : 0;
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        // TODO: Need to get from JPEGImageReader (no ImageWidth tag), this is an ok (but lame) implementation for now
        return super.getThumbnailWidth(imageIndex, thumbnailIndex);
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        // TODO: Need to get from JPEGImageReader (no ImageHeight tag), this is an ok (but lame) implementation for now
        return super.getThumbnailHeight(imageIndex, thumbnailIndex);
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        readIFD(THUMBNAIL_IFD);

        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("No thumbnail for imageIndex: " + imageIndex);
        }
        if (thumbnailIndex != 0) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds: " + thumbnailIndex);
        }

        // This IFD (IFD1) lacks Compression 6 (old JPEG), but has the relevant tags for a JPEG/EXIF thumbnail
        int jpegOffset = getValueAsInt(TIFF.TAG_JPEG_INTERCHANGE_FORMAT, "JPEGInterchangeFormat");
        int jpegLength = getValueAsInt(TIFF.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, "JPEGInterchangeFormatLength");

        imageInput.seek(jpegOffset);

        // TODO: Consider using cached JPEGImageReader directly
        return ImageIO.read(new SubImageInputStream(imageInput, jpegLength));
    }

    private long[] getValueAsLongArray(final int tag, final String tagName, boolean required) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);
        if (entry == null) {
            if (required) {
                throw new IIOException("Missing TIFF tag " + tagName);
            }

            return null;
        }

        long[] value;

        if (entry.valueCount() == 1) {
            // For single entries, this will be a boxed type
            value = new long[] {((Number) entry.getValue()).longValue()};
        }
        else if (entry.getValue() instanceof short[]) {
            short[] shorts = (short[]) entry.getValue();
            value = new long[shorts.length];

            for (int i = 0, length = value.length; i < length; i++) {
                value[i] = shorts[i];
            }
        }
        else if (entry.getValue() instanceof int[]) {
            int[] ints = (int[]) entry.getValue();
            value = new long[ints.length];

            for (int i = 0, length = value.length; i < length; i++) {
                value[i] = ints[i];
            }
        }
        else if (entry.getValue() instanceof long[]) {
            value = (long[]) entry.getValue();
        }
        else {
            throw new IIOException(String.format("Unsupported %s type: %s (%s)", tagName, entry.getTypeName(), entry.getValue().getClass()));
        }

        return value;
    }

    private Number getValueAsNumberWithDefault(final int tag, final String tagName, final Number defaultValue) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);

        if (entry == null) {
            if (defaultValue != null)  {
                return defaultValue;
            }

            throw new IIOException("Missing TIFF tag: " + (tagName != null ? tagName : tag));
        }

        return (Number) entry.getValue();
    }

    private long getValueAsLongWithDefault(final int tag, final String tagName, final Long defaultValue) throws IIOException {
        return getValueAsNumberWithDefault(tag, tagName, defaultValue).longValue();
    }

    private long getValueAsLongWithDefault(final int tag, final Long defaultValue) throws IIOException {
        return getValueAsLongWithDefault(tag, null, defaultValue);
    }

    private int getValueAsIntWithDefault(final int tag, final String tagName, final Integer defaultValue) throws IIOException {
        return getValueAsNumberWithDefault(tag, tagName, defaultValue).intValue();
    }

    private int getValueAsIntWithDefault(final int tag, Integer defaultValue) throws IIOException {
        return getValueAsIntWithDefault(tag, null, defaultValue);
    }

    private int getValueAsInt(final int tag, String tagName) throws IIOException {
        return getValueAsIntWithDefault(tag, tagName, null);
    }

    private int imageIndexToIFDNumber(int imageIndex) {
        return imageIndex >= THUMBNAIL_IFD ? imageIndex + 1 : imageIndex;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        return getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        return getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        // TODO: For IFD0, get from JPEGImageReader delagate

//        return Arrays.asList(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR)).iterator();
        Entry bitsPerSample = currentIFD.getEntryById(TIFF.TAG_BITS_PER_SAMPLE);
        if (bitsPerSample == null) {
            // TODO: FixME!
            return Arrays.asList(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR)).iterator();
        }

        // For IFD1, create linear RGB, but we don't really know...
        int bitDepth = ((int[]) bitsPerSample.getValue())[0]; // Assume all equal!
        if (bitDepth == 8) {
            return Arrays.asList(ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), new int [] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false)).iterator();
        }
        else if (bitDepth == 16) {
            return Arrays.asList(ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), new int [] {0, 1, 2}, DataBuffer.TYPE_USHORT, false, false)).iterator();
        }

        throw new IIOException("Unsupported bit depth: " + bitDepth);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readIFD(imageIndexToIFDNumber(imageIndex));

        if (imageIndex == 0) {
            // This one says Compression 6 (old JPEG) and contains normal JPEG data at StripOffsets (but has no JPEGInterchangeFormat tag)
            int compression = getValueAsInt(TIFF.TAG_COMPRESSION, "Compression");
            if (compression != 6) {
                throw new IIOException("Unknown TIFF compression for CR2 IFD0: " + compression);
            }

            int stripOffsets = getValueAsInt(TIFF.TAG_STRIP_OFFSETS, "StripOffsets");
            int stripByteCounts = getValueAsInt(TIFF.TAG_STRIP_BYTE_COUNTS, "StripByteCounts");
            imageInput.seek(stripOffsets);

            List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(new SubImageInputStream(imageInput, stripByteCounts), JPEGSegmentUtil.ALL_SEGMENTS);
            System.err.println("segments: " + segments);

            imageInput.seek(stripOffsets);
            BufferedImage image = ImageIO.read(new SubImageInputStream(imageInput, stripByteCounts));
            System.err.println("image: " + image);
            return image;
        }

        if (imageIndex == 1) {
            // This one is semi-ok, for older cameras is says Compression 6 (old JPEG), for 7D it says Compression 1 (None)
            // We'll just ignore the compression and assume it's 1 (None).
            // TODO: Probably a good idea to verify that we have no other compression than 6/1
            // TODO: Consider just masking out this image, as it's not of much use...

            int width = getWidth(imageIndex);
            int height = getHeight(imageIndex);

            BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
            WritableRaster raster;

            int dataType = destination.getSampleModel().getDataType();
            if (dataType == DataBuffer.TYPE_BYTE) {
                // Emulate raw type (as dest, but RGB instead of BGR)
                raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, 1, width * 3, 3, new int[] {0, 1, 2}, null);
            }
            else if (dataType == DataBuffer.TYPE_USHORT) {
                // Emulate raw type (as dest, but RGB instead of BGR)
                raster = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, width, 1, width * 3, 3, new int[] {0, 1, 2}, null);
            }
            else {
                throw new AssertionError();
            }

            DataBuffer dataBuffer = raster.getDataBuffer();

            SampleModel destSampleModel = destination.getSampleModel();
            DataBuffer destBuffer = destination.getRaster().getDataBuffer();
            SampleModel srcSampleModel = raster.getSampleModel();

            if (destBuffer.getSize() != getValueAsInt(TIFF.TAG_STRIP_BYTE_COUNTS, "StripByteCounts")) {
                System.err.println("dataBuffer: " + dataBuffer.getSize());
                System.err.println("StripByteCounts: " + getValueAsInt(TIFF.TAG_STRIP_BYTE_COUNTS, "StripByteCounts"));
            }

            int stripOffsets = getValueAsInt(TIFF.TAG_STRIP_OFFSETS, "StripOffsets");
            imageInput.seek(stripOffsets);

            Object data = null;
            for (int y = 0; y < height; y++) {
                if (dataType == DataBuffer.TYPE_BYTE) {
                    imageInput.readFully(((DataBufferByte) dataBuffer).getData());
                }
                else {
                    imageInput.readFully(((DataBufferUShort) dataBuffer).getData(), 0, dataBuffer.getSize());
                }

                data = srcSampleModel.getDataElements(0, 0, width, 1, data, dataBuffer);
                destSampleModel.setDataElements(0, y, width, 1, data, destBuffer);
            }

            // TODO: This seems to work as crop values, if so correct w/h from getImageWidth/Height??
            Entry unknown = currentIFD.getEntryById(50908);
            if (unknown != null) {
                Graphics2D g = destination.createGraphics();
                try {
                    long[] values = (long[]) unknown.getValue();

                    g.setPaint(new Color(63, 223, 88, 128));
//                    g.drawRect((int) values[2], (int) values[3], (int) values[0], (int) values[1]);
//                    g.fillRect((int) values[2], (int) values[3], (int) values[0], (int) values[1]);
                    g.fillRect(0, 0, width, (int) values[3]);
                    g.fillRect(0, (int) values[3], (int) values[2], (int) (values[1] + values[3]));
                    g.fillRect((int) (values[2] + values[0]), (int) values[3], (int) values[2], (int) (values[1] + values[3]));
                    g.fillRect(0, (int) (values[3] + values[1]), width, height - (int) (values[3] + values[1]));
                }
                finally {
                    g.dispose();
                }
            }

            return destination;
        }

        if (imageIndex == 2) {
            // TODO: This is the real RAW data. It's supposed to be lossless JPEG encoded, single channel,
            // and has to be interpolated to become full 3 channel data from the Bayer CFA array,
            // then further processed with white balance correction, black subtraction and color scaling.
            // At least. ;-)

            // We should probably just mask this image out, until we can read it

            int stripOffsets = getValueAsInt(TIFF.TAG_STRIP_OFFSETS, "StripOffsets");
            int stripByteCounts = getValueAsInt(TIFF.TAG_STRIP_BYTE_COUNTS, "StripByteCounts");
            long[] slices = getValueAsLongArray(50752, "Slices", true);

            // Format of this array, is slices[0] = N, slices[1] = slice0.width ... slices[N + 1] = sliceN.width
            if (slices[0] != slices.length - 2) {
                throw new IIOException("Unexpected slices array: " + Arrays.toString(slices));
            }
            // TODO: We really have multiple slices...

            // TODO: Get correct dimensions (sensor size?)
            int width = getWidth(0);
            int height = getHeight(0);

            imageInput.seek(stripOffsets);
//            byte[] data = new LosslessJPEGDecoder().decompress(new SubImageInputStream(imageInput, stripByteCounts), null);
//
//            // TODO: We really have 2 bytes/sample
//            short[] data2 = new short[data.length / 2];
//            ByteBuffer wrap = ByteBuffer.wrap(data);
//            wrap.asShortBuffer().get(data2);
//
//            System.err.println("data.length: " + data2.length);
//            System.err.println("width x height: " + width * height);
//
//            DataBuffer dataBuffer = new DataBufferUShort(data2, data2.length);
//            WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width, 1, new int[] {0}, null);
//            ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, raster.getTransferType());
//            BufferedImage image = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
//
//            System.err.println("image: " + image);
//
//            return image;
        }

        return null;
    }

    public static void main(String[] args) throws IOException {
        CR2ImageReader reader = new CR2ImageReader(new CR2ImageReaderSpi());

        for (String arg : args) {
            ImageInputStream stream = ImageIO.createImageInputStream(new File(arg));
            reader.setInput(stream);

            int numImages = reader.getNumImages(true);
            for (int i = 0; i < numImages; i++) {
                int numThumbnails = reader.getNumThumbnails(i);
                for (int n = 0; n < numThumbnails; n++) {
                    showIt(reader.readThumbnail(i, n), arg + " image thumbnail" + n);
                }

                showIt(reader.read(i), arg + " image " + i);
            }
        }
    }
}
