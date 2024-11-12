package com.twelvemonkeys.imageio.plugins.hdr.tonemap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GammaToneMapperTest {
    private final GammaToneMapper mapper = new GammaToneMapper();

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
        assertArrayEquals(new float[]{0.5f}, rgb, 0);
    }

    @Test
    public void testMap16() {
        float[] rgb = {15.999999f};
        mapper.map(rgb);
        assertArrayEquals(new float[]{1}, rgb, 0);
    }

    @Test
    public void testMapMax() {
        float[] rgb = {Float.MAX_VALUE};
        mapper.map(rgb);
        assertArrayEquals(new float[]{1}, rgb, 0);
    }

}