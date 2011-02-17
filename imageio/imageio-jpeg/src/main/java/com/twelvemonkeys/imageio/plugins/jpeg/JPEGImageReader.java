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
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
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
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

/**
 * JPEGImageReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author LUT-based YCbCR conversion by Werner Randelshofer
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGImageReader.java,v 1.0 24.01.11 16.37 haraldk Exp$
 */
public class JPEGImageReader extends ImageReaderBase {

    private final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.jpeg.debug"));

    /** Segment identifiers for the JPEG app segments we care about reading. */
    private static final Map<Integer, List<String>> SEGMENT_IDENTIFIERS = createSegmentIds();

    private static Map<Integer, List<String>> createSegmentIds() {
        Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

        // JFIF APP0 markers
        map.put(JPEG.APP0, Arrays.asList("JFIF", "JFXX"));

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

    private final ICCSpaceInterceptor iccSpaceInterceptor;
    private final ProgressDelegator progressDelegator;

    /** Cached JFIF app segments */
    private List<JPEGSegmentUtil.Segment> segments;

    private static Field getFieldSafely(final Class<?> cl, final String fieldName) {
        try {
            Field field = cl.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        }
        catch (NoSuchFieldException ignore) {
        }
        catch (SecurityException ignore) {
        }

        return null;
    }

    JPEGImageReader(final ImageReaderSpi provider, final ImageReader delegate) {
        super(provider);
        this.delegate = Validate.notNull(delegate);

        Field iccCS = getFieldSafely(delegate.getClass(), "iccCS");
        iccSpaceInterceptor = iccCS != null ? new ICCSpaceInterceptor(iccCS) : null;

        progressDelegator = new ProgressDelegator();
    }

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);

        ImageInputStream input = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) {
            System.err.println("No reader for: " + file);
            System.exit(1);
        }
        ImageReader myReader = readers.next();
        System.err.println("Reading using: " + myReader);
        myReader.addIIOReadWarningListener(new IIOReadWarningListener() {
            public void warningOccurred(ImageReader source, String warning) {
                System.err.println("warning: " + warning);
            }
        });
        myReader.addIIOReadProgressListener(new ProgressListenerBase() {
            private static final int MAX_W = 78;
            int lastProgress = 0;

            @Override
            public void imageStarted(ImageReader source, int imageIndex) {
                System.out.print("[");
            }

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                int steps = ((int) (percentageDone * MAX_W) / 100);
//                System.err.println("percentageDone: " + percentageDone);
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

        myReader.setInput(input);

        try {
            ImageReadParam param = myReader.getDefaultReadParam();
            if (args.length > 1) {
                int sub = Integer.parseInt(args[1]);
                param.setSourceSubsampling(sub, sub, 1, 1);
            }

            long start = System.currentTimeMillis();
            BufferedImage image = myReader.read(0, param);
            System.err.println("Time: " + (System.currentTimeMillis() - start) + " ms");
            System.err.println("image: " + image);

//            image = new ResampleOp(myReader.getWidth(0) / 4, myReader.getHeight(0) / 4, ResampleOp.FILTER_BLACKMAN_SINC).filter(image, null);

            if (image.getWidth() > 1600 || image.getHeight() > 1000) {
                float aspect = myReader.getAspectRatio(0);
                int height = Math.round(1600 / aspect);
                if (height <= 1000) {
                    image = ImageUtil.createResampled(image, 1600, height, Image.SCALE_DEFAULT);
                }
                else {
                    image = ImageUtil.createResampled(image, Math.round(1000 * aspect), 1000, Image.SCALE_DEFAULT);
                }
            }

            showIt(image, String.format("Image: %s [%d x %d]", file.getName(), myReader.getWidth(0), myReader.getHeight(0)));
        }
        finally {
            input.close();
        }
    }

    private void installListeners() {
        if (iccSpaceInterceptor != null) {
            delegate.addIIOReadProgressListener(iccSpaceInterceptor);
        }

        delegate.addIIOReadProgressListener(progressDelegator);
        delegate.addIIOReadUpdateListener(progressDelegator);
        delegate.addIIOReadWarningListener(progressDelegator);
    }

    // TODO: Delegate all methods?!

    @Override
    protected void resetMembers() {
        delegate.reset();
        segments = null;

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
        if (iccSpaceInterceptor != null) {
            iccSpaceInterceptor.replaceCS(delegate);
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

        delegate.setInput(input, seekForwardOnly, ignoreMetadata);
    }

    @Override
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        return delegate.isRandomAccessEasy(imageIndex);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        assertInput();
        checkBounds(imageIndex);

        // NOTE: We rely on the fact that unsupported images has no valid types. This is kind of hacky.
        // Might want to look into the metadata, to see if there's a better way to identify these.
        boolean unsupported = !delegate.getImageTypes(imageIndex).hasNext();

        ICC_Profile profile = getEmbeddedICCProfile();
        if (delegate.canReadRaster() && (unsupported || profile != null && ColorSpaces.isOffendingColorProfile(profile))) {
            if (DEBUG) {
                System.out.println("Reading using raster and extra conversion");
                System.out.println("ICC color profile = " + profile);
            }

            return readImageAsRasterAndReplaceColorProfile(imageIndex, param, profile);
        }

        if (DEBUG) {
            System.out.println("Reading using " + (iccSpaceInterceptor != null ? "intercepted " : "") + "delegate");
        }
        return delegate.read(imageIndex, param);
    }

    private BufferedImage readImageAsRasterAndReplaceColorProfile(int imageIndex, ImageReadParam param, ICC_Profile profile) throws IOException {

        int origWidth = getWidth(imageIndex);
        int origHeight = getHeight(imageIndex);

        ColorSpace srcCs = null;
        int xform = AdobeDCT.Unknown;

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

        // TODO: Fix this algorithm to behave like above, except for the presence of JFIF APP0 dictating YCbCr or gray,
        // as it might just as well be CMYK...
        // AdobeApp14 with transform either 1 or 2 can be trusted to be YCC/YCCK respectively, transform 0 means 1 component gray, 3 comp rgb, 4 comp cmyk
        //

        // 9788245605525.jpg:   JFIF App0 + Adobe App14 transform 0, channel Id's C, M, Y, K
        // lund-logo-cmyk.jpg:  Adobe App14 transform 0 (+ flag?), channel Id's 1-4
        // teastar_300dpi_cmyk.jpg: Adobe App14 transform 2 (+ flag), channel Id's 1-4


//        System.err.println("----> isAPP0Present(): " + isJFIFAPP0Present());
        SOF startOfFrame = getSOF();

//        System.err.println("startOfFrame: " + startOfFrame);

        Iterator<ImageTypeSpecifier> imageTypes = delegate.getImageTypes(imageIndex);

        // CMYK Support, assuming the delegate reader can't decode, and any 4 component image is CMYK
        if (!imageTypes.hasNext() && startOfFrame.componentsInFrame == 4) {
            // NOTE: Reading the metadata here chokes on some images. Instead, parse the Adobe App14 segment and read transform directly
            AdobeDCT adobeDCT = getAdobeDCT();
//            System.err.println("adobeDCT: " + adobeDCT);
            xform = adobeDCT != null ? adobeDCT.getTransform() : AdobeDCT.Unknown;

            srcCs = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);
            imageTypes = Arrays.asList(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR)).iterator();
        }

        BufferedImage image = getDestination(param, imageTypes, origWidth, origHeight);

        WritableRaster destination = image.getRaster();

        // TODO: checkReadParamBandSettings(param, );

        ColorConvertOp convert = null;
        ICC_ColorSpace replacement = profile != null ? ColorSpaces.createColorSpace(profile) : null;

        if (profile.getColorSpaceType() == ColorSpace.TYPE_GRAY && image.getColorModel().getColorSpace().getType() == ColorSpace.CS_GRAY) {
            // com.sun. reader does not do ColorConvertOp for CS_GRAY, even if embedded ICC profile,
            // probably because IJG native part does it already...? If applied, color looks wrong (too dark)...
        }
        else if (replacement != null) {
            // NOTE: CCO is not really necessary if replacement == image.getCM().getCS(),
            // but in practice it's as fast/faster than raster.setRect() (see below)
            convert = new ColorConvertOp(replacement, image.getColorModel().getColorSpace(), null);
        }
        else if (srcCs != null) {
            convert = new ColorConvertOp(srcCs, image.getColorModel().getColorSpace(), null);
        }
//        else if (!image.getColorModel().getColorSpace().isCS_sRGB()) {
        // TODO: Need to handle case where src and dest differ still
//            convert = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), image.getColorModel().getColorSpace(), null);
//        }
        else if (profile != null) {
            processWarningOccurred("Image contains an ICC color profile that is incompatible with Java 2D, color profile ignored.");
        }

        // We'll need a read param
        if (param == null) {
            param = delegate.getDefaultReadParam();
        }

        Rectangle srcRegion = new Rectangle();
        Rectangle dstRegion = new Rectangle();

        computeRegions(param, origWidth, origHeight, image, srcRegion, dstRegion);

        // We're ready to go
        processImageStarted(imageIndex);

        // Unfortunately looping is slower than reading all at once, but
        // that requires 2 x memory, so a few steps is an ok compromise I guess
        try {
            int csType = srcCs != null ? srcCs.getType() : image.getColorModel().getColorSpace().getType();
            int step = Math.max(1024, srcRegion.height / 10);

            for (int y = srcRegion.y; y < srcRegion.height; y += step) {
                int scan = Math.min(step, srcRegion.height - y);
                // Let the progress delegator handle progress, using corrected range
                progressDelegator.updateProgressRange(100f * (y + scan) / srcRegion.height);
//                try {
                param.setSourceRegion(new Rectangle(srcRegion.x, y, srcRegion.width, scan));
                Raster raster = delegate.readRaster(imageIndex, param); // non-converted

                // Apply source color conversion form implicit color space
                if ((xform == AdobeDCT.YCC || xform == AdobeDCT.Unknown) && csType == ColorSpace.TYPE_RGB) {
                    YCbCrConverter.convertYCbCr2RGB(raster);
                }
                else if (xform == AdobeDCT.YCCK && csType == ColorSpace.TYPE_CMYK) {
                    YCbCrConverter.convertYCCK2CMYK(raster);
                }
                else if (xform == AdobeDCT.Unknown && csType == ColorSpace.TYPE_CMYK) {
                    invertCMYK(raster);
                }

                // TODO: Subsampling

                Raster src = raster.createChild(0, 0, raster.getWidth(), raster.getHeight(), 0, 0, param.getSourceBands());
                WritableRaster dest = destination.createWritableChild(dstRegion.x, dstRegion.y + y - srcRegion.y, dstRegion.width, raster.getHeight(), 0, 0, param.getDestinationBands());

                // Apply further color conversion for explicit color space, or just copy the pixels into place
                if (convert != null) {
                    convert.filter(src, dest);
                }
                else {
                    dest.setRect(0, 0, src);
                }

                if (abortRequested()) {
                    processReadAborted();
                    break;
                }
//                }
//                catch (RasterFormatException e) {
//                    System.err.println("y: " + y);
//                    System.err.println("step: " + step);
//                    System.err.println("scan: " + scan);
//                    System.err.println("srcRegion: " + srcRegion);
//                    System.err.println("dstRegion: " + dstRegion);
//
//                    throw e;
//                }
            }
        }
        finally {
            // NOTE: Would be cleaner to clone the param, unfortunately it can't be done easily...
            param.setSourceRegion(srcRegion);

            // Restore normal read progress processing
            progressDelegator.resetProgressRange();
        }

        processImageComplete();

        return image;
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
        long pos = imageInput.getStreamPosition();

        try {
            imageInput.seek(0); // TODO: Seek to wanted image

            segments = JPEGSegmentUtil.readSegments(imageInput, SEGMENT_IDENTIFIERS);
        }
        catch (IOException ignore) {
        }
        catch (IllegalArgumentException foo) {
            foo.printStackTrace();
        }
        finally {
            imageInput.seek(pos);
        }
    }

    private List<JPEGSegmentUtil.Segment> getAppSegments(final int marker, final String identifier) throws IOException {
        initHeader();

        List<JPEGSegmentUtil.Segment> appSegments = Collections.emptyList();

        for (JPEGSegmentUtil.Segment segment : segments) {
            if (segment.marker() == marker && identifier.equals(segment.identifier())) {
                if (appSegments == Collections.EMPTY_LIST) {
                    appSegments = new ArrayList<JPEGSegmentUtil.Segment>(segments.size());
                }

                appSegments.add(segment);
            }
        }

        return appSegments;
    }

    public boolean isJFIFAPP0Present() throws IOException {
        return !(getAppSegments(JPEG.APP0, "JFIF").isEmpty() && getAppSegments(JPEG.APP0, "JFXX").isEmpty());
    }

    private SOF getSOF() throws IOException {
        for (JPEGSegmentUtil.Segment segment : segments) {
            if (JPEG.SOF0 <= segment.marker() && segment.marker() <= JPEG.SOF3 ||
                    JPEG.SOF5 <= segment.marker() && segment.marker() <= JPEG.SOF7 ||
                    JPEG.SOF9 <= segment.marker() && segment.marker() <= JPEG.SOF11 ||
                    JPEG.SOF13 <= segment.marker() && segment.marker() <= JPEG.SOF15) {

                DataInputStream data = new DataInputStream(segment.data());
                try {
                    int samplePrecision = data.readUnsignedByte();
                    int lines = data.readUnsignedShort();
                    int samplesPerLine = data.readUnsignedShort();
                    int componentsInFrame = data.readUnsignedByte();

                    /**
                    // Might not need this
                    for (int i = 0; i < componentsInFrame; i++) {
                        int comp = i + 1;
                        int id = data.readUnsignedByte();
                        System.err.println(comp + " id: " + id + " '" + (char) id + "'"); // typically 1-4, but may be 'C'/'M'/'Y'/'K' or similar...
                        int hv = data.readUnsignedByte();
                        System.err.println(comp + " horiz sub: " + ((hv & 0xF0) >> 4));
                        System.err.println(comp + " vertical sub: " + ((hv & 0xF)));
                        System.err.println(comp + " qt sel: " + data.readUnsignedByte());
                    }
                    //*/

                    return new SOF(segment.marker(), samplePrecision, lines, samplesPerLine, componentsInFrame);
                }
                finally {
                    data.close();
                }
            }
        }

        return null;
    }

    private AdobeDCT getAdobeDCT() throws IOException {
        List<JPEGSegmentUtil.Segment> adobe = getAppSegments(JPEG.APP14, "Adobe");

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

    private ICC_Profile getEmbeddedICCProfile() throws IOException {
        // ICC v 1.42 (2006) annex B:
        // APP2 marker (0xFFE2) + 2 byte length + ASCII 'ICC_PROFILE' + 0 (termination)
        // + 1 byte chunk number + 1 byte chunk count (allows ICC profiles chunked in multiple APP2 segments)
        List<JPEGSegmentUtil.Segment> segments = getAppSegments(JPEG.APP2, "ICC_PROFILE");

        if (segments.size() == 1) {
            // Faster code for the common case
            JPEGSegmentUtil.Segment segment = segments.get(0);
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

            InputStream[] streams = new InputStream[chunkCount];
            streams[chunkNumber - 1] = stream;

            for (int i = 1; i < chunkCount; i++) {
                stream = new DataInputStream(segments.get(i).data());

                chunkNumber = stream.readUnsignedByte();
                if (stream.readUnsignedByte() != chunkCount) {
                    throw new IIOException(String.format("Bad number of 'ICC_PROFILE' chunks."));
                }

                streams[chunkNumber - 1] = stream;
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
        return delegate.readerSupportsThumbnails();
    }

    @Override
    public boolean hasThumbnails(int imageIndex) throws IOException {
        return delegate.hasThumbnails(imageIndex);
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        return delegate.getNumThumbnails(imageIndex);
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        return delegate.getThumbnailWidth(imageIndex, thumbnailIndex);
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        return delegate.getThumbnailHeight(imageIndex, thumbnailIndex);
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        return delegate.readThumbnail(imageIndex, thumbnailIndex);
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

        @Override
        public void imageComplete(ImageWriter source) {
            processImageComplete();
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

    private static class ICCSpaceInterceptor extends ProgressListenerBase {
        final Field iccCS;

        public ICCSpaceInterceptor(final Field iccField) {
            iccCS = iccField;
        }

        @Override
        public void imageStarted(final ImageReader source, final int imageIndex) {
            replaceCS(source);
        }

        private void replaceCS(final ImageReader source) {
            // Intercept and modify the ICC color profile just before the read starts
            if (iccCS != null) {
                try {
                    ICC_ColorSpace cs = (ICC_ColorSpace) iccCS.get(source);

                    if (cs != null) {
                        ICC_Profile profile = cs.getProfile();

                        byte[] header = profile.getData(ICC_Profile.icSigHead);

                        // ColorConvertOp (or the sun.awt.color.CMM class, really) seems to choke on
                        // rendering intent other than perceptual (0), so we'll change the intent
                        // NOTE: Rendering intent is really a 4 byte value,
                        // but legal values are 0-3 (see ICC1v42_2006_05_1.pdf, 7.2.15, p. 19)
                        if (header[ICC_Profile.icHdrRenderingIntent] != 0) {
                            header[ICC_Profile.icHdrRenderingIntent] = 0;

                            // NOTE: We are mutating the current profile in place,
                            // so we don't need to write the iccCS field back..
                            profile.setData(ICC_Profile.icSigHead, header);

                            // But do it anyway, just in case...
                            iccCS.set(source, ColorSpaces.createColorSpace(profile));
                        }
                    }
                }
                catch (IllegalAccessException ignore) {
                }
            }
        }
    }

    private static class SOF {
        private final int marker;
        private final int samplePrecision;
        private final int lines;          // height
        private final int samplesPerLine; // width
        private final int componentsInFrame;

        public SOF(int marker, int samplePrecision, int lines, int samplesPerLine, int componentsInFrame) {
            this.marker = marker;
            this.samplePrecision = samplePrecision;
            this.lines = lines;
            this.samplesPerLine = samplesPerLine;
            this.componentsInFrame = componentsInFrame;
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
                    "SOF[marker: %04x, preciscion: %d, lines: %d, samples/line: %d, components: %d]",
                    marker, samplePrecision, lines, samplesPerLine, componentsInFrame
            );
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
            this.version = version;
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
                    "AdobeDCT[ver: %d, flags: %s %s, transform: %d]",
                    getVersion(), Integer.toBinaryString(getFlags0()), Integer.toBinaryString(getFlags1()), getTransform()
            );
        }
    }
}
