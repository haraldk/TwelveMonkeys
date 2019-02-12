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

package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.CIELabColorConverter;
import com.twelvemonkeys.imageio.color.CIELabColorConverter.Illuminant;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.color.YCbCrConverter;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.iptc.IPTCReader;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.psd.PSD;
import com.twelvemonkeys.imageio.metadata.psd.PSDReader;
import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.metadata.xmp.XMPReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBitsDecoder;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.xml.XMLSerializer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGImageReadParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.CMMException;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static com.twelvemonkeys.imageio.util.IIOUtil.createStreamAdapter;
import static com.twelvemonkeys.imageio.util.IIOUtil.lookupProviderByName;
import static java.util.Arrays.asList;

/**
 * ImageReader implementation for Aldus/Adobe Tagged Image File Format (TIFF).
 * <p/>
 * The reader is supposed to be fully "Baseline TIFF" compliant, and supports the following image types:
 * <ul>
 *     <li>Class B (Bi-level), all relevant compression types, 1 bit per sample</li>
 *     <li>Class G (Gray), all relevant compression types, 2, 4, 8, 16 or 32 bits per sample, unsigned integer</li>
 *     <li>Class P (Palette/indexed color), all relevant compression types, 1, 2, 4, 8 or 16 bits per sample, unsigned integer</li>
 *     <li>Class R (RGB), all relevant compression types, 8 or 16 bits per sample, unsigned integer</li>
 * </ul>
 * In addition, it supports many common TIFF extensions such as:
 * <ul>
 *     <li>Tiling</li>
 *     <li>Class F (Facsimile), CCITT T.4 and T.6 compression (types 3 and 4), 1 bit per sample</li>
 *     <li>LZW Compression (type 5)</li>
 *     <li>"Old-style" JPEG Compression (type 6), as a best effort, as the spec is not well-defined</li>
 *     <li>JPEG Compression (type 7)</li>
 *     <li>ZLib (aka Adobe-style Deflate) Compression (type 8)</li>
 *     <li>Deflate Compression (type 32946)</li>
 *     <li>Horizontal differencing Predictor (type 2) for LZW, ZLib, Deflate and PackBits compression</li>
 *     <li>Alpha channel (ExtraSamples types 1/Associated Alpha and 2/Unassociated Alpha)</li>
 *     <li>Class S, CMYK data (PhotometricInterpretation type 5/Separated)</li>
 *     <li>Class Y, YCbCr data (PhotometricInterpretation type 6/YCbCr for both JPEG and other compressions</li>
 *     <li>Planar data (PlanarConfiguration type 2/Planar)</li>
 *     <li>ICC profiles (ICCProfile)</li>
 *     <li>BitsPerSample values up to 16 for most PhotometricInterpretations</li>
 *     <li>Multiple images (pages) in one file</li>
 * </ul>
 *
 * @see <a href="http://partners.adobe.com/public/developer/tiff/index.html">Adobe TIFF developer resources</a>
 * @see <a href="http://www.alternatiff.com/resources/TIFF6.pdf">TIFF 6.0 specification</a>
 * @see <a href="http://en.wikipedia.org/wiki/Tagged_Image_File_Format">Wikipedia TIFF</a>
 * @see <a href="http://www.awaresystems.be/imaging/tiff.html">AWare Systems TIFF pages</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TIFFImageReader.java,v 1.0 08.05.12 15:14 haraldk Exp$
 */
public final class TIFFImageReader extends ImageReaderBase {
    // TODOs ImageIO basic functionality:
    // TODO: Thumbnail support (what is a TIFF thumbnail anyway? Photoshop way? Or use subfiletype?)

    // TODOs ImageIO advanced functionality:
    // TODO: Tiling support (readTile, readTileRaster)
    // TODO: Implement readAsRenderedImage to allow tiled RenderedImage?
    //       For some layouts, we could do reads super-fast with a memory mapped buffer.
    // TODO: Implement readRaster directly (100% correctly)

    // TODOs Extension support
    // TODO: Auto-rotate based on Orientation
    // TODO: Support Compression 34712 (JPEG2000)? Depends on JPEG2000 ImageReader
    // TODO: Support Compression 34661 (JBIG)? Depends on JBIG ImageReader

    // DONE:
    // Handle SampleFormat
    // Support Compression 6 ('Old-style' JPEG)
    // Support Compression 2 (CCITT Modified Huffman RLE) for bi-level images
    // Source region
    // Subsampling
    // IIOMetadata (stay close to Sun's TIFF metadata)
    // http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html#ImageMetadata
    // Support ICCProfile
    // Support PlanarConfiguration 2
    // Support Compression 3 & 4 (CCITT T.4 & T.6)
    // Support ExtraSamples (an array, if multiple extra samples!)
    //       (0: Unspecified (not alpha), 1: Associated Alpha (pre-multiplied), 2: Unassociated Alpha (non-multiplied)

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.tiff.debug"));

    // NOTE: DO NOT MODIFY OR EXPOSE THIS ARRAY OUTSIDE PACKAGE!
    static final double[] CCIR_601_1_COEFFICIENTS = new double[] {299.0 / 1000.0, 587.0 / 1000.0, 114.0 / 1000.0};
    static final double[] REFERENCE_BLACK_WHITE_YCC_DEFAULT = new double[] {0, 255, 128, 255, 128, 255};

    private CompoundDirectory IFDs;
    private Directory currentIFD;

    TIFFImageReader(final ImageReaderSpi provider) {
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
            IFDs = (CompoundDirectory) new TIFFReader().read(imageInput); // NOTE: Sets byte order as a side effect

            if (DEBUG) {
                System.err.println("Byte order: " + imageInput.getByteOrder());
                System.err.println("Number of images: " + IFDs.directoryCount());

                for (int i = 0; i < IFDs.directoryCount(); i++) {
                    System.err.printf("IFD %d: %s\n", i, IFDs.getDirectory(i));
                }

                Entry tiffXMP = IFDs.getEntryById(TIFF.TAG_XMP);
                if (tiffXMP != null) {
                    byte[] value = (byte[]) tiffXMP.getValue();

                    // The XMPReader doesn't like null-termination...
                    int len = value.length;
                    for (int i = len - 1; i > 0; i--) {
                        if (value[i] == 0) {
                            len--;
                        }
                        else {
                            break;
                        }
                    }

                    Directory xmp = new XMPReader().read(new ByteArrayImageInputStream(value, 0, len));
                    System.err.println("-----------------------------------------------------------------------------");
                    System.err.println("xmp: " + xmp);
                }

                Entry tiffIPTC = IFDs.getEntryById(TIFF.TAG_IPTC);
                if (tiffIPTC != null) {
                    Object value = tiffIPTC.getValue();
                    if (value instanceof short[]) {
                        System.err.println("short[]: " + value);
                    }
                    if (value instanceof long[]) {
                        // As seen in a Magick produced image...
                        System.err.println("long[]: " + value);
                        long[] longs = (long[]) value;
                        value = new byte[longs.length * 8];
                        ByteBuffer.wrap((byte[]) value).asLongBuffer().put(longs);
                    }
                    if (value instanceof float[]) {
                        System.err.println("float[]: " + value);
                    }
                    if (value instanceof double[]) {
                        System.err.println("double[]: " + value);
                    }

                    Directory iptc = new IPTCReader().read(new ByteArrayImageInputStream((byte[]) value));
                    System.err.println("-----------------------------------------------------------------------------");
                    System.err.println("iptc: " + iptc);
                }

                Entry tiffPSD = IFDs.getEntryById(TIFF.TAG_PHOTOSHOP);
                if (tiffPSD != null) {
                    Directory psd = new PSDReader().read(new ByteArrayImageInputStream((byte[]) tiffPSD.getValue()));
                    System.err.println("-----------------------------------------------------------------------------");
                    System.err.println("psd: " + psd);
                }
                Entry tiffPSD2 = IFDs.getEntryById(TIFF.TAG_PHOTOSHOP_IMAGE_SOURCE_DATA);
                if (tiffPSD2 != null) {
                    byte[] value = (byte[]) tiffPSD2.getValue();
                    String foo = "Adobe Photoshop Document Data Block";

                    if (Arrays.equals(foo.getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(value, foo.length()))) {
                        System.err.println("foo: " + foo);
                        int offset = foo.length() + 1;
                        ImageInputStream input = new ByteArrayImageInputStream(value, offset, value.length - offset);
//                        input.setByteOrder(ByteOrder.LITTLE_ENDIAN); // TODO: WHY???!

                        while (input.getStreamPosition() < value.length - offset) {
                            int resourceId = input.readInt();
                            if (resourceId != PSD.RESOURCE_TYPE) {
                                System.err.println("Not a PSD resource: " + resourceId);
                                break;
                            }

                            int resourceKey = input.readInt();
                            System.err.println("resourceKey: " + intToStr(resourceKey));
                            long resourceLength = input.readUnsignedInt();
                            System.err.println("resourceLength: " + resourceLength);

                            long pad = (4 - (resourceLength % 4)) % 4;
                            long resourceLengthPadded = resourceLength + pad; // Padded to 32 bit boundary, possibly 64 bit for 8B64 resources
                            long streamPosition = input.getStreamPosition();

                            if (resourceKey == ('L' << 24 | 'a' << 16 | 'y' << 8 | 'r')) {
                                short count = input.readShort();
                                System.err.println("layer count: " + count);

                                for (int layer = 0; layer < count; layer++) {
                                    int top = input.readInt();
                                    int left = input.readInt();
                                    int bottom = input.readInt();
                                    int right = input.readInt();
                                    System.err.printf("%d, %d, %d, %d\n", top, left, bottom, right);

                                    short channels = input.readShort();
                                    System.err.println("channels: " + channels);

                                    for (int channel = 0; channel < channels; channel++) {
                                        short channelId = input.readShort();
                                        System.err.println("channelId: " + channelId);
                                        long channelLength = input.readUnsignedInt();
                                        System.err.println("channelLength: " + channelLength);
                                    }

                                    System.err.println("8BIM: " + intToStr(input.readInt()));
                                    int blendMode = input.readInt();
                                    System.err.println("blend mode key: " + intToStr(blendMode));

                                    int opacity = input.readUnsignedByte();
                                    System.err.println("opacity: " + opacity);
                                    int clipping = input.readUnsignedByte();
                                    System.err.println("clipping: " + clipping);
                                    byte flags = input.readByte();
                                    System.err.printf("flags: 0x%02x\n", flags);
                                    input.readByte(); // Pad

                                    long layerExtraDataLength = input.readUnsignedInt();
                                    long pos = input.getStreamPosition();
                                    System.err.println("length: " + layerExtraDataLength);

                                    long layerMaskSize = input.readUnsignedInt();
                                    input.skipBytes(layerMaskSize);
                                    long layerBlendingRangesSize = input.readUnsignedInt();
                                    input.skipBytes(layerBlendingRangesSize);

                                    String layerName = readPascalString(input);
                                    System.err.println("layerName: " + layerName);
                                    int mod = (layerName.length() + 1) % 4; // len + 1 for null-term
                                    System.err.println("mod: " + mod);
                                    if (mod != 0) {
                                        input.skipBytes(4 - mod);
                                    }
                                    System.err.println("input.getStreamPosition(): " + input.getStreamPosition());

                                    // TODO: More data here
                                    System.err.println(TIFFReader.HexDump.dump(0, value, (int) (offset + input.getStreamPosition()), 64));;

                                    input.seek(pos + layerExtraDataLength);
                                }


//                                long len = input.readUnsignedInt();
//                                System.err.println("len: " + len);
//
//                                int count = input.readUnsignedShort();
//                                System.err.println("count: " + count);

                                System.err.println(TIFFReader.HexDump.dump(0, value, (int) (offset + input.getStreamPosition()), 64));;

                            }
                            input.seek(streamPosition + resourceLengthPadded);
                            System.out.println("input.getStreamPosition(): " + input.getStreamPosition());
                        }

//                        Directory psd2 = new PSDReader().read(input);
//                        System.err.println("-----------------------------------------------------------------------------");
//                        System.err.println("psd2: " + psd2);
                    }
                }
            }
        }
    }

    static String readPascalString(final DataInput pInput) throws IOException {
        int length = pInput.readUnsignedByte();

        if (length == 0) {
            return "";
        }

        byte[] bytes = new byte[length];
        pInput.readFully(bytes);

        return StringUtil.decode(bytes, 0, bytes.length, "ASCII");
    }

    static String intToStr(int value) {
        return new String(
                new byte[]{
                        (byte) ((value & 0xff000000) >>> 24),
                        (byte) ((value & 0x00ff0000) >> 16),
                        (byte) ((value & 0x0000ff00) >> 8),
                        (byte) ((value & 0x000000ff))
                }
        );
    }

    private void readIFD(final int imageIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);
        currentIFD = IFDs.getDirectory(imageIndex);
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        readMetadata();

        return IFDs.directoryCount();
    }

    private Number getValueAsNumberWithDefault(final int tag, final String tagName, final Number defaultValue) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);

        if (entry == null) {
            if (defaultValue != null) {
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

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readIFD(imageIndex);

        return getValueAsInt(TIFF.TAG_IMAGE_WIDTH, "ImageWidth");
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readIFD(imageIndex);

        return getValueAsInt(TIFF.TAG_IMAGE_HEIGHT, "ImageHeight");
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        readIFD(imageIndex);

        int sampleFormat = getSampleFormat();
        int planarConfiguration = getValueAsIntWithDefault(TIFF.TAG_PLANAR_CONFIGURATION, TIFFBaseline.PLANARCONFIG_CHUNKY);
        int interpretation = getPhotometricInterpretationWithFallback();
        int samplesPerPixel = getValueAsIntWithDefault(TIFF.TAG_SAMPLES_PER_PIXEL, 1);
        int bitsPerSample = getBitsPerSample();
        int dataType = getDataType(sampleFormat, bitsPerSample);

        int opaqueSamplesPerPixel = getOpaqueSamplesPerPixel(interpretation);

        // Spec says ExtraSamples are mandatory for extra samples, however known encoders
        // (ie. SeaShore) writes ARGB TIFFs without ExtraSamples.
        long[] extraSamples = getValueAsLongArray(TIFF.TAG_EXTRA_SAMPLES, "ExtraSamples", false);
        if (extraSamples == null && samplesPerPixel > opaqueSamplesPerPixel) {
            // TODO: Log warning!
            // First extra is alpha, rest is "unspecified" (0)
            extraSamples = new long[samplesPerPixel - opaqueSamplesPerPixel];
            extraSamples[0] = TIFFBaseline.EXTRASAMPLE_UNASSOCIATED_ALPHA;
        }

        // Determine alpha
        boolean hasAlpha = extraSamples != null
                && (extraSamples[0] == TIFFBaseline.EXTRASAMPLE_ASSOCIATED_ALPHA
                || extraSamples[0] == TIFFBaseline.EXTRASAMPLE_UNASSOCIATED_ALPHA);
        boolean isAlphaPremultiplied = hasAlpha && extraSamples[0] == TIFFBaseline.EXTRASAMPLE_ASSOCIATED_ALPHA;
        int significantSamples = opaqueSamplesPerPixel + (hasAlpha ? 1 : 0);

        // Read embedded cs
        ICC_Profile profile = getICCProfile();
        ColorSpace cs;

        switch (interpretation) {
            // TIFF 6.0 baseline
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
                // WhiteIsZero
                // We need special case to preserve WhiteIsZero for CCITT 1 bit encodings
                // as some software will treat black/white runs as-is, regardless of photometric.
                // Special handling is also in the normalizeColor method
                if (significantSamples == 1 && bitsPerSample == 1) {
                    if (profile != null) {
                        processWarningOccurred("Ignoring embedded ICC color profile for Bi-level/Gray TIFF");
                    }

                    byte[] lut = new byte[] {-1, 0};
                    return ImageTypeSpecifier.createIndexed(lut, lut, lut, null, bitsPerSample, dataType);
                }

                // Otherwise, we'll handle this by inverting the values when reading

            case TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO:
                // BlackIsZero
                // Gray scale or B/W
                switch (significantSamples) {
                    case 1:
                        // TIFF 6.0 Spec says: 1, 4 or 8 for baseline (1 for bi-level, 4/8 for gray)
                        // ImageTypeSpecifier supports 1, 2, 4, 8 or 16 bits per sample, we'll support 32 bits as well.
                        // (Chunky or planar makes no difference for a single channel).
                        if (profile != null && profile.getColorSpaceType() != ColorSpace.TYPE_GRAY) {
                            processWarningOccurred(String.format("Embedded ICC color profile (type %s), is incompatible with image data (GRAY/type 6). Ignoring profile.", profile.getColorSpaceType()));
                            profile = null;
                        }

                        cs = profile == null ? ColorSpace.getInstance(ColorSpace.CS_GRAY) : ColorSpaces.createColorSpace(profile);

                        if (cs == ColorSpace.getInstance(ColorSpace.CS_GRAY) && (bitsPerSample == 1 || bitsPerSample == 2 || bitsPerSample == 4 || bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 32)) {
                            return ImageTypeSpecifiers.createGrayscale(bitsPerSample, dataType);
                        }
                        else if (bitsPerSample == 1 || bitsPerSample == 2 || bitsPerSample == 4 ) {
                            // Use packed format for 1/2/4 bits
                            return ImageTypeSpecifiers.createPackedGrayscale(cs, bitsPerSample, dataType);
                        }
                        else if (bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 32) {
                            return createImageTypeSpecifier(TIFFBaseline.PLANARCONFIG_CHUNKY, cs, dataType, significantSamples, samplesPerPixel, false, false);
                        }
                        else if (bitsPerSample % 2 == 0) {
                            ColorModel colorModel = new ComponentColorModel(cs, new int[] {bitsPerSample}, false, false, Transparency.OPAQUE, dataType);
                            return new ImageTypeSpecifier(colorModel, colorModel.createCompatibleSampleModel(1, 1));
                        }

                        throw new IIOException(String.format("Unsupported BitsPerSample for Bi-level/Gray TIFF (expected 1, 2, 4, 8, 16 or 32): %d", bitsPerSample));

                    case 2:
                        // Gray + alpha. We'll support:
                        // * 8, 16 or 32 bits per sample
                        // * Associated (pre-multiplied) or unassociated (non-pre-multiplied) alpha
                        // * Chunky (interleaved) or planar (banded) data
                        if (profile != null && profile.getColorSpaceType() != ColorSpace.TYPE_GRAY) {
                            processWarningOccurred(String.format("Embedded ICC color profile (type %s), is incompatible with image data (GRAY/type 6). Ignoring profile.", profile.getColorSpaceType()));
                            profile = null;
                        }

                        cs = profile == null ? ColorSpace.getInstance(ColorSpace.CS_GRAY) : ColorSpaces.createColorSpace(profile);

                        if (cs == ColorSpace.getInstance(ColorSpace.CS_GRAY) && (bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 32)) {
                            switch (planarConfiguration) {
                                case TIFFBaseline.PLANARCONFIG_CHUNKY:
                                    return ImageTypeSpecifiers.createGrayscale(bitsPerSample, dataType, isAlphaPremultiplied);
                                case TIFFExtension.PLANARCONFIG_PLANAR:
                                    return ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1}, new int[] {0, 0}, dataType, true, isAlphaPremultiplied);
                            }
                        }
                        else if (/*bitsPerSample == 1 || bitsPerSample == 2 || bitsPerSample == 4 ||*/ bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 32) {
                            // TODO: Should use packed format for 1/2/4 chunky.
                            // TODO: For 1/2/4 bit planar, we might need to fix while reading... Look at IFFImageReader?
                            return createImageTypeSpecifier(planarConfiguration, cs, dataType, significantSamples, samplesPerPixel, true, isAlphaPremultiplied);
                        }

                        throw new IIOException(String.format("Unsupported BitsPerSample for Gray + Alpha TIFF (expected 8, 16 or 32): %d", bitsPerSample));

                    default:
                        throw new IIOException(String.format("Unsupported SamplesPerPixel/BitsPerSample combination for Bi-level/Gray TIFF (expected 1/1, 1/2, 1/4, 1/8, 1/16 or 1/32, or 2/8, 2/16 or 2/32): %d/%d", samplesPerPixel, bitsPerSample));
                }

            case TIFFExtension.PHOTOMETRIC_YCBCR:
                // JPEG reader will handle YCbCr to RGB for us, otherwise we'll convert while reading
                // TODO: Sanity check that we have SamplesPerPixel == 3, BitsPerSample == [8,8,8] (or [16,16,16]) and Compression == 1 (none), 5 (LZW), or 6 (JPEG)
            case TIFFBaseline.PHOTOMETRIC_RGB:
                // RGB
                if (profile != null && profile.getColorSpaceType() != ColorSpace.TYPE_RGB) {
                    processWarningOccurred(String.format("Embedded ICC color profile (type %s), is incompatible with image data (RGB/type 5). Ignoring profile.", profile.getColorSpaceType()));
                    profile = null;
                }

                cs = profile == null ? ColorSpace.getInstance(ColorSpace.CS_sRGB) : ColorSpaces.createColorSpace(profile);

                switch (significantSamples) {
                    case 3:
                        if (bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 32) {
                            return createImageTypeSpecifier(planarConfiguration, cs, dataType, significantSamples, samplesPerPixel, false, false);
                        }
                        else if (bitsPerSample > 8 && bitsPerSample % 2 == 0) {
                            // TODO: Support variable bits/sample?
                            ColorModel colorModel = new ComponentColorModel(cs, new int[] {bitsPerSample, bitsPerSample, bitsPerSample}, false, false, Transparency.OPAQUE, dataType);
                            SampleModel sampleModel = planarConfiguration == TIFFBaseline.PLANARCONFIG_CHUNKY
                                                      ? colorModel.createCompatibleSampleModel(1, 1)
                                                      : new BandedSampleModel(dataType, 1, 1, 3, new int[]{0, 1, 2}, new int[]{0, 0, 0});
                            return new ImageTypeSpecifier(colorModel, sampleModel);
                        }
                    case 4:
                        if (bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 32) {
                            return createImageTypeSpecifier(planarConfiguration, cs, dataType, significantSamples, samplesPerPixel, true, isAlphaPremultiplied);
                        }
                        else if (significantSamples == 4 && bitsPerSample == 4) {
                            return ImageTypeSpecifiers.createPacked(cs, 0xF000, 0xF00, 0xF0, 0xF, DataBuffer.TYPE_USHORT, isAlphaPremultiplied);
                        }
                    default:
                        throw new IIOException(String.format("Unsupported SamplesPerPixel/BitsPerSample combination for RGB TIFF (expected 3/8, 4/8, 3/16 or 4/16): %d/%d", samplesPerPixel, bitsPerSample));
                }
            case TIFFBaseline.PHOTOMETRIC_PALETTE:
                // Palette
                if (samplesPerPixel != 1 && !(samplesPerPixel == 2 && extraSamples != null && extraSamples.length == 1)) {
                    throw new IIOException("Bad SamplesPerPixel value for Palette TIFF (expected 1): " + samplesPerPixel);
                }
                else if (bitsPerSample <= 0 || bitsPerSample > 16) {
                    throw new IIOException("Bad BitsPerSample value for Palette TIFF (expected <= 16): " + bitsPerSample);
                }

                // NOTE: If ExtraSamples is used, PlanarConfiguration must be taken into account also for pixel data
                Entry colorMap = currentIFD.getEntryById(TIFF.TAG_COLOR_MAP);
                if (colorMap == null) {
                    throw new IIOException("Missing ColorMap for Palette TIFF");
                }

                IndexColorModel icm = createIndexColorModel(bitsPerSample, dataType, (int[]) colorMap.getValue());

                if (extraSamples != null) {
                    return ImageTypeSpecifiers.createDiscreteExtraSamplesIndexedFromIndexColorModel(icm, extraSamples.length, hasAlpha);
                }

                return ImageTypeSpecifiers.createFromIndexColorModel(icm);

            case TIFFExtension.PHOTOMETRIC_SEPARATED:
                // Separated (CMYK etc)
                // Consult the 332/InkSet (1=CMYK, 2=Not CMYK; see InkNames), 334/NumberOfInks (def=4) and optionally 333/InkNames
                // If "Not CMYK" we'll need an ICC profile to be able to display (in a useful way), readAsRaster should still work.
                int inkSet = getValueAsIntWithDefault(TIFF.TAG_INK_SET, TIFFExtension.INKSET_CMYK);
                int numberOfInks = getValueAsIntWithDefault(TIFF.TAG_NUMBER_OF_INKS, 4);

                // Profile must be CMYK, OR color component must match NumberOfInks
                if (inkSet != TIFFExtension.INKSET_CMYK && (profile == null || profile.getNumComponents() != numberOfInks)) {
                    throw new IIOException(String.format(
                            "Embedded ICC color profile for Photometric Separated is missing or is incompatible with image data: %s != NumberOfInks (%s).",
                            profile != null ? profile.getNumComponents() : "null", numberOfInks));
                }
                if (profile != null && inkSet == TIFFExtension.INKSET_CMYK && profile.getColorSpaceType() != ColorSpace.TYPE_CMYK) {
                    processWarningOccurred(String.format("Embedded ICC color profile (type %s), is incompatible with image data (CMYK/type 9). Ignoring profile.", profile.getColorSpaceType()));
                    profile = null;
                }

                cs = profile == null ? ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK) : ColorSpaces.createColorSpace(profile);

                switch (significantSamples) {
                    case 4:
                    case 5:
                        if (bitsPerSample == 8 || bitsPerSample == 16) {
                            return createImageTypeSpecifier(planarConfiguration, cs, dataType, significantSamples, samplesPerPixel, significantSamples == 5, isAlphaPremultiplied);
                        }

                    default:
                        throw new IIOException(
                                String.format("Unsupported SamplesPerPixel/BitsPerSample combination for Separated TIFF (expected 4/8, 4/16, 5/8 or 5/16): %d/%s", samplesPerPixel, bitsPerSample)
                        );
                }
            case TIFFExtension.PHOTOMETRIC_CIELAB:
            case TIFFExtension.PHOTOMETRIC_ICCLAB:
            case TIFFExtension.PHOTOMETRIC_ITULAB:
                // TODO: Would probably be more correct to handle using a CIELabColorSpace for RAW type?
                // L*a*b* color. Handled using conversion to sRGB
                cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                switch (planarConfiguration) {
                    case TIFFBaseline.PLANARCONFIG_CHUNKY:
                        return createImageTypeSpecifier(TIFFBaseline.PLANARCONFIG_CHUNKY, cs, dataType, 3, samplesPerPixel, false, false);
                    case TIFFExtension.PLANARCONFIG_PLANAR:
                        // TODO: Reading works fine, but we can't convert the Lab values properly yet. Need to rewrite normalizeColor
                        //return ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2}, new int[] {0, 0, 0}, dataType, false, false);
                    default:
                        throw new IIOException(
                                String.format("Unsupported PlanarConfiguration for Lab color TIFF (expected 1): %d", planarConfiguration)
                        );
                }
            case TIFFBaseline.PHOTOMETRIC_MASK:
                // Transparency mask
                // TODO: Treat as grey?
            case TIFFCustom.PHOTOMETRIC_LOGL:
            case TIFFCustom.PHOTOMETRIC_LOGLUV:
                // Log
            case TIFFCustom.PHOTOMETRIC_CFA:
            case TIFFCustom.PHOTOMETRIC_LINEAR_RAW:
                // RAW (DNG)
                throw new IIOException("Unsupported TIFF PhotometricInterpretation value: " + interpretation);
            default:
                throw new IIOException("Unknown TIFF PhotometricInterpretation value: " + interpretation);
        }
    }

    private ImageTypeSpecifier createImageTypeSpecifier(int planarConfiguration, ColorSpace cs, int dataType, int significantSamples, int samplesPerPixel, boolean alpha, boolean alphaPremultiplied) throws IIOException {
        switch (planarConfiguration) {
            case TIFFBaseline.PLANARCONFIG_CHUNKY:
                if (samplesPerPixel > significantSamples) {
                    return new ImageTypeSpecifier(
                            new ExtraSamplesColorModel(cs, alpha, alphaPremultiplied, dataType, samplesPerPixel - significantSamples),
                            new PixelInterleavedSampleModel(dataType, 1, 1, samplesPerPixel, samplesPerPixel, createOffsets(samplesPerPixel)));
                }
                return ImageTypeSpecifiers.createInterleaved(cs, createOffsets(significantSamples), dataType, alpha, alphaPremultiplied);
            case TIFFExtension.PLANARCONFIG_PLANAR:
                return ImageTypeSpecifiers.createBanded(cs, createOffsets(significantSamples), new int[significantSamples], dataType, alpha, alphaPremultiplied);
            default:
                throw new IIOException(String.format("Unsupported PlanarConfiguration (expected 1 or 2): %d", planarConfiguration));
        }
    }

    private static int[] createOffsets(int samplesPerPixel) {
        int[] offsets = new int[samplesPerPixel];
        for (int i = 0; i < samplesPerPixel; i++) {
            offsets[i] = i;
        }
        return offsets;
    }

    private int getPhotometricInterpretationWithFallback() throws IIOException {
        // PhotometricInterpretation is a required TAG, but as it can be guessed this does a fallback that is equal to JAI ImageIO.
        int interpretation = getValueAsIntWithDefault(TIFF.TAG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation", -1);
        if (interpretation == -1) {
            int compression = getValueAsIntWithDefault(TIFF.TAG_COMPRESSION, TIFFBaseline.COMPRESSION_NONE);
            int samplesPerPixel = getValueAsIntWithDefault(TIFF.TAG_SAMPLES_PER_PIXEL, 1);
            Entry extraSamplesEntry = currentIFD.getEntryById(TIFF.TAG_EXTRA_SAMPLES);
            int extraSamples = extraSamplesEntry == null ? 0 : extraSamplesEntry.valueCount();

            if (compression == TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE
                    || compression == TIFFExtension.COMPRESSION_CCITT_T4
                    || compression == TIFFExtension.COMPRESSION_CCITT_T6) {
                interpretation = TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO;
            }
            else if (currentIFD.getEntryById(TIFF.TAG_COLOR_MAP) != null) {
                interpretation = TIFFBaseline.PHOTOMETRIC_PALETTE;
            }
            else if ((samplesPerPixel - extraSamples) == 3) {
                interpretation = TIFFBaseline.PHOTOMETRIC_RGB;
            }
            else if ((samplesPerPixel - extraSamples) == 4) {
                interpretation = TIFFExtension.PHOTOMETRIC_SEPARATED;
            }
            else {
                interpretation = TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO;
            }
            processWarningOccurred("Missing PhotometricInterpretation, determining fallback: " + interpretation);
        }
        return interpretation;
    }

    private int getOpaqueSamplesPerPixel(final int photometricInterpretation) throws IIOException {
        switch (photometricInterpretation) {
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
            case TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO:
            case TIFFBaseline.PHOTOMETRIC_PALETTE:
            case TIFFBaseline.PHOTOMETRIC_MASK:
                return 1;
            case TIFFBaseline.PHOTOMETRIC_RGB:
            case TIFFExtension.PHOTOMETRIC_YCBCR:
            case TIFFExtension.PHOTOMETRIC_CIELAB:
            case TIFFExtension.PHOTOMETRIC_ICCLAB:
            case TIFFExtension.PHOTOMETRIC_ITULAB:
                return 3;
            case TIFFExtension.PHOTOMETRIC_SEPARATED:
                return getValueAsIntWithDefault(TIFF.TAG_NUMBER_OF_INKS, 4);

            case TIFFCustom.PHOTOMETRIC_LOGL:
            case TIFFCustom.PHOTOMETRIC_LOGLUV:
            case TIFFCustom.PHOTOMETRIC_CFA:
            case TIFFCustom.PHOTOMETRIC_LINEAR_RAW:
                throw new IIOException("Unsupported TIFF PhotometricInterpretation value: " + photometricInterpretation);
            default:
                throw new IIOException("Unknown TIFF PhotometricInterpretation value: " + photometricInterpretation);
        }
    }

    private int getDataType(int sampleFormat, int bitsPerSample) throws IIOException {
        switch (sampleFormat) {
            case TIFFExtension.SAMPLEFORMAT_UNDEFINED:
                // Spec says:
                // A field value of “undefined” is a statement by the writer that it did not know how
                // to interpret the data samples; for example, if it were copying an existing image. A
                // reader would typically treat an image with “undefined” data as if the field were
                // not present (i.e. as unsigned integer data).
            case TIFFBaseline.SAMPLEFORMAT_UINT:
                return bitsPerSample <= 8 ? DataBuffer.TYPE_BYTE : bitsPerSample <= 16 ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_INT;
            case TIFFExtension.SAMPLEFORMAT_INT:
                switch (bitsPerSample) {
                    case 8:
                        return DataBuffer.TYPE_BYTE;
                    case 16:
                        return DataBuffer.TYPE_SHORT;
                    case 32:
                        return DataBuffer.TYPE_INT;
                }

                throw new IIOException("Unsupported BitsPerSample for SampleFormat 2/Signed Integer (expected 8/16/32): " + bitsPerSample);

            case TIFFExtension.SAMPLEFORMAT_FP:
                if (bitsPerSample == 32) {
                    return DataBuffer.TYPE_FLOAT;
                }

                throw new IIOException("Unsupported BitsPerSample for SampleFormat 3/Floating Point (expected 32): " + bitsPerSample);
            default:
                throw new IIOException("Unknown TIFF SampleFormat (expected 1, 2, 3 or 4): " + sampleFormat);
        }
    }

    private IndexColorModel createIndexColorModel(final int bitsPerSample, final int dataType, final int[] cmapShort) {
        // According to the spec, there should be exactly 3 * bitsPerSample^2 entries in the color map for TIFF.
        // Should we enforce this?

        int[] cmap = new int[cmapShort.length / 3];

        // We'll detect whether the color map data is 8 bit, rather than 16 bit while converting
        boolean cmapIs8Bit = true;

        // All reds, then greens, and finally blues
        for (int i = 0; i < cmap.length; i++) {
            cmap[i] = (cmapShort[i                  ] / 256) << 16
                    | (cmapShort[i +     cmap.length] / 256) << 8
                    | (cmapShort[i + 2 * cmap.length] / 256);

            if (cmapIs8Bit && cmap[i] != 0) {
                cmapIs8Bit = false;
            }
        }

        if (cmapIs8Bit) {
            // This color map is using only the lower 8 bits, making the image all black.
            // We'll create a new color map, based on the non-scaled 8 bit values.

            processWarningOccurred("8 bit ColorMap detected.");

            // All reds, then greens, and finally blues
            for (int i = 0; i < cmap.length; i++) {
                cmap[i] = (cmapShort[i                  ]) << 16
                        | (cmapShort[i +     cmap.length]) << 8
                        | (cmapShort[i + 2 * cmap.length]);
            }
        }

        return new IndexColorModel(bitsPerSample, cmap.length, cmap, 0, false, -1, dataType);
    }

    private int getSampleFormat() throws IIOException {
        long[] value = getValueAsLongArray(TIFF.TAG_SAMPLE_FORMAT, "SampleFormat", false);

        if (value != null) {
            long sampleFormat = value[0];

            for (int i = 1; i < value.length; i++) {
                if (value[i] != sampleFormat) {
                    throw new IIOException("Variable TIFF SampleFormat not supported: " + Arrays.toString(value));
                }
            }

            return (int) sampleFormat;
        }

        // The default
        return TIFFBaseline.SAMPLEFORMAT_UINT;
    }

    private int getBitsPerSample() throws IIOException {
        long[] value = getValueAsLongArray(TIFF.TAG_BITS_PER_SAMPLE, "BitsPerSample", false);

        if (value == null || value.length == 0) {
            return 1;
        }
        else {
            int bitsPerSample = (int) value[0];

            if (value.length == 3 && (value[0] == 5 && value[1] == 6 && value[2] == 5)) {
                // Special case for UINT_565. We're good.
            }
            else {
                for (int i = 1; i < value.length; i++) {
                    if (value[i] != bitsPerSample) {
                        throw new IIOException("Variable BitsPerSample not supported: " + Arrays.toString(value));
                    }
                }
            }

            return bitsPerSample;
        }
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readIFD(imageIndex);

        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        Set<ImageTypeSpecifier> specs = new LinkedHashSet<>(5);

        // TODO: Based on raw type, we can probably convert to most RGB types at least, maybe gray etc
        if (rawType.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB) {
            if (rawType.getNumBands() == 3 && rawType.getBitsPerBand(0) == 8) {
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
//                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));
//                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
            }
            else if (rawType.getNumBands() == 4 && rawType.getBitsPerBand(0) == 8) {
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR));
//                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
                specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE));
            }
        }

        specs.add(rawType);

        return specs.iterator();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readIFD(imageIndex);

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        checkReadParamBandSettings(param, rawType.getNumBands(), destination.getSampleModel().getNumBands());

        final Rectangle srcRegion = new Rectangle();
        final Rectangle dstRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, dstRegion);

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        WritableRaster destRaster = clipToRect(destination.getRaster(), dstRegion, param != null ? param.getDestinationBands() : null);

        final int interpretation = getPhotometricInterpretationWithFallback();
        final int compression = getValueAsIntWithDefault(TIFF.TAG_COMPRESSION, TIFFBaseline.COMPRESSION_NONE);
        final int predictor = getValueAsIntWithDefault(TIFF.TAG_PREDICTOR, 1);
        final int planarConfiguration = getValueAsIntWithDefault(TIFF.TAG_PLANAR_CONFIGURATION, TIFFBaseline.PLANARCONFIG_CHUNKY);
        final int numBands = planarConfiguration == TIFFExtension.PLANARCONFIG_PLANAR ? 1 : rawType.getNumBands();

        // NOTE: We handle strips as tiles of tileWidth == width by tileHeight == rowsPerStrip
        //       Strips are top/down, tiles are left/right, top/down
        int stripTileWidth = width;
        long rowsPerStrip = getValueAsLongWithDefault(TIFF.TAG_ROWS_PER_STRIP, (long) Integer.MAX_VALUE);
        int stripTileHeight = rowsPerStrip < height ? (int) rowsPerStrip : height;

        long[] stripTileOffsets = getValueAsLongArray(TIFF.TAG_TILE_OFFSETS, "TileOffsets", false);
        long[] stripTileByteCounts;

        if (stripTileOffsets != null) {
            stripTileByteCounts = getValueAsLongArray(TIFF.TAG_TILE_BYTE_COUNTS, "TileByteCounts", false);
            if (stripTileByteCounts == null) {
                processWarningOccurred("Missing TileByteCounts for tiled TIFF with compression: " + compression);
            }
            else if (stripTileByteCounts.length == 0 || containsZero(stripTileByteCounts)) {
                stripTileByteCounts = null;
                processWarningOccurred("Ignoring all-zero TileByteCounts for tiled TIFF with compression: " + compression);
            }

            stripTileWidth = getValueAsInt(TIFF.TAG_TILE_WIDTH, "TileWidth");
            stripTileHeight = getValueAsInt(TIFF.TAG_TILE_HEIGTH, "TileHeight");
        }
        else {
            stripTileOffsets = getValueAsLongArray(TIFF.TAG_STRIP_OFFSETS, "StripOffsets", true);
            stripTileByteCounts = getValueAsLongArray(TIFF.TAG_STRIP_BYTE_COUNTS, "StripByteCounts", false);
            if (stripTileByteCounts == null) {
                processWarningOccurred("Missing StripByteCounts for TIFF with compression: " + compression);
            }
            else if (stripTileByteCounts.length == 0 || containsZero(stripTileByteCounts)) {
                stripTileByteCounts = null;
                processWarningOccurred("Ignoring all-zero StripByteCounts for TIFF with compression: " + compression);
            }

            // NOTE: This is really against the spec, but libTiff seems to handle it. TIFF 6.0 says:
            //       "Do not use both strip- oriented and tile-oriented fields in the same TIFF file".
            stripTileWidth = getValueAsIntWithDefault(TIFF.TAG_TILE_WIDTH, "TileWidth", stripTileWidth);
            stripTileHeight = getValueAsIntWithDefault(TIFF.TAG_TILE_HEIGTH, "TileHeight", stripTileHeight);
        }

        int tilesAcross = (width + stripTileWidth - 1) / stripTileWidth;
        int tilesDown = (height + stripTileHeight - 1) / stripTileHeight;

        // TODO: Get number of extra samples not part of the rawType spec...
        // TODO: If extrasamples, we might need to create a raster with more samples...
        WritableRaster rowRaster = rawType.createBufferedImage(stripTileWidth, 1).getRaster();
//        WritableRaster rowRaster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, stripTileWidth, 1, 2, null).createWritableChild(0, 0, stripTileWidth, 1, 0, 0, new int[]{0});
        Rectangle clip = new Rectangle(srcRegion);
        int srcRow = 0;
        Boolean needsCSConversion = null;

        switch (compression) {
            case TIFFBaseline.COMPRESSION_NONE:
                // No compression
            case TIFFBaseline.COMPRESSION_PACKBITS:
                // PackBits
            case TIFFExtension.COMPRESSION_LZW:
                // LZW
            case TIFFExtension.COMPRESSION_ZLIB:
                // 'Adobe-style' Deflate
            case TIFFExtension.COMPRESSION_DEFLATE:
                // 'PKZIP-style' Deflate
            case TIFFCustom.COMPRESSION_PIXTIFF_ZIP:
                // PIXTIFF proprietary 'ZIP' compression, same as Deflate
            case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
                // CCITT modified Huffman
            case TIFFExtension.COMPRESSION_CCITT_T4:
                // CCITT Group 3 fax encoding
            case TIFFExtension.COMPRESSION_CCITT_T6:
                // CCITT Group 4 fax encoding

                int[] yCbCrSubsampling = null;
                int yCbCrPos = 1;

                if (interpretation == TIFFExtension.PHOTOMETRIC_YCBCR) {
                    // getRawImageType does the lookup/conversion for these
                    if (rowRaster.getNumBands() != 3) {
                        throw new IIOException("TIFF PhotometricInterpretation YCbCr requires SamplesPerPixel == 3: " + rowRaster.getNumBands());
                    }
                    if (rowRaster.getTransferType() != DataBuffer.TYPE_BYTE  && rowRaster.getTransferType() != DataBuffer.TYPE_USHORT) {
                        throw new IIOException("TIFF PhotometricInterpretation YCbCr requires BitsPerSample == [8,8,8] or [16,16,16]");
                    }

                    yCbCrPos = getValueAsIntWithDefault(TIFF.TAG_YCBCR_POSITIONING, TIFFExtension.YCBCR_POSITIONING_CENTERED);
                    if (yCbCrPos != TIFFExtension.YCBCR_POSITIONING_CENTERED && yCbCrPos != TIFFExtension.YCBCR_POSITIONING_COSITED) {
                        processWarningOccurred("Uknown TIFF YCbCrPositioning value, expected 1 or 2: " + yCbCrPos);
                    }

                    Entry subSampling = currentIFD.getEntryById(TIFF.TAG_YCBCR_SUB_SAMPLING);

                    if (subSampling != null) {
                        try {
                            yCbCrSubsampling = (int[]) subSampling.getValue();
                        }
                        catch (ClassCastException e) {
                            throw new IIOException("Unknown TIFF YCbCrSubSampling value type: " + subSampling.getTypeName(), e);
                        }

                        if (yCbCrSubsampling.length != 2 ||
                                yCbCrSubsampling[0] != 1 && yCbCrSubsampling[0] != 2 && yCbCrSubsampling[0] != 4 ||
                                yCbCrSubsampling[1] != 1 && yCbCrSubsampling[1] != 2 && yCbCrSubsampling[1] != 4) {
                            throw new IIOException("Bad TIFF YCbCrSubSampling value: " + Arrays.toString(yCbCrSubsampling));
                        }

                        if (yCbCrSubsampling[0] < yCbCrSubsampling[1]) {
                            processWarningOccurred("TIFF PhotometricInterpretation YCbCr with bad subsampling, expected subHoriz >= subVert: " + Arrays.toString(yCbCrSubsampling));
                        }
                    }
                    else {
                        yCbCrSubsampling = new int[] {2, 2};
                    }
                }

                // Read data
                processImageStarted(imageIndex);

                // General uncompressed/compressed reading
                int bands = planarConfiguration == TIFFExtension.PLANARCONFIG_PLANAR ? rawType.getNumBands() : 1;
                int bitsPerSample = getBitsPerSample();
                boolean needsBitPadding = bitsPerSample > 16 && bitsPerSample % 16 != 0 || bitsPerSample > 8 && bitsPerSample % 8 != 0 || bitsPerSample == 6;
                boolean needsAdapter = compression != TIFFBaseline.COMPRESSION_NONE
                        || interpretation == TIFFExtension.PHOTOMETRIC_YCBCR || needsBitPadding;

                for (int y = 0; y < tilesDown; y++) {
                    int col = 0;
                    int rowsInTile = Math.min(stripTileHeight, height - srcRow);

                    for (int x = 0; x < tilesAcross; x++) {
                        int colsInTile = Math.min(stripTileWidth, width - col);

                        for (int b = 0; b < bands; b++) {
                            int i = b * tilesDown * tilesAcross + y * tilesAcross + x;

                            imageInput.seek(stripTileOffsets[i]);

                            DataInput input;
                            if (!needsAdapter) {
                                // No need for transformation, fast forward
                                input = imageInput;
                            }
                            else {
                                InputStream adapter = stripTileByteCounts != null
                                                      ? createStreamAdapter(imageInput, stripTileByteCounts[i])
                                                      : createStreamAdapter(imageInput);

                                adapter = createDecompressorStream(compression, stripTileWidth, numBands, adapter);
                                adapter = createUnpredictorStream(predictor, stripTileWidth, numBands, bitsPerSample, adapter, imageInput.getByteOrder());

                                if (interpretation == TIFFExtension.PHOTOMETRIC_YCBCR && rowRaster.getTransferType() == DataBuffer.TYPE_BYTE) {
                                    adapter = new YCbCrUpsamplerStream(adapter, yCbCrSubsampling, yCbCrPos, colsInTile);
                                }
                                else if (interpretation == TIFFExtension.PHOTOMETRIC_YCBCR && rowRaster.getTransferType() == DataBuffer.TYPE_USHORT) {
                                    adapter = new YCbCr16UpsamplerStream(adapter, yCbCrSubsampling, yCbCrPos, colsInTile, imageInput.getByteOrder());
                                }
                                else if (interpretation == TIFFExtension.PHOTOMETRIC_YCBCR) {
                                    // Handled in getRawImageType
                                    throw new AssertionError();
                                }

                                if (needsBitPadding) {
                                    // We'll pad "odd" bitsPerSample streams to the smallest data type (byte/short/int) larger than the input
                                    adapter = new BitPaddingStream(adapter, numBands, bitsPerSample, colsInTile, imageInput.getByteOrder());
                                }

                                // According to the spec, short/long/etc should follow order of containing stream
                                input = imageInput.getByteOrder() == ByteOrder.BIG_ENDIAN
                                        ? new DataInputStream(adapter)
                                        : new LittleEndianDataInputStream(adapter);
                            }

                            // Clip the stripTile rowRaster to not exceed the srcRegion
                            clip.width = Math.min(colsInTile, srcRegion.width);
                            Raster clippedRow = clipRowToRect(rowRaster, clip,
                                    param != null ? param.getSourceBands() : null,
                                    param != null ? param.getSourceXSubsampling() : 1);

                            // Read a full strip/tile
                            readStripTileData(clippedRow, srcRegion, xSub, ySub, b, numBands, interpretation, destRaster, col, srcRow, colsInTile, rowsInTile, input);
                        }

                        col += colsInTile;

                        if (abortRequested()) {
                            break;
                        }
                    }

                    srcRow += rowsInTile;
                    processImageProgress(100f * srcRow / height);

                    if (abortRequested()) {
                        processReadAborted();
                        break;
                    }
                }


                break;

            case TIFFExtension.COMPRESSION_JPEG:
                // JPEG ('new-style' JPEG)
                // TODO: Refactor all JPEG reading out to separate JPEG support class?
                // TODO: Cache the JPEG reader for later use? Remember to reset to avoid resource leaks

                ImageReader jpegReader = createJPEGDelegate();
                JPEGImageReadParam jpegParam = (JPEGImageReadParam) jpegReader.getDefaultReadParam();

                // JPEG_TABLES should be a full JPEG 'abbreviated table specification', containing:
                // SOI, DQT, DHT, (optional markers that we ignore)..., EOI
                Entry tablesEntry = currentIFD.getEntryById(TIFF.TAG_JPEG_TABLES);
                byte[] tablesValue = tablesEntry != null ? (byte[]) tablesEntry.getValue() : null;
                if (tablesValue != null) {
                    // Whatever values I pass the reader as the read param, it never gets the same quality as if
                    // I just invoke jpegReader.getStreamMetadata(), so we'll do that...
                    jpegReader.setInput(new ByteArrayImageInputStream(tablesValue));

                    // This initializes the tables and other internal settings for the reader,
                    // and is actually a feature of JPEG, see abbreviated streams:
                    // http://docs.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html#abbrev
                    jpegReader.getStreamMetadata();
                }
                else if (tilesDown * tilesAcross > 1) {
                    processWarningOccurred("Missing JPEGTables for tiled/striped TIFF with compression: 7 (JPEG)");
                    // ...and the JPEG reader will probably choke on missing tables...
                }

                // Read data
                processImageStarted(imageIndex); // Better yet, would be to delegate read progress here...

                for (int y = 0; y < tilesDown; y++) {
                    int col = 0;
                    int rowsInTile = Math.min(stripTileHeight, height - srcRow);

                    for (int x = 0; x < tilesAcross; x++) {
                        int i = y * tilesAcross + x;
                        int colsInTile = Math.min(stripTileWidth, width - col);

                        // Read only tiles that lies within region
                        Rectangle tileRect = new Rectangle(col, srcRow, colsInTile, rowsInTile);
                        Rectangle intersection = tileRect.intersection(srcRegion);
                        if (!intersection.isEmpty()) {
                            imageInput.seek(stripTileOffsets[i]);

                            int length = stripTileByteCounts != null ? (int) stripTileByteCounts[i] : Short.MAX_VALUE;

                            try (ImageInputStream subStream = new SubImageInputStream(imageInput, length)) {
                                jpegReader.setInput(subStream);
                                jpegParam.setSourceRegion(new Rectangle(intersection.x - col, intersection.y - srcRow, intersection.width, intersection.height));
                                jpegParam.setSourceSubsampling(xSub, ySub, 0, 0);
                                Point offset = new Point((intersection.x - srcRegion.x) / xSub, (intersection.y - srcRegion.y) / ySub);

                                // TODO: If we have non-standard reference B/W or yCbCr coefficients,
                                // we might still have to do extra color space conversion...
                                if (needsCSConversion == null) {
                                    needsCSConversion = needsCSConversion(compression, interpretation, readJPEGMetadataSafe(jpegReader));
                                }

                                if (!needsCSConversion) {
                                    jpegParam.setDestinationOffset(offset);
                                    jpegParam.setDestination(destination);
                                    jpegReader.read(0, jpegParam);
                                }
                                else {
                                    // Otherwise, it's likely CMYK or some other interpretation we don't need to convert.
                                    // We'll have to use readAsRaster and later apply color space conversion ourselves
                                    Raster raster = jpegReader.readRaster(0, jpegParam);
                                    // TODO: Refactor + duplicate this for all JPEG-in-TIFF cases
                                    switch (raster.getTransferType()) {
                                        case DataBuffer.TYPE_BYTE:
                                            normalizeColor(interpretation, ((DataBufferByte) raster.getDataBuffer()).getData());
                                            break;
                                        case DataBuffer.TYPE_USHORT:
                                            normalizeColor(interpretation, ((DataBufferUShort) raster.getDataBuffer()).getData());
                                            break;
                                        default:
                                            throw new IllegalStateException("Unsupported transfer type: " + raster.getTransferType());
                                    }

                                    destination.getRaster().setDataElements(offset.x, offset.y, raster);
                                }
                            }
                        }

                        if (abortRequested()) {
                            break;
                        }

                        col += colsInTile;
                    }

                    processImageProgress(100f * srcRow / height);

                    if (abortRequested()) {
                        processReadAborted();
                        break;
                    }

                    srcRow += rowsInTile;
                }

                break;

            case TIFFExtension.COMPRESSION_OLD_JPEG:
                // JPEG ('old-style' JPEG, later overridden in Technote2)
                // http://www.remotesensing.org/libtiff/TIFFTechNote2.html

                // 512/JPEGProc: 1=Baseline, 14=Lossless (with Huffman coding), no default, although 1 is assumed if absent
                int mode = getValueAsIntWithDefault(TIFF.TAG_OLD_JPEG_PROC, TIFFExtension.JPEG_PROC_BASELINE);
                switch (mode) {
                    case TIFFExtension.JPEG_PROC_BASELINE:
                    case TIFFExtension.JPEG_PROC_LOSSLESS:
                        break; // Supported
                    default:
                        throw new IIOException("Unknown TIFF JPEGProcessingMode value: " + mode);
                }

                jpegReader = createJPEGDelegate();
                jpegParam = (JPEGImageReadParam) jpegReader.getDefaultReadParam();

                // 513/JPEGInterchangeFormat (may be absent or 0)
                int jpegOffset = getValueAsIntWithDefault(TIFF.TAG_JPEG_INTERCHANGE_FORMAT, -1);
                // 514/JPEGInterchangeFormatLength (may be absent, or incorrect)
                // TODO: We used to issue a warning if the value was incorrect, should we still do that?
                int jpegLength = getValueAsIntWithDefault(TIFF.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, -1);
                // TODO: 515/JPEGRestartInterval (may be absent)

                // Currently ignored (for lossless only)
                // 517/JPEGLosslessPredictors
                // 518/JPEGPointTransforms

                if (jpegOffset > 0) {
                    if (currentIFD.getEntryById(TIFF.TAG_OLD_JPEG_Q_TABLES) != null
                            || currentIFD.getEntryById(TIFF.TAG_OLD_JPEG_DC_TABLES) != null
                            || currentIFD.getEntryById(TIFF.TAG_OLD_JPEG_AC_TABLES) != null) {
                        processWarningOccurred("Old-style JPEG compressed TIFF with JPEGInterchangeFormat encountered. Ignoring JPEG tables.");
                    }
                    else {
                        processWarningOccurred("Old-style JPEG compressed TIFF with JPEGInterchangeFormat encountered.");
                    }

                    imageInput.seek(jpegOffset);

                    // NOTE: Some known TIFF encoder encodes bad JPEGInterchangeFormat tags,
                    // but has the correct offset to the JPEG stream in the StripOffsets tag.
                    long realJPEGOffset = jpegOffset;

                    short expectedSOI = (short) (imageInput.readByte() << 8 | imageInput.readByte());
                    if (expectedSOI != (short) JPEG.SOI) {
                        if (stripTileOffsets != null && stripTileOffsets.length == 1) {
                            imageInput.seek(stripTileOffsets[0]);

                            expectedSOI = (short) (imageInput.readByte() << 8 | imageInput.readByte());
                            if (expectedSOI == (short) JPEG.SOI) {
                                realJPEGOffset = stripTileOffsets[0];
                            }
                        }

                        if (realJPEGOffset != jpegOffset) {
                            processWarningOccurred("Incorrect JPEGInterchangeFormat tag, using StripOffsets/TileOffsets instead.");
                        }
                        else {
                            processWarningOccurred("Incorrect JPEGInterchangeFormat tag encountered (not a valid SOI marker).");
                            // We'll fail below, but we don't need to handle this especially
                        }
                    }

                    byte[] jpegHeader;

                    if (stripTileOffsets == null || stripTileOffsets.length == 1 && realJPEGOffset == stripTileOffsets[0]) {
                        // In this case, we'll just read everything as a single tile
                        jpegHeader = new byte[0];
                    }
                    else {
                        // Wang TIFF weirdness, see http://www.eztwain.com/wangtiff.htm
                        // If the first tile stream starts with SOS, we'll correct offset/length
                        imageInput.seek(stripTileOffsets[0]);

                        if ((short) (imageInput.readByte() << 8 | imageInput.readByte()) == (short) JPEG.SOS) {
                            int len = 2 + (imageInput.readByte() << 8 | imageInput.readByte());
                            stripTileOffsets[0] += len;
                            stripTileByteCounts[0] -= len;
                        }

                        // We'll prepend each tile with a JFIF "header" (SOI...SOS)
                        imageInput.seek(realJPEGOffset);
                        jpegHeader = new byte[(int) (stripTileOffsets[0] - realJPEGOffset)];
                        imageInput.readFully(jpegHeader);
                    }

                    // In case of single tile, make sure we read the entire JFIF stream
                    if (stripTileByteCounts != null && stripTileByteCounts.length == 1) {
                        // TODO: Consider issue warning here!
                        stripTileByteCounts[0] = Math.max(stripTileByteCounts[0], jpegLength);
                    }

                    // Read data
                    processImageStarted(imageIndex);

                    for (int y = 0; y < tilesDown; y++) {
                        int col = 0;
                        int rowsInTile = Math.min(stripTileHeight, height - srcRow);

                        for (int x = 0; x < tilesAcross; x++) {
                            int colsInTile = Math.min(stripTileWidth, width - col);
                            int i = y * tilesAcross + x;

                            // Read only tiles that lies within region
                            if (new Rectangle(col, srcRow, colsInTile, rowsInTile).intersects(srcRegion)) {
                                int len = stripTileByteCounts != null ? (int) stripTileByteCounts[i] : Integer.MAX_VALUE;
                                imageInput.seek(stripTileOffsets != null ? stripTileOffsets[i] : realJPEGOffset);

                                try (ImageInputStream stream = ImageIO.createImageInputStream(new SequenceInputStream(Collections.enumeration(asList(
                                        new ByteArrayInputStream(jpegHeader),
                                        createStreamAdapter(imageInput, len),
                                        new ByteArrayInputStream(new byte[]{(byte) 0xff, (byte) 0xd9}) // EOI
                                ))))) {
                                    jpegReader.setInput(stream);
                                    jpegParam.setSourceRegion(new Rectangle(0, 0, colsInTile, rowsInTile));
                                    jpegParam.setSourceSubsampling(xSub, ySub, 0, 0);
                                    Point offset = new Point(col - srcRegion.x, srcRow - srcRegion.y);

                                    if (needsCSConversion == null) {
                                        needsCSConversion = needsCSConversion(compression, interpretation, readJPEGMetadataSafe(jpegReader));
                                    }

                                    if (!needsCSConversion) {
                                        jpegParam.setDestinationOffset(offset);
                                        jpegParam.setDestination(destination);
                                        jpegReader.read(0, jpegParam);
                                    }
                                    else {
                                        // Otherwise, it's likely CMYK or some other interpretation we don't need to convert.
                                        // We'll have to use readAsRaster and later apply color space conversion ourselves
                                        Raster raster = jpegReader.readRaster(0, jpegParam);
                                        normalizeColor(interpretation, ((DataBufferByte) raster.getDataBuffer()).getData());
                                        destination.getRaster().setDataElements(offset.x, offset.y, raster);
                                    }
                                }
                            }

                            if (abortRequested()) {
                                break;
                            }

                            col += colsInTile;
                        }

                        processImageProgress(100f * srcRow / height);

                        if (abortRequested()) {
                            processReadAborted();
                            break;
                        }

                        srcRow += rowsInTile;
                    }

                }
                else {
                    // The hard way: Read tables and re-create a full JFIF stream
                    processWarningOccurred("Old-style JPEG compressed TIFF without JPEGInterchangeFormat encountered. Attempting to re-create JFIF stream.");

                    // 519/JPEGQTables
                    // 520/JPEGDCTables
                    // 521/JPEGACTables

                    // These fields were originally intended to point to a list of offsets to the quantization tables, one per
                    // component. Each table consists of 64 BYTES (one for each DCT coefficient in the 8x8 block). The
                    // quantization tables are stored in zigzag order, and are compatible with the quantization tables
                    // usually found in a JPEG stream DQT marker.

                    // The original specification strongly recommended that, within the TIFF file, each component be
                    // assigned separate tables, and labelled this field as mandatory whenever the JPEGProc field specifies
                    // a DCT-based process.

                    // We've seen old-style JPEG in TIFF files where some or all Table offsets, contained the JPEGQTables,
                    // JPEGDCTables, and JPEGACTables tags are incorrect values beyond EOF. However, these files do always
                    // seem to contain a useful JPEGInterchangeFormat tag. Therefore, we recommend a careful attempt to read
                    // the Tables tags only as a last resort, if no table data is found in a JPEGInterchangeFormat stream.

                    // TODO: If any of the q/dc/ac tables are equal (or have same offset, even if "spec" violation),
                    // use only the first occurrence, and update selectors in SOF0 and SOS

                    long[] qTablesOffsets = getValueAsLongArray(TIFF.TAG_OLD_JPEG_Q_TABLES, "JPEGQTables", true);
                    byte[][] qTables = new byte[qTablesOffsets.length][64];
                    for (int j = 0; j < qTables.length; j++) {
                        imageInput.seek(qTablesOffsets[j]);
                        imageInput.readFully(qTables[j]);
                    }

                    long[] dcTablesOffsets = getValueAsLongArray(TIFF.TAG_OLD_JPEG_DC_TABLES, "JPEGDCTables", true);
                    byte[][] dcTables = new byte[dcTablesOffsets.length][];

                    for (int j = 0; j < dcTables.length; j++) {
                        imageInput.seek(dcTablesOffsets[j]);
                        byte[] lengths = new byte[16];

                        imageInput.readFully(lengths);

                        int length = 0;
                        for (int i = 0; i < 16; i++) {
                            length += lengths[i] & 0xff;
                        }

                        dcTables[j] = new byte[16 + length];
                        System.arraycopy(lengths, 0, dcTables[j], 0, 16);
                        imageInput.readFully(dcTables[j], 16, length);
                    }

                    long[] acTablesOffsets = getValueAsLongArray(TIFF.TAG_OLD_JPEG_AC_TABLES, "JPEGACTables", true);
                    byte[][] acTables = new byte[acTablesOffsets.length][];
                    for (int j = 0; j < acTables.length; j++) {
                        imageInput.seek(acTablesOffsets[j]);
                        byte[] lengths = new byte[16];

                        imageInput.readFully(lengths);

                        int length = 0;
                        for (int i = 0; i < 16; i++) {
                            length += lengths[i] & 0xff;
                        }

                        acTables[j] = new byte[16 + length];
                        System.arraycopy(lengths, 0, acTables[j], 0, 16);
                        imageInput.readFully(acTables[j], 16, length);
                    }

                    long[] yCbCrSubSampling = getValueAsLongArray(TIFF.TAG_YCBCR_SUB_SAMPLING, "YCbCrSubSampling", false);
                    int subsampling = yCbCrSubSampling != null
                                      ? (int) ((yCbCrSubSampling[0] & 0xf) << 4 | yCbCrSubSampling[1] & 0xf)
                                      : 0x22;

                    // Read data
                    processImageStarted(imageIndex);

                    for (int y = 0; y < tilesDown; y++) {
                        int col = 0;
                        int rowsInTile = Math.min(stripTileHeight, height - srcRow);

                        for (int x = 0; x < tilesAcross; x++) {
                            int colsInTile = Math.min(stripTileWidth, width - col);
                            int i = y * tilesAcross + x;

                            // Read only tiles that lies within region
                            if (new Rectangle(col, srcRow, colsInTile, rowsInTile).intersects(srcRegion)) {
                                int length = stripTileByteCounts != null ? (int) stripTileByteCounts[i] : Short.MAX_VALUE;
                                imageInput.seek(stripTileOffsets[i]);

                                // If the tile stream starts with SOS...
                                if (x == 0 && y == 0) {
                                    if ((short) (imageInput.readByte() << 8 | imageInput.readByte()) == (short) JPEG.SOS) {
                                        imageInput.seek(stripTileOffsets[i] + 14); // TODO: Read from SOS length from stream, in case of gray/CMYK
                                        length -= 14;
                                    }
                                    else {
                                        imageInput.seek(stripTileOffsets[i]);
                                    }
                                }

                                try (ImageInputStream stream = ImageIO.createImageInputStream(new SequenceInputStream(Collections.enumeration(
                                        asList(
                                                createJFIFStream(destRaster.getNumBands(), stripTileWidth, stripTileHeight, qTables, dcTables, acTables, subsampling),
                                                createStreamAdapter(imageInput, length),
                                                new ByteArrayInputStream(new byte[] {(byte) 0xff, (byte) 0xd9}) // EOI
                                        )
                                )))) {
                                    jpegReader.setInput(stream);
                                    jpegParam.setSourceRegion(new Rectangle(0, 0, colsInTile, rowsInTile));
                                    jpegParam.setSourceSubsampling(xSub, ySub, 0, 0);
                                    Point offset = new Point(col - srcRegion.x, srcRow - srcRegion.y);

                                    if (needsCSConversion == null) {
                                        needsCSConversion = needsCSConversion(compression, interpretation, readJPEGMetadataSafe(jpegReader));
                                    }

                                    if (!needsCSConversion) {
                                        jpegParam.setDestinationOffset(offset);
                                        jpegParam.setDestination(destination);
                                        jpegReader.read(0, jpegParam);
                                    }
                                    else {
                                        // Otherwise, it's likely CMYK or some other interpretation we don't need to convert.
                                        // We'll have to use readAsRaster and later apply color space conversion ourselves
                                        Raster raster = jpegReader.readRaster(0, jpegParam);
                                        normalizeColor(interpretation, ((DataBufferByte) raster.getDataBuffer()).getData());
                                        destination.getRaster().setDataElements(offset.x, offset.y, raster);
                                    }
                                }
                            }

                            if (abortRequested()) {
                                break;
                            }

                            col += colsInTile;
                        }

                        processImageProgress(100f * srcRow / height);

                        if (abortRequested()) {
                            processReadAborted();
                            break;
                        }

                        srcRow += rowsInTile;
                    }
                }

                break;

                // Known, but unsupported compression types
            case TIFFCustom.COMPRESSION_NEXT:
            case TIFFCustom.COMPRESSION_CCITTRLEW:
            case TIFFCustom.COMPRESSION_THUNDERSCAN:
            case TIFFCustom.COMPRESSION_IT8CTPAD:
            case TIFFCustom.COMPRESSION_IT8LW:
            case TIFFCustom.COMPRESSION_IT8MP:
            case TIFFCustom.COMPRESSION_IT8BL:
            case TIFFCustom.COMPRESSION_PIXARFILM:
            case TIFFCustom.COMPRESSION_PIXARLOG:
            case TIFFCustom.COMPRESSION_DCS:
            case TIFFCustom.COMPRESSION_JBIG: // Doable with JBIG plugin?
            case TIFFCustom.COMPRESSION_SGILOG:
            case TIFFCustom.COMPRESSION_SGILOG24:
            case TIFFCustom.COMPRESSION_JPEG2000: // Doable with JPEG2000 plugin?

                throw new IIOException("Unsupported TIFF Compression value: " + compression);
            default:
                throw new IIOException("Unknown TIFF Compression value: " + compression);
        }

        // TODO: Convert color space from source to destination

        processImageComplete();

        return destination;
    }

    private boolean containsZero(long[] byteCounts) {
        for (long byteCount : byteCounts) {
            if (byteCount <= 0) {
                return true;
            }
        }

        return false;
    }

    private IIOMetadata readJPEGMetadataSafe(final ImageReader jpegReader) throws IOException {
        try {
            return jpegReader.getImageMetadata(0);
        }
        catch (IIOException e) {
            processWarningOccurred(String.format("Could not read metadata for JPEG compressed TIFF (%s). Colors may look incorrect", e.getMessage()));

            return null;
        }
    }

    private boolean needsCSConversion(int compression, final int photometricInterpretation, final IIOMetadata imageMetadata) {
        if (imageMetadata == null) {
            // Assume we're ok
            return false;
        }

        int sourceCS = getJPEGSourceCS(imageMetadata);

        if (sourceCS == ColorSpace.TYPE_YCbCr && photometricInterpretation == TIFFExtension.PHOTOMETRIC_YCBCR
                || sourceCS == ColorSpace.TYPE_RGB && photometricInterpretation == TIFFBaseline.PHOTOMETRIC_RGB
                || sourceCS == ColorSpace.TYPE_GRAY && photometricInterpretation == TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO) {
            // Happy case, all equal and supported
            return false;
        }
        else if ((sourceCS == ColorSpace.TYPE_CMYK || sourceCS == ColorSpace.TYPE_4CLR)
                && photometricInterpretation == TIFFExtension.PHOTOMETRIC_SEPARATED) {
            // For YCCK/CMYK we always have to convert, as it's unsupported in
            // the standard JPEGImageReader
            return true;
        }
        else {
            // Otherwise, we have a mismatch

            // For "new-style" JPEG, assume TIFF PhotometricInterpretation to
            // be correct. This is in compliance with the TIFF spec.
            if (compression == TIFFExtension.COMPRESSION_JPEG) {
                return true;
            }

            processWarningOccurred(String.format("Determined color space from JPEG stream: '%s' does not match PhotometricInterpretation: %d. Colors may look incorrect", sourceCS, photometricInterpretation));

            // For "old-style" JPEG, we'll go with YCbCr if that's what
            // the JPEG stream says even though the TIFF spec says: "The
            // Photometric Interpretation and sub sampling fields written
            // to the file must describe what is actually in the file."
            return sourceCS != ColorSpace.TYPE_YCbCr;
        }
    }

    // NOTE: This algorithm is similar to the one found in the JPEGImageReader.
    // Perhaps we should instead expose it in the
    // com.twelvemonkeys.imageio.metadata.jpeg package to avoid duplication?
    // TODO: For a more failsafe detection of YCbCr/YCCK we could take the
    // chroma subsampling into account.
    // TODO: We should probably also emit a warning, if the TIFF subsampling
    // fields does not match the JPEG SOF subsampling fields.
    private int getJPEGSourceCS(final IIOMetadata imageMetadata) {
        if (imageMetadata == null) {
            return -1;
        }

        IIOMetadataNode nativeTree = (IIOMetadataNode) imageMetadata.getAsTree("javax_imageio_jpeg_image_1.0");

        IIOMetadataNode startOfFrame = getNode(nativeTree, "sof");
        IIOMetadataNode jfif = getNode(nativeTree, "app0JFIF");
        IIOMetadataNode adobe = getNode(nativeTree, "app14Adobe");

        if (startOfFrame != null) {
            int components = Integer.parseInt(startOfFrame.getAttribute("numFrameComponents"));

            switch (components) {
                case 1:
                case 2:
                    return ColorSpace.TYPE_GRAY;
                case 3:
                    if (jfif != null) {
                        return ColorSpace.TYPE_YCbCr;
                    }
                    else if (adobe != null) {
                        int transform = Integer.parseInt(adobe.getAttribute("transform"));

                        switch (transform) {
                            case 0:
                                return ColorSpace.TYPE_RGB;
                            case 1:
                                return ColorSpace.TYPE_YCbCr;
                            default:
                                // TODO: Warning!
                                return ColorSpace.TYPE_YCbCr; // assume it's YCbCr
                        }
                    }
                    else {
                        // Saw no special markers, try to guess from the component IDs
                        NodeList componentSpecs = startOfFrame.getElementsByTagName("componentSpec");

                        int cid0 = Integer.parseInt(((IIOMetadataNode) componentSpecs.item(0)).getAttribute("componentId"));
                        int cid1 = Integer.parseInt(((IIOMetadataNode) componentSpecs.item(1)).getAttribute("componentId"));
                        int cid2 = Integer.parseInt(((IIOMetadataNode) componentSpecs.item(2)).getAttribute("componentId"));

                        if (cid0 == 1 && cid1 == 2 && cid2 == 3) {
                            return ColorSpace.TYPE_YCbCr; // assume JFIF w/out marker
                        }
                        else if (cid0 == 'R' && cid1 == 'G' && cid2 == 'B') {
                            return ColorSpace.TYPE_RGB; // ASCII 'R', 'G', 'B'
                        }
                        else if (cid0 == 'Y' && cid1 == 'C' && cid2 == 'c') {
                            return ColorSpace.TYPE_3CLR; // Java special case: YCc
                        }
                        else {
                            // TODO: Warning!
                            return ColorSpace.TYPE_YCbCr; // assume it's YCbCr
                        }
                    }

                case 4:
                    if (adobe != null) {
                        int transform = Integer.parseInt(adobe.getAttribute("transform"));

                        switch (transform) {
                            case 0:
                                return ColorSpace.TYPE_CMYK;
                            case 2:
                                return ColorSpace.TYPE_4CLR; // YCCK
                            default:
                                // TODO: Warning!
                                return ColorSpace.TYPE_4CLR; // assume it's YCCK
                        }
                    }
                    else {
                        // Saw no special markers, try to guess from the component IDs
                        NodeList componentSpecs = startOfFrame.getElementsByTagName("componentSpec");

                        int cid0 = Integer.parseInt(((IIOMetadataNode) componentSpecs.item(0)).getAttribute("componentId"));
                        int cid1 = Integer.parseInt(((IIOMetadataNode) componentSpecs.item(1)).getAttribute("componentId"));
                        int cid2 = Integer.parseInt(((IIOMetadataNode) componentSpecs.item(2)).getAttribute("componentId"));
                        int cid3 = Integer.parseInt(((IIOMetadataNode) componentSpecs.item(3)).getAttribute("componentId"));

                        if (cid0 == 1 && cid1 == 2 && cid2 == 3 && cid3 == 4) {
                            return ColorSpace.TYPE_YCbCr; // Java special case: YCbCrA
                        }
                        else if (cid0 == 'R' && cid1 == 'G' && cid2 == 'B' && cid3 == 'A') {
                            return ColorSpace.TYPE_RGB; // Java special case: RGBA
                        }
                        else if (cid0 == 'Y' && cid1 == 'C' && cid2 == 'c' && cid3 == 'A') {
                            return ColorSpace.TYPE_3CLR; // Java special case: YCcA
                        }
                        else {
                            // TODO: Warning!
                            // No special markers, assume straight CMYK.
                            return ColorSpace.TYPE_CMYK;
                        }
                    }

                default:
                    return -1;
            }
        }

        return -1;
    }

    private IIOMetadataNode getNode(final IIOMetadataNode parent, final String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes != null && nodes.getLength() >= 1 ? (IIOMetadataNode) nodes.item(0) : null;
    }

    private ImageReader createJPEGDelegate() throws IOException {
        // We'll just use the default (first) reader
        // If it's the TwelveMonkeys one, we will be able to read JPEG Lossless etc.
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
        if (!readers.hasNext()) {
            throw new IIOException("Could not instantiate JPEGImageReader");
        }

        return readers.next();
    }

    private static InputStream createJFIFStream(int bands, int stripTileWidth, int stripTileHeight, byte[][] qTables, byte[][] dcTables, byte[][] acTables, int subsampling) throws IOException {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream(
                2 +
                        5 * qTables.length + qTables.length * qTables[0].length +
                        5 * dcTables.length + dcTables.length * dcTables[0].length +
                        5 * acTables.length + acTables.length * acTables[0].length +
                        2 + 2 + 6 + 3 * bands +
                        8 + 2 * bands
        );

        DataOutputStream out = new DataOutputStream(stream);

        out.writeShort(JPEG.SOI);

        // TODO: Consider merging if tables are equal
        for (int tableIndex = 0; tableIndex < qTables.length; tableIndex++) {
            byte[] table = qTables[tableIndex];
            out.writeShort(JPEG.DQT);
            out.writeShort(3 + table.length); // DQT length
            out.writeByte(tableIndex); // Q table id
            out.write(table); // Table data
        }

        // TODO: Consider merging if tables are equal
        for (int tableIndex = 0; tableIndex < dcTables.length; tableIndex++) {
            byte[] table = dcTables[tableIndex];
            out.writeShort(JPEG.DHT);
            out.writeShort(3 + table.length); // DHT length
            out.writeByte(tableIndex & 0xf); // Huffman table id
            out.write(table); // Table data
        }

        // TODO: Consider merging if tables are equal
        for (int tableIndex = 0; tableIndex < acTables.length; tableIndex++) {
            byte[] table = acTables[tableIndex];
            out.writeShort(JPEG.DHT);
            out.writeShort(3 + table.length); // DHT length
            out.writeByte(0x10 + (tableIndex & 0xf)); // Huffman table id
            out.write(table); // Table data
        }

        out.writeShort(JPEG.SOF0); // TODO: Use correct process for data
        out.writeShort(2 + 6 + 3 * bands); // SOF0 len
        out.writeByte(8); // bits TODO: Consult raster/transfer type or BitsPerSample for 12/16 bits support
        out.writeShort(stripTileHeight); // height
        out.writeShort(stripTileWidth); // width
        out.writeByte(bands); // Number of components

        for (int comp = 0; comp < bands; comp++) {
            out.writeByte(comp); // Component id
            out.writeByte(comp == 0 ? subsampling : 0x11); // h/v subsampling
            out.writeByte(comp); // Q table selector TODO: Consider merging if tables are equal
        }

        out.writeShort(JPEG.SOS);
        out.writeShort(6 + 2 * bands); // SOS length
        out.writeByte(bands); // Num comp

        for (int component = 0; component < bands; component++) {
            out.writeByte(component); // Comp id
            out.writeByte(component == 0 ? component : 0x10 + (component & 0xf)); // dc/ac selector
        }

        out.writeByte(0); // Spectral selection start
        out.writeByte(0); // Spectral selection end
        out.writeByte(0); // Approx high & low

//        System.err.println(TIFFReader.HexDump.dump(stream.toByteArray()));
//
        return stream.createInputStream();
    }

    private Raster clipRowToRect(final Raster raster, final Rectangle rect, final int[] bands, final int xSub) {
        if (rect.contains(raster.getMinX(), 0, raster.getWidth(), 1)
                && xSub == 1
                && bands == null /* TODO: Compare bands with that of raster */) {
            return raster;
        }

        return raster.createChild((rect.x + xSub - 1) / xSub, 0, (rect.width + xSub - 1) / xSub, 1, 0, 0, bands);
    }

    private WritableRaster clipToRect(final WritableRaster raster, final Rectangle rect, final int[] bands) {
        if (rect.contains(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight())
                && bands == null /* TODO: Compare bands with that of raster */) {
            return raster;
        }

        return raster.createWritableChild(rect.x, rect.y, rect.width, rect.height, 0, 0, bands);
    }

    private void readStripTileData(final Raster tileRowRaster, final Rectangle srcRegion, final int xSub, final int ySub,
                                   final int band, final int numBands, final int interpretation,
                                   final WritableRaster raster, final int startCol, final int startRow,
                                   final int colsInTile, final int rowsInTile, final DataInput input)
            throws IOException {

        DataBuffer dataBuffer = tileRowRaster.getDataBuffer();
        int bands = dataBuffer.getNumBanks();
        boolean banded = bands > 1;

        switch (tileRowRaster.getTransferType()) {
            case DataBuffer.TYPE_BYTE:

                /*for (int band = 0; band < bands; band++)*/ {
                int bank = banded ? ((BandedSampleModel) tileRowRaster.getSampleModel()).getBankIndices()[band] : band;

                byte[] rowDataByte = ((DataBufferByte) dataBuffer).getData(bank);
                WritableRaster destChannel = banded
                                             ? raster.createWritableChild(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), 0, 0, new int[] {band})
                                             : raster;
                Raster srcChannel = banded
                                    ? tileRowRaster.createChild(tileRowRaster.getMinX(), 0, tileRowRaster.getWidth(), 1, 0, 0, new int[] {band})
                                    : tileRowRaster;

                for (int row = startRow; row < startRow + rowsInTile; row++) {
                    if (row >= srcRegion.y + srcRegion.height) {
                        break; // We're done with this tile
                    }

                    input.readFully(rowDataByte);

                    if (row % ySub == 0 && row >= srcRegion.y) {
                        if (!banded) {
                            normalizeColor(interpretation, rowDataByte);
                        }

                        // Subsample horizontal
                        if (xSub != 1) {
                            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + colsInTile) / xSub) * numBands; x += numBands) {
                                System.arraycopy(rowDataByte, x * xSub, rowDataByte, x, numBands);
                            }
                        }

                        destChannel.setDataElements(startCol / xSub, (row - srcRegion.y) / ySub, srcChannel);
                    }
                    // Else skip data
                }
            }

//                if (banded) {
//                    // TODO: Normalize colors for tile (need to know tile region and sample model)
//                    // Unfortunately, this will disable acceleration...
//                }

            break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                /*for (int band = 0; band < bands; band++)*/ {
                short[] rowDataShort = dataBuffer.getDataType() == DataBuffer.TYPE_USHORT
                                       ? ((DataBufferUShort) dataBuffer).getData(band)
                                       : ((DataBufferShort) dataBuffer).getData(band);

                WritableRaster destChannel = banded
                                             ? raster.createWritableChild(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), 0, 0, new int[] {band})
                                             : raster;
                Raster srcChannel = banded
                                    ? tileRowRaster.createChild(tileRowRaster.getMinX(), 0, tileRowRaster.getWidth(), 1, 0, 0, new int[] {band})
                                    : tileRowRaster;

                for (int row = startRow; row < startRow + rowsInTile; row++) {
                    if (row >= srcRegion.y + srcRegion.height) {
                        break; // We're done with this tile
                    }

                    readFully(input, rowDataShort);

                    if (row >= srcRegion.y) {
                        normalizeColor(interpretation, rowDataShort);

                        // Subsample horizontal
                        if (xSub != 1) {
                            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + colsInTile) / xSub) * numBands; x += numBands) {
                                System.arraycopy(rowDataShort, x * xSub, rowDataShort, x, numBands);
                            }
                        }

                        destChannel.setDataElements(startCol / xSub, (row - srcRegion.y) / ySub, srcChannel);
                        // TODO: Possible speedup ~30%!:
//                        raster.setDataElements(startCol, row - srcRegion.y, colsInTile, 1, rowDataShort);
                    }
                    // Else skip data
                }
            }

            break;
            case DataBuffer.TYPE_INT:
                /*for (int band = 0; band < bands; band++)*/ {
                int[] rowDataInt = ((DataBufferInt) dataBuffer).getData(band);

                WritableRaster destChannel = banded
                                             ? raster.createWritableChild(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), 0, 0, new int[] {band})
                                             : raster;
                Raster srcChannel = banded
                                    ? tileRowRaster.createChild(tileRowRaster.getMinX(), 0, tileRowRaster.getWidth(), 1, 0, 0, new int[] {band})
                                    : tileRowRaster;

                for (int row = startRow; row < startRow + rowsInTile; row++) {
                    if (row >= srcRegion.y + srcRegion.height) {
                        break; // We're done with this tile
                    }

                    readFully(input, rowDataInt);

                    if (row >= srcRegion.y) {
                        normalizeColor(interpretation, rowDataInt);

                        // Subsample horizontal
                        if (xSub != 1) {
                            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + colsInTile) / xSub) * numBands; x += numBands) {
                                System.arraycopy(rowDataInt, x * xSub, rowDataInt, x, numBands);
                            }
                        }

                        destChannel.setDataElements(startCol / xSub, (row - srcRegion.y) / ySub, srcChannel);
                    }
                    // Else skip data
                }
            }

            break;

            case DataBuffer.TYPE_FLOAT:
                /*for (int band = 0; band < bands; band++)*/ {
                float[] rowDataFloat = ((DataBufferFloat) tileRowRaster.getDataBuffer()).getData(band);

                WritableRaster destChannel = banded
                                             ? raster.createWritableChild(raster.getMinX(), raster.getMinY(), raster.getWidth(), raster.getHeight(), 0, 0, new int[] {band})
                                             : raster;
                Raster srcChannel = banded
                                    ? tileRowRaster.createChild(tileRowRaster.getMinX(), 0, tileRowRaster.getWidth(), 1, 0, 0, new int[] {band})
                                    : tileRowRaster;

                for (int row = startRow; row < startRow + rowsInTile; row++) {
                    if (row >= srcRegion.y + srcRegion.height) {
                        break; // We're done with this tile
                    }

                    readFully(input, rowDataFloat);

                    if (row >= srcRegion.y) {
                        normalizeColor(interpretation, rowDataFloat);

                        // Subsample horizontal
                        if (xSub != 1) {
                            for (int x = srcRegion.x / xSub * numBands; x < ((srcRegion.x + srcRegion.width) / xSub) * numBands; x += numBands) {
                                System.arraycopy(rowDataFloat, x * xSub, rowDataFloat, x, numBands);
                            }
                        }

                        destChannel.setDataElements(startCol, row - srcRegion.y, srcChannel);
                    }
                    // Else skip data
                }
            }

            break;
        }
    }

    private void clamp(float[] rowDataFloat) {
        for (int i = 0; i < rowDataFloat.length; i++) {
            if (rowDataFloat[i] > 1f) {
                rowDataFloat[i] = 1f;
            }
            else if (rowDataFloat[i] < 0f) {
                rowDataFloat[i] = 0f;
            }
        }
    }

    // TODO: Candidate util method (with off/len + possibly byte order)
    private void readFully(final DataInput input, final float[] rowDataFloat) throws IOException {
        if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            imageInputStream.readFully(rowDataFloat, 0, rowDataFloat.length);
        }
        else {
            for (int k = 0; k < rowDataFloat.length; k++) {
                rowDataFloat[k] = input.readFloat();
            }
        }
    }

    // TODO: Candidate util method (with off/len + possibly byte order)
    private void readFully(final DataInput input, final int[] rowDataInt) throws IOException {
        if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            imageInputStream.readFully(rowDataInt, 0, rowDataInt.length);
        }
        else {
            for (int k = 0; k < rowDataInt.length; k++) {
                rowDataInt[k] = input.readInt();
            }
        }
    }

    // TODO: Candidate util method (with off/len + possibly byte order)
    private void readFully(final DataInput input, final short[] rowDataShort) throws IOException {
        if (input instanceof ImageInputStream) {
            ImageInputStream imageInputStream = (ImageInputStream) input;
            imageInputStream.readFully(rowDataShort, 0, rowDataShort.length);
        }
        else {
            for (int k = 0; k < rowDataShort.length; k++) {
                rowDataShort[k] = input.readShort();
            }
        }
    }

    private void normalizeColor(int photometricInterpretation, byte[] data) throws IOException {
        switch (photometricInterpretation) {
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
                // NOTE: Preserve WhiteIsZero for 1 bit monochrome, for CCITT compatibility
                if (getBitsPerSample() > 1 || getValueAsIntWithDefault(TIFF.TAG_SAMPLES_PER_PIXEL, 1) > 1) {
                    // Inverse values
                    for (int i = 0; i < data.length; i++) {
                        data[i] ^= -1;
                    }
                }

                break;

            case TIFFExtension.PHOTOMETRIC_CIELAB:
            case TIFFExtension.PHOTOMETRIC_ICCLAB:
            case TIFFExtension.PHOTOMETRIC_ITULAB:
                // TODO: White point may be encoded in separate tag
                CIELabColorConverter converter = new CIELabColorConverter(
                        photometricInterpretation == TIFFExtension.PHOTOMETRIC_CIELAB
                        ? Illuminant.D65
                        : Illuminant.D50
                );
                float[] temp = new float[3];

                for (int i = 0; i < data.length; i += 3) {
                    // Unsigned scaled form 0...100
                    float LStar = (data[i] & 0xff) * 100f / 255.0f;
                    float aStar;
                    float bStar;

                    if (photometricInterpretation == TIFFExtension.PHOTOMETRIC_CIELAB) {
                        // -128...127
                        aStar = data[i + 1];
                        bStar = data[i + 2];
                    }
                    else {
                        // Assumes same data for ICC and ITU (unsigned)
                        // 0...255
                        aStar = (data[i + 1] & 0xff) - 128;
                        bStar = (data[i + 2] & 0xff) - 128;
                    }

                    converter.toRGB(LStar, aStar, bStar, temp);

                    data[i    ] = (byte) temp[0];
                    data[i + 1] = (byte) temp[1];
                    data[i + 2] = (byte) temp[2];
                }

                break;

            case TIFFExtension.PHOTOMETRIC_YCBCR:
                // Default:  CCIR Recommendation 601-1: 299/1000, 587/1000 and 114/1000
                double[] coefficients = getValueAsDoubleArray(TIFF.TAG_YCBCR_COEFFICIENTS, "YCbCrCoefficients", false, 3);

                // "Default" [0, 255, 128, 255, 128, 255] for YCbCr (real default is [0, 255, 0, 255, 0, 255] for RGB)
                double[] referenceBW = getValueAsDoubleArray(TIFF.TAG_REFERENCE_BLACK_WHITE, "ReferenceBlackWhite", false, 6);

                if ((coefficients == null || Arrays.equals(coefficients, CCIR_601_1_COEFFICIENTS))
                        && (referenceBW == null || Arrays.equals(referenceBW, REFERENCE_BLACK_WHITE_YCC_DEFAULT))) {
                    // Fast, default conversion
                    for (int i = 0; i < data.length; i += 3) {
                        YCbCrConverter.convertYCbCr2RGB(data, data, i);
                    }
                }
                else {
                    // If one of the values are null, we'll need the other here...
                    if (coefficients == null) {
                        coefficients = CCIR_601_1_COEFFICIENTS;
                    }

                    if (referenceBW != null && Arrays.equals(referenceBW, REFERENCE_BLACK_WHITE_YCC_DEFAULT)) {
                        referenceBW = null;
                    }

                    for (int i = 0; i < data.length; i += 3) {
                        YCbCrConverter.convertYCbCr2RGB(data, data, coefficients, referenceBW, i);
                    }
                }

                break;
        }
    }

    private void normalizeColor(int photometricInterpretation, short[] data) throws IIOException {
        switch (photometricInterpretation) {
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
                // Inverse values
                for (int i = 0; i < data.length; i++) {
                    data[i] ^= -1;
                }

                break;

            case TIFFExtension.PHOTOMETRIC_CIELAB:
            case TIFFExtension.PHOTOMETRIC_ICCLAB:
            case TIFFExtension.PHOTOMETRIC_ITULAB:
                // TODO: White point may be encoded in separate tag
                CIELabColorConverter converter = new CIELabColorConverter(
                        photometricInterpretation == TIFFExtension.PHOTOMETRIC_ITULAB
                        ? Illuminant.D65
                        : Illuminant.D50
                );

                float[] temp = new float[3];
                float scaleL = photometricInterpretation == TIFFExtension.PHOTOMETRIC_CIELAB ? 65535f : 65280f; // Is for ICC lab, assumes the same for ITU....

                for (int i = 0; i < data.length; i += 3) {
                    // Unsigned scaled form 0...100
                    float LStar = (data[i] & 0xffff) * 100.0f / scaleL;
                    float aStar;
                    float bStar;

                    if (photometricInterpretation == TIFFExtension.PHOTOMETRIC_CIELAB) {
                        // -32768...32767
                        aStar = data[i + 1] / 256f;
                        bStar = data[i + 2] / 256f;
                    }
                    else {
                        // Assumes same data for ICC and ITU (unsigned)
                        // 0...65535f
                        aStar = ((data[i + 1] & 0xffff) - 32768) / 256f;
                        bStar = ((data[i + 2] & 0xffff) - 32768) / 256f;
                    }

                    converter.toRGB(LStar, aStar, bStar, temp);

                    data[i    ] = (short) (temp[0] * 257f);
                    data[i + 1] = (short) (temp[1] * 257f);
                    data[i + 2] = (short) (temp[2] * 257f);
                }

                break;

            case TIFFExtension.PHOTOMETRIC_YCBCR:
                // Default:  CCIR Recommendation 601-1: 299/1000, 587/1000 and 114/1000
                double[] coefficients = getValueAsDoubleArray(TIFF.TAG_YCBCR_COEFFICIENTS, "YCbCrCoefficients", false, 3);

                // "Default" [0, 255, 128, 255, 128, 255] for YCbCr (real default is [0, 255, 0, 255, 0, 255] for RGB)
                double[] referenceBW = getValueAsDoubleArray(TIFF.TAG_REFERENCE_BLACK_WHITE, "ReferenceBlackWhite", false, 6);

                // If one of the values are null, we'll need the other here...
                if (coefficients == null) {
                    coefficients = CCIR_601_1_COEFFICIENTS;
                }

                if (referenceBW != null && Arrays.equals(referenceBW, REFERENCE_BLACK_WHITE_YCC_DEFAULT)) {
                    referenceBW = null;
                }

                for (int i = 0; i < data.length; i += 3) {
                    convertYCbCr2RGB(data, data, coefficients, referenceBW, i);
                }
        }
    }
    private void normalizeColor(int photometricInterpretation, int[] data) {
        switch (photometricInterpretation) {
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
                // Inverse values
                for (int i = 0; i < data.length; i++) {
                    data[i] ^= -1;
                }

                break;

            case TIFFExtension.PHOTOMETRIC_CIELAB:
            case TIFFExtension.PHOTOMETRIC_ICCLAB:
            case TIFFExtension.PHOTOMETRIC_ITULAB:
            case TIFFExtension.PHOTOMETRIC_YCBCR:
                // Not supported
                break;
        }
    }

    private void normalizeColor(int photometricInterpretation, float[] data) {
        // TODO: Allow param to decide tone mapping strategy, like in the HDRImageReader
        clamp(data);

        switch (photometricInterpretation) {
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
                // Inverse values
                for (int i = 0; i < data.length; i++) {
                    data[i] = 1f - data[i];
                }

                break;

            case TIFFExtension.PHOTOMETRIC_CIELAB:
            case TIFFExtension.PHOTOMETRIC_ICCLAB:
            case TIFFExtension.PHOTOMETRIC_ITULAB:
            case TIFFExtension.PHOTOMETRIC_YCBCR:
                // Not supported
                break;
        }
    }

    private void convertYCbCr2RGB(final short[] yCbCr, final short[] rgb, final double[] coefficients, final double[] referenceBW, final int offset) {
        double y;
        double cb;
        double cr;

        if (referenceBW == null) {
            // Default case
            y = (yCbCr[offset] & 0xffff);
            cb = (yCbCr[offset + 1] & 0xffff) - 32768;
            cr = (yCbCr[offset + 2] & 0xffff) - 32768;
        }
        else {
            // Custom values
            y = ((yCbCr[offset] & 0xffff) - referenceBW[0]) * (65535.0) / (referenceBW[1] - referenceBW[0]);
            cb = ((yCbCr[offset + 1] & 0xffff) - referenceBW[2]) * 32767.0 / (referenceBW[3] - referenceBW[2]);
            cr = ((yCbCr[offset + 2] & 0xffff) - referenceBW[4]) * 32767.0 / (referenceBW[5] - referenceBW[4]);
        }

        double lumaRed = coefficients[0];
        double lumaGreen = coefficients[1];
        double lumaBlue = coefficients[2];

        int red = (int) Math.round(cr * (2.0 - 2.0 * lumaRed) + y);
        int blue = (int) Math.round(cb * (2.0 - 2.0 * lumaBlue) + y);
        int green = (int) Math.round((y - lumaRed * (red) - lumaBlue * (blue)) / lumaGreen);

        short r = clampShort(red);
        short g = clampShort(green);
        short b = clampShort(blue);

        // Short values, depends on byte order!
        rgb[offset] = r;
        rgb[offset + 1] = g;
        rgb[offset + 2] = b;
    }

    private short clampShort(int val) {
        return (short) Math.max(0, Math.min(0xffff, val));
    }

    private InputStream createDecompressorStream(final int compression, final int width, final int bands, final InputStream stream) throws IOException {
        int fillOrder = getValueAsIntWithDefault(TIFF.TAG_FILL_ORDER, 1);

        switch (compression) {
            case TIFFBaseline.COMPRESSION_NONE:
                return stream;
            case TIFFBaseline.COMPRESSION_PACKBITS:
                return new DecoderStream(createFillOrderStream(fillOrder, stream), new PackBitsDecoder(), 1024);
            case TIFFExtension.COMPRESSION_LZW:
                // NOTE: Needs large buffer for compatibility with certain encoders
                return new DecoderStream(createFillOrderStream(fillOrder, stream), LZWDecoder.create(LZWDecoder.isOldBitReversedStream(stream)), Math.max(width * bands, 4096));
            case TIFFExtension.COMPRESSION_ZLIB:
            case TIFFExtension.COMPRESSION_DEFLATE:
                // TIFF specification, supplement 2 says ZLIB (8) and DEFLATE (32946) algorithms are identical
            case TIFFCustom.COMPRESSION_PIXTIFF_ZIP:
                return new InflaterInputStream(createFillOrderStream(fillOrder, stream), new Inflater(), 1024);
            case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
            case TIFFExtension.COMPRESSION_CCITT_T4:
            case TIFFExtension.COMPRESSION_CCITT_T6:
                return new CCITTFaxDecoderStream(stream, width, compression, fillOrder, getCCITTOptions(compression));
            default:
                throw new IllegalArgumentException("Unsupported TIFF compression: " + compression);
        }
    }

    private InputStream createFillOrderStream(final int fillOrder, final InputStream stream) {
        switch (fillOrder) {
            case TIFFBaseline.FILL_LEFT_TO_RIGHT:
                return stream;
            case TIFFExtension.FILL_RIGHT_TO_LEFT:
                return new ReverseInputStream(stream);
            default:
                throw new IllegalArgumentException("Unsupported TIFF FillOrder: " + fillOrder);
        }
    }

    private long getCCITTOptions(final int compression) throws IIOException {
        switch (compression) {
            case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
                return 0L;
            case TIFFExtension.COMPRESSION_CCITT_T4:
                return getValueAsLongWithDefault(TIFF.TAG_GROUP3OPTIONS, 0L);
            case TIFFExtension.COMPRESSION_CCITT_T6:
                return getValueAsLongWithDefault(TIFF.TAG_GROUP4OPTIONS, 0L);
            default:
                throw new IllegalArgumentException("No CCITT options for compression: " + compression);
        }
    }

    private InputStream createUnpredictorStream(final int predictor, final int width, final int samplesPerPixel, final int bitsPerSample, final InputStream stream, final ByteOrder byteOrder) throws IOException {
        switch (predictor) {
            case TIFFBaseline.PREDICTOR_NONE:
                return stream;
            case TIFFExtension.PREDICTOR_HORIZONTAL_DIFFERENCING:
                return new HorizontalDeDifferencingStream(stream, width, samplesPerPixel, bitsPerSample, byteOrder);
            case TIFFExtension.PREDICTOR_HORIZONTAL_FLOATINGPOINT:
                throw new IIOException("Unsupported TIFF Predictor value: " + predictor);
            default:
                throw new IIOException("Unknown TIFF Predictor value: " + predictor);
        }
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

    private double[] getValueAsDoubleArray(final int tag, final String tagName, final boolean required, final int expectedLength) throws IIOException {
        Entry entry = currentIFD.getEntryById(tag);

        if (entry == null) {
            if (required) {
                throw new IIOException("Missing TIFF tag " + tagName);
            }

            return null;
        }

        if (expectedLength > 0 && entry.valueCount() != expectedLength) {
            if (required) {
                throw new IIOException(String.format("Unexpected value count for %s: %d (expected %d values)", tagName, entry.valueCount(), expectedLength));
            }

            return null;
        }

        double[] value;

        if (entry.valueCount() == 1) {
            // For single entries, this will be a boxed type
            value = new double[] {((Number) entry.getValue()).doubleValue()};
        }
        else if (entry.getValue() instanceof float[]) {
            float[] floats = (float[]) entry.getValue();
            value = new double[floats.length];

            for (int i = 0, length = value.length; i < length; i++) {
                value[i] = floats[i];
            }
        }
        else if (entry.getValue() instanceof double[]) {
            value = (double[]) entry.getValue();
        }
        else if (entry.getValue() instanceof Rational[]) {
            Rational[] rationals = (Rational[]) entry.getValue();
            value = new double[rationals.length];

            for (int i = 0, length = value.length; i < length; i++) {
                value[i] = rationals[i].doubleValue();
            }
        }
        else {
            throw new IIOException(String.format("Unsupported %s type: %s (%s)", tagName, entry.getTypeName(), entry.getValue().getClass()));
        }

        return value;
    }

    private ICC_Profile getICCProfile() throws IOException {
        Entry entry = currentIFD.getEntryById(TIFF.TAG_ICC_PROFILE);

        if (entry != null) {
            byte[] value = (byte[]) entry.getValue();

            // Validate ICC profile size vs actual value size
            int size = (value[0] & 0xff) << 24 | (value[1] & 0xff) << 16 | (value[2] & 0xff) << 8 | (value[3] & 0xff);
            if (size < 0 || size > value.length) {
                processWarningOccurred("Ignoring truncated ICC profile: Bad ICC profile size (" + size + ")");
                return null;
            }

            try {
                // WEIRDNESS: Reading profile from InputStream is somehow more compatible
                // than reading from byte array (chops off extra bytes + validates profile).
                ICC_Profile profile = ICC_Profile.getInstance(new ByteArrayInputStream(value));
                return ColorSpaces.validateProfile(profile);
            }
            catch (CMMException | IllegalArgumentException ignore) {
                processWarningOccurred("Ignoring broken/incompatible ICC profile: " + ignore.getMessage());
            }
        }

        return null;
    }
    @Override
    public boolean canReadRaster() {
        return true;
    }

    @Override
    public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
        return read(imageIndex, param).getData();
    }

    // TODO: Tiling support
    // isImageTiled
    // getTileWidth
    // getTileHeight
    // readTile
    // readTileRaster

    // TODO: Thumbnail support

    /// Metadata

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        readIFD(imageIndex);

        return new TIFFImageMetadata(currentIFD);
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        readMetadata();

        return new TIFFStreamMetadata(imageInput.getByteOrder());
    }

    public static void main(final String[] args) throws IOException {
        ImageIO.setUseCache(false);

        for (final String arg : args) {
            File file = new File(arg);

            ImageInputStream input = ImageIO.createImageInputStream(file);
            if (input == null) {
                System.err.println("Could not read file: " + file);
                continue;
            }

            deregisterOSXTIFFImageReaderSpi();

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                System.err.println("No reader for: " + file);
                continue;
            }

            ImageReader reader = readers.next();
            System.err.println("Reading using: " + reader);

            reader.addIIOReadWarningListener(new IIOReadWarningListener() {
                public void warningOccurred(ImageReader source, String warning) {
                    System.err.println("Warning: " + arg + ": " + warning);
                }
            });
            reader.addIIOReadProgressListener(new ProgressListenerBase() {
                private static final int MAX_W = 78;
                int lastProgress = 0;

                @Override
                public void imageStarted(ImageReader source, int imageIndex) {
                    System.out.print("[");
                }

                @Override
                public void imageProgress(ImageReader source, float percentageDone) {
                    int steps = ((int) (percentageDone * MAX_W) / 100);

                    for (int i = lastProgress; i < steps; i++) {
                        System.out.print(".");
                    }

                    System.out.flush();
                    lastProgress = steps;
                }

                @Override
                public void imageComplete(ImageReader source) {
                    for (int i = lastProgress; i < MAX_W; i++) {
                        System.out.print(".");
                    }

                    System.out.println("]");
                }
            });

            reader.setInput(input);

            try {
                ImageReadParam param = reader.getDefaultReadParam();

                if (param.getClass().getName().equals("com.twelvemonkeys.imageio.plugins.svg.SVGReadParam")) {
                    Method setBaseURI = param.getClass().getMethod("setBaseURI", String.class);
                    String uri = file.getAbsoluteFile().toURI().toString();
                    setBaseURI.invoke(param, uri);
                }

                int numImages = reader.getNumImages(true);
                for (int imageNo = 0; imageNo < numImages; imageNo++) {
                    //            if (args.length > 1) {
                    //                int sub = Integer.parseInt(args[1]);
                    //                int sub = 4;
                    //                param.setSourceSubsampling(sub, sub, 0, 0);
                    //            }

                    try {
                        long start = System.currentTimeMillis();
//                    int width = reader.getWidth(imageNo);
//                    int height = reader.getHeight(imageNo);
//                    param.setSourceRegion(new Rectangle(width / 4, height / 4, width / 2, height / 2));
//                    param.setSourceRegion(new Rectangle(100, 300, 400, 400));
//                    param.setSourceRegion(new Rectangle(95, 105, 100, 100));
//                    param.setSourceRegion(new Rectangle(3, 3, 9, 9));
//                    param.setDestinationOffset(new Point(50, 150));
//                    param.setSourceSubsampling(2, 2, 0, 0);
//                    param.setSourceSubsampling(3, 3, 0, 0);
                        BufferedImage image = reader.read(imageNo, param);
                        System.err.println("Read time: " + (System.currentTimeMillis() - start) + " ms");

                        IIOMetadata metadata = reader.getImageMetadata(imageNo);
                        if (metadata != null) {
                            if (metadata.getNativeMetadataFormatName() != null) {
                                Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                                replaceBytesWithUndefined((IIOMetadataNode) tree);
                                new XMLSerializer(System.out, "UTF-8").serialize(tree, false);
                            }
                        /*else*/
                            if (metadata.isStandardMetadataFormatSupported()) {
                                new XMLSerializer(System.out, "UTF-8").serialize(metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName), false);
                            }
                        }

                        System.err.println("image: " + image);

//                        int w = image.getWidth();
//                        int h = image.getHeight();
//
//                        int newW = h;
//                        int newH = w;
//
//                        AffineTransform xform =  AffineTransform.getTranslateInstance((newW - w) / 2.0, (newH - h) / 2.0);
//                        xform.concatenate(AffineTransform.getQuadrantRotateInstance(3, w / 2.0, h / 2.0));
//                        AffineTransformOp op = new AffineTransformOp(xform, null);
//
//                        image = op.filter(image, null);
//
//                        System.err.println("image: " + image);
//
//                    File tempFile = File.createTempFile("lzw-", ".bin");
//                    byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
//                    FileOutputStream stream = new FileOutputStream(tempFile);
//                    try {
//                        FileUtil.copy(new ByteArrayInputStream(data, 45 * image.getWidth() * 3, 5 * image.getWidth() * 3), stream);
//
//                        showIt(image.getSubimage(0, 45, image.getWidth(), 5), tempFile.getAbsolutePath());
//                    }
//                    finally {
//                        stream.close();
//                    }
//
//                    System.err.println("tempFile: " + tempFile.getAbsolutePath());

                        //            image = new ResampleOp(reader.getWidth(0) / 4, reader.getHeight(0) / 4, ResampleOp.FILTER_LANCZOS).filter(image, null);
//
//                int maxW = 800;
//                int maxH = 800;
//
//                if (image.getWidth() > maxW || image.getHeight() > maxH) {
//                    start = System.currentTimeMillis();
//                    float aspect = reader.getAspectRatio(0);
//                    if (aspect >= 1f) {
//                        image = ImageUtil.createResampled(image, maxW, Math.round(maxW / aspect), Image.SCALE_DEFAULT);
//                    }
//                    else {
//                        image = ImageUtil.createResampled(image, Math.round(maxH * aspect), maxH, Image.SCALE_DEFAULT);
//                    }
//    //                    System.err.println("Scale time: " + (System.currentTimeMillis() - start) + " ms");
//                }

                        if (image != null && image.getType() == BufferedImage.TYPE_CUSTOM) {
                            start = System.currentTimeMillis();
                            image = new ColorConvertOp(null).filter(image, new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB));
                            System.err.println("Conversion time: " + (System.currentTimeMillis() - start) + " ms");
                        }

                        showIt(image, String.format("Image: %s [%d x %d]", file.getName(), reader.getWidth(imageNo), reader.getHeight(imageNo)));

                        try {
                            int numThumbnails = reader.getNumThumbnails(imageNo);
                            for (int thumbnailNo = 0; thumbnailNo < numThumbnails; thumbnailNo++) {
                                BufferedImage thumbnail = reader.readThumbnail(imageNo, thumbnailNo);
                                //                        System.err.println("thumbnail: " + thumbnail);
                                showIt(thumbnail, String.format("Thumbnail: %s [%d x %d]", file.getName(), thumbnail.getWidth(), thumbnail.getHeight()));
                            }
                        }
                        catch (IIOException e) {
                            System.err.println("Could not read thumbnails: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    catch (Throwable t) {
                        System.err.println(file + " image " + imageNo + " can't be read:");
                        t.printStackTrace();
                    }
                }
            }
            catch (Throwable t) {
                System.err.println(file + " can't be read:");
                t.printStackTrace();
            }
            finally {
                input.close();
            }
        }
    }

    // XMP Spec says "The field type should be UNDEFINED (7) or BYTE (1)"
    // Adobe Photoshop® TIFF Technical Notes says (for Image Source Data): "Type: UNDEFINED"
    private static final Set<String> BYTE_TO_UNDEFINED_NODES = new HashSet<>(asList(
              "700", // XMP
            "34377", // Photoshop Image Resources
            "37724"  // Image Source Data
    ));

    private static void replaceBytesWithUndefined(IIOMetadataNode tree) {
        // The output of the TIFFUndefined tag is just much more readable (or easier to skip)

        NodeList nodes = tree.getElementsByTagName("TIFFBytes");
        for (int i = 0; i < nodes.getLength(); i++) {
            IIOMetadataNode node = (IIOMetadataNode) nodes.item(i);

            IIOMetadataNode parentNode = (IIOMetadataNode) node.getParentNode();

            NodeList childNodes = node.getChildNodes();
            if (BYTE_TO_UNDEFINED_NODES.contains(parentNode.getAttribute("number")) && childNodes.getLength() > 16) {
                IIOMetadataNode undefined = new IIOMetadataNode("TIFFUndefined");
                StringBuilder values = new StringBuilder();

                IIOMetadataNode child = (IIOMetadataNode) node.getFirstChild();
                while (child != null) {
                    if (values.length() > 0) {
                        values.append(", ");
                    }

                    String value = child.getAttribute("value");
                    values.append(value);

                    child = (IIOMetadataNode) child.getNextSibling();
                }

                undefined.setAttribute("value", values.toString());

                parentNode.replaceChild(undefined, node);
            }
        }
    }

    protected static void showIt(BufferedImage image, String title) {
        ImageReaderBase.showIt(image, title);
    }

    private static void deregisterOSXTIFFImageReaderSpi() {
        IIORegistry registry = IIORegistry.getDefaultInstance();
        ImageReaderSpi provider = lookupProviderByName(registry, "com.sun.imageio.plugins.tiff.TIFFImageReaderSpi", ImageReaderSpi.class);

        if (provider != null) {
            registry.deregisterServiceProvider(provider);
        }
    }
}
