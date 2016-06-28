package com.twelvemonkeys.imageio.plugins.pnm;

import org.junit.Test;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

import static com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest.assertClassExists;
import static com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfoTest.assertClassesExist;
import static org.junit.Assert.assertNotNull;

/**
 * PNMImageReaderSpiTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: PNMImageReaderSpiTest.java,v 1.0 02/06/16 harald.kuhr Exp$
 */
public class PNMImageReaderSpiTest {

    private final ImageReaderSpi spi = new PNMImageReaderSpi();

    @Test
    public void getPluginClassName() {
        assertClassExists(spi.getPluginClassName(), ImageReader.class);
    }

    @Test
    public void getImageWriterSpiNames() {
        assertClassesExist(spi.getImageWriterSpiNames(), ImageWriterSpi.class);
    }

    @Test
    public void getInputTypes() {
        assertNotNull(spi.getInputTypes());
    }
}