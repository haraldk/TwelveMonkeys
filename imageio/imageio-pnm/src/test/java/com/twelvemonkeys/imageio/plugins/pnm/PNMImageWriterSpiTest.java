package com.twelvemonkeys.imageio.plugins.pnm;

import org.junit.Test;

import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

import static com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest.assertClassExists;
import static com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest.assertClassesExist;
import static org.junit.Assert.assertNotNull;

/**
 * PNMImageWriterSpiTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PNMImageWriterSpiTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class PNMImageWriterSpiTest {

    private final ImageWriterSpi spi = new PNMImageWriterSpi();

    @Test
    public void getPluginClassName() {
        assertClassExists(spi.getPluginClassName(), ImageWriter.class);
    }

    @Test
    public void getImageReaderSpiNames() {
        assertClassesExist(spi.getImageReaderSpiNames(), ImageReaderSpi.class);
    }

    @Test
    public void getOutputTypes() {
        assertNotNull(spi.getOutputTypes());
    }
}