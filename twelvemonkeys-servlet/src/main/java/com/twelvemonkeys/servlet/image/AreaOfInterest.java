package com.twelvemonkeys.servlet.image;

import java.awt.*;

/**
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <a href="mailto:erlend@hamnaberg.net">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class AreaOfInterest {
    protected final int mOriginalWidth;
    protected final int mOriginalHeight;
    protected final boolean mPercent;
    protected final boolean pUniform;

    public AreaOfInterest(int pOriginalWidth, int pOriginalHeight, boolean pPercent, boolean pUniform) {
        this.mOriginalWidth = pOriginalWidth;
        this.mOriginalHeight = pOriginalHeight;
        this.mPercent = pPercent;
        this.pUniform = pUniform;
    }

    public Rectangle getAOI(final int pX, final int pY, final int pWidth, final int pHeight) {
        return getAOI(new Rectangle(pX, pY, pWidth, pHeight));
    }

    public Rectangle getAOI(final Rectangle pCrop) {
        int y = pCrop.y;
        int x = pCrop.x;

        Dimension crop;
        if (mPercent) {
            crop = getPercentCrop(pCrop);
        }
        else if (pUniform) {
            crop = getAOIUniform(pCrop);
        }
        else {
            crop = getOriginalDimension(pCrop);
        }

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

    private Dimension getAOIUniform(final Rectangle pCrop) {
        float ratio;

        if (pCrop.width >= 0 && pCrop.height >= 0) {
            // Compute both ratios
            ratio = (float) pCrop.width / (float) pCrop.height;
            float originalRatio = (float) mOriginalWidth / (float) mOriginalHeight;
            if (ratio > originalRatio) {
                pCrop.width = mOriginalWidth;
                pCrop.height = Math.round((float) mOriginalWidth / ratio);
            }
            else {
                pCrop.height = mOriginalHeight;
                pCrop.width = Math.round((float) mOriginalHeight * ratio);
            }
        }
        else if (pCrop.width >= 0) {
            // Find ratio from pWidth
            ratio = (float) pCrop.width / (float) mOriginalWidth;
            pCrop.height = Math.round((float) mOriginalHeight * ratio);
        }
        else if (pCrop.height >= 0) {
            // Find ratio from pHeight
            ratio = (float) pCrop.height / (float) mOriginalHeight;
            pCrop.width = Math.round((float) mOriginalWidth * ratio);
        }
        // Else: No crop
        return new Dimension(pCrop.width, pCrop.height);
    }

    private Dimension getPercentCrop(final Rectangle pCrop) {
        int cropWidth = pCrop.width;
        int cropHeight = pCrop.height;
        float ratio;

        if (cropWidth >= 0 && cropHeight >= 0) {
            // Non-uniform
            cropWidth = Math.round((float) mOriginalWidth * (float) pCrop.width / 100f);
            cropHeight = Math.round((float) mOriginalHeight * (float) pCrop.height / 100f);
        }
        else if (cropWidth >= 0) {
            // Find ratio from pWidth
            ratio = (float) cropWidth / 100f;
            cropWidth = Math.round((float) mOriginalWidth * ratio);
            cropHeight = Math.round((float) mOriginalHeight * ratio);

        }
        else if (cropHeight >= 0) {
            // Find ratio from pHeight
            ratio = (float) cropHeight / 100f;
            cropWidth = Math.round((float) mOriginalWidth * ratio);
            cropHeight = Math.round((float) mOriginalHeight * ratio);
        }
        // Else: No crop

        return new Dimension(cropWidth, cropHeight);
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
