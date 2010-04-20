package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class PercentAreaOfInterest extends DefaultAreaOfInterest {

    public PercentAreaOfInterest(int pOriginalWidth, int pOriginalHeight) {
        super(pOriginalWidth, pOriginalHeight);
    }

    protected Dimension getCrop(final Rectangle pCrop) {
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

}
