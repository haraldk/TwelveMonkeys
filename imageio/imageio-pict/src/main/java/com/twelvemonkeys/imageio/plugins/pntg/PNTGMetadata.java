package com.twelvemonkeys.imageio.plugins.pntg;

import com.twelvemonkeys.imageio.AbstractMetadata;

import javax.imageio.metadata.IIOMetadataNode;

/**
 * PNTGMetadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PNTGMetadata.java,v 1.0 23/03/2021 haraldk Exp$
 */
public class PNTGMetadata extends AbstractMetadata {
    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

        IIOMetadataNode csType = new IIOMetadataNode("ColorSpaceType");
        chroma.appendChild(csType);
        csType.setAttribute("name", "GRAY");

        // NOTE: Channels in chroma node reflects channels in color model (see data node, for channels in data)
        IIOMetadataNode numChannels = new IIOMetadataNode("NumChannels");
        chroma.appendChild(numChannels);
        numChannels.setAttribute("value", "1");

        IIOMetadataNode blackIsZero = new IIOMetadataNode("BlackIsZero");
        chroma.appendChild(blackIsZero);
        blackIsZero.setAttribute("value", "FALSE");

        return chroma;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compressionNode = new IIOMetadataNode("Compression");

        IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
        compressionTypeName.setAttribute("value", "PackBits"); // RLE?
        compressionNode.appendChild(compressionTypeName);
        compressionNode.appendChild(new IIOMetadataNode("Lossless"));
        // "value" defaults to TRUE

        return compressionNode;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode data = new IIOMetadataNode("Data");

        // PlanarConfiguration
        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
        planarConfiguration.setAttribute("value", "PixelInterleaved");
        data.appendChild(planarConfiguration);

        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
        sampleFormat.setAttribute("value", "UnsignedIntegral");
        data.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        bitsPerSample.setAttribute("value", "1");
        data.appendChild(bitsPerSample);

        return data;
    }

    @Override
    protected IIOMetadataNode getStandardDocumentNode() {
        IIOMetadataNode document = new IIOMetadataNode("Document");

        IIOMetadataNode formatVersion = new IIOMetadataNode("FormatVersion");
        document.appendChild(formatVersion);
        formatVersion.setAttribute("value",  "1.0");

        // TODO: We could get the file creation time from MacBinary header here...

        return document;
    }

    @Override
    protected IIOMetadataNode getStandardTextNode() {
        // TODO: We could get the file name from MacBinary header here...
        return super.getStandardTextNode();
    }
}
