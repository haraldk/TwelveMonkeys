package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

final class PNMHeaderParser extends HeaderParser {
    private final short fileType;
    private final TupleType tupleType;

    public PNMHeaderParser(final ImageInputStream input, final short type) {
        super(input);
        this.fileType = type;
        this.tupleType = asTupleType(type);
    }

    static TupleType asTupleType(int fileType) {
        switch (fileType) {
            case PNM.PBM:
            case PNM.PBM_PLAIN:
                return TupleType.BLACKANDWHITE_WHITE_IS_ZERO;
            case PNM.PGM:
            case PNM.PGM_PLAIN:
                return TupleType.GRAYSCALE;
            case PNM.PPM:
            case PNM.PPM_PLAIN:
                return TupleType.RGB;
            default:
                throw new AssertionError("Illegal PNM type :" + fileType);
        }
    }

    @Override public PNMHeader parse() throws IOException {
        int width = 0;
        int height = 0;
        int maxSample = tupleType == TupleType.BLACKANDWHITE_WHITE_IS_ZERO ? 1 : 0; // PBM has no maxSample line

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
                        maxSample = Integer.parseInt(token);
                    } else {
                        throw new IIOException("Unknown PNM token: " + token);
                    }
                }
            }
        }

        return new PNMHeader(fileType, tupleType, width, height, tupleType.getSamplesPerPixel(), maxSample, comments);
    }
}
