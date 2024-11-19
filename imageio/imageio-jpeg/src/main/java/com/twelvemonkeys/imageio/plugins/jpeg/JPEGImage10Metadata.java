/*
 * Copyright (c) 2016, Harald Kuhr
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

import com.twelvemonkeys.imageio.AbstractMetadata;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;

import org.w3c.dom.Node;

import javax.imageio.IIOException;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.color.ICC_Profile;
import java.util.List;

/**
 * JPEGImage10Metadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: JPEGImage10Metadata.java,v 1.0 10/08/16 harald.kuhr Exp$
 */
class JPEGImage10Metadata extends AbstractMetadata {

    /**
     * Native metadata format name
     */
    static final String JAVAX_IMAGEIO_JPEG_IMAGE_1_0 = "javax_imageio_jpeg_image_1.0";

    // TODO: Create our own native format, which is simply markerSequence from the Sun format, with the segments as-is, in sequence...
    //  + add special case for app segments, containing appXX + identifier (ie. <app0JFIF /> to  <app0 identifier="JFIF" /> or <app app="0" identifier="JFIF" />

    private final List<Segment> segments;

    private final Frame frame;
    private final JFIF jfif;
    private final AdobeDCT adobeDCT;
    private final JFXX jfxx;
    private final ICC_Profile embeddedICCProfile;

    private final CompoundDirectory exif;

    // TODO: Consider moving all the metadata stuff from the reader, over here...
    JPEGImage10Metadata(final List<Segment> segments, Frame frame, JFIF jfif, JFXX jfxx, ICC_Profile embeddedICCProfile, AdobeDCT adobeDCT, final CompoundDirectory exif) {
        super(true, JAVAX_IMAGEIO_JPEG_IMAGE_1_0, null, null, null);

        this.segments = segments;
        this.frame = frame;
        this.jfif = jfif;
        this.adobeDCT = adobeDCT;
        this.jfxx = jfxx;
        this.embeddedICCProfile = embeddedICCProfile;
        this.exif = exif;
    }

    @Override
    protected Node getNativeTree() {
        IIOMetadataNode root = new IIOMetadataNode(JAVAX_IMAGEIO_JPEG_IMAGE_1_0);

        IIOMetadataNode jpegVariety = new IIOMetadataNode("JPEGvariety");
        boolean isJFIF = jfif != null;
        if (isJFIF) {
            IIOMetadataNode app0JFIF = new IIOMetadataNode("app0JFIF");
            app0JFIF.setAttribute("majorVersion", Integer.toString(jfif.majorVersion));
            app0JFIF.setAttribute("minorVersion", Integer.toString(jfif.minorVersion));

            app0JFIF.setAttribute("resUnits", Integer.toString(jfif.units));
            app0JFIF.setAttribute("Xdensity", Integer.toString(jfif.xDensity));
            app0JFIF.setAttribute("Ydensity", Integer.toString(jfif.yDensity));

            app0JFIF.setAttribute("thumbWidth", Integer.toString(jfif.xThumbnail));
            app0JFIF.setAttribute("thumbHeight", Integer.toString(jfif.yThumbnail));

            jpegVariety.appendChild(app0JFIF);

            // Due to format oddity, add JFXX and app2ICC as subnodes here...
            //  ...and ignore them below, if added...
            apendJFXX(app0JFIF);
            appendICCProfile(app0JFIF);
        }

        root.appendChild(jpegVariety);

        appendMarkerSequence(root, segments, isJFIF);

        return root;
    }

    private void appendMarkerSequence(IIOMetadataNode root, List<Segment> segments, boolean isJFIF) {
        IIOMetadataNode markerSequence = new IIOMetadataNode("markerSequence");
        root.appendChild(markerSequence);

        for (Segment segment : segments)
            switch (segment.marker) {
                case JPEG.SOF0:
                case JPEG.SOF1:
                case JPEG.SOF2:
                case JPEG.SOF3:
                case JPEG.SOF5:
                case JPEG.SOF6:
                case JPEG.SOF7:
                case JPEG.SOF9:
                case JPEG.SOF10:
                case JPEG.SOF11:
                case JPEG.SOF13:
                case JPEG.SOF14:
                case JPEG.SOF15:
                    Frame sofSegment = (Frame) segment;

                    IIOMetadataNode sof = new IIOMetadataNode("sof");
                    sof.setAttribute("process", String.valueOf(sofSegment.marker & 0xf));
                    sof.setAttribute("samplePrecision", String.valueOf(sofSegment.samplePrecision));
                    sof.setAttribute("numLines", String.valueOf(sofSegment.lines));
                    sof.setAttribute("samplesPerLine", String.valueOf(sofSegment.samplesPerLine));
                    sof.setAttribute("numFrameComponents", String.valueOf(sofSegment.componentsInFrame()));

                    for (Frame.Component component : sofSegment.components) {
                        IIOMetadataNode componentSpec = new IIOMetadataNode("componentSpec");
                        componentSpec.setAttribute("componentId", String.valueOf(component.id));
                        componentSpec.setAttribute("HsamplingFactor", String.valueOf(component.hSub));
                        componentSpec.setAttribute("VsamplingFactor", String.valueOf(component.vSub));
                        componentSpec.setAttribute("QtableSelector", String.valueOf(component.qtSel));

                        sof.appendChild(componentSpec);
                    }

                    markerSequence.appendChild(sof);
                    break;

                case JPEG.DHT:
                    HuffmanTable huffmanTable = (HuffmanTable) segment;

                    IIOMetadataNode dcTables = new IIOMetadataNode("dht");
                    IIOMetadataNode acTables = new IIOMetadataNode("dht");

                    appendHuffmanTables(huffmanTable, 0, dcTables);
                    appendHuffmanTables(huffmanTable, 1, acTables);

                    markerSequence.appendChild(dcTables);

                    // Native metadata has a limit of max 4 children of the DHT, we split by class only if we must...
                    if (dcTables.getLength() + acTables.getLength() > 4) {
                        markerSequence.appendChild(acTables);
                    }
                    else {
                        while (acTables.hasChildNodes()) {
                            dcTables.appendChild(acTables.removeChild(acTables.getFirstChild()));
                        }
                    }

                    break;

                case JPEG.DQT:
                    QuantizationTable quantizationTable = (QuantizationTable) segment;
                    IIOMetadataNode dqt = new IIOMetadataNode("dqt");

                    for (int i = 0; i < 4; i++) {
                        if (quantizationTable.isPresent(i)) {
                            IIOMetadataNode dqtable = new IIOMetadataNode("dqtable");
                            dqtable.setAttribute("elementPrecision", quantizationTable.precision(i) != 16 ? "0" : "1"); // 0 = 8 bits, 1 = 16 bits
                            dqtable.setAttribute("qtableId", Integer.toString(i));
                            dqtable.setUserObject(quantizationTable.toNativeTable(i));
                            dqt.appendChild(dqtable);
                        }
                    }
                    markerSequence.appendChild(dqt);

                    break;

                case JPEG.DRI:
                    RestartInterval restartInterval = (RestartInterval) segment;
                    IIOMetadataNode dri = new IIOMetadataNode("dri");
                    dri.setAttribute("interval", Integer.toString(restartInterval.interval));
                    markerSequence.appendChild(dri);

                    break;

                case JPEG.SOS:
                    Scan scan = (Scan) segment;
                    IIOMetadataNode sos = new IIOMetadataNode("sos");
                    sos.setAttribute("numScanComponents", String.valueOf(scan.components.length));
                    sos.setAttribute("startSpectralSelection", String.valueOf(scan.spectralSelStart));
                    sos.setAttribute("endSpectralSelection", String.valueOf(scan.spectralSelEnd));
                    sos.setAttribute("approxHigh", String.valueOf(scan.approxHigh));
                    sos.setAttribute("approxLow", String.valueOf(scan.approxLow));

                    for (Scan.Component component : scan.components) {
                        IIOMetadataNode spec = new IIOMetadataNode("scanComponentSpec");
                        spec.setAttribute("componentSelector", String.valueOf(component.scanCompSel));
                        spec.setAttribute("dcHuffTable", String.valueOf(component.dcTabSel));
                        spec.setAttribute("acHuffTable", String.valueOf(component.acTabSel));
                        sos.appendChild(spec);
                    }

                    markerSequence.appendChild(sos);
                    break;

                case JPEG.COM:
                    IIOMetadataNode com = new IIOMetadataNode("com");
                    com.setAttribute("comment", ((Comment) segment).comment);

                    markerSequence.appendChild(com);

                    break;

                case JPEG.APP0:
                    if (segment instanceof JFIF) {
                        // Either already added, or we'll ignore it anyway...
                        break;
                    }
                    else if (isJFIF && segment instanceof JFXX) {
                        // Already added
                        break;
                    }

                    // Else, fall through to unknown segment

                case JPEG.APP2:
                    if (isJFIF && segment instanceof ICCProfile) {
                        // Already added
                        break;
                    }
                    // Else, fall through to unknown segment

                case JPEG.APP14:
                    if (segment instanceof AdobeDCT) {
                        AdobeDCT adobe = (AdobeDCT) segment;
                        IIOMetadataNode app14Adobe = new IIOMetadataNode("app14Adobe");
                        app14Adobe.setAttribute("version", String.valueOf(adobe.version));
                        app14Adobe.setAttribute("flags0", String.valueOf(adobe.flags0));
                        app14Adobe.setAttribute("flags1", String.valueOf(adobe.flags1));
                        app14Adobe.setAttribute("transform", String.valueOf(adobe.transform));
                        markerSequence.appendChild(app14Adobe);
                        break;
                    }
                    // Else, fall through to unknown segment

                default:
                    IIOMetadataNode unknown = new IIOMetadataNode("unknown");
                    unknown.setAttribute("MarkerTag", String.valueOf(segment.marker & 0xFF));
                    byte[] data = segment instanceof Application ? ((Application) segment).data : ((Unknown) segment).data;
                    unknown.setUserObject(data);
                    markerSequence.appendChild(unknown);

                    break;
            }
    }

    private void appendHuffmanTables(HuffmanTable huffmanTable, int tableClass, IIOMetadataNode dht) {
        for (int i = 0; i < 4; i++) {
            if (huffmanTable.isPresent(i, tableClass)) {
                IIOMetadataNode dhtable = new IIOMetadataNode("dhtable");
                dhtable.setAttribute("class", String.valueOf(tableClass));
                dhtable.setAttribute("htableId", String.valueOf(i));
                dhtable.setUserObject(huffmanTable.toNativeTable(i, tableClass));
                dht.appendChild(dhtable);
            }
        }
    }

    private void appendICCProfile(IIOMetadataNode app0JFIF) {
        if (embeddedICCProfile != null) {
            IIOMetadataNode app2ICC = new IIOMetadataNode("app2ICC");
            app2ICC.setUserObject(embeddedICCProfile);

            app0JFIF.appendChild(app2ICC);
        }
    }

    private void apendJFXX(IIOMetadataNode app0JFIF) {
        if (jfxx != null) {
            IIOMetadataNode jfxxNode = new IIOMetadataNode("JFXX");
            app0JFIF.appendChild(jfxxNode);

            IIOMetadataNode app0JFXX = new IIOMetadataNode("app0JFXX");
            app0JFXX.setAttribute("extensionCode", Integer.toString(jfxx.extensionCode));
            jfxxNode.appendChild(app0JFXX);

            switch (jfxx.extensionCode) {
                case JFXX.JPEG:
                    IIOMetadataNode thumbJPEG = new IIOMetadataNode("JFIFthumbJPEG");
                    thumbJPEG.appendChild(new IIOMetadataNode("markerSequence"));
                    // TODO: Insert segments in marker sequence...
//                    List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(new ByteArrayImageInputStream(jfxx.thumbnail), JPEGSegmentUtil.ALL_SEGMENTS);
                    // Convert to Segment as in JPEGImageReader...
//                    appendMarkerSequence(thumbJPEG, segments, false);

                    app0JFXX.appendChild(thumbJPEG);

                    break;

                case JFXX.INDEXED:
                    IIOMetadataNode thumbPalette = new IIOMetadataNode("JFIFthumbPalette");
                    thumbPalette.setAttribute("thumbWidth", Integer.toString(jfxx.thumbnail[0] & 0xFF));
                    thumbPalette.setAttribute("thumbHeight", Integer.toString(jfxx.thumbnail[1] & 0xFF));
                    app0JFXX.appendChild(thumbPalette);
                    break;

                case JFXX.RGB:
                    IIOMetadataNode thumbRGB = new IIOMetadataNode("JFIFthumbRGB");
                    thumbRGB.setAttribute("thumbWidth", Integer.toString(jfxx.thumbnail[0] & 0xFF));
                    thumbRGB.setAttribute("thumbHeight", Integer.toString(jfxx.thumbnail[1] & 0xFF));
                    app0JFXX.appendChild(thumbRGB);
                    break;
            }
        }
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IIOMetadataNode colorSpaceType = new IIOMetadataNode("ColorSpaceType");
        colorSpaceType.setAttribute("name", getColorSpaceType());
        chroma.appendChild(colorSpaceType);

        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        numChannels.setAttribute("value", String.valueOf(frame.componentsInFrame()));
        chroma.appendChild(numChannels);

        return chroma;
    }

    private String getColorSpaceType() {
        try {
            JPEGColorSpace csType = JPEGImageReader.getSourceCSType(jfif, adobeDCT, frame);

            switch (csType) {
                case Gray:
                case GrayA:
                    return "GRAY";
                case YCbCr:
                case YCbCrA:
                    return "YCbCr";
                case RGB:
                case RGBA:
                    return "RGB";
                case PhotoYCC:
                case PhotoYCCA:
                    return "PhotoYCC";
                case YCCK:
                    return "YCCK";
                case CMYK:
                    return "CMYK";
                default:

            }
        }
        catch (IIOException ignore) {
        }

        return Integer.toString(frame.componentsInFrame(), 16) + "CLR";
    }

    private boolean hasAlpha() {
        try {
            JPEGColorSpace csType = JPEGImageReader.getSourceCSType(jfif, adobeDCT, frame);

            switch (csType) {
                case GrayA:
                case YCbCrA:
                case RGBA:
                case PhotoYCCA:
                    return true;
                default:

            }
        }
        catch (IIOException ignore) {
        }

        return false;
    }

    private boolean isLossess() {
        switch (frame.marker) {
            case JPEG.SOF3:
            case JPEG.SOF7:
            case JPEG.SOF11:
            case JPEG.SOF15:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        if (hasAlpha()) {
            IIOMetadataNode transparency = new IIOMetadataNode("Transparency");

            IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
            alpha.setAttribute("value", "nonpremultipled");
            transparency.appendChild(alpha);

            return transparency;
        }

        return null;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression = new IIOMetadataNode("Compression");

        IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
        compressionTypeName.setAttribute("value", "JPEG"); // ...or "JPEG-LOSSLESS" (which is the name used by the JAI JPEGImageWriter for it's compression name)?
        compression.appendChild(compressionTypeName);

        IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
        lossless.setAttribute("value", isLossess() ? "TRUE" : "FALSE");
        compression.appendChild(lossless);

        IIOMetadataNode numProgressiveScans = new IIOMetadataNode("NumProgressiveScans");
        numProgressiveScans.setAttribute("value", "1"); // TODO!
        compression.appendChild(numProgressiveScans);

        return compression;
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
        imageOrientation.setAttribute("value", getExifOrientation(exif));
        dimension.appendChild(imageOrientation);

        if (jfif != null) {
            // Aspect ratio
            float xDensity = Math.max(1, jfif.xDensity);
            float yDensity = Math.max(1, jfif.yDensity);
            float aspectRatio = jfif.units == 0 ? xDensity / yDensity : yDensity / xDensity;

            IIOMetadataNode pixelAspectRatio = new IIOMetadataNode("PixelAspectRatio");
            pixelAspectRatio.setAttribute("value", Float.toString(aspectRatio));
            dimension.insertBefore(pixelAspectRatio, imageOrientation); // Keep order

            if (jfif.units != 0) {
                // Pixel size
                float scale = jfif.units == 1 ? 25.4F : 10.0F; // DPI or DPcm

                IIOMetadataNode horizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
                horizontalPixelSize.setAttribute("value", Float.toString(scale / xDensity));
                dimension.appendChild(horizontalPixelSize);

                IIOMetadataNode verticalPixelSize = new IIOMetadataNode("VerticalPixelSize");
                verticalPixelSize.setAttribute("value", Float.toString(scale / yDensity));
                dimension.appendChild(verticalPixelSize);
            }
        }

        return dimension;
    }

    private String getExifOrientation(Directory exif) {
        if (exif != null) {
            Entry orientationEntry = exif.getEntryById(TIFF.TAG_ORIENTATION);

            if (orientationEntry != null) {
                switch (((Number) orientationEntry.getValue()).intValue()) {
                    case 2:
                        return "FlipH";
                    case 3:
                        return "Rotate180";
                    case 4:
                        return "FlipV";
                    case 5:
                        return "FlipVRotate90";
                    case 6:
                        return "Rotate270";
                    case 7:
                        return "FlipHRotate90";
                    case 8:
                        return "Rotate90";
                    case 0:
                    case 1:
                    default:
                        // Fall-through
                }
            }
        }

        return "Normal";
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = new IIOMetadataNode("Text");

        for (Segment segment : segments) {
            if (segment instanceof Comment) {
                IIOMetadataNode com = new IIOMetadataNode("TextEntry");
                com.setAttribute("keyword", "comment");
                com.setAttribute("value", ((Comment) segment).comment);

                text.appendChild(com);
            }
        }

        // TODO: Add the following from Exif (as in TIFFMetadata)
        // DocumentName, ImageDescription, Make, Model, PageName, Software, Artist, HostComputer, InkNames, Copyright:
        // /Text/TextEntry@keyword = field name, /Text/TextEntry@value = field value.

        return text.hasChildNodes() ? text : null;
    }
}
