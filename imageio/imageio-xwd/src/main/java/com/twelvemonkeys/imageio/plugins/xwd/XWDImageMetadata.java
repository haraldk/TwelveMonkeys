package com.twelvemonkeys.imageio.plugins.xwd;

import com.twelvemonkeys.imageio.AbstractMetadata;

import javax.imageio.metadata.IIOMetadataNode;
import java.nio.ByteOrder;

import static com.twelvemonkeys.lang.Validate.notNull;

final class XWDImageMetadata extends AbstractMetadata {
    private final XWDX11Header header;

    XWDImageMetadata(XWDX11Header header) {
        super(true, null, null, null, null);
        this.header = notNull(header, "header");
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");
        IIOMetadataNode colorSpaceType = new IIOMetadataNode("ColorSpaceType");

        switch (header.visualClass) {
            case X11.VISUAL_CLASS_STATIC_GRAY:
            case X11.VISUAL_CLASS_GRAY_SCALE:
                colorSpaceType.setAttribute("name", "GRAY");
                break;
            default:
                colorSpaceType.setAttribute("name", "RGB");
        }

        chroma.appendChild(colorSpaceType);

        // TODO: Depending on visual class OR the presence of color mop!?
        switch (header.visualClass) {
            case X11.VISUAL_CLASS_STATIC_COLOR:
            case X11.VISUAL_CLASS_PSEUDO_COLOR:
                IIOMetadataNode palette = new IIOMetadataNode("Palette");
                chroma.appendChild(palette);

                for (int i = 0; i < header.colorMap.getMapSize(); i++) {
                    IIOMetadataNode paletteEntry = new IIOMetadataNode("PaletteEntry");
                    paletteEntry.setAttribute("index", Integer.toString(i));

                    paletteEntry.setAttribute("red", Integer.toString(header.colorMap.getRed(i)));
                    paletteEntry.setAttribute("green", Integer.toString(header.colorMap.getGreen(i)));
                    paletteEntry.setAttribute("blue", Integer.toString(header.colorMap.getBlue(i)));

                    palette.appendChild(paletteEntry);
                }
                break;

            default:
                // No palette
        }


        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        planarConfiguration.setAttribute("value", "PixelInterleaved");
        node.appendChild(planarConfiguration);

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        node.appendChild(sampleFormat);

        switch (header.visualClass) {
            case X11.VISUAL_CLASS_STATIC_COLOR:
            case X11.VISUAL_CLASS_PSEUDO_COLOR:
                sampleFormat.setAttribute("value", "Index");
                break;
            default:
                sampleFormat.setAttribute("value", "UnsignedIntegral");
                break;
        }

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        node.appendChild(bitsPerSample);

        int numComponents = header.numComponents();
        bitsPerSample.setAttribute("value", createListValue(numComponents, Integer.toString(header.bitsPerPixel / numComponents)));

        // SampleMSB
        if (header.bitsPerRGB < 8 && header.bitFillOrder == ByteOrder.LITTLE_ENDIAN) {
            IIOMetadataNode sampleMSB = new IIOMetadataNode("SampleMSB");
            node.appendChild(sampleMSB);
            sampleMSB.setAttribute("value", createListValue(header.numComponents(), "0"));
        }

        return node;
    }


    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");

        // The only format we support is the X11 format, and it's version is 7.
        IIOMetadataNode formatVersion = new IIOMetadataNode("FormatVersion");
        document.appendChild(formatVersion);
        formatVersion.setAttribute("value", "7");

        return document;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        IIOMetadataNode text = new IIOMetadataNode("Text");

        if (header.windowName != null) {
            IIOMetadataNode node = new IIOMetadataNode("TextEntry");
            text.appendChild(node);
            node.setAttribute("keyword", "DocumentName"); // For TIFF interop. :-)
            node.setAttribute("value", header.windowName);
        }

        return text.hasChildNodes() ? text : null;
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
}
