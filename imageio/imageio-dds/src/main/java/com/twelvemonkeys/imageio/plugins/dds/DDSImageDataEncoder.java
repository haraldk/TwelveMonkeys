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
 * <p>
 * References:
 * [1] <a href="https://www.ludicon.com/castano/blog/2009/03/gpu-dxt-decompression/">GPU DXT Decompression</a>.
 * [2] <a href="https://sv-journal.org/2014-1/06/en/index.php">TEXTURE COMPRESSION TECHNIQUES</a>.
 * [3] <a href="https://mrelusive.com/publications/papers/Real-Time-Dxt-Compression.pdf">Real-Time DXT Compression by J.M.P. van Waveren</a>
 * </p>
 */
public class DDSImageDataEncoder {
    //A cap for alpha value for BC1 where if alpha value is smaller than this, the 4x4 block will enable alpha mode.
    private static final int ALPHA_CAP = 128;
    private static final int C565_5_MASK = 0xF8;
    private static final int C565_6_MASK = 0xFC;

    static void writeImageData(ImageOutputStream imageOutput, RenderedImage renderedImage, DDSType type) throws IOException {
        switch (type) {
            case DXT1:
                BlockCompressor1.encode(imageOutput, renderedImage);
                break;
            default:
                throw new IllegalArgumentException("DDS Type is not supported for encoder yet");
        }
    }

    /**
     * Handles BC1 compression.
     */
    private static final class BlockCompressor1 {
        static void encode(ImageOutputStream imageOutput, RenderedImage image) throws IOException {
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            int blocksXCount = (imageWidth + 3) / 4;
            int blocksYCount = (imageHeight + 3) / 4;
            Raster raster = image.getData();

            //r-g-b-a = 4 uint8 each pixel
            int[] sampled = new int[64];
            //color0,1 : space 565
            //color2,3 : space 888
            int[] palettes = new int[4];
            int[][] colors = new int[4][4];

            //starting a 4x4 block
            for (int blockY = 0; blockY < blocksYCount; blockY++) {
                for (int blockX = 0; blockX < blocksXCount; blockX++) {
                    raster.getPixels(blockX * 4, blockY * 4, 4, 4, sampled);
                    boolean alphaMode = getBlockEndpoints2(sampled, palettes);

                    imageOutput.writeShort((short) palettes[0]);
                    imageOutput.writeShort((short) palettes[1]);
                    calculateIntermediate(alphaMode, palettes);
                    //indices encoding start.
                    int indices =
                            encodeBlockIndices(alphaMode, sampled, palettes);
                            //encodeBlockIndices2(alphaMode, sampled, palettes[0], palettes[1], colors);
                    imageOutput.writeInt(indices);
                }
            }
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
        private static int encodeBlockIndices(boolean alphaMode, int[] sampled, int[] palettes) {
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
                    if (palettes[0] > palettes[1]) {
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
        @SuppressWarnings("DuplicatedCode")//just in case intellij warns for 'duplication'
        private static void calculateIntermediate(boolean alphaMode, int[] palettes) {
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
        private static boolean getBlockEndpoints2(int[] sampled, int[] paletteBuffer) {
            int maxDistance = -1;
            boolean alphaMode = false;
            for (int i = 0; i < 60; i += 4) {
                for (int j = i + 4; j < 64; j += 4) {
                    int distance = getColorDistance(sampled[i], sampled[i + 1], sampled[i + 2], sampled[j], sampled[j + 1], sampled[j + 2]);
                    if (!alphaMode && isAlphaBelowCap(Math.min(sampled[i + 3], sampled[j + 3]))) {
                        alphaMode = true;
                    }
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        paletteBuffer[0] = convertTo565(sampled[i], sampled[i + 1], sampled[i + 2]);
                        paletteBuffer[1] = convertTo565(sampled[j], sampled[j + 1], sampled[j + 2]);
                    }
                }
            }

            if ((alphaMode && paletteBuffer[0] > paletteBuffer[1]) || (!alphaMode && paletteBuffer[1] > paletteBuffer[0])){
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

        private static boolean isAlphaBelowCap(int alpha) {
            return alpha < ALPHA_CAP;
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

    //pack 32 bits of the colors to a single int value.
    private static int color888ToInt(int r, int g, int b, int a) {
        return (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
    }

    //pack 16 bits of the colors to a single int value.
    private static int color565ToInt(int r5, int g6, int b5) {
        return (r5 << RGB_16_ORDER.redShift) | (g6 << RGB_16_ORDER.greenShift) | (b5 << RGB_16_ORDER.blueShift);
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