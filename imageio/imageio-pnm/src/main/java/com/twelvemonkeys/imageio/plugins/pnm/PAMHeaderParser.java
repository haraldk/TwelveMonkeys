package com.twelvemonkeys.imageio.plugins.pnm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

final class PAMHeaderParser extends HeaderParser {

    static final String ENDHDR = "ENDHDR";
    static final String WIDTH = "WIDTH";
    static final String HEIGHT = "HEIGHT";
    static final String MAXVAL = "MAXVAL";
    static final String DEPTH = "DEPTH";
    static final String TUPLTYPE = "TUPLTYPE";

    public PAMHeaderParser(final ImageInputStream input) {
        super(input);
    }

    @Override public PNMHeader parse() throws IOException {
        /* Note: Comments are allowed
        P7
        WIDTH 227
        HEIGHT 149
        DEPTH 3
        MAXVAL 255
        TUPLTYPE RGB
        ENDHDR
        */

        int width = -1;
        int height = -1;
        int depth = -1;
        int maxVal = -1;
        TupleType tupleType = null;
        List<String> comments = new ArrayList<String>();

        String line;
        while ((line = input.readLine()) != null && !line.startsWith(ENDHDR)) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith(WIDTH)) {
                width = Integer.parseInt(line.substring(WIDTH.length() + 1));
            }
            else if (line.startsWith(HEIGHT)) {
                height = Integer.parseInt(line.substring(HEIGHT.length() + 1));
            }
            else if (line.startsWith(DEPTH)) {
                depth = Integer.parseInt(line.substring(DEPTH.length() + 1));
            }
            else if (line.startsWith(MAXVAL)) {
                maxVal = Integer.parseInt(line.substring(MAXVAL.length() + 1));
            }
            else if (line.startsWith(TUPLTYPE)) {
                tupleType = TupleType.valueOf(line.substring(TUPLTYPE.length() + 1));
            }
            else if (line.startsWith("#")) {
                comments.add(line.substring(1).trim());
            }
            else {
                throw new IIOException("Unknown PAM header token: '" + line + "'");
            }
        }

        if (tupleType == null) {
            // TODO: Assume a type, based on depth + maxVal, or at least, allow reading as raster
        }

        return new PNMHeader(PNM.PAM, tupleType, width, height, depth, maxVal, comments);
    }
}
