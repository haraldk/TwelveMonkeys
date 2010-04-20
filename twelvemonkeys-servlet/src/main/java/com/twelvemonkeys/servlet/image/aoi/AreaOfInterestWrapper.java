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

    public Rectangle getAOI(int pX, int pY, int pWidth, int pHeight) {
        return mDelegate.getAOI(pX, pY, pWidth, pHeight);
    }

    public Rectangle getAOI(Rectangle pCrop) {
        return mDelegate.getAOI(pCrop);
    }
}
