/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.enc.EncoderStream;
import com.twelvemonkeys.io.enc.PackBitsEncoder;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writer for Commodore Amiga (Electronic Arts) IFF ILBM (InterLeaved BitMap) format.
 * The IFF format (Interchange File Format) is the standard file format
 * supported by almost all image software for the Amiga computer.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: IFFImageWriter.java,v 1.0 02.mar.2006 13:32:30 haku Exp$
 *
 * @see <a href="http://en.wikipedia.org/wiki/Interchange_File_Format">Wikipedia: IFF</a>
 * @see <a href="http://en.wikipedia.org/wiki/ILBM">Wikipedia: IFF ILBM</a>
 */
public class IFFImageWriter extends ImageWriterBase {

    public IFFImageWriter() {
        this(null);
    }

    protected IFFImageWriter(ImageWriterSpi pProvider) {
        super(pProvider);
    }

    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Method getDefaultImageMetadata not implemented");// TODO: Implement
    }

    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Method convertImageMetadata not implemented");// TODO: Implement
    }

    public void write(IIOMetadata pStreamMetadata, IIOImage pImage, ImageWriteParam pParam) throws IOException {
        assertOutput();

        if (pImage.hasRaster()) {
            throw new UnsupportedOperationException("Cannot write raster");
        }

        processImageStarted(0);

        // Prepare image data to be written
        ByteArrayOutputStream imageData = new FastByteArrayOutputStream(1024);
        packImageData(imageData, pImage.getRenderedImage(), pParam);

        //System.out.println("Image data: " + imageData.size());

        // Write metadata
        writeMeta(pImage.getRenderedImage(), imageData.size());

        // Write image data
        writeBody(imageData);

        processImageComplete();
    }

    private void writeBody(ByteArrayOutputStream pImageData) throws IOException {
        imageOutput.writeInt(IFF.CHUNK_BODY);
        imageOutput.writeInt(pImageData.size());

        // NOTE: This is much faster than imageOutput.write(pImageData.toByteArray())
        // as the data array is not duplicated
        OutputStream adapter = IIOUtil.createStreamAdapter(imageOutput);
        try {
            pImageData.writeTo(adapter);
        }
        finally {
            adapter.close();
        }

        if (pImageData.size() % 2 == 0) {
            imageOutput.writeByte(0); // PAD
        }

        imageOutput.flush();
    }

    private void packImageData(OutputStream pOutput, RenderedImage pImage, ImageWriteParam pParam) throws IOException {
        // TODO: Allow param to dictate uncompressed
        // TODO: Allow param to dictate type PBM?
        // TODO: Subsample/AOI
        final boolean compress = shouldCompress(pImage);
        final OutputStream output = compress ? new EncoderStream(pOutput, new PackBitsEncoder(), true) : pOutput;
        final ColorModel model = pImage.getColorModel();
        final Raster raster = pImage.getData();

        final int width = pImage.getWidth();
        final int height = pImage.getHeight();

        // Store each row of pixels
        // 0. Loop pr channel
        // 1. Convert to planar
        // 2. Perform byteRun1 compression for each plane separately
        // 3. Write the plane data for each plane

        //final int planeWidth = (width + 7) / 8;
        final int planeWidth = 2 * ((width + 15) / 16);
        final byte[] planeData = new byte[8 * planeWidth];
        final int channels = (model.getPixelSize() + 7) / 8;
        final int planesPerChannel = channels == 1 ? model.getPixelSize() : 8;
        int[] pixels = new int[8 * planeWidth];

        // TODO: The spec says "Do not compress across rows!".. I think we currently do.
        // NOTE: I'm a little unsure if this is correct for 4 channel (RGBA)
        // data, but it is at least consistent with the IFFImageReader for now...
        for (int y = 0; y < height; y++) {
            for (int c = 0; c < channels; c++) {
                pixels = raster.getSamples(0, y, width, 1, c, pixels);

                int pixelPos = 0;
                int planePos = 0;
                for (int i = 0; i < planeWidth; i++) {
                    IFFUtil.bitRotateCCW(pixels, pixelPos, 1,
                                         planeData, planePos, planeWidth);
                    pixelPos += 8;
                    planePos++;
                }

                for (int p = 0; p < planesPerChannel; p++) {
                    output.write(planeData, p * planeWidth, planeWidth);

                    if (!compress && planeWidth % 2 != 0) {
                        output.write(0); // PAD
                    }
                }
            }

            output.flush();

            processImageProgress(y * 100f / height);
        }

        output.flush();
    }

    private void writeMeta(RenderedImage pImage, int pBodyLength) throws IOException {
        // Annotation ANNO chunk, 8 + annoData.length bytes
        String annotation = "Written by " + getOriginatingProvider().getDescription(null) + " by " + getOriginatingProvider().getVendorName();
        GenericChunk anno = new GenericChunk(IFFUtil.toInt("ANNO".getBytes()), annotation.getBytes());

        ColorModel cm = pImage.getColorModel();
        IndexColorModel icm = null;

        // Bitmap header BMHD chunk, 8 + 20 bytes
        // By default, don't compress narrow images
        int compression = shouldCompress(pImage) ? BMHDChunk.COMPRESSION_BYTE_RUN : BMHDChunk.COMPRESSION_NONE;

        BMHDChunk header;
        if (cm instanceof IndexColorModel) {
            //System.out.println("IndexColorModel");
            icm = (IndexColorModel) cm;
            int trans = icm.getTransparency() == Transparency.BITMASK ? BMHDChunk.MASK_TRANSPARENT_COLOR : BMHDChunk.MASK_NONE;
            int transPixel = icm.getTransparency() == Transparency.BITMASK ? icm.getTransparentPixel() : 0;
            header = new BMHDChunk(pImage.getWidth(), pImage.getHeight(), icm.getPixelSize(),
                                   trans, compression, transPixel);
        }
        else {
            //System.out.println(cm.getClass().getName());
            header = new BMHDChunk(pImage.getWidth(), pImage.getHeight(), cm.getPixelSize(),
                                   BMHDChunk.MASK_NONE, compression, 0);
        }

        // Colormap CMAP chunk, 8 + icm.getMapSize() * 3 bytes (+ 1 optional pad).
        CMAPChunk cmap = null;
        if (icm != null) {
            //System.out.println("CMAP!");
            cmap = new CMAPChunk(icm);
        }

        // ILBM(4) + anno(8+len) + header(8+20) + cmap(8+len)? + body(8+len);
        int size = 4 + 8 + anno.chunkLength + 28 + 8 + pBodyLength;
        if (cmap != null) {
            size += 8 + cmap.chunkLength;
        }

        imageOutput.writeInt(IFF.CHUNK_FORM);
        imageOutput.writeInt(size);

        imageOutput.writeInt(IFF.TYPE_ILBM);

        anno.writeChunk(imageOutput);
        header.writeChunk(imageOutput);

        if (cmap != null) {
            //System.out.println("CMAP written");
            cmap.writeChunk(imageOutput);
        }

    }

    private boolean shouldCompress(RenderedImage pImage) {
        return pImage.getWidth() >= 32;
    }

    public static void main(String[] pArgs) throws IOException {
        BufferedImage image = ImageIO.read(new File(pArgs[0]));

        ImageWriter writer = new IFFImageWriter(new IFFImageWriterSpi());
        writer.setOutput(ImageIO.createImageOutputStream(new File(pArgs[1])));
        //writer.addIIOWriteProgressListener(new ProgressListenerBase() {
        //    int mCurrPct = 0;
        //
        //    public void imageComplete(ImageWriter pSource) {
        //        mCurrPct = 100;
        //        printProgress(mCurrPct);
        //    }
        //
        //    public void imageProgress(ImageWriter pSource, float pPercentageDone) {
        //        if ((int) pPercentageDone > mCurrPct) {
        //            printProgress((int) pPercentageDone);
        //            mCurrPct = (int) pPercentageDone;
        //        }
        //    }
        //
        //    private void printProgress(int pCurrPct) {
        //        if (mCurrPct == 0) {
        //            System.out.print("[");
        //        }
        //        for (int i = mCurrPct / 2; i < pCurrPct / 2; i++) {
        //            System.out.print(".");
        //        }
        //        if (mCurrPct == 100) {
        //            System.out.println("]");
        //        }
        //    }
        //});

        //image = com.twelvemonkeys.image.ImageUtil.toBuffered(image, BufferedImage.TYPE_INT_ARGB);

        writer.write(image);
    }
}
