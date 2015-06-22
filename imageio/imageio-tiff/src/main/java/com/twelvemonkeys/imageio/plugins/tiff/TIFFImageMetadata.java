package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.AbstractMetadata;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.metadata.IIOMetadataNode;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

/**
 * TIFFImageMetadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: TIFFImageMetadata.java,v 1.0 17/04/15 harald.kuhr Exp$
 */
final class TIFFImageMetadata extends AbstractMetadata {

    private final Directory ifd;

    TIFFImageMetadata(final Directory ifd) {
        super(true, TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME, TIFFMedataFormat.class.getName(), null, null);
        this.ifd = Validate.notNull(ifd, "IFD");
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    protected IIOMetadataNode getNativeTree() {
        IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);
        root.appendChild(asTree(ifd));

        return root;
    }

    private IIOMetadataNode asTree(final Directory ifd) {
        IIOMetadataNode ifdNode = new IIOMetadataNode("TIFFIFD");

        for (Entry tag : ifd) {
            IIOMetadataNode tagNode;
            Object value = tag.getValue();

            if (value instanceof Directory) {
                // TODO: Don't expand non-TIFF IFDs...
                tagNode = asTree((Directory) value);
                tagNode.setAttribute("parentTagNumber", String.valueOf(tag.getIdentifier()));
                String fieldName = tag.getFieldName();
                if (fieldName != null) {
                    tagNode.setAttribute("parentTagName", fieldName);
                }

                // TODO: tagSets is REQUIRED!
            }
            else {
                tagNode = new IIOMetadataNode("TIFFField");
                tagNode.setAttribute("number", String.valueOf(tag.getIdentifier()));

                String fieldName = tag.getFieldName();
                if (fieldName != null) {
                    tagNode.setAttribute("name", fieldName);
                }

                int count = tag.valueCount();

                if (TIFF.TYPE_NAMES[TIFF.TYPE_UNDEFINED].equals(tag.getTypeName())) {
                    // Why does "undefined" need special handling?! It's just a byte array.. :-P
                    // Or maybe rather, why isn't all types implemented like this..?
                    // TODO: Consider handling IPTC, Photoshop/Adobe, XMP and ICC Profile as Undefined always
                    // (even if older software wrote as Byte), as it's more compact?
                    IIOMetadataNode valueNode = new IIOMetadataNode("TIFFUndefined");
                    tagNode.appendChild(valueNode);

                    if (count == 1) {
                        valueNode.setAttribute("value", String.valueOf(value));
                    }
                    else {
                        valueNode.setAttribute("value", Arrays.toString((byte[]) value).replaceAll("\\[?\\]?", ""));
                    }
                }
                else {
                    String arrayTypeName = getMetadataArrayType(tag);
                    IIOMetadataNode valueNode = new IIOMetadataNode(arrayTypeName);
                    tagNode.appendChild(valueNode);

                    boolean unsigned = !isSignedType(tag);
                    String typeName = getMetadataType(tag);

                    // NOTE: ASCII/Strings have count 1, always. This seems consistent with the JAI ImageIO version.
                    if (count == 1) {
                        IIOMetadataNode elementNode = new IIOMetadataNode(typeName);
                        valueNode.appendChild(elementNode);

                        setValue(value, unsigned, elementNode);
                    }
                    else {
                        for (int i = 0; i < count; i++) {
                            Object val = Array.get(value, i);
                            IIOMetadataNode elementNode = new IIOMetadataNode(typeName);
                            valueNode.appendChild(elementNode);

                            setValue(val, unsigned, elementNode);
                        }
                    }
                }
            }

            ifdNode.appendChild(tagNode);
        }

        return ifdNode;
    }

    private void setValue(final Object value, final boolean unsigned, final IIOMetadataNode elementNode) {
        if (unsigned && value instanceof Byte) {
            elementNode.setAttribute("value", String.valueOf((Byte) value & 0xFF));
        }
        else if (unsigned && value instanceof Short) {
            elementNode.setAttribute("value", String.valueOf((Short) value & 0xFFFF));
        }
        else if (unsigned && value instanceof Integer) {
            elementNode.setAttribute("value", String.valueOf((Integer) value & 0xFFFFFFFFl));
        }
        else {
            elementNode.setAttribute("value", String.valueOf(value));
        }
    }

    private boolean isSignedType(final Entry tag) {
        String typeName = tag.getTypeName();

        // Stupid special cases implementation, until we can access the type id...
        if ("SBYTE".equals(typeName)) {
            return true;
        }
        if ("SSHORT".equals(typeName)) {
            return true;
        }
        if ("SLONG".equals(typeName)) {
            return true;
        }
        if ("SRATIONAL".equals(typeName)) {
            return true;
        }
        if ("FLOAT".equals(typeName)) {
            return true;
        }
        if ("DOUBLE".equals(typeName)) {
            return true;
        }
        if ("SLONG8".equals(typeName)) {
            return true;
        }
        // IFD8 not used

        return false;
    }

    private String getMetadataArrayType(final Entry tag) {
        String typeName = tag.getTypeName();

        // Stupid special cases implementation, until we can access the type id...
        if ("BYTE".equals(typeName)) {
            return "TIFFBytes";
        }
        if ("ASCII".equals(typeName)) {
            return "TIFFAsciis";
        }
        if ("SHORT".equals(typeName)) {
            return "TIFFShorts";
        }
        if ("LONG".equals(typeName)) {
            return "TIFFLongs";
        }
        if ("RATIONAL".equals(typeName)) {
            return "TIFFRationals";
        }
        // UNDEFINED not used...
        if ("SBYTE".equals(typeName)) {
            return "TIFFSBytes";
        }
        if ("SSHORT".equals(typeName)) {
            return "TIFFSShorts";
        }
        if ("SLONG".equals(typeName)) {
            return "TIFFSLongs";
        }
        if ("SRATIONAL".equals(typeName)) {
            return "TIFFSRationals";
        }
        if ("FLOAT".equals(typeName)) {
            return "TIFFFloats";
        }
        if ("DOUBLE".equals(typeName)) {
            return "TIFFDoubles";
        }
        // IFD not used
        if ("LONG8".equals(typeName)) {
            return "TIFFLong8s";
        }
        if ("SLONG8".equals(typeName)) {
            return "TIFFSLong8s";
        }
        // IFD8 not used

        throw new IllegalArgumentException(typeName);
    }

    private String getMetadataType(final Entry tag) {
        String typeName = tag.getTypeName();

        // Stupid special cases implementation, until we can access the type id...
        if ("BYTE".equals(typeName)) {
            return "TIFFByte";
        }
        if ("ASCII".equals(typeName)) {
            return "TIFFAscii";
        }
        if ("SHORT".equals(typeName)) {
            return "TIFFShort";
        }
        if ("LONG".equals(typeName)) {
            return "TIFFLong";
        }
        if ("RATIONAL".equals(typeName)) {
            return "TIFFRational";
        }
        // UNDEFINED not used...
        if ("SBYTE".equals(typeName)) {
            return "TIFFSByte";
        }
        if ("SSHORT".equals(typeName)) {
            return "TIFFSShort";
        }
        if ("SLONG".equals(typeName)) {
            return "TIFFSLong";
        }
        if ("SRATIONAL".equals(typeName)) {
            return "TIFFSRational";
        }
        if ("FLOAT".equals(typeName)) {
            return "TIFFFloat";
        }
        if ("DOUBLE".equals(typeName)) {
            return "TIFFDouble";
        }
        // IFD not used
        if ("LONG8".equals(typeName)) {
            return "TIFFLong8";
        }
        if ("SLONG8".equals(typeName)) {
            return "TIFFSLong8";
        }
        // IFD8 not used

        throw new IllegalArgumentException(typeName);
    }

    // TODO: Candidate superclass method!
    private IIOMetadataNode addChildNode(final IIOMetadataNode parent,
                                         final String name,
                                         final Object object) {
        IIOMetadataNode child = new IIOMetadataNode(name);

        if (object != null) {
            child.setUserObject(object); // TODO: Should we always store user object?!?!
            child.setNodeValue(object.toString()); // TODO: Fix this line
        }

        parent.appendChild(child);

        return child;
    }

    /// Standard metadata
    // See: http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        // Handle ColorSpaceType (RGB/CMYK/YCbCr etc)...
        Entry photometricTag = ifd.getEntryById(TIFF.TAG_PHOTOMETRIC_INTERPRETATION);
        int photometricValue = ((Number) photometricTag.getValue()).intValue(); // No default for this tag!

        Entry samplesPerPixelTag = ifd.getEntryById(TIFF.TAG_SAMPLES_PER_PIXEL);
        Entry bitsPerSampleTag = ifd.getEntryById(TIFF.TAG_BITS_PER_SAMPLE);
        int numChannelsValue = samplesPerPixelTag != null
                               ? ((Number) samplesPerPixelTag.getValue()).intValue()
                               : bitsPerSampleTag.valueCount();

        IIOMetadataNode colorSpaceType = new IIOMetadataNode("ColorSpaceType");
        chroma.appendChild(colorSpaceType);
        switch (photometricValue) {
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
            case TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO:
            case TIFFBaseline.PHOTOMETRIC_MASK: // It's really a transparency mask/alpha channel, but...
                colorSpaceType.setAttribute("value", "GRAY");
                break;
            case TIFFBaseline.PHOTOMETRIC_RGB:
            case TIFFBaseline.PHOTOMETRIC_PALETTE:
                colorSpaceType.setAttribute("value", "RGB");
                break;
            case TIFFExtension.PHOTOMETRIC_YCBCR:
                colorSpaceType.setAttribute("value", "YCbCr");
                break;
            case TIFFExtension.PHOTOMETRIC_CIELAB:
            case TIFFExtension.PHOTOMETRIC_ICCLAB:
            case TIFFExtension.PHOTOMETRIC_ITULAB:
                colorSpaceType.setAttribute("value", "Lab");
                break;
            case TIFFExtension.PHOTOMETRIC_SEPARATED:
                // TODO: May be CMYK, or something else... Consult InkSet and NumberOfInks!
                if (numChannelsValue == 3) {
                    colorSpaceType.setAttribute("value", "CMY");
                }
                else {
                    colorSpaceType.setAttribute("value", "CMYK");
                }
                break;
            case TIFFCustom.PHOTOMETRIC_LOGL: // ..?
            case TIFFCustom.PHOTOMETRIC_LOGLUV:
                colorSpaceType.setAttribute("value", "Luv");
                break;
            case TIFFCustom.PHOTOMETRIC_CFA:
            case TIFFCustom.PHOTOMETRIC_LINEAR_RAW: // ...or is this RGB?
                colorSpaceType.setAttribute("value", "3CLR");
                break;
            default:
                colorSpaceType.setAttribute("value", Integer.toHexString(numChannelsValue) + "CLR");
                break;
        }

        // NumChannels
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        chroma.appendChild(numChannels);
        if (photometricValue == TIFFBaseline.PHOTOMETRIC_PALETTE) {
            numChannels.setAttribute("value", "3");
        }
        else {
            numChannels.setAttribute("value", Integer.toString(numChannelsValue));
        }

        // BlackIsZero (defaults to TRUE)
        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        chroma.appendChild(blackIsZero);
        switch (photometricValue) {
            case TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO:
                blackIsZero.setAttribute("value", "FALSE");
                break;
            default:
                break;
        }

        Entry colorMapTag = ifd.getEntryById(TIFF.TAG_COLOR_MAP);

        if (colorMapTag != null) {
            int[] colorMapValues = (int[]) colorMapTag.getValue();

            IIOMetadataNode palette = new IIOMetadataNode("Palette");
            chroma.appendChild(palette);

            int count = colorMapValues.length / 3;
            for (int i = 0; i < count; i++) {
                IIOMetadataNode paletteEntry = new IIOMetadataNode("PaletteEntry");
                paletteEntry.setAttribute("index", Integer.toString(i));

                // TODO: See TIFFImageReader createIndexColorModel, to detect 8 bit colorMap
                paletteEntry.setAttribute("red", Integer.toString((colorMapValues[i] >> 8) & 0xff));
                paletteEntry.setAttribute("green", Integer.toString((colorMapValues[i + count] >> 8) & 0xff));
                paletteEntry.setAttribute("blue", Integer.toString((colorMapValues[i + count * 2] >> 8) & 0xff));

                palette.appendChild(paletteEntry);
            }
        }

        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression = new IIOMetadataNode("Compression");
        IIOMetadataNode compressionTypeName = addChildNode(compression, "CompressionTypeName", null);

        Entry compressionTag = ifd.getEntryById(TIFF.TAG_COMPRESSION);
        int compressionValue = compressionTag == null
                               ? TIFFBaseline.COMPRESSION_NONE
                               : ((Number) compressionTag.getValue()).intValue();

        // Naming is identical to JAI ImageIO metadata as far as possible
        switch (compressionValue) {
            case TIFFBaseline.COMPRESSION_NONE:
                compressionTypeName.setAttribute("value", "None");
                break;
            case TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE:
                compressionTypeName.setAttribute("value", "CCITT RLE");
                break;
            case TIFFExtension.COMPRESSION_CCITT_T4:
                compressionTypeName.setAttribute("value", "CCITT T4");
                break;
            case TIFFExtension.COMPRESSION_CCITT_T6:
                compressionTypeName.setAttribute("value", "CCITT T6");
                break;
            case TIFFExtension.COMPRESSION_LZW:
                compressionTypeName.setAttribute("value", "LZW");
                break;
            case TIFFExtension.COMPRESSION_OLD_JPEG:
                compressionTypeName.setAttribute("value", "Old JPEG");
                break;
            case TIFFExtension.COMPRESSION_JPEG:
                compressionTypeName.setAttribute("value", "JPEG");
                break;
            case TIFFExtension.COMPRESSION_ZLIB:
                compressionTypeName.setAttribute("value", "ZLib");
                break;
            case TIFFExtension.COMPRESSION_DEFLATE:
                compressionTypeName.setAttribute("value", "Deflate");
                break;
            case TIFFBaseline.COMPRESSION_PACKBITS:
                compressionTypeName.setAttribute("value", "PackBits");
                break;
            case TIFFCustom.COMPRESSION_CCITTRLEW:
                compressionTypeName.setAttribute("value", "CCITT RLEW");
                break;
            case TIFFCustom.COMPRESSION_DCS:
                compressionTypeName.setAttribute("value", "DCS");
                break;
            case TIFFCustom.COMPRESSION_IT8BL:
                compressionTypeName.setAttribute("value", "IT8BL");
                break;
            case TIFFCustom.COMPRESSION_IT8CTPAD:
                compressionTypeName.setAttribute("value", "IT8CTPAD");
                break;
            case TIFFCustom.COMPRESSION_IT8LW:
                compressionTypeName.setAttribute("value", "IT8LW");
                break;
            case TIFFCustom.COMPRESSION_IT8MP:
                compressionTypeName.setAttribute("value", "IT8MP");
                break;
            case TIFFCustom.COMPRESSION_JBIG:
                compressionTypeName.setAttribute("value", "JBIG");
                break;
            case TIFFCustom.COMPRESSION_JPEG2000:
                compressionTypeName.setAttribute("value", "JPEG 2000");
                break;
            case TIFFCustom.COMPRESSION_NEXT:
                compressionTypeName.setAttribute("value", "NEXT");
                break;
            case TIFFCustom.COMPRESSION_PIXARFILM:
                compressionTypeName.setAttribute("value", "Pixar Film");
                break;
            case TIFFCustom.COMPRESSION_PIXARLOG:
                compressionTypeName.setAttribute("value", "Pixar Log");
                break;
            case TIFFCustom.COMPRESSION_SGILOG:
                compressionTypeName.setAttribute("value", "SGI Log");
                break;
            case TIFFCustom.COMPRESSION_SGILOG24:
                compressionTypeName.setAttribute("value", "SGI Log24");
                break;
            case TIFFCustom.COMPRESSION_THUNDERSCAN:
                compressionTypeName.setAttribute("value", "ThunderScan");
                break;
            default:
                compressionTypeName.setAttribute("value", "Unknown " + compressionValue);
                break;
        }

        if (compressionValue != TIFFBaseline.COMPRESSION_NONE) {
            // Lossless (defaults to TRUE)
            IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
            compression.appendChild(lossless);

            switch (compressionValue) {
                case TIFFExtension.COMPRESSION_OLD_JPEG:
                case TIFFExtension.COMPRESSION_JPEG:
                case TIFFCustom.COMPRESSION_JBIG:
                case TIFFCustom.COMPRESSION_JPEG2000:
                    lossless.setAttribute("value", "FALSE");
                    break;
                default:
                    break;
            }
        }

        return compression;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        Entry planarConfigurationTag = ifd.getEntryById(TIFF.TAG_PLANAR_CONFIGURATION);
        int planarConfigurationValue = planarConfigurationTag == null
                                       ? TIFFBaseline.PLANARCONFIG_CHUNKY
                                       : ((Number) planarConfigurationTag.getValue()).intValue();

        switch (planarConfigurationValue) {
            case TIFFBaseline.PLANARCONFIG_CHUNKY:
                planarConfiguration.setAttribute("value", "PixelInterleaved");
                break;
            case TIFFExtension.PLANARCONFIG_PLANAR:
                planarConfiguration.setAttribute("value", "PlaneInterleaved");
                break;
            default:
                planarConfiguration.setAttribute("value", "Unknown " + planarConfigurationValue);
        }
        node.appendChild(planarConfiguration);

        Entry photometricInterpretationTag = ifd.getEntryById(TIFF.TAG_PHOTOMETRIC_INTERPRETATION);
        int photometricInterpretationValue = photometricInterpretationTag == null
                                             ? TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO
                                             : ((Number) photometricInterpretationTag.getValue()).intValue();

        Entry samleFormatTag = ifd.getEntryById(TIFF.TAG_SAMPLE_FORMAT);
        int sampleFormatValue = samleFormatTag == null
                                ? TIFFBaseline.SAMPLEFORMAT_UINT
                                : ((Number) samleFormatTag.getValue()).intValue();
        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        node.appendChild(sampleFormat);
        switch (sampleFormatValue) {
            case TIFFBaseline.SAMPLEFORMAT_UINT:
                if (photometricInterpretationValue == TIFFBaseline.PHOTOMETRIC_PALETTE) {
                    sampleFormat.setAttribute("value", "Index");
                }
                else {
                    sampleFormat.setAttribute("value", "UnsignedIntegral");
                }
                break;
            case TIFFExtension.SAMPLEFORMAT_INT:
                sampleFormat.setAttribute("value", "SignedIntegral");
                break;
            case TIFFExtension.SAMPLEFORMAT_FP:
                sampleFormat.setAttribute("value", "Real");
                break;
            default:
                sampleFormat.setAttribute("value", "Unknown " + sampleFormatValue);
                break;
        }

        // TODO: See TIFFImageReader.getBitsPerSample + fix the metadata to have getAsXxxArray methods.
        // BitsPerSample (not required field for Class B/Bilevel, defaults to 1)
        Entry bitsPerSampleTag = ifd.getEntryById(TIFF.TAG_BITS_PER_SAMPLE);
        String bitsPerSampleValue = bitsPerSampleTag == null &&
                                            (photometricInterpretationValue == TIFFBaseline.PHOTOMETRIC_WHITE_IS_ZERO ||
                                                    photometricInterpretationValue == TIFFBaseline.PHOTOMETRIC_BLACK_IS_ZERO)
                                    ? "1"
                                    : bitsPerSampleTag.getValueAsString().replaceAll("\\[?\\]?,?", "");

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        node.appendChild(bitsPerSample);
        bitsPerSample.setAttribute("value", bitsPerSampleValue);

        Entry samplesPerPixelTag = ifd.getEntryById(TIFF.TAG_SAMPLES_PER_PIXEL);
        int numChannelsValue = samplesPerPixelTag != null
                               ? ((Number) samplesPerPixelTag.getValue()).intValue()
                               : bitsPerSampleTag.valueCount();

        // SampleMSB
        Entry fillOrderTag = ifd.getEntryById(TIFF.TAG_FILL_ORDER);
        int fillOrder = fillOrderTag != null
                        ? ((Number) fillOrderTag.getValue()).intValue()
                        : TIFFBaseline.FILL_LEFT_TO_RIGHT;
        IIOMetadataNode sampleMSB = new IIOMetadataNode("SampleMSB");
        node.appendChild(sampleMSB);
        if (fillOrder == TIFFBaseline.FILL_LEFT_TO_RIGHT) {
            sampleMSB.setAttribute("value", createListValue(numChannelsValue, "0"));
        }
        else {
            if ("1".equals(bitsPerSampleValue)) {
                sampleMSB.setAttribute("value", createListValue(numChannelsValue, "7"));
            }
            else {
                // TODO: FixMe for bitsPerSample > 8
                sampleMSB.setAttribute("value", createListValue(numChannelsValue, "7"));
            }
        }

        return node;
    }

    // TODO: Candidate superclass method!
    private String createListValue(final int itemCount, final String... values) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < itemCount; i++) {
            if (buffer.length() > 0) {
                buffer.append(' ');
            }

            buffer.append(values[i % values.length]);
        }

        return buffer.toString();
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        // PixelAspectRatio
        Entry xResTag = ifd.getEntryById(TIFF.TAG_X_RESOLUTION);
        Entry yResTag = ifd.getEntryById(TIFF.TAG_Y_RESOLUTION);
        double xSizeValue = 1 / (xResTag == null ? 72.0 : ((Number) xResTag.getValue()).doubleValue());
        double ySizeValue = 1 / (xResTag == null ? 72.0 : ((Number) yResTag.getValue()).doubleValue());

        IIOMetadataNode pixelAspectRatio = new IIOMetadataNode("PixelAspectRatio");
        dimension.appendChild(pixelAspectRatio);
        pixelAspectRatio.setAttribute("value", String.valueOf(xSizeValue / ySizeValue));

        // ImageOrientation
        Entry orientationTag = ifd.getEntryById(TIFF.TAG_ORIENTATION);
        if (orientationTag != null) {
            int orientationValue = ((Number) orientationTag.getValue()).intValue();

            String value = null;
            switch (orientationValue) {
                case TIFFBaseline.ORIENTATION_TOPLEFT:
                    value = "Normal";
                    break;
                case TIFFExtension.ORIENTATION_TOPRIGHT:
                    value = "FlipH";
                    break;
                case TIFFExtension.ORIENTATION_BOTRIGHT:
                    value = "Rotate180";
                    break;
                case TIFFExtension.ORIENTATION_BOTLEFT:
                    value = "FlipV";
                    break;
                case TIFFExtension.ORIENTATION_LEFTTOP:
                    value = "FlipHRotate90";
                    break;
                case TIFFExtension.ORIENTATION_RIGHTTOP:
                    value = "Rotate270";
                    break;
                case TIFFExtension.ORIENTATION_RIGHTBOT:
                    value = "FlipVRotate90";
                    break;
                case TIFFExtension.ORIENTATION_LEFTBOT:
                    value = "Rotate90";
                    break;
            }

            if (value != null) {
                IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
                dimension.appendChild(imageOrientation);
                imageOrientation.setAttribute("value", value);
            }

        }

        Entry resUnitTag = ifd.getEntryById(TIFF.TAG_RESOLUTION_UNIT);
        int resUnitValue = resUnitTag == null ? TIFFBaseline.RESOLUTION_UNIT_DPI : ((Number) resUnitTag.getValue()).intValue();
        if (resUnitValue == TIFFBaseline.RESOLUTION_UNIT_CENTIMETER || resUnitValue == TIFFBaseline.RESOLUTION_UNIT_DPI) {
            // 10 mm in 1 cm or 25.4 mm in 1 inch
            double scale = resUnitValue == TIFFBaseline.RESOLUTION_UNIT_CENTIMETER ? 10 : 25.4;

            // HorizontalPixelSize
            // VerticalPixelSize
            IIOMetadataNode horizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
            dimension.appendChild(horizontalPixelSize);
            horizontalPixelSize.setAttribute("value", String.valueOf(xSizeValue * scale));

            IIOMetadataNode verticalPixelSize = new IIOMetadataNode("VerticalPixelSize");
            dimension.appendChild(verticalPixelSize);
            verticalPixelSize.setAttribute("value", String.valueOf(ySizeValue * scale));

            // HorizontalPosition
            // VerticalPosition
            Entry xPosTag = ifd.getEntryById(TIFF.TAG_X_POSITION);
            Entry yPosTag = ifd.getEntryById(TIFF.TAG_Y_POSITION);

            if (xPosTag != null && yPosTag != null) {
                double xPosValue = ((Number) xPosTag.getValue()).doubleValue();
                double yPosValue = ((Number) yPosTag.getValue()).doubleValue();

                IIOMetadataNode horizontalPosition = new IIOMetadataNode("HorizontalPosition");
                dimension.appendChild(horizontalPosition);
                horizontalPosition.setAttribute("value", String.valueOf(xPosValue * scale));

                IIOMetadataNode verticalPosition = new IIOMetadataNode("VerticalPosition");
                dimension.appendChild(verticalPosition);
                verticalPosition.setAttribute("value", String.valueOf(yPosValue * scale));
            }
        }

        return dimension;
    }

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        // Consult ExtraSamples
        Entry extraSamplesTag = ifd.getEntryById(TIFF.TAG_EXTRA_SAMPLES);

        if (extraSamplesTag != null) {
            int extraSamplesValue = (extraSamplesTag.getValue() instanceof Number)
                                    ? ((Number) extraSamplesTag.getValue()).intValue()
                                    : ((Number) Array.get(extraSamplesTag.getValue(), 0)).intValue();

            // Other values exists, these are not alpha
            if (extraSamplesValue == TIFFBaseline.EXTRASAMPLE_ASSOCIATED_ALPHA || extraSamplesValue == TIFFBaseline.EXTRASAMPLE_UNASSOCIATED_ALPHA) {
                IIOMetadataNode transparency = new IIOMetadataNode("Transparency");
                IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
                transparency.appendChild(alpha);

                alpha.setAttribute("value", extraSamplesValue == TIFFBaseline.EXTRASAMPLE_ASSOCIATED_ALPHA
                                            ? "premultiplied"
                                            : "nonpremultiplied");

                return transparency;
            }
        }

        return null;

    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");

        // FormatVersion, hardcoded to 6.0 (the current TIFF specification version),
        // as there's no format information in the TIFF structure.
        IIOMetadataNode formatVersion = new IIOMetadataNode("FormatVersion");
        document.appendChild(formatVersion);
        formatVersion.setAttribute("value", "6.0");

        // SubImageInterpretation from SubImageInterpretation (if applicable)
        Entry subFileTypeTag = ifd.getEntryById(TIFF.TAG_SUBFILE_TYPE);
        if (subFileTypeTag != null) {
            // NOTE: The JAI metadata is somewhat broken here, as these are bit flags, not values...
            String value = null;
            int subFileTypeValue = ((Number) subFileTypeTag.getValue()).intValue();
            if ((subFileTypeValue & TIFFBaseline.FILETYPE_MASK) != 0) {
                value = "TransparencyMask";
            }
            else if ((subFileTypeValue & TIFFBaseline.FILETYPE_REDUCEDIMAGE) != 0) {
                value = "ReducedResolution";
            }
            else if ((subFileTypeValue & TIFFBaseline.FILETYPE_PAGE) != 0) {
                value = "SinglePage";
            }

            // If no flag is set, we don't know...
            if (value != null) {
                IIOMetadataNode subImageInterpretation = new IIOMetadataNode("SubImageInterpretation");
                document.appendChild(subImageInterpretation);
                subImageInterpretation.setAttribute("value", value);
            }
        }

        // ImageCreationTime from DateTime
        Entry dateTimeTag = ifd.getEntryById(TIFF.TAG_DATE_TIME);
        if (dateTimeTag != null) {
            DateFormat format = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");

            try {
                IIOMetadataNode imageCreationTime = new IIOMetadataNode("ImageCreationTime");
                document.appendChild(imageCreationTime);

                Calendar date = Calendar.getInstance();
                date.setTime(format.parse(dateTimeTag.getValueAsString()));

                imageCreationTime.setAttribute("year", String.valueOf(date.get(Calendar.YEAR)));
                imageCreationTime.setAttribute("month", String.valueOf(date.get(Calendar.MONTH) + 1));
                imageCreationTime.setAttribute("day", String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
                imageCreationTime.setAttribute("hour", String.valueOf(date.get(Calendar.HOUR_OF_DAY)));
                imageCreationTime.setAttribute("minute", String.valueOf(date.get(Calendar.MINUTE)));
                imageCreationTime.setAttribute("second", String.valueOf(date.get(Calendar.SECOND)));
            }
            catch (ParseException ignore) {
                // Bad format...
            }
        }

        return document;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = new IIOMetadataNode("Text");

        // DocumentName, ImageDescription, Make, Model, PageName, Software, Artist, HostComputer, InkNames, Copyright:
        // /Text/TextEntry@keyword = field name, /Text/TextEntry@value = field value.
        addTextEntryIfPresent(text, TIFF.TAG_DOCUMENT_NAME);
        addTextEntryIfPresent(text, TIFF.TAG_IMAGE_DESCRIPTION);
        addTextEntryIfPresent(text, TIFF.TAG_MAKE);
        addTextEntryIfPresent(text, TIFF.TAG_MODEL);
        addTextEntryIfPresent(text, TIFF.TAG_SOFTWARE);
        addTextEntryIfPresent(text, TIFF.TAG_ARTIST);
        addTextEntryIfPresent(text, TIFF.TAG_HOST_COMPUTER);
        addTextEntryIfPresent(text, TIFF.TAG_INK_NAMES);
        addTextEntryIfPresent(text, TIFF.TAG_COPYRIGHT);

        return text.hasChildNodes() ? text : null;
    }

    private void addTextEntryIfPresent(final IIOMetadataNode text, final int tag) {
        Entry entry = ifd.getEntryById(tag);
        if (entry != null) {
            IIOMetadataNode node = new IIOMetadataNode("TextEntry");
            text.appendChild(node);
            node.setAttribute("keyword", entry.getFieldName());
            node.setAttribute("value", entry.getValueAsString());
        }
    }

    @Override
    protected IIOMetadataNode getStandardTileNode() {
        // TODO! Woot?! This node is not documented in the DTD (although the page mentions a "tile" node)..?
        // See http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html
        // See http://stackoverflow.com/questions/30910719/javax-imageio-1-0-standard-plug-in-neutral-metadata-format-tiling-information
        return super.getStandardTileNode();
    }
}
