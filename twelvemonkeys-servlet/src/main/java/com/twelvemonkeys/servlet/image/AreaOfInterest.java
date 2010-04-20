package com.twelvemonkeys.servlet.image;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
class AreaOfInterest {
    private final int mOriginalWidth;
    private final int mOriginalHeight;
    private final boolean mPercent;
    private final boolean pUniform;

    AreaOfInterest(int pOriginalWidth, int pOriginalHeight, boolean pPercent, boolean pUniform) {
        this.mOriginalWidth = pOriginalWidth;
        this.mOriginalHeight = pOriginalHeight;
        this.mPercent = pPercent;
        this.pUniform = pUniform;
    }

    Rectangle getAOI(int pX, int pY, int pWidth, int pHeight) {
        // Algoritm:
        // Try to get x and y (default 0,0).
        // Try to get width and height (default width-x, height-y)
        //
        // If percent, get ratio
        //
        // If uniform
        //

        float ratio;

        if (mPercent) {
            if (pWidth >= 0 && pHeight >= 0) {
                // Non-uniform
                pWidth = Math.round((float) mOriginalWidth * (float) pWidth / 100f);
                pHeight = Math.round((float) mOriginalHeight * (float) pHeight / 100f);
            }
            else if (pWidth >= 0) {
                // Find ratio from pWidth
                ratio = (float) pWidth / 100f;
                pWidth = Math.round((float) mOriginalWidth * ratio);
                pHeight = Math.round((float) mOriginalHeight * ratio);

            }
            else if (pHeight >= 0) {
                // Find ratio from pHeight
                ratio = (float) pHeight / 100f;
                pWidth = Math.round((float) mOriginalWidth * ratio);
                pHeight = Math.round((float) mOriginalHeight * ratio);
            }
            // Else: No crop
        }
        else {
            // Uniform
            if (pUniform) {
                if (pWidth >= 0 && pHeight >= 0) {
                    // Compute both ratios
                    ratio = (float) pWidth / (float) pHeight;
                    float originalRatio = (float) mOriginalWidth / (float) mOriginalHeight;
                    if (ratio > originalRatio) {
                        pWidth = mOriginalWidth;
                        pHeight = Math.round((float) mOriginalWidth / ratio);
                    }
                    else {
                        pHeight = mOriginalHeight;
                        pWidth = Math.round((float) mOriginalHeight * ratio);
                    }
                }
                else if (pWidth >= 0) {
                    // Find ratio from pWidth
                    ratio = (float) pWidth / (float) mOriginalWidth;
                    pHeight = Math.round((float) mOriginalHeight * ratio);
                }
                else if (pHeight >= 0) {
                    // Find ratio from pHeight
                    ratio = (float) pHeight / (float) mOriginalHeight;
                    pWidth = Math.round((float) mOriginalWidth * ratio);
                }
                // Else: No crop
            }
        }

        // Not specified, or outside bounds: Use original dimensions
        if (pWidth < 0 || (pX < 0 && pWidth > mOriginalWidth)
                || (pX >= 0 && (pX + pWidth) > mOriginalWidth)) {
            pWidth = (pX >= 0 ? mOriginalWidth - pX : mOriginalWidth);
        }
        if (pHeight < 0 || (pY < 0 && pHeight > mOriginalHeight)
                || (pY >= 0 && (pY + pHeight) > mOriginalHeight)) {
            pHeight = (pY >= 0 ? mOriginalHeight - pY : mOriginalHeight);
        }
        if (Boolean.getBoolean("rule-of-thirds")) {
            pY = calculateRuleOfThirds(pY, pWidth, pHeight);
        }
       // Center
        if (pY < 0) {
            pY = (mOriginalHeight - pHeight) / 2;
        }
 
        if (pX < 0) {
            pX = (mOriginalWidth - pWidth) / 2;
        }

//        System.out.println("x: " + pX + " y: " + pY
//                           + " w: " + pWidth + " h " + pHeight);

        return new Rectangle(pX, pY, pWidth, pHeight);
    }

    private int calculateRuleOfThirds(final int pY, final int pWidth, final int pHeight) {
        int y = pY;
        if (y < 0) {
            float origRatio = (float) mOriginalWidth / (float) mOriginalHeight;
            float cropRatio = (float) pWidth / (float) pHeight;
            if (cropRatio > origRatio && origRatio < 1) {
                y = (int) ((mOriginalHeight * 0.33f) - (pHeight / 2));
                if (y < 0) {
                    y = 0;
                }
            }
        }
        return y;
    }
}
