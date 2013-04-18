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
 * with support for CMYK/YCCK JPEGs, non-standard color spaces,broken ICC profiles
 * and more.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author LUT-based YCbCR conversion by Werner Randelshofer
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReader.java,v 1.0 24.01.11 16.37 haraldk Exp$
 */
public class JPEGImageReader extends ImageReaderBase {
    // TODO: Fix the (stream) metadata inconsistency issues.
    // - Sun JPEGMetadata class does not (and can not be made to) support CMYK data.. We need to create all new metadata classes.. :-/

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

    private List<ThumbnailReader> thumbnails;

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
        Iterator<ImageTypeSpecifier> types = delegate.getImageTypes(imageIndex);
        JPEGColorSpace csType = getSourceCSType(getAdobeDCT(), getSOF());

        if (types == null || !types.hasNext() || csType == JPEGColorSpace.CMYK || csType == JPEGColorSpace.YCCK) {
            ArrayList<ImageTypeSpecifier> typeList = new ArrayList<ImageTypeSpecifier>();
            // Add the standard types, we can always convert to these
            typeList.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
            typeList.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
            typeList.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));

            // We also read and return CMYK if the source image is CMYK/YCCK + original color profile if present
            ICC_Profile profile = getEmbeddedICCProfile();

            if (csType == JPEGColorSpace.CMYK || csType == JPEGColorSpace.YCCK) {
                if (profile != null) {
                    typeList.add(ImageTypeSpecifier.createInterleaved(ColorSpaces.createColorSpace(profile), new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
                }

                typeList.add(ImageTypeSpecifier.createInterleaved(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
            }
            else if (csType == JPEGColorSpace.YCbCr || csType == JPEGColorSpace.RGB) {
                if (profile != null) {
                    typeList.add(ImageTypeSpecifier.createInterleaved(ColorSpaces.createColorSpace(profile), new int[] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false));
                }
            }
            else if (csType == JPEGColorSpace.YCbCrA || csType == JPEGColorSpace.RGBA) {
                // Prepend ARGB types
                typeList.addAll(0, Arrays.asList(
                        ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB),
                        ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR),
                        ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE),
                        ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE)
                ));

                if (profile != null) {
                    typeList.add(ImageTypeSpecifier.createInterleaved(ColorSpaces.createColorSpace(profile), new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, false, false));
                }
            }

            return typeList.iterator();
        }

        return types;
    }

    @Override
    public
    ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        // If delegate can determine the spec, we'll just go with that
        ImageTypeSpecifier rawType = delegate.getRawImageType(imageIndex);

        if (rawType != null) {
            return rawType;
        }

        // Otherwise, consult the image metadata
        JPEGColorSpace csType = getSourceCSType(getAdobeDCT(), getSOF());

        switch (csType) {
            case CMYK:
                // Create based on embedded profile if exists, or create from "Generic CMYK"
                ICC_Profile profile = getEmbeddedICCProfile();

                if (profile != null) {
                    return ImageTypeSpecifier.createInterleaved(ColorSpaces.createColorSpace(profile), new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);
                }

                return ImageTypeSpecifier.createInterleaved(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);
            default:
                // For other types, we probably can't give a proper type, return null
                return null;
        }
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

        // TODO: Probably something bogus here, as ICC profile isn't applied if reading through the delegate any more...
        // We need to apply ICC profile unless the profile is sRGB/default gray (whatever that is)
        // - or only filter out the bad ICC profiles in the JPEGSegmentImageInputStream.
        if (delegate.canReadRaster() && (
                unsupported ||
                adobeDCT != null && adobeDCT.getTransform() == AdobeDCT.YCCK ||
                profile != null && (ColorSpaces.isOffendingColorProfile(profile) || profile.getColorSpaceType() == ColorSpace.TYPE_CMYK))) {
            if (DEBUG) {
                System.out.println("Reading using raster and extra conversion");
                System.out.println("ICC color profile: " + profile);
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

        AdobeDCT adobeDCT = getAdobeDCT();
        SOF startOfFrame = getSOF();
        JPEGColorSpace csType = getSourceCSType(adobeDCT, startOfFrame);

        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        BufferedImage image = getDestination(param, imageTypes, origWidth, origHeight);
        WritableRaster destination = image.getRaster();

        // TODO: checkReadParamBandSettings(param, );

        RasterOp convert = null;
        ICC_ColorSpace intendedCS = profile != null ? ColorSpaces.createColorSpace(profile) : null;

        if (profile != null && (csType == JPEGColorSpace.Gray || csType == JPEGColorSpace.GrayA)) {
            // com.sun. reader does not do ColorConvertOp for CS_GRAY, even if embedded ICC profile,
            // probably because IJG native part does it already...? If applied, color looks wrong (too dark)...
//            convert = new ColorConvertOp(intendedCS, image.getColorModel().getColorSpace(), null);
        }
        else if (intendedCS != null) {
            // Handle inconsistencies
            if (startOfFrame.componentsInFrame != intendedCS.getNumComponents()) {
                if (startOfFrame.componentsInFrame < 4 && (csType == JPEGColorSpace.CMYK || csType == JPEGColorSpace.YCCK)) {
                    processWarningOccurred(String.format(
                            "Invalid Adobe App14 marker. Indicates YCCK/CMYK data, but SOF%d has %d color components. " +
                                    "Ignoring Adobe App14 marker, assuming YCbCr/RGB data.",
                            startOfFrame.marker & 0xf, startOfFrame.componentsInFrame
                    ));

                    csType = JPEGColorSpace.YCbCr;
                }
                else {
                    // If ICC profile number of components and startOfFrame does not match, ignore ICC profile
                    processWarningOccurred(String.format(
                            "Embedded ICC color profile is incompatible with image data. " +
                                    "Profile indicates %d components, but SOF%d has %d color components. " +
                                    "Ignoring ICC profile, assuming source color space %s.",
                            intendedCS.getNumComponents(), startOfFrame.marker & 0xf, startOfFrame.componentsInFrame, csType
                    ));
                }
            }
            // NOTE: Avoid using CCOp if same color space, as it's more compatible that way
            else if (intendedCS != image.getColorModel().getColorSpace()) {
                convert = new ColorConvertOp(intendedCS, image.getColorModel().getColorSpace(), null);
            }
            // Else, pass through with no conversion
        }
        else if (csType == JPEGColorSpace.YCCK || csType == JPEGColorSpace.CMYK) {
            ColorSpace cmykCS = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);

            if (cmykCS instanceof ICC_ColorSpace) {
                convert = new ColorConvertOp(cmykCS, image.getColorModel().getColorSpace(), null);
            }
            else {
                // ColorConvertOp using non-ICC CS is deadly slow, fall back to fast conversion instead
                convert = new FastCMYKToRGB();
            }
        }
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
        // that requires 2 x memory or more, so a few steps is an ok compromise I guess
        try {
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
                if (csType == JPEGColorSpace.YCbCr || csType == JPEGColorSpace.YCbCrA) {
                    YCbCrConverter.convertYCbCr2RGB(raster);
                }
                else if (csType == JPEGColorSpace.YCCK) {
                    YCbCrConverter.convertYCCK2CMYK(raster);
                }
                else if (csType == JPEGColorSpace.CMYK) {
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

    static JPEGColorSpace getSourceCSType(AdobeDCT adobeDCT, final SOF startOfFrame) throws IIOException {
        /*
        ADAPTED from http://download.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html:

        When reading, the contents of the stream are interpreted by the usual JPEG conventions, as follows:

        • If a JFIF APP0 marker segment is present, the colorspace is known to be either grayscale, YCbCr or CMYK.
        If an APP2 marker segment containing an embedded ICC profile is also present, then YCbCr is converted to RGB according
        to the formulas given in the JFIF spec, and the ICC profile is assumed to refer to the resulting RGB space.
        CMYK data is read as is, and the ICC profile is assumed to refer to the resulting CMYK space.

        • If an Adobe APP14 marker segment is present, the colorspace is determined by consulting the transform flag.
        The transform flag takes one of three values:
         o 2 - The image is encoded as YCCK (implicitly converted from CMYK on encoding).
         o 1 - The image is encoded as YCbCr (implicitly converted from RGB on encoding).
         o 0 - Unknown. 3-channel images are assumed to be RGB, 4-channel images are assumed to be CMYK.

        • If neither marker segment is present, the following procedure is followed: Single-channel images are assumed
        to be grayscale, and 2-channel images are assumed to be grayscale with an alpha channel. For 3- and 4-channel
        images, the component ids are consulted. If these values are 1-3 for a 3-channel image, then the image is
        assumed to be YCbCr. If these values are 1-4 for a 4-channel image, then the image is assumed to be YCbCrA. If
        these values are > 4, they are checked against the ASCII codes for 'R', 'G', 'B', 'A', 'C', 'c', 'M', 'Y', 'K'.
        These can encode the following colorspaces:

        RGB
        RGBA
        YCC (as 'Y','C','c'), assumed to be PhotoYCC
        YCCA (as 'Y','C','c','A'), assumed to be PhotoYCCA
        CMYK (as 'C', 'M', 'Y', 'K').

        Otherwise, 3-channel subsampled images are assumed to be YCbCr, 3-channel non-subsampled images are assumed to
        be RGB, 4-channel subsampled images are assumed to be YCCK, and 4-channel, non-subsampled images are assumed to
        be CMYK.

        • All other images are declared uninterpretable and an exception is thrown if an attempt is made to read one as
        a BufferedImage. Such an image may be read only as a Raster. If an image is interpretable but there is no Java
        ColorSpace available corresponding to the encoded colorspace (e.g. YCbCr/YCCK), then ImageReader.getRawImageType
        will return null.
        */

        if (adobeDCT != null) {
            switch (adobeDCT.getTransform()) {
                case AdobeDCT.YCC:
                    return JPEGColorSpace.YCbCr;
                case AdobeDCT.YCCK:
                    return JPEGColorSpace.YCCK;
                case AdobeDCT.Unknown:
                    if (startOfFrame.components.length == 1) {
                        return JPEGColorSpace.Gray;
                    }
                    else if (startOfFrame.components.length == 3) {
                        return JPEGColorSpace.RGB;
                    }
                    else if (startOfFrame.components.length == 4) {
                        return JPEGColorSpace.CMYK;
                    }
                    // Else fall through
                default:
            }
        }

        switch (startOfFrame.components.length) {
            case 1:
                return JPEGColorSpace.Gray;
            case 2:
                return JPEGColorSpace.GrayA;
            case 3:
                if (startOfFrame.components[0].id == 1 && startOfFrame.components[1].id == 2 && startOfFrame.components[2].id == 3) {
                    return JPEGColorSpace.YCbCr;
                }
                else if (startOfFrame.components[0].id == 'R' && startOfFrame.components[1].id == 'G' && startOfFrame.components[2].id == 'B') {
                    return JPEGColorSpace.RGB;
                }
                else if (startOfFrame.components[0].id == 'Y' && startOfFrame.components[1].id == 'C' && startOfFrame.components[2].id == 'c') {
                    return JPEGColorSpace.PhotoYCC;
                }
                else {
                    // if subsampled, YCbCr else RGB
                    for (SOFComponent component : startOfFrame.components) {
                        if (component.hSub != 1 || component.vSub != 1) {
                            return JPEGColorSpace.YCbCr;
                        }
                    }

                    return JPEGColorSpace.RGB;
                }
            case 4:
                if (startOfFrame.components[0].id == 1 && startOfFrame.components[1].id == 2 && startOfFrame.components[2].id == 3 && startOfFrame.components[3].id == 4) {
                    return JPEGColorSpace.YCbCrA;
                }
                else if (startOfFrame.components[0].id == 'R' && startOfFrame.components[1].id == 'G' && startOfFrame.components[2].id == 'B' && startOfFrame.components[3].id == 'A') {
                    return JPEGColorSpace.RGBA;
                }
                else if (startOfFrame.components[0].id == 'Y' && startOfFrame.components[1].id == 'C' && startOfFrame.components[2].id == 'c' && startOfFrame.components[3].id == 'A') {
                    return JPEGColorSpace.PhotoYCCA;
                }
                else if (startOfFrame.components[0].id == 'C' && startOfFrame.components[1].id == 'M' && startOfFrame.components[2].id == 'Y' && startOfFrame.components[3].id == 'K') {
                    return JPEGColorSpace.CMYK;
                }
                else if (startOfFrame.components[0].id == 'Y' && startOfFrame.components[1].id == 'C' && startOfFrame.components[2].id == 'c' && startOfFrame.components[3].id == 'K') {
                    return JPEGColorSpace.YCCK;
                }
                else {
                    // if subsampled, YCCK else CMYK
                    for (SOFComponent component : startOfFrame.components) {
                        if (component.hSub != 1 || component.vSub != 1) {
                            return JPEGColorSpace.YCCK;
                        }
                    }

                    return JPEGColorSpace.CMYK;
                }
            default:
                throw new IIOException("Cannot determine source color space");
        }
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
            segments = Collections.emptyList();
        }
        catch (IllegalArgumentException foo) {
            foo.printStackTrace();
        }
        finally {
            imageInput.reset();
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
        // TODO: Investigate http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6355567: 33/35 byte Adobe APP14 markers
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

    private JFIFSegment getJFIF() throws IOException{
        List<JPEGSegment> jfif = getAppSegments(JPEG.APP0, "JFIF");
        
        if (!jfif.isEmpty()) {
            JPEGSegment segment = jfif.get(0);
            return JFIFSegment.read(segment.data());
        }
        
        return null;
    }

    private JFXXSegment getJFXX() throws IOException {
        List<JPEGSegment> jfxx = getAppSegments(JPEG.APP0, "JFXX");

        if (!jfxx.isEmpty()) {
            JPEGSegment segment = jfxx.get(0);
            return JFXXSegment.read(segment.data(), segment.length());
        }

        return null;
    }

    // TODO: Util method?
    static byte[] readFully(DataInput stream, int len) throws IOException {
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
                processWarningOccurred(String.format("Bad number of 'ICC_PROFILE' chunks: %d of %d. Assuming single chunk.", chunkNumber, chunkCount));
            }

            return readICCProfileSafe(stream);
        }
        else if (!segments.isEmpty()) {
            // NOTE: This is probably over-complicated, as I've never encountered ICC_PROFILE chunks out of order...
            DataInputStream stream = new DataInputStream(segments.get(0).data());
            int chunkNumber = stream.readUnsignedByte();
            int chunkCount = stream.readUnsignedByte();

            boolean badICC = false;
            if (chunkCount != segments.size()) {
                // Some weird JPEGs use 0-based indexes... count == 0 and all numbers == 0.
                // Others use count == 1, and all numbers == 1.
                // Handle these by issuing warning
                badICC = true;
                processWarningOccurred(String.format("Unexpected 'ICC_PROFILE' chunk count: %d. Ignoring count, assuming %d chunks in sequence.", chunkCount, segments.size()));
            }

            if (!badICC && chunkNumber < 1) {
                // Anything else is just ignored
                processWarningOccurred(String.format("Invalid 'ICC_PROFILE' chunk index: %d. Ignoring ICC profile.", chunkNumber));
                return null;
            }

            int count = badICC ? segments.size() : chunkCount;
            InputStream[] streams = new InputStream[count];
            streams[badICC ? 0 : chunkNumber - 1] = stream;

            for (int i = 1; i < count; i++) {
                stream = new DataInputStream(segments.get(i).data());

                chunkNumber = stream.readUnsignedByte();

                if (!badICC && stream.readUnsignedByte() != chunkCount) {
                    throw new IIOException(String.format("Bad number of 'ICC_PROFILE' chunks: %d of %d.", chunkNumber, chunkCount));
                }

                streams[badICC ? i : chunkNumber - 1] = stream;
            }

            return readICCProfileSafe(new SequenceInputStream(Collections.enumeration(Arrays.asList(streams))));
        }

        return null;
    }

    private ICC_Profile readICCProfileSafe(final InputStream stream) throws IOException {
        try {
            return ICC_Profile.getInstance(stream);
        }
        catch (RuntimeException e) {
            // NOTE: Throws either IllegalArgumentException or CMMException, depending on platform.
            // Usual reason: Broken tools store truncated ICC profiles in a single ICC_PROFILE chunk...
            processWarningOccurred(String.format("Bad 'ICC_PROFILE' chunk(s): %s. Ignoring ICC profile.", e.getMessage()));
            return null;
        }
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
        return true; // We support EXIF, JFIF and JFXX style thumbnails
    }

    private void readThumbnailMetadata(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        if (thumbnails == null) {
            thumbnails = new ArrayList<ThumbnailReader>();
            ThumbnailReadProgressListener thumbnailProgressDelegator = new ThumbnailProgressDelegate();

            // Read JFIF thumbnails if present
            JFIFSegment jfif = getJFIF();
            if (jfif != null && jfif.thumbnail != null) {
                thumbnails.add(new JFIFThumbnailReader(thumbnailProgressDelegator, imageIndex, thumbnails.size(), jfif));
            }

            // Read JFXX thumbnails if present
            JFXXSegment jfxx = getJFXX();
            if (jfxx != null && jfxx.thumbnail != null) {
                switch (jfxx.extensionCode) {
                    case JFXXSegment.JPEG:
                    case JFXXSegment.INDEXED:
                    case JFXXSegment.RGB:
                        thumbnails.add(new JFXXThumbnailReader(thumbnailProgressDelegator, imageIndex, thumbnails.size(), jfxx));
                        break;
                    default:
                        processWarningOccurred("Unknown JFXX extension code: " + jfxx.extensionCode);
                }
            }

            // Read Exif thumbnails if present
            List<JPEGSegment> exifSegments = getAppSegments(JPEG.APP1, "Exif");
            if (!exifSegments.isEmpty()) {
                JPEGSegment exif = exifSegments.get(0);
                InputStream data = exif.data();

                if (data.read() == -1) {
                    // Pad
                    processWarningOccurred("Exif chunk has no data.");
                }
                else {
                    ImageInputStream stream = ImageIO.createImageInputStream(data);
                    CompoundDirectory exifMetadata = (CompoundDirectory) new EXIFReader().read(stream);

                    if (exifMetadata.directoryCount() == 2) {
                        Directory ifd1 = exifMetadata.getDirectory(1);

                        Entry compression = ifd1.getEntryById(TIFF.TAG_COMPRESSION);

                        // 1 = no compression, 6 = JPEG compression (default)
                        if (compression == null || compression.getValue().equals(1) || compression.getValue().equals(6)) {
                            thumbnails.add(new EXIFThumbnailReader(thumbnailProgressDelegator, 0, thumbnails.size(), ifd1, stream));
                        }
                        else {
                            processWarningOccurred("EXIF IFD with unknown compression (expected 1 or 6): " + compression.getValue());
                        }
                    }
                }
            }
        }
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

        return thumbnails.get(thumbnailIndex).read();
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

        static void convertYCbCr2RGB(final byte[] yCbCr, final byte[] rgb, final int offset) {
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

    private class ThumbnailProgressDelegate implements ThumbnailReadProgressListener {
        public void processThumbnailStarted(int imageIndex, int thumbnailIndex) {
            JPEGImageReader.this.processThumbnailStarted(imageIndex, thumbnailIndex);
        }

        public void processThumbnailProgress(float percentageDone) {
            JPEGImageReader.this.processThumbnailProgress(percentageDone);
        }

        public void processThumbnailComplete() {
            JPEGImageReader.this.processThumbnailComplete();
        }
    }

    private static class SOF {
        private final int marker;
        private final int samplePrecision;
        private final int lines;          // height
        private final int samplesPerLine; // width
        private final int componentsInFrame;
        final SOFComponent[] components;

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
        final int id;
        final int hSub;
        final int vSub;
        final int qtSel;

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
