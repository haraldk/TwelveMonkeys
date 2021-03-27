package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.AbstractMetadata;

import javax.imageio.metadata.IIOMetadataNode;

/**
 * PICTMetadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PICTMetadata.java,v 1.0 23/03/2021 haraldk Exp$
 */
public class PICTMetadata extends AbstractMetadata {

    private final int version;
    private final double screenImageXRatio;
    private final double screenImageYRatio;

    PICTMetadata(final int version, final double screenImageXRatio, final double screenImageYRatio) {
        this.version = version;
        this.screenImageXRatio = screenImageXRatio;
        this.screenImageYRatio = screenImageYRatio;
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        chroma.appendChild(csType);
        csType.setAttribute("name", "RGB");

        // NOTE: Channels in chroma node reflects channels in color model (see data node, for channels in data)
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        chroma.appendChild(numChannels);
        numChannels.setAttribute("value", "3");

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        chroma.appendChild(blackIsZero);
        blackIsZero.setAttribute("value", "TRUE");

        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        if (screenImageXRatio > 0.0d && screenImageYRatio > 0.0d) {
            IIOMetadataNode node = new IIOMetadataNode("Dimension");
            double ratio = screenImageXRatio / screenImageYRatio;
            IIOMetadataNode subNode = new IIOMetadataNode("PixelAspectRatio");
            subNode.setAttribute("value", "" + ratio);
            node.appendChild(subNode);

            return node;
        }
        return null;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data = new IIOMetadataNode("Data");

        // As this is a vector-ish format, with possibly multiple regions of pixel data, this makes no sense... :-P
        // This is, however, consistent with the getRawImageTyp/getImageTypes

        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        planarConfiguration.setAttribute("value", "PixelInterleaved");
        data.appendChild(planarConfiguration);

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        sampleFormat.setAttribute("value", "UnsignedIntegral");
        data.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        bitsPerSample.setAttribute("value", "32");
        data.appendChild(bitsPerSample);

        return data;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");

        IIOMetadataNode formatVersion = new IIOMetadataNode("FormatVersion");
        document.appendChild(formatVersion);
        formatVersion.setAttribute("value", Integer.toString(version));

        return document;
    }
}
