package com.twelvemonkeys.servlet.image.aoi;

import com.twelvemonkeys.lang.Validate;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public class AreaOfInterestWrapper implements AreaOfInterest {
    private AreaOfInterest mDelegate;

    public AreaOfInterestWrapper(AreaOfInterest mDelegate) {
        this.mDelegate = Validate.notNull(mDelegate);
    }

    public Rectangle getAOI(Rectangle pCrop) {
        return mDelegate.getAOI(pCrop);
    }

    public Dimension getOriginalDimension() {
        return mDelegate.getOriginalDimension();
    }

    public int calculateX(Dimension pOriginalDimension, Rectangle pCrop) {
        return mDelegate.calculateX(pOriginalDimension, pCrop);
    }

    public int calculateY(Dimension pOriginalDimension, Rectangle pCrop) {
        return mDelegate.calculateY(pOriginalDimension, pCrop);
    }

    public Dimension getCrop(Dimension pOriginalDimension, Rectangle pCrop) {
        return mDelegate.getCrop(pOriginalDimension, pCrop);
    }
}
