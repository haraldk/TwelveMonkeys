package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.lang.Validate;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;

/**
 * LumaToGray.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LumaToGray.java,v 1.0 10/04/2021 haraldk Exp$
 */
final class LuminanceToGray implements RasterOp {

    @Override
    public WritableRaster filter(final Raster src, WritableRaster dest) {
        Validate.notNull(src, "src may not be null");
        Validate.isTrue(src != dest, "src and dest raster may not be same");
        Validate.isTrue(src.getNumDataElements() >= 3, src.getNumDataElements(), "Luma raster must have at least 3 data elements: %s");

        if (dest == null) {
            dest = createCompatibleDestRaster(src);
        }

        // If src and dest have alpha component, keep it, otherwise extract luma only
        int[] bandList = src.getNumBands() > 3 && dest.getNumBands() > 1 ? new int[] {0, 3} : new int[] {0};
        dest.setRect(0, 0, src.createChild(0, 0, src.getWidth(), src.getHeight(), 0, 0, bandList));

        return dest;
    }

    @Override
    public Rectangle2D getBounds2D(final Raster src) {
        return src.getBounds();
    }

    @Override
    public WritableRaster createCompatibleDestRaster(final Raster src) {
        WritableRaster raster = src.createCompatibleWritableRaster();
        return raster.createWritableChild(0, 0, src.getWidth(), src.getHeight(), 0, 0, new int[] {0});
    }

    @Override
    public Point2D getPoint2D(final Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Double(srcPt.getX(), srcPt.getY());
        }
        else {
            dstPt.setLocation(srcPt);
        }

        return dstPt;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}
