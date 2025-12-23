package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

import static com.twelvemonkeys.imageio.plugins.dds.DDSReader.*;
import static java.lang.Math.abs;

/**
 * A designated class to encode image data to binary.
 */
class DDSImageDataEncoder {
    //A cap for alpha value for BC1 where if alpha value is smaller than this, the 4x4 block will enable alpha mode.
    private static final int ALPHA_CAP = 128;
    private static final int C565_5_MASK = 0xF8;
    private static final int C565_6_MASK = 0xFC;

    static void writeImageData(ImageOutputStream imageOutput, RenderedImage renderedImage, DDSEncoderType type) throws IOException {
        switch (type) {
            case BC1:
                new BlockCompressor1().encode(imageOutput, renderedImage);
                break;
            case BC2:
                new BlockCompressor2().encode(imageOutput, renderedImage);
                break;
            case BC3:
                new BlockCompressor3().encode(imageOutput, renderedImage);
                break;
            case BC4_SNORM:
            case BC4_UNORM:
                BlockCompressor4.encode(imageOutput, renderedImage, type);
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
    private static class BlockCompressor1 {
        void encode(ImageOutputStream imageOutput, RenderedImage image) throws IOException {
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            int blocksXCount = (imageWidth + 3) / 4;
            int blocksYCount = (imageHeight + 3) / 4;
            Raster raster = image.getData();
            //r-g-b-a = 4 uint8 each pixel
            int[] sampled = new int[64];
            //color0,1 : space 565
            //color2,3 : space 888
            int[] palettes = new int[4]; //starting a 4x4 block
            for (int blockY = 0; blockY < blocksYCount; blockY++) {
                for (int blockX = 0; blockX < blocksXCount; blockX++) {
                    raster.getPixels(blockX * 4, blockY * 4, 4, 4, sampled);
                    startEncodeBlock(imageOutput, sampled, palettes);
                }
            }
        }

        void startEncodeBlock(ImageOutputStream imageOutput, int[] sampled, int[] palettes) throws IOException {
            boolean alphaMode = getBlockEndpoints2(sampled, palettes, false);

            imageOutput.writeShort((short) palettes[0]);
            imageOutput.writeShort((short) palettes[1]);
            calculateIntermediate(alphaMode, palettes);
            //indices encoding start.
            int indices = encodeBlockIndices(alphaMode, sampled, palettes, false);
            //encodeBlockIndices2(alphaMode, sampled, palettes[0], palettes[1], colors);
            imageOutput.writeInt(indices);
        }

        //Reference [3] Page 10-12
        private static int encodeBlockIndices2(boolean alphaMode, int[] sampled, int color0, int color1, int[][] colors) {
            Color16 c0 = new Color16(color0);
            Color16 c1 = new Color16(color1);
            int indices = 0;
            colors[0][0] = (c1.r & C565_5_MASK) | (c1.r >> 5);
            colors[0][1] = (c1.g & C565_6_MASK) | (c1.g >> 6);
            colors[0][2] = (c1.b & C565_5_MASK) | (c1.b >> 5);

            colors[1][0] = (c0.r & C565_5_MASK) | (c0.r >> 5);
            colors[1][1] = (c0.g & C565_6_MASK) | (c0.g >> 6);
            colors[1][2] = (c0.b & C565_5_MASK) | (c0.b >> 5);

            if (alphaMode) {
                colors[2][0] = (colors[0][0] + colors[1][0]) / 2;
                colors[2][1] = (colors[0][1] + colors[1][1]) / 2;
                colors[2][2] = (colors[0][2] + colors[1][2]) / 2;

                colors[3][0] = 0;
                colors[3][1] = 0;
                colors[3][2] = 0;
            } else {
                colors[2][0] = (2 * colors[0][0] + colors[1][0]) / 3;
                colors[2][1] = (2 * colors[0][1] + colors[1][1]) / 3;
                colors[2][2] = (2 * colors[0][2] + colors[1][2]) / 3;

                colors[3][0] = (colors[0][0] + 2 * colors[1][0]) / 3;
                colors[3][1] = (colors[0][1] + 2 * colors[1][1]) / 3;
                colors[3][2] = (colors[0][2] + 2 * colors[1][2]) / 3;
            }

            for (int i = 15; i >= 0; i--) {
                int r = sampled[i * 4];
                int g = sampled[i * 4 + 1];
                int b = sampled[i * 4 + 2];

                if (alphaMode && isAlphaBelowCap(sampled[i * 4 + 3])) {
                    indices |= (0b11 << (i * 2));
                } else {
                    int d0 = abs(colors[0][0] - r) + abs(colors[0][1] - g) + abs(colors[0][2] - b);
                    int d1 = abs(colors[1][0] - r) + abs(colors[1][1] - g) + abs(colors[1][2] - b);
                    int d2 = abs(colors[2][0] - r) + abs(colors[2][1] - g) + abs(colors[2][2] - b);
                    int d3 = abs(colors[3][0] - r) + abs(colors[3][1] - g) + abs(colors[3][2] - b);
                    int b0 = d0 > d3 ? 1 : 0;
                    int b1 = d1 > d2 ? 1 : 0;
                    int b2 = d0 > d2 ? 1 : 0;
                    int b3 = d1 > d3 ? 1 : 0;
                    int b4 = d2 > d3 ? 1 : 0;
                    int x0 = b1 & b2;
                    int x1 = b0 & b3;
                    int x2 = b0 & b4;
                    indices |= (x2 | ((x0 | x1) << 1)) << (i << 1);

                }
            }
            return indices;
        }


        //all palettes now in 8:8:8 space
        int encodeBlockIndices(boolean alphaMode, int[] sampled, int[] palettes, boolean forceOpaque) {
            int i = 0;
            int colorPos = 0;
            int indices = 0;

            Color c0 = convertTo888(palettes[0]);
            Color c1 = convertTo888(palettes[1]);
            Color c2 = color888ToObject(palettes[2]);
            Color c3 = color888ToObject(palettes[3]);

            while (i < 64) {
                Color c = new Color(sampled[i++], sampled[i++], sampled[i++]);
                int a = sampled[i++];
                byte index;
                if (alphaMode && isAlphaBelowCap(a)) {
                    index = 0b11;
                } else {
                    double distance0 = calculateDistance(c, c0);
                    double distance1 = calculateDistance(c, c1);
                    double distance2 = calculateDistance(c, c2);
                    if (forceOpaque || palettes[0] > palettes[1]) {
                        double distance3 = calculateDistance(c, c3);
                        index = getClosest(distance0, distance1, distance2, distance3);
                    } else index = getClosest(distance0, distance1, distance2, Long.MAX_VALUE);
                }
                indices |= (index << (colorPos * 2));
                colorPos++;
            }
            return indices;
        }


        //color space 888
        private static double calculateDistance(Color colorB, Color colorA) {
            float r = colorB.getRed() - colorA.getRed();
            float g = colorB.getGreen() - colorA.getGreen();
            float b = colorB.getBlue() - colorA.getBlue();
            return r * r + g * g + b * b;
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
            if (alphaMode && palettes[0] <= palettes[1]) {
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

        //this method, we work in 565 space
        private static boolean getBlockEndpoints(int[] sampledColors, int[] paletteBuffer) {
            if (sampledColors.length != 64)
                throw new IllegalStateException("Unintended behaviour, expecting sampled colors of block to be 64, got " + sampledColors.length);
            int minR = 0xff, minG = 0xff, minB = 0xff;
            int maxR = 0, maxG = 0, maxB = 0;
            boolean hasAlpha = false; //decides whether this block is in Transparent mode, otherwise Opaque mode
            int i = 0;
            while (i < 64) {
                int r = sampledColors[i++];
                int g = sampledColors[i++];
                int b = sampledColors[i++];

                if (isAlphaBelowCap(sampledColors[i++])) {
                    hasAlpha = true;
                    continue;
                }

                minR = Math.min(minR, r);
                minG = Math.min(minG, g);
                minB = Math.min(minB, b);

                maxR = Math.max(maxR, r);
                maxG = Math.max(maxG, g);
                maxB = Math.max(maxB, b);
            }

            int color1 = convertTo565(minR, minG, minB);
            int color0 = convertTo565(maxR, maxG, maxB);
            if ((hasAlpha && color0 > color1) || (!hasAlpha && color0 < color1)) {
                int a = color0;
                color0 = color1;
                color1 = a;
            }

            paletteBuffer[0] = color0;
            paletteBuffer[1] = color1;
            return hasAlpha;
        }


        //Reference [3] Page 7
        boolean getBlockEndpoints2(int[] sampled, int[] paletteBuffer, boolean forceOpaque) {
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

            if ((alphaMode && paletteBuffer[0] > paletteBuffer[1]) || (!alphaMode && !forceOpaque && paletteBuffer[1] > paletteBuffer[0])) {
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

        @Override
        void startEncodeBlock(ImageOutputStream imageOutput, int[] sampled, int[] palettes) throws IOException {
            //write 64 bit alpha first (4 bit alpha per pixel)
            long alphaData = 0;
            for (int i = 0; i < 16; i++) {
                int alpha = sampled[i * 4 + 3] >> 4;
                alphaData |= ((long) alpha) << (i * 4);
            }
            imageOutput.writeLong(alphaData);

            super.startEncodeBlock(imageOutput, sampled, palettes);
        }

        @Override
        int encodeBlockIndices(boolean alphaMode, int[] sampled, int[] palettes, boolean forceOpaque) {
            return super.encodeBlockIndices(alphaMode, sampled, palettes, true);
        }

        @Override
        void calculateIntermediate(boolean alphaMode, int[] palettes) {
            super.calculateIntermediate(false, palettes);
        }

        @Override
        boolean getBlockEndpoints2(int[] sampled, int[] paletteBuffer, boolean forceOpaque) {
            super.getBlockEndpoints2(sampled, paletteBuffer, true);
            return false;
        }
    }

    private static final class BlockCompressor3 extends BlockCompressor1 {
        private final int[] alphas = new int[8];

        @Override
        void startEncodeBlock(ImageOutputStream imageOutput, int[] sampled, int[] palettes) throws IOException {
            getAlphaEndpoints(sampled);
            interpolateEndpoints();
            long alphaData = encodeAlphaIndices(sampled);
            alphaData |= alphas[0] & 0xFF;
            alphaData |= (long) (alphas[1] & 0xFF) << 8;
            imageOutput.writeLong(alphaData);
            super.startEncodeBlock(imageOutput, sampled, palettes);
        }

        private long encodeAlphaIndices(int[] sampled) {
            long alphaData = 0;
            for (int i = 0; i < 16; i++) {
                int a = sampled[i * 4 + 3];
                int index = getNearest(a);
                alphaData |= (long) (index & 0b111) << (16 + i * 3);
            }
            return alphaData;
        }

        private int getNearest(int a) {
            if (alphas[0] <= alphas[1]) {
                if (a == 0xff) return 0b111;
                if (a == 0x00) return 0b110;
            }

            int nearestIndex = 0;
            int nearestValue = Integer.MAX_VALUE;
            for (int i = 0; i < 8; i++) {
                int value = Math.abs(a - alphas[i]);
                if (value < nearestValue) {
                    nearestValue = value;
                    nearestIndex = i;
                }
            }
            return nearestIndex;
        }

        private void interpolateEndpoints() {
            if (alphas[0] > alphas[1]) {
                for (int i = 1; i < 6; i++) {
                    alphas[i] = ((i * alphas[1]) + ((7 - i) * alphas[0])) / 7;
                }
            } else {
                for (int i = 1; i < 4; i++) {
                    alphas[i] = ((i * alphas[1]) + ((5 - i) * alphas[0])) / 5;
                }
                alphas[6] = 0;
                alphas[7] = 0xFF;
            }
        }

        private void getAlphaEndpoints(int[] sampled) {
            int alphaMin = 0xFF, alphaMax = 0;
            for (int i = 0; i < 16; i++) {
                int a = sampled[i * 4 + 3];
                alphaMin = Math.min(alphaMin, a);
                alphaMax = Math.max(alphaMax, a);
            }
            if (alphaMin == 0xFF || alphaMax == 0) {
                alphas[0] = alphaMin;
                alphas[1] = alphaMax;
            } else {
                alphas[0] = alphaMax;
                alphas[1] = alphaMin;
            }
        }

        @Override
        int encodeBlockIndices(boolean alphaMode, int[] sampled, int[] palettes, boolean forceOpaque) {
            return super.encodeBlockIndices(alphaMode, sampled, palettes, true);
        }

        @Override
        void calculateIntermediate(boolean alphaMode, int[] palettes) {
            super.calculateIntermediate(false, palettes);
        }

        @Override
        boolean getBlockEndpoints2(int[] sampled, int[] paletteBuffer, boolean forceOpaque) {
            super.getBlockEndpoints2(sampled, paletteBuffer, true);
            return false;
        }
    }

    private static final class BlockCompressor4 {
        private static void encode(ImageOutputStream imageOutput, RenderedImage image, DDSEncoderType type) throws IOException {
            int blocksXCount = (image.getWidth() + 3) / 4;
            int blocksYCount = (image.getHeight() + 3) / 4;
            Raster raster = image.getData();

            int[] samples = new int[64];
            float[] reds = new float[8];
            for (int blockY = 0; blockY < blocksYCount; blockY++) {
                for (int blockX = 0; blockX < blocksXCount; blockX++) {
                    raster.getPixels(blockX * 4, blockY * 4, 4, 4, samples);
                    getColorRange(samples, reds, type);
                    interpolate(reds, type);
                    long data = calculateIndices(samples, reds, type);
                    data |= composeRange(reds[0], reds[1], type);
                    imageOutput.writeLong(data);
                }
            }
        }

        private static int composeRange(float red0, float red1, DDSEncoderType type) {
            int r0, r1;
            if (type == DDSEncoderType.BC4_SNORM) {
                r0 = ((int) (red0 * 127f)) & 0xff;
                r1 = ((int) (red1 * 127f)) & 0xff;
            } else {
                r0 = (int) (red0 * 255f);
                r1 = (int) (red1 * 255f);
            }
            return ((r1 << 8) | r0) & 0xffff;
        }

        // 6 bytes MSB will be for indices, the LSB is for the 2 red endpoints,
        // as we write to file in LE the bytes will be swapped back to the desired order
        private static long calculateIndices(int[] samples, float[] reds, DDSEncoderType type) {
            long data = 0;
            for (int i = 0; i < 16; i++) {
                byte index;
                if (reds[0] <= reds[1] && isAlphaBelowCap(samples[i * 4 + 3])) {
                    index = 0b110;
                } else {
                    int rSample = (byte) (samples[i * 4]);
                    float r = type == DDSEncoderType.BC4_UNORM ? rSample / 255f : ((byte) rSample / 127f);
                    index = getNearest(r, reds, type);
                }
                data |= ((long) index << (16 + i * 3));
            }
            return data;
        }

        private static byte getNearest(float r, float[] reds, DDSEncoderType type) {
            int nearest = 0;
            float nearestValue = type == DDSEncoderType.BC4_SNORM ? -1f : 0f;
            for (int i = 0; i < 8; i++) {
                if (nearestValue > Math.abs(r - reds[i])) {
                    nearest = i;
                }
            }
            return (byte) nearest;
        }

        private static void interpolate(float[] reds, DDSEncoderType type) {
            float r0 = reds[0];
            float r1 = reds[1];
            if (r0 > r1) {
                for (int i = 1; i <= 6; i++) {
                    reds[i + 1] = ((7 - i) * r0 + i * r1) / 7f;
                }
            } else {
                for (int i = 1; i <= 4; i++) {
                    reds[i + 1] = ((5 - i) * r0 + i * r1) / 5f;
                }
                reds[6] = type == DDSEncoderType.BC4_SNORM ? -1f : 0f;
                reds[7] = 1f;
            }
        }


        //r0 >  r1 : use 6 interpolated color values
        //r0 <= r1 : use 4
        private static void getColorRange(int[] samples, float[] red01, DDSEncoderType type) {
            int i = 0;
            boolean flag = true; //use 6 interpolated color values, otherwise 4
            int r0 = 0, r1 = 255;
            while (i < 64) {
                int r = samples[i += 3];
                int a = samples[i++];

                if (isAlphaBelowCap(a)) {
                    flag = false;
                    continue;
                }
                r0 = Math.max(r0, r);
                r1 = Math.min(r1, r);
            }
            writeRangeData(red01, flag, r1, r0, type);
        }

        private static void writeRangeData(float[] red01, boolean flag, int r1, int r0, DDSEncoderType type) {
            if (type == DDSEncoderType.BC4_SNORM) {
                byte red0 = (byte) r0;
                byte red1 = (byte) r1;
                if ((flag && red1 > red0) || (!flag && red0 > red1)) {
                    red01[0] = red1 / 127f;
                    red01[1] = red0 / 127f;
                } else {
                    red01[0] = red0 / 127f;
                    red01[1] = red1 / 127f;
                }
            } else {
                if ((flag && r1 > r0) || (!flag && r0 > r1)) {
                    red01[0] = r1 / 255f;
                    red01[1] = r0 / 255f;
                } else {
                    red01[0] = r0 / 255f;
                    red01[1] = r1 / 255f;
                }
            }

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

    private static boolean isAlphaBelowCap(int alpha) {
        return alpha < ALPHA_CAP;
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