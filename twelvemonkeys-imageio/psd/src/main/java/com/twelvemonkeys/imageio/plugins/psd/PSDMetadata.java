package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.metadata.iptc.IPTC;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.FilterIterator;
import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.IndexColorModel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * PSDMetadata
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDMetadata.java,v 1.0 Nov 4, 2009 5:28:12 PM haraldk Exp$
 */
public final class PSDMetadata extends AbstractMetadata {

    // TODO: Decide on image/stream metadata...
    static final String NATIVE_METADATA_FORMAT_NAME = "com_twelvemonkeys_imageio_psd_image_1.0";
    static final String NATIVE_METADATA_FORMAT_CLASS_NAME = "com.twelvemonkeys.imageio.plugins.psd.PSDMetadataFormat";

    PSDHeader mHeader;
    PSDColorData mColorData;
    int mCompression = -1;
    List<PSDImageResource> mImageResources;
    PSDGlobalLayerMask mGlobalLayerMask;
    List<PSDLayerInfo> mLayerInfo;

    static final String[] COLOR_MODES = {
            "MONOCHROME", "GRAYSCALE", "INDEXED", "RGB", "CMYK", null, null, "MULTICHANNEL", "DUOTONE", "LAB"
    };

    static final String[] DISPLAY_INFO_CS = {
            "RGB", "HSB", "CMYK", "PANTONE", "FOCOLTONE", "TRUMATCH", "TOYO", "LAB", "GRAYSCALE", null, "HKS", "DIC",
            null, // TODO: ... (until index 2999),
            "ANPA"
    };
    static final String[] DISPLAY_INFO_KINDS = {"selected", "protected"};

    static final String[] RESOLUTION_UNITS = {null, "pixels/inch", "pixels/cm"};
    static final String[] DIMENSION_UNITS = {null, "in", "cm", "pt", "picas", "columns"};

    static final String[] JAVA_CS = {
            "XYZ", "Lab", "Yuv", "YCbCr", "Yxy", "RGB", "GRAY", "HSV", "HLS", "CMYK", "CMY",
            "2CLR", "3CLR", "4CLR", "5CLR", "6CLR", "7CLR", "8CLR", "9CLR", "ACLR", "BCLR", "CCLR", "DCLR", "ECLR", "FCLR"
    };

    static final String[] GUIDE_ORIENTATIONS = {"vertical", "horizontal"};

    static final String[] PRINT_SCALE_STYLES = {"centered", "scaleToFit", "userDefined"};

    protected PSDMetadata() {
        // TODO: Allow XMP, EXIF and IPTC as extra formats?
        super(true, NATIVE_METADATA_FORMAT_NAME, NATIVE_METADATA_FORMAT_CLASS_NAME, null, null);
    }

    /// Native format support

    @Override
    protected Node getNativeTree() {
        IIOMetadataNode root = new IIOMetadataNode(NATIVE_METADATA_FORMAT_NAME);

        root.appendChild(createHeaderNode());

        if (mHeader.mMode == PSD.COLOR_MODE_INDEXED) {
            root.appendChild(createPaletteNode());
        }

        if (mImageResources != null && !mImageResources.isEmpty()) {
            root.appendChild(createImageResourcesNode());
        }
        
        return root;
    }

    private Node createHeaderNode() {
        IIOMetadataNode header = new IIOMetadataNode("Header");

        header.setAttribute("type", "PSD");
        header.setAttribute("version", "1");
        header.setAttribute("channels", Integer.toString(mHeader.mChannels));
        header.setAttribute("height", Integer.toString(mHeader.mHeight));
        header.setAttribute("width", Integer.toString(mHeader.mWidth));
        header.setAttribute("bits", Integer.toString(mHeader.mBits));
        header.setAttribute("mode", COLOR_MODES[mHeader.mMode]);

        return header;
    }

    private Node createImageResourcesNode() {
        IIOMetadataNode resource = new IIOMetadataNode("ImageResources");
        IIOMetadataNode node;

        for (PSDImageResource imageResource : mImageResources) {
            // TODO: Always add name (if set) and id (as resourceId) to all nodes?
            // Resource Id is useful for people with access to the PSD spec..

            if (imageResource instanceof ICCProfile) {
                ICCProfile profile = (ICCProfile) imageResource;

                // TODO: Format spec
                node = new IIOMetadataNode("ICCProfile");
                node.setAttribute("colorSpaceType", JAVA_CS[profile.getProfile().getColorSpaceType()]);
//
//                FastByteArrayOutputStream data = new FastByteArrayOutputStream(0);
//                EncoderStream base64 = new EncoderStream(data, new Base64Encoder(), true);
//
//                try {
//                    base64.write(profile.getProfile().getData());
//                }
//                catch (IOException ignore) {
//                }
//
//                byte[] bytes = data.toByteArray();
//                node.setAttribute("data", StringUtil.decode(bytes, 0, bytes.length, "ASCII"));
                node.setUserObject(profile.getProfile());
            }
            else if (imageResource instanceof PSDAlphaChannelInfo) {
                PSDAlphaChannelInfo alphaChannelInfo = (PSDAlphaChannelInfo) imageResource;

                node = new IIOMetadataNode("AlphaChannelInfo");

                for (String name : alphaChannelInfo.mNames) {
                    IIOMetadataNode nameNode = new IIOMetadataNode("Name");
                    nameNode.setAttribute("value", name);
                    node.appendChild(nameNode);
                }
            }
            else if (imageResource instanceof PSDDisplayInfo) {
                PSDDisplayInfo displayInfo = (PSDDisplayInfo) imageResource;

                node = new IIOMetadataNode("DisplayInfo");
                node.setAttribute("colorSpace", DISPLAY_INFO_CS[displayInfo.mColorSpace]);
                
                StringBuilder builder = new StringBuilder();

                for (short color : displayInfo.mColors) {
                    if (builder.length() > 0) {
                        builder.append(" ");
                    }

                    builder.append(Integer.toString(color));
                }

                node.setAttribute("colors", builder.toString());
                node.setAttribute("opacity", Integer.toString(displayInfo.mOpacity));
                node.setAttribute("kind", DISPLAY_INFO_KINDS[displayInfo.mKind]);
            }
            else if (imageResource instanceof PSDGridAndGuideInfo) {
                PSDGridAndGuideInfo info = (PSDGridAndGuideInfo) imageResource;

                node = new IIOMetadataNode("GridAndGuideInfo");
                node.setAttribute("version", String.valueOf(info.mVersion));
                node.setAttribute("verticalGridCycle", String.valueOf(info.mGridCycleVertical));
                node.setAttribute("horizontalGridCycle", String.valueOf(info.mGridCycleHorizontal));

                for (PSDGridAndGuideInfo.GuideResource guide : info.mGuides) {
                    IIOMetadataNode guideNode = new IIOMetadataNode("Guide");
                    guideNode.setAttribute("location", Integer.toString(guide.mLocation));
                    guideNode.setAttribute("orientation", GUIDE_ORIENTATIONS[guide.mDirection]);
                }
            }
            else if (imageResource instanceof PSDPixelAspectRatio) {
                PSDPixelAspectRatio aspectRatio = (PSDPixelAspectRatio) imageResource;

                node = new IIOMetadataNode("PixelAspectRatio");
                node.setAttribute("version", String.valueOf(aspectRatio.mVersion));
                node.setAttribute("aspectRatio", String.valueOf(aspectRatio.mAspect));
            }
            else if (imageResource instanceof PSDPrintFlags) {
                PSDPrintFlags flags = (PSDPrintFlags) imageResource;

                node = new IIOMetadataNode("PrintFlags");
                node.setAttribute("labels", String.valueOf(flags.mLabels));
                node.setAttribute("cropMarks", String.valueOf(flags.mCropMasks));
                node.setAttribute("colorBars", String.valueOf(flags.mColorBars));
                node.setAttribute("registrationMarks", String.valueOf(flags.mRegistrationMarks));
                node.setAttribute("negative", String.valueOf(flags.mNegative));
                node.setAttribute("flip", String.valueOf(flags.mFlip));
                node.setAttribute("interpolate", String.valueOf(flags.mInterpolate));
                node.setAttribute("caption", String.valueOf(flags.mCaption));
            }
            else if (imageResource instanceof PSDPrintFlagsInformation) {
                PSDPrintFlagsInformation information = (PSDPrintFlagsInformation) imageResource;

                node = new IIOMetadataNode("PrintFlagsInformation");
                node.setAttribute("version", String.valueOf(information.mVersion));
                node.setAttribute("cropMarks", String.valueOf(information.mCropMasks));
                node.setAttribute("field", String.valueOf(information.mField));
                node.setAttribute("bleedWidth", String.valueOf(information.mBleedWidth));
                node.setAttribute("bleedScale", String.valueOf(information.mBleedScale));
            }
            else if (imageResource instanceof PSDPrintScale) {
                PSDPrintScale printScale = (PSDPrintScale) imageResource;

                node = new IIOMetadataNode("PrintScale");
                node.setAttribute("style", PRINT_SCALE_STYLES[printScale.mStyle]);
                node.setAttribute("xLocation", String.valueOf(printScale.mXLocation));
                node.setAttribute("yLocation", String.valueOf(printScale.mYlocation));
                node.setAttribute("scale", String.valueOf(printScale.mScale));
            }
            else if (imageResource instanceof PSDResolutionInfo) {
                PSDResolutionInfo information = (PSDResolutionInfo) imageResource;

                node = new IIOMetadataNode("ResolutionInfo");
                node.setAttribute("horizontalResolution", String.valueOf(information.mHRes));
                node.setAttribute("horizontalResolutionUnit", RESOLUTION_UNITS[information.mHResUnit]);
                node.setAttribute("widthUnit", DIMENSION_UNITS[information.mWidthUnit]);
                node.setAttribute("verticalResolution", String.valueOf(information.mVRes));
                node.setAttribute("verticalResolutionUnit", RESOLUTION_UNITS[information.mVResUnit]);
                node.setAttribute("heightUnit", DIMENSION_UNITS[information.mHeightUnit]);
            }
            else if (imageResource instanceof PSDUnicodeAlphaNames) {
                PSDUnicodeAlphaNames alphaNames = (PSDUnicodeAlphaNames) imageResource;

                node = new IIOMetadataNode("UnicodeAlphaNames");

                for (String name : alphaNames.mNames) {
                    IIOMetadataNode nameNode = new IIOMetadataNode("Name");
                    nameNode.setAttribute("value", name);
                    node.appendChild(nameNode);
                }
            }
            else if (imageResource instanceof PSDVersionInfo) {
                PSDVersionInfo information = (PSDVersionInfo) imageResource;

                node = new IIOMetadataNode("VersionInfo");
                node.setAttribute("version", String.valueOf(information.mVersion));
                node.setAttribute("hasRealMergedData", String.valueOf(information.mHasRealMergedData));
                node.setAttribute("writer", information.mWriter);
                node.setAttribute("reader", information.mReader);
                node.setAttribute("fileVersion", String.valueOf(information.mFileVersion));
            }
            else if (imageResource instanceof PSDThumbnail) {
                // TODO: Revise/rethink this...
                PSDThumbnail thumbnail = (PSDThumbnail) imageResource;

                node = new IIOMetadataNode("Thumbnail");
                // TODO: Thumbnail attributes + access to data, to avoid JPEG re-compression problems
                node.setUserObject(thumbnail.getThumbnail());
            }
            else if (imageResource instanceof PSDIPTCData) {
                // TODO: Revise/rethink this...
                PSDIPTCData iptc = (PSDIPTCData) imageResource;

                node = new IIOMetadataNode("DirectoryResource");
                node.setAttribute("type", "IPTC");
                node.setUserObject(iptc.mDirectory);

                appendEntries(node, "IPTC", iptc.mDirectory);
            }
            else if (imageResource instanceof PSDEXIF1Data) {
                // TODO: Revise/rethink this...
                PSDEXIF1Data exif = (PSDEXIF1Data) imageResource;

                node = new IIOMetadataNode("DirectoryResource");
                node.setAttribute("type", "EXIF");
                // TODO: Set byte[] data instead
                node.setUserObject(exif.mDirectory);

                appendEntries(node, "EXIF", exif.mDirectory);
            }
            else if (imageResource instanceof PSDXMPData) {
                // TODO: Revise/rethink this... Would it be possible to parse XMP as IIOMetadataNodes? Or is that just stupid...
                // Or maybe use the Directory approach used by IPTC and EXIF.. 
                PSDXMPData xmp = (PSDXMPData) imageResource;

                node = new IIOMetadataNode("DirectoryResource");
                node.setAttribute("type", "XMP");
                appendEntries(node, "XMP", xmp.mDirectory);

                // Set the entire XMP document as user data
                node.setUserObject(xmp.mData);
            }
            else {
                // Generic resource..
                node = new IIOMetadataNode("ImageResource");
                String value = PSDImageResource.resourceTypeForId(imageResource.mId);
                if (!"UnknownResource".equals(value)) {
                    node.setAttribute("name", value);
                }
                node.setAttribute("length", String.valueOf(imageResource.mSize));
                // TODO: Set user object: byte array
            }

            // TODO: More resources

            node.setAttribute("resourceId", String.format("0x%04x", imageResource.mId));
            resource.appendChild(node);
        }

        // TODO: Layers and layer info

        // TODO: Global mask etc..

        return resource;
    }

    private void appendEntries(final IIOMetadataNode pNode, final String pType, final Directory pDirectory) {
        for (Entry entry : pDirectory) {
            Object tagId = entry.getIdentifier();

            IIOMetadataNode tag = new IIOMetadataNode("Entry");
            tag.setAttribute("tag", String.format("%s", tagId));

            String field = entry.getFieldName();
            if (field != null) {
                tag.setAttribute("field", String.format("%s", field));
            }
            else {
                if ("IPTC".equals(pType)) {
                    tag.setAttribute("field", String.format("%s:%s", (Integer) tagId >> 8, (Integer) tagId & 0xff));
                }
            }

            if (entry.getValue() instanceof Directory) {
                appendEntries(tag, pType, (Directory) entry.getValue());
                tag.setAttribute("type", "Directory");
            }
            else {
                tag.setAttribute("value", entry.getValueAsString());
                tag.setAttribute("type", entry.getTypeName());
            }

            pNode.appendChild(tag);
        }
    }

    /// Standard format support

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma_node = new IIOMetadataNode("Chroma");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("ColorSpaceType");
        String cs;
        switch (mHeader.mMode) {
            case PSD.COLOR_MODE_MONOCHROME:
            case PSD.COLOR_MODE_GRAYSCALE:
            case PSD.COLOR_MODE_DUOTONE: // Rationale: Spec says treat as gray...
                cs = "GRAY";
                break;
            case PSD.COLOR_MODE_RGB:
            case PSD.COLOR_MODE_INDEXED:
                cs = "RGB";
                break;
            case PSD.COLOR_MODE_CMYK:
                cs = "CMYK";
                break;
            case PSD.COLOR_MODE_MULTICHANNEL:
                cs = getMultiChannelCS(mHeader.mChannels);
                break;
            case PSD.COLOR_MODE_LAB:
                cs = "Lab";
                break;
            default:
                throw new AssertionError("Unreachable");
        }
        node.setAttribute("name", cs);
        chroma_node.appendChild(node);

        // TODO: Channels might be 5 for RGB + A + Mask... Probably not correct
        node = new IIOMetadataNode("NumChannels");
        node.setAttribute("value", Integer.toString(mHeader.mChannels));
        chroma_node.appendChild(node);

        // TODO: Check if this is correct with bitmap (monchrome)
        node = new IIOMetadataNode("BlackIsZero");
        node.setAttribute("value", "true");
        chroma_node.appendChild(node);

        if (mHeader.mMode == PSD.COLOR_MODE_INDEXED) {
            node = createPaletteNode();
            chroma_node.appendChild(node);
        }

        // TODO: Hardcode background color to white?
//        if (bKGD_present) {
//            if (bKGD_colorType == PNGImageReader.PNG_COLOR_PALETTE) {
//                node = new IIOMetadataNode("BackgroundIndex");
//                node.setAttribute("value", Integer.toString(bKGD_index));
//            } else {
//                node = new IIOMetadataNode("BackgroundColor");
//                int r, g, b;
//
//                if (bKGD_colorType == PNGImageReader.PNG_COLOR_GRAY) {
//                    r = g = b = bKGD_gray;
//                } else {
//                    r = bKGD_red;
//                    g = bKGD_green;
//                    b = bKGD_blue;
//                }
//                node.setAttribute("red", Integer.toString(r));
//                node.setAttribute("green", Integer.toString(g));
//                node.setAttribute("blue", Integer.toString(b));
//            }
//            chroma_node.appendChild(node);
//        }

        return chroma_node;
    }

    private IIOMetadataNode createPaletteNode() {
        IIOMetadataNode node = new IIOMetadataNode("Palette");
        IndexColorModel cm = mColorData.getIndexColorModel();

        for (int i = 0; i < cm.getMapSize(); i++) {
            IIOMetadataNode entry = new IIOMetadataNode("PaletteEntry");
            entry.setAttribute("index", Integer.toString(i));
            entry.setAttribute("red", Integer.toString(cm.getRed(i)));
            entry.setAttribute("green", Integer.toString(cm.getGreen(i)));
            entry.setAttribute("blue", Integer.toString(cm.getBlue(i)));

            node.appendChild(entry);
        }

        return node;
    }

    private String getMultiChannelCS(short pChannels) {
        if (pChannels < 16) {
            return String.format("%xCLR", pChannels);
        }

        throw new UnsupportedOperationException("Standard meta data format does not support more than 15 channels");
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compressionNode = new IIOMetadataNode("Compression");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("CompressionTypeName");
        String compression;

        switch (mCompression) {
            case PSD.COMPRESSION_NONE:
                compression = "none";
                break;
            case PSD.COMPRESSION_RLE:
                compression = "PackBits";
                break;
            case PSD.COMPRESSION_ZIP:
            case PSD.COMPRESSION_ZIP_PREDICTION:
                compression = "Deflate"; // TODO: ZLib? (TIFF native metadata format specifies both.. :-P)
                break;
            default:
                throw new AssertionError("Unreachable");
        }

        node.setAttribute("value", compression);
        compressionNode.appendChild(node);

        // TODO: Does it make sense to specify lossless for compression "none"?
        node = new IIOMetadataNode("Lossless");
        node.setAttribute("value", "true");
        compressionNode.appendChild(node);

        return compressionNode;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode dataNode = new IIOMetadataNode("Data");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("PlanarConfiguration");
        node.setAttribute("value", "PlaneInterleaved"); // TODO: Check with spec
        dataNode.appendChild(node);

        node = new IIOMetadataNode("SampleFormat");
        node.setAttribute("value", mHeader.mMode == PSD.COLOR_MODE_INDEXED ? "Index" : "UnsignedIntegral");
        dataNode.appendChild(node);

        String bitDepth = Integer.toString(mHeader.mBits); // bits per plane

        // TODO: Channels might be 5 for RGB + A + Mask...
        String[] bps = new String[mHeader.mChannels];
        Arrays.fill(bps, bitDepth);

        node = new IIOMetadataNode("BitsPerSample");
        node.setAttribute("value", StringUtil.toCSVString(bps, " "));
        dataNode.appendChild(node);

        // TODO: SampleMSB? Or is network (aka Motorola/big endian) byte order assumed?

        return dataNode;
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimensionNode = new IIOMetadataNode("Dimension");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("PixelAspectRatio");

        // TODO: This is not correct wrt resolution info
        float aspect = 1f;

        Iterator<PSDPixelAspectRatio> ratios = getResources(PSDPixelAspectRatio.class);
        if (ratios.hasNext()) {
            PSDPixelAspectRatio ratio = ratios.next();
            aspect = (float) ratio.mAspect;
        }

        node.setAttribute("value", Float.toString(aspect));
        dimensionNode.appendChild(node);

        node = new IIOMetadataNode("ImageOrientation");
        node.setAttribute("value", "Normal");
        dimensionNode.appendChild(node);

        // TODO: If no PSDResolutionInfo, this might still be available in the EXIF data...
        Iterator<PSDResolutionInfo> resolutionInfos = getResources(PSDResolutionInfo.class);
        if (!resolutionInfos.hasNext()) {
            PSDResolutionInfo resolutionInfo = resolutionInfos.next();

            node = new IIOMetadataNode("HorizontalPixelSize");
            node.setAttribute("value", Float.toString(asMM(resolutionInfo.mHResUnit, resolutionInfo.mHRes)));
            dimensionNode.appendChild(node);

            node = new IIOMetadataNode("VerticalPixelSize");
            node.setAttribute("value", Float.toString(asMM(resolutionInfo.mVResUnit, resolutionInfo.mVRes)));
            dimensionNode.appendChild(node);
        }

        // TODO:
        /*
      <!ELEMENT "HorizontalPixelOffset" EMPTY>
        <!-- The horizonal position, in pixels, where the image should be
             rendered onto a raster display -->
        <!ATTLIST "HorizontalPixelOffset" "value" #CDATA #REQUIRED>
          <!-- Data type: Integer -->

      <!ELEMENT "VerticalPixelOffset" EMPTY>
        <!-- The vertical position, in pixels, where the image should be
             rendered onto a raster display -->
        <!ATTLIST "VerticalPixelOffset" "value" #CDATA #REQUIRED>
          <!-- Data type: Integer -->

      <!ELEMENT "HorizontalScreenSize" EMPTY>
        <!-- The width, in pixels, of the raster display into which the
             image should be rendered -->
        <!ATTLIST "HorizontalScreenSize" "value" #CDATA #REQUIRED>
          <!-- Data type: Integer -->

      <!ELEMENT "VerticalScreenSize" EMPTY>
        <!-- The height, in pixels, of the raster display into which the
             image should be rendered -->
        <!ATTLIST "VerticalScreenSize" "value" #CDATA #REQUIRED>
          <!-- Data type: Integer -->

         */
        return dimensionNode;
    }

    private static float asMM(final short pUnit, final float pResolution) {
        // Unit: 1 -> pixels per inch, 2 -> pixels pr cm   
        return (pUnit == 1 ? 25.4f : 10) / pResolution;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document_node = new IIOMetadataNode("Document");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("FormatVersion");
        node.setAttribute("value", "1"); // PSD format version is always 1
        document_node.appendChild(node);

        // Get EXIF data if present
        Iterator<PSDEXIF1Data> exif = getResources(PSDEXIF1Data.class);
        if (exif.hasNext()) {
            PSDEXIF1Data data = exif.next();

            // Get the EXIF DateTime (aka ModifyDate) tag if present
            Entry dateTime = data.mDirectory.getEntryById(TIFF.TAG_DATE_TIME);
            if (dateTime != null) {
                node = new IIOMetadataNode("ImageCreationTime"); // As TIFF, but could just as well be ImageModificationTime
                // Format: "YYYY:MM:DD hh:mm:ss"
                String value = dateTime.getValueAsString();

                node.setAttribute("year", value.substring(0, 4));
                node.setAttribute("month", value.substring(5, 7));
                node.setAttribute("day", value.substring(8, 10));
                node.setAttribute("hour", value.substring(11, 13));
                node.setAttribute("minute", value.substring(14, 16));
                node.setAttribute("second", value.substring(17, 19));

                document_node.appendChild(node);
            }
        }

        return document_node;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        // TODO: TIFF uses
        // DocumentName, ImageDescription, Make, Model, PageName, Software, Artist, HostComputer, InkNames, Copyright:
        // /Text/TextEntry@keyword = field name, /Text/TextEntry@value = field value.
        // Example: TIFF Software field => /Text/TextEntry@keyword = "Software",
        //          /Text/TextEntry@value = Name and version number of the software package(s) used to create the image.

        Iterator<PSDImageResource> textResources = getResources(PSD.RES_IPTC_NAA, PSD.RES_EXIF_DATA_1, PSD.RES_XMP_DATA);

        if (!textResources.hasNext()) {
            return null;
        }

        IIOMetadataNode text = new IIOMetadataNode("Text");
        IIOMetadataNode node;

        // TODO: Alpha channel names? (PSDAlphaChannelInfo/PSDUnicodeAlphaNames)
        // TODO: Reader/writer (PSDVersionInfo)

        while (textResources.hasNext()) {
            PSDImageResource textResource = textResources.next();

            if (textResource instanceof PSDIPTCData) {
                PSDIPTCData iptc = (PSDIPTCData) textResource;

                appendTextEntriesFlat(text, iptc.mDirectory, new FilterIterator.Filter<Entry>() {
                    public boolean accept(final Entry pEntry) {
                        Integer tagId = (Integer) pEntry.getIdentifier();

                        switch (tagId) {
                            case IPTC.TAG_SOURCE:
                                return true;
                            default:
                                return false;
                        }
                    }
                });
            }
            else if (textResource instanceof PSDEXIF1Data) {
                PSDEXIF1Data exif = (PSDEXIF1Data) textResource;

                appendTextEntriesFlat(text, exif.mDirectory, new FilterIterator.Filter<Entry>() {
                    public boolean accept(final Entry pEntry) {
                        Integer tagId = (Integer) pEntry.getIdentifier();

                        switch (tagId) {
                            case TIFF.TAG_SOFTWARE:
                            case TIFF.TAG_ARTIST:
                            case TIFF.TAG_COPYRIGHT:
                                return true;
                            default:
                                return false;
                        }
                    }
                });
            }
            else if (textResource instanceof PSDXMPData) {
                // TODO: Parse XMP (heavy) ONLY if we don't have required fields from IPTC/EXIF?
                // TODO: Use XMP IPTC/EXIF/TIFFF NativeDigest field to validate if the values are in sync...
                PSDXMPData xmp = (PSDXMPData) textResource;
            }
        }

        return text;
    }

    private void appendTextEntriesFlat(final IIOMetadataNode pNode, final Directory pDirectory, final FilterIterator.Filter<Entry> pFilter) {
        FilterIterator<Entry> pEntries = new FilterIterator<Entry>(pDirectory.iterator(), pFilter);
        while (pEntries.hasNext()) {
            Entry entry = pEntries.next();

            if (entry.getValue() instanceof Directory) {
                appendTextEntriesFlat(pNode, (Directory) entry.getValue(), pFilter);
            }
            else if (entry.getValue() instanceof String) {
                IIOMetadataNode tag = new IIOMetadataNode("TextEntry");
                String fieldName = entry.getFieldName();

                if (fieldName != null) {
                    tag.setAttribute("keyword", String.format("%s", fieldName));
                }
                else {
                    // TODO: This should never happen, as we filter out only specific nodes
                    tag.setAttribute("keyword", String.format("%s", entry.getIdentifier()));
                }

                tag.setAttribute("value", entry.getValueAsString());
                pNode.appendChild(tag);
            }
        }
    }

    @Override
    protected IIOMetadataNode getStandardTileNode() {
        return super.getStandardTileNode();
    }

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        IIOMetadataNode transparency_node = new IIOMetadataNode("Transparency");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("Alpha");
        node.setAttribute("value", hasAlpha() ? "nonpremultiplied" : "none"); // TODO: Check  spec
        transparency_node.appendChild(node);

        return transparency_node;
    }

    private boolean hasAlpha() {
        return mHeader.mMode == PSD.COLOR_MODE_RGB && mHeader.mChannels >= 4 ||
                mHeader.mMode == PSD.COLOR_MODE_CMYK & mHeader.mChannels >= 5;
    }

    <T extends PSDImageResource> Iterator<T> getResources(final Class<T> pResourceType) {
        // NOTE: The cast here is wrong, strictly speaking, but it does not matter...
        @SuppressWarnings({"unchecked"})
        Iterator<T> iterator = (Iterator<T>) mImageResources.iterator();

        return new FilterIterator<T>(iterator, new FilterIterator.Filter<T>() {
            public boolean accept(final T pElement) {
                return pResourceType.isInstance(pElement);
            }
        });
    }

    Iterator<PSDImageResource> getResources(final int... pResourceTypes) {
        Iterator<PSDImageResource> iterator = mImageResources.iterator();

        return new FilterIterator<PSDImageResource>(iterator, new FilterIterator.Filter<PSDImageResource>() {
            public boolean accept(final PSDImageResource pResource) {
                for (int type : pResourceTypes) {
                    if (type == pResource.mId) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    @Override
    public Object clone() {
        // TODO: Make it a deep clone
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
