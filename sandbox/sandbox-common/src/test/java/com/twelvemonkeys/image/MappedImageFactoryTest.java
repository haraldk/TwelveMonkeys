package com.twelvemonkeys.image;

import org.junit.Test;

import java.awt.image.BufferedImage;

import static com.twelvemonkeys.image.MappedImageFactory.createCompatibleMappedImage;
import static com.twelvemonkeys.image.MappedImageFactory.getCompatibleBufferedImageType;
import static org.junit.Assert.assertEquals;

public class MappedImageFactoryTest {

    @Test
    public void testGetCompatibleBufferedImageTypeFromBufferedImage() throws Exception {
        for (int type = BufferedImage.TYPE_INT_RGB; type <= BufferedImage.TYPE_BYTE_INDEXED; type++) { // 1 - 13
            assertEquals(type, getCompatibleBufferedImageType(new BufferedImage(1, 1, type)));
        }
    }

    @Test
    public void testGetCompatibleBufferedImageType() throws Exception {
        for (int type = BufferedImage.TYPE_INT_RGB; type <= BufferedImage.TYPE_BYTE_INDEXED; type++) { // 1 - 13
            assertEquals(type, getCompatibleBufferedImageType(createCompatibleMappedImage(1, 1, type)));
        }
    }
}