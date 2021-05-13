package com.twelvemonkeys.imageio.plugins.hdr.tonemap;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class NullToneMapperTest {
    private final NullToneMapper mapper = new NullToneMapper();

    @Test
    public void testMap0() {
        float[] rgb = {0};
        mapper.map(rgb);
        assertArrayEquals(new float[]{0}, rgb, 0);
    }

    @Test
    public void testMap1() {
        float[] rgb = {1};
        mapper.map(rgb);
        assertArrayEquals(new float[]{1}, rgb, 0);
    }

    @Test
    public void testMapMax() {
        float[] rgb = {Float.MAX_VALUE};
        mapper.map(rgb);
        assertArrayEquals(new float[]{Float.MAX_VALUE}, rgb, 0);
    }

}