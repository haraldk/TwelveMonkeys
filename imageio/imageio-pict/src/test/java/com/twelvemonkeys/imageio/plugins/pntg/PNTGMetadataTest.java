package com.twelvemonkeys.imageio.plugins.pntg;

import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import org.junit.jupiter.api.Test;

import java.awt.image.*;

/**
 * PNTGMetadataTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PNTGMetadataTest.java,v 1.0 23/03/2021 haraldk Exp$
 */
public class PNTGMetadataTest {
    @Test
    public void testCreate() {
        new PNTGMetadata(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY));
    }
}