package com.twelvemonkeys.imageio.plugins.jpeg;

import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * JPEGImage10MetadataCleanerTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: JPEGImage10MetadataCleanerTest.java,v 1.0 08/08/16 harald.kuhr Exp$
 */
public class JPEGImage10MetadataCleanerTest {

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    protected static final JPEGImageReaderSpi SPI = new JPEGImageReaderSpi(lookupDelegateProvider());

    protected static ImageReaderSpi lookupDelegateProvider() {
        return JPEGImageReaderSpi.lookupDelegateProvider(IIORegistry.getDefaultInstance());
    }

    // Unit/regression test for #276
    @Test
    public void cleanMetadataMoreThan4DHTSegments() throws Exception {
        List<String> testData = Arrays.asList("/jpeg/5dhtsegments.jpg", "/jpeg/6dhtsegments.jpg");

        for (String data : testData) {
            try (ImageInputStream origInput = ImageIO.createImageInputStream(getClass().getResource(data));
                 ImageInputStream input = ImageIO.createImageInputStream(getClass().getResource(data))) {

                ImageReader origReader = SPI.delegateProvider.createReaderInstance();
                origReader.setInput(origInput);

                ImageReader reader = SPI.createReaderInstance();
                reader.setInput(input);

                IIOMetadata original = origReader.getImageMetadata(0);
                IIOMetadataNode origTree = (IIOMetadataNode) original.getAsTree(JPEGImage10MetadataCleaner.JAVAX_IMAGEIO_JPEG_IMAGE_1_0);

                JPEGImage10MetadataCleaner cleaner = new JPEGImage10MetadataCleaner((JPEGImageReader) reader);
                IIOMetadata cleaned = cleaner.cleanMetadata(origReader.getImageMetadata(0));

                IIOMetadataNode cleanTree = (IIOMetadataNode) cleaned.getAsTree(JPEGImage10MetadataCleaner.JAVAX_IMAGEIO_JPEG_IMAGE_1_0);

                NodeList origDHT = origTree.getElementsByTagName("dht");
                assertEquals(1, origDHT.getLength());

                NodeList cleanDHT = cleanTree.getElementsByTagName("dht");
                assertEquals(2, cleanDHT.getLength());

                NodeList cleanDHTable = cleanTree.getElementsByTagName("dhtable");
                NodeList origDHTable = origTree.getElementsByTagName("dhtable");

                assertEquals(origDHTable.getLength(), cleanDHTable.getLength());

                // Note: This also tests that the order of the htables are the same,
                // but that will only hold if they are already sorted by class.
                // Luckily, they are in these cases...
                for (int i = 0; i < origDHTable.getLength(); i++) {
                    Element cleanDHTableElem = (Element) cleanDHTable.item(i);
                    Element origDHTableElem = (Element) origDHTable.item(i);

                    assertNotNull(cleanDHTableElem);

                    assertNotNull(cleanDHTableElem.getAttribute("class"));
                    assertEquals(origDHTableElem.getAttribute("class"), cleanDHTableElem.getAttribute("class"));

                    assertNotNull(cleanDHTableElem.getAttribute("htableId"));
                    assertEquals(origDHTableElem.getAttribute("htableId"), cleanDHTableElem.getAttribute("htableId"));
                }

                reader.dispose();
                origReader.dispose();
            }
        }
    }
}