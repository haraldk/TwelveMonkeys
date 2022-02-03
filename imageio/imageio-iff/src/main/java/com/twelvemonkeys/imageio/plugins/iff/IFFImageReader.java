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

import com.twelvemonkeys.image.ResampleOp;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.io.enc.DecoderStream;
import com.twelvemonkeys.io.enc.PackBitsDecoder;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
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

import static com.twelvemonkeys.imageio.plugins.iff.IFFUtil.toChunkStr;

/**
 * Reader for Commodore Amiga (Electronic Arts) IFF ILBM (InterLeaved BitMap) and PBM
 * format (Packed BitMap). Also supports IFF RGB8 (Impulse) and IFF DEEP (TVPaint).
 * The IFF format (Interchange File Format) is the standard file format
 * supported by allmost all image software for the Amiga computer.
 * <p>
 * This reader supports the original palette-based 1-8 bit formats, including
 * EHB (Extra Half-Bright), HAM (Hold and Modify), and the more recent "deep"
 * formats, 8 bit gray, 24 bit RGB and 32 bit ARGB.
 * Uncompressed and ByteRun1 compressed (run length encoding) files are
 * supported.
 * </p>
 * <p>
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
 * </p>
 * <p>
 * Issues: HAM and HAM8 (Hold and Modify) formats are converted to RGB (24 bit),
 * as it seems to be very hard to create an {@code IndexColorModel} subclass
 * that would correctly describe these formats.
 * These formats utilizes the special display hardware in the Amiga computers.
 * HAM (6 bits) needs 12 bits storage/pixel, if unpacked to RGB (4 bits/gun).
 * HAM8 (8 bits) needs 18 bits storage/pixel, if unpacked to RGB (6 bits/gun).
 * See <a href="http://en.wikipedia.org/wiki/Hold_And_Modify">Wikipedia: HAM</a>
 * for more information.
 * <br>
 * EHB palette is expanded to an {@link IndexColorModel} with 64 entries.
 * See <a href="http://en.wikipedia.org/wiki/Extra_Half-Brite">Wikipedia: EHB</a>
 * for more information.
 * </p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: IFFImageReader.java,v 1.0 29.aug.2004 20:26:58 haku Exp $
 * @see <a href="http://en.wikipedia.org/wiki/Interchange_File_Format">Wikipedia: IFF</a>
 * @see <a href="http://en.wikipedia.org/wiki/ILBM">Wikipedia: IFF ILBM</a>
 */
public final class IFFImageReader extends ImageReaderBase {
    // http://home.comcast.net/~erniew/lwsdk/docs/filefmts/ilbm.html
    // http://www.fileformat.info/format/iff/spec/7866a9f0e53c42309af667c5da3bd426/view.htm
    //   - Contains definitions of some "new" chunks, as well as alternative FORM types
    // http://amigan.1emu.net/index/iff.html

    // TODO: XS24 chunk seems to be a raw 24 bit thumbnail for TVPaint images: XS24 <4 byte len> <2 byte width> <2 byte height> <pixel data...>
    // TODO: Allow reading rasters for HAM6/HAM8 and multipalette images that are expanded to RGB (24 bit) during read.

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.iff.debug"));

    private Form header;
    private DataInputStream byteRunStream;

    IFFImageReader(ImageReaderSpi pProvider) {
        super(pProvider);
    }

    private void init(int pIndex) throws IOException {
        checkBounds(pIndex);

        if (header == null) {
            readMeta();
        }
    }

    @Override
    protected void resetMembers() {
        header = null;
        byteRunStream = null;
    }

    private void readMeta() throws IOException {
        int chunkType = imageInput.readInt();
        if (chunkType != IFF.CHUNK_FORM) {
            throw new IIOException(String.format("Unknown file format for IFFImageReader, expected 'FORM': %s", toChunkStr(chunkType)));
        }

        int remaining = imageInput.readInt() - 4; // We'll read 4 more in a sec

        int formType = imageInput.readInt();
        if (formType != IFF.TYPE_ILBM && formType != IFF.TYPE_PBM && formType != IFF.TYPE_RGB8 && formType != IFF.TYPE_DEEP && formType != IFF.TYPE_TVPP) {
            throw new IIOException(String.format("Only IFF FORM types 'ILBM' and 'PBM ' supported: %s", toChunkStr(formType)));
        }

        if (DEBUG) {
            System.out.println("IFF type FORM '" + toChunkStr(formType) + "', len: " + (remaining + 4));
            System.out.println("Reading Chunks...");
        }

        header = Form.ofType(formType);

        // TODO: Delegate the FORM reading to the Form class or a FormReader class?
        while (remaining > 0) {
            int chunkId = imageInput.readInt();
            int length = imageInput.readInt();

            remaining -= 8;
            remaining -= length % 2 == 0 ? length : length + 1;

            if (DEBUG) {
                System.out.println("Next chunk: " + toChunkStr(chunkId) + " @ pos: " + (imageInput.getStreamPosition() - 8) + ", len: " + length);
                System.out.println("Remaining bytes after chunk: " + remaining);
            }

            switch (chunkId) {
                case IFF.CHUNK_BMHD:
                    BMHDChunk bitmapHeader = new BMHDChunk(length);
                    bitmapHeader.readChunk(imageInput);
                    header = header.with(bitmapHeader);
                    break;

                case IFF.CHUNK_DGBL:
                    DGBLChunk deepGlobal = new DGBLChunk(length);
                    deepGlobal.readChunk(imageInput);
                    header = header.with(deepGlobal);
                    break;

                case IFF.CHUNK_DLOC:
                    DLOCChunk deepLocation = new DLOCChunk(length);
                    deepLocation.readChunk(imageInput);
                    header = header.with(deepLocation);
                    break;

                case IFF.CHUNK_DPEL:
                    DPELChunk deepPixel = new DPELChunk(length);
                    deepPixel.readChunk(imageInput);
                    header = header.with(deepPixel);
                    break;

                case IFF.CHUNK_CMAP:
                    CMAPChunk colorMap = new CMAPChunk(length);
                    colorMap.readChunk(imageInput);
                    header = header.with(colorMap);
                    break;

                case IFF.CHUNK_GRAB:
                    GRABChunk grab = new GRABChunk(length);
                    grab.readChunk(imageInput);
                    header = header.with(grab);
                    break;

                case IFF.CHUNK_CAMG:
                    CAMGChunk viewMode = new CAMGChunk(length);
                    viewMode.readChunk(imageInput);
                    header = header.with(viewMode);
                    break;

                case IFF.CHUNK_PCHG:
                    PCHGChunk pchg = new PCHGChunk(length);
                    pchg.readChunk(imageInput);
                    header = header.with(pchg);
                    break;

                case IFF.CHUNK_SHAM:
                    SHAMChunk sham = new SHAMChunk(length);
                    sham.readChunk(imageInput);
                    header = header.with(sham);
                    break;

                case IFF.CHUNK_CTBL:
                    CTBLChunk ctbl = new CTBLChunk(length);
                    ctbl.readChunk(imageInput);
                    header = header.with(ctbl);
                    break;

                case IFF.CHUNK_BODY:
                case IFF.CHUNK_DBOD:
                    BODYChunk body = new BODYChunk(chunkId, length, imageInput.getStreamPosition());
                    // NOTE: We don't read the body here, it's done later in the read(int, ImageReadParam) method
                    header = header.with(body);

                    // Done reading meta
                    if (DEBUG) {
                        System.out.println("header = " + header);
                    }
                    return;

                case IFF.CHUNK_ANNO:
                case IFF.CHUNK_AUTH:
                case IFF.CHUNK_COPY:
                case IFF.CHUNK_NAME:
                case IFF.CHUNK_TEXT:
                case IFF.CHUNK_UTF8:
                    GenericChunk generic = new GenericChunk(chunkId, length);
                    generic.readChunk(imageInput);
                    header = header.with(generic);
                    break;

                case IFF.CHUNK_JUNK:
                    // Always skip junk chunks
                default:
                    // TODO: DEST, SPRT and more
                    // Everything else, we'll just skip
                    IFFChunk.skipData(imageInput, length, 0);
                    break;
            }
        }

        if (DEBUG) {
            System.out.println("header = " + header);
            System.out.println("No BODY chunk found...");
        }
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        init(imageIndex);
        processImageStarted(imageIndex);

        BufferedImage result = getDestination(param, getImageTypes(imageIndex), getWidth(imageIndex), getHeight(imageIndex));
        readBody(param, result);

        processImageComplete();

        return result;
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        init(imageIndex);
        return header.width();
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        init(imageIndex);
        return header.height();
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        init(imageIndex);

        return new IFFImageMetadata(header, header.colorMap());
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        init(imageIndex);

        int bitplanes = header.bitplanes();
        List<ImageTypeSpecifier> types =
                header.formType == IFF.TYPE_DEEP || header.formType == IFF.TYPE_TVPP // TODO: Make a header attribute here
                ? Arrays.asList(
                        ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE),
                        getRawImageType(imageIndex)
                )
                : Arrays.asList(
                        getRawImageType(imageIndex),
                        ImageTypeSpecifiers.createFromBufferedImageType(bitplanes == 32 ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR)
                );
        // TODO: Allow 32 bit INT types?
        return types.iterator();
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int pIndex) throws IOException {
        init(pIndex);

        // NOTE: colorMap may be null for 8 bit (gray), 24 bit or 32 bit only
        switch (header.bitplanes()) {
            case 1:
                // -> 1 bit IndexColorModel
            case 2:
                // -> 2 bit IndexColorModel
            case 3:
            case 4:
                // -> 4 bit IndexColorModel
            case 5:
            case 6:
                // May be EHB or HAM6
            case 7:
            case 8:
                // May be HAM8
                // otherwise -> 8 bit IndexColorModel
                if (!needsConversionToRGB()) {
                    IndexColorModel indexColorModel = header.colorMap();

                    if (indexColorModel != null) {
                        return ImageTypeSpecifiers.createFromIndexColorModel(indexColorModel);
                    }

                    return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
                }
                // NOTE: HAM modes falls through, as they are converted to RGB
            case 24:
                // 24 bit RGB
                return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);

            case 25:
                // For TYPE_RGB8: 24 bit + 1 bit mask (we'll convert to full alpha during decoding)
                if (header.formType != IFF.TYPE_RGB8) {
                    throw new IIOException(String.format("25 bit depth only supported for FORM type RGB8: %s", toChunkStr(header.formType)));
                }

                return ImageTypeSpecifiers.createInterleaved(ColorSpaces.getColorSpace(ColorSpace.CS_sRGB),
                        new int[] {0, 1, 2, 3}, DataBuffer.TYPE_BYTE, true, false);

            case 32:
                // 32 bit ARGB
                return header.formType == IFF.TYPE_DEEP || header.formType == IFF.TYPE_TVPP
                       //                                                                                                R  G  B  A
                       ? ImageTypeSpecifiers.createInterleaved(ColorSpaces.getColorSpace(ColorSpace.CS_sRGB), new int[] {1, 2, 3, 0}, DataBuffer.TYPE_BYTE, true, header.premultiplied()) // TODO: Create based on DPEL!
                       : ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR);

            default:
                throw new IIOException(String.format("Bit depth not implemented: %d", header.bitplanes()));
        }
    }

    private boolean needsConversionToRGB() {
        return header.isHAM() || header.isMultiPalette();
    }

    private void readBody(final ImageReadParam param, final BufferedImage destination) throws IOException {
        if (DEBUG) {
            System.out.println("Reading body");
            System.out.println("pos: " + imageInput.getStreamPosition());
            System.out.println("body offset: " + header.bodyOffset());
        }

        imageInput.seek(header.bodyOffset());
        byteRunStream = null;

        if (header.formType == IFF.TYPE_RGB8 || header.formType == IFF.TYPE_DEEP || header.formType == IFF.TYPE_TVPP) {
            readChunky(param, destination, imageInput);
        }
        else if (header.colorMap() != null) {
            // NOTE: For ILBM types, colorMap may be null for 8 bit (gray), 24 bit or 32 bit only
            IndexColorModel palette = header.colorMap();
            readInterleavedIndexed(param, destination, palette, imageInput);
        }
        else {
            readInterleaved(param, destination, imageInput);
        }
    }

    private void readInterleavedIndexed(final ImageReadParam param, final BufferedImage destination, final IndexColorModel palette, final ImageInputStream input) throws IOException {
        final int width = header.width();
        final int height = header.height();

        final Rectangle aoi = getSourceRegion(param, width, height);
        final Point offset = param == null ? new Point(0, 0) : param.getDestinationOffset();

        // Set everything to default values
        int sourceXSubsampling = 1;
        int sourceYSubsampling = 1;
        int[] sourceBands = null;
        int[] destinationBands = null;

        // Get values from the ImageReadParam, if any
        if (param != null) {
            sourceXSubsampling = param.getSourceXSubsampling();
            sourceYSubsampling = param.getSourceYSubsampling();

            sourceBands = param.getSourceBands();
            destinationBands = param.getDestinationBands();
        }

        // Ensure band settings from param are compatible with images
        checkReadParamBandSettings(param, needsConversionToRGB() ? 3 : 1, destination.getSampleModel().getNumBands());

        WritableRaster destRaster = destination.getRaster();
        if (destinationBands != null || offset.x != 0 || offset.y != 0) {
            destRaster = destRaster.createWritableChild(0, 0, destRaster.getWidth(), destRaster.getHeight(), offset.x, offset.y, destinationBands);
        }

        // NOTE:  Each row of the image is stored in an integral number of 16 bit words.
        // The number of words per row is words=((w+15)/16)
        int planeWidth = 2 * ((width + 15) / 16);
        final byte[] planeData = new byte[8 * planeWidth];

        ColorModel cm;
        WritableRaster rowRaster;

        if (needsConversionToRGB()) {
            // TODO: Create a HAMColorModel, if at all possible?
            // TYPE_3BYTE_BGR
            cm = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8},
                    false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
            );
            // Create a byte raster with BGR order
            rowRaster = Raster.createInterleavedRaster(
                    DataBuffer.TYPE_BYTE, width, 1, width * 3, 3, new int[] {2, 1, 0}, null
            );
        }
        else {
            // TYPE_BYTE_BINARY or TYPE_BYTE_INDEXED
            cm = palette;
            rowRaster = palette.createCompatibleWritableRaster(width, 1);
        }

        Raster sourceRow = rowRaster.createChild(aoi.x, 0, aoi.width, 1, 0, 0, sourceBands);

        final byte[] row = new byte[width * 8];
        final byte[] data = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
        final int planes = header.bitplanes();

        Object dataElements = null;
        Object outDataElements = null;
        ColorConvertOp converter = null;

        for (int srcY = 0; srcY < height; srcY++) {
            for (int p = 0; p < planes; p++) {
                readPlaneData(planeData, p * planeWidth, planeWidth, input);
            }

            // Skip rows outside AOI
            if (srcY < aoi.y || (srcY - aoi.y) % sourceYSubsampling != 0) {
                continue;
            }
            else if (srcY >= (aoi.y + aoi.height)) {
                return;
            }

            if (header.formType == IFF.TYPE_ILBM) {
                int pixelPos = 0;
                for (int planePos = 0; planePos < planeWidth; planePos++) {
                    IFFUtil.bitRotateCW(planeData, planePos, planeWidth, row, pixelPos, 1);
                    pixelPos += 8;
                }

                if (header.isHAM()) {
                    hamToRGB(row, palette, data, 0);
                }
                else if (needsConversionToRGB()) {
                    multiPaletteToRGB(srcY, row, palette, data, 0);
                }
                else {
                    rowRaster.setDataElements(0, 0, width, 1, row);
                }
            }
            else if (header.formType == IFF.TYPE_PBM) {
                rowRaster.setDataElements(0, 0, width, 1, planeData);
            }
            else {
                throw new AssertionError(String.format("Unsupported FORM type: %s", toChunkStr(header.formType)));
            }

            int dstY = (srcY - aoi.y) / sourceYSubsampling;
            // Handle non-converting raster as special case for performance
            if (cm.isCompatibleRaster(destRaster)) {
                // Rasters are compatible, just write to destination
                if (sourceXSubsampling == 1) {
                    destRaster.setRect(offset.x, dstY, sourceRow);
                }
                else {
                    for (int srcX = 0; srcX < sourceRow.getWidth(); srcX += sourceXSubsampling) {
                        dataElements = sourceRow.getDataElements(srcX, 0, dataElements);
                        int dstX = /*offset.x +*/ srcX / sourceXSubsampling;
                        destRaster.setDataElements(dstX, dstY, dataElements);
                    }
                }
            }
            else {
                if (cm instanceof IndexColorModel) {
                    IndexColorModel icm = (IndexColorModel) cm;

                    for (int srcX = 0; srcX < sourceRow.getWidth(); srcX += sourceXSubsampling) {
                        dataElements = sourceRow.getDataElements(srcX, 0, dataElements);
                        int rgb = icm.getRGB(dataElements);
                        outDataElements = destination.getColorModel().getDataElements(rgb, outDataElements);
                        int dstX = srcX / sourceXSubsampling;
                        destRaster.setDataElements(dstX, dstY, outDataElements);
                    }
                }
                else {
                    // TODO: This branch is never tested, and is probably "dead"
                    // ColorConvertOp
                    if (converter == null) {
                        converter = new ColorConvertOp(cm.getColorSpace(), destination.getColorModel().getColorSpace(), null);
                    }
                    converter.filter(
                            rowRaster.createChild(aoi.x, 0, aoi.width, 1, 0, 0, null),
                            destRaster.createWritableChild(offset.x, offset.y + srcY - aoi.y, aoi.width, 1, 0, 0, null)
                    );
                }
            }

            processImageProgress(srcY * 100f / width);
            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }
    }

    private void readChunky(final ImageReadParam param, final BufferedImage destination, final ImageInputStream input) throws IOException {
        final int width = header.width();
        final int height = header.height();

        final Rectangle aoi = getSourceRegion(param, width, height);
        final Point offset = param == null ? new Point(0, 0) : param.getDestinationOffset();

        // Set everything to default values
        int sourceXSubsampling = 1;
        int sourceYSubsampling = 1;
        int[] sourceBands = null;
        int[] destinationBands = null;

        // Get values from the ImageReadParam, if any
        if (param != null) {
            sourceXSubsampling = param.getSourceXSubsampling();
            sourceYSubsampling = param.getSourceYSubsampling();

            sourceBands = param.getSourceBands();
            destinationBands = param.getDestinationBands();
        }

        // Ensure band settings from param are compatible with images
        checkReadParamBandSettings(param, 4, destination.getSampleModel().getNumBands());

        WritableRaster destRaster = destination.getRaster();
        if (destinationBands != null || offset.x != 0 || offset.y != 0) {
            destRaster = destRaster.createWritableChild(0, 0, destRaster.getWidth(), destRaster.getHeight(), offset.x, offset.y, destinationBands);
        }

        ImageTypeSpecifier rawType = getRawImageType(0);
        WritableRaster rowRaster = rawType.createBufferedImage(width, 1).getRaster();
        Raster sourceRow = rowRaster.createChild(aoi.x, 0, aoi.width, 1, 0, 0, sourceBands);

        int planeWidth = width * 4;

        final byte[] data = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
        Object dataElements = null;

        for (int srcY = 0; srcY < height; srcY++) {
            readPlaneData(data, 0, planeWidth, input);

            if (srcY >= aoi.y && (srcY - aoi.y) % sourceYSubsampling == 0) {
                int dstY = (srcY - aoi.y) / sourceYSubsampling;
                if (sourceXSubsampling == 1) {
                    destRaster.setRect(0, dstY, sourceRow);
                }
                else {
                    for (int srcX = 0; srcX < sourceRow.getWidth(); srcX += sourceXSubsampling) {
                        dataElements = sourceRow.getDataElements(srcX, 0, dataElements);
                        int dstX = srcX / sourceXSubsampling;
                        destRaster.setDataElements(dstX, dstY, dataElements);
                    }
                }
            }

            processImageProgress(srcY * 100f / width);
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
    private void readInterleaved(final ImageReadParam param, final BufferedImage destination, final ImageInputStream input) throws IOException {
        final int width = header.width();
        final int height = header.height();

        final Rectangle aoi = getSourceRegion(param, width, height);
        final Point offset = param == null ? new Point(0, 0) : param.getDestinationOffset();

        // Set everything to default values
        int sourceXSubsampling = 1;
        int sourceYSubsampling = 1;
        int[] sourceBands = null;
        int[] destinationBands = null;

        // Get values from the ImageReadParam, if any
        if (param != null) {
            sourceXSubsampling = param.getSourceXSubsampling();
            sourceYSubsampling = param.getSourceYSubsampling();

            sourceBands = param.getSourceBands();
            destinationBands = param.getDestinationBands();
        }

        // Ensure band settings from param are compatible with images
        checkReadParamBandSettings(param, header.bitplanes() / 8, destination.getSampleModel().getNumBands());

        // NOTE:  Each row of the image is stored in an integral number of 16 bit words.
        // The number of words per row is words=((w+15)/16)
        int planeWidth = 2 * ((width + 15) / 16);
        final byte[] planeData = new byte[8 * planeWidth];

        WritableRaster destRaster = destination.getRaster();
        if (destinationBands != null || offset.x != 0 || offset.y != 0) {
            destRaster = destRaster.createWritableChild(0, 0, destRaster.getWidth(), destRaster.getHeight(), offset.x, offset.y, destinationBands);
        }

        WritableRaster rowRaster = destination.getRaster().createCompatibleWritableRaster(8 * planeWidth, 1);
        Raster sourceRow = rowRaster.createChild(aoi.x, 0, aoi.width, 1, 0, 0, sourceBands);

        final byte[] data = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
        final int channels = (header.bitplanes() + 7) / 8;
        final int planesPerChannel = 8;
        Object dataElements = null;

        for (int srcY = 0; srcY < height; srcY++) {
            for (int c = 0; c < channels; c++) {
                for (int p = 0; p < planesPerChannel; p++) {
                    readPlaneData(planeData, p * planeWidth, planeWidth, input);
                }

                // Skip rows outside AOI
                if (srcY >= (aoi.y + aoi.height)) {
                    return;
                }
                else if (srcY < aoi.y || (srcY - aoi.y) % sourceYSubsampling != 0) {
                    continue;
                }

                if (header.formType == IFF.TYPE_ILBM) {
                    // NOTE: Using (channels - c - 1) instead of just c,
                    // effectively reverses the channel order from RGBA to ABGR
                    int off = (channels - c - 1);

                    int pixelPos = 0;
                    for (int planePos = 0; planePos < planeWidth; planePos++) {
                        IFFUtil.bitRotateCW(planeData, planePos, planeWidth, data, off + pixelPos * channels, channels);
                        pixelPos += 8;
                    }
                }
                else if (header.formType == IFF.TYPE_PBM) {
                    System.arraycopy(planeData, 0, data, srcY * 8 * planeWidth, planeWidth);
                }
                else {
                    throw new AssertionError(String.format("Unsupported FORM type: %s", toChunkStr(header.formType)));
                }
            }

            if (srcY >= aoi.y && (srcY - aoi.y) % sourceYSubsampling == 0) {
                int dstY = (srcY - aoi.y) / sourceYSubsampling;
                // TODO: Avoid createChild if no region?
                if (sourceXSubsampling == 1) {
                    destRaster.setRect(0, dstY, sourceRow);
                }
                else {
                    for (int srcX = 0; srcX < sourceRow.getWidth(); srcX += sourceXSubsampling) {
                        dataElements = sourceRow.getDataElements(srcX, 0, dataElements);
                        int dstX = srcX / sourceXSubsampling;
                        destRaster.setDataElements(dstX, dstY, dataElements);
                    }
                }
            }

            processImageProgress(srcY * 100f / width);
            if (abortRequested()) {
                processReadAborted();
                break;
            }
        }
    }

    private void readPlaneData(final byte[] destination, final int offset, final int planeWidth, final ImageInputStream input)
            throws IOException {
        switch (header.compressionType()) {
            case BMHDChunk.COMPRESSION_NONE:
                input.readFully(destination, offset, planeWidth);

                // Uncompressed rows must have an even number of bytes
                if ((header.bitplanes() * planeWidth) % 2 != 0) {
                    input.readByte();
                }

                break;

            case BMHDChunk.COMPRESSION_BYTE_RUN:
                // TODO: How do we know if the last byte in the body is a pad byte or not?!
                // The body consists of byte-run (PackBits) compressed rows of bit plane data.
                // However, we don't know how long each compressed row is, without decoding it...
                // The workaround below, is to use a decode buffer size of planeWidth,
                // to make sure we don't decode anything we don't have to (shouldn't).
                if (byteRunStream == null) {
                    byteRunStream = new DataInputStream(
                            new DecoderStream(
                                    IIOUtil.createStreamAdapter(input, header.bodyLength()),
                                    new PackBitsDecoder(header.sampleSize(), true),
                                    planeWidth * (header.sampleSize() > 1 ? 1 : header.bitplanes())
                            )
                    );
                }

                byteRunStream.readFully(destination, offset, planeWidth);
                break;

            case 4: // Compression type 4 means different things for different FORM types... :-P
                if (header.formType == IFF.TYPE_RGB8) {
                    // Impulse RGB8 RLE compression: 24 bit RGB + 1 bit mask + 7 bit run count
                    if (byteRunStream == null) {
                        byteRunStream = new DataInputStream(
                                new DecoderStream(
                                        IIOUtil.createStreamAdapter(input, header.bodyLength()),
                                        new RGB8RLEDecoder(), 1024
                                )
                        );
                    }

                    byteRunStream.readFully(destination, offset, planeWidth);
                    break;
                }

            default:
                throw new IIOException(String.format("Unknown compression type: %d", header.compressionType()));
        }
    }

    private void multiPaletteToRGB(final int row, final byte[] indexed, final IndexColorModel colorModel, final byte[] dest, @SuppressWarnings("SameParameterValue") final int destOffset) {
        final int width = header.width();

        ColorModel palette = header.colorMapForRow(colorModel, row);

        for (int x = 0; x < width; x++) {
            int pixel = indexed[x] & 0xff;

            int rgb = palette.getRGB(pixel);

            int offset = (x * 3) + destOffset;
            dest[2 + offset] = (byte) ((rgb >> 16) & 0xff);
            dest[1 + offset] = (byte) ((rgb >>  8) & 0xff);
            dest[    offset] = (byte) ( rgb        & 0xff);
        }
    }

    private void hamToRGB(final byte[] indexed, final IndexColorModel colorModel, final byte[] dest, @SuppressWarnings("SameParameterValue") final int destOffset) {
        final int bits = header.bitplanes();
        final int width = header.width();

        //  Initialize to the "border color" (index 0)
        int lastRed = colorModel.getRed(0);
        int lastGreen = colorModel.getGreen(0);
        int lastBlue = colorModel.getBlue(0);

        for (int x = 0; x < width; x++) {
            int pixel = indexed[x] & 0xff;

            int paletteIndex = bits == 6 ? pixel & 0x0f : pixel & 0x3f;
            int indexShift = bits == 6 ? 4 : 2;
            int colorMask = bits == 6 ? 0x0f : 0x03;

            // Get Hold and Modify bits
            switch ((pixel >> (8 - indexShift)) & 0x03) {
                case 0x00:// HOLD
                    lastRed = colorModel.getRed(paletteIndex);
                    lastGreen = colorModel.getGreen(paletteIndex);
                    lastBlue = colorModel.getBlue(paletteIndex);
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

            int offset = (x * 3) + destOffset;
            dest[2 + offset] = (byte) lastRed;
            dest[1 + offset] = (byte) lastGreen;
            dest[    offset] = (byte) lastBlue;
        }
    }

    public static void main(String[] args) {
        ImageReader reader = new IFFImageReader(new IFFImageReaderSpi());

        boolean scale = false;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                scale = true;
                continue;
            }

            File file = new File(arg);
            if (!file.isFile()) {
                continue;
            }

            try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
                boolean canRead = reader.getOriginatingProvider().canDecodeInput(input);

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

                    if (scale) {
                        image = new ResampleOp(image.getWidth() / 2, image.getHeight(), ResampleOp.FILTER_LANCZOS).filter(image, null);
    //                image = ImageUtil.createResampled(image, image.getWidth(), image.getHeight() * 2, Image.SCALE_FAST);
                    }

                    showIt(image, arg);
                }
                else {
                    System.err.println("Foo!");
                }
            }
            catch (IOException e) {
                System.err.println("Error reading file: " + file);
                e.printStackTrace();
            }
        }
    }
}
