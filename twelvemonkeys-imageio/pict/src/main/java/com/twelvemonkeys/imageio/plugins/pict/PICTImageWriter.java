/*
Copyright (c) 2008, Harald Kuhr
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name "TwelveMonkeys" nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


Parts of this software is based on JVG/JIS.
See http://www.cs.hut.fi/~framling/JVG/index.html for more information.
Redistribution under BSD authorized by Kary Främling:

Copyright (c) 2003, Kary Främling
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the JIS/JVG nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.enc.EncoderStream;
import com.twelvemonkeys.io.enc.PackBitsEncoder;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.*;

/**
 * Writer for Apple Mac Paint Picture (PICT) format.
 * <p/>
 * Images are stored using the "opDirectBitsRect" opcode, which directly
 * stores RGB values (using PackBits run-length encoding).
 *
 * @author <a href="http://www.cs.hut.fi/~framling/JVG/">Kary Främling</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: PICTWriter.java,v 1.0 05.apr.2006 15:20:48 haku Exp$
 */
public class PICTImageWriter extends ImageWriterBase {

    // TODO: Inline these?
    private int mRowBytes;
    private byte[] mScanlineBytes;
    private int mScanWidthLeft;

    public PICTImageWriter() {
        this(null);
    }

    /**
     * Constructs an {@code ImageWriter} and sets its
     * {@code originatingProvider} instance variable to the
     * supplied value.
     * <p/>
     * <p> Subclasses that make use of extensions should provide a
     * constructor with signature {@code (ImageWriterSpi,
     *Object)} in order to retrieve the extension object.  If
     * the extension object is unsuitable, an
     * {@code IllegalArgumentException} should be thrown.
     *
     * @param pProvider the {@code ImageWriterSpi} that
     *                  is constructing this object, or {@code null}.
     */
    protected PICTImageWriter(final ImageWriterSpi pProvider) {
        super(pProvider);
    }

    private void writePICTHeader(RenderedImage pImage) throws IOException {
        // TODO: Make 512 byte header optional
        // Write empty 512-byte header
        byte[] buf = new byte[PICT.PICT_NULL_HEADER_SIZE];
        mImageOutput.write(buf);

        // Write out the size, leave as 0, this is ok
        mImageOutput.writeShort(0);

        // Write image frame (same as image bounds)
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(pImage.getHeight());
        mImageOutput.writeShort(pImage.getWidth());

        // Write version, version 2
        mImageOutput.writeShort(PICT.OP_VERSION);
        mImageOutput.writeShort(PICT.OP_VERSION_2);

        // Version 2 HEADER_OP, extended version.
        mImageOutput.writeShort(PICT.OP_HEADER_OP);
        mImageOutput.writeInt(PICT.HEADER_V2_EXT); // incl 2 bytes reseverd

        // Image resolution, 72 dpi
        mImageOutput.writeShort(PICT.MAC_DEFAULT_DPI);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(PICT.MAC_DEFAULT_DPI);
        mImageOutput.writeShort(0);

        // Optimal source rectangle (same as image bounds)
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(pImage.getHeight());
        mImageOutput.writeShort(pImage.getWidth());

        // Reserved (4 bytes)
        mImageOutput.writeInt(0);

        // TODO: The header really ends here...

        // Highlight
        mImageOutput.writeShort(PICT.OP_DEF_HILITE);

        // Set the clip rectangle
        mImageOutput.writeShort(PICT.OP_CLIP_RGN);
        mImageOutput.writeShort(10);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(pImage.getHeight());
        mImageOutput.writeShort(pImage.getWidth());

        // Pixmap operation
        mImageOutput.writeShort(PICT.OP_DIRECT_BITS_RECT);

        // PixMap pointer (always 0x000000FF);
        mImageOutput.writeInt(0x000000ff);

        // Write rowBytes, this is 4 times the width.
        // Set the high bit, to indicate a PixMap.
        mRowBytes = 4 * pImage.getWidth();
        mImageOutput.writeShort(0x8000 | mRowBytes);

        // Write bounds rectangle (same as image bounds)
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(pImage.getHeight()); // TODO: Handle overflow?
        mImageOutput.writeShort(pImage.getWidth());

        // PixMap record version
        mImageOutput.writeShort(0);

        // Packing format (always 4: PackBits)
        mImageOutput.writeShort(4);

        // Size of packed data (leave as 0)
        mImageOutput.writeInt(0);

        // Pixmap resolution, 72 dpi
        mImageOutput.writeShort(PICT.MAC_DEFAULT_DPI);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(PICT.MAC_DEFAULT_DPI);
        mImageOutput.writeShort(0);

        // Pixel type, 16 is allright for direct pixels
        mImageOutput.writeShort(16);

        // Pixel size
        mImageOutput.writeShort(32);

        // TODO: Allow alpha? Allow 5 bit per pixel component (16 bit)?
        // Pixel component count
        mImageOutput.writeShort(3);

        // Pixel component size
        mImageOutput.writeShort(8);

        // PlaneBytes, ignored for now
        mImageOutput.writeInt(0);

        // TODO: Allow IndexColorModel?
        // ColorTable record (for RGB direct pixels, just write 0)
        mImageOutput.writeInt(0);

        // Reserved (4 bytes)
        mImageOutput.writeInt(0);

        // Source and dest rect (both are same as image bounds)
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(pImage.getHeight());
        mImageOutput.writeShort(pImage.getWidth());

        mImageOutput.writeShort(0);
        mImageOutput.writeShort(0);
        mImageOutput.writeShort(pImage.getHeight());
        mImageOutput.writeShort(pImage.getWidth());

        // Transfer mode
        mImageOutput.writeShort(QuickDraw.SRC_COPY);

        // TODO: Move to writePICTData?
        // TODO: Alpha support
        // Set up the buffers for storing scanline bytes
        mScanlineBytes = new byte[3 * pImage.getWidth()];
        mScanWidthLeft = pImage.getWidth();
    }

    private void writePICTData(int x, int y, int w, int h, ColorModel model,
                               byte[] pixels, int off, int scansize) throws IOException {

        ByteArrayOutputStream bytes = new FastByteArrayOutputStream(mScanlineBytes.length / 2);

        int components = model.getNumComponents();

        // TODO: Clean up, as we only have complete scanlines

        // Fill the scanline buffer. We get problems if ever we have several
        // lines (h > 1) and (w < width). This should never be the case.
        for (int i = 0; i < h; i++) {
            // Reduce the counter of bytes left on the scanline.
            mScanWidthLeft -= w;

            // Treat the scanline.
            for (int j = 0; j < w; j++) {
                if (model instanceof ComponentColorModel && model.getColorSpace().getType() == ColorSpace.TYPE_RGB) {
                    // TODO: Component order?
                    // TODO: Alpha support
                    mScanlineBytes[x + j] = pixels[off + i * scansize * components + components * j + 2];
                    mScanlineBytes[x + w + j] = pixels[off + i * scansize * components + components * j + 1];
                    mScanlineBytes[x + 2 * w + j] = pixels[off + i * scansize * components + components * j];
                }
                else {
                    int rgb = model.getRGB(pixels[off + i * scansize + j] & 0xFF);
                    // Set red, green and blue components.
                    mScanlineBytes[x + j] = (byte) ((rgb >> 16) & 0xFF);
                    mScanlineBytes[x + w + j] = (byte) ((rgb >> 8) & 0xFF);
                    mScanlineBytes[x + 2 * w + j] = (byte) (rgb & 0xFF);
                }

            }

            // If we have a complete scanline, then pack it and write it out.
            if (mScanWidthLeft == 0) {
                // Pack using PackBitsEncoder/EncoderStream
                bytes.reset();
                DataOutput packBits = new DataOutputStream(new EncoderStream(bytes, new PackBitsEncoder(), true));

                packBits.write(mScanlineBytes);

                if (mRowBytes > 250) {
                    mImageOutput.writeShort(bytes.size());
                }
                else {
                    mImageOutput.writeByte(bytes.size());
                }

                bytes.writeTo(IIOUtil.createStreamAdapter(mImageOutput));

                mScanWidthLeft = w;
            }
        }
    }

    private void writePICTData(int x, int y, int w, int h, ColorModel model,
                               int[] pixels, int off, int scansize) throws IOException {

        ByteArrayOutputStream bytes = new FastByteArrayOutputStream(mScanlineBytes.length / 2);

        // TODO: Clean up, as we only have complete scanlines

        // Fill the scanline buffer. We get problems if ever we have several
        // lines (h > 1) and (w < width). This should never be the case.
        for (int i = 0; i < h; i++) {
            // Reduce the counter of bytes left on the scanline.
            mScanWidthLeft -= w;

            // Treat the scanline.
            for (int j = 0; j < w; j++) {
                int rgb = model.getRGB(pixels[off + i * scansize + j]);

                // Set red, green and blue components.
                mScanlineBytes[x + j] = (byte) ((rgb >> 16) & 0xFF);
                mScanlineBytes[x + w + j] = (byte) ((rgb >> 8) & 0xFF);
                mScanlineBytes[x + 2 * w + j] = (byte) (rgb & 0xFF);
            }

            // If we have a complete scanline, then pack it and write it out.
            if (mScanWidthLeft == 0) {
                // Pack using PackBitsEncoder/EncoderStream
                bytes.reset();
                DataOutput packBits = new DataOutputStream(new EncoderStream(bytes, new PackBitsEncoder(), true));

                packBits.write(mScanlineBytes);

                if (mRowBytes > 250) {
                    mImageOutput.writeShort(bytes.size());
                }
                else {
                    mImageOutput.writeByte(bytes.size());
                }

                bytes.writeTo(IIOUtil.createStreamAdapter(mImageOutput));

                mScanWidthLeft = w;
            }
        }
    }

    private void writePICTTrailer() throws IOException {
        // Write out end opcode. Be sure to be word-aligned.
        long length = mImageOutput.length();
        if (length == -1) {
            throw new IIOException("Cannot write trailer without knowing length");
        }

        if ((length & 1) > 0) {
            mImageOutput.writeByte(0);
        }
        mImageOutput.writeShort(PICT.OP_END_OF_PICTURE);
    }

    public void write(IIOMetadata pStreamMetadata, IIOImage pImage, ImageWriteParam pParam) throws IOException {
        assertOutput();

        if (pImage.hasRaster()) {
            throw new UnsupportedOperationException("Cannot write raster");
        }

        processImageStarted(0);

        RenderedImage image = pImage.getRenderedImage();
        writePICTHeader(image);

        // NOTE: getRaster is much faster than getData, as it does no copying
        Raster raster = image instanceof BufferedImage ? ((BufferedImage) image).getRaster() : image.getData();
        DataBuffer buf = raster.getDataBuffer();
        if (buf instanceof DataBufferByte) {
            writePICTData(0, 0, image.getWidth(), image.getHeight(),
                    image.getColorModel(), ((DataBufferByte) buf).getData(),
                    0, image.getWidth());
        }
        else if (buf instanceof DataBufferInt) {
            writePICTData(0, 0, image.getWidth(), image.getHeight(),
                    image.getColorModel(), ((DataBufferInt) buf).getData(),
                    0, image.getWidth());
        }
        else {
            throw new IIOException("DataBuffer type " + buf.getDataType() + " not supported");
        }
        // TODO: Support 16 bit USHORT type

        writePICTTrailer();

        processImageComplete();
    }

    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Method getDefaultImageMetadata not implemented");// TODO: Implement
    }

    public IIOMetadata convertImageMetadata(IIOMetadata inData, ImageTypeSpecifier imageType, ImageWriteParam param) {
        throw new UnsupportedOperationException("Method convertImageMetadata not implemented");// TODO: Implement
    }

    public static void main(String[] pArgs) throws IOException {
        System.out.print("Reading image.. ");

        BufferedImage image = ImageIO.read(new File(pArgs[0]));

        System.out.println("image read");

        System.out.println("image: " + image);

        ImageWriter writer = new PICTImageWriter(null);
        ImageOutputStream stream = ImageIO.createImageOutputStream(new File(pArgs[1]));
        writer.setOutput(stream);
        writer.write(image);
        stream.close();
    }
}
