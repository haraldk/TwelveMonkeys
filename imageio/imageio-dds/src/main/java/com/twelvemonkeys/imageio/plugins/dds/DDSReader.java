/*
 * Copyright (c) 2024, Paul Allen, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Based on DDSReader.java:
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Kenji Sasaki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.twelvemonkeys.imageio.plugins.dds;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * DDSReader.java
 * <p>
 * Copyright (c) 2015 Kenji Sasaki
 * Released under the MIT license.
 * <a href="https://github.com/npedotnet/DDSReader/blob/master/LICENSE">MIT License</a>
 * <p>
 * <a href="https://github.com/npedotnet/DDSReader/blob/master/README.md">English document</a>
 * <p>
 * <a href="http://3dtech.jp/wiki/index.php?DDSReader">Japanese document</a>
 */
final class DDSReader {
    static final Order ARGB_ORDER = new Order(16, 8, 0, 24);   //  8 alpha | 8 red | 8 green | 8 blue
    static final Order RGB_16_ORDER = new Order(11, 5, 0, -1); // no alpha | 5 red | 6 green | 5 blue

    private final DDSHeader header;
    private DX10Header dxt10Header;

    DDSReader(DDSHeader header) {
        this.header = header;
    }

    int[] read(ImageInputStream imageInput, int imageIndex) throws IOException {
        // type
        DDSType type = getType();
        if (type == DDSType.DXT10) {
            dxt10Header = DX10Header.read(imageInput);
            type = dxt10Header.getDDSType();
        }

        // offset buffer to index mipmap image
        byte[] buffer = null;
        for (int i = 0; i <= imageIndex; i++) {
            int len = getLength(type, i);
            buffer = new byte[len];
            imageInput.readFully(buffer);
        }

        int width = header.getWidth(imageIndex);
        int height = header.getHeight(imageIndex);

        switch (type) {
            case DXT1:
                return decodeDXT1(width, height, buffer);
            case DXT2:
                return decodeDXT2(width, height, buffer);
            case DXT3:
                return decodeDXT3(width, height, buffer);
            case DXT4:
                return decodeDXT4(width, height, buffer);
            case DXT5:
                return decodeDXT5(width, height, buffer);
            case A1R5G5B5:
                return readA1R5G5B5(width, height, buffer);
            case X1R5G5B5:
                return readX1R5G5B5(width, height, buffer);
            case A4R4G4B4:
                return readA4R4G4B4(width, height, buffer);
            case X4R4G4B4:
                return readX4R4G4B4(width, height, buffer);
            case R5G6B5:
                return readR5G6B5(width, height, buffer);
            case R8G8B8:
                return readR8G8B8(width, height, buffer);
            case A8B8G8R8:
                return readA8B8G8R8(width, height, buffer);
            case X8B8G8R8:
                return readX8B8G8R8(width, height, buffer);
            case A8R8G8B8:
                return readA8R8G8B8(width, height, buffer);
            case X8R8G8B8:
                return readX8R8G8B8(width, height, buffer);
            default:
                throw new IIOException("Unsupported type: " + type);
        }
    }

    private DDSType getType() throws IIOException {
        int flags = header.getPixelFormatFlags();

        if ((flags & DDS.PIXEL_FORMAT_FLAG_FOURCC) != 0) {
            // DXT
            int type = header.getFourCC();
            return DDSType.valueOf(type);
        } else if ((flags & DDS.PIXEL_FORMAT_FLAG_RGB) != 0) {
            // RGB
            int bitCount = header.getBitCount();
            int redMask = header.getRedMask();
            int greenMask = header.getGreenMask();
            int blueMask = header.getBlueMask();
            int alphaMask = ((flags & 0x01) != 0) ? header.getAlphaMask() : 0; // 0x01 alpha
            if (bitCount == 16) {
                if (redMask == A1R5G5B5_MASKS[0] && greenMask == A1R5G5B5_MASKS[1] && blueMask == A1R5G5B5_MASKS[2] && alphaMask == A1R5G5B5_MASKS[3]) {
                    // A1R5G5B5
                    return DDSType.A1R5G5B5;
                } else if (redMask == X1R5G5B5_MASKS[0] && greenMask == X1R5G5B5_MASKS[1] && blueMask == X1R5G5B5_MASKS[2] && alphaMask == X1R5G5B5_MASKS[3]) {
                    // X1R5G5B5
                    return DDSType.X1R5G5B5;
                } else if (redMask == A4R4G4B4_MASKS[0] && greenMask == A4R4G4B4_MASKS[1] && blueMask == A4R4G4B4_MASKS[2] && alphaMask == A4R4G4B4_MASKS[3]) {
                    // A4R4G4B4
                    return DDSType.A4R4G4B4;
                } else if (redMask == X4R4G4B4_MASKS[0] && greenMask == X4R4G4B4_MASKS[1] && blueMask == X4R4G4B4_MASKS[2] && alphaMask == X4R4G4B4_MASKS[3]) {
                    // X4R4G4B4
                    return DDSType.X4R4G4B4;
                } else if (redMask == R5G6B5_MASKS[0] && greenMask == R5G6B5_MASKS[1] && blueMask == R5G6B5_MASKS[2] && alphaMask == R5G6B5_MASKS[3]) {
                    // R5G6B5
                    return DDSType.R5G6B5;
                } else {
                    throw new IIOException("Unsupported 16bit RGB image.");
                }
            } else if (bitCount == 24) {
                if (redMask == R8G8B8_MASKS[0] && greenMask == R8G8B8_MASKS[1] && blueMask == R8G8B8_MASKS[2] && alphaMask == R8G8B8_MASKS[3]) {
                    // R8G8B8
                    return DDSType.R8G8B8;
                } else {
                    throw new IIOException("Unsupported 24bit RGB image.");
                }
            } else if (bitCount == 32) {
                if (redMask == A8B8G8R8_MASKS[0] && greenMask == A8B8G8R8_MASKS[1] && blueMask == A8B8G8R8_MASKS[2] && alphaMask == A8B8G8R8_MASKS[3]) {
                    // A8B8G8R8
                    return DDSType.A8B8G8R8;
                } else if (redMask == X8B8G8R8_MASKS[0] && greenMask == X8B8G8R8_MASKS[1] && blueMask == X8B8G8R8_MASKS[2] && alphaMask == X8B8G8R8_MASKS[3]) {
                    // X8B8G8R8
                    return DDSType.X8B8G8R8;
                } else if (redMask == A8R8G8B8_MASKS[0] && greenMask == A8R8G8B8_MASKS[1] && blueMask == A8R8G8B8_MASKS[2] && alphaMask == A8R8G8B8_MASKS[3]) {
                    // A8R8G8B8
                    return DDSType.A8R8G8B8;
                } else if (redMask == X8R8G8B8_MASKS[0] && greenMask == X8R8G8B8_MASKS[1] && blueMask == X8R8G8B8_MASKS[2] && alphaMask == X8R8G8B8_MASKS[3]) {
                    // X8R8G8B8
                    return DDSType.X8R8G8B8;
                } else {
                    throw new IIOException("Unsupported 32bit RGB image.");
                }
            } else {
                throw new IIOException("Unsupported bit count: " + bitCount);
            }
        } else {
            throw new IIOException("Unsupported YUV or LUMINANCE image.");
        }
    }

    private int getLength(DDSType type, int imageIndex) throws IIOException {
        int width = header.getWidth(imageIndex);
        int height = header.getHeight(imageIndex);

        switch (type) {
            case DXT1:
                return 8 * ((width + 3) / 4) * ((height + 3) / 4);
            case DXT2:
            case DXT3:
            case DXT4:
            case DXT5:
                return 16 * ((width + 3) / 4) * ((height + 3) / 4);
            case A1R5G5B5:
            case X1R5G5B5:
            case A4R4G4B4:
            case X4R4G4B4:
            case R5G6B5:
            case R8G8B8:
            case A8B8G8R8:
            case X8B8G8R8:
            case A8R8G8B8:
            case X8R8G8B8:
                return (type.value() & 0xFF) * width * height;
            default:
                throw new IIOException("Unknown type: " + Integer.toHexString(type.value()));
        }
    }


    private static int[] decodeDXT1(int width, int height, byte[] buffer) {
        int[] pixels = new int[width * height];
        int index = 0;
        int w = (width + 3) / 4;
        int h = (height + 3) / 4;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int c0 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
                index += 2;
                int c1 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
                index += 2;
                for (int k = 0; k < 4; k++) {
                    if (4 * i + k >= height) break;
                    int t0 = (buffer[index] & 0x03);
                    int t1 = (buffer[index] & 0x0C) >> 2;
                    int t2 = (buffer[index] & 0x30) >> 4;
                    int t3 = (buffer[index++] & 0xC0) >> 6;
                    pixels[4 * width * i + 4 * j + width * k] = getDXTColor(c0, c1, 0xFF, t0);
                    if (4 * j + 1 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 1] = getDXTColor(c0, c1, 0xFF, t1);
                    if (4 * j + 2 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 2] = getDXTColor(c0, c1, 0xFF, t2);
                    if (4 * j + 3 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 3] = getDXTColor(c0, c1, 0xFF, t3);
                }
            }
        }
        return pixels;
    }

    private static int[] decodeDXT2(int width, int height, byte[] buffer) {
        return decodeDXT3(width, height, buffer);
    }

    private static int[] decodeDXT3(int width, int height, byte[] buffer) {
        int index = 0;
        int w = (width + 3) / 4;
        int h = (height + 3) / 4;
        int[] pixels = new int[width * height];
        int[] alphaTable = new int[16];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                // create alpha table(4bit to 8bit)
                for (int k = 0; k < 4; k++) {
                    int a0 = (buffer[index++] & 0xFF);
                    int a1 = (buffer[index++] & 0xFF);
                    // 4bit alpha to 8bit alpha
                    alphaTable[4 * k] = 17 * ((a0 & 0xF0) >> 4);
                    alphaTable[4 * k + 1] = 17 * (a0 & 0x0F);
                    alphaTable[4 * k + 2] = 17 * ((a1 & 0xF0) >> 4);
                    alphaTable[4 * k + 3] = 17 * (a1 & 0x0F);
                }
                int c0 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
                index += 2;
                int c1 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
                index += 2;
                for (int k = 0; k < 4; k++) {
                    if (4 * i + k >= height) break;
                    int t0 = (buffer[index] & 0x03);
                    int t1 = (buffer[index] & 0x0C) >> 2;
                    int t2 = (buffer[index] & 0x30) >> 4;
                    int t3 = (buffer[index++] & 0xC0) >> 6;
                    pixels[4 * width * i + 4 * j + width * k] = getDXTColor(c0, c1, alphaTable[4 * k], t0);
                    if (4 * j + 1 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 1] = getDXTColor(c0, c1, alphaTable[4 * k + 1], t1);
                    if (4 * j + 2 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 2] = getDXTColor(c0, c1, alphaTable[4 * k + 2], t2);
                    if (4 * j + 3 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 3] = getDXTColor(c0, c1, alphaTable[4 * k + 3], t3);
                }
            }
        }
        return pixels;
    }

    private static int[] decodeDXT4(int width, int height, byte[] buffer) {
        return decodeDXT5(width, height, buffer);
    }

    private static int[] decodeDXT5(int width, int height, byte[] buffer) {
        int index = 0;
        int w = (width + 3) / 4;
        int h = (height + 3) / 4;
        int[] pixels = new int[width * height];
        int[] alphaTable = new int[16];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                // create alpha table
                int a0 = (buffer[index++] & 0xFF);
                int a1 = (buffer[index++] & 0xFF);
                int b0 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8 | (buffer[index + 2] & 0xFF) << 16;
                index += 3;
                int b1 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8 | (buffer[index + 2] & 0xFF) << 16;
                index += 3;
                alphaTable[0] = b0 & 0x07;
                alphaTable[1] = (b0 >> 3) & 0x07;
                alphaTable[2] = (b0 >> 6) & 0x07;
                alphaTable[3] = (b0 >> 9) & 0x07;
                alphaTable[4] = (b0 >> 12) & 0x07;
                alphaTable[5] = (b0 >> 15) & 0x07;
                alphaTable[6] = (b0 >> 18) & 0x07;
                alphaTable[7] = (b0 >> 21) & 0x07;
                alphaTable[8] = b1 & 0x07;
                alphaTable[9] = (b1 >> 3) & 0x07;
                alphaTable[10] = (b1 >> 6) & 0x07;
                alphaTable[11] = (b1 >> 9) & 0x07;
                alphaTable[12] = (b1 >> 12) & 0x07;
                alphaTable[13] = (b1 >> 15) & 0x07;
                alphaTable[14] = (b1 >> 18) & 0x07;
                alphaTable[15] = (b1 >> 21) & 0x07;
                int c0 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
                index += 2;
                int c1 = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
                index += 2;
                for (int k = 0; k < 4; k++) {
                    if (4 * i + k >= height) break;
                    int t0 = (buffer[index] & 0x03);
                    int t1 = (buffer[index] & 0x0C) >> 2;
                    int t2 = (buffer[index] & 0x30) >> 4;
                    int t3 = (buffer[index++] & 0xC0) >> 6;
                    pixels[4 * width * i + 4 * j + width * k] = getDXTColor(c0, c1, getDXT5Alpha(a0, a1, alphaTable[4 * k]), t0);
                    if (4 * j + 1 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 1] = getDXTColor(c0, c1, getDXT5Alpha(a0, a1, alphaTable[4 * k + 1]), t1);
                    if (4 * j + 2 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 2] = getDXTColor(c0, c1, getDXT5Alpha(a0, a1, alphaTable[4 * k + 2]), t2);
                    if (4 * j + 3 >= width) continue;
                    pixels[4 * width * i + 4 * j + width * k + 3] = getDXTColor(c0, c1, getDXT5Alpha(a0, a1, alphaTable[4 * k + 3]), t3);
                }
            }
        }
        return pixels;
    }

    private static int[] readA1R5G5B5(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int rgba = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
            index += 2;
            int r = BIT5[(rgba & A1R5G5B5_MASKS[0]) >> 10];
            int g = BIT5[(rgba & A1R5G5B5_MASKS[1]) >> 5];
            int b = BIT5[(rgba & A1R5G5B5_MASKS[2])];
            int a = 255 * ((rgba & A1R5G5B5_MASKS[3]) >> 15);
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readX1R5G5B5(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int rgba = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
            index += 2;
            int r = BIT5[(rgba & X1R5G5B5_MASKS[0]) >> 10];
            int g = BIT5[(rgba & X1R5G5B5_MASKS[1]) >> 5];
            int b = BIT5[(rgba & X1R5G5B5_MASKS[2])];
            int a = 255;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readA4R4G4B4(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int rgba = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
            index += 2;
            int r = 17 * ((rgba & A4R4G4B4_MASKS[0]) >> 8);
            int g = 17 * ((rgba & A4R4G4B4_MASKS[1]) >> 4);
            int b = 17 * ((rgba & A4R4G4B4_MASKS[2]));
            int a = 17 * ((rgba & A4R4G4B4_MASKS[3]) >> 12);
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readX4R4G4B4(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int rgba = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
            index += 2;
            int r = 17 * ((rgba & A4R4G4B4_MASKS[0]) >> 8);
            int g = 17 * ((rgba & A4R4G4B4_MASKS[1]) >> 4);
            int b = 17 * ((rgba & A4R4G4B4_MASKS[2]));
            int a = 255;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readR5G6B5(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int rgba = (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8;
            index += 2;
            int r = BIT5[((rgba & R5G6B5_MASKS[0]) >> 11)];
            int g = BIT6[((rgba & R5G6B5_MASKS[1]) >> 5)];
            int b = BIT5[((rgba & R5G6B5_MASKS[2]))];
            int a = 255;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readR8G8B8(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int b = buffer[index++] & 0xFF;
            int g = buffer[index++] & 0xFF;
            int r = buffer[index++] & 0xFF;
            int a = 255;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readA8B8G8R8(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int r = buffer[index++] & 0xFF;
            int g = buffer[index++] & 0xFF;
            int b = buffer[index++] & 0xFF;
            int a = buffer[index++] & 0xFF;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readX8B8G8R8(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int r = buffer[index++] & 0xFF;
            int g = buffer[index++] & 0xFF;
            int b = buffer[index++] & 0xFF;
            int a = 255;
            index++;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readA8R8G8B8(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int b = buffer[index++] & 0xFF;
            int g = buffer[index++] & 0xFF;
            int r = buffer[index++] & 0xFF;
            int a = buffer[index++] & 0xFF;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    private static int[] readX8R8G8B8(int width, int height, byte[] buffer) {
        int index = 0;
        int[] pixels = new int[width * height];
        for (int i = 0; i < height * width; i++) {
            int b = buffer[index++] & 0xFF;
            int g = buffer[index++] & 0xFF;
            int r = buffer[index++] & 0xFF;
            int a = 255;
            index++;
            pixels[i] = (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
        }
        return pixels;
    }

    static int getDXTColor(int c0, int c1, int a, int t) {
        switch (t) {
            case 0:
                return getDXTColor1(c0, a);
            case 1:
                return getDXTColor1(c1, a);
            case 2:
                return (c0 > c1) ? getDXTColor2_1(c0, c1, a) : getDXTColor1_1(c0, c1, a);
            case 3:
                return (c0 > c1) ? getDXTColor2_1(c1, c0, a) : 0;
        }
        return 0;
    }

    private static int getDXTColor2_1(int c0, int c1, int a) {
        // 2*c0/3 + c1/3
        int r = (2 * BIT5[(c0 & 0xF800) >> 11] + BIT5[(c1 & 0xF800) >> 11]) / 3;
        int g = (2 * BIT6[(c0 & 0x07E0) >> 5] + BIT6[(c1 & 0x07E0) >> 5]) / 3;
        int b = (2 * BIT5[c0 & 0x001F] + BIT5[c1 & 0x001F]) / 3;
        return (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
    }

    private static int getDXTColor1_1(int c0, int c1, int a) {
        // (c0+c1) / 2
        int r = (BIT5[(c0 & 0xF800) >> 11] + BIT5[(c1 & 0xF800) >> 11]) / 2;
        int g = (BIT6[(c0 & 0x07E0) >> 5] + BIT6[(c1 & 0x07E0) >> 5]) / 2;
        int b = (BIT5[c0 & 0x001F] + BIT5[c1 & 0x001F]) / 2;
        return (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
    }

    private static int getDXTColor1(int c, int a) {
        int r = BIT5[(c & 0xF800) >> 11];
        int g = BIT6[(c & 0x07E0) >> 5];
        int b = BIT5[(c & 0x001F)];
        return (a << ARGB_ORDER.alphaShift) | (r << ARGB_ORDER.redShift) | (g << ARGB_ORDER.greenShift) | (b << ARGB_ORDER.blueShift);
    }

    private static int getDXT5Alpha(int a0, int a1, int t) {
        if (a0 > a1) switch (t) {
            case 0:
                return a0;
            case 1:
                return a1;
            case 2:
                return (6 * a0 + a1) / 7;
            case 3:
                return (5 * a0 + 2 * a1) / 7;
            case 4:
                return (4 * a0 + 3 * a1) / 7;
            case 5:
                return (3 * a0 + 4 * a1) / 7;
            case 6:
                return (2 * a0 + 5 * a1) / 7;
            case 7:
                return (a0 + 6 * a1) / 7;
        }
        else switch (t) {
            case 0:
                return a0;
            case 1:
                return a1;
            case 2:
                return (4 * a0 + a1) / 5;
            case 3:
                return (3 * a0 + 2 * a1) / 5;
            case 4:
                return (2 * a0 + 3 * a1) / 5;
            case 5:
                return (a0 + 4 * a1) / 5;
            case 6:
                return 0;
            case 7:
                return 255;
        }
        return 0;
    }

    // RGBA Masks
    static final int[] A1R5G5B5_MASKS = {0x7C00, 0x03E0, 0x001F, 0x8000};
    static final int[] X1R5G5B5_MASKS = {0x7C00, 0x03E0, 0x001F, 0x0000};
    static final int[] A4R4G4B4_MASKS = {0x0F00, 0x00F0, 0x000F, 0xF000};
    static final int[] X4R4G4B4_MASKS = {0x0F00, 0x00F0, 0x000F, 0x0000};
    static final int[] R5G6B5_MASKS = {0xF800, 0x07E0, 0x001F, 0x0000};
    static final int[] R8G8B8_MASKS = {0xFF0000, 0x00FF00, 0x0000FF, 0x000000};
    static final int[] A8B8G8R8_MASKS = {0x000000FF, 0x0000FF00, 0x00FF0000, 0xFF000000};
    static final int[] X8B8G8R8_MASKS = {0x000000FF, 0x0000FF00, 0x00FF0000, 0x00000000};
    static final int[] A8R8G8B8_MASKS = {0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000};
    static final int[] X8R8G8B8_MASKS = {0x00FF0000, 0x0000FF00, 0x000000FF, 0x00000000};

    // BIT4 = 17 * index;
    static final int[] BIT5 = {0, 8, 16, 25, 33, 41, 49, 58, 66, 74, 82, 90, 99, 107, 115, 123, 132, 140, 148, 156, 165, 173, 181, 189, 197, 206, 214, 222, 230, 239, 247, 255};
    static final int[] BIT6 = {0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 45, 49, 53, 57, 61, 65, 69, 73, 77, 81, 85, 89, 93, 97, 101, 105, 109, 113, 117, 121, 125, 130, 134, 138, 142, 146, 150, 154, 158, 162, 166, 170, 174, 178, 182, 186, 190, 194, 198, 202, 206, 210, 215, 219, 223, 227, 231, 235, 239, 243, 247, 251, 255};

    static final class Order {
        Order(int redShift, int greenShift, int blueShift, int alphaShift) {
            this.redShift = redShift;
            this.greenShift = greenShift;
            this.blueShift = blueShift;
            this.alphaShift = alphaShift;
        }

        public int redShift;
        public int greenShift;
        public int blueShift;
        public int alphaShift;
    }

}
