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
    JPEGTileDecoder(final IIOReadWarningListener warningListener, final int compression, final byte[] jpegTables, final int numTiles, final ImageReadParam originalParam, final Predicate<ImageReader> needsConversion, final RasterConverter converter) throws IOException {
        super(warningListener, "JPEG", originalParam, needsConversion, converter);

        if (jpegTables != null) {
            // This initializes the tables and other internal settings for the reader,
            // and is actually a feature of JPEG, see "abbreviated streams":
            // http://docs.oracle.com/javase/6/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html#abbrev
            delegate.setInput(new ByteArrayImageInputStream(jpegTables));
            delegate.getStreamMetadata();
        }
        else if (numTiles > 1) {
            // TODO: This is not really a problem as long as we read ALL tiles, but we can't have random access in this case...
            if (compression == TIFFExtension.COMPRESSION_JPEG) {
                warningListener.warningOccurred(delegate, "Missing JPEGTables for tiled/striped TIFF with compression: 7 (JPEG)");
            }
            // ...and the JPEG reader might choke on missing tables...
        }
    }
}
