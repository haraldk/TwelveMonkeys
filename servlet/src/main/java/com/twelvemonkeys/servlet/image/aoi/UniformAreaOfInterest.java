package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class UniformAreaOfInterest extends DefaultAreaOfInterest {

    public UniformAreaOfInterest(Dimension pOriginalDimension) {
        super(pOriginalDimension);
    }

    public UniformAreaOfInterest(int pOriginalWidth, int pOriginalHeight) {
        super(pOriginalWidth, pOriginalHeight);
    }

    public Dimension getCrop(Dimension pOriginalDimension, final Rectangle pCrop) {
        float ratio;
        if (pCrop.width >= 0 && pCrop.height >= 0) {
            // Compute both ratios
            ratio = (float) pCrop.width / (float) pCrop.height;
            float originalRatio = (float) pOriginalDimension.width / (float) pOriginalDimension.height;
            if (ratio > originalRatio) {
                pCrop.width = pOriginalDimension.width;
                pCrop.height = Math.round((float) pOriginalDimension.width / ratio);
            }
            else {
                pCrop.height = pOriginalDimension.height;
                pCrop.width = Math.round((float) pOriginalDimension.height * ratio);
            }
        }
        else if (pCrop.width >= 0) {
            // Find ratio from pWidth
            ratio = (float) pCrop.width / (float) pOriginalDimension.width;
            pCrop.height = Math.round((float) pOriginalDimension.height * ratio);
        }
        else if (pCrop.height >= 0) {
            // Find ratio from pHeight
            ratio = (float) pCrop.height / (float) pOriginalDimension.height;
            pCrop.width = Math.round((float) pOriginalDimension.width * ratio);
        }
        // Else: No crop
        return new Dimension(pCrop.width, pCrop.height);
    }
}
