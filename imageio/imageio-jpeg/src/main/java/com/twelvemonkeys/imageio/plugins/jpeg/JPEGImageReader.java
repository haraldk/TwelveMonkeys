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

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.color.YCbCrConverter;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.plugins.jpeg.lossless.DataStream;
import com.twelvemonkeys.imageio.plugins.jpeg.lossless.JPEGLosslessDecoderWrapper;
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
import javax.imageio.stream.MemoryCacheImageInputStream;
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
 * that adds support and properly handles cases where the JRE version throws exceptions.
 * <p/>
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
public class JPEGImageReader extends ImageReaderBase {
    // TODO: Allow automatic rotation based on EXIF rotation field?
    // TODO: Create a simplified native metadata format that is closer to the actual JPEG stream AND supports EXIF in a sensible way
    // TODO: As we already parse the SOF segments, maybe we should stop delegating getWidth/getHeight etc?

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.jpeg.debug"));

    /** Internal constant for referring all APP segments */
    static final int ALL_APP_MARKERS = -1;

    /** Segment identifiers for the JPEG segments we care about reading. */
    private static final Map<Integer, List<String>> SEGMENT_IDENTIFIERS = createSegmentIds();

    private static Map<Integer, List<String>> createSegmentIds() {
        Map<Integer, List<String>> map = new LinkedHashMap<>();

        // Need all APP markers to be able to re-generate proper metadata later
        for (int appMarker = JPEG.APP0; appMarker <= JPEG.APP15; appMarker++) {
            map.put(appMarker, JPEGSegmentUtil.ALL_IDS);
        }

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

    /** Extra delegate for reading JPEG encoded thumbnails */
    private ImageReader thumbnailReader;
    private List<ThumbnailReader> thumbnails;

    private JPEGImage10MetadataCleaner metadataCleaner;

    /** Cached list of JPEG segments we filter from the underlying stream */
    private List<JPEGSegment> segments;

    protected JPEGImageReader(final ImageReaderSpi provider, final ImageReader delegate) {
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

        if (thumbnailReader != null) {
            thumbnailReader.reset();
        }

        metadataCleaner = null;

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

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        try {
            return delegate.getNumImages(allowSearch);
        }
        catch (ArrayIndexOutOfBoundsException ignore) {
            // This will happen if we find a "tables only" image, with no more images in stream.
            return 0;
        }
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
        JPEGColorSpace csType = getSourceCSType(getJFIF(), getAdobeDCT(), getSOF());

        if (types == null || !types.hasNext() || csType == JPEGColorSpace.CMYK || csType == JPEGColorSpace.YCCK) {
            ArrayList<ImageTypeSpecifier> typeList = new ArrayList<>();
            // Add the standard types, we can always convert to these
            typeList.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
            typeList.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
            typeList.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_BGR));

            // We also read and return CMYK if the source image is CMYK/YCCK + original color profile if present
            ICC_Profile profile = getEmbeddedICCProfile(false);

            if (csType == JPEGColorSpace.CMYK || csType == JPEGColorSpace.YCCK) {
                if (profile != null && profile.getNumComponents() == 4) {
                    typeList.add(ImageTypeSpecifiers.createInterleaved(ColorSpaces.createColorSpace(profile), new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
                }

                typeList.add(ImageTypeSpecifiers.createInterleaved(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
            }
            else if (csType == JPEGColorSpace.YCbCr || csType == JPEGColorSpace.RGB) {
                if (profile != null && profile.getNumComponents() == 3) {
                    typeList.add(ImageTypeSpecifiers.createInterleaved(ColorSpaces.createColorSpace(profile), new int[] {0, 1, 2}, DataBuffer.TYPE_BYTE, false, false));
                }
            }
            else if (csType == JPEGColorSpace.YCbCrA || csType == JPEGColorSpace.RGBA) {
                // Prepend ARGB types
                typeList.addAll(0, Arrays.asList(
                        ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB),
                        ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR),
                        ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB_PRE),
                        ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE)
                ));

                if (profile != null && profile.getNumComponents() == 3) {
                    typeList.add(ImageTypeSpecifiers.createInterleaved(ColorSpaces.createColorSpace(profile), new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, false));
                }
            }

            return typeList.iterator();
        }

        return types;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        // If delegate can determine the spec, we'll just go with that
        try {
            ImageTypeSpecifier rawType = delegate.getRawImageType(imageIndex);

            if (rawType != null) {
                return rawType;
            }
        }
        catch (NullPointerException ignore) {
            // Fall through
        }

        // Otherwise, consult the image metadata
        JPEGColorSpace csType = getSourceCSType(getJFIF(), getAdobeDCT(), getSOF());

        switch (csType) {
            case CMYK:
                // Create based on embedded profile if exists, or create from "Generic CMYK"
                ICC_Profile profile = getEmbeddedICCProfile(false);

                if (profile != null && profile.getNumComponents() == 4) {
                    return ImageTypeSpecifiers.createInterleaved(ColorSpaces.createColorSpace(profile), new int[]{3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);
                }

                return ImageTypeSpecifiers.createInterleaved(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK), new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);
            default:
                // For other types, we probably can't give a proper type, return null
                return null;
        }
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        // JPEGSegmentImageInputStream that filters out/skips bad/unnecessary segments
        delegate.setInput(imageInput != null
                          ? new JPEGSegmentImageInputStream(imageInput)
                          : null, seekForwardOnly, ignoreMetadata);
    }

    @Override
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        return delegate.isRandomAccessEasy(imageIndex);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        assertInput();
        checkBounds(imageIndex);

        SOFSegment sof = getSOF();
        ICC_Profile profile = getEmbeddedICCProfile(false);
        AdobeDCTSegment adobeDCT = getAdobeDCT();
        boolean bogusAdobeDCT = false;

        if (adobeDCT != null && (adobeDCT.getTransform() == AdobeDCTSegment.YCC && sof.componentsInFrame() != 3 ||
                adobeDCT.getTransform() == AdobeDCTSegment.YCCK && sof.componentsInFrame() != 4)) {
            processWarningOccurred(String.format(
                    "Invalid Adobe App14 marker. Indicates %s data, but SOF%d has %d color component(s). " +
                            "Ignoring Adobe App14 marker.",
                    adobeDCT.getTransform() == AdobeDCTSegment.YCCK ? "YCCK/CMYK" : "YCC/RGB",
                    sof.marker & 0xf, sof.componentsInFrame()
            ));

            bogusAdobeDCT = true;
            adobeDCT = null;
        }

        JPEGColorSpace sourceCSType = getSourceCSType(getJFIF(), adobeDCT, sof);

        //try to read an JPEG Lossless image
        if(sof.marker == JPEG.SOF3){
        	JPEGLosslessDecoderWrapper decoder = new JPEGLosslessDecoderWrapper();
        	ImageInputStream inputStream = (ImageInputStream) this.getInput();
        	
        	/* try to read the input stream as once. If length is not supported
        	 * the image is read blockwise
        	 */
        	byte[] rawByteData;
        	long byteLength = inputStream.length();
        	if(byteLength > 0 && byteLength < Integer.MAX_VALUE){
        		rawByteData = new byte[(int)byteLength];
        		inputStream.read(rawByteData);
        	} else {
        		ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024 * 1024 /* Provide a size close to what is likely the case for better performance*/);

        		byte[] buffer = new byte[1024];
        		int count;
        		while ((count = inputStream.read(buffer)) > 0) {
        		    bytes.write(buffer, 0, count);
        		}

        		rawByteData = bytes.toByteArray();
        	}
        	
        	//try do read the image, IOException can be thrown
        	return decoder.readImage(rawByteData);
        }
        
        // We need to apply ICC profile unless the profile is sRGB/default gray (whatever that is)
        // - or only filter out the bad ICC profiles in the JPEGSegmentImageInputStream.
        else if (delegate.canReadRaster() && (
                bogusAdobeDCT ||
                sourceCSType == JPEGColorSpace.CMYK ||
                sourceCSType == JPEGColorSpace.YCCK ||
                profile != null && !ColorSpaces.isCS_sRGB(profile) ||
                sourceCSType == JPEGColorSpace.YCbCr && getRawImageType(imageIndex) != null)) { // TODO: Issue warning?
            if (DEBUG) {
                System.out.println("Reading using raster and extra conversion");
                System.out.println("ICC color profile: " + profile);
            }

            // TODO: Possible to optimize slightly, to avoid readAsRaster for non-CMYK and other good types?
            return readImageAsRasterAndReplaceColorProfile(imageIndex, param, sof, sourceCSType, profile);
        }

        if (DEBUG) {
            System.out.println("Reading using delegate");
        }

        return delegate.read(imageIndex, param);
    }

    private BufferedImage readImageAsRasterAndReplaceColorProfile(int imageIndex, ImageReadParam param, SOFSegment startOfFrame, JPEGColorSpace csType, ICC_Profile profile) throws IOException {
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

        if (profile != null && (csType == JPEGColorSpace.Gray || csType == JPEGColorSpace.GrayA)) {
            // com.sun. reader does not do ColorConvertOp for CS_GRAY, even if embedded ICC profile,
            // probably because IJG native part does it already...? If applied, color looks wrong (too dark)...
//            convert = new ColorConvertOp(intendedCS, image.getColorModel().getColorSpace(), null);
        }
        else if (intendedCS != null) {
            // Handle inconsistencies
            if (startOfFrame.componentsInFrame() != intendedCS.getNumComponents()) {
                // If ICC profile number of components and startOfFrame does not match, ignore ICC profile
                processWarningOccurred(String.format(
                        "Embedded ICC color profile is incompatible with image data. " +
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
                processWarningOccurred(
                        "No embedded ICC color profile, defaulting to \"generic\" CMYK ICC profile. " +
                                "Colors may look incorrect."
                );

                // NOTE: Avoid using CCOp if same color space, as it's more compatible that way
                if (cmykCS != image.getColorModel().getColorSpace()) {
                    convert = new ColorConvertOp(cmykCS, image.getColorModel().getColorSpace(), null);
                }
            }
            else {
                // ColorConvertOp using non-ICC CS is deadly slow, fall back to fast conversion instead
                processWarningOccurred(
                        "No embedded ICC color profile, will convert using inaccurate CMYK to RGB conversion. " +
                                "Colors may look incorrect."
                );

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
            Raster raster = delegate.readRaster(imageIndex, param); // non-converted

            // Apply source color conversion from implicit color space
            if (csType == JPEGColorSpace.YCbCr || csType == JPEGColorSpace.YCbCrA) {
                convertYCbCr2RGB(raster);
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

    static JPEGColorSpace getSourceCSType(JFIFSegment jfif, AdobeDCTSegment adobeDCT, final SOFSegment startOfFrame) throws IIOException {
        /*
        ADAPTED from http://download.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html:

        When reading, the contents of the stream are interpreted by the usual JPEG conventions, as follows:

        • If a JFIF APP0 marker segment is present, the colorspace should be either grayscale or YCbCr.
        If an APP2 marker segment containing an embedded ICC profile is also present, then YCbCr is converted to RGB according
        to the formulas given in the JFIF spec, and the ICC profile is assumed to refer to the resulting RGB space.
        But, as software does not follow the spec, we can't really assume anything.

        • If an Adobe APP14 marker segment is present, the colorspace is determined by consulting the transform flag.
        The transform flag takes one of three values:
         o 2 - The image is encoded as YCCK (implicitly converted from CMYK on encoding).
         o 1 - The image is encoded as YCbCr (implicitly converted from RGB on encoding).
         o 0 - Unknown. 1-channel images are assumed to be Gray, 3-channel images are assumed to be RGB,
               4-channel images are assumed to be CMYK.

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
                case AdobeDCTSegment.YCC:
                    if (startOfFrame.components.length != 3) {
                        // This probably means the Adobe marker is bogus
                        break;
                    }
                    return JPEGColorSpace.YCbCr;
                case AdobeDCTSegment.YCCK:
                    if (startOfFrame.components.length != 4) {
                        // This probably means the Adobe marker is bogus
                        break;
                    }
                    return JPEGColorSpace.YCCK;
                case AdobeDCTSegment.Unknown:
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

        // TODO: We should probably allow component ids out of order (ie. BGR or KMCY)...
        switch (startOfFrame.components.length) {
            case 1:
                return JPEGColorSpace.Gray;
            case 2:
                return JPEGColorSpace.GrayA;
            case 3:
                if (startOfFrame.components[0].id == 1 && startOfFrame.components[1].id == 2 && startOfFrame.components[2].id == 3) {
                    // NOTE: Due to a bug in JPEGMetadata, standard format will report RGB for non-subsampled, non-JFIF files
                    return JPEGColorSpace.YCbCr;
                }
                else if (startOfFrame.components[0].id == 'R' && startOfFrame.components[1].id == 'G' && startOfFrame.components[2].id == 'B') {
                    return JPEGColorSpace.RGB;
                }
                else if (startOfFrame.components[0].id == 'Y' && startOfFrame.components[1].id == 'C' && startOfFrame.components[2].id == 'c') {
                    return JPEGColorSpace.PhotoYCC;
                }
                else {
                    // If subsampled, YCbCr else RGB
                    for (SOFComponent component : startOfFrame.components) {
                        if (component.hSub != 1 || component.vSub != 1) {
                            return JPEGColorSpace.YCbCr;
                        }
                    }

                    return jfif != null ? JPEGColorSpace.YCbCr : JPEGColorSpace.RGB;
                }
            case 4:
                if (startOfFrame.components[0].id == 1 && startOfFrame.components[1].id == 2 && startOfFrame.components[2].id == 3 && startOfFrame.components[3].id == 4) {
                    // NOTE: Due to a bug in JPEGMetadata, standard format will report RGBA for non-subsampled, non-JFIF files
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
                    // TODO: JPEGMetadata (standard format) will report YCbCrA for 4 channel subsampled... :-/
                    // If subsampled, YCCK else CMYK
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

    protected ICC_Profile ensureDisplayProfile(final ICC_Profile profile) {
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
        catch (IIOException | IllegalArgumentException ignore) {
            if (DEBUG) {
                ignore.printStackTrace();
            }
        }
        finally {
            imageInput.reset();
        }

        // In case of an exception, avoid NPE when referencing segments later
        if (segments == null) {
            segments = Collections.emptyList();
        }
    }

    List<JPEGSegment> getAppSegments(final int marker, final String identifier) throws IOException {
        initHeader();

        List<JPEGSegment> appSegments = Collections.emptyList();

        for (JPEGSegment segment : segments) {
            if ((marker == ALL_APP_MARKERS && segment.marker() >= JPEG.APP0 && segment.marker() <= JPEG.APP15 || segment.marker() == marker)
                    && (identifier == null || identifier.equals(segment.identifier()))) {
                if (appSegments == Collections.EMPTY_LIST) {
                    appSegments = new ArrayList<>(segments.size());
                }

                appSegments.add(segment);
            }
        }

        return appSegments;
    }

    SOFSegment getSOF() throws IOException {
        initHeader();

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

                    return new SOFSegment(segment.marker(), samplePrecision, lines, samplesPerLine, components);
                }
                finally {
                    data.close();
                }
            }
        }

        throw new IIOException("No SOF segment in stream");
    }

    AdobeDCTSegment getAdobeDCT() throws IOException {
        // TODO: Investigate http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6355567: 33/35 byte Adobe APP14 markers
        List<JPEGSegment> adobe = getAppSegments(JPEG.APP14, "Adobe");

        if (!adobe.isEmpty()) {
            // version (byte), flags (4bytes), color transform (byte: 0=unknown, 1=YCC, 2=YCCK)
            DataInputStream stream = new DataInputStream(adobe.get(0).data());

            return new AdobeDCTSegment(
                    stream.readUnsignedByte(),
                    stream.readUnsignedShort(),
                    stream.readUnsignedShort(),
                    stream.readUnsignedByte()
            );
        }

        return null;
    }

    JFIFSegment getJFIF() throws IOException{
        List<JPEGSegment> jfif = getAppSegments(JPEG.APP0, "JFIF");

        if (!jfif.isEmpty()) {
            JPEGSegment segment = jfif.get(0);

            if (segment.length() >= 9) {
                return JFIFSegment.read(segment.data());
            }
            else {
                processWarningOccurred("Bogus JFIF segment, ignoring");
            }
        }

        return null;
    }

    JFXXSegment getJFXX() throws IOException {
        List<JPEGSegment> jfxx = getAppSegments(JPEG.APP0, "JFXX");

        if (!jfxx.isEmpty()) {
            JPEGSegment segment = jfxx.get(0);
            if (segment.length() >= 1) {
                return JFXXSegment.read(segment.data(), segment.length());
            }
            else {
                processWarningOccurred("Bogus JFXX segment, ignoring");
            }
        }

        return null;
    }

    private CompoundDirectory getExif() throws IOException {
        List<JPEGSegment> exifSegments = getAppSegments(JPEG.APP1, "Exif");

        if (!exifSegments.isEmpty()) {
            JPEGSegment exif = exifSegments.get(0);
            InputStream data = exif.data();

            if (data.read() == -1) { // Read pad
                processWarningOccurred("Exif chunk has no data.");
            }
            else {
                ImageInputStream stream = ImageIO.createImageInputStream(data);
                return (CompoundDirectory) new EXIFReader().read(stream);

                // TODO: Directory offset of thumbnail is wrong/relative to container stream, causing trouble for the EXIFReader...
            }
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

    protected ICC_Profile getEmbeddedICCProfile(final boolean allowBadIndexes) throws IOException {
        // ICC v 1.42 (2006) annex B:
        // APP2 marker (0xFFE2) + 2 byte length + ASCII 'ICC_PROFILE' + 0 (termination)
        // + 1 byte chunk number + 1 byte chunk count (allows ICC profiles chunked in multiple APP2 segments)

        // TODO: Allow metadata to contain the wrongly indexed profiles, if readable
        // NOTE: We ignore any profile with wrong index for reading and image types, just to be on the safe side

        List<JPEGSegment> segments = getAppSegments(JPEG.APP2, "ICC_PROFILE");

        if (segments.size() == 1) {
            // Faster code for the common case
            JPEGSegment segment = segments.get(0);
            DataInputStream stream = new DataInputStream(segment.data());
            int chunkNumber = stream.readUnsignedByte();
            int chunkCount = stream.readUnsignedByte();

            if (chunkNumber != 1 && chunkCount != 1) {
                processWarningOccurred(String.format("Unexpected number of 'ICC_PROFILE' chunks: %d of %d. Ignoring ICC profile.", chunkNumber, chunkCount));
                return null;
            }

            return readICCProfileSafe(stream, allowBadIndexes);
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

            for (int i = 1; i < count; i++) {
                stream = new DataInputStream(segments.get(i).data());

                chunkNumber = stream.readUnsignedByte();

                if (!badICC && stream.readUnsignedByte() != chunkCount) {
                    throw new IIOException(String.format("Bad number of 'ICC_PROFILE' chunks: %d of %d.", chunkNumber, chunkCount));
                }

                streams[badICC ? i : chunkNumber - 1] = stream;
            }

            return readICCProfileSafe(new SequenceInputStream(Collections.enumeration(Arrays.asList(streams))), allowBadIndexes);
        }

        return null;
    }

    private ICC_Profile readICCProfileSafe(final InputStream stream, final boolean allowBadProfile) throws IOException {
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
    public ImageReadParam getDefaultReadParam() {
        return delegate.getDefaultReadParam();
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true; // We support EXIF, JFIF and JFXX style thumbnails
    }

    private void readThumbnailMetadata(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        if (thumbnails == null) {
            thumbnails = new ArrayList<>();
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
                        thumbnails.add(new JFXXThumbnailReader(thumbnailProgressDelegator, getThumbnailReader(), imageIndex, thumbnails.size(), jfxx));
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
                    ImageInputStream stream = new MemoryCacheImageInputStream(data);
                    CompoundDirectory exifMetadata = (CompoundDirectory) new EXIFReader().read(stream);

                    if (exifMetadata.directoryCount() == 2) {
                        Directory ifd1 = exifMetadata.getDirectory(1);

                        Entry compression = ifd1.getEntryById(TIFF.TAG_COMPRESSION);
                        // 1 = no compression, 6 = JPEG compression (default)
                        if (compression == null || compression.getValue().equals(1) || compression.getValue().equals(6)) {
                            Entry jpegLength = ifd1.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

                            if ((jpegLength == null || ((Number) jpegLength.getValue()).longValue() > 0)) {
                                thumbnails.add(new EXIFThumbnailReader(thumbnailProgressDelegator, getThumbnailReader(), 0, thumbnails.size(), ifd1, stream));
                            }
                            else {
                                processWarningOccurred("EXIF IFD with empty (zero-length) thumbnail");
                            }
                        }
                        else {
                            processWarningOccurred("EXIF IFD with unknown compression (expected 1 or 6): " + compression.getValue());
                        }
                    }
                }
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

        return thumbnails.get(thumbnailIndex).read();
    }

    // Metadata

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        // checkBounds needed, as we catch the IndexOutOfBoundsException below.
        checkBounds(imageIndex);

        IIOMetadata imageMetadata;

        try {
            imageMetadata = delegate.getImageMetadata(imageIndex);
        }
        catch (IndexOutOfBoundsException knownIssue) {
            // TMI-101: com.sun.imageio.plugins.jpeg.JPEGBuffer doesn't do proper sanity check of input data.
            throw new IIOException("Corrupt JPEG data: Bad segment length", knownIssue);
        }
        catch (NegativeArraySizeException knownIssue) {
            // Most likely from com.sun.imageio.plugins.jpeg.SOSMarkerSegment
            throw new IIOException("Corrupt JPEG data: Bad component count", knownIssue);
        }

        if (imageMetadata != null && Arrays.asList(imageMetadata.getMetadataFormatNames()).contains(JPEGImage10MetadataCleaner.JAVAX_IMAGEIO_JPEG_IMAGE_1_0)) {
            if (metadataCleaner == null) {
                metadataCleaner = new JPEGImage10MetadataCleaner(this);
            }

            return metadataCleaner.cleanMetadata(imageMetadata);
        }

        return imageMetadata;
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        return delegate.getStreamMetadata();
    }

    @Override
    protected void processWarningOccurred(String warning) {
        super.processWarningOccurred(warning);
    }

    static void invertCMYK(final Raster raster) {
        byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            data[i] = (byte) (255 - data[i] & 0xff);
        }
    }

    public static void convertYCbCr2RGB(final Raster raster) {
        final int height = raster.getHeight();
        final int width = raster.getWidth();
        final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                YCbCrConverter.convertYCbCr2RGB(data, data, (x + y * width) * 3);
            }
        }
    }

    public static void convertYCCK2CMYK(final Raster raster) {
        final int height = raster.getHeight();
        final int width = raster.getWidth();
        final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (x + y * width) * 4;
                // YCC -> CMY
                YCbCrConverter.convertYCbCr2RGB(data, data, offset);
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
                processImageStarted(imageIndex);
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
                if (arg.equals("-s") || arg.equals("--subsample") && args.length > argIdx) {
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
                else if (arg.equals("-r") || arg.equals("--roi") && args.length > argIdx) {
                    String[] region = args[++argIdx].split(",");

                    try {
                        if (region.length >= 4) {
                            roi = new Rectangle(Integer.parseInt(region[0]), Integer.parseInt(region[2]), Integer.parseInt(region[2]), Integer.parseInt(region[3]));
                        }
                        else {
                            roi = new Rectangle(Integer.parseInt(region[0]), Integer.parseInt(region[2]));
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

                    image = reader.getImageTypes(0).next().createBufferedImage((reader.getWidth(0) + subX - 1)/ subX, (reader.getHeight(0) + subY - 1) / subY);
                }
                else {
                    image = reader.getImageTypes(0).next().createBufferedImage(reader.getWidth(0), reader.getHeight(0));
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
