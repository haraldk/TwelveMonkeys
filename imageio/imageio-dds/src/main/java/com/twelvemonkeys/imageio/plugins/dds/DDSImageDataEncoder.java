package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.*;

/**
 * A designated class to encode image data to binary.
 */
class DDSImageDataEncoder {
    //A cap for alpha value for BC1 where if alpha value is smaller than this, the 4x4 block will enable alpha mode.
    private static final int BC1_ALPHA_CAP = 124;
    private static final int BC4_CHANNEL_RED = 0; //default for BC4.
    private static final int BC4_CHANNEL_GREEN = 1;
    private static final int BC4_CHANNEL_ALPHA = 3; //BC3 reuse algorithm from BC4 but use alpha channelIndex for sampling.


    static void writeImageData(ImageOutputStream imageOutput, RenderedImage renderedImage, DDSEncoderType type) throws IOException {
        switch (type) {
            case BC1:
                new BlockCompressor1(false).encode(imageOutput, renderedImage);
                break;
            case BC2:
                new BlockCompressor2().encode(imageOutput, renderedImage);
                break;
            case BC3:
                new BlockCompressor3().encode(imageOutput, renderedImage);
                break;
            case BC4:
                new BlockCompressor4(BC4_CHANNEL_RED).encode(imageOutput, renderedImage);
                break;
            case BC5:
                new BlockCompressor5().encode(imageOutput, renderedImage);
                break;
            default:
                throw new IllegalArgumentException("DDS Type is not supported for encoder yet : " + type);
        }
    }

    /**
     * Handles BC1 compression.
     * <p>
     * References:
     * <p>[1] <a href="https://www.ludicon.com/castano/blog/2009/03/gpu-dxt-decompression/">GPU DXT Decompression</a>.</p>
     * <p>[2] <a href="https://sv-journal.org/2014-1/06/en/index.php">TEXTURE COMPRESSION TECHNIQUES</a>.</p>
     * <p>[3] <a href="https://mrelusive.com/publications/papers/Real-Time-Dxt-Compression.pdf">Real-Time DXT Compression by J.M.P. van Waveren</a></p>
     * </p>
     */
    private static class BlockCompressor1 extends BlockCompressorBase {
        private final boolean forceOpaque;
        //color0,1 : space 565
        //color2,3 : space 888
        private final int[] palettes;

        private BlockCompressor1(boolean forceOpaque) {
            super();
            this.forceOpaque = forceOpaque;
            palettes = new int[4];
        }

        void startEncodeBlock(ImageOutputStream imageOutput, int[] sampled) throws IOException {
            boolean alphaMode = getBlockEndpoints(sampled, palettes);
            imageOutput.writeShort((short) palettes[0]);
            imageOutput.writeShort((short) palettes[1]);
            //simulating color2,3
            calculateIntermediate(alphaMode, palettes);
            //indices encoding start.
            int indices = encodeBlockIndices(alphaMode, sampled, palettes);
            //encodeBlockIndices2(alphaMode, sampled, palettes[0], palettes[1], colors);
            imageOutput.writeInt(indices);
        }

        //all palettes now in 8:8:8 space
        int encodeBlockIndices(boolean alphaMode, int[] sampled, int[] palettes) {
            int i = 0;
            int colorPos = 0;
            int indices = 0;

            Color c0 = convertTo888(palettes[0]);
            Color c1 = convertTo888(palettes[1]);
            Color c2 = color888ToObject(palettes[2]);
            Color c3 = color888ToObject(palettes[3]);

            while (i < 64) {
                Color c = new Color(sampled[i++], sampled[i++], sampled[i++]);
                byte index;
                int a = sampled[i++];
                if (alphaMode && isAlphaBelowCap(a)) {
                    index = 0b11;
                } else {
                    double distance0 = calculateDistance(c, c0);
                    double distance1 = calculateDistance(c, c1);
                    double distance2 = calculateDistance(c, c2);
                    double distance3 = calculateDistance(c, c3);
                    index = getClosest(distance0, distance1, distance2, distance3);
                }
                indices |= (index << (colorPos * 2));
                colorPos++;
            }
            return indices;
        }

        //color space 888
        private static double calculateDistance(Color color1, Color color0) {
            float r = color0.getRed() - color1.getRed();
            float g = color0.getGreen() - color1.getGreen();
            float b = color0.getBlue() - color1.getBlue();
            return Math.sqrt(r * r + g * g + b * b);
        }

        private static byte getClosest(double d0, double d1, double d2, double d3) {
            double min = Math.min(d0, Math.min(d1, Math.min(d2, d3)));
            if (min == d0) return 0b00;
            if (min == d1) return 0b01;
            if (min == d2) return 0b10;
            return 0b11;
        }

        //this method, we work in 888 space
        @SuppressWarnings("DuplicatedCode")
        //just in case intellij warns for 'duplication'
        void calculateIntermediate(boolean alphaMode, int[] palettes) {
            Color rgb0 = convertTo888(palettes[0]);
            Color rgb1 = convertTo888(palettes[1]);
            int rgb2;
            int rgb3;
            if (alphaMode) {
                //alpha mode
                int r2 = (rgb0.getRed() + rgb1.getRed()) / 2;
                int g2 = (rgb0.getGreen() + rgb1.getGreen()) / 2;
                int b2 = (rgb0.getBlue() + rgb1.getBlue()) / 2;
                rgb2 = color888ToInt(r2, g2, b2, 0xff);
                rgb3 = 0;
            } else {
                //opaque mode
                int r2 = (2 * rgb0.getRed() + rgb1.getRed()) / 3;
                int g2 = (2 * rgb0.getGreen() + rgb1.getGreen()) / 3;
                int b2 = (2 * rgb0.getBlue() + rgb1.getBlue()) / 3;
                rgb2 = color888ToInt(r2, g2, b2, 0xff);

                int r3 = (rgb0.getRed() + 2 * rgb1.getRed()) / 3;
                int g3 = (rgb0.getGreen() + 2 * rgb1.getGreen()) / 3;
                int b3 = (rgb0.getBlue() + 2 * rgb1.getBlue()) / 3;
                rgb3 = color888ToInt(r3, g3, b3, 0xff);
            }

            palettes[2] = rgb2;
            palettes[3] = rgb3;
        }

        //this method, we work in 888 space, return color0&1 in 565 space
        boolean getBlockEndpoints(int[] sampledColors, int[] paletteBuffer) {
            if (sampledColors.length != 64)
                throw new IllegalStateException("Unintended behaviour, expecting sampled colors of block to be 64, got " + sampledColors.length);
            int minR = 0xff, minG = 0xff, minB = 0xff;
            int maxR = 0, maxG = 0, maxB = 0;
            boolean alphaMode = false;
            int i = 0;
            while (i < 64) {
                int r = sampledColors[i++];
                int g = sampledColors[i++];
                int b = sampledColors[i++];
                int a = sampledColors[i++];
                if (!forceOpaque && isAlphaBelowCap(a)) {
                    alphaMode = true;
                    continue;
                }

                minR = Math.min(minR, r);
                minG = Math.min(minG, g);
                minB = Math.min(minB, b);

                maxR = Math.max(maxR, r);
                maxG = Math.max(maxG, g);
                maxB = Math.max(maxB, b);
            }

            int color0 = convertTo565(maxR, maxG, maxB);
            int color1 = convertTo565(minR, minG, minB);
            if ((alphaMode && color0 > color1) || (!alphaMode && color0 < color1)) {
                paletteBuffer[0] = color1;
                paletteBuffer[1] = color0;
            } else {
                paletteBuffer[0] = color0;
                paletteBuffer[1] = color1;
            }

            return alphaMode;
        }


        //Reference [3] Page 7
        boolean getBlockEndpoints2(int[] sampled, int[] paletteBuffer) {
            int maxDistance = -1;
            boolean alphaMode = false;
            for (int i = 0; i < 60; i += 4) {
                for (int j = i + 4; j < 64; j += 4) {
                    if (!forceOpaque && isAlphaBelowCap(Math.min(sampled[i + 3], sampled[j + 3]))) {
                        alphaMode = true;
                        continue;
                    }
                    int distance = getColorDistance(sampled[i], sampled[i + 1], sampled[i + 2], sampled[j], sampled[j + 1], sampled[j + 2]);
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        paletteBuffer[0] = convertTo565(sampled[i], sampled[i + 1], sampled[i + 2]);
                        paletteBuffer[1] = convertTo565(sampled[j], sampled[j + 1], sampled[j + 2]);
                    }
                }
            }

            if ((alphaMode && paletteBuffer[0] > paletteBuffer[1]) || (!alphaMode && paletteBuffer[1] > paletteBuffer[0])) {
                int a = paletteBuffer[0];
                paletteBuffer[0] = paletteBuffer[1];
                paletteBuffer[1] = a;
            }
            return alphaMode;
        }

        private static int getColorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
            int r3 = r1 - r2;
            int g3 = g1 - g2;
            int b3 = b1 - b2;
            return r3 * r3 + g3 * g3 + b3 * b3;
        }

        private static int getScore(int r, int g, int b) {
            return (r + g + b) / 3;
        }

        //https://rgbcolorpicker.com/565
        private static int convertTo565(int r8, int g8, int b8) {
            int r5 = (r8 >> 3);
            int g6 = (g8 >> 2);
            int b5 = (b8 >> 3);
            return color565ToInt(r5, g6, b5);
        }

        private static Color convertTo888(int c565) {
            int r8 = BIT5[(c565 & 0xF800) >> 11];
            int g8 = BIT6[(c565 & 0x07E0) >> 5];
            int b8 = BIT5[(c565 & 0x001F)];
            return new Color(r8, g8, b8, 0xff);
        }

        private static Color color888ToObject(int c888) {
            return new Color(
                    (c888 & 0xFF0000) >> ARGB_ORDER.redShift,
                    (c888 & 0x00FF00) >> ARGB_ORDER.greenShift,
                    (c888 & 0x0000FF) >> ARGB_ORDER.blueShift,
                    (c888) >>> ARGB_ORDER.alphaShift
            );
        }
    }

    private static final class BlockCompressor2 extends BlockCompressor1 {

        private BlockCompressor2() {
            super(true);
        }

        @Override
        void startEncodeBlock(ImageOutputStream imageOutput, int[] sampled) throws IOException {
            //write 64 bit alpha first (4 bit alpha per pixel)
            long alphaData = 0;
            for (int i = 0; i < 16; i++) {
                int alpha = sampled[i * 4 + 3] >> 4;
                alphaData |= ((long) alpha) << (i * 4);
            }
            imageOutput.writeLong(alphaData);

            super.startEncodeBlock(imageOutput, sampled);
        }
    }

    private static final class BlockCompressor3 extends BlockCompressor1 {
        private final BlockCompressor4 bc4;

        private BlockCompressor3() {
            super(true);
            bc4 = new BlockCompressor4(BC4_CHANNEL_ALPHA);
        }

        @Override
        void startEncodeBlock(ImageOutputStream imageOutput, int[] sampled) throws IOException {
            bc4.startEncodeBlock(imageOutput, sampled);
            super.startEncodeBlock(imageOutput, sampled);
        }
    }

    private static final class BlockCompressor4 extends BlockCompressorBase {
        private final int channelIndex;
        private final int[] reds;

        private BlockCompressor4(int channelIndex) {
            super();
            this.channelIndex = channelIndex;
            this.reds = new int[8];
        }

        void startEncodeBlock(ImageOutputStream imageOutput, int[] samples) throws IOException {
            getColorRange(samples, reds);
            interpolate(reds);
            long data = calculateIndices(samples, reds);
            data |= (((long) reds[1] << 8) | reds[0]);
            imageOutput.writeLong(data);
        }

        // 6 bytes MSB will be for indices, the LSB is for the 2 red endpoints,
        // as we write to file in LE the bytes will be swapped back to the desired order
        private long calculateIndices(int[] samples, int[] reds) {
            long data = 0;
            for (int i = 0; i < 16; i++) {
                int index;
                if (isAlphaBelowCap(samples[i * 4 + channelIndex])) {
                    index = 0b110;
                } else {
                    int rSample = samples[i * 4 + channelIndex];
                    index = getNearest(rSample, reds);
                }
                data |= ((long) index << (16 + i * 3));
            }
            return data;
        }

        private int getNearest(int r, int[] reds) {
            int nearest = 0;
            int nearestValue = 255;
            for (int i = 0; i < 8; i++) {
                int v = Math.abs(reds[i] - r);
                if (nearestValue >= v) {
                    nearest = i;
                    nearestValue = v;
                }
            }
            return nearest;
        }

        private void interpolate(int[] reds) {
            int r0 = reds[0];
            int r1 = reds[1];
            if (r0 > r1) {
                for (int i = 1; i <= 6; i++) {
                    reds[i + 1] = ((7 - i) * r0 + i * r1) / 7;
                }
            } else {
                for (int i = 1; i <= 4; i++) {
                    reds[i + 1] = ((5 - i) * r0 + i * r1) / 5;
                }
                reds[6] = 0;
                reds[7] = 255;
            }
        }

        //r0 >  r1 : use 6 interpolated color values
        //r0 <= r1 : use 4
        private void getColorRange(int[] samples, int[] red01) {
            int r0 = 0, r1 = 255;
            boolean flag = false;
            for (int i = 0; i < 16; i++) {
                int r = samples[i * 4 + channelIndex];

                if (r == 0 || r == 255) {
                    flag = true;
                }
                r0 = Math.max(r0, r);
                r1 = Math.min(r1, r);
            }
            if (flag) {
                red01[0] = r1;
                red01[1] = r0;
            } else {
                red01[0] = r0;
                red01[1] = r1;
            }
        }
    }

    private static final class BlockCompressor5 extends BlockCompressorBase {
        private final BlockCompressor4 bc4r, bc4g;

        public BlockCompressor5() {
            bc4r = new BlockCompressor4(BC4_CHANNEL_RED);
            bc4g = new BlockCompressor4(BC4_CHANNEL_GREEN);
        }

        @Override
        void startEncodeBlock(ImageOutputStream imageOutput, int[] samples) throws IOException {
            bc4r.startEncodeBlock(imageOutput, samples);
            bc4g.startEncodeBlock(imageOutput, samples);
        }
    }

    //pack 32 bits of the colors to a single int value.
    private static int color888ToInt(int r, int g, int b, int a) {
        return (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
    }

    //pack 16 bits of the colors to a single int value.
    private static int color565ToInt(int r5, int g6, int b5) {
        return (r5 << RGB_16_ORDER.redShift) | (g6 << RGB_16_ORDER.greenShift) | (b5 << RGB_16_ORDER.blueShift);
    }

    //workaround for 24 dpi (no alpha) -> 32dpi (with alpha default to 0xff)
    //as this mess the color0 & color1 up spectacularly bc alpha is not present in 24dpi
    private static void adjustSampledBands(Raster raster, int[] samples) {
        if (raster.getNumBands() == 4) return;
        for (int i = 15; i >= 0; i--) {
            int r24Index = i * 3;
            int r32Index = i * 4;
            samples[r32Index + 3] = 0xFF;
            samples[r32Index + 2] = samples[r24Index + 2];  //b24 -> b32
            samples[r32Index + 1] = samples[r24Index + 1];  //g24 -> g32
            samples[r32Index] = samples[r24Index];      //r24 -> r32
        }
    }

    private static abstract class BlockCompressorBase {
        final int[] samples;

        BlockCompressorBase() {
            this.samples = new int[64];
        }

        void encode(ImageOutputStream imageOutput, RenderedImage image) throws IOException {
            int blocksXCount = (image.getWidth() + 3) / 4;
            int blocksYCount = (image.getHeight() + 3) / 4;
            Raster raster = image.getData();
            for (int blockY = 0; blockY < blocksYCount; blockY++) {
                for (int blockX = 0; blockX < blocksXCount; blockX++) {
                    raster.getPixels(blockX * 4, blockY * 4, 4, 4, samples);
                    adjustSampledBands(raster, samples);
                    startEncodeBlock(imageOutput, samples);
                }
            }
        }

        boolean isAlphaBelowCap(int alpha) {
            return alpha < BC1_ALPHA_CAP;
        }

        abstract void startEncodeBlock(ImageOutputStream imageOutput, int[] samples) throws IOException;
    }

    private static final class Color16 {
        int r;
        int g;
        int b;

        Color16(int red, int green, int blue) {
            this.r = red;
            this.g = green;
            this.b = blue;
            if ((r + g + b) < 0 || (r + g + b) > 31 + 63 + 31)
                throw new IllegalArgumentException("Invalid 5:6:5 color : " + red + ", " + green + ", " + blue);
        }

        Color16(int c565) {
            this(
                    (c565) >> RGB_16_ORDER.redShift,
                    (c565 & 0b111111_00000) >> RGB_16_ORDER.greenShift,
                    (c565 & 0b11111) >> RGB_16_ORDER.blueShift
            );
        }
    }
}