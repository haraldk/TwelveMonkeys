package com.twelvemonkeys.imageio.plugins.hdr;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * HDRHeader.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDRHeader.java,v 1.0 27/07/15 harald.kuhr Exp$
 */
final class HDRHeader {
    private static final String KEY_FORMAT = "FORMAT=";
    private static final String KEY_PRIMARIES = "PRIMARIES=";
    private static final String KEY_EXPOSURE = "EXPOSURE=";
    private static final String KEY_GAMMA = "GAMMA=";
    private static final String KEY_SOFTWARE = "SOFTWARE=";

    private int width;
    private int height;

    private String software;

    public static HDRHeader read(final ImageInputStream stream) throws IOException {
        HDRHeader header = new HDRHeader();

        while (true) {
            String line = stream.readLine().trim();

            if (line.isEmpty()) {
                // This is the last line before the dimensions
                break;
            }

            if (line.startsWith("#?")) {
                // Program specifier, don't need that...
            }
            else if (line.startsWith("#")) {
                // Comment (ignore)
            }
            else if (line.startsWith(KEY_FORMAT)) {
                String format = line.substring(KEY_FORMAT.length()).trim();

                if (!format.equals("32-bit_rle_rgbe")) {
                    throw new IIOException("Unsupported format \"" + format + "\"(expected \"32-bit_rle_rgbe\")");
                }
                // TODO: Support the 32-bit_rle_xyze format
            }
            else if (line.startsWith(KEY_PRIMARIES)) {
                // TODO: We are going to need these values...
                // Should contain 8 (RGB + white point) coordinates
            }
            else if (line.startsWith(KEY_EXPOSURE)) {
                // TODO: We are going to need these values...
            }
            else if (line.startsWith(KEY_GAMMA)) {
                // TODO: We are going to need these values...
            }
            else if (line.startsWith(KEY_SOFTWARE)) {
                header.software = line.substring(KEY_SOFTWARE.length()).trim();
            }
            else {
                // ...ignore
            }
        }

        // TODO: Proper parsing of width/height and orientation!
        String dimensionsLine = stream.readLine().trim();
        String[] dims = dimensionsLine.split("\\s");

        if (dims[0].equals("-Y") && dims[2].equals("+X")) {
            header.height = Integer.parseInt(dims[1]);
            header.width = Integer.parseInt(dims[3]);

            return header;
        }
        else {
            throw new IIOException("Unsupported RGBE orientation (expected \"-Y ... +X ...\")");
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getSoftware() {
        return software;
    }
}
