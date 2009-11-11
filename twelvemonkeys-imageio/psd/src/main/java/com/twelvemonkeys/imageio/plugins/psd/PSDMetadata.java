package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.lang.StringUtil;
import org.w3c.dom.Node;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.IndexColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PSDMetadata
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDMetadata.java,v 1.0 Nov 4, 2009 5:28:12 PM haraldk Exp$
 */
public final class PSDMetadata extends IIOMetadata implements Cloneable {

    static final String NATIVE_METADATA_FORMAT_NAME = "com_twelvemonkeys_imageio_psd_1.0";
    static final String NATIVE_METADATA_FORMAT_CLASS_NAME = "com.twelvemonkeys.imageio.plugins.psd.PSDMetadataFormat";

    // TODO: Move fields from PSDImageReader (header, color map, resources, etc) here
    PSDHeader mHeader;
    PSDColorData mColorData;
    List<PSDImageResource> mImageResources;
    PSDGlobalLayerMask mGlobalLayerMask;
    List<PSDLayerInfo> mLayerInfo;

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

    private Node getNativeTree() {
        throw new UnsupportedOperationException("getNativeTree");
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
            case PSD.COLOR_MODE_DUOTONE: // Rationale is spec says treat as gray...
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
                // TODO: FixMe
                cs = "???";
                break;
            case PSD.COLOR_MODE_LAB:
                cs = "Lab";
                break;
            default:
                throw new AssertionError("Unreachable");
        }
        node.setAttribute("name", cs);
        chroma_node.appendChild(node);

        // TODO: Channels might be 5 for RGB + A + Mask...
        node = new IIOMetadataNode("NumChannels");
        node.setAttribute("value", Integer.toString(mHeader.mChannels));
        chroma_node.appendChild(node);

//        if (gAMA_present) {
//            node = new IIOMetadataNode("Gamma");
//            node.setAttribute("value", Float.toString(gAMA_gamma*1.0e-5F));
//            chroma_node.appendChild(node);
//        }

        // TODO: Check if this is correct with bitmap (monchrome)
        node = new IIOMetadataNode("BlackIsZero");
        node.setAttribute("value", "true");
        chroma_node.appendChild(node);

        if (mHeader.mMode == PSD.COLOR_MODE_INDEXED) {
            node = new IIOMetadataNode("Palette");

            IndexColorModel cm = mColorData.getIndexColorModel();
            for (int i = 0; i < cm.getMapSize(); i++) {
                IIOMetadataNode entry =
                    new IIOMetadataNode("PaletteEntry");
                entry.setAttribute("index", Integer.toString(i));
                entry.setAttribute("red",
                                   Integer.toString(cm.getRed(i)));
                entry.setAttribute("green",
                                   Integer.toString(cm.getGreen(i)));
                entry.setAttribute("blue",
                                   Integer.toString(cm.getBlue(i)));

                node.appendChild(entry);
            }
            chroma_node.appendChild(node);
        }

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

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression_node = new IIOMetadataNode("Compression");
        IIOMetadataNode node; // scratch node

        node = new IIOMetadataNode("CompressionTypeName");
        // TODO: Only if set... 
        node.setAttribute("value", "PackBits");
        compression_node.appendChild(node);

        node = new IIOMetadataNode("Lossless");
        node.setAttribute("value", "true");
        compression_node.appendChild(node);

//        compression_node.appendChild(node);

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

        List<PSDResolutionInfo> resolutionInfos = getResources(PSDResolutionInfo.class);
        if (!resolutionInfos.isEmpty()) {
            PSDResolutionInfo resolutionInfo = resolutionInfos.get(0);

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
        // TODO: PSDVersionInfo

//        if (!tIME_present) {
//            return null;
//        }
//
//        IIOMetadataNode document_node = new IIOMetadataNode("Document");
//        IIOMetadataNode node = null; // scratch node
//
//        node = new IIOMetadataNode("ImageModificationTime");
//        node.setAttribute("year", Integer.toString(tIME_year));
//        node.setAttribute("month", Integer.toString(tIME_month));
//        node.setAttribute("day", Integer.toString(tIME_day));
//        node.setAttribute("hour", Integer.toString(tIME_hour));
//        node.setAttribute("minute", Integer.toString(tIME_minute));
//        node.setAttribute("second", Integer.toString(tIME_second));
//        document_node.appendChild(node);
//
//        return document_node;
        return null;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        // TODO: CaptionDigest?, EXIF, XMP
        
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
        IIOMetadataNode transparency_node =
            new IIOMetadataNode("Transparency");
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

    // TODO: Replace with filter iterator?
    <T extends PSDImageResource> List<T> getResources(final Class<T> pResourceType) {
        List<T> filtered = null;

        for (PSDImageResource resource : mImageResources) {
            if (pResourceType.isInstance(resource)) {
                if (filtered == null) {
                    filtered = new ArrayList<T>();
                }

                filtered.add(pResourceType.cast(resource));
            }
        }

        return filtered;
    }
}
