package com.twelvemonkeys.imageio.plugins.pnm;

import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.NodeList;

abstract class HeaderWriter {
    protected static final Charset UTF8 = Charset.forName("UTF8");
    protected final ImageOutputStream imageOutput;

    protected HeaderWriter(final ImageOutputStream imageOutput) {
        this.imageOutput = imageOutput;
    }

    public static void write(final IIOImage image, final ImageWriterSpi provider, final ImageOutputStream imageOutput) throws IOException {
        // TODO: This is somewhat sketchy...
        if (provider.getFormatNames()[0].equals("pam")) {
            new PAMHeaderWriter(imageOutput).writeHeader(image, provider);
        }
        else if (provider.getFormatNames()[0].equals("pnm")) {
            new PNMHeaderWriter(imageOutput).writeHeader(image, provider);
        }
        else {
            throw new AssertionError("Unsupported provider: " + provider);
        }
    }

    public abstract void writeHeader(IIOImage image, final ImageWriterSpi provider) throws IOException;

    protected final int getWidth(final IIOImage image) {
        return image.hasRaster() ? image.getRaster().getWidth() : image.getRenderedImage().getWidth();
    }

    protected final int getHeight(final IIOImage image) {
        return image.hasRaster() ? image.getRaster().getHeight() : image.getRenderedImage().getHeight();
    }

    protected final int getNumBands(final IIOImage image) {
        return image.hasRaster() ? image.getRaster().getNumBands() : image.getRenderedImage().getSampleModel().getNumBands();
    }

    protected int getMaxVal(final IIOImage image) {
        int transferType = getTransferType(image);

        if (transferType == DataBuffer.TYPE_BYTE) {
            return PNM.MAX_VAL_8BIT;
        }
        else if (transferType == DataBuffer.TYPE_USHORT) {
            return PNM.MAX_VAL_16BIT;
        }
//        else if (transferType == DataBuffer.TYPE_INT) {
        // TODO: Support TYPE_INT through conversion, if number of channels is 3 or 4 (TYPE_INT_RGB, TYPE_INT_ARGB)
//        }
        else {
            throw new IllegalArgumentException("Unsupported data type: " + transferType);
        }
    }

    protected final int getTransferType(final IIOImage image) {
        return  image.hasRaster() ? image.getRaster().getTransferType() : image.getRenderedImage().getSampleModel().getTransferType();
    }

    protected final void writeComments(final IIOMetadata metadata, final ImageWriterSpi provider) throws IOException {
        // TODO: Only write creator if not already present
        imageOutput.write(String.format("# CREATOR: %s %s\n", provider.getVendorName(), provider.getDescription(Locale.getDefault())).getBytes(UTF8));

        // Comments from metadata
        if (metadata != null && metadata.isStandardMetadataFormatSupported()) {
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            NodeList textEntries = root.getElementsByTagName("TextEntry");

            for (int i = 0; i < textEntries.getLength(); i++) {
                // TODO: Write on the format "# KEYWORD: value" (if keyword != comment)?
                IIOMetadataNode textEntry = (IIOMetadataNode) textEntries.item(i);
                imageOutput.write(String.format("# %s", textEntry.getAttribute("value")).getBytes(UTF8));
            }
        }
    }

}
