package com.twelvemonkeys.imageio.plugins.pict;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * TestPICTEagerDetect
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TestPICTEagerDetect.java,v 1.0 Aug 6, 2009 2:59:41 PM haraldk Exp$
 */
public class TestPICTEagerDetect {
    public static void main(final String[] pArgs) throws IOException {
        PICTImageReaderSpi provider = new PICTImageReaderSpi();

        if (pArgs.length == 0) {
            System.exit(1);
        }
        for (String arg : pArgs) {
            boolean canDecode = provider.canDecodeInput(ImageIO.createImageInputStream(new File(arg)));
            System.err.printf("canDecode %s: %s%n", arg, canDecode);
        }
    }
}
