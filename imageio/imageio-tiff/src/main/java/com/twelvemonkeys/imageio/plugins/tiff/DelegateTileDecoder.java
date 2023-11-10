package com.twelvemonkeys.imageio.plugins.tiff;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * DelegateTileDecoder.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DelegateTileDecoder.java,v 1.0 09/11/2023 haraldk Exp$
 */
class DelegateTileDecoder extends TileDecoder {

    protected final ImageReader delegate;
    protected final ImageReadParam param;

    // TODO: Naming... Is this only due to color space conversion? Is it because we need to read raster?
    private final Predicate<ImageReader> needsConversion;
    private final RasterConverter converter;
    private Boolean readRasterAndConvert;

    DelegateTileDecoder(final IIOReadWarningListener warningListener, final String format, final ImageReadParam originalParam) throws IOException {
        this(warningListener, createDelegate(format), originalParam, imageReader -> false, null);
    }

    DelegateTileDecoder(final IIOReadWarningListener warningListener, final String format, final ImageReadParam originalParam, final Predicate<ImageReader> needsConversion, final RasterConverter converter) throws IOException {
        this(warningListener, createDelegate(format), originalParam, needsConversion, converter);
    }

    private DelegateTileDecoder(final IIOReadWarningListener warningListener, final ImageReader delegate, final ImageReadParam originalParam, final Predicate<ImageReader> needsConversion, final RasterConverter converter) {
        super(warningListener);

        this.delegate = notNull(delegate, "delegate");
        delegate.addIIOReadWarningListener(warningListener);

        param = delegate.getDefaultReadParam();
        param.setSourceSubsampling(originalParam.getSourceXSubsampling(), originalParam.getSourceYSubsampling(), 0, 0);

        this.needsConversion = needsConversion;
        this.converter = converter;
    }

    private static ImageReader createDelegate(String format) throws IOException {
        // We'll just use the default (first) reader
        // If it's the TwelveMonkeys one, we will be able to read JPEG Lossless etc.
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
        if (!readers.hasNext()) {
            throw new IIOException("Could not instantiate " + format + "ImageReader");
        }

        return readers.next();
    }

    @Override
    void decodeTile(final ImageInputStream input, final Rectangle sourceRegion, final Point destinationOffset, final BufferedImage destination) throws IOException {
        delegate.setInput(input);
        param.setSourceRegion(sourceRegion);

        if (readRasterAndConvert == null) {
            // All tiles in an image will use the same format, test once and cache result
            readRasterAndConvert = needsConversion.test(delegate);
        }

        if (!readRasterAndConvert) {
            // No conversion needed
            param.setDestinationOffset(destinationOffset);
            param.setDestination(destination);
            delegate.read(0, param);
        }
        else {
            // Otherwise, it's likely CMYK or some other interpretation we don't need to convert.
            // We'll have to use readAsRaster and later apply color space conversion ourselves
            Raster raster = delegate.readRaster(0, param);
            converter.convert(raster);

            destination.getRaster().setDataElements(destinationOffset.x, destinationOffset.y, raster);
        }
    }

    @Override
    public void close() {
        delegate.dispose();
    }
}
