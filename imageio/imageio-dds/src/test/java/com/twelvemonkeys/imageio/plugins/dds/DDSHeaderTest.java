package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.jupiter.api.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DDSHeaderTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: DDSHeaderTest.java,v 1.0 06/09/2026 haraldk Exp$
 */
class DDSHeaderTest {
    @Test
    void testLegalMipMapCount() throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(getClass().getResourceAsStream("/dds/dds_X8B8G8R8_mipmap.dds"))) {
            input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            int magic = input.readInt();

            assertEquals(DDS.MAGIC, magic, String.format("Not a DDS file. Expected DDS magic 0x%8x', read 0x%8x", DDS.MAGIC, magic));

            DDSHeader header = DDSHeader.read(input);
            assertEquals(9, header.getMipMapCount());
        }
    }

    @Test
    void testInvalidMipMapCount() throws IOException {
        try (ImageInputStream input = new ByteArrayImageInputStream(Base64.getDecoder().decode("RERTIHwAAAAHEAA2ODk3NDY3MjE5NjMwMjQ5MDA3AAIAAAACAAA="))) {
            input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            int magic = input.readInt();

            assertEquals(DDS.MAGIC, magic, String.format("Not a DDS file. Expected DDS magic 0x%8x', read 0x%8x", DDS.MAGIC, magic));

            assertThrows(IIOException.class,  () -> DDSHeader.read(input));
        }
    }
}
