/*
 * Copyright (c) 2015, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.hdr;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.plugins.hdr.tonemap.ToneMapper;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * HDRImageReader.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: HDRImageReader.java,v 1.0 27/07/15 harald.kuhr Exp$
 */
public final class HDRImageReader extends ImageReaderBase {
    // Specs: http://radsite.lbl.gov/radiance/refer/filefmts.pdf

    private HDRHeader header;

    protected HDRImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        header = null;
    }

    private void readHeader() throws IOException {
        if (header == null) {
            header = HDRHeader.read(imageInput);

            imageInput.flushBefore(imageInput.getStreamPosition());
        }

        imageInput.seek(imageInput.getFlushedPosition());
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getWidth();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return header.getHeight();
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        return Collections.singletonList(ImageTypeSpecifiers.createInterleaved(sRGB, new int[] {0, 1, 2}, DataBuffer.TYPE_FLOAT, false, false)).iterator();
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, destination, srcRegion, destRegion);

        WritableRaster raster = destination.getRaster()
                .createWritableChild(destRegion.x, destRegion.y, destRegion.width, destRegion.height, 0, 0, null);

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        // Allow pluggable tone mapper via ImageReadParam
        ToneMapper toneMapper = param instanceof HDRImageReadParam
                                ? ((HDRImageReadParam) param).getToneMapper()
                                : HDRImageReadParam.DEFAULT_TONE_MAPPER;

        byte[] rowRGBE = new byte[width * 4];
        float[] rgb = new float[3];

        processImageStarted(imageIndex);

        // Process one scanline of RGBE data at a time
        for (int srcY = 0; srcY < height; srcY++) {
            int dstY = ((srcY - srcRegion.y) / ySub) + destRegion.y;
            if (dstY >= destRegion.height) {
                break;
            }

            RGBE.readPixelsRawRLE(imageInput, rowRGBE, 0, width, 1);

            if (srcY % ySub == 0 && dstY >= destRegion.y) {
                for (int srcX = srcRegion.x; srcX < srcRegion.x + srcRegion.width; srcX += xSub) {
                    int dstX = ((srcX - srcRegion.x) / xSub) + destRegion.x;
                    if (dstX >= destRegion.width) {
                        break;
                    }

                    RGBE.rgbe2float(rgb, rowRGBE, srcX * 4);

                    // Map/clamp RGB values into visible range, normally [0...1]
                    toneMapper.map(rgb);

                    raster.setDataElements(dstX, dstY, rgb);
                }
            }

            processImageProgress(srcY * 100f / height);

            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }

        processImageComplete();

        return destination;
    }

    @Override
    public boolean canReadRaster() {
        return true;
    }

    @Override
    public Raster readRaster(final int imageIndex, final ImageReadParam param) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);

        Rectangle srcRegion = new Rectangle();
        Rectangle destRegion = new Rectangle();
        computeRegions(param, width, height, null, srcRegion, destRegion);
        destRegion = srcRegion; // We don't really care about destination for raster

        BufferedImage destination = new BufferedImage(srcRegion.width, srcRegion.height, BufferedImage.TYPE_4BYTE_ABGR);
        WritableRaster raster = destination.getRaster();

        int xSub = param != null ? param.getSourceXSubsampling() : 1;
        int ySub = param != null ? param.getSourceYSubsampling() : 1;

        byte[] rowRGBE = new byte[width * 4];
        byte[] pixelRGBE = new byte[width];

        processImageStarted(imageIndex);

        // Process one scanline of RGBE data at a time
        for (int srcY = 0; srcY < height; srcY++) {
            int dstY = ((srcY - srcRegion.y) / ySub) + destRegion.y;
            if (dstY >= destRegion.height) {
                break;
            }

            RGBE.readPixelsRawRLE(imageInput, rowRGBE, 0, width, 1);

            if (srcY % ySub == 0 && dstY >= destRegion.y) {
                for (int srcX = srcRegion.x; srcX < srcRegion.x + srcRegion.width; srcX += xSub) {
                    int dstX = ((srcX - srcRegion.x) / xSub) + destRegion.x;
                    if (dstX >= destRegion.width) {
                        break;
                    }

                    System.arraycopy(rowRGBE, srcX * 4, pixelRGBE, 0, 4);
                    raster.setDataElements(dstX, dstY, pixelRGBE);
                }
            }

            processImageProgress(srcY * 100f / height);

            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }

        processImageComplete();

        return destination.getRaster();
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new HDRImageReadParam();
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        return new HDRMetadata(header);
    }

    public static void main(final String[] args) throws IOException {
        File file = new File(args[0]);

        BufferedImage image = ImageIO.read(file);

        showIt(image, file.getName());
    }
}
