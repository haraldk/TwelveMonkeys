/*
 * Copyright (c) 2014, Harald Kuhr
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
 */

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.AbstractMetadata;
import com.twelvemonkeys.lang.Validate;
import org.w3c.dom.Node;

import javax.imageio.metadata.IIOMetadataNode;

/**
 * BMPMetadata.
 */
final class BMPMetadata extends AbstractMetadata {
    /** We return metadata in the exact same form as the JRE built-in, to be compatible with the DIBImageWriter. */
    public static final String nativeMetadataFormatName = "javax_imageio_bmp_1.0";

    private final DIBHeader header;
    private final int[] colorMap;

    BMPMetadata(final DIBHeader header, final int[] colorMap) {
        super(true, nativeMetadataFormatName, "com.sun.imageio.plugins.bmp.BMPMetadataFormat", null, null);
        this.header = Validate.notNull(header, "header");
        this.colorMap = colorMap == null || colorMap.length == 0 ? null : colorMap;
    }

    @Override
    protected Node getNativeTree() {
        IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);

        addChildNode(root, "BMPVersion", header.getBMPVersion());
        addChildNode(root, "Width", header.getWidth());
        addChildNode(root, "Height", header.getHeight());
        addChildNode(root, "BitsPerPixel", (short) header.getBitCount());
        addChildNode(root, "Compression", header.getCompression());
        addChildNode(root, "ImageSize", header.getImageSize());

        IIOMetadataNode pixelsPerMeter = addChildNode(root, "PixelsPerMeter", null);
        addChildNode(pixelsPerMeter, "X", header.xPixelsPerMeter);
        addChildNode(pixelsPerMeter, "Y", header.yPixelsPerMeter);

        addChildNode(root, "ColorsUsed", header.colorsUsed);
        addChildNode(root, "ColorsImportant", header.colorsImportant);

        if (header.getSize() == DIB.BITMAP_V4_INFO_HEADER_SIZE || header.getSize() == DIB.BITMAP_V5_INFO_HEADER_SIZE) {
            IIOMetadataNode mask = addChildNode(root, "Mask", null);
            addChildNode(mask, "Red", header.masks[0]);
            addChildNode(mask, "Green", header.masks[1]);
            addChildNode(mask, "Blue", header.masks[2]);
            addChildNode(mask, "Alpha", header.masks[3]);

            addChildNode(root, "ColorSpaceType", header.colorSpaceType);

            // It makes no sense to include these if colorSpaceType != 0, but native format does it...
            IIOMetadataNode cieXYZEndPoints = addChildNode(root, "CIEXYZEndPoints", null);
            addXYZPoints(cieXYZEndPoints, "Red", header.cieXYZEndpoints[0], header.cieXYZEndpoints[1], header.cieXYZEndpoints[2]);
            addXYZPoints(cieXYZEndPoints, "Green", header.cieXYZEndpoints[3], header.cieXYZEndpoints[4], header.cieXYZEndpoints[5]);
            addXYZPoints(cieXYZEndPoints, "Blue", header.cieXYZEndpoints[6], header.cieXYZEndpoints[7], header.cieXYZEndpoints[8]);

            // TODO: Gamma?! Will need a new native format version...

            addChildNode(root, "Intent", header.intent);

            // TODO: Profile data & profile size
        }

        // Palette
        if (colorMap != null) {
            IIOMetadataNode paletteNode = addChildNode(root, "Palette", null);

            // The original BitmapCoreHeader has only RGB values in the palette, all others have RGBA
            boolean hasAlpha = header.getSize() != DIB.BITMAP_CORE_HEADER_SIZE;

            for (int color : colorMap) {
                // NOTE: The native format has the red and blue values mixed up, we'll report the correct values
                IIOMetadataNode paletteEntry = addChildNode(paletteNode, "PaletteEntry", null);
                addChildNode(paletteEntry, "Red", (byte) ((color >> 16) & 0xff));
                addChildNode(paletteEntry, "Green", (byte) ((color >> 8) & 0xff));
                addChildNode(paletteEntry, "Blue", (byte) (color & 0xff));

                // Not sure why the native format specifies this, as no palette-based BMP has alpha
                if (hasAlpha) {
                    addChildNode(paletteEntry, "Alpha", (byte) ((color >>> 24) & 0xff));
                }
            }
        }

        return root;
    }

    private void addXYZPoints(IIOMetadataNode cieXYZNode, String color, double colorX, double colorY, double colorZ) {
        IIOMetadataNode colorNode = addChildNode(cieXYZNode, color, null);
        addChildNode(colorNode, "X", colorX);
        addChildNode(colorNode, "Y", colorY);
        addChildNode(colorNode, "Z", colorZ);
    }

    private IIOMetadataNode addChildNode(final IIOMetadataNode parent,
                                         final String name,
                                         final Object object) {
        IIOMetadataNode child = new IIOMetadataNode(name);

        if (object != null) {
            child.setUserObject(object); // TODO: Should we always store user object?!?!
            child.setNodeValue(object.toString()); // TODO: Fix this line
        }

        parent.appendChild(child);

        return child;
    }

    @Override
    protected IIOMetadataNode getStandardChromaNode() {
        // NOTE: BMP files may contain a color map, even if true color...
        // Not sure if this is a good idea to expose to the meta data,
        // as it might be unexpected... Then again...
        if (colorMap != null) {
            IIOMetadataNode chroma = new IIOMetadataNode("Chroma");

            IIOMetadataNode palette = new IIOMetadataNode("Palette");
            chroma.appendChild(palette);

            for (int i = 0; i < colorMap.length; i++) {
                IIOMetadataNode paletteEntry = new IIOMetadataNode("PaletteEntry");
                paletteEntry.setAttribute("index", Integer.toString(i));

                paletteEntry.setAttribute("red", Integer.toString((colorMap[i] >> 16) & 0xff));
                paletteEntry.setAttribute("green", Integer.toString((colorMap[i] >> 8) & 0xff));
                paletteEntry.setAttribute("blue", Integer.toString(colorMap[i] & 0xff));

                palette.appendChild(paletteEntry);
            }

            return chroma;
        }

        return null;
    }

    @Override
    protected IIOMetadataNode getStandardCompressionNode() {
        IIOMetadataNode compression = new IIOMetadataNode("Compression");
        IIOMetadataNode compressionTypeName = addChildNode(compression, "CompressionTypeName", null);

        // TODO: Should the compression names always match the compression names used in the ImageWriteParam?
        // OR should they be as standard as possible..?
        // The built-in plugin uses "BI_RGB", "BI_RLE8", "BI_RLE4", "BI_BITFIELDS", "BI_JPEG and "BI_PNG"
        switch (header.compression) {
            case DIB.COMPRESSION_RLE4:
            case DIB.COMPRESSION_RLE8:
                compressionTypeName.setAttribute("value", "RLE");
                break;
            case DIB.COMPRESSION_JPEG:
                compressionTypeName.setAttribute("value", "JPEG");
                break;
            case DIB.COMPRESSION_PNG:
                compressionTypeName.setAttribute("value", "PNG");
                break;
            case DIB.COMPRESSION_RGB:
            case DIB.COMPRESSION_BITFIELDS:
            case DIB.COMPRESSION_ALPHA_BITFIELDS:
            default:
                compressionTypeName.setAttribute("value", "NONE");
                break;
        }

        return compression;
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        switch (header.getBitCount()) {
            // TODO: case 0: determined by embedded format (PNG/JPEG)
            case 1:
            case 2:
            case 4:
            case 8:
                bitsPerSample.setAttribute("value", createListValue(1, Integer.toString(header.getBitCount())));
                break;

            case 16:
                // Default is 555
                bitsPerSample.setAttribute("value", header.hasMasks()
                        ? createBitsPerSampleForBitMasks()
                        : createListValue(3, Integer.toString(5)));
                break;

            case 24:
                bitsPerSample.setAttribute("value", createListValue(3, Integer.toString(8)));
                break;

            case 32:
                // Default is 888
                bitsPerSample.setAttribute("value", header.hasMasks()
                        ? createBitsPerSampleForBitMasks()
                        : createListValue(3, Integer.toString(8)));

                break;
        }

        node.appendChild(bitsPerSample);

        return node;
    }

    private String createBitsPerSampleForBitMasks() {
        boolean hasAlpha = header.masks[3] != 0;

        return createListValue(hasAlpha ? 4 : 3,
                Integer.toString(countMaskBits(header.masks[0])), Integer.toString(countMaskBits(header.masks[1])),
                Integer.toString(countMaskBits(header.masks[2])), Integer.toString(countMaskBits(header.masks[3])));
    }

    private int countMaskBits(int mask) {
        // See https://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetKernighan
        int count;

        for (count = 0; mask != 0; count++) {
            mask &= mask - 1; // clear the least significant bit set
        }

        return count;
    }

    private String createListValue(final int itemCount, final String... values) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < itemCount; i++) {
            if (buffer.length() > 0) {
                buffer.append(' ');
            }

            buffer.append(values[i % values.length]);
        }

        return buffer.toString();
    }

    @Override
    protected IIOMetadataNode getStandardDimensionNode() {
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

        if (header.xPixelsPerMeter > 0 && header.yPixelsPerMeter > 0) {
            float ratio = header.xPixelsPerMeter / (float) header.yPixelsPerMeter;
            addChildNode(dimension, "PixelAspectRatio", null)
                    .setAttribute("value", String.valueOf(ratio));

            addChildNode(dimension, "HorizontalPixelSize", null)
                    .setAttribute("value", String.valueOf(1f / header.xPixelsPerMeter * 1000));
            addChildNode(dimension, "VerticalPixelSize", null)
                    .setAttribute("value", String.valueOf(1f / header.yPixelsPerMeter * 1000));

            // Hmmm.. The JRE version includes these for some reason, even if values seem to be same as default...
            addChildNode(dimension, "HorizontalPhysicalPixelSpacing", null)
                    .setAttribute("value", String.valueOf(0));
            addChildNode(dimension, "VerticalPhysicalPixelSpacing", null)
                    .setAttribute("value", String.valueOf(0));
        }

        if (header.topDown) {
            addChildNode(dimension, "ImageOrientation", null)
                    .setAttribute("value", "FlipH"); // For BMP, bottom-up is "normal"...
        }

        return dimension;
    }

    // No document node

    // No text node

    // No tiling

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        if (header.hasMasks() && header.masks[3] != 0) {
            IIOMetadataNode transparency = new IIOMetadataNode("Transparency");
            IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
            alpha.setAttribute("value", "nonpremultiplied");
            transparency.appendChild(alpha);

            return transparency;
        }

        return null;
    }
}
