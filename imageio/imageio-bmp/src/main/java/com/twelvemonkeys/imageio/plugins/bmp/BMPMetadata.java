/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    /** We return metadata in the exact same form as the JRE built-in, to be compatible with the BMPImageWriter. */
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
        compressionTypeName.setAttribute("value", "NONE");

        return compression;
//        switch (header.getImageType()) {
//            case TGA.IMAGETYPE_COLORMAPPED_RLE:
//            case TGA.IMAGETYPE_TRUECOLOR_RLE:
//            case TGA.IMAGETYPE_MONOCHROME_RLE:
//            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
//            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
//                IIOMetadataNode node = new IIOMetadataNode("Compression");
//                IIOMetadataNode compressionTypeName = new IIOMetadataNode("CompressionTypeName");
//
//                // Compression can be RLE4, RLE8, PNG, JPEG or NONE
//                String value = header.getImageType() == TGA.IMAGETYPE_COLORMAPPED_HUFFMAN || header.getImageType() == TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE
//                                ? "Uknown" : "RLE";
//                compressionTypeName.setAttribute("value", value);
//                node.appendChild(compressionTypeName);
//
//                IIOMetadataNode lossless = new IIOMetadataNode("Lossless");
//                lossless.setAttribute("value", "TRUE"); // TODO: Unless JPEG!
//                node.appendChild(lossless);
//
//                return node;
//            default:
//                // No compression
//                return null;
//        }
    }

    @Override
    protected IIOMetadataNode getStandardDataNode() {
        IIOMetadataNode node = new IIOMetadataNode("Data");

//        IIOMetadataNode planarConfiguration = new IIOMetadataNode("PlanarConfiguration");
//        planarConfiguration.setAttribute("value", "PixelInterleaved");
//        node.appendChild(planarConfiguration);

//        IIOMetadataNode sampleFormat = new IIOMetadataNode("SampleFormat");
//        switch (header.getImageType()) {
//            case TGA.IMAGETYPE_COLORMAPPED:
//            case TGA.IMAGETYPE_COLORMAPPED_RLE:
//            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN:
//            case TGA.IMAGETYPE_COLORMAPPED_HUFFMAN_QUADTREE:
//                sampleFormat.setAttribute("value", "Index");
//                break;
//            default:
//                sampleFormat.setAttribute("value", "UnsignedIntegral");
//                break;
//        }

//        node.appendChild(sampleFormat);

        IIOMetadataNode bitsPerSample = new IIOMetadataNode("BitsPerSample");
        switch (header.getBitCount()) {
            case 1:
            case 2:
            case 4:
            case 8:
                bitsPerSample.setAttribute("value", createListValue(1, Integer.toString(header.getBitCount())));
                break;
            case 16:
                // TODO: Consult masks here!
                bitsPerSample.setAttribute("value", createListValue(4, Integer.toString(4)));
                break;
            case 24:
                bitsPerSample.setAttribute("value", createListValue(3, Integer.toString(8)));
                break;
            case 32:
                bitsPerSample.setAttribute("value", createListValue(4, Integer.toString(8)));
                break;
        }

        node.appendChild(bitsPerSample);

        // TODO: Do we need MSB?
//        IIOMetadataNode sampleMSB = new IIOMetadataNode("SampleMSB");
//        sampleMSB.setAttribute("value", createListValue(header.getChannels(), "0"));

        return node;
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
        if (header.xPixelsPerMeter > 0 || header.yPixelsPerMeter > 0) {
            IIOMetadataNode dimension = new IIOMetadataNode("Dimension");

            addChildNode(dimension, "PixelAspectRatio", null);
            addChildNode(dimension, "HorizontalPhysicalPixelSpacing", null);
            addChildNode(dimension, "VerticalPhysicalPixelSpacing", null);

//        IIOMetadataNode imageOrientation = new IIOMetadataNode("ImageOrientation");
//
//        if (header.topDown) {
//            imageOrientation.setAttribute("value", "FlipH");
//        }
//        else {
//            imageOrientation.setAttribute("value", "Normal");
//        }
//
//        dimension.appendChild(imageOrientation);

            return dimension;
        }

        return null;
    }

    // No document node

    // No text node

    // No tiling

    @Override
    protected IIOMetadataNode getStandardTransparencyNode() {
        return null;

//        IIOMetadataNode transparency = new IIOMetadataNode("Transparency");
//
//        IIOMetadataNode alpha = new IIOMetadataNode("Alpha");
//
//        // TODO: Consult masks
//        alpha.setAttribute("value", header.getBitCount() == 32 ? "nonpremultiplied" : "none");
//        transparency.appendChild(alpha);
//
//        return transparency;
    }
}
