package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class UniformAreaOfInterest extends DefaultAreaOfInterest {
    
    public UniformAreaOfInterest(int pOriginalWidth, int pOriginalHeight) {
        super(pOriginalWidth, pOriginalHeight);
    }

    protected Dimension getCrop(final Rectangle pCrop) {
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

}
