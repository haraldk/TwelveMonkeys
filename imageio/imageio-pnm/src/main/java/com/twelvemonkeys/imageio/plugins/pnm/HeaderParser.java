package com.twelvemonkeys.imageio.plugins.pnm;

import static com.twelvemonkeys.lang.Validate.notNull;

import java.io.IOException;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

abstract class HeaderParser {
    protected final ImageInputStream input;

    protected HeaderParser(final ImageInputStream input) {
        this.input = notNull(input);
    }

    public abstract PNMHeader parse() throws IOException;

    public static PNMHeader parse(ImageInputStream input) throws IOException {
        short type = input.readShort();

        return createParser(input, type).parse();
    }

    private static HeaderParser createParser(final ImageInputStream input, final short type) throws IOException {
        switch (type) {
            case PNM.PBM_PLAIN:
            case PNM.PBM:
            case PNM.PGM_PLAIN:
            case PNM.PGM:
            case PNM.PPM_PLAIN:
            case PNM.PPM:
                return new PNMHeaderParser(input, type);
            case PNM.PAM:
                return new PAMHeaderParser(input);
            case PNM.PFM_GRAY:
            case PNM.PFM_RGB:
                return new PFMHeaderParser(input, type);
            default:
                throw new IIOException("Unexpected type for PBM, PGM or PPM format: " + type);
        }
    }
}
