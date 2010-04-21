package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class DefaultAreaOfInterest implements AreaOfInterest {
    private final int mOriginalWidth;
    private final int mOriginalHeight;

    public DefaultAreaOfInterest(int pOriginalWidth, int pOriginalHeight) {
        this.mOriginalWidth = pOriginalWidth;
        this.mOriginalHeight = pOriginalHeight;
    }

    public DefaultAreaOfInterest(Dimension pOriginalDimension) {
        this(pOriginalDimension.width, pOriginalDimension.height);
    }

    Rectangle getAOI(final int pX, final int pY, final int pWidth, final int pHeight) {
        return getAOI(new Rectangle(pX, pY, pWidth, pHeight));
    }

    public Rectangle getAOI(final Rectangle pCrop) {
        int y = pCrop.y;
        int x = pCrop.x;
        Dimension dimension = getOriginalDimension();

        Dimension crop = getCrop(dimension, pCrop);

       // Center
        if (y < 0) {
            y = calculateY(dimension, new Rectangle(x, y, crop.width, crop.height));
        }

        if (x < 0) {
            x = calculateX(dimension, new Rectangle(x, y, crop.width, crop.height));
        }
        return new Rectangle(x, y, crop.width, crop.height);
    }

    public Dimension getOriginalDimension() {
        return new Dimension(mOriginalWidth, mOriginalHeight);
    }

    public int calculateX(Dimension pOriginalDimension, Rectangle pCrop) {
        return (pOriginalDimension.width - pCrop.width) / 2;
    }

    public int calculateY(Dimension pOriginalDimension, Rectangle pCrop) {
        return (pOriginalDimension.height - pCrop.height) / 2;
    }

    public Dimension getCrop(Dimension pOriginalDimension, final Rectangle pCrop) {
        int mOriginalWidth1 = pOriginalDimension.width;
        int mOriginalHeight1 = pOriginalDimension.height;
        int x = pCrop.x;
        int y = pCrop.y;
        int cropWidth = pCrop.width;
        int cropHeight = pCrop.height;

        if (cropWidth < 0 || (x < 0 && cropWidth > mOriginalWidth1)
                || (x >= 0 && (x + cropWidth) > mOriginalWidth1)) {
            cropWidth = (x >= 0 ? mOriginalWidth1 - x : mOriginalWidth1);
        }
        if (cropHeight < 0 || (y < 0 && cropHeight > mOriginalHeight1)
                || (y >= 0 && (y + cropHeight) > mOriginalHeight1)) {
            cropHeight = (y >= 0 ? mOriginalHeight1 - y : mOriginalHeight1);
        }
        return new Dimension(cropWidth, cropHeight);
    }
}
