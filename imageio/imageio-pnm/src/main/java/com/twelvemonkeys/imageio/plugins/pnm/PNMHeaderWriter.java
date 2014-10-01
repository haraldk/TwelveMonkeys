package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

final class PNMHeaderWriter extends HeaderWriter {
    public PNMHeaderWriter(final ImageOutputStream imageOutput) {
        super(imageOutput);
    }

    @Override public void writeHeader(final IIOImage image, final ImageWriterSpi provider) throws IOException {
        // Write P4/P5/P6 magic (Support only RAW formats for now; if we are to support PLAIN formats, pass parameter)
        // TODO: Determine PBM, PBM or PPM based on input color model and image data?
        short type = PNM.PPM;
        imageOutput.writeShort(type);
        imageOutput.write('\n');

        // Comments
        writeComments(image.getMetadata(), provider);

        // Dimensions (width/height)
        imageOutput.write(String.format("%s %s\n", getWidth(image), getHeight(image)).getBytes(HeaderWriter.UTF8));

        // MaxSample
        if (type != PNM.PBM) {
            imageOutput.write(String.format("%s\n", getMaxVal(image)).getBytes(HeaderWriter.UTF8));
        }
    }
}
