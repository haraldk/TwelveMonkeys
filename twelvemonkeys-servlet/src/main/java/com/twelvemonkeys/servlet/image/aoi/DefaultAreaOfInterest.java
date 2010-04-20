package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class DefaultAreaOfInterest implements AreaOfInterest {
    protected final int mOriginalWidth;
    protected final int mOriginalHeight;

    public DefaultAreaOfInterest(int pOriginalWidth, int pOriginalHeight) {
        this.mOriginalWidth = pOriginalWidth;
        this.mOriginalHeight = pOriginalHeight;
    }

    public Rectangle getAOI(final int pX, final int pY, final int pWidth, final int pHeight) {
        return getAOI(new Rectangle(pX, pY, pWidth, pHeight));
    }

    public Rectangle getAOI(final Rectangle pCrop) {
        int y = pCrop.y;
        int x = pCrop.x;

        Dimension crop = getCrop(pCrop);        

       // Center
        if (y < 0) {
            y = calculateY(crop.height);
        }

        if (x < 0) {
            x = calculateX(crop.width);
        }
        return new Rectangle(x, y, crop.width, crop.height);
    }

    protected int calculateX(int pWidth) {
        return (mOriginalWidth - pWidth) / 2;
    }


    protected int calculateY(int pHeight) {
        return (mOriginalHeight - pHeight) / 2;
    }

    private int calculateRuleOfThirds(final int pY, final int pCropWidth, final int pCropHeight) {
        int y = pY;
        if (y < 0) {
            float origRatio = (float) mOriginalWidth / (float) mOriginalHeight;
            float cropRatio = (float) pCropWidth / (float) pCropHeight;
            if (cropRatio > origRatio && origRatio < 1) {
                y = (int) ((mOriginalHeight * 0.33f) - (pCropHeight / 2));
                if (y < 0) {
                    y = 0;
                }
            }
        }
        return y;
    }

    protected Dimension getCrop(final Rectangle pCrop) {
        return getOriginalDimension(pCrop);
    }

    private Dimension getOriginalDimension(Rectangle pCrop) {
        int x = pCrop.x;
        int y = pCrop.y;
        int cropWidth = pCrop.width;
        int cropHeight = pCrop.height;

        if (cropWidth < 0 || (x < 0 && cropWidth > mOriginalWidth)
                || (x >= 0 && (x + cropWidth) > mOriginalWidth)) {
            cropWidth = (x >= 0 ? mOriginalWidth - x : mOriginalWidth);
        }
        if (cropHeight < 0 || (y < 0 && cropHeight > mOriginalHeight)
                || (y >= 0 && (y + cropHeight) > mOriginalHeight)) {
            cropHeight = (y >= 0 ? mOriginalHeight - y : mOriginalHeight);
        }
        return new Dimension(cropWidth, cropHeight);
    }
}
