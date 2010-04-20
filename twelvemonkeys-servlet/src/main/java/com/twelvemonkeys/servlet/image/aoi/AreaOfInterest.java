package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public interface AreaOfInterest {
    Rectangle getAOI(int pX, int pY, int pWidth, int pHeight);

    Rectangle getAOI(Rectangle pCrop);
}
