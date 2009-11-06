package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.FilterIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
public final class PSDMetadata extends IIOMetadata implements Cloneable {

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
            null, // ... (until index 2999),
            "ANPA"
    };
    static final String[] DISPLAY_INFO_KINDS = {"selected", "protected"};

    protected PSDMetadata() {
        // TODO: Allow XMP, EXIF and IPTC as extra formats?
        super(true, NATIVE_METADATA_FORMAT_NAME, NATIVE_METADATA_FORMAT_CLASS_NAME, null, null);
    }

    @Override
    public boolean isReadOnly() {
        // TODO: Extract to abstract metadata impl class?
        return true;
    }

    @Override
    public Node getAsTree(final String pFormatName) {
        validateFormatName(pFormatName);

        if (pFormatName.equals(nativeMetadataFormatName)) {
            return getNativeTree();
        }
        else if (pFormatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        }

        throw new AssertionError("Unreachable");
    }

    @Override
    public void mergeTree(final String pFormatName, final Node pRoot) throws IIOInvalidTreeException {
        // TODO: Extract to abstract metadata impl class?
        assertMutable();

        validateFormatName(pFormatName);

        if (!pRoot.getNodeName().equals(nativeMetadataFormatName)) {
            throw new IIOInvalidTreeException("Root must be " + nativeMetadataFormatName, pRoot);
        }

        Node node = pRoot.getFirstChild();
        while (node != null) {
            // TODO: Merge values from node into this

            // Move to the next sibling
            node = node.getNextSibling();
        }
    }

    @Override
    public void reset() {
        // TODO: Extract to abstract metadata impl class?
        assertMutable();

        throw new UnsupportedOperationException("Method reset not implemented"); // TODO: Implement
    }

    // TODO: Extract to abstract metadata impl class?
    private void assertMutable() {
        if (isReadOnly()) {
            throw new IllegalStateException("Metadata is read-only");
        }
    }

    // TODO: Extract to abstract metadata impl class?
    private void validateFormatName(final String pFormatName) {
        String[] metadataFormatNames = getMetadataFormatNames();

        if (metadataFormatNames != null) {
            for (String metadataFormatName : metadataFormatNames) {
                if (metadataFormatName.equals(pFormatName)) {
                    return; // Found, we're ok!
                }
            }
        }

        throw new IllegalArgumentException(
                String.format("Bad format name: \"%s\". Expected one of %s", pFormatName, Arrays.toString(metadataFormatNames))
        );
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

    /// Native format support

    private Node getNativeTree() {
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
        IIOMetadataNode header = new IIOMetadataNode("PSDHeader");

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

            if (imageResource instanceof PSDAlphaChannelInfo) {
                PSDAlphaChannelInfo alphaChannelInfo = (PSDAlphaChannelInfo) imageResource;

                node = new IIOMetadataNode("AlphaChannelInfo");

                for (String name : alphaChannelInfo.mNames) {
                    IIOMetadataNode nameNode = new IIOMetadataNode("Name");
                    nameNode.setAttribute("value", name);
                    node.appendChild(nameNode);
                }

                resource.appendChild(node);
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

                resource.appendChild(node);
            }
            else if (imageResource instanceof PSDXMPData) {
                // TODO: Revise/rethink this...
                PSDXMPData xmp = (PSDXMPData) imageResource;

                node = new IIOMetadataNode("XMPData");

                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.parse(new InputSource(xmp.getData()));

                    // Set the entire XMP document as user data
                    node.setUserObject(document);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                resource.appendChild(node);
            }
            else {
                // Generic resource..
                node = new IIOMetadataNode(PSDImageResource.resourceTypeForId(imageResource.mId));

                resource.appendChild(node);
            }


            // TODO: More resources

            node.setAttribute("resourceId", Integer.toHexString(imageResource.mId));
        }

        return resource;
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
            return Integer.toHexString(pChannels) + "CLR";
        }

        throw new UnsupportedOperationException("Standard meta data format does not support more than 15 channels");
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("CompressionTypeName");
        String compression;
        switch (mCompression) {
            case PSD.COMPRESSION_NONE:
                compression = "none";
                break;
            case PSD.COMPRESSION_RLE:
                compression = "packbits";
                break;
            case PSD.COMPRESSION_ZIP:
            case PSD.COMPRESSION_ZIP_PREDICTION:
                compression = "zip";
                break;
            default:
                throw new AssertionError("Unreachable");
        }
        node.setAttribute("value", compression);
        compression_node.appendChild(node);

        node = new IIOMetadataNode("Lossless");
        node.setAttribute("value", "true");
        compression_node.appendChild(node);

        return compression_node;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data_node = new IIOMetadataNode("Data");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("PlanarConfiguration");
        node.setAttribute("value", "PlaneInterleaved"); // TODO: Check with spec
        data_node.appendChild(node);

        node = new IIOMetadataNode("SampleFormat");
        node.setAttribute("value", mHeader.mMode == PSD.COLOR_MODE_INDEXED ? "Index" : "UnsignedIntegral");
        data_node.appendChild(node);

        String bitDepth = Integer.toString(mHeader.mBits); // bits per plane
        // TODO: Channels might be 5 for RGB + A + Mask...
        String[] bps = new String[mHeader.mChannels];
        Arrays.fill(bps, bitDepth);

        node = new IIOMetadataNode("BitsPerSample");
        node.setAttribute("value", StringUtil.toCSVString(bps, " "));
        data_node.appendChild(node);

        // TODO: SampleMSB? Or is network (aka Motorola/big endian) byte order assumed?

        return data_node;
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension_node = new IIOMetadataNode("Dimension");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("PixelAspectRatio");
        // TODO: This is not incorrect wrt resolution info  
        float ratio = 1f;
        node.setAttribute("value", Float.toString(ratio));
        dimension_node.appendChild(node);

        node = new IIOMetadataNode("ImageOrientation");
        node.setAttribute("value", "Normal");
        dimension_node.appendChild(node);

        Iterator<PSDResolutionInfo> resolutionInfos = getResources(PSDResolutionInfo.class);
        if (!resolutionInfos.hasNext()) {
            PSDResolutionInfo resolutionInfo = resolutionInfos.next();

            node = new IIOMetadataNode("HorizontalPixelSize");
            node.setAttribute("value", Float.toString(asMM(resolutionInfo.mHResUnit, resolutionInfo.mHRes)));
            dimension_node.appendChild(node);

            node = new IIOMetadataNode("VerticalPixelSize");
            node.setAttribute("value", Float.toString(asMM(resolutionInfo.mVResUnit, resolutionInfo.mVRes)));
            dimension_node.appendChild(node);
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
        return dimension_node;
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
            PSDEXIF1Data.Entry dateTime = data.mDirectory.get(0x0132); // TODO: Constant
            if (dateTime != null) {
                node = new IIOMetadataNode("ImageModificationTime");
                // Format: "YYYY:MM:DD hh:mm:ss" (with quotes! :-P)
                String value = dateTime.getValueAsString();

                node.setAttribute("year", value.substring(1, 5));
                node.setAttribute("month", value.substring(6, 8));
                node.setAttribute("day", value.substring(9, 11));
                node.setAttribute("hour", value.substring(12, 14));
                node.setAttribute("minute", value.substring(15, 17));
                node.setAttribute("second", value.substring(18, 20));

                document_node.appendChild(node);
            }
        }

        return document_node;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        // TODO: CaptionDigest?, EXIF, XMP

        Iterator<PSDImageResource> textResources = getResources(PSDEXIF1Data.class, PSDXMPData.class);

        while (textResources.hasNext()) {
            PSDImageResource textResource = textResources.next();

        }
        
//        int numEntries = tEXt_keyword.size() +
//            iTXt_keyword.size() + zTXt_keyword.size();
//        if (numEntries == 0) {
//            return null;
//        }
//
//        IIOMetadataNode text_node = new IIOMetadataNode("Text");
//        IIOMetadataNode node = null; // scratch node
//
//        for (int i = 0; i < tEXt_keyword.size(); i++) {
//            node = new IIOMetadataNode("TextEntry");
//            node.setAttribute("keyword", (String)tEXt_keyword.get(i));
//            node.setAttribute("value", (String)tEXt_text.get(i));
//            node.setAttribute("encoding", "ISO-8859-1");
//            node.setAttribute("compression", "none");
//
//            text_node.appendChild(node);
//        }
//
//        for (int i = 0; i < iTXt_keyword.size(); i++) {
//            node = new IIOMetadataNode("TextEntry");
//            node.setAttribute("keyword", iTXt_keyword.get(i));
//            node.setAttribute("value", iTXt_text.get(i));
//            node.setAttribute("language",
//                              iTXt_languageTag.get(i));
//            if (iTXt_compressionFlag.get(i)) {
//                node.setAttribute("compression", "deflate");
//            } else {
//                node.setAttribute("compression", "none");
//            }
//
//            text_node.appendChild(node);
//        }
//
//        for (int i = 0; i < zTXt_keyword.size(); i++) {
//            node = new IIOMetadataNode("TextEntry");
//            node.setAttribute("keyword", (String)zTXt_keyword.get(i));
//            node.setAttribute("value", (String)zTXt_text.get(i));
//            node.setAttribute("compression", "deflate");
//
//            text_node.appendChild(node);
//        }
//
//        return text_node;
        return null;

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
        node.setAttribute("value", hasAlpha() ? "nonpremultipled" : "none"); // TODO: Check  spec
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

    Iterator<PSDImageResource> getResources(final Class<? extends PSDImageResource>... pResourceTypes) {
        Iterator<PSDImageResource> iterator = mImageResources.iterator();

        return new FilterIterator<PSDImageResource>(iterator, new FilterIterator.Filter<PSDImageResource>() {
            public boolean accept(final PSDImageResource pElement) {
                for (Class<?> type : pResourceTypes) {
                    if (type.isInstance(pElement)) {
                        return true;
                    }
                }

                return false;
            }
        });
    }
}
