package com.twelvemonkeys.imageio;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.twelvemonkeys.imageio.StandardImageMetadataSupport.ColorSpaceType.*;
import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * Base class for easy read-only implementation of the standard image metadata format.
 * Chroma, Data and Transparency nodes values are based on the required
 * {@link ImageTypeSpecifier}.
 * Other values or overrides may be specified using the builder.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 */
public class StandardImageMetadataSupport extends AbstractMetadata {

    // The only required field, most standard metadata can be extracted from the type
    private final ImageTypeSpecifier type;
    protected final ColorSpaceType colorSpaceType;
    protected final boolean blackIsZero;
    private final IndexColorModel palette;
    protected final String compressionName;
    protected final boolean compressionLossless;
    protected final PlanarConfiguration planarConfiguration;
    private final int[] bitsPerSample;
    private final int[] significantBits;
    private final int[] sampleMSB;
    protected final Double pixelAspectRatio;
    protected final ImageOrientation orientation;
    protected final String formatVersion;
    protected final SubimageInterpretation subimageInterpretation;
    private final Calendar documentCreationTime; // TODO: This field should be a LocalDateTime or other java.time type, Consider a long timestamp + TimeZone to avoid messing up the API...
    private final Collection<TextEntry> textEntries;

    protected StandardImageMetadataSupport(Builder builder) {
        notNull(builder, "builder");

        // Baseline
        type = builder.type;

        // Chroma
        colorSpaceType = builder.colorSpaceType;
        blackIsZero = builder.blackIsZero;
        palette = builder.palette;

        // Compression
        compressionName = builder.compressionName;
        compressionLossless = builder.compressionLossless;

        // Data
        planarConfiguration = builder.planarConfiguration;
        bitsPerSample = builder.bitsPerSample;
        significantBits = builder.significantBits;
        sampleMSB = builder.sampleMSB;

        // Dimension
        orientation = builder.orientation;
        pixelAspectRatio = builder.pixelAspectRatio;

        // Document
        formatVersion = builder.formatVersion;
        documentCreationTime = builder.documentCreationTime;
        subimageInterpretation = builder.subimageInterpretation;

        // Text
        textEntries = builder.textEntries;
    }

    public static Builder builder(ImageTypeSpecifier type) {
        return new Builder(type);
    }

    public static class Builder {
        private final ImageTypeSpecifier type;
        private ColorSpaceType colorSpaceType;
        private boolean blackIsZero = true;
        private IndexColorModel palette;
        private String compressionName;
        private boolean compressionLossless = true;
        private PlanarConfiguration planarConfiguration;
        public int[] bitsPerSample;
        private int[] significantBits;
        private int[] sampleMSB;
        private Double pixelAspectRatio;
        private ImageOrientation orientation = ImageOrientation.Normal;
        private String formatVersion;
        private SubimageInterpretation subimageInterpretation;
        private Calendar documentCreationTime; // TODO: This field should be a LocalDateTime or other java.time type
        private final Collection<TextEntry> textEntries = new ArrayList<>();

        protected Builder(ImageTypeSpecifier type) {
            this.type = notNull(type, "type");
        }

        public Builder withColorSpaceType(ColorSpaceType colorSpaceType) {
            this.colorSpaceType = colorSpaceType;

            return this;
        }

        public Builder withBlackIsZero(boolean blackIsZero) {
            this.blackIsZero = blackIsZero;

            return this;
        }

        public Builder withPalette(IndexColorModel palette) {
            this.palette = palette;

            return this;
        }

        public Builder withCompressionTypeName(String compressionName) {
            this.compressionName = notNull(compressionName, "compressionName").equalsIgnoreCase("none") ? null : compressionName;

            return this;
        }

        public Builder withCompressionLossless(boolean lossless) {
            this.compressionLossless = isTrue(lossless || compressionName != null, lossless, "Lossy compression requires compression name");

            return this;
        }

        public Builder withPlanarConfiguration(PlanarConfiguration planarConfiguration) {
            this.planarConfiguration = planarConfiguration;

            return this;
        }

        public Builder withBitsPerSample(int... bitsPerSample) {
            this.bitsPerSample = bitsPerSample;

            return this;
        }

        public Builder withSignificantBitsPerSample(int... significantBits) {
            this.significantBits = isTrue(significantBits.length == 1 || significantBits.length == type.getNumBands(),
                                          significantBits,
                                          String.format("single value or %d values expected", type.getNumBands()));

            return this;
        }

        public Builder withSampleMSB(int... sampleMSB) {
            this.sampleMSB = isTrue(sampleMSB.length == 1 || sampleMSB.length == type.getNumBands(),
                                          sampleMSB,
                                          String.format("single value or %d values expected", type.getNumBands()));

            return this;
        }

        public Builder withPixelAspectRatio(Double pixelAspectRatio) {
            this.pixelAspectRatio = pixelAspectRatio;

            return this;
        }

        public Builder withOrientation(ImageOrientation orientation) {
            this.orientation = notNull(orientation, "orientation");

            return this;
        }

        public Builder withFormatVersion(String formatVersion) {
            this.formatVersion = notNull(formatVersion, "formatVersion");

            return this;
        }

        public Builder withSubimageInterpretation(SubimageInterpretation interpretation) {
            this.subimageInterpretation = interpretation;

            return this;
        }

        public Builder withDocumentCreationTime(Calendar creationTime) {
            this.documentCreationTime = creationTime;

            return this;
        }

        public Builder withTextEntries(Map<String, String> entries) {
            return withTextEntries(toTextEntries(notNull(entries, "entries").entrySet()));
        }

        private Collection<TextEntry> toTextEntries(Collection<Map.Entry<String, String>> entries) {
            TextEntry[] result = new TextEntry[entries.size()];

            int i = 0;
            for (Map.Entry<String, String> entry : entries) {
                result[i++] = new TextEntry(entry.getKey(), entry.getValue());
            }

            return Arrays.asList(result);
        }

        public Builder withTextEntries(Collection<TextEntry> entries) {
            this.textEntries.addAll(notNull(entries, "entries"));

            return this;
        }

        public Builder withTextEntry(String keyword, String value) {
            if (value != null && !value.isEmpty()) {
                this.textEntries.add(new TextEntry(notNull(keyword, "keyword"), value));
            }

            return this;
        }

        public IIOMetadata build() {
            return new StandardImageMetadataSupport(this);
        }
    }

    protected enum ColorSpaceType {
        XYZ(3),
        Lab(3),
        Luv(3),
        YCbCr(3),
        Yxy(3),
        YCCK(4),
        PhotoYCC(3),
        RGB(3),
        GRAY(1),
        HSV(3),
        HLS(3),
        CMYK(3),
        CMY(3),

        // Generic types (so much extra work, because Java names can't start with a number, phew...)
        GENERIC_2CLR(2, "2CLR"),
        GENERIC_3CLR(3, "3CLR"),
        GENERIC_4CLR(4, "4CLR"),
        GENERIC_5CLR(5, "5CLR"),
        GENERIC_6CLR(6, "6CLR"),
        GENERIC_7CLR(7, "7CLR"),
        GENERIC_8CLR(8, "8CLR"),
        GENERIC_9CLR(9, "9CLR"),
        GENERIC_ACLR(0xA, "ACLR"),
        GENERIC_BCLR(0xB, "BCLR"),
        GENERIC_CCLR(0xC, "CCLR"),
        GENERIC_DCLR(0xD, "DCLR"),
        GENERIC_ECLR(0xE, "ECLR"),
        GENERIC_FCLR(0xF, "FCLR");

        final int numChannels;
        private final String nameOverride;

        ColorSpaceType(int numChannels) {
            this(numChannels, null);
        }
        ColorSpaceType(int numChannels, String nameOverride) {
            this.numChannels = numChannels;
            this.nameOverride = nameOverride;
        }

        @Override
        public String toString() {
            return nameOverride != null ? nameOverride : super.toString();
        }
    }

    protected enum PlanarConfiguration {
        PixelInterleaved,
        PlaneInterleaved,
        LineInterleaved,
        TileInterleaved
    }

    protected enum ImageOrientation {
        Normal,
        Rotate90,
        Rotate180,
        Rotate270,
        FlipH,
        FlipV,
        FlipHRotate90,
        FlipVRotate90
    }

    protected enum SubimageInterpretation {
        Standalone,
        SinglePage,
        FullResolution,
        ReducedResolution,
        PyramidLayer,
        Preview,
        VolumeSlice,
        ObjectView,
        Panorama,
        AnimationFrame,
        TransparencyMask,
        CompositingLayer,
        SpectralSlice,
        Unknown
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chromaNode = new IIOMetadataNode("Chroma");

        ColorModel colorModel = colorSpaceType != null ? null : type.getColorModel();
        ColorSpaceType csType = colorSpaceType != null ? colorSpaceType : colorSpaceType(colorModel.getColorSpace());
        int numComponents = colorSpaceType != null ? colorSpaceType.numChannels : colorModel.getNumComponents();

        IIOMetadataNode colorSpaceTypeNode = new IIOMetadataNode("ColorSpaceType");
        chromaNode.appendChild(colorSpaceTypeNode);
        colorSpaceTypeNode.setAttribute("name", csType.toString());

        IIOMetadataNode numChannelsNode = new IIOMetadataNode("NumChannels");
        numChannelsNode.setAttribute("value", String.valueOf(numComponents));
        chromaNode.appendChild(numChannelsNode);

        IIOMetadataNode blackIsZeroNode = new IIOMetadataNode("BlackIsZero");
        blackIsZeroNode.setAttribute("value", booleanString(blackIsZero));
        chromaNode.appendChild(blackIsZeroNode);

        if (colorModel instanceof IndexColorModel || palette != null) {
            IndexColorModel colorMap = palette != null ? palette : (IndexColorModel) colorModel;

            IIOMetadataNode paletteNode = new IIOMetadataNode("Palette");
            chromaNode.appendChild(paletteNode);

            for (int i = 0; i < colorMap.getMapSize(); i++) {
                IIOMetadataNode paletteEntryNode = new IIOMetadataNode("PaletteEntry");
                paletteNode.appendChild(paletteEntryNode);

                paletteEntryNode.setAttribute("index", Integer.toString(i));
                paletteEntryNode.setAttribute("red", Integer.toString(colorMap.getRed(i)));
                paletteEntryNode.setAttribute("green", Integer.toString(colorMap.getGreen(i)));
                paletteEntryNode.setAttribute("blue", Integer.toString(colorMap.getBlue(i)));

                // Assumption: BITMASK transparency will use single transparent pixel
                if (colorMap.getTransparency() == Transparency.TRANSLUCENT) {
                    paletteEntryNode.setAttribute("alpha", Integer.toString(colorMap.getAlpha(i)));
                }
            }

            if (colorMap.getTransparentPixel() != -1) {
                IIOMetadataNode backgroundIndexNode = new IIOMetadataNode("BackgroundIndex");
                chromaNode.appendChild(backgroundIndexNode);
                backgroundIndexNode.setAttribute("value", Integer.toString(colorMap.getTransparentPixel()));
            }
        }

        // TODO: BackgroundColor?

        return chromaNode;
    }

    private static ColorSpaceType colorSpaceType(ColorSpace colorSpace) {
        switch (colorSpace.getType()) {
            case ColorSpace.TYPE_XYZ:
                return XYZ;
            case ColorSpace.TYPE_Lab:
                return Lab;
            case ColorSpace.TYPE_Luv:
                return Luv;
            case ColorSpace.TYPE_YCbCr:
                return YCbCr;
            case ColorSpace.TYPE_Yxy:
                return Yxy;
                // Note: Can't map to YCCK or PhotoYCC, as there's no corresponding constant in java.awt.ColorSpace
            case ColorSpace.TYPE_RGB:
                return RGB;
            case ColorSpace.TYPE_GRAY:
                return GRAY;
            case ColorSpace.TYPE_HSV:
                return HSV;
            case ColorSpace.TYPE_HLS:
                return HLS;
            case ColorSpace.TYPE_CMYK:
                return CMYK;
            case ColorSpace.TYPE_CMY:
                return CMY;
            default:
                int numComponents = colorSpace.getNumComponents();
                if (numComponents == 1) {
                    return GRAY;
                }
                else if (numComponents < 16) {
                    return ColorSpaceType.valueOf("GENERIC_" + Integer.toHexString(numComponents) + "CLR");
                }
        }

        throw new IllegalArgumentException("Unknown ColorSpace type: " + colorSpace);
    }

    protected static final class TextEntry {
        static final List<String> COMPRESSIONS = Arrays.asList("none", "lzw", "zip", "bzip", "other");

        final String keyword;
        final String value;
        final String language;
        final String encoding;
        final String compression;

        public TextEntry(final String keyword, final String value) {
            this(keyword, value, null, null, null);
        }

        public TextEntry(final String keyword, final String value, final String language, final String encoding, final String compression) {
            this.keyword = keyword;
            this.value = notNull(value, "value");
            this.language = language;
            this.encoding = encoding;
            this.compression = isTrue(compression == null || COMPRESSIONS.contains(compression), compression, String.format("Unknown compression: %s (expected: %s)", compression, COMPRESSIONS));
        }
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        if (compressionName == null) {
            return null;
        }

        IIOMetadataNode node = new IIOMetadataNode("Compression");

        IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
        compressionTypeName.setAttribute("value", compressionName);
        node.appendChild(compressionTypeName);

        IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
        lossless.setAttribute("value", booleanString(compressionLossless));
        node.appendChild(lossless);

        return node;
    }

    protected static String booleanString(boolean booleanValue) {
        return booleanValue ? "TRUE" : "FALSE";
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode dataNode = new IIOMetadataNode("Data");

        IIOMetadataNode planarConfigurationNode = new IIOMetadataNode("PlanarConfiguration");
        dataNode.appendChild(planarConfigurationNode);
        planarConfigurationNode.setAttribute("value", planarConfiguration != null ? planarConfiguration.toString() :
                                                  (type.getSampleModel() instanceof BandedSampleModel ? "PlaneInterleaved" : "PixelInterleaved"));

        String sampleFormatValue = colorSpaceType == null && type.getColorModel() instanceof IndexColorModel
                                   ? "Index"
                                   : sampleFormat(type.getSampleModel());

        if (sampleFormatValue != null) {
            IIOMetadataNode sampleFormatNode = new IIOMetadataNode("SampleFormat");
            sampleFormatNode.setAttribute("value", sampleFormatValue);
            dataNode.appendChild(sampleFormatNode);
        }

        int[] bitsPerSample = this.bitsPerSample != null ? this.bitsPerSample : type.getSampleModel().getSampleSize();
        IIOMetadataNode bitsPerSampleNode = new IIOMetadataNode("BitsPerSample");
        bitsPerSampleNode.setAttribute("value", createListValue(bitsPerSample.length, bitsPerSample));
        dataNode.appendChild(bitsPerSampleNode);

        if (significantBits != null) {
            String significantBitsValue = createListValue(type.getNumBands(), significantBits);
            if (!significantBitsValue.equals(bitsPerSampleNode.getAttribute("value"))) {
                IIOMetadataNode significantBitsPerSampleNode = new IIOMetadataNode("SignificantBitsPerSample");
                significantBitsPerSampleNode.setAttribute("value", significantBitsValue);
                dataNode.appendChild(significantBitsPerSampleNode);
            }
        }

        if (sampleMSB != null) {
            // TODO: Only if different from default!
            IIOMetadataNode sampleMSBNode = new IIOMetadataNode("SampleMSB");
            sampleMSBNode.setAttribute("value", createListValue(type.getNumBands(), sampleMSB));
            dataNode.appendChild(sampleMSBNode);
        }

        return dataNode;
    }

    private static String createListValue(final int itemCount, final int... values) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < itemCount; i++) {
            if (buffer.length() > 0) {
                buffer.append(' ');
            }

            buffer.append(values[i % values.length]);
        }

        return buffer.toString();
    }

    private static String sampleFormat(SampleModel sampleModel) {
        switch (sampleModel.getDataType()) {
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                if (sampleModel instanceof ComponentSampleModel) {
                    return "SignedIntegral";
                }
                // Otherwise fall-through, most likely a *PixelPackedSampleModel
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
                return "UnsignedIntegral";
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                return "Real";
            default:
                return null;
        }
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimensionNode = new IIOMetadataNode("Dimension");

        if (pixelAspectRatio != null) {
            IIOMetadataNode pixelAspectRatioNode = new IIOMetadataNode("PixelAspectRatio");
            pixelAspectRatioNode.setAttribute("value", String.valueOf(pixelAspectRatio));
            dimensionNode.appendChild(pixelAspectRatioNode);
        }

        IIOMetadataNode imageOrientationNode = new IIOMetadataNode("ImageOrientation");
        imageOrientationNode.setAttribute("value", orientation.toString());
        dimensionNode.appendChild(imageOrientationNode);

        return dimensionNode.hasChildNodes() ? dimensionNode : null;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode documentNode = new IIOMetadataNode("Document");

        if (formatVersion != null) {
            IIOMetadataNode formatVersionNode = new IIOMetadataNode("FormatVersion");
            documentNode.appendChild(formatVersionNode);
            formatVersionNode.setAttribute("value", formatVersion);
        }

        if (subimageInterpretation != null) {
            IIOMetadataNode subImageInterpretationNode = new IIOMetadataNode("SubimageInterpretation");
            documentNode.appendChild(subImageInterpretationNode);
            subImageInterpretationNode.setAttribute("value", subimageInterpretation.toString());
        }

        if (documentCreationTime != null) {
            IIOMetadataNode imageCreationTimeNode = new IIOMetadataNode("ImageCreationTime");
            documentNode.appendChild(imageCreationTimeNode);

            imageCreationTimeNode.setAttribute("year", String.valueOf(documentCreationTime.get(Calendar.YEAR)));
            imageCreationTimeNode.setAttribute("month", String.valueOf(documentCreationTime.get(Calendar.MONTH) + 1));
            imageCreationTimeNode.setAttribute("day", String.valueOf(documentCreationTime.get(Calendar.DAY_OF_MONTH)));
            imageCreationTimeNode.setAttribute("hour", String.valueOf(documentCreationTime.get(Calendar.HOUR_OF_DAY)));
            imageCreationTimeNode.setAttribute("minute", String.valueOf(documentCreationTime.get(Calendar.MINUTE)));
            imageCreationTimeNode.setAttribute("second", String.valueOf(documentCreationTime.get(Calendar.SECOND)));
        }

        return documentNode.hasChildNodes() ? documentNode : null;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        if (textEntries.isEmpty()) {
            return null;
        }

        IIOMetadataNode textNode = new IIOMetadataNode("Text");

        // DocumentName, ImageDescription, Make, Model, PageName, Software, Artist, HostComputer, InkNames, Copyright:
        // /Text/TextEntry@keyword = field name, /Text/TextEntry@value = field value.

        for (TextEntry entry : textEntries) {
            IIOMetadataNode textEntryNode = new IIOMetadataNode("TextEntry");
            textNode.appendChild(textEntryNode);
            if (entry.keyword != null) {
                textEntryNode.setAttribute("keyword", entry.keyword);
            }
            textEntryNode.setAttribute("value", entry.value);
            if (entry.language != null) {
                textEntryNode.setAttribute("language", entry.language);
            }
            if (entry.encoding != null) {
                textEntryNode.setAttribute("encoding", entry.encoding);
            }
            if (entry.compression != null) {
                textEntryNode.setAttribute("compression", entry.compression);
            }
        }

        return textNode;

    }

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparencyNode = new IIOMetadataNode("Transparency");

        ColorModel colorModel = type.getColorModel();

        IIOMetadataNode alphaNode = new IIOMetadataNode("Alpha");
        transparencyNode.appendChild(alphaNode);
        alphaNode.setAttribute("value", colorModel.hasAlpha() ? (colorModel.isAlphaPremultiplied() ? "premultiplied" : "nonpremultiplied") : "none");

        if (colorModel instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) colorModel;
            if (icm.getTransparentPixel() != -1) {
                IIOMetadataNode transparentIndexNode = new IIOMetadataNode("TransparentIndex");
                transparencyNode.appendChild(transparentIndexNode);
                transparentIndexNode.setAttribute("value", Integer.toString(icm.getTransparentPixel()));
            }
        }

        return transparencyNode;
    }
}
