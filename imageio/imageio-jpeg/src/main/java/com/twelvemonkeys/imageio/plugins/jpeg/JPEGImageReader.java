/*
 * Copyright (c) 2011, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.color.YCbCrConverter;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.xml.XMLSerializer;

import javax.imageio.*;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.*;
import java.util.List;
import java.util.*;

/**
 * A JPEG {@code ImageReader} implementation based on the JRE {@code JPEGImageReader},
 * that adds support and properly handles cases where the JRE version throws exceptions.
 * <br>
 * Main features:
 * <ul>
 * <li>Support for YCbCr JPEGs without JFIF segment (converted to RGB, using the embedded ICC profile if applicable)</li>
 * <li>Support for CMYK JPEGs (converted to RGB by default or as CMYK, using the embedded ICC profile if applicable)</li>
 * <li>Support for Adobe YCCK JPEGs (converted to RGB by default or as CMYK, using the embedded ICC profile if applicable)</li>
 * <li>Support for JPEGs containing ICC profiles with interpretation other than 'Perceptual' (profile is assumed to be 'Perceptual' and used)</li>
 * <li>Support for JPEGs containing ICC profiles with class other than 'Display' (profile is assumed to have class 'Display' and used)</li>
 * <li>Support for JPEGs containing ICC profiles that are incompatible with stream data (image data is read, profile is ignored)</li>
 * <li>Support for JPEGs with corrupted ICC profiles (image data is read, profile is ignored)</li>
 * <li>Support for JPEGs with corrupted {@code ICC_PROFILE} segments (image data is read, profile is ignored)</li>
 * <li>Support for JPEGs using non-standard color spaces, unsupported by Java 2D (image data is read, profile is ignored)</li>
 * <li>Issues warnings instead of throwing exceptions in cases of corrupted data where ever the image data can still be read in a reasonable way</li>
 * </ul>
 * Thumbnail support:
 * <ul>
 * <li>Support for JFIF thumbnails (even if stream contains inconsistent metadata)</li>
 * <li>Support for JFXX thumbnails (JPEG, Indexed and RGB)</li>
 * <li>Support for EXIF thumbnails (JPEG, RGB and YCbCr)</li>
 * </ul>
 * Metadata support:
 * <ul>
 * <li>Support for JPEG metadata in both standard and native formats (even if stream contains inconsistent metadata)</li>
 * <li>Support for {@code javax_imageio_jpeg_image_1.0} format (currently as native format, may change in the future)</li>
 * <li>Support for illegal combinations of JFIF, Exif and Adobe markers, using "unknown" segments in the
 * "MarkerSequence" tag for the unsupported segments (for {@code javax_imageio_jpeg_image_1.0} format)</li>
 * </ul>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author LUT-based YCbCR conversion by Werner Randelshofer
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReader.java,v 1.0 24.01.11 16.37 haraldk Exp$
 */
public final class JPEGImageReader extends ImageReaderBase {
    // TODO: Allow automatic rotation based on EXIF rotation field?
    // TODO: Create a simplified native metadata format that is closer to the actual JPEG stream AND supports EXIF in a sensible way
    // TODO: As we already parse the SOF segments, maybe we should stop delegating getWidth/getHeight etc?

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.jpeg.debug"));

    /** Internal constant for referring all APP segments */
    static final int ALL_APP_MARKERS = -1;

    /** Our JPEG reading delegate */
    private final ImageReader delegate;

    /** Listens to progress updates in the delegate, and delegates back to this instance */
    private final ProgressDelegator progressDelegator;

    /** Extra delegate for reading JPEG encoded thumbnails */
    private ImageReader thumbnailReader;
    private List<ThumbnailReader> thumbnails;

    /** Cached list of JPEG segments we filter from the underlying stream */
    private List<Segment> segments;

    private int currentStreamIndex = 0;
    private final List<Long> streamOffsets = new ArrayList<>();

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

        currentStreamIndex = 0;
        streamOffsets.clear();

        segments = null;
        thumbnails = null;

        if (thumbnailReader != null) {
            thumbnailReader.reset();
        }

        installListeners();
    }

    @Override
    public void dispose() {
        super.dispose();

        if (thumbnailReader != null) {
            thumbnailReader.dispose();
            thumbnailReader = null;
        }

        delegate.dispose();
    }

    @Override
    public String getFormatName() throws IOException {
        return delegate.getFormatName();
    }

    private boolean isLossless() throws IOException {
        assertInput();

        return getSOF().marker == JPEG.SOF3;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        initHeader(imageIndex);

        return getSOF().samplesPerLine;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        initHeader(imageIndex);

        return getSOF().lines;
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        ImageTypeSpecifier rawImageType = getRawImageType(imageIndex);
        ColorModel rawColorModel = rawImageType.getColorModel();
        JPEGColorSpace sourceCSType = getSourceCSType(getJFIF(), getAdobeDCT(), getSOF());

        Set<ImageTypeSpecifier> types = new LinkedHashSet<>();

        if (rawColorModel.getColorSpace().getType() != ColorSpace.TYPE_GRAY) {
            // Add the standard types, we can always convert to these, except for gray
            if (rawColorModel.hasAlpha()) {
                types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB));
                types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR));
                types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE));
                types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE));
            }

            types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
            types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
            types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));
        }

        types.add(rawImageType);

        // If the source type has a luminance (Y) component, we can also convert to gray
        if (sourceCSType != JPEGColorSpace.RGB && sourceCSType != JPEGColorSpace.RGBA && sourceCSType != JPEGColorSpace.CMYK) {
            if (rawColorModel.hasAlpha()) {
                types.add(ImageTypeSpecifiers.createGrayscale(8, DataBuffer.TYPE_BYTE, false));
            }

            types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
        }

        return types.iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        initHeader(imageIndex);

        // Consult the image metadata
        JPEGColorSpace csType = getSourceCSType(getJFIF(), getAdobeDCT(), getSOF());
        ICC_Profile profile = getEmbeddedICCProfile(false);

        ColorSpace cs;
        boolean hasAlpha = false;

        switch (csType) {
            case GrayA:
                hasAlpha = true;
            case Gray:
                // Create based on embedded profile if exists, otherwise create from Gray
                cs = profile != null && profile.getNumComponents() == 1
                     ? ColorSpaces.createColorSpace(profile)
                     : ColorSpaces.getColorSpace(ColorSpace.CS_GRAY);
                return ImageTypeSpecifiers.createInterleaved(cs, hasAlpha ? new int[] {1, 0} : new int[] {0}, DataBuffer.TYPE_BYTE, hasAlpha, false);

            case YCbCrA:
            case RGBA:
            case PhotoYCCA:
                hasAlpha = true;
            case YCbCr:
            case RGB:
            case PhotoYCC:
                // Create based on PhotoYCC profile...
                if (csType == JPEGColorSpace.PhotoYCC || csType == JPEGColorSpace.PhotoYCCA) {
                    cs = ColorSpaces.getColorSpace(ColorSpace.CS_PYCC);
                }
                else {
                    // ...or create based on embedded profile if exists, otherwise create from sRGB
                    cs = profile != null && profile.getNumComponents() == 3
                         ? ColorSpaces.createColorSpace(profile)
                         : ColorSpaces.getColorSpace(ColorSpace.CS_sRGB);
                }

                return ImageTypeSpecifiers.createInterleaved(cs, hasAlpha ? new int[] {3, 2, 1, 0} : new int[] {2, 1, 0}, DataBuffer.TYPE_BYTE, hasAlpha, false);

            case YCCK:
            case CMYK:
                // Create based on embedded profile if exists, otherwise create from "Generic CMYK"
                cs = profile != null && profile.getNumComponents() == 4
                     ? ColorSpaces.createColorSpace(profile)
                     : ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);

                return ImageTypeSpecifiers.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);

            default:
                // For other types, we probably can't give a proper type
                throw new IIOException("Could not determine JPEG source color space");
        }
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        initHeader(imageIndex);

        Frame sof = getSOF();
        ICC_Profile profile = getEmbeddedICCProfile(false);
        AdobeDCT adobeDCT = getAdobeDCT();
        boolean bogusAdobeDCT = false;

        if (adobeDCT != null && (adobeDCT.transform == AdobeDCT.YCC && sof.componentsInFrame() != 3 ||
                adobeDCT.transform == AdobeDCT.YCCK && sof.componentsInFrame() != 4)) {
            processWarningOccurred(String.format(
                    "Invalid Adobe App14 marker. Indicates %s data, but SOF%d has %d color component(s). " +
                            "Ignoring Adobe App14 marker.",
                    adobeDCT.transform == AdobeDCT.YCCK ? "YCCK/CMYK" : "YCC/RGB",
                    sof.marker & 0xf, sof.componentsInFrame()
            ));

            bogusAdobeDCT = true;
            adobeDCT = null;
        }

        JFIF jfif = getJFIF();
        JPEGColorSpace sourceCSType = getSourceCSType(jfif, adobeDCT, sof);

        if (sof.marker == JPEG.SOF3) {
            // Read image as lossless
            if (DEBUG) {
                System.out.println("Reading using Lossless decoder");
            }

            // TODO: What about stream position?
            // TODO: Param handling: Source region, offset, subsampling, destination, destination type, etc....
            BufferedImage bufferedImage = new JPEGLosslessDecoderWrapper(this).readImage(segments, imageInput);

            // TODO: This is QnD, move param handling to lossless wrapper
            // TODO: Create test!
            BufferedImage destination = param != null ? param.getDestination() : null;
            if (destination != null) {
                destination.getRaster().setDataElements(0, 0, bufferedImage.getRaster());
                return destination;
            }

            return bufferedImage;
        }

        // We need to apply ICC profile unless the profile is sRGB/default gray (whatever that is)
        // - or only filter out the bad ICC profiles in the JPEGSegmentImageInputStream.
        else if (bogusAdobeDCT
                || profile != null && !ColorSpaces.isCS_sRGB(profile)
                || (long) sof.lines * sof.samplesPerLine > Integer.MAX_VALUE
                || delegateCSTypeMismatch(jfif, adobeDCT, sof, sourceCSType)) {
            if (DEBUG) {
                System.out.println("Reading using raster and extra conversion");
                System.out.println("ICC color profile: " + profile);
            }

            return readImageAsRasterAndReplaceColorProfile(imageIndex, param, sof, sourceCSType, profile);
        }

        if (DEBUG) {
            System.out.println("Reading using delegate");
        }

        return delegate.read(0, param);
    }

    private boolean delegateCSTypeMismatch(final JFIF jfif, final AdobeDCT adobeDCT, final Frame startOfFrame, final JPEGColorSpace sourceCSType) throws IOException {
        switch (sourceCSType) {
            case GrayA:
            case RGBA:
            case YCbCrA:
            case PhotoYCC:
            case PhotoYCCA:
            case CMYK:
            case YCCK:
                // These are no longer supported by the delegate, we'll handle ourselves
                return true;
        }

        try {
            ImageTypeSpecifier rawImageType = delegate.getRawImageType(0);

            switch (sourceCSType) {
                case Gray:
                    return rawImageType == null || rawImageType.getColorModel().getColorSpace().getType() != ColorSpace.TYPE_GRAY;
                case YCbCr:
                    // NOTE: For backwards compatibility, null is allowed for YCbCr
                    if (rawImageType == null) {
                        return false;
                    }

                    //  If We have a JFIF, but with non-standard component Ids, the standard reader mistakes it for RGB
                    if (jfif != null && (startOfFrame.components[0].id != 1 || startOfFrame.components[1].id != 2 || startOfFrame.components[2].id != 3)) {
                        return true;
                    }
                    // Else, if we have no Adobe marker and no subsampling, the standard reader mistakes it for RGB
                    else if (adobeDCT == null
                            && (startOfFrame.components[0].id != 1 || startOfFrame.components[1].id != 2 || startOfFrame.components[2].id != 3)
                            && (startOfFrame.components[0].hSub == 1 || startOfFrame.components[0].vSub == 1
                            || startOfFrame.components[1].hSub == 1 || startOfFrame.components[1].vSub == 1
                            || startOfFrame.components[2].hSub == 1 || startOfFrame.components[2].vSub == 1)) {
                        return true;
                    }
                case RGB:
                    return rawImageType == null || rawImageType.getColorModel().getColorSpace().getType() != ColorSpace.TYPE_RGB;
                default:
                    // Probably needs special handling, but we don't know what to do...
                    return false;
            }
        }
        catch (IIOException | NullPointerException | ArrayIndexOutOfBoundsException | NegativeArraySizeException ignore) {
            // An exception here is a clear indicator we need to handle conversion
            return true;
        }
    }

    private BufferedImage readImageAsRasterAndReplaceColorProfile(int imageIndex, ImageReadParam param, Frame startOfFrame, JPEGColorSpace csType, ICC_Profile profile) throws IOException {
        int origWidth = getWidth(imageIndex);
        int origHeight = getHeight(imageIndex);

        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        // TODO: Avoid creating destination here, if possible (as it saves time and memory)
        // If YCbCr or RGB, we could instead create a BufferedImage around the converted raster directly.
        // If YCCK or CMYK, we could instead create a BufferedImage around the converted raster,
        // leaving the fourth band as alpha (or pretend it's not there, by creating a child raster).
        BufferedImage image = getDestination(param, imageTypes, origWidth, origHeight);
        WritableRaster destination = image.getRaster();

        // TODO: checkReadParamBandSettings(param, );

        RasterOp convert = null;
        ICC_ColorSpace intendedCS = profile != null ? ColorSpaces.createColorSpace(profile) : null;

        if (destination.getNumBands() <= 2 && (csType != JPEGColorSpace.Gray && csType != JPEGColorSpace.GrayA)) {
            convert = new LuminanceToGray();
        }
        else if (profile != null && (csType == JPEGColorSpace.Gray || csType == JPEGColorSpace.GrayA)) {
            // com.sun. reader does not do ColorConvertOp for CS_GRAY, even if embedded ICC profile,
            // probably because IJG native part does it already...? If applied, color looks wrong (too dark)...
//            convert = new ColorConvertOp(intendedCS, image.getColorModel().getColorSpace(), null);
        }
        else if (intendedCS != null) {
            // Handle inconsistencies
            if (startOfFrame.componentsInFrame() != intendedCS.getNumComponents()) {
                // If ICC profile number of components and startOfFrame does not match, ignore ICC profile
                processWarningOccurred(String.format("Embedded ICC color profile is incompatible with image data. " +
                                "Profile indicates %d components, but SOF%d has %d color components. " +
                                "Ignoring ICC profile, assuming source color space %s.",
                        intendedCS.getNumComponents(), startOfFrame.marker & 0xf, startOfFrame.componentsInFrame(), csType
                ));

                if (csType == JPEGColorSpace.CMYK && image.getColorModel().getColorSpace().getType() != ColorSpace.TYPE_CMYK) {
                    convert = new ColorConvertOp(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), image.getColorModel().getColorSpace(), null);
                }
            }
            // NOTE: Avoid using CCOp if same color space, as it's more compatible that way
            else if (intendedCS != image.getColorModel().getColorSpace()) {
                if (DEBUG) {
                    System.err.println("Converting from " + intendedCS + " to " + (image.getColorModel().getColorSpace().isCS_sRGB() ? "sRGB" : image.getColorModel().getColorSpace()));
                }

                convert = new ColorConvertOp(intendedCS, image.getColorModel().getColorSpace(), null);
            }
            // Else, pass through with no conversion
        }
        else if (csType == JPEGColorSpace.YCCK || csType == JPEGColorSpace.CMYK) {
            ColorSpace cmykCS = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);

            if (cmykCS instanceof ICC_ColorSpace) {
                processWarningOccurred("No embedded ICC color profile, defaulting to \"generic\" CMYK ICC profile. Colors may look incorrect.");

                // NOTE: Avoid using CCOp if same color space, as it's more compatible that way
                if (cmykCS != image.getColorModel().getColorSpace()) {
                    convert = new ColorConvertOp(cmykCS, image.getColorModel().getColorSpace(), null);
                }
            }
            else {
                // ColorConvertOp using non-ICC CS is deadly slow, fall back to fast conversion instead
                processWarningOccurred("No embedded ICC color profile, will convert using inaccurate CMYK to RGB conversion. Colors may look incorrect.");

                convert = new FastCMYKToRGB();
            }
        }

        // We'll need a read param
        if (param == null) {
            param = delegate.getDefaultReadParam();
        }

        Rectangle origSourceRegion = param.getSourceRegion();

        Rectangle srcRegion = new Rectangle();
        Rectangle dstRegion = new Rectangle();
        computeRegions(param, origWidth, origHeight, image, srcRegion, dstRegion);

        // Need to undo the subsampling offset translations, as they are applied again in delegate.readRaster
        int gridX = param.getSubsamplingXOffset();
        int gridY = param.getSubsamplingYOffset();
        srcRegion.translate(-gridX, -gridY);
        srcRegion.width += gridX;
        srcRegion.height += gridY;

        // Unfortunately, reading the image in steps, is increasingly slower
        // for each iteration, so we'll read all at once.
        try {
            param.setSourceRegion(srcRegion);
            Raster raster = delegate.readRaster(0, param); // non-converted

            // Apply source color conversion from implicit color space
            if (csType == JPEGColorSpace.YCbCr) {
                convertYCbCr2RGB(raster, 3);
            }
            else if (csType == JPEGColorSpace.YCbCrA) {
                convertYCbCr2RGB(raster, 4);
            }
            else if (csType == JPEGColorSpace.YCCK) {
                // TODO: Need to rethink this (non-) inversion, see #147
                // TODO: Allow param to specify inversion, or possibly the PDF decode array
                // flag0 bit 15, blend = 1 see http://graphicdesign.stackexchange.com/questions/12894/cmyk-jpegs-extracted-from-pdf-appear-inverted
                convertYCCK2CMYK(raster);
            }
            else if (csType == JPEGColorSpace.CMYK) {
                invertCMYK(raster);
            }
            // ...else assume the raster is already converted

            WritableRaster dest = destination.createWritableChild(dstRegion.x, dstRegion.y, raster.getWidth(), raster.getHeight(), 0, 0, param.getDestinationBands());

            // Apply further color conversion for explicit color space, or just copy the pixels into place
            if (convert != null) {
                convert.filter(raster, dest);
            }
            else {
                dest.setRect(0, 0, raster);
            }
        }
        finally {
            // NOTE: Would be cleaner to clone the param, unfortunately it can't be done easily...
            param.setSourceRegion(origSourceRegion);
        }

        return image;
    }

    static JPEGColorSpace getSourceCSType(final JFIF jfif, final AdobeDCT adobeDCT, final Frame startOfFrame) throws IIOException {
        // Adapted from libjpeg jdapimin.c:
        // Guess the input colorspace
        // (Wish JPEG committee had provided a real way to specify this...)
        switch (startOfFrame.componentsInFrame()) {
            case 1:
                return JPEGColorSpace.Gray;
            case 2:
                return JPEGColorSpace.GrayA; // Java special case: Gray + Alpha
            case 3:
                if (jfif != null) {
                    return JPEGColorSpace.YCbCr; // JFIF implies YCbCr
                }
                else if (adobeDCT != null) {
                    switch (adobeDCT.transform) {
                        case AdobeDCT.Unknown:
                            return JPEGColorSpace.RGB;
                        default:
                            // TODO: Warning!
                        case AdobeDCT.YCC:
                            return JPEGColorSpace.YCbCr; // assume it's YCbCr
                    }
                }
                else {
                    // Saw no special markers, try to guess from the component IDs
                    int cid0 = startOfFrame.components[0].id;
                    int cid1 = startOfFrame.components[1].id;
                    int cid2 = startOfFrame.components[2].id;

                    if (cid0 == 1 && cid1 == 2 && cid2 == 3) {
                        return JPEGColorSpace.YCbCr; // assume JFIF w/out marker
                    }
                    else if (cid0 == 'R' && cid1 == 'G' && cid2 == 'B') {
                        return JPEGColorSpace.RGB; // ASCII 'R', 'G', 'B'
                    }
                    else if (cid0 == 'Y' && cid1 == 'C' && cid2 == 'c') {
                        return JPEGColorSpace.PhotoYCC; // Java special case: YCc
                    }
                    else {
                        // TODO: Warning!
                        return JPEGColorSpace.YCbCr; // assume it's YCbCr
                    }
                }

            case 4:
                if (adobeDCT != null) {
                    switch (adobeDCT.transform) {
                        case AdobeDCT.Unknown:
                            return JPEGColorSpace.CMYK;
                        default:
                            // TODO: Warning!
                        case AdobeDCT.YCCK:
                            return JPEGColorSpace.YCCK; // assume it's YCCK
                    }
                }
                else {
                    // Saw no special markers, try to guess from the component IDs
                    int cid0 = startOfFrame.components[0].id;
                    int cid1 = startOfFrame.components[1].id;
                    int cid2 = startOfFrame.components[2].id;
                    int cid3 = startOfFrame.components[3].id;

                    if (cid0 == 1 && cid1 == 2 && cid2 == 3 && cid3 == 4) {
                        return JPEGColorSpace.YCbCrA; // Java special case: YCbCrA
                    }
                    else if (cid0 == 'R' && cid1 == 'G' && cid2 == 'B' && cid3 == 'A') {
                        return JPEGColorSpace.RGBA; // Java special case: RGBA
                    }
                    else if (cid0 == 'Y' && cid1 == 'C' && cid2 == 'c' && cid3 == 'A') {
                        return JPEGColorSpace.PhotoYCCA; // Java special case: YCcA
                    }
                    else {
                        // TODO: Warning!
                        // No special markers, assume straight CMYK.
                        return JPEGColorSpace.CMYK;
                    }
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
            byte[] profileData = profile.getData(); // Need to clone entire profile, due to a OpenJDK bug

            if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual) {
                processWarningOccurred("ICC profile is Perceptual, ignoring, treating as Display class");

                intToBigEndian(ICC_Profile.icSigDisplayClass, profileData, ICC_Profile.icHdrDeviceClass); // Header is first

                return ICC_Profile.getInstance(profileData);
            }
        }

        return profile;
    }

    // TODO: Move to some common util
    static int intFromBigEndian(final byte[] array, final int index) {
        return ((array[index     ] & 0xff) << 24) |
                ((array[index + 1] & 0xff) << 16) |
                ((array[index + 2] & 0xff) <<  8) |
                ((array[index + 3] & 0xff)      );
    }

    // TODO: Move to some common util
    static void intToBigEndian(final int value, final byte[] array, final int index) {
        array[index    ] = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >>  8);
        array[index + 3] = (byte) (value      );
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        try {
            if (imageInput != null) {
                // Need to wrap stream to avoid messing with the byte order of the underlying stream
                // in the case we are operating as a delegate for ie. TIFFImageReader.
                if (!(imageInput instanceof SubImageInputStream)) {
                    imageInput = new SubImageInputStream(imageInput, Long.MAX_VALUE);
                }

                streamOffsets.add(imageInput.getStreamPosition());
            }

            initDelegate(seekForwardOnly, ignoreMetadata);
        }
        catch (IOException e) {
            // TODO: This should ideally be reported as an IOException, but I don't see how
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void initDelegate(boolean seekForwardOnly, boolean ignoreMetadata) throws IOException {
        // JPEGSegmentImageInputStream that filters out/skips bad/unnecessary segments
        delegate.setInput(imageInput != null
                          ? new JPEGSegmentImageInputStream(imageInput, new JPEGSegmentWarningDelegate())
                          : null, seekForwardOnly, ignoreMetadata);
    }

    private void initHeader() throws IOException {
        if (segments == null) {
            long start = DEBUG ? System.currentTimeMillis() : 0;

            // TODO: Consider just reading the segments directly, for better performance...
            List<JPEGSegment> jpegSegments = readSegments();

            List<Segment> segments = new ArrayList<>(jpegSegments.size());

            for (JPEGSegment segment : jpegSegments) {
                try (DataInputStream data = new DataInputStream(segment.segmentData())) {
                    segments.add(Segment.read(segment.marker(), segment.identifier(), segment.segmentLength(), data));
                }
                catch (IOException e) {
                    // TODO: Handle bad segments better, for now, just ignore any bad APP markers
                    if (segment.marker() >= JPEG.APP0 && JPEG.APP15 >= segment.marker()) {
                        processWarningOccurred("Bogus APP" + (segment.marker() & 0x0f) + "/" + segment.identifier() + " segment, ignoring");
                        continue;
                    }

                    throw e;
                }
            }

            this.segments = segments;

            if (DEBUG) {
                System.out.println("segments: " + segments);
                System.out.println("Read metadata in " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }

    private void initHeader(final int imageIndex) throws IOException {
        assertInput();
        if (imageIndex < 0) {
            throw new IndexOutOfBoundsException("imageIndex < 0: " + imageIndex);
        }

        if (imageIndex == currentStreamIndex) {
            initHeader();
            return;
        }

        gotoImage(imageIndex);

        // Reset segments and re-init the header
        segments = null;
        thumbnails = null;

        initDelegate(seekForwardOnly, ignoreMetadata);
        initHeader();
    }

    private void gotoImage(final int imageIndex) throws IOException {
        if (imageIndex < streamOffsets.size()) {
            imageInput.seek(streamOffsets.get(imageIndex));
        }
        else {
            long lastKnownSOIOffset = streamOffsets.get(streamOffsets.size() - 1);
            imageInput.seek(lastKnownSOIOffset);

            try {
                for (int i = streamOffsets.size() - 1; i < imageIndex; i++) {
                    long start = 0;

                    if (DEBUG) {
                        start = System.currentTimeMillis();
                        System.out.printf("Start seeking for image index %d%n", i + 1);
                    }

                    // Need to skip over segments, as they may contain JPEG markers (eg. JFXX or EXIF thumbnail)
                    JPEGSegmentUtil.readSegments(imageInput, Collections.<Integer, List<String>>emptyMap());

                    // Now, search for EOI and following SOI...
                    int marker;
                    while ((marker = imageInput.read()) != -1) {
                        if (marker == 0xFF && (0xFF00 | imageInput.readUnsignedByte()) == JPEG.EOI) {
                            // Found EOI, now the SOI should be nearby...
                            while ((marker = imageInput.read()) != -1) {
                                if (marker == 0xFF && (0xFF00 | imageInput.readUnsignedByte()) == JPEG.SOI) {
                                    long nextSOIOffset = imageInput.getStreamPosition() - 2;
                                    imageInput.seek(nextSOIOffset);
                                    streamOffsets.add(nextSOIOffset);

                                    break;
                                }
                            }

                            // ...or we may have missed it, but at least we tried
                            break;
                        }
                    }

                    if (DEBUG) {
                        System.out.printf("Seek in %d ms%n", System.currentTimeMillis() - start);
                    }
                }
            }
            catch (EOFException eof) {
                IndexOutOfBoundsException ioobe = new IndexOutOfBoundsException("Image index " + imageIndex + " not found in stream");
                ioobe.initCause(eof);
                throw ioobe;
            }

            if (imageIndex >= streamOffsets.size()) {
                throw new IndexOutOfBoundsException("Image index " + imageIndex + " not found in stream");
            }
        }

        currentStreamIndex = imageIndex;
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        assertInput();

        if (allowSearch) {
            if (seekForwardOnly) {
                throw new IllegalStateException("seekForwardOnly and allowSearch are both true");
            }

            int index = 0;
            int count = 0;
            while (true) {
                try {
                    gotoImage(index++);
                }
                catch (IndexOutOfBoundsException e) {
                    break;
                }

                // TODO: We should probably optimize this
                try {
                    segments = null;
                    getSOF(); // No SOF, no image
                    count++;
                }
                catch (IIOException ignore) {}
            }

            imageInput.seek(streamOffsets.get(currentStreamIndex));

            return count;
        }

        // We can't possibly know without searching
        return -1;
    }

    private List<JPEGSegment> readSegments() throws IOException {
        imageInput.mark();

        try {
            imageInput.seek(streamOffsets.get(currentStreamIndex));

            return JPEGSegmentUtil.readSegments(imageInput, JPEGSegmentUtil.ALL_SEGMENTS);
        }
        catch (IIOException | IllegalArgumentException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }
        finally {
            imageInput.reset();
        }

        // In case of an exception, avoid NPE when referencing segments later
        return Collections.emptyList();
    }

    List<Application> getAppSegments(final int marker, final String identifier) throws IOException {
        initHeader();

        List<Application> appSegments = Collections.emptyList();

        for (Segment segment : segments) {
            if (segment instanceof Application
                    && (marker == ALL_APP_MARKERS || marker == segment.marker)
                    && (identifier == null || identifier.equals(((Application) segment).identifier))) {
                if (appSegments == Collections.EMPTY_LIST) {
                    appSegments = new ArrayList<>(segments.size());
                }

                appSegments.add((Application) segment);
            }
        }

        return appSegments;
    }

    Frame getSOF() throws IOException {
        initHeader();

        for (Segment segment : segments) {
            if (segment instanceof Frame) {
                return (Frame) segment;
            }
        }

        throw new IIOException("No SOF segment in stream");
    }

    AdobeDCT getAdobeDCT() throws IOException {
        List<Application> adobe = getAppSegments(JPEG.APP14, "Adobe");
        return adobe.isEmpty() ? null : (AdobeDCT) adobe.get(0);
    }

    JFIF getJFIF() throws IOException{
        List<Application> jfif = getAppSegments(JPEG.APP0, "JFIF");
        return jfif.isEmpty() ? null : (JFIF) jfif.get(0);

    }

    JFXX getJFXX() throws IOException {
        List<Application> jfxx = getAppSegments(JPEG.APP0, "JFXX");
        return jfxx.isEmpty() ? null : (JFXX) jfxx.get(0);
    }

    private EXIF getExif() throws IOException {
        List<Application> exif = getAppSegments(JPEG.APP1, "Exif");
        return exif.isEmpty() ? null : (EXIF) exif.get(0); // TODO: Can there actually be more Exif segments?
    }

    private CompoundDirectory parseExif(final EXIF exif) throws IOException {
        if (exif != null) {
            // Identifier is "Exif\0" + 1 byte pad
            if (exif.data.length > exif.identifier.length() + 2) {
                try (ImageInputStream stream = exif.exifData()) {
                    return (CompoundDirectory) new TIFFReader().read(stream);
                }
                catch (IIOException e) {
                    processWarningOccurred("Exif chunk is present, but can't be read: " + e.getMessage());
                }
            }
            else {
                processWarningOccurred("Exif chunk has no data.");
            }
        }

        return null;
    }

    ICC_Profile getEmbeddedICCProfile(final boolean allowBadIndexes) throws IOException {
        // ICC v 1.42 (2006) annex B:
        // APP2 marker (0xFFE2) + 2 byte length + ASCII 'ICC_PROFILE' + 0 (termination)
        // + 1 byte chunk number + 1 byte chunk count (allows ICC profiles chunked in multiple APP2 segments)

        // TODO: Allow metadata to contain the wrongly indexed profiles, if readable
        // NOTE: We ignore any profile with wrong index for reading and image types, just to be on the safe side

        List<Application> segments = getAppSegments(JPEG.APP2, "ICC_PROFILE");

        // TODO: Possibly move this logic to the ICCProfile class...

        if (segments.size() == 1) {
            // Faster code for the common case
            Application segment = segments.get(0);
            DataInputStream stream = new DataInputStream(segment.data());
            int chunkNumber = stream.readUnsignedByte();
            int chunkCount = stream.readUnsignedByte();

            if (chunkNumber != 1 && chunkCount != 1) {
                processWarningOccurred(String.format("Unexpected number of 'ICC_PROFILE' chunks: %d of %d. Ignoring ICC profile.", chunkNumber, chunkCount));
                return null;
            }

            int segmentDataStart = segment.identifier.length() + 3; // ICC_PROFILE + null + chunk number + count
            int iccChunkDataSize = segment.data.length - segmentDataStart;
            int iccSize = segment.data.length < segmentDataStart + 4 ? 0 : intFromBigEndian(segment.data, segmentDataStart);

            return readICCProfileSafe(stream, allowBadIndexes, iccSize, iccChunkDataSize);
        }
        else if (!segments.isEmpty()) {
            // NOTE: This is probably over-complicated, as I've never encountered ICC_PROFILE chunks out of order...
            DataInputStream stream = new DataInputStream(segments.get(0).data());
            int chunkNumber = stream.readUnsignedByte();
            int chunkCount = stream.readUnsignedByte();

            // TODO: Most of the time the ICC profiles are readable and should be obtainable from metadata...
            boolean badICC = false;
            if (chunkCount != segments.size()) {
                // Some weird JPEGs use 0-based indexes... count == 0 and all numbers == 0.
                // Others use count == 1, and all numbers == 1.
                // Handle these by issuing warning
                processWarningOccurred(String.format("Bad 'ICC_PROFILE' chunk count: %d. Ignoring ICC profile.", chunkCount));
                badICC = true;

                if (!allowBadIndexes) {
                    return null;
                }
            }

            if (!badICC && chunkNumber < 1) {
                // Anything else is just ignored
                processWarningOccurred(String.format("Invalid 'ICC_PROFILE' chunk index: %d. Ignoring ICC profile.", chunkNumber));

                if (!allowBadIndexes) {
                    return null;
                }
            }

            int count = badICC ? segments.size() : chunkCount;
            InputStream[] streams = new InputStream[count];
            streams[badICC ? 0 : chunkNumber - 1] = stream;

            int iccChunkDataSize = 0;
            int iccSize = 0;

            for (int i = 1; i < count; i++) {
                Application segment = segments.get(i);
                stream = new DataInputStream(segment.data());

                chunkNumber = stream.readUnsignedByte();

                if (stream.readUnsignedByte() != chunkCount && !badICC) {
                    throw new IIOException(String.format("Bad number of 'ICC_PROFILE' chunks: %d of %d.", chunkNumber, chunkCount));
                }

                int index = badICC ? i : chunkNumber - 1;
                streams[index] = stream;

                int segmentDataStart = segment.identifier.length() + 3; // ICC_PROFILE + null + chunk number + count
                iccChunkDataSize += segment.data.length - segmentDataStart;
                if (index == 0) {
                    iccSize = intFromBigEndian(segment.data, segmentDataStart);
                }
            }

            return readICCProfileSafe(new SequenceInputStream(Collections.enumeration(Arrays.asList(streams))), allowBadIndexes, iccSize, iccChunkDataSize);
        }

        return null;
    }

    private ICC_Profile readICCProfileSafe(final InputStream stream, final boolean allowBadProfile, final int iccSize, final int iccChunkDataSize) throws IOException {
        if (iccSize < 0 || iccSize > iccChunkDataSize) {
            processWarningOccurred(String.format("Truncated 'ICC_PROFILE' chunk(s), size: %d. Ignoring ICC profile.", iccSize));
            return null;
        }

        try {
            ICC_Profile profile = ICC_Profile.getInstance(stream);

            // NOTE: Need to ensure we have a display profile *before* validating, for the caching to work
            return allowBadProfile ? profile : ColorSpaces.validateProfile(ensureDisplayProfile(profile));
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
    public Raster readRaster(final int imageIndex, final ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        initHeader(imageIndex);

        if (isLossless()) {
            // TODO: What about stream position?
            // TODO: Param handling: Reading as raster should support source region, subsampling etc.
            return new JPEGLosslessDecoderWrapper(this).readRaster(segments, imageInput);
        }

        try {
            return delegate.readRaster(0, param);
        }
        catch (IndexOutOfBoundsException knownIssue) {
            // com.sun.imageio.plugins.jpeg.JPEGBuffer doesn't do proper sanity check of input data.
            throw new IIOException("Corrupt JPEG data: Bad segment length", knownIssue);
        }
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
    public ImageReadParam getDefaultReadParam() {
        return delegate.getDefaultReadParam();
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true; // We support EXIF, JFIF and JFXX style thumbnails
    }

    private void readThumbnailMetadata(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        initHeader(imageIndex);

        if (thumbnails == null) {
            thumbnails = new ArrayList<>();

            // Read JFIF thumbnails if present
            try {
                ThumbnailReader thumbnail = JFIFThumbnail.from(getJFIF());
                if (thumbnail != null) {
                    thumbnails.add(thumbnail);
                }
            }
            catch (IOException e) {
                processWarningOccurred(e.getMessage());
            }

            // Read JFXX thumbnails if present
            try {
                ThumbnailReader thumbnail = JFXXThumbnail.from(getJFXX(), getThumbnailReader());
                if (thumbnail != null) {
                    thumbnails.add(thumbnail);
                }
            }
            catch (IOException e) {
                processWarningOccurred(e.getMessage());
            }

            // Read Exif thumbnails if present
            try {
                EXIF exif = getExif();
                ThumbnailReader thumbnailReader = EXIFThumbnail.from(exif, parseExif(exif), getThumbnailReader());
                if (thumbnailReader != null) {
                    thumbnails.add(thumbnailReader);
                }
            }
            catch (IOException e) {
                processWarningOccurred(e.getMessage());
            }
        }
    }

    ImageReader getThumbnailReader() throws IOException {
        if (thumbnailReader == null) {
            thumbnailReader = delegate.getOriginatingProvider().createReaderInstance();
        }

        return thumbnailReader;
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
        processThumbnailProgress(0f);

        BufferedImage thumbnail = thumbnails.get(thumbnailIndex).read();

        processThumbnailProgress(100f);
        processThumbnailComplete();

        return thumbnail;
    }

    // Metadata

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        initHeader(imageIndex);

        return new JPEGImage10Metadata(segments, getSOF(), getJFIF(), getJFXX(), getEmbeddedICCProfile(true), getAdobeDCT(), parseExif(getExif()));
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        return delegate.getStreamMetadata();
    }

    @Override
    protected void processWarningOccurred(String warning) {
        super.processWarningOccurred(warning);
    }

    private static void invertCMYK(final Raster raster) {
        byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            data[i] = (byte) (255 - data[i] & 0xff);
        }
    }

    private static void convertYCbCr2RGB(final Raster raster, final int numComponents) {
        final int height = raster.getHeight();
        final int width = raster.getWidth();
        final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                YCbCrConverter.convertJPEGYCbCr2RGB(data, data, (x + y * width) * numComponents);
            }
        }
    }

    private static void convertYCCK2CMYK(final Raster raster) {
        final int height = raster.getHeight();
        final int width = raster.getWidth();
        final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (x + y * width) * 4;
                // YCC -> CMY
                YCbCrConverter.convertJPEGYCbCr2RGB(data, data, offset);
                // Inverse K
                data[offset + 3] = (byte) (0xff - data[offset + 3] & 0xff);
            }
        }
    }

    private class ProgressDelegator extends ProgressListenerBase implements IIOReadUpdateListener, IIOReadWarningListener {

        @Override
        public void imageComplete(ImageReader source) {
            processImageComplete();
        }

        @Override
        public void imageProgress(ImageReader source, float percentageDone) {
            processImageProgress(percentageDone);
        }

        @Override
        public void imageStarted(ImageReader source, int imageIndex) {
            processImageStarted(currentStreamIndex);
        }

        @Override
        public void readAborted(ImageReader source) {
            processReadAborted();
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
            processThumbnailStarted(currentStreamIndex, thumbnailIndex);
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

    private class JPEGSegmentWarningDelegate implements JPEGSegmentWarningListener {
        @Override
        public void warningOccurred(String warning) {
            processWarningOccurred(warning);
        }
    }

    protected static void showIt(final BufferedImage pImage, final String pTitle) {
        ImageReaderBase.showIt(pImage, pTitle);
    }

    public static void main(final String[] args) throws IOException {
        ImageIO.setUseCache(false);

        int subX = 1;
        int subY = 1;
        int xOff = 0;
        int yOff = 0;
        Rectangle roi = null;
        boolean metadata = false;
        boolean thumbnails = false;

        for (int argIdx = 0; argIdx < args.length; argIdx++) {
            final String arg = args[argIdx];

            if (arg.charAt(0) == '-') {
                if (arg.equals("-s") || arg.equals("--subsample") && args.length > argIdx + 1) {
                    String[] sub = args[++argIdx].split(",");

                    try {
                        if (sub.length >= 4) {
                            subX = Integer.parseInt(sub[0]);
                            subY = Integer.parseInt(sub[1]);
                            xOff = Integer.parseInt(sub[2]);
                            yOff = Integer.parseInt(sub[3]);
                        }
                        else {
                            subX = Integer.parseInt(sub[0]);
                            subY = sub.length > 1 ? Integer.parseInt(sub[1]) : subX;
                        }
                    }
                    catch (NumberFormatException e) {
                        System.err.println("Bad sub sampling (x,y): '" + args[argIdx] + "'");
                    }
                }
                else if (arg.equals("-r") || arg.equals("--roi") && args.length > argIdx + 1) {
                    String[] region = args[++argIdx].split(",");

                    try {
                        if (region.length >= 4) {
                            roi = new Rectangle(Integer.parseInt(region[0]), Integer.parseInt(region[1]), Integer.parseInt(region[2]), Integer.parseInt(region[3]));
                        }
                        else {
                            roi = new Rectangle(Integer.parseInt(region[0]), Integer.parseInt(region[1]));
                        }
                    }
                    catch (IndexOutOfBoundsException | NumberFormatException e) {
                        System.err.println("Bad source region ([x,y,]w, h): '" + args[argIdx] + "'");
                    }
                }
                else if (arg.equals("-m") || arg.equals("--metadata")) {
                    metadata = true;
                }
                else if (arg.equals("-t") || arg.equals("--thumbnails")) {
                    thumbnails = true;
                }
                else {
                    System.err.println("Unknown argument: '" + arg + "'");
                    System.exit(-1);
                }

                continue;
            }

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

            final ImageReader reader = readers.next();
            System.err.println("Reading using: " + reader);

            reader.addIIOReadWarningListener(new IIOReadWarningListener() {
                public void warningOccurred(ImageReader source, String warning) {
                    System.err.println("Warning: " + arg + ": " + warning);
                }
            });
            final ProgressListenerBase listener = new ProgressListenerBase() {
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
            };
            reader.addIIOReadProgressListener(listener);

            reader.setInput(input);

            try {
                // For a tables-only image, we can't read image, but we should get metadata.
                if (reader.getNumImages(true) == 0) {
                    IIOMetadata streamMetadata = reader.getStreamMetadata();
                    IIOMetadataNode streamNativeTree = (IIOMetadataNode) streamMetadata.getAsTree(streamMetadata.getNativeMetadataFormatName());
                    new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(streamNativeTree, false);
                    continue;
                }

                BufferedImage image;
                ImageReadParam param = reader.getDefaultReadParam();
                if (subX > 1 || subY > 1 || roi != null) {
                    param.setSourceSubsampling(subX, subY, xOff, yOff);
                    param.setSourceRegion(roi);

//                    image = reader.getImageTypes(0).next().createBufferedImage((reader.getWidth(0) + subX - 1)/ subX, (reader.getHeight(0) + subY - 1) / subY);
                    image = null;
                }
                else {
//                    image = reader.getImageTypes(0).next().createBufferedImage(reader.getWidth(0), reader.getHeight(0));
                    image = null;
                }
                param.setDestination(image);

                long start = DEBUG ? System.currentTimeMillis() : 0;

                try {
                    image = reader.read(0, param);
                }
                catch (IOException e) {
                    e.printStackTrace();

                    if (image == null) {
                        continue;
                    }
                }

                if (DEBUG) {
                    System.err.println("Read time: " + (System.currentTimeMillis() - start) + " ms");
                    System.err.println("image: " + image);
                }

                /*
                int maxW = 1280;
                int maxH = 800;
                if (image.getWidth() > maxW || image.getHeight() > maxH) {
//                    start = System.currentTimeMillis();
                    float aspect = reader.getAspectRatio(0);
                    if (aspect >= 1f) {
                        image = ImageUtil.createResampled(image, maxW, Math.round(maxW / aspect), Image.SCALE_SMOOTH);
                    }
                    else {
                        image = ImageUtil.createResampled(image, Math.round(maxH * aspect), maxH, Image.SCALE_SMOOTH);
                    }
//                    System.err.println("Scale time: " + (System.currentTimeMillis() - start) + " ms");
                }
                */

                showIt(image, String.format("Image: %s [%d x %d]", file.getName(), reader.getWidth(0), reader.getHeight(0)));

                if (metadata) {
                    try {
                        IIOMetadata imageMetadata = reader.getImageMetadata(0);
                        System.out.println("Metadata for File: " + file.getName());

                        if (imageMetadata.getNativeMetadataFormatName() != null) {
                            System.out.println("Native:");
                            new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(imageMetadata.getAsTree(imageMetadata.getNativeMetadataFormatName()), false);
                        }
                        if (imageMetadata.isStandardMetadataFormatSupported()) {
                            System.out.println("Standard:");
                            new XMLSerializer(System.out, System.getProperty("file.encoding")).serialize(imageMetadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName), false);
                        }

                        System.out.println();
                    }
                    catch (IIOException e) {
                        System.err.println("Could not read thumbnails: " + arg + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                if (thumbnails) {
                    try {
                        int numThumbnails = reader.getNumThumbnails(0);
                        for (int i = 0; i < numThumbnails; i++) {
                            BufferedImage thumbnail = reader.readThumbnail(0, i);
                            //                        System.err.println("thumbnail: " + thumbnail);
                            showIt(thumbnail, String.format("Thumbnail: %s [%d x %d]", file.getName(), thumbnail.getWidth(), thumbnail.getHeight()));
                        }
                    }
                    catch (IIOException e) {
                        System.err.println("Could not read thumbnails: " + arg + ": " + e.getMessage());
                        e.printStackTrace();
                    }
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
