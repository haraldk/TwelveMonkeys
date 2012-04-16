/*
 * Copyright (c) 2011, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  Neither the name "TwelveMonkeys" nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.image.InverseColorMapIndexColorModel;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.*;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * A JPEG {@code ImageReader} implementation based on the JRE {@code JPEGImageReader},
 * with support for CMYK JPEGs and other non-standard color spaces,
 * like embedded ICC color spaces with rendering intent other than 'perceptual'.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author LUT-based YCbCR conversion by Werner Randelshofer
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReader.java,v 1.0 24.01.11 16.37 haraldk Exp$
 */
public class JPEGImageReader extends ImageReaderBase {
    // TODO: Fix the (stream) metadata inconsistency issues.
    // - Sun JPEGMetadata class does not (and can not be made to) support CMYK data.. We need to create all new metadata classes.. :-/
    // TODO: Split thumbnail reading into separate class

    private final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.jpeg.debug"));

    /** Segment identifiers for the JPEG segments we care about reading. */
    private static final Map<Integer, List<String>> SEGMENT_IDENTIFIERS = createSegmentIds();

    private static Map<Integer, List<String>> createSegmentIds() {
        Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

        // JFIF/JFXX APP0 markers
        map.put(JPEG.APP0, JPEGSegmentUtil.ALL_IDS);

        // Exif metadata
        map.put(JPEG.APP1, Collections.singletonList("Exif"));

        // ICC Color Profile
        map.put(JPEG.APP2, Collections.singletonList("ICC_PROFILE"));

        // Adobe APP14 marker
        map.put(JPEG.APP14, Collections.singletonList("Adobe"));

        // SOFn markers
        map.put(JPEG.SOF0, null);
        map.put(JPEG.SOF1, null);
        map.put(JPEG.SOF2, null);
        map.put(JPEG.SOF3, null);
        map.put(JPEG.SOF5, null);
        map.put(JPEG.SOF6, null);
        map.put(JPEG.SOF7, null);
        map.put(JPEG.SOF9, null);
        map.put(JPEG.SOF10, null);
        map.put(JPEG.SOF11, null);
        map.put(JPEG.SOF13, null);
        map.put(JPEG.SOF14, null);
        map.put(JPEG.SOF15, null);

        return Collections.unmodifiableMap(map);
    }

    /** Our JPEG reading delegate */
    private final ImageReader delegate;

    /** Listens to progress updates in the delegate, and delegates back to this instance */
    private final ProgressDelegator progressDelegator;

    /** Cached JPEG app segments */
    private List<JPEGSegment> segments;

    private List<BufferedImage> thumbnails;

    JPEGImageReader(final ImageReaderSpi provider, final ImageReader delegate) {
        super(provider);
        this.delegate = Validate.notNull(delegate);

        progressDelegator = new ProgressDelegator();
    }

    private void installListeners() {
        delegate.addIIOReadProgressListener(progressDelegator);
        delegate.addIIOReadUpdateListener(progressDelegator);
        delegate.addIIOReadWarningListener(progressDelegator);
    }

    @Override
    protected void resetMembers() {
        delegate.reset();
        segments = null;
        thumbnails = null;

        installListeners();
    }

    @Override
    public void dispose() {
        super.dispose();

        delegate.dispose();
    }

    @Override
    public String getFormatName() throws IOException {
        return delegate.getFormatName();
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        return delegate.getNumImages(allowSearch);
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        return delegate.getWidth(imageIndex);
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        return delegate.getHeight(imageIndex);
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        // TODO: Read header, and make sure we return valid types for the images we can now read

        Iterator<ImageTypeSpecifier> types = delegate.getImageTypes(imageIndex);

        ICC_Profile profile = getEmbeddedICCProfile();
        AdobeDCT adobeDCT = getAdobeDCT();

        // TODO: FixMe
        if (types == null || !types.hasNext() || adobeDCT != null && adobeDCT.getTransform() == AdobeDCT.YCCK || profile != null && profile.getColorSpaceType() == ColorSpace.TYPE_CMYK) {
            return Arrays.asList(
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR),
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB),
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR)

                    // TODO: We can/should also read and return it as CMYK if the source image is CMYK..
                    // + original color profile should be an option

            ).iterator();
        }

        return types;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        // TODO: Implement something better, so we don't return null for CMYK images + fixes the "Inconsistent metadata" issue
        return delegate.getRawImageType(imageIndex);
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        // JPEGSegmentImageInputStream that filters out/skips bad/unnecessary segments
        delegate.setInput(imageInput != null ? new JPEGSegmentImageInputStream(imageInput) : null, seekForwardOnly, ignoreMetadata);
    }

    @Override
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        return delegate.isRandomAccessEasy(imageIndex);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        assertInput();
        checkBounds(imageIndex);

        // TODO: This test is not good enough for JDK7, which seems to have fixed some of the issues.
        // NOTE: We rely on the fact that unsupported images has no valid types. This is kind of hacky.
        // Might want to look into the metadata, to see if there's a better way to identify these.
        boolean unsupported = !delegate.getImageTypes(imageIndex).hasNext();

        ICC_Profile profile = getEmbeddedICCProfile();
        AdobeDCT adobeDCT = getAdobeDCT();

        if (delegate.canReadRaster() && (
                unsupported ||
                adobeDCT != null && adobeDCT.getTransform() == AdobeDCT.YCCK ||
                profile != null && (ColorSpaces.isOffendingColorProfile(profile) || profile.getColorSpaceType() == ColorSpace.TYPE_CMYK))) {
            if (DEBUG) {
                System.out.println("Reading using raster and extra conversion");
                System.out.println("ICC color profile = " + profile);
            }

            return readImageAsRasterAndReplaceColorProfile(imageIndex, param, ensureDisplayProfile(profile));
        }

        if (DEBUG) {
            System.out.println("Reading using delegate");
        }
        
        return delegate.read(imageIndex, param);
    }

    private BufferedImage readImageAsRasterAndReplaceColorProfile(int imageIndex, ImageReadParam param, ICC_Profile profile) throws IOException {
        int origWidth = getWidth(imageIndex);
        int origHeight = getHeight(imageIndex);

        ColorSpace srcCs = null;

        /*--------------------------------------------------------------------------------------------------------------

        From http://download.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html:

        "When reading, the contents of the stream are interpreted by the usual JPEG conventions, as follows:

        • If a JFIF APP0 marker segment is present, the colorspace is known to be either grayscale or YCbCr. If an APP2
        marker segment containing an embedded ICC profile is also present, then the YCbCr is converted to RGB according
        to the formulas given in the JFIF spec, and the ICC profile is assumed to refer to the resulting RGB space.

        • If an Adobe APP14 marker segment is present, the colorspace is determined by consulting the transform flag.
        The transform flag takes one of three values:
         o 2 - The image is encoded as YCCK (implicitly converted from CMYK on encoding).
         o 1 - The image is encoded as YCbCr (implicitly converted from RGB on encoding).
         o 0 - Unknown. 3-channel images are assumed to be RGB, 4-channel images are assumed to be CMYK.

        • If neither marker segment is present, the following procedure is followed: Single-channel images are assumed
        to be grayscale, and 2-channel images are assumed to be grayscale with an alpha channel. For 3- and 4-channel
        images, the component ids are consulted. If these values are 1-3 for a 3-channel image, then the image is
        assumed to be YCbCr. If these values are 1-4 for a 4-channel image, then the image is assumed to be YCbCrA. If
        these values are > 4, they are checked against the ASCII codes for 'R', 'G', 'B', 'A', 'C', 'c'.
        These can encode the following colorspaces:

        RGB
        RGBA
        YCC (as 'Y','C','c'), assumed to be PhotoYCC
        YCCA (as 'Y','C','c','A'), assumed to be PhotoYCCA

        Otherwise, 3-channel subsampled images are assumed to be YCbCr, 3-channel non-subsampled images are assumed to
        be RGB, 4-channel subsampled images are assumed to be YCCK, and 4-channel, non-subsampled images are assumed to
        be CMYK.

        • All other images are declared uninterpretable and an exception is thrown if an attempt is made to read one as
        a BufferedImage. Such an image may be read only as a Raster. If an image is interpretable but there is no Java
        ColorSpace available corresponding to the encoded colorspace (e.g. YCbCr), then ImageReader.getRawImageType
        will return null."

        --------------------------------------------------------------------------------------------------------------*/

        // TODO: Fix this algorithm to behave like above, except the presence of JFIF APP0 might mean YCbCr, gray *or CMYK*.
        // AdobeApp14 with transform either 1 or 2 can be trusted to be YCC/YCCK respectively, transform 0 means 1 component gray, 3 comp rgb, 4 comp cmyk

        SOF startOfFrame = getSOF();
        AdobeDCT adobeDCT = getAdobeDCT();

        Iterator<ImageTypeSpecifier> imageTypes = delegate.getImageTypes(imageIndex);
        int transform = adobeDCT != null ? adobeDCT.getTransform() : AdobeDCT.Unknown;

        // TODO: The !types.hasNext test is broken for JDK7, as it does return types...
        // CMYK Support, assuming the delegate reader can't decode, and any 4 component image is CMYK
        if ((!imageTypes.hasNext() || transform == AdobeDCT.YCCK || profile != null && profile.getColorSpaceType() == ColorSpace.TYPE_CMYK) && startOfFrame.componentsInFrame == 4) {
            // NOTE: Reading the metadata here chokes on some images. Instead, parse the Adobe App14 segment and read transform directly

            // TODO: If cmyk and no ICC profile, just use FastCMYKToRGB, without attempting loading Generic CMYK profile first?
            // TODO: Don't get generic CMYK if we already have a CMYK profile...
            srcCs = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);
            imageTypes = Arrays.asList(
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR),
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB),
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR)
                    // TODO: Only alpha if source has alpha... (ColorConvertOp chokes otherwise)
//                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB),
//                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE),
//                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR),
//                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE)
                    // TODO: Move to getImageTypes + add native color space if profile != null
                    ).iterator();
        }
        else if (!imageTypes.hasNext() && profile != null) {
            // TODO: Merge with above?
            srcCs = null;
            imageTypes = Arrays.asList(
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR),
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB),
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR)
                    ).iterator();
        }
        // ...else blow up as there's no possible types to decode into...

        BufferedImage image = getDestination(param, imageTypes, origWidth, origHeight);

//        System.err.println("JPEGImageReader.readImageAsRasterAndReplaceColorProfile: " + image);

        WritableRaster destination = image.getRaster();

        // TODO: checkReadParamBandSettings(param, );

        RasterOp convert = null;
        ICC_ColorSpace replacement = profile != null ? ColorSpaces.createColorSpace(profile) : null;

        if (profile != null && profile.getColorSpaceType() == ColorSpace.TYPE_GRAY && image.getColorModel().getColorSpace().getType() == ColorSpace.CS_GRAY) {
            // com.sun. reader does not do ColorConvertOp for CS_GRAY, even if embedded ICC profile,
            // probably because IJG native part does it already...? If applied, color looks wrong (too dark)...
            convert = new ColorConvertOp(srcCs, image.getColorModel().getColorSpace(), null);
        }
        else if (replacement != null) {
            // Handle inconsistencies
            if (startOfFrame.componentsInFrame != replacement.getNumComponents()) {
                if (startOfFrame.componentsInFrame < 4 && transform == AdobeDCT.YCCK) {
                    processWarningOccurred(String.format(
                            "Invalid Adobe App14 marker. Indicates YCCK/CMYK data, but SOF%d has %d color components. " +
                                    "Ignoring Adobe App14 marker, assuming YCC/RGB data.",
                            startOfFrame.marker & 0xf, startOfFrame.componentsInFrame
                    ));
                    transform = AdobeDCT.YCC;
                }

                // If ICC profile number of components and startOfFrame does not match, ignore ICC profile
                processWarningOccurred(String.format(
                        "Embedded ICC color profile is incompatible with image data. " +
                                "Profile indicates %d components, but SOF%d has %d color components. " +
                                "Ignoring ICC profile, assuming YCC/RGB data.",
                        replacement.getNumComponents(), startOfFrame.marker & 0xf, startOfFrame.componentsInFrame
                ));
                srcCs = null;
            }
            // NOTE: Avoid using CCOp if same color space, as it's more compatible that way
            else if (replacement != image.getColorModel().getColorSpace()) {
                // TODO: Use profiles instead of CS, if ICC profiles? Avoid creating expensive CS.
                convert = new ColorConvertOp(replacement, image.getColorModel().getColorSpace(), null);
            }
            // Else, pass through with no conversion
        }
        else if (srcCs != null) {
            if (!(srcCs instanceof ICC_ColorSpace) && image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB && srcCs.getType() == ColorSpace.TYPE_CMYK) {
                convert = new FastCMYKToRGB();
            }
            else {
                // TODO: Use profiles instead of CS, if ICC profiles? Avoid creating expensive CS.
                convert = new ColorConvertOp(srcCs, image.getColorModel().getColorSpace(), null);
            }
        }
//        else if (!image.getColorModel().getColorSpace().isCS_sRGB()) {
        // TODO: Need to handle case where src and dest differ still
//            convert = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), image.getColorModel().getColorSpace(), null);
//        }
        else if (profile != null) {
            processWarningOccurred("Embedded ICC color profile is incompatible with Java 2D, color profile will be ignored.");
        }

        // We'll need a read param
        Rectangle origSourceRegion;
        if (param == null) {
            param = delegate.getDefaultReadParam();
            origSourceRegion = null;
        }
        else {
            origSourceRegion = param.getSourceRegion();
        }

        Rectangle srcRegion = new Rectangle();
        Rectangle dstRegion = new Rectangle();
        computeRegions(param, origWidth, origHeight, image, srcRegion, dstRegion);

        // We're ready to go
        processImageStarted(imageIndex);

        // Unfortunately looping is slower than reading all at once, but
        // that requires 2 x + memory, so a few steps is an ok compromise I guess
        try {
            int srcCsType = srcCs != null ? srcCs.getType() : image.getColorModel().getColorSpace().getType();

            final int step = Math.max(1024, srcRegion.height / 10); // * param.getSourceYSubsampling(); // TODO: Using a multiple of 8 is probably a good idea for JPEG
            final int srcMaxY = srcRegion.y + srcRegion.height;
            int destY = dstRegion.y;

            for (int y = srcRegion.y; y < srcMaxY; y += step) {
                int scan = Math.min(step, srcMaxY - y);

                // Let the progress delegator handle progress, using corrected range
                progressDelegator.updateProgressRange(100f * (y + scan) / srcRegion.height);

                Rectangle subRegion = new Rectangle(srcRegion.x, y, srcRegion.width, scan);
                param.setSourceRegion(subRegion);
                Raster raster = delegate.readRaster(imageIndex, param); // non-converted

                // Apply source color conversion from implicit color space
                if ((transform == AdobeDCT.YCC || transform == AdobeDCT.Unknown) && srcCsType == ColorSpace.TYPE_RGB) {
                    YCbCrConverter.convertYCbCr2RGB(raster);
                }
                else if (transform == AdobeDCT.YCCK && srcCsType == ColorSpace.TYPE_CMYK) {
                    YCbCrConverter.convertYCCK2CMYK(raster);
                }
                else if (transform == AdobeDCT.Unknown && srcCsType == ColorSpace.TYPE_CMYK) {
                    invertCMYK(raster);
                }
                // ...else assume the raster is already converted

                int destHeight = Math.min(raster.getHeight(), dstRegion.height - destY); // Avoid off-by-one
                Raster src = raster.createChild(0, 0, raster.getWidth(), destHeight, 0, 0, param.getSourceBands());
                WritableRaster dest = destination.createWritableChild(dstRegion.x, destY, raster.getWidth(), destHeight, 0, 0, param.getDestinationBands());

                // Apply further color conversion for explicit color space, or just copy the pixels into place
                if (convert != null) {
                    convert.filter(src, dest);
                }
                else {
                    dest.setRect(0, 0, src);
                }

                destY += raster.getHeight();

                if (abortRequested()) {
                    processReadAborted();
                    break;
                }
            }
        }
        finally {
            // Restore normal read progress processing
            progressDelegator.resetProgressRange();

            // NOTE: Would be cleaner to clone the param, unfortunately it can't be done easily...
            param.setSourceRegion(origSourceRegion);
        }

        processImageComplete();

        return image;
    }

    private ICC_Profile ensureDisplayProfile(final ICC_Profile profile) {
        // NOTE: This is probably not the right way to do it... :-P
        // TODO: Consider moving method to ColorSpaces class or new class in imageio.color package

        // NOTE: Workaround for the ColorConvertOp treating the input as relative colorimetric,
        // if the FIRST profile has class OUTPUT, regardless of the actual rendering intent in that profile...
        // See ColorConvertOp#filter(Raster, WritableRaster)

        if (profile != null && profile.getProfileClass() != ICC_Profile.CLASS_DISPLAY) {
            byte[] profileData = profile.getData(); // Need to clone entire profile, due to a JDK 7 bug

            if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual) {
                intToBigEndian(ICC_Profile.icSigDisplayClass, profileData, ICC_Profile.icHdrDeviceClass); // Header is first

                return ICC_Profile.getInstance(profileData);
            }
        }

        return profile;
    }

    static void intToBigEndian(int value, byte[] array, int index) {
        array[index]   = (byte) (value >> 24);
        array[index+1] = (byte) (value >> 16);
        array[index+2] = (byte) (value >>  8);
        array[index+3] = (byte) (value);
    }

    private void initHeader() throws IOException {
        if (segments == null) {
            long start = DEBUG ? System.currentTimeMillis() : 0;

            readSegments();

            if (DEBUG) {
                System.out.println("Read metadata in " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }

    private void readSegments() throws IOException {
        imageInput.mark();

        try {
            imageInput.seek(0); // TODO: Seek to wanted image, skip images on the way

            segments = JPEGSegmentUtil.readSegments(imageInput, SEGMENT_IDENTIFIERS);
        }
        catch (IOException ignore) {
        }
        catch (IllegalArgumentException foo) {
            foo.printStackTrace();
        }
        finally {
            imageInput.reset();
        }
    }

    private CompoundDirectory getEXIFMetadata() throws IOException {
        List<JPEGSegment> exifSegments = getAppSegments(JPEG.APP1, "Exif");

        if (!exifSegments.isEmpty()) {
            JPEGSegment exif = exifSegments.get(0);
            InputStream data = exif.data();
            //noinspection ResultOfMethodCallIgnored
            data.read(); // Pad

            ImageInputStream stream = ImageIO.createImageInputStream(data);

            CompoundDirectory exifMetadata = (CompoundDirectory) new EXIFReader().read(stream);

            extractEXIFThumbnails(stream, exifMetadata);

            return exifMetadata;
        }

        return null;
    }

    private void extractEXIFThumbnails(ImageInputStream stream, CompoundDirectory exifMetadata) throws IOException {
        if (exifMetadata.directoryCount() == 2) {
            Directory ifd1 = exifMetadata.getDirectory(1);
            Entry compression = ifd1.getEntryById(TIFF.TAG_COMPRESSION);

            if (compression != null && compression.getValue().equals(1)) { // 1 = no compression
                // Read ImageWidth, ImageLength (height) and BitsPerSample (=8 8 8, always)
                // PhotometricInterpretation (2=RGB, 6=YCbCr), SamplesPerPixel (=3, always),
                Entry width = ifd1.getEntryById(TIFF.TAG_IMAGE_WIDTH);
                Entry height = ifd1.getEntryById(TIFF.TAG_IMAGE_HEIGHT);

                if (width == null || height == null) {
                    throw new IIOException("Missing dimensions for RAW EXIF thumbnail");
                }

                Entry bitsPerSample = ifd1.getEntryById(TIFF.TAG_BITS_PER_SAMPLE);
                Entry samplesPerPixel = ifd1.getEntryById(TIFF.TAG_SAMPLES_PER_PIXELS);
                Entry photometricInterpretation = ifd1.getEntryById(TIFF.TAG_PHOTOMETRIC_INTERPRETATION);

                // Required
                int w = ((Number) width.getValue()).intValue();
                int h = ((Number) height.getValue()).intValue();

                if (bitsPerSample != null) {
                    int[] bpp = (int[]) bitsPerSample.getValue();
                    if (!Arrays.equals(bpp, new int[] {8, 8, 8})) {
                        throw new IIOException("Unknown bits per sample for RAW EXIF thumbnail: " + bitsPerSample.getValueAsString());
                    }
                }

                if (samplesPerPixel != null && (Integer) samplesPerPixel.getValue() != 3) {
                    throw new IIOException("Unknown samples per pixel for RAW EXIF thumbnail: " + samplesPerPixel.getValueAsString());
                }

                int interpretation = photometricInterpretation != null ? ((Number) photometricInterpretation.getValue()).intValue() : 2;

                // IFD1 should contain strip offsets for uncompressed images
                Entry offset = ifd1.getEntryById(TIFF.TAG_STRIP_OFFSETS);
                if (offset != null) {
                    stream.seek(((Number) offset.getValue()).longValue());

                    // Read raw image data, either RGB or YCbCr
                    int thumbSize = w * h * 3;
                    byte[] thumbData = readFully(stream, thumbSize);

                    switch (interpretation) {
                        case 2:
                            // RGB
                            break;
                        case 6:
                            // YCbCr
                            for (int i = 0, thumbDataLength = thumbData.length; i < thumbDataLength; i += 3) {
                                YCbCrConverter.convertYCbCr2RGB(thumbData, thumbData, i);
                            }
                            break;
                        default:
                            throw new IIOException("Unknown photometric interpretation for RAW EXIF thumbnail: " + interpretation);
                    }

                    thumbnails.add(readRawThumbnail(thumbData, thumbData.length, 0, w, h));
                }
            }
            else if (compression == null || compression.getValue().equals(6)) { // 6 = JPEG compression
                // IFD1 should contain JPEG offset for JPEG thumbnail
                Entry jpegOffset = ifd1.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT);

                if (jpegOffset != null) {
                    stream.seek(((Number) jpegOffset.getValue()).longValue());
                    InputStream input = IIOUtil.createStreamAdapter(stream);

                    // For certain EXIF files (encoded with TIFF.TAG_YCBCR_POSITIONING = 2?), we need
                    // EXIF information to read the thumbnail correctly (otherwise the colors are messed up).

                    // HACK: Splice empty EXIF information into the thumbnail stream
                    byte[] fakeEmptyExif = {
                            // SOI (from original data)
                            (byte) input.read(), (byte) input.read(),
                            // APP1 + len (016) + 'Exif' + 0-term + pad
                            (byte) 0xFF, (byte) 0xE1, 0, 16, 'E', 'x', 'i', 'f', 0, 0,
                            // Big-endian BOM (MM), TIFF magic (042), offset (0000)
                            'M', 'M', 0, 42, 0, 0, 0, 0,
                    };
                    input = new SequenceInputStream(new ByteArrayInputStream(fakeEmptyExif), input);

                    BufferedImage exifThumb = ImageIO.read(input);

                    if (exifThumb != null) {
                        thumbnails.add(exifThumb);
                    }

                    input.close();
                }
            }
        }
    }

    private List<JPEGSegment> getAppSegments(final int marker, final String identifier) throws IOException {
        initHeader();

        List<JPEGSegment> appSegments = Collections.emptyList();

        for (JPEGSegment segment : segments) {
            if (segment.marker() == marker && (identifier == null || identifier.equals(segment.identifier()))) {
                if (appSegments == Collections.EMPTY_LIST) {
                    appSegments = new ArrayList<JPEGSegment>(segments.size());
                }

                appSegments.add(segment);
            }
        }

        return appSegments;
    }

    private SOF getSOF() throws IOException {
        for (JPEGSegment segment : segments) {
            if (JPEG.SOF0 >= segment.marker() && segment.marker() <= JPEG.SOF3 ||
                    JPEG.SOF5 >= segment.marker() && segment.marker() <= JPEG.SOF7 ||
                    JPEG.SOF9 >= segment.marker() && segment.marker() <= JPEG.SOF11 ||
                    JPEG.SOF13 >= segment.marker() && segment.marker() <= JPEG.SOF15) {

                DataInputStream data = new DataInputStream(segment.data());

                try {
                    int samplePrecision = data.readUnsignedByte();
                    int lines = data.readUnsignedShort();
                    int samplesPerLine = data.readUnsignedShort();
                    int componentsInFrame = data.readUnsignedByte();

                    SOFComponent[] components = new SOFComponent[componentsInFrame];

                    for (int i = 0; i < componentsInFrame; i++) {
                        int id = data.readUnsignedByte();
                        int sub = data.readUnsignedByte();
                        int qtSel = data.readUnsignedByte();

                        components[i] = new SOFComponent(id, ((sub & 0xF0) >> 4), (sub & 0xF), qtSel);
                    }

                    return new SOF(segment.marker(), samplePrecision, lines, samplesPerLine, componentsInFrame, components);
                }
                finally {
                    data.close();
                }
            }
        }

        return null;
    }

    private AdobeDCT getAdobeDCT() throws IOException {
        // TODO: Investigate http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6355567: 33/35 byte Adobe app14 markers
        List<JPEGSegment> adobe = getAppSegments(JPEG.APP14, "Adobe");

        if (!adobe.isEmpty()) {
            // version (byte), flags (4bytes), color transform (byte: 0=unknown, 1=YCC, 2=YCCK)
            DataInputStream stream = new DataInputStream(adobe.get(0).data());

            return new AdobeDCT(
                    stream.readUnsignedByte(),
                    stream.readUnsignedShort(),
                    stream.readUnsignedShort(),
                    stream.readUnsignedByte()
            );
        }

        return null;
    }

    private JFIF getJFIF() throws IOException{
        List<JPEGSegment> jfif = getAppSegments(JPEG.APP0, "JFIF");
        
        if (!jfif.isEmpty()) {
            JPEGSegment segment = jfif.get(0);
            DataInputStream stream = new DataInputStream(segment.data());
            
            int x, y;
            
            return new JFIF(
                stream.readUnsignedByte(),
                stream.readUnsignedByte(),
                stream.readUnsignedByte(),
                stream.readUnsignedShort(),
                stream.readUnsignedShort(),
                x = stream.readUnsignedByte(),
                y = stream.readUnsignedByte(),
                readFully(stream, x * y * 3)
            );
        }
        
        return null;
    }

    private JFXX getJFXX() throws IOException {
        List<JPEGSegment> jfxx = getAppSegments(JPEG.APP0, "JFXX");

        if (!jfxx.isEmpty()) {
            JPEGSegment segment = jfxx.get(0);

            DataInputStream stream = new DataInputStream(segment.data());

            return new JFXX(
                    stream.readUnsignedByte(),
                    readFully(stream, segment.length() - 1)
            );
        }

        return null;
    }


    private byte[] readFully(DataInput stream, int len) throws IOException {
        if (len == 0) {
            return null;
        }

        byte[] data = new byte[len];
        stream.readFully(data);
        return data;
    }

    private ICC_Profile getEmbeddedICCProfile() throws IOException {
        // ICC v 1.42 (2006) annex B:
        // APP2 marker (0xFFE2) + 2 byte length + ASCII 'ICC_PROFILE' + 0 (termination)
        // + 1 byte chunk number + 1 byte chunk count (allows ICC profiles chunked in multiple APP2 segments)
        List<JPEGSegment> segments = getAppSegments(JPEG.APP2, "ICC_PROFILE");

        if (segments.size() == 1) {
            // Faster code for the common case
            JPEGSegment segment = segments.get(0);
            DataInputStream stream = new DataInputStream(segment.data());
            int chunkNumber = stream.readUnsignedByte();
            int chunkCount = stream.readUnsignedByte();

            if (chunkNumber != 1 && chunkCount != 1) {
                throw new IIOException(String.format("Bad number of 'ICC_PROFILE' chunks."));
            }

            return ICC_Profile.getInstance(stream);
        }
        else if (!segments.isEmpty()) {
            // NOTE: This is probably over-complicated, as I've never encountered ICC_PROFILE chunks out of order...
            DataInputStream stream = new DataInputStream(segments.get(0).data());
            int chunkNumber = stream.readUnsignedByte();
            int chunkCount = stream.readUnsignedByte();

            // Some weird JPEGs use 0-based indexes... count == 0 and all numbers == 0.
            // Others use count == 1, and all numbers == 1.
            // Handle these by issuing warning
            boolean badICC = false;
            if (chunkNumber < 1) {
                badICC = true;
                processWarningOccurred("Unexpected ICC profile chunk index: " + chunkNumber + ". Ignoring indexes, assuming chunks are in sequence.");
            }
            if (chunkCount != segments.size()) {
                badICC = true;
                processWarningOccurred("Unexpected ICC profile chunk count: " + chunkCount + ". Ignoring count, assuming " + segments.size() + " chunks in sequence.");
            }

            int count = badICC ? segments.size() : chunkCount;
            InputStream[] streams = new InputStream[count];
            streams[badICC ? 0 : chunkNumber - 1] = stream;

            for (int i = 1; i < count; i++) {
                stream = new DataInputStream(segments.get(i).data());

                chunkNumber = stream.readUnsignedByte();

                if (!badICC && stream.readUnsignedByte() != chunkCount) {
                    throw new IIOException(String.format("Bad number of 'ICC_PROFILE' chunks."));
                }

                streams[badICC ? i : chunkNumber - 1] = stream;
            }

            return ICC_Profile.getInstance(new SequenceInputStream(Collections.enumeration(Arrays.asList(streams))));
        }

        return null;
    }

    @Override
    public boolean canReadRaster() {
        return delegate.canReadRaster();
    }

    @Override
    public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
        return delegate.readRaster(imageIndex, param);
    }

    @Override
    public RenderedImage readAsRenderedImage(int imageIndex, ImageReadParam param) throws IOException {
        return read(imageIndex, param);
    }

    @Override
    public void abort() {
        super.abort();

        delegate.abort();
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true; // We support EXIF thumbnails, even if no JFIF thumbnail is present
    }

    private void readThumbnailMetadata(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        if (thumbnails == null) {
            thumbnails = new ArrayList<BufferedImage>();

            JFIF jfif = getJFIF();
            if (jfif != null && jfif.thumbnail != null) {
                thumbnails.add(readRawThumbnail(jfif.thumbnail, jfif.thumbnail.length, 0, jfif.xThumbnail, jfif.yThumbnail));
            }

            JFXX jfxx = getJFXX();
            if (jfxx != null && jfxx.thumbnail != null) {
                switch (jfxx.extensionCode) {
                    case JFXX.JPEG:
                        thumbnails.add(ImageIO.read(new ByteArrayInputStream(jfxx.thumbnail)));
                        break;
                    case JFXX.INDEXED:
                        // 1 byte: xThumb
                        // 1 byte: yThumb
                        // 768 bytes: palette
                        // x * y bytes: 8 bit indexed pixels
                        int w = jfxx.thumbnail[0] & 0xff;
                        int h = jfxx.thumbnail[1] & 0xff;
                        
                        int[] rgbs = new int[256];
                        for (int i = 0; i < rgbs.length; i++) {
                            int rgb = (jfxx.thumbnail[3 * i] & 0xff) << 16
                                    | (jfxx.thumbnail[3 * i] & 0xff) << 8
                                    | (jfxx.thumbnail[3 * i] & 0xff);

                            rgbs[i] = rgb;
                        }

                        IndexColorModel icm = new InverseColorMapIndexColorModel(8, rgbs.length, rgbs, 0, false, -1, DataBuffer.TYPE_BYTE);
                        DataBufferByte buffer = new DataBufferByte(jfxx.thumbnail, jfxx.thumbnail.length - 770, 770);
                        WritableRaster raster = Raster.createPackedRaster(buffer, w, h, 8, null);

                        thumbnails.add(new BufferedImage(icm, raster, icm.isAlphaPremultiplied(), null));
                        break;
                    case JFXX.RGB:
                        // 1 byte: xThumb
                        // 1 byte: yThumb
                        // 3 * x * y bytes: 24 bit RGB pixels
                        w = jfxx.thumbnail[0] & 0xff;
                        h = jfxx.thumbnail[1] & 0xff;

                        thumbnails.add(readRawThumbnail(jfxx.thumbnail, jfxx.thumbnail.length - 2, 2, w, h));
                        break;
                    default:
                        processWarningOccurred("Unknown JFXX extension code: " + jfxx.extensionCode);
                }
            }

            // TODO: Ideally we want to decode image data in getThumbnail, less ideally here, but at least not in getEXIFMetadata()
            CompoundDirectory exifMetadata = getEXIFMetadata();
//            System.err.println("exifMetadata: " + exifMetadata);
//            if (exifMetadata != null && exifMetadata.directoryCount() >= 2) {
//                Directory ifd1 = exifMetadata.getDirectory(1);
//                if (ifd1.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT) != null) {
//                }
//            }
        }
    }

    // TODO: Candidate for util method
    private BufferedImage readRawThumbnail(final byte[] thumbnail, final int size, final int offset, int w, int h) {
        DataBufferByte buffer = new DataBufferByte(thumbnail, size, offset);
        WritableRaster raster = Raster.createInterleavedRaster(buffer, w, h, w * 3, 3, new int[] {0, 1, 2}, null);
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }

    @Override
    public int getNumThumbnails(final int imageIndex) throws IOException {
        readThumbnailMetadata(imageIndex);

        return thumbnails.size();
    }

    private void checkThumbnailBounds(int imageIndex, int thumbnailIndex) throws IOException {
        Validate.isTrue(thumbnailIndex >= 0, thumbnailIndex, "thumbnailIndex < 0; %d");
        Validate.isTrue(getNumThumbnails(imageIndex) > thumbnailIndex, thumbnailIndex, "thumbnailIndex >= numThumbnails; %d");
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        checkThumbnailBounds(imageIndex, thumbnailIndex);

        return thumbnails.get(thumbnailIndex).getWidth();
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        checkThumbnailBounds(imageIndex, thumbnailIndex);

        return thumbnails.get(thumbnailIndex).getHeight();
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        checkThumbnailBounds(imageIndex, thumbnailIndex);

        processThumbnailStarted(imageIndex, thumbnailIndex);
        // For now: Clone. TODO: Do the actual decoding/reading here.
        BufferedImage cached = thumbnails.get(thumbnailIndex);
        BufferedImage thumbnail = new BufferedImage(cached.getColorModel(), cached.copyData(null), cached.getColorModel().isAlphaPremultiplied(), null);
        processThumbnailProgress(100f);
        processThumbnailComplete();

        return thumbnail;
    }

    private static void invertCMYK(final Raster raster) {
        byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            data[i] = (byte) (255 - data[i] & 0xff);
        }
    }

    /**
     * Static inner class for lazy-loading of conversion tables.
     */
    static final class YCbCrConverter {
        /** Define tables for YCC->RGB color space conversion. */
        private final static int SCALEBITS = 16;
        private final static int MAXJSAMPLE = 255;
        private final static int CENTERJSAMPLE = 128;
        private final static int ONE_HALF = 1 << (SCALEBITS - 1);

        private final static int[] Cr_R_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_B_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cr_G_LUT = new int[MAXJSAMPLE + 1];
        private final static int[] Cb_G_LUT = new int[MAXJSAMPLE + 1];

        /**
         * Initializes tables for YCC->RGB color space conversion.
         */
        private static void buildYCCtoRGBtable() {
            if (DEBUG) {
                System.err.println("Building YCC conversion table");
            }

            for (int i = 0, x = -CENTERJSAMPLE; i <= MAXJSAMPLE; i++, x++) {
                // i is the actual input pixel value, in the range 0..MAXJSAMPLE
                // The Cb or Cr value we are thinking of is x = i - CENTERJSAMPLE
                // Cr=>R value is nearest int to 1.40200 * x
                Cr_R_LUT[i] = (int) ((1.40200 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF) >> SCALEBITS;
                // Cb=>B value is nearest int to 1.77200 * x
                Cb_B_LUT[i] = (int) ((1.77200 * (1 << SCALEBITS) + 0.5) * x + ONE_HALF) >> SCALEBITS;
                // Cr=>G value is scaled-up -0.71414 * x
                Cr_G_LUT[i] = -(int) (0.71414 * (1 << SCALEBITS) + 0.5) * x;
                // Cb=>G value is scaled-up -0.34414 * x
                // We also add in ONE_HALF so that need not do it in inner loop
                Cb_G_LUT[i] = -(int) ((0.34414) * (1 << SCALEBITS) + 0.5) * x + ONE_HALF;
            }
        }

        static {
            buildYCCtoRGBtable();
        }

        static void convertYCbCr2RGB(final Raster raster) {
            final int height = raster.getHeight();
            final int width = raster.getWidth();
            final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    convertYCbCr2RGB(data, data, (x + y * width) * 3);
                }
            }
        }

        private static void convertYCbCr2RGB(final byte[] yCbCr, final byte[] rgb, final int offset) {
            int y  = yCbCr[offset    ] & 0xff;
            int cr = yCbCr[offset + 2] & 0xff;
            int cb = yCbCr[offset + 1] & 0xff;

            rgb[offset    ] = clamp(y + Cr_R_LUT[cr]);
            rgb[offset + 1] = clamp(y + (Cb_G_LUT[cb] + Cr_G_LUT[cr] >> SCALEBITS));
            rgb[offset + 2] = clamp(y + Cb_B_LUT[cb]);
        }

        static void convertYCCK2CMYK(final Raster raster) {
            final int height = raster.getHeight();
            final int width = raster.getWidth();
            final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    convertYCCK2CMYK(data, data, (x + y * width) * 4);
                }
            }
        }

        private static void convertYCCK2CMYK(byte[] ycck, byte[] cmyk, int offset) {
            // Inverted
            int y  = 255 - ycck[offset    ] & 0xff;
            int cb = 255 - ycck[offset + 1] & 0xff;
            int cr = 255 - ycck[offset + 2] & 0xff;
            int k  = 255 - ycck[offset + 3] & 0xff;

            int cmykC = MAXJSAMPLE - (y + Cr_R_LUT[cr]);
            int cmykM = MAXJSAMPLE - (y + (Cb_G_LUT[cb] + Cr_G_LUT[cr] >> SCALEBITS));
            int cmykY = MAXJSAMPLE - (y + Cb_B_LUT[cb]);

            cmyk[offset    ] = clamp(cmykC);
            cmyk[offset + 1] = clamp(cmykM);
            cmyk[offset + 2] = clamp(cmykY);
            cmyk[offset + 3] = (byte) k; // K passes through unchanged
        }

        private static byte clamp(int val) {
            return (byte) Math.max(0, Math.min(255, val));
        }
    }

    private class ProgressDelegator extends ProgressListenerBase implements IIOReadUpdateListener, IIOReadWarningListener {
        float readProgressStart = -1;
        float readProgressStop = -1;

        void resetProgressRange() {
            readProgressStart = -1;
            readProgressStop = -1;
        }

        private boolean isProgressRangeCorrected() {
            return readProgressStart == -1 && readProgressStop == -1;
        }

        void updateProgressRange(float limit) {
            Validate.isTrue(limit >= 0, limit, "Negative range limit");

            readProgressStart = readProgressStop != -1 ? readProgressStop : 0;
            readProgressStop = limit;
        }

        @Override
        public void imageComplete(ImageReader source) {
            if (isProgressRangeCorrected()) {
                processImageComplete();
            }
        }

        @Override
        public void imageProgress(ImageReader source, float percentageDone) {
            if (isProgressRangeCorrected()) {
                processImageProgress(percentageDone);
            }
            else {
                processImageProgress(readProgressStart + (percentageDone * (readProgressStop - readProgressStart) / 100f));
            }
        }

        @Override
        public void imageStarted(ImageReader source, int imageIndex) {
            if (isProgressRangeCorrected()) {
                processImageStarted(imageIndex);
            }
        }

        @Override
        public void readAborted(ImageReader source) {
            if (isProgressRangeCorrected()) {
                processReadAborted();
            }
        }

        @Override
        public void sequenceComplete(ImageReader source) {
            processSequenceComplete();
        }

        @Override
        public void sequenceStarted(ImageReader source, int minIndex) {
            processSequenceStarted(minIndex);
        }

        @Override
        public void thumbnailComplete(ImageReader source) {
            processThumbnailComplete();
        }

        @Override
        public void thumbnailProgress(ImageReader source, float percentageDone) {
            processThumbnailProgress(percentageDone);
        }

        @Override
        public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
            processThumbnailStarted(imageIndex, thumbnailIndex);
        }

        public void passStarted(ImageReader source, BufferedImage theImage, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
            processPassStarted(theImage, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
        }

        public void imageUpdate(ImageReader source, BufferedImage theImage, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
            processImageUpdate(theImage, minX, minY, width, height, periodX, periodY, bands);
        }

        public void passComplete(ImageReader source, BufferedImage theImage) {
            processPassComplete(theImage);
        }

        public void thumbnailPassStarted(ImageReader source, BufferedImage theThumbnail, int pass, int minPass, int maxPass, int minX, int minY, int periodX, int periodY, int[] bands) {
            processThumbnailPassStarted(theThumbnail, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
        }

        public void thumbnailUpdate(ImageReader source, BufferedImage theThumbnail, int minX, int minY, int width, int height, int periodX, int periodY, int[] bands) {
            processThumbnailUpdate(theThumbnail, minX, minY, width, height, periodX, periodY, bands);
        }

        public void thumbnailPassComplete(ImageReader source, BufferedImage theThumbnail) {
            processThumbnailPassComplete(theThumbnail);
        }

        public void warningOccurred(ImageReader source, String warning) {
            processWarningOccurred(warning);
        }
    }

    private static class SOF {
        private final int marker;
        private final int samplePrecision;
        private final int lines;          // height
        private final int samplesPerLine; // width
        private final int componentsInFrame;
        private final SOFComponent[] components;

        public SOF(int marker, int samplePrecision, int lines, int samplesPerLine, int componentsInFrame, SOFComponent[] components) {
            this.marker = marker;
            this.samplePrecision = samplePrecision;
            this.lines = lines;
            this.samplesPerLine = samplesPerLine;
            this.componentsInFrame = componentsInFrame;
            this.components = components;
        }

        public int getMarker() {
            return marker;
        }

        public int getSamplePrecision() {
            return samplePrecision;
        }

        public int getLines() {
            return lines;
        }

        public int getSamplesPerLine() {
            return samplesPerLine;
        }

        public int getComponentsInFrame() {
            return componentsInFrame;
        }

        @Override
        public String toString() {
            return String.format(
                    "SOF%d[%04x, precision: %d, lines: %d, samples/line: %d, components: %s]",
                    marker & 0xf, marker, samplePrecision, lines, samplesPerLine, Arrays.toString(components)
            );
        }
    }

    private static class SOFComponent {
        private final int id;
        private final int hSub;
        private final int vSub;
        private final int qtSel;

        public SOFComponent(int id, int hSub, int vSub, int qtSel) {
            this.id = id;
            this.hSub = hSub;
            this.vSub = vSub;
            this.qtSel = qtSel;
        }

        @Override
        public String toString() {
            // Use id either as component number or component name, based on value
            Serializable idStr = (id >= 'a' && id <= 'z' || id >= 'A' && id <= 'Z') ? "'" + (char) id + "'" : id;
            return String.format("id: %s, sub: %d/%d, sel: %d", idStr, hSub, vSub, qtSel);
        }
    }

    private static class JFIF {
        private final int majorVersion;
        private final int minorVersion;
        private final int units;
        private final int xDensity;
        private final int yDensity;
        private final int xThumbnail;
        private final int yThumbnail;
        private final byte[] thumbnail;

        public JFIF(int majorVersion, int minorVersion, int units, int xDensity, int yDensity, int xThumbnail, int yThumbnail, byte[] thumbnail) {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.units = units;
            this.xDensity = xDensity;
            this.yDensity = yDensity;
            this.xThumbnail = xThumbnail;
            this.yThumbnail = yThumbnail;
            this.thumbnail = thumbnail;
        }

        @Override
        public String toString() {
            return String.format("JFIF v%d.%02d %dx%d %s (%s)", majorVersion, minorVersion, xDensity, yDensity, unitsAsString(), thumbnailToString());
        }

        private String unitsAsString() {
            switch (units) {
                case 0:
                    return "(aspect only)";
                case 1:
                    return "dpi";
                case 2:
                    return "dpcm";
                default:
                    return "(unknown unit)";
            }
        }

        private String thumbnailToString() {
            if (xThumbnail == 0 || yThumbnail == 0) {
                return "no thumbnail";
            }

            return String.format("thumbnail: %dx%d", xThumbnail, yThumbnail);
        }
    }

    private static class JFXX {
        public static final int JPEG = 0x10;
        public static final int INDEXED = 0x11;
        public static final int RGB  = 0x13;

        private final int extensionCode;
        private final byte[] thumbnail;

        public JFXX(int extensionCode, byte[] thumbnail) {
            this.extensionCode = extensionCode;
            this.thumbnail = thumbnail;
        }

        @Override
        public String toString() {
            return String.format("JFXX extension (%s thumb size: %d)", extensionAsString(), thumbnail.length);
        }

        private String extensionAsString() {
            switch (extensionCode) {
                case JPEG:
                    return "JPEG";
                case INDEXED:
                    return "Indexed";
                case RGB:
                    return "RGB";
                default:
                    return String.valueOf(extensionCode);
            }
        }
    }

    
    private static class AdobeDCT {
        public static final int Unknown = 0;
        public static final int YCC = 1;
        public static final int YCCK = 2;

        private final int version;
        private final int flags0;
        private final int flags1;
        private final int transform;

        public AdobeDCT(int version, int flags0, int flags1, int transform) {
            this.version = version; // 100 or 101
            this.flags0 = flags0;
            this.flags1 = flags1;
            this.transform = transform;
        }

        public int getVersion() {
            return version;
        }

        public int getFlags0() {
            return flags0;
        }

        public int getFlags1() {
            return flags1;
        }

        public int getTransform() {
            return transform;
        }

        @Override
        public String toString() {
            return String.format(
                    "AdobeDCT[ver: %d.%02d, flags: %s %s, transform: %d]",
                    getVersion() / 100, getVersion() % 100, Integer.toBinaryString(getFlags0()), Integer.toBinaryString(getFlags1()), getTransform()
            );
        }
    }

    protected static void showIt(final BufferedImage pImage, final String pTitle) {
        ImageReaderBase.showIt(pImage, pTitle);
    }

    public static void main(final String[] args) throws IOException {
        for (final String arg : args) {
            File file = new File(arg);

            ImageInputStream input = ImageIO.createImageInputStream(file);
            if (input == null) {
                System.err.println("Could not read file: " + file);
                continue;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                System.err.println("No reader for: " + file);
                continue;
            }

            ImageReader reader = readers.next();
//            System.err.println("Reading using: " + reader);

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
//            if (args.length > 1) {
//                int sub = Integer.parseInt(args[1]);
//                int sub = 4;
//                param.setSourceSubsampling(sub, sub, 0, 0);
//            }

                long start = System.currentTimeMillis();
                BufferedImage image = reader.read(0, param);
//                System.err.println("Read time: " + (System.currentTimeMillis() - start) + " ms");
//                System.err.println("image: " + image);


//            image = new ResampleOp(reader.getWidth(0) / 4, reader.getHeight(0) / 4, ResampleOp.FILTER_LANCZOS).filter(image, null);

//                int maxW = 1280;
//                int maxH = 800;
                int maxW = 400;
                int maxH = 400;
                if (image.getWidth() > maxW || image.getHeight() > maxH) {
                    start = System.currentTimeMillis();
                    float aspect = reader.getAspectRatio(0);
                    if (aspect >= 1f) {
                        image = ImageUtil.createResampled(image, maxW, Math.round(maxW / aspect), Image.SCALE_DEFAULT);
                    }
                    else {
                        image = ImageUtil.createResampled(image, Math.round(maxH * aspect), maxH, Image.SCALE_DEFAULT);
                    }
//                    System.err.println("Scale time: " + (System.currentTimeMillis() - start) + " ms");
                }

                showIt(image, String.format("Image: %s [%d x %d]", file.getName(), reader.getWidth(0), reader.getHeight(0)));

                try {
                    int numThumbnails = reader.getNumThumbnails(0);
                    for (int i = 0; i < numThumbnails; i++) {
                        BufferedImage thumbnail = reader.readThumbnail(0, i);
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
                System.err.println(file);
                t.printStackTrace();
            }
            finally {
                input.close();
            }
        }
    }
}
