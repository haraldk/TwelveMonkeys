package com.twelvemonkeys.imageio.plugins.tiff;

import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;

/**
 * TileDecoder.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TileDecoder.java,v 1.0 09/11/2023 haraldk Exp$
 */
abstract class TileDecoder implements AutoCloseable {

    protected final IIOReadWarningListener warningListener;

    public TileDecoder(IIOReadWarningListener warningListener) {
        this.warningListener = warningListener;
    }

    abstract void decodeTile(ImageInputStream input, Rectangle sourceRegion, Point destinationOffset, BufferedImage destination) throws IOException;

    @Override
    public abstract void close();

    interface RasterConverter {
        void convert(Raster raster) throws IOException;
    }
}
