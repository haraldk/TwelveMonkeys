/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.stream.BufferedImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.IndexedImageTypeSpecifier;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBitsDecoder;

import javax.imageio.*;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Reader for Amiga (Electronic Arts) IFF ILBM (InterLeaved BitMap) and PBM
 * format (Packed BitMap).
 * The IFF format (Interchange File Format) is the standard file format
 * supported by allmost all image software for the Amiga computer.
 * <p/>
 * This reader supports the original palette-based 1-8 bit formats, including
 * EHB (Extra Halfbright), HAM (Hold and Modify), and the more recent "deep"
 * formats, 8 bit gray, 24 bit RGB and 32 bit ARGB.
 * Uncompressed and ByteRun1 compressed (run lenght encoding) files are
 * supported.
 * <p/>
 * Palette based images are read as {@code BufferedImage} of
 * {@link BufferedImage#TYPE_BYTE_INDEXED TYPE_BYTE_INDEXED} or
 * {@link BufferedImage#TYPE_BYTE_BINARY BufferedImage#}
 * depending on the bit depth.
 * Gray images are read as
 * {@link BufferedImage#TYPE_BYTE_GRAY TYPE_BYTE_GRAY}.
 * 24 bit true-color images are read as
 * {@link BufferedImage#TYPE_3BYTE_BGR TYPE_3BYTE_BGR}.
 * 32 bit true-color images are read as
 * {@link BufferedImage#TYPE_4BYTE_ABGR TYPE_4BYTE_ABGR}.
 * <p/>
 * Issues: HAM and HAM8 (Hold and Modify) formats are converted to RGB (24 bit),
 * as it seems to be very hard to create an {@code IndexColorModel} subclass
 * that would correctly describe these formats.
 * These formats utilizes the special display hardware in the Amiga computers.
 * HAM (6 bits) needs 12 bits storage/pixel, if unpacked to RGB (4 bits/gun).
 * HAM8 (8 bits) needs 18 bits storage/pixel, if unpacked to RGB (6 bits/gun).
 * See <a href="http://en.wikipedia.org/wiki/Hold_And_Modify">Wikipedia: HAM</a>
 * for more information.
 * <br/>
 * EHB palette is expanded to an {@link IndexColorModel} with 64 entries.
 * See <a href="http://en.wikipedia.org/wiki/Extra_Half-Brite">Wikipedia: EHB</a>
 * for more information.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: IFFImageReader.java,v 1.0 29.aug.2004 20:26:58 haku Exp $
 * @see <a href="http://en.wikipedia.org/wiki/Interchange_File_Format">Wikipedia: IFF</a>
 * @see <a href="http://en.wikipedia.org/wiki/ILBM">Wikipedia: IFF ILBM</a>
 */
public class IFFImageReader extends ImageReaderBase {
    // http://home.comcast.net/~erniew/lwsdk/docs/filefmts/ilbm.html
    // http://www.fileformat.info/format/iff/spec/7866a9f0e53c42309af667c5da3bd426/view.htm
    //   - Contains definitions of some "new" chunks, as well as alternative FORM types
    // http://amigan.1emu.net/reg/iff.html

    private BMHDChunk mHeader;
    private CMAPChunk mColorMap;
    private BODYChunk mBody;
    private GRABChunk mGrab;
    private CAMGChunk mViewPort;
    private int mFormType;
    private long mBodyStart;

    private BufferedImage mImage;
    private DataInputStream mByteRunStream;

    public IFFImageReader() {
        super(IFFImageReaderSpi.sharedProvider());
    }

    protected IFFImageReader(ImageReaderSpi pProvider) {
        super(pProvider);
    }

    private void init(int pIndex) throws IOException {
        checkBounds(pIndex);

        if (mHeader == null) {
            readMeta();
        }
    }

    protected void resetMembers() {
        mHeader = null;
        mColorMap = null;
        mBody = null;
        mViewPort = null;
        mFormType = 0;

        mImage = null;
        mByteRunStream = null;
    }

    private void readMeta() throws IOException {
        if (imageInput.readInt() != IFF.CHUNK_FORM) {
            throw new IIOException("Unknown file format for IFFImageReader");
        }

        int remaining = imageInput.readInt() - 4; // We'll read 4 more in a sec

        mFormType = imageInput.readInt();
        if (mFormType != IFF.TYPE_ILBM && mFormType != IFF.TYPE_PBM) {
            throw new IIOException("Only IFF (FORM) type ILBM and PBM supported: " + IFFUtil.toChunkStr(mFormType));
        }

        //System.out.println("IFF type FORM " + toChunkStr(type));

        mGrab = null;
        mViewPort = null;

        while (remaining > 0) {
            int chunkId = imageInput.readInt();
            int length = imageInput.readInt();

            remaining -= 8;
            remaining -= length % 2 == 0 ? length : length + 1;

            //System.out.println("Next chunk: " + toChunkStr(chunkId) + " length: " + length);
            //System.out.println("Remaining bytes after chunk: " + remaining);

            switch (chunkId) {
                case IFF.CHUNK_BMHD:
                    if (mHeader != null) {
                        throw new IIOException("Multiple BMHD chunks not allowed");
                    }

                    mHeader = new BMHDChunk(length);
                    mHeader.readChunk(imageInput);

                    //System.out.println(mHeader);
                    break;
                case IFF.CHUNK_CMAP:
                    if (mColorMap != null) {
                        throw new IIOException("Multiple CMAP chunks not allowed");
                    }
                    mColorMap = new CMAPChunk(length, mHeader, mViewPort);
                    mColorMap.readChunk(imageInput);

                    //System.out.println(mColorMap);
                    break;
                case IFF.CHUNK_GRAB:
                    if (mGrab != null) {
                        throw new IIOException("Multiple GRAB chunks not allowed");
                    }
                    mGrab = new GRABChunk(length);
                    mGrab.readChunk(imageInput);

                    //System.out.println(mGrab);
                    break;
                case IFF.CHUNK_CAMG:
                    if (mViewPort != null) {
                        throw new IIOException("Multiple CAMG chunks not allowed");
                    }
                    mViewPort = new CAMGChunk(length);
                    mViewPort.readChunk(imageInput);

                    //System.out.println(mViewPort);
                    break;
                case IFF.CHUNK_BODY:
                    if (mBody != null) {
                        throw new IIOException("Multiple BODY chunks not allowed");
                    }

                    mBody = new BODYChunk(length);
                    mBodyStart = imageInput.getStreamPosition();

                    // NOTE: We don't read the body here, it's done later in the read(int, ImageReadParam) method

                    // Done reading meta
                    return;
                default:
                    // TODO: We probably want to store anno chunks as Metadata
                    // ANNO, DEST, SPRT and more
                    IFFChunk generic = new GenericChunk(chunkId, length);
                    generic.readChunk(imageInput);

                    //System.out.println(generic);
                    break;
            }
        }
    }

    public BufferedImage read(int pIndex, ImageReadParam pParam) throws IOException {
        init(pIndex);

        processImageStarted(pIndex);

        mImage = getDestination(pParam, getImageTypes(pIndex), mHeader.mWidth, mHeader.mHeight);
        //System.out.println(mBody);
        if (mBody != null) {
            //System.out.println("Read body");
            readBody(pParam);
        }
        else {
            // TODO: Remove this hack when we have metadata
            // In the rare case of an ILBM containing nothing but a CMAP
            //System.out.println(mColorMap);
            if (mColorMap != null) {
                //System.out.println("Creating palette!");
                mImage = mColorMap.createPaletteImage();
            }
        }

        BufferedImage result = mImage;

        processImageComplete();

        return result;
    }

    public int getWidth(int pIndex) throws IOException {
        init(pIndex);
        return mHeader.mWidth;
    }

    public int getHeight(int pIndex) throws IOException {
        init(pIndex);
        return mHeader.mHeight;
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(int pIndex) throws IOException {
        init(pIndex);

        List<ImageTypeSpecifier> types = Arrays.asList(
                getRawImageType(pIndex),
                ImageTypeSpecifier.createFromBufferedImageType(mHeader.mBitplanes == 32 ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR)
// TODO:               ImageTypeSpecifier.createFromBufferedImageType(mHeader.mBitplanes == 32 ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB),
                // TODO: Allow 32 bit always. Allow RGB and discard alpha, if present?
        );
        return types.iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int pIndex) throws IOException {
        init(pIndex);
        // TODO: Stay DRY...
        // TODO: Use this for creating the Image/Buffer in the read code below...
        // NOTE: mColorMap may be null for 8 bit (gray), 24 bit or 32 bit only
        ImageTypeSpecifier specifier;
        switch (mHeader.mBitplanes) {
            case 1:
                // 1 bit
            case 2:
                // 2 bit
            case 3:
            case 4:
                // 4 bit
            case 5:
            case 6:
                // May be HAM6
                // May be EHB
            case 7:
            case 8:
                // 8 bit
                // May be HAM8
                if (!isHAM()) {
                    if (mColorMap != null) {
                        IndexColorModel cm = mColorMap.getIndexColorModel();
                        specifier = IndexedImageTypeSpecifier.createFromIndexColorModel(cm);
                        break;
                    }
                    else {
                        specifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
                        break;
                    }
                }
                // NOTE: HAM modes falls through, as they are converted to RGB
            case 24:
                // 24 bit RGB
                specifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);
                break;
            case 32:
                // 32 bit ARGB
                specifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR);
                break;
            default:
                throw new IIOException(String.format("Bit depth not implemented: %d", mHeader.mBitplanes));
        }
        return specifier;
    }

    private void readBody(final ImageReadParam pParam) throws IOException {
        imageInput.seek(mBodyStart);
        mByteRunStream = null;

        // NOTE: mColorMap may be null for 8 bit (gray), 24 bit or 32 bit only
        if (mColorMap != null) {
            IndexColorModel cm = mColorMap.getIndexColorModel();
            readIndexed(pParam, imageInput, cm);
        }
        else {
            readTrueColor(pParam, imageInput);
        }

    }

    private void readIndexed(final ImageReadParam pParam, final ImageInputStream pInput, final IndexColorModel pModel) throws IOException {
        final int width = mHeader.mWidth;
        final int height = mHeader.mHeight;

        final Rectangle aoi = getSourceRegion(pParam, width, height);
        final Point offset = pParam == null ? new Point(0, 0) : pParam.getDestinationOffset();

        // Set everything to default values
        int sourceXSubsampling = 1;
        int sourceYSubsampling = 1;
        int[] sourceBands = null;
        int[] destinationBands = null;

        // Get values from the ImageReadParam, if any
        if (pParam != null) {
            sourceXSubsampling = pParam.getSourceXSubsampling();
            sourceYSubsampling = pParam.getSourceYSubsampling();

            sourceBands = pParam.getSourceBands();
            destinationBands = pParam.getDestinationBands();
        }

        // Ensure band settings from param are compatible with images
        checkReadParamBandSettings(pParam, isHAM() ? 3 : 1, mImage.getSampleModel().getNumBands());

        WritableRaster destination = mImage.getRaster();
        if (destinationBands != null || offset.x != 0 || offset.y != 0) {
            destination = destination.createWritableChild(0, 0, destination.getWidth(), destination.getHeight(), offset.x, offset.y, destinationBands);
        }

        // NOTE:  Each row of the image is stored in an integral number of 16 bit words.
        // The number of words per row is words=((w+15)/16)
        int planeWidth = 2 * ((width + 15) / 16);
        final byte[] planeData = new byte[8 * planeWidth];

        ColorModel cm;
        WritableRaster raster;

        if (isHAM()) {
            // TODO: If HAM6, use type USHORT_444_RGB or 2BYTE_444_RGB?
            // Or create a HAMColorModel, if at all possible?
            // TYPE_3BYTE_BGR
            cm = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8},
                    false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
            );
            // Create a byte raster with BGR order
            raster = Raster.createInterleavedRaster(
                    DataBuffer.TYPE_BYTE, width, 1, width * 3, 3, new int[]{2, 1, 0}, null
            );
        }
        else {
            // TYPE_BYTE_BINARY or TYPE_BYTE_INDEXED
            cm = pModel;
            raster = pModel.createCompatibleWritableRaster(width, 1);
        }
        Raster sourceRow = raster.createChild(aoi.x, 0, aoi.width, 1, 0, 0, sourceBands);

        final byte[] row = new byte[width * 8];

        //System.out.println("Data length: " + data.length);
        //System.out.println("PlaneData length: " + planeData.length * planeData[0].length);
        //System.out.println("Row length: " + row.length);

        final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();

        final int planes = mHeader.mBitplanes;

        Object dataElements = null;
        Object outDataElements = null;
        ColorConvertOp converter = null;

        for (int srcY = 0; srcY < height; srcY++) {
            for (int p = 0; p < planes; p++) {
                readPlaneData(pInput, planeData, p * planeWidth, planeWidth);
            }

            // Skip rows outside AOI
            if (srcY < aoi.y || (srcY - aoi.y) % sourceYSubsampling != 0) {
                continue;
            }
            else if (srcY >= (aoi.y + aoi.height)) {
                return;
            }

            if (mFormType == IFF.TYPE_ILBM) {
                int pixelPos = 0;
                for (int planePos = 0; planePos < planeWidth; planePos++) {
                    IFFUtil.bitRotateCW(planeData, planePos, planeWidth, row, pixelPos, 1);
                    pixelPos += 8;
                }

                if (isHAM()) {
                    hamToRGB(row, pModel, data, 0);
                }
                else {
                    raster.setDataElements(0, 0, width, 1, row);
                }
            }
            else if (mFormType == IFF.TYPE_PBM) {
                // TODO: Arraycopy might not be necessary, if it's okay with row larger than width
                System.arraycopy(planeData, 0, row, 0, mHeader.mBitplanes * planeWidth);
                raster.setDataElements(0, 0, width, 1, row);
            }
            else {
                throw new AssertionError(String.format("Unsupported FORM type: %s", mFormType));
            }

            int dstY = (srcY - aoi.y) / sourceYSubsampling;
            // Handle non-converting raster as special case for performance
            if (cm.isCompatibleRaster(destination)) {
                // Rasters are compatible, just write to destinaiton
                if (sourceXSubsampling == 1) {
                    destination.setRect(offset.x, dstY, sourceRow);
//                    dataElements = raster.getDataElements(aoi.x, 0, aoi.width, 1, dataElements);
//                    destination.setDataElements(offset.x, offset.y + (srcY - aoi.y) / sourceYSubsampling, aoi.width, 1, dataElements);
                }
                else {
                    for (int srcX = 0; srcX < sourceRow.getWidth(); srcX += sourceXSubsampling) {
                        dataElements = sourceRow.getDataElements(srcX, 0, dataElements);
                        int dstX = /*offset.x +*/ srcX / sourceXSubsampling;
                        destination.setDataElements(dstX, dstY, dataElements);
                    }
                }
            }
            else {
                if (cm instanceof IndexColorModel) {
                    // TODO: Optimize this thing... Maybe it's faster to just get the data indexed, and use drawImage?
                    IndexColorModel icm = (IndexColorModel) cm;

                    for (int srcX = 0; srcX < sourceRow.getWidth(); srcX += sourceXSubsampling) {
                        dataElements = sourceRow.getDataElements(srcX, 0, dataElements);
                        int rgb = icm.getRGB(dataElements);
                        outDataElements = mImage.getColorModel().getDataElements(rgb, outDataElements);
                        int dstX = srcX / sourceXSubsampling;
                        destination.setDataElements(dstX, dstY, outDataElements);
                    }
                }
                else {
                    // TODO: This branch is never tested, and is probably "dead"
                    // ColorConvertOp
                    if (converter == null) {
                        converter = new ColorConvertOp(cm.getColorSpace(), mImage.getColorModel().getColorSpace(), null);
                    }
                    converter.filter(
                            raster.createChild(aoi.x, 0, aoi.width, 1, 0, 0, null),
                            destination.createWritableChild(offset.x, offset.y + srcY - aoi.y, aoi.width, 1, 0, 0, null)
                    );
                }
            }

            processImageProgress(srcY * 100f / mHeader.mWidth);
            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }
    }

    // One row from each of the 24 bitplanes is written before moving to the
    // next scanline. For each scanline, the red bitplane rows are stored first,
    // followed by green and blue. The first plane holds the least significant
    // bit of the red value for each pixel, and the last holds the most
    // significant bit of the blue value.
    private void readTrueColor(ImageReadParam pParam, final ImageInputStream pInput) throws IOException {
        final int width = mHeader.mWidth;
        final int height = mHeader.mHeight;

        final Rectangle aoi = getSourceRegion(pParam, width, height);
        final Point offset = pParam == null ? new Point(0, 0) : pParam.getDestinationOffset();

        // Set everything to default values
        int sourceXSubsampling = 1;
        int sourceYSubsampling = 1;
        int[] sourceBands = null;
        int[] destinationBands = null;

        // Get values from the ImageReadParam, if any
        if (pParam != null) {
            sourceXSubsampling = pParam.getSourceXSubsampling();
            sourceYSubsampling = pParam.getSourceYSubsampling();

            sourceBands = pParam.getSourceBands();
            destinationBands = pParam.getDestinationBands();
        }

        // Ensure band settings from param are compatible with images
        checkReadParamBandSettings(pParam, mHeader.mBitplanes / 8, mImage.getSampleModel().getNumBands());

        // NOTE:  Each row of the image is stored in an integral number of 16 bit words.
        // The number of words per row is words=((w+15)/16)
        int planeWidth = 2 * ((width + 15) / 16);
        final byte[] planeData = new byte[8 * planeWidth];

        WritableRaster destination = mImage.getRaster();
        if (destinationBands != null || offset.x != 0 || offset.y != 0) {
            destination = destination.createWritableChild(0, 0, destination.getWidth(), destination.getHeight(), offset.x, offset.y, destinationBands);
        }
//        WritableRaster raster = mImage.getRaster().createCompatibleWritableRaster(width, 1);
        WritableRaster raster = mImage.getRaster().createCompatibleWritableRaster(8 * planeWidth, 1);
        Raster sourceRow = raster.createChild(aoi.x, 0, aoi.width, 1, 0, 0, sourceBands);

        final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();
        final int channels = (mHeader.mBitplanes + 7) / 8;
        final int planesPerChannel = 8;
        Object dataElements = null;

        for (int srcY = 0; srcY < height; srcY++) {
            for (int c = 0; c < channels; c++) {

                for (int p = 0; p < planesPerChannel; p++) {
                    readPlaneData(pInput, planeData, p * planeWidth, planeWidth);
                }

                // Skip rows outside AOI
                if (srcY < aoi.y || (srcY - aoi.y) % sourceYSubsampling != 0) {
                    continue;
                }
                else if (srcY >= (aoi.y + aoi.height)) {
                    return;
                }

                if (mFormType == IFF.TYPE_ILBM) {
                    // NOTE: Using (channels - c - 1) instead of just c,
                    // effectively reverses the channel order from RGBA to ABGR
                    int off = (channels - c - 1);

                    int pixelPos = 0;
                    for (int planePos = 0; planePos < planeWidth; planePos++) {
                        IFFUtil.bitRotateCW(planeData, planePos, planeWidth, data, off + pixelPos * channels, channels);
                        pixelPos += 8;
                    }
                }
                else if (mFormType == IFF.TYPE_PBM) {
                    System.arraycopy(planeData, 0, data, srcY * 8 * planeWidth, planeWidth);
                }
                else {
                    throw new AssertionError(String.format("Unsupported FORM type: %s", mFormType));
                }
            }

            int dstY = (srcY - aoi.y) / sourceYSubsampling;
            // TODO: Support conversion to INT (A)RGB rasters (maybe using ColorConvertOp?)
            // TODO: Avoid createChild if no region?
            if (sourceXSubsampling == 1) {
                destination.setRect(0, dstY, sourceRow);
//                dataElements = raster.getDataElements(aoi.x, 0, aoi.width, 1, dataElements);
//                destination.setDataElements(offset.x, offset.y + (srcY - aoi.y) / sourceYSubsampling, aoi.width, 1, dataElements);
            }
            else {
                for (int srcX = 0; srcX < sourceRow.getWidth(); srcX += sourceXSubsampling) {
                    dataElements = sourceRow.getDataElements(srcX, 0, dataElements);
                    int dstX = srcX / sourceXSubsampling;
                    destination.setDataElements(dstX, dstY, dataElements);
                }
            }

            processImageProgress(srcY * 100f / mHeader.mWidth);
            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }
    }

    private void readPlaneData(final ImageInputStream pInput, final byte[] pData, final int pOffset, final int pPlaneWidth)
            throws IOException {

        switch (mHeader.mCompressionType) {
            case BMHDChunk.COMPRESSION_NONE:
                pInput.readFully(pData, pOffset, pPlaneWidth);
                // Uncompressed rows must have even number of bytes
                if ((mHeader.mBitplanes * pPlaneWidth) % 2 != 0) {
                    pInput.readByte();
                }
                break;

            case BMHDChunk.COMPRESSION_BYTE_RUN:
                // TODO: How do we know if the last byte in the body is a pad byte or not?!
                // The body consists of byte-run (PackBits) compressed rows of bit plane data.
                // However, we don't know how long each compressed row is, without decoding it...
                // The workaround below, is to use a decode buffer size of pPlaneWidth,
                // to make sure we don't decode anything we don't have to (shouldn't).
                if (mByteRunStream == null) {
                    mByteRunStream = new DataInputStream(
                            new DecoderStream(
                                    IIOUtil.createStreamAdapter(pInput, mBody.mChunkLength),
                                    new PackBitsDecoder(true),
                                    pPlaneWidth * mHeader.mBitplanes
                            )
                    );
                }
                mByteRunStream.readFully(pData, pOffset, pPlaneWidth);
                break;

            default:
                throw new IIOException(String.format("Unknown compression type: %d", mHeader.mCompressionType));
        }
    }

    private void hamToRGB(final byte[] pIndexed, final IndexColorModel pModel,
                          final byte[] pDest, final int pDestOffset) {
        final int bits = mHeader.mBitplanes;
        final int width = mHeader.mWidth;
        int lastRed = 0;
        int lastGreen = 0;
        int lastBlue = 0;

        for (int x = 0; x < width; x++) {
            int pixel = pIndexed[x] & 0xff;

            //System.out.println("--> ham" + bits);
            int paletteIndex = bits == 6 ? pixel & 0x0f : pixel & 0x3f;
            int indexShift = bits == 6 ? 4 : 2;
            int colorMask = bits == 6 ? 0x0f : 0x03;
            //System.out.println("palette index=" + paletteIndex);

            // Get Hold and Modify bits
            switch ((pixel >> (8 - indexShift)) & 0x03) {
                case 0x00:// HOLD
                    lastRed = pModel.getRed(paletteIndex);
                    lastGreen = pModel.getGreen(paletteIndex);
                    lastBlue = pModel.getBlue(paletteIndex);
                    break;
                case 0x01:// MODIFY BLUE
                    lastBlue = (lastBlue & colorMask) | (paletteIndex << indexShift);
                    break;
                case 0x02:// MODIFY RED
                    lastRed = (lastRed & colorMask) | (paletteIndex << indexShift);
                    break;
                case 0x03:// MODIFY GREEN
                    lastGreen = (lastGreen & colorMask) | (paletteIndex << indexShift);
                    break;
            }
            int offset = (x * 3) + pDestOffset;
            pDest[2 + offset] = (byte) lastRed;
            pDest[1 + offset] = (byte) lastGreen;
            pDest[offset] = (byte) lastBlue;
        }
    }

    private boolean isHAM() {
        return mViewPort != null && mViewPort.isHAM();
    }

    public static void main(String[] pArgs) throws IOException {
        ImageReader reader = new IFFImageReader();

//        ImageInputStream input = ImageIO.createImageInputStream(new File(pArgs[0]));
        ImageInputStream input = new BufferedImageInputStream(ImageIO.createImageInputStream(new File(pArgs[0])));
        boolean canRead = reader.getOriginatingProvider().canDecodeInput(input);

        System.out.println("Can read: " + canRead);

        if (canRead) {
            reader.setInput(input);
            ImageReadParam param = reader.getDefaultReadParam();
//            param.setSourceRegion(new Rectangle(0, 0, 160, 200));
//            param.setSourceRegion(new Rectangle(160, 200, 160, 200));
//            param.setSourceRegion(new Rectangle(80, 100, 160, 200));
//            param.setDestinationOffset(new Point(80, 100));
//            param.setSourceSubsampling(3, 3, 0, 0);
//            param.setSourceBands(new int[]{0, 1, 2});
//            param.setDestinationBands(new int[]{1, 0, 2});
            BufferedImage image = reader.read(0, param);
            System.out.println("image = " + image);

            showIt(image, pArgs[0]);
        }
    }
}
