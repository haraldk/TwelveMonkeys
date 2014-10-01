package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

final class PAMHeaderWriter extends HeaderWriter {
    public PAMHeaderWriter(final ImageOutputStream imageOutput) {
        super(imageOutput);
    }

    @Override public void writeHeader(final IIOImage image, final ImageWriterSpi provider) throws IOException {
        // Write PAM magic
        imageOutput.writeShort(PNM.PAM);
        imageOutput.write('\n');
        // Comments
        writeComments(image.getMetadata(), provider);
        // Write width/height and number of channels
        imageOutput.write(String.format("WIDTH %s\nHEIGHT %s\n", getWidth(image), getHeight(image)).getBytes(UTF8));
        imageOutput.write(String.format("DEPTH %s\n", getNumBands(image)).getBytes(UTF8));

        // TODO: maxSample (8 or16 bit)
        imageOutput.write(String.format("MAXVAL %s\n", getMaxVal(image)).getBytes(UTF8));

        // TODO: Determine tuple type based on input color model and image data
        TupleType tupleType = getNumBands(image) > 3 ? TupleType.RGB_ALPHA : TupleType.RGB;
        imageOutput.write(String.format("TUPLTYPE %s\nENDHDR\n", tupleType).getBytes(UTF8));
    }
}
