package com.twelvemonkeys.imageio.plugins.tiff;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * JPEGTileDecoder.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGTileDecoder.java,v 1.0 09/11/2023 haraldk Exp$
 */
final class JPEGTileDecoder extends DelegateTileDecoder {
    JPEGTileDecoder(final IIOReadWarningListener warningListener, final byte[] jpegTables, final int numTiles, final ImageReadParam originalParam, final Predicate<ImageReader> needsConversion, final RasterConverter converter) throws IOException {
        super(warningListener, "JPEG", originalParam, needsConversion, converter);

        if (jpegTables != null) {
            // Whatever values I pass the reader as the read param, it never gets the same quality as if
            // I just invoke jpegReader.getStreamMetadata(), so we'll do that...
            delegate.setInput(new ByteArrayImageInputStream(jpegTables));

            // This initializes the tables and other internal settings for the reader,
            // and is actually a feature of JPEG, see abbreviated streams:
            // http://docs.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html#abbrev
            delegate.getStreamMetadata();
        }
        else if (numTiles > 1) {
            warningListener.warningOccurred(delegate, "Missing JPEGTables for tiled/striped TIFF with compression: 7 (JPEG)");
            // ...and the JPEG reader might choke on missing tables...
        }
    }
}
