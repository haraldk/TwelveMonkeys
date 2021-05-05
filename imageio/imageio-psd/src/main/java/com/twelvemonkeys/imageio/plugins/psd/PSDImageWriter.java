/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.RasterUtils;
import com.twelvemonkeys.io.enc.EncoderStream;
import com.twelvemonkeys.io.enc.PackBitsEncoder;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Collections;

/**
 * Minimal ImageWriter for Adobe Photoshop Document (PSD) format.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageWriter.java,v 1.0 Apr 29, 2008 4:45:52 PM haraldk Exp$
 * @see <a href="http://www.adobe.com/devnet-apps/photoshop/fileformatashtml/">Adobe Photoshop File Formats Specification<a>
 * @see <a href="http://www.fileformat.info/format/psd/egff.htm">Adobe Photoshop File Format Summary<a>
 */
public final class PSDImageWriter extends ImageWriterBase  {

    PSDImageWriter(ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        // TODO: Implement
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        // TODO: Implement
        return null;
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new PSDImageWriteParam(getLocale());
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage iioImage, ImageWriteParam param) throws IOException {
        assertOutput();
        imageOutput.setByteOrder(ByteOrder.BIG_ENDIAN);

        RenderedImage image = iioImage.getRenderedImage();
        SampleModel sampleModel = image.getSampleModel();

        int colorComponents = image.getColorModel().getColorSpace().getNumComponents();
        int channels = sampleModel.getNumBands();
        int width = image.getWidth();
        int height = image.getHeight();

        int bits = getBitsPerSample(sampleModel);
        int mode = getColorMode(image.getColorModel());
        // TODO: Allow stream metadata or param to force PSD/PSB (version 1/2)?
        boolean largeFormat = width > PSDHeader.PSD_MAX_SIZE || height > PSDHeader.PSD_MAX_SIZE;

        new PSDHeader(channels, width, height, bits, mode, largeFormat).write(imageOutput);
        writeColorModeData(image, mode);
        writeImageResources(image, mode);

        // Length of the layer and mask information section. (**PSB** length is 8 bytes.)
        // TODO: Write an empty dummy layer here, if there's alpha? See below... Or see if Photoshop handles alpha if no layers at all...
        if (largeFormat) {
            imageOutput.writeLong(0);
        }
        else {
            imageOutput.writeInt(0);
        }

        processImageStarted(0);

        // Image Data Section (composite layer only).
        // The last section of a Photoshop file contains the image pixel data.
        // Image data is stored in planar order: first all the red data, then all the green data, etc.
        // Each plane is stored in scan-line order, with no pad bytes,
        final int compression = PSDImageWriteParam.getCompressionType(param);
        imageOutput.writeShort(compression);

        long byteCountPos = imageOutput.getStreamPosition();
        // PSB (large format) byte counts are actually 32 bit offsets, not 16 bit as described in spec
        int[] byteCounts = new int[compression == PSD.COMPRESSION_RLE ? height * channels : 0];
        imageOutput.skipBytes(byteCounts.length * (largeFormat ? 4 : 2));

        // TODO: Loop over tiles?
        for (int channel = 0; channel < channels; channel++) {
            // TODO: Alpha issues:
            //  1. Alpha channel is written (but not read, because there are no layers, and alpha is considered present only if layer count is negative)
            //     - Can we write a small hidden layer, just to have -1 layers?
            //  2. Alpha needs to be premultiplied against white background (to avoid inverse halo)
            Raster tile = sampleModel.getTransferType() == DataBuffer.TYPE_INT && sampleModel instanceof SinglePixelPackedSampleModel
                          ? RasterUtils.asByteRaster(image.getTile(0, 0))
                          : image.getTile(0, 0);
            Raster channelRaster = tile.createChild(0, 0, width, height, 0, 0, new int[] {channel});

            switch (bits) {
                case 1:
                    // TODO: Figure out why we can't write multi-pixel packed 1 bit samples as bytes...
                case 8:
                    write8BitChannel(channel, colorComponents, mode, compression, channelRaster, byteCounts);
                    break;
                case 16:
                    write16BitChannel(channel, colorComponents, mode, compression, channelRaster, byteCounts);
                    break;
                case 32:
                    write32BitChannel(channel, colorComponents, mode, compression, channelRaster, byteCounts);
                    break;
                default:
                    throw new AssertionError(); // Should be guarded against already
            }

            processImageProgress(channel * 100f / channels);
        }

        updateByteCounts(byteCountPos, byteCounts, largeFormat);

        processImageComplete();
    }

    private void updateByteCounts(long byteCountPos, int[] byteCounts, boolean largeFormat) throws IOException {
        if (byteCounts.length == 0) {
            return;
        }

        // Update byte counts for RLE
        long pos = imageOutput.getStreamPosition();

        imageOutput.seek(byteCountPos);
        if (largeFormat) {
            imageOutput.writeInts(byteCounts, 0, byteCounts.length);
        }
        else {
            for (int byteCount : byteCounts) {
                imageOutput.writeShort(byteCount);
            }
        }

        imageOutput.seek(pos);
    }

    private void writeColorModeData(RenderedImage image, int mode) throws IOException {
        if (mode == PSD.COLOR_MODE_INDEXED) {
            IndexColorModel icm = (IndexColorModel) image.getColorModel();

            // Indexed color images: length is 768; color data contains the color table for the image, in non-interleaved order.
            imageOutput.writeInt(768);
            byte[] colors = new byte[256];

            icm.getReds(colors);
            imageOutput.write(colors);
            icm.getGreens(colors);
            imageOutput.write(colors);
            icm.getBlues(colors);
            imageOutput.write(colors);
        }
        else {
            imageOutput.writeInt(0);
        }
    }

    private void writeImageResources(RenderedImage image, int mode) throws IOException {
        // Length of image resource section. The length may be zero
        imageOutput.writeInt(0);
        long startImageResources  = imageOutput.getStreamPosition();

        // Write ICC color profile if not "native" sRGB or gray (or bitmap/indexed)
        if (mode != PSD.COLOR_MODE_BITMAP && mode != PSD.COLOR_MODE_INDEXED) {
            ColorSpace colorSpace = image.getColorModel().getColorSpace();
            if (!colorSpace.isCS_sRGB() && colorSpace instanceof ICC_ColorSpace) {
                ICC_Profile profile = ((ICC_ColorSpace) colorSpace).getProfile();
                ICCProfile.writeData(imageOutput, profile);
            }
        }

        // Write creator software (Exif)
        Entry software = new TIFFEntry(TIFF.TAG_SOFTWARE, TIFF.TYPE_ASCII, "TwelveMonkeys ImageIO PSD writer " + originatingProvider.getVersion());
        PSDEXIF1Data.writeData(imageOutput, Collections.singleton(software));

        long endImageResources = imageOutput.getStreamPosition();

        // Update image resources length
        imageOutput.seek(startImageResources - 4);
        imageOutput.writeInt((int) (endImageResources - startImageResources));
        imageOutput.seek(endImageResources);
    }

    private void write8BitChannel(int channel, int colorComponents, int colorMode, int compression, Raster raster, int[] byteCounts) throws IOException {
        int width = raster.getWidth();
        int height = raster.getHeight();

        byte[] rowBytes = null;

        for (int y = 0; y < height; y++) {
            rowBytes = (byte[]) raster.getDataElements(0, y, width, 1, rowBytes);

            // Photoshop likes to store CMYK values inverted (but not the alpha value)
            if (colorMode == PSD.COLOR_MODE_CMYK && channel < colorComponents) {
                for (int i = 0; i < rowBytes.length; i++) {
                    rowBytes[i] = (byte) (0xff - rowBytes[i] & 0xff);
                }
            }

            if (compression == PSD.COMPRESSION_NONE) {
                imageOutput.write(rowBytes);
            }
            else if (compression == PSD.COMPRESSION_RLE) {
                long startPos = imageOutput.getStreamPosition();

                // The RLE compressed data follows, with each scan line compressed separately
                try (OutputStream stream = new EncoderStream(IIOUtil.createStreamAdapter(imageOutput), new PackBitsEncoder())) {
                    stream.write(rowBytes);
                }

                long endPos = imageOutput.getStreamPosition();
                byteCounts[y + channel * height] = (int) (endPos - startPos);
            }
            else {
                throw new IIOException("PSD with ZIP compression not supported");
            }
        }
    }

    private void write16BitChannel(int channel, int colorComponents, int colorMode, int compression, Raster raster, int[] byteCounts) throws IOException {
        int width = raster.getWidth();
        int height = raster.getHeight();

        short[] row = null;

        for (int y = 0; y < height; y++) {
            row = (short[]) raster.getDataElements(0, y, width, 1, row);

            // Photoshop likes to store CMYK values inverted (but not the alpha value)
            if (colorMode == PSD.COLOR_MODE_CMYK && channel < colorComponents) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = (short) (0xffff - row[i] & 0xffff);
                }
            }

            if (compression == PSD.COMPRESSION_NONE) {
                imageOutput.writeShorts(row, 0, row.length);
            }
            else if (compression == PSD.COMPRESSION_RLE) {
                long startPos = imageOutput.getStreamPosition();

                // The RLE compressed data follows, with each scan line compressed separately
                try (DataOutputStream stream = new DataOutputStream(new EncoderStream(IIOUtil.createStreamAdapter(imageOutput), new PackBitsEncoder()))) {
                    for (short sample : row) {
                        stream.writeShort(sample);
                    }
                }

                long endPos = imageOutput.getStreamPosition();
                byteCounts[y + channel * height] = (int) (endPos - startPos);
            }
            else {
                throw new IIOException("PSD with ZIP compression not supported");
            }
        }
    }

    private void write32BitChannel(int channel, int colorComponents, int colorMode, int compression, Raster raster, int[] byteCounts) throws IOException {
        int width = raster.getWidth();
        int height = raster.getHeight();

        int[] row = null;

        for (int y = 0; y < height; y++) {
            row = (int[]) raster.getDataElements(0, y, width, 1, row);

            // Photoshop likes to store CMYK values inverted (but not the alpha value)
            if (colorMode == PSD.COLOR_MODE_CMYK && channel < colorComponents) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = 0xffffffff - row[i];
                }
            }

            if (compression == PSD.COMPRESSION_NONE) {
                imageOutput.writeInts(row, 0, row.length);
            }
            else if (compression == PSD.COMPRESSION_RLE) {
                long startPos = imageOutput.getStreamPosition();

                // The RLE compressed data follows, with each scan line compressed separately
                try (DataOutputStream stream = new DataOutputStream(new EncoderStream(IIOUtil.createStreamAdapter(imageOutput), new PackBitsEncoder()))) {
                    for (int sample : row) {
                        stream.writeInt(sample);
                    }
                }

                long endPos = imageOutput.getStreamPosition();
                byteCounts[y + channel * height] = (int) (endPos - startPos);
            }
            else {
                throw new IIOException("PSD with ZIP compression not supported");
            }
        }
    }

    static int getColorMode(ColorModel colorModel) {
        if (colorModel instanceof IndexColorModel) {
            if (colorModel.getPixelSize() == 1) {
                return PSD.COLOR_MODE_BITMAP;
            }
            else {
                return PSD.COLOR_MODE_INDEXED;
            }
        }

        int csType = colorModel.getColorSpace().getType();
        switch (csType) {
            case ColorSpace.TYPE_GRAY:
                if (colorModel.getPixelSize() == 1) {
                    return PSD.COLOR_MODE_BITMAP;
                }
                else {
                    return PSD.COLOR_MODE_GRAYSCALE;
                }
            case ColorSpace.TYPE_RGB:
                return PSD.COLOR_MODE_RGB;
            case ColorSpace.TYPE_CMYK:
                return PSD.COLOR_MODE_CMYK;
            default:
                throw new IllegalArgumentException("Unsupported color space type for PSD:  " + csType);
        }
    }

    static int getBitsPerSample(SampleModel sampleModel) {
        int bits = sampleModel.getSampleSize(0);

        for (int i = 1; i < sampleModel.getNumBands(); i++) {
            if (bits != sampleModel.getSampleSize(i)) {
                throw new IllegalArgumentException("All samples must be of equal size for PSD: " + bits);
            }
        }

        switch (bits) {
            case 1:
            case 8:
            case 16:
            case 32:
                return (short) bits;
            default:
                throw new IllegalArgumentException("Unsupported sample size for PSD (expected 1, 8, 16 or 32): " + bits);
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new File(args[0]));
        ImageIO.write(image, "PSD", new File("test.psd"));
    }
}
