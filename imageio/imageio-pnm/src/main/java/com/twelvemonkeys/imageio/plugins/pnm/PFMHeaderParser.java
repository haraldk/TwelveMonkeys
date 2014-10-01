package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

final class PFMHeaderParser extends HeaderParser {
    private final short fileType;
    private final TupleType tupleType;

    public PFMHeaderParser(final ImageInputStream input, final short type) {
        super(input);
        this.fileType = type;
        this.tupleType = asTupleType(type);
    }

    static TupleType asTupleType(int fileType) {
        switch (fileType) {
            case PNM.PFM_GRAY:
                return TupleType.GRAYSCALE;
            case PNM.PFM_RGB:
                return TupleType.RGB;
            default:
                throw new AssertionError("Illegal PNM type :" + fileType);
        }
    }

    // http://netpbm.sourceforge.net/doc/pfm.html
    // http://www.pauldebevec.com/Research/HDR/PFM/ (note that this is just one of *several* *incompatible* specs)
    // The text header of a .pfm file takes the following form:
    // [type]
    // [xres] [yres]
    // [scale/byte_order] where positive means big-endian, negative means little-endian, maxVal is abs(scale)
    // Samples are 1 or 3 samples/pixels, interleaved, IEEE 32 bit floating point values
    @Override public PNMHeader parse() throws IOException {
        int width = 0;
        int height = 0;
        float maxSample = tupleType == TupleType.BLACKANDWHITE_WHITE_IS_ZERO ? 1 : 0; // PBM has no maxSample line

        List<String> comments = new ArrayList<String>();

        while (width == 0 || height == 0 || maxSample == 0) {
            String line = input.readLine();

            if (line == null) {
                throw new IIOException("Unexpeced end of stream");
            }

            int commentStart = line.indexOf('#');
            if (commentStart >= 0) {
                String comment = line.substring(commentStart + 1).trim();
                if (!comment.isEmpty()) {
                    comments.add(comment);
                }

                line = line.substring(0, commentStart);
            }

            line = line.trim();

            if (!line.isEmpty()) {
                // We have tokens...
                String[] tokens = line.split("\\s");
                for (String token : tokens) {
                    if (width == 0) {
                        width = Integer.parseInt(token);
                    } else if (height == 0) {
                        height = Integer.parseInt(token);
                    } else if (maxSample == 0) {
                        maxSample = Float.parseFloat(token);
                    } else {
                        throw new IIOException("Unknown PNM token: " + token);
                    }
                }
            }
        }

        return new PNMHeader(fileType, tupleType, width, height, tupleType.getSamplesPerPixel(), byteOrder(maxSample), comments);
    }

    private ByteOrder byteOrder(final float maxSample) {
        return maxSample > 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
    }
}
