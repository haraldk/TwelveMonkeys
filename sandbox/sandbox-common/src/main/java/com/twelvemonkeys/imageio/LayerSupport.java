package com.twelvemonkeys.imageio;

import javax.imageio.ImageReadParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * LayerSupport
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: LayerSupport.java,v 1.0 Oct 29, 2009 12:46:00 AM haraldk Exp$
 */
public interface LayerSupport {
    // TODO: ..or maybe we should just allow a parameter to the PSDImageReadParam to specify layer?
    //       - then, how do we get the number of layers, dimension and offset?

    // boolean readerSupportsLayers(), always true if the interface is implemented
    
    int getNumLayers(int pImageIndex) throws IOException;

    boolean hasLayers(int pImageIndex) throws IOException;

    BufferedImage readLayer(int pImageIndex, int pLayerIndex, ImageReadParam pParam) throws IOException;

    int getLayerWidth(int pImageIndex, int pLayerIndex) throws IOException;

    int getLayerHeight(int pImageIndex, int pLayerIndex) throws IOException;

    // ?
    Point getLayerOffset(int pImageIndex, int pLayerIndex) throws IOException;

    // TODO: Blend modes.. Layer meta data?
}
