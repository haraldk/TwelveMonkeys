package com.twelvemonkeys.servlet.image.aoi;

import java.awt.*;

/**
 * @author <a href="mailto:erlend@escenic.com">Erlend Hamnaberg</a>
 * @version $Revision: $
 */
public interface AreaOfInterest {
    
    Rectangle getAOI(Rectangle pCrop);

    Dimension getOriginalDimension();

    int calculateX(Dimension pOriginalDimension, Rectangle pCrop);

    int calculateY(Dimension pOriginalDimension, Rectangle pCrop);

    Dimension getCrop(Dimension pOriginalDimension, Rectangle pCrop);
}
