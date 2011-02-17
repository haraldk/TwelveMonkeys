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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.util.IndexedImageTypeSpecifier;
import com.twelvemonkeys.xml.XMLSerializer;
import org.w3c.dom.Node;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * ImageReader for Adobe Photoshop Document (PSD) format.
 *
 * @see <a href="http://www.fileformat.info/format/psd/egff.htm">Adobe Photoshop File Format Summary<a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageReader.java,v 1.0 Apr 29, 2008 4:45:52 PM haraldk Exp$
 */
// TODO: Implement ImageIO meta data interface
// TODO: Allow reading the extra alpha channels (index after composite data)
// TODO: Figure out of we should assume Adobe RGB (1998) color model, if no embedded profile?
// TODO: Support for PSDVersionInfo hasRealMergedData=false (no real composite data, layers will be in index 0)
// TODO: Support for API for reading separate layers (index after composite data, and optional alpha channels)
// TODO: Consider Romain Guy's Java 2D implementation of PS filters for the blending modes in layers
// http://www.curious-creature.org/2006/09/20/new-blendings-modes-for-java2d/
// See http://www.codeproject.com/KB/graphics/PSDParser.aspx
// See http://www.adobeforums.com/webx?14@@.3bc381dc/0
public class PSDImageReader extends ImageReaderBase {
    private PSDHeader mHeader;
//    private PSDColorData mColorData;
//    private List<PSDImageResource> mImageResources;
//    private PSDGlobalLayerMask mGlobalLayerMask;
//    private List<PSDLayerInfo> mLayerInfo;
    private ICC_ColorSpace mColorSpace;
    protected PSDMetadata mMetadata;

    protected PSDImageReader(final ImageReaderSpi pOriginatingProvider) {
        super(pOriginatingProvider);
    }

    protected void resetMembers() {
        mHeader = null;
//        mColorData = null;
//        mImageResources = null;
        mMetadata = null;
        mColorSpace = null;
    }

    public int getWidth(final int pIndex) throws IOException {
        checkBounds(pIndex);
        readHeader();
        return mHeader.mWidth;
    }

    public int getHeight(final int pIndex) throws IOException {
        checkBounds(pIndex);
        readHeader();
        return mHeader.mHeight;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(final int pIndex) throws IOException {
        return getRawImageTypeInternal(pIndex);
    }

    private ImageTypeSpecifier getRawImageTypeInternal(final int pIndex) throws IOException {
        checkBounds(pIndex);
        readHeader();

        ColorSpace cs;

        switch (mHeader.mMode) {
            case PSD.COLOR_MODE_MONOCHROME:
                if (mHeader.mChannels == 1 && mHeader.mBits == 1) {
                    return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);
                }

                throw new IIOException(
                        String.format("Unsupported channel count/bit depth for Monochrome PSD: %d channels/%d bits", mHeader.mChannels, mHeader.mBits)
                );

            case PSD.COLOR_MODE_INDEXED:
                // TODO: 16 bit indexed?! Does it exist?
                if (mHeader.mChannels == 1 && mHeader.mBits == 8) {
                    return IndexedImageTypeSpecifier.createFromIndexColorModel(mMetadata.mColorData.getIndexColorModel());
                }

                throw new IIOException(
                        String.format("Unsupported channel count/bit depth for Indexed Color PSD: %d channels/%d bits", mHeader.mChannels, mHeader.mBits)
                );

            case PSD.COLOR_MODE_DUOTONE:
                // NOTE: Duotone (whatever that is) should be treated as gray scale
                // Fall-through
            case PSD.COLOR_MODE_GRAYSCALE:
                if (mHeader.mChannels == 1 && mHeader.mBits == 8) {
                    return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);
                }
                else if (mHeader.mChannels == 1 && mHeader.mBits == 16) {
                    return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_USHORT_GRAY);
                }

                throw new IIOException(
                        String.format("Unsupported channel count/bit depth for Gray Scale PSD: %d channels/%d bits", mHeader.mChannels, mHeader.mBits)
                );

            case PSD.COLOR_MODE_RGB:
                cs = getEmbeddedColorSpace();
                if (cs == null) {
                    // TODO: Should probably be Adobe RGB (1998), not sRGB. Or..?
                    cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                }

                if (mHeader.mChannels == 3 && mHeader.mBits == 8) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_BYTE, false, false);
                }
                else if (mHeader.mChannels >= 4 && mHeader.mBits == 8) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_BYTE, true, false);
                }
                else if (mHeader.mChannels == 3 && mHeader.mBits == 16) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_USHORT, false, false);
                }
                else if (mHeader.mChannels >= 4 && mHeader.mBits == 16) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_USHORT, true, false);
                }

                throw new IIOException(
                        String.format("Unsupported channel count/bit depth for RGB PSD: %d channels/%d bits", mHeader.mChannels, mHeader.mBits)
                );

            case PSD.COLOR_MODE_CMYK:
                cs = getEmbeddedColorSpace();
                if (cs == null) {
                    cs = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);
                }

                if (mHeader.mChannels == 4 &&  mHeader.mBits == 8) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_BYTE, false, false);
                }
                else if (mHeader.mChannels == 5 &&  mHeader.mBits == 8) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3, 4}, new int[] {0, 0, 0, 0, 0}, DataBuffer.TYPE_BYTE, true, false);
                }
                else if (mHeader.mChannels == 4 &&  mHeader.mBits == 16) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_USHORT, false, false);
                }
                else if (mHeader.mChannels == 5 &&  mHeader.mBits == 16) {
                    return ImageTypeSpecifier.createBanded(cs, new int[] {0, 1, 2, 3, 4}, new int[] {0, 0, 0, 0, 0}, DataBuffer.TYPE_USHORT, true, false);
                }

                throw new IIOException(
                        String.format("Unsupported channel count/bit depth for CMYK PSD: %d channels/%d bits", mHeader.mChannels, mHeader.mBits)
                );

            case PSD.COLOR_MODE_MULTICHANNEL:
                // TODO: Implement
            case PSD.COLOR_MODE_LAB:
                // TODO: Implement
                // TODO: If there's a color profile embedded, it should be easy, otherwise we're out of luck...
            default:
                throw new IIOException(
                        String.format("Unsupported PSD MODE: %s (%d channels/%d bits)", mHeader.mMode, mHeader.mChannels, mHeader.mBits)
                );
        }
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(final int pIndex) throws IOException {
        // TODO: Check out the custom ImageTypeIterator and ImageTypeProducer used in the Sun provided JPEGImageReader
        // Could use similar concept to create lazily-created ImageTypeSpecifiers (util candidate, based on FilterIterator?)

        // Get the raw type. Will fail for unsupported types
        ImageTypeSpecifier rawType = getRawImageTypeInternal(pIndex);

        ColorSpace cs = rawType.getColorModel().getColorSpace();
        List<ImageTypeSpecifier> types = new ArrayList<ImageTypeSpecifier>();

        switch (mHeader.mMode) {
            case PSD.COLOR_MODE_RGB:
                // Prefer interleaved versions as they are much faster to display
                if (mHeader.mChannels == 3 && mHeader.mBits == 8) {
                    // TODO: ColorConvertOp to CS_sRGB
                    // TODO: Integer raster
                    // types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.INT_RGB));
                    types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));

                    if (!cs.isCS_sRGB()) {
                        // Basically BufferedImage.TYPE_3BYTE_BGR, with corrected ColorSpace. Possibly slow.
                        types.add(ImageTypeSpecifier.createInterleaved(cs, new int[] {2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
                    }
                }
                else if (mHeader.mChannels >= 4 && mHeader.mBits == 8) {
                    // TODO: ColorConvertOp to CS_sRGB
                    // TODO: Integer raster
                    // types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.INT_ARGB));
                    types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR));
//
                    if (!cs.isCS_sRGB()) {
                        // Basically BufferedImage.TYPE_4BYTE_ABGR, with corrected ColorSpace. Possibly slow.
                        types.add(ImageTypeSpecifier.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, true, false));
                    }
                }
                else if (mHeader.mChannels == 3 && mHeader.mBits == 16) {
                    types.add(ImageTypeSpecifier.createInterleaved(cs, new int[] {2, 1, 0}, DataBuffer.TYPE_USHORT, false, false));
                }
                else if (mHeader.mChannels >= 4 && mHeader.mBits == 16) {
                    types.add(ImageTypeSpecifier.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_USHORT, true, false));
                }
                break;
            case PSD.COLOR_MODE_CMYK:
                // Prefer interleaved versions as they are much faster to display
                // TODO: ColorConvertOp to CS_sRGB
                // TODO: We should convert these to their RGB equivalents while reading for the common-case,
                // as Java2D is extremely slow displaying custom images.
                // Converting to RGB is also correct behaviour, according to the docs.
                // Doing this, will require rewriting the image reading, as the raw image data is channelled, not interleaved :-/
                if (mHeader.mChannels == 4 &&  mHeader.mBits == 8) {
                    types.add(ImageTypeSpecifier.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
                }
                else if (mHeader.mChannels == 5 &&  mHeader.mBits == 8) {
                    types.add(ImageTypeSpecifier.createInterleaved(cs, new int[] {4, 3, 2, 1, 0}, DataBuffer.TYPE_BYTE, true, false));
                }
                else if (mHeader.mChannels == 4 &&  mHeader.mBits == 16) {
                    types.add(ImageTypeSpecifier.createInterleaved(cs, new int[]{3, 2, 1, 0}, DataBuffer.TYPE_USHORT, false, false));
                }
                else if (mHeader.mChannels == 5 &&  mHeader.mBits == 16) {
                    types.add(ImageTypeSpecifier.createInterleaved(cs, new int[] {4, 3, 2, 1, 0}, DataBuffer.TYPE_USHORT, true, false));
                }
                break;
            default:
                // Just stick to the raw type
        }

        // Finally add the raw type
        types.add(rawType);

        return types.iterator();
    }

    private ColorSpace getEmbeddedColorSpace() throws IOException {
        readImageResources(true);
        // TODO: Skip this, requires storing some stream offsets
        readLayerAndMaskInfo(false);

        if (mColorSpace == null) {
            ICC_Profile profile = null;
            for (PSDImageResource resource : mMetadata.mImageResources) {
                if (resource instanceof ICCProfile) {
                    profile = ((ICCProfile) resource).getProfile();
                    break;
                }
            }

            mColorSpace = profile == null ? null : ColorSpaces.createColorSpace(profile);
        }

        return mColorSpace;
    }

    public BufferedImage read(final int pIndex, final ImageReadParam pParam) throws IOException {
        checkBounds(pIndex);

        readHeader();

        readImageResources(false);
        readLayerAndMaskInfo(false);

        BufferedImage image = getDestination(pParam, getImageTypes(pIndex), mHeader.mWidth, mHeader.mHeight);
        ImageTypeSpecifier rawType = getRawImageType(pIndex);
        checkReadParamBandSettings(pParam, rawType.getNumBands(), image.getSampleModel().getNumBands());

        final Rectangle source = new Rectangle();
        final Rectangle dest = new Rectangle();
        computeRegions(pParam, mHeader.mWidth, mHeader.mHeight, image, source, dest);

        /*
        NOTE: It seems safe to just leave this out for now. The only thing we need is to support sub sampling.
        Sun's readers does not support arbitrary destination formats.

        // TODO: Create temp raster in native format w * 1
        // Read (sub-sampled) row into temp raster (skip other rows)
        // If color model (color space) is not RGB, do color convert op
        // Otherwise, copy "through" ColorModel?
        // Copy pixels from temp raster
        // If possible, leave the destination image "untouched" (accelerated)
        // See Jim Grahams comments:
        // http://forums.java.net/jive/message.jspa?messageID=295758#295758

        // TODO: Doing a per line color convert will be expensive, as data is channelled...
        // Will need to either convert entire image, or skip back/forth between channels...

        // TODO: Banding...

        ImageTypeSpecifier spec = getRawImageType(pIndex);
        BufferedImage temp = spec.createBufferedImage(getWidth(pIndex), 1);
        temp.getRaster();

        if (...)
        ColorConvertOp convert = new ColorConvertOp(...);

        */

        final int xSub;
        final int ySub;

        if (pParam == null) {
            xSub = ySub = 1;
        }
        else {
            xSub = pParam.getSourceXSubsampling();
            ySub = pParam.getSourceYSubsampling();
        }

        processImageStarted(pIndex);

        int[] byteCounts = null;
        int compression = imageInput.readShort();
        // TODO: Need to make sure compression is set in metadata, even without reading the image data!        
        mMetadata.mCompression = compression;

        switch (compression) {
            case PSD.COMPRESSION_NONE:
                break;
            case PSD.COMPRESSION_RLE:
                // NOTE: Byte counts will allow us to easily skip rows before AOI
                byteCounts = new int[mHeader.mChannels * mHeader.mHeight];
                for (int i = 0; i < byteCounts.length; i++) {
                    byteCounts[i] = imageInput.readUnsignedShort();
                }
                break;
            case PSD.COMPRESSION_ZIP:
                // TODO: Could probably use the ZIPDecoder (DeflateDecoder) here..
            case PSD.COMPRESSION_ZIP_PREDICTION:
                // TODO: Need to find out if the normal java.util.zip can handle this...
                // Could be same as PNG prediction? Read up...
                throw new IIOException("ZIP compression not supported yet");
            default:
                throw new IIOException(
                        String.format(
                                "Unknown PSD compression: %d. Expected 0 (none), 1 (RLE), 2 (ZIP) or 3 (ZIP w/prediction).",
                                compression
                        )
                );
        }

        // What we read here is the "composite layer" of the PSD file
        readImageData(image, rawType.getColorModel(), source, dest, xSub, ySub, byteCounts, compression);

        if (abortRequested()) {
            processReadAborted();
        }
        else {
            processImageComplete();
        }

        return image;
    }

    private void readImageData(final BufferedImage pImage,
                               final ColorModel pSourceCM, final Rectangle pSource, final Rectangle pDest,
                               final int pXSub, final int pYSub,
                               final int[] pByteCounts, final int pCompression) throws IOException {

        final WritableRaster raster = pImage.getRaster();
        // TODO: Conversion if destination cm is not compatible
        final ColorModel destCM = pImage.getColorModel();

        // TODO: This raster is 3-5 times longer than needed, depending on number of channels...
        final WritableRaster rowRaster = pSourceCM.createCompatibleWritableRaster(mHeader.mWidth, 1);

        final int channels = rowRaster.getNumBands();
        final boolean banded = raster.getDataBuffer().getNumBanks() > 1;
        final int interleavedBands = banded ? 1 : raster.getNumBands();

        for (int c = 0; c < channels; c++) {
            int bandOffset = banded ? 0 : interleavedBands - 1 - c;

            switch (mHeader.mBits) {
                case 1:
                    byte[] row1 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                    read1bitChannel(c, mHeader.mChannels, raster.getDataBuffer(), interleavedBands, bandOffset, pSourceCM, row1, pSource, pDest, pXSub, pYSub, mHeader.mWidth, mHeader.mHeight, pByteCounts, pCompression == PSD.COMPRESSION_RLE);
                    break;
                case 8:
                    byte[] row8 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                    read8bitChannel(c, mHeader.mChannels, raster.getDataBuffer(), interleavedBands, bandOffset, pSourceCM, row8, pSource, pDest, pXSub, pYSub, mHeader.mWidth, mHeader.mHeight, pByteCounts, c * mHeader.mHeight, pCompression == PSD.COMPRESSION_RLE);
                    break;
                case 16:
                    short[] row16 = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
                    read16bitChannel(c, mHeader.mChannels, raster.getDataBuffer(), interleavedBands, bandOffset, pSourceCM, row16, pSource, pDest, pXSub, pYSub, mHeader.mWidth, mHeader.mHeight, pByteCounts, c * mHeader.mHeight, pCompression == PSD.COMPRESSION_RLE);
                    break;
                default:
                    throw new IIOException(String.format("Unknown PSD bit depth: %s", mHeader.mBits));
            }

            if (abortRequested()) {
                break;
            }
        }

        if (mHeader.mBits == 8) {
            // Compose out the background of the semi-transparent pixels, as PS somehow has the background composed in
            decomposeAlpha(destCM, raster.getDataBuffer(), pDest.width, pDest.height, raster.getNumBands());
        }
    }

    private void processImageProgressForChannel(int channel, int channelCount, int y, int height) {
        processImageProgress(100f * channel / channelCount + 100f * y / (height * channelCount));
    }

    private void read16bitChannel(final int pChannel, final int pChannelCount,
                                  final DataBuffer pData, final int pBands, final int pBandOffset,
                                  final ColorModel pSourceColorModel,
                                  final short[] pRow,
                                  final Rectangle pSource, final Rectangle pDest,
                                  final int pXSub, final int pYSub,
                                  final int pChannelWidth, final int pChannelHeight,
                                  final int[] pRowByteCounts, final int pRowOffset,
                                  final boolean pRLECompressed) throws IOException {

        final boolean isCMYK = pSourceColorModel.getColorSpace().getType() == ColorSpace.TYPE_CMYK;
        final int colorComponents = pSourceColorModel.getColorSpace().getNumComponents();
        final boolean banded = pData.getNumBanks() > 1;

        for (int y = 0; y < pChannelHeight; y++) {
            // NOTE: Length is in *16 bit values* (shorts)
            int length = 2 * (pRLECompressed ? pRowByteCounts[pRowOffset + y] : pChannelWidth);

            // TODO: Sometimes need to read the line y == source.y + source.height...
            // Read entire line, if within source region and sampling
            if (y >= pSource.y && y < pSource.y + pSource.height && y % pYSub == 0) {
                if (pRLECompressed) {
                    DataInputStream input = PSDUtil.createPackBitsStream(imageInput, length);
                    try {
                        for (int x = 0; x < pChannelWidth; x++) {
                            pRow[x] = input.readShort();
                        }
                    }
                    finally {
                        input.close();
                    }
                }
                else {
                    imageInput.readFully(pRow, 0, pChannelWidth);
                }

                // TODO: Destination offset...??
                // Copy line sub sampled into real data
                int offset = (y - pSource.y) / pYSub * pDest.width * pBands + pBandOffset;
                for (int x = 0; x < pDest.width; x++) {
                    short value = pRow[pSource.x + x * pXSub];

                    // CMYK values are stored inverted, but alpha is not
                    if (isCMYK && pChannel < colorComponents) {
                        value = (short) (65535 - value & 0xffff);
                    }

                    pData.setElem(banded ? pChannel : 0, offset + x * pBands, value);
                }
            }
            else {
                imageInput.skipBytes(length);
            }

            if (abortRequested()) {
                break;
            }
            processImageProgressForChannel(pChannel, pChannelCount, y, pChannelHeight);
        }
    }

    private void read8bitChannel(final int pChannel, final int pChannelCount,
                                 final DataBuffer pData, final int pBands, final int pBandOffset,
                                 final ColorModel pSourceColorModel,
                                 final byte[] pRow,
                                 final Rectangle pSource, final Rectangle pDest,
                                 final int pXSub, final int pYSub,
                                 final int pChannelWidth, final int pChannelHeight,
                                 final int[] pRowByteCounts, final int pRowOffset,
                                 final boolean pRLECompressed) throws IOException {

        final boolean isCMYK = pSourceColorModel.getColorSpace().getType() == ColorSpace.TYPE_CMYK;
        final int colorComponents = pSourceColorModel.getColorSpace().getNumComponents();
        final boolean banded = pData.getNumBanks() > 1;

        for (int y = 0; y < pChannelHeight; y++) {
            int length = pRLECompressed ? pRowByteCounts[pRowOffset + y] : pChannelWidth;

            // TODO: Sometimes need to read the line y == source.y + source.height...
            // Read entire line, if within source region and sampling
            if (y >= pSource.y && y < pSource.y + pSource.height && y % pYSub == 0) {
                if (pRLECompressed) {
                    DataInputStream input = PSDUtil.createPackBitsStream(imageInput, length);
                    try {
                        input.readFully(pRow, 0, pChannelWidth);
                    }
                    finally {
                        input.close();
                    }
                }
                else {
                    imageInput.readFully(pRow, 0, pChannelWidth);
                }

                // TODO: If banded and not sub sampling/cmyk, we could just copy using System.arraycopy
                // TODO: Destination offset...??
                // Copy line sub sampled into real data
                int offset = (y - pSource.y) / pYSub * pDest.width * pBands + pBandOffset;
                for (int x = 0; x < pDest.width; x++) {
                    byte value = pRow[pSource.x + x * pXSub];

                    // CMYK values are stored inverted, but alpha is not
                    if (isCMYK && pChannel < colorComponents) {
                        value = (byte) (255 - value & 0xff);
                    }

                    pData.setElem(banded ? pChannel : 0, offset + x * pBands, value);
                }
            }
            else {
                imageInput.skipBytes(length);
            }

            if (abortRequested()) {
                break;
            }
            processImageProgressForChannel(pChannel, pChannelCount, y, pChannelHeight);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void read1bitChannel(final int pChannel, final int pChannelCount,
                                 final DataBuffer pData, final int pBands, final int pBandOffset,
                                 final ColorModel pSourceColorModel,
                                 final byte[] pRow,
                                 final Rectangle pSource, final Rectangle pDest,
                                 final int pXSub, final int pYSub,
                                 final int pChannelWidth, final int pChannelHeight,
                                 final int[] pRowByteCounts, boolean pRLECompressed) throws IOException {
        // NOTE: 1 bit channels only occurs once

        final int destWidth = (pDest.width + 7) / 8;
        final boolean banded = pData.getNumBanks() > 1;

        for (int y = 0; y < pChannelHeight; y++) {
            int length = pRLECompressed ? pRowByteCounts[y] : pChannelWidth;

            // TODO: Sometimes need to read the line y == source.y + source.height...
            // Read entire line, if within source region and sampling
            if (y >= pSource.y && y < pSource.y + pSource.height && y % pYSub == 0) {
                if (pRLECompressed) {
                    DataInputStream input = PSDUtil.createPackBitsStream(imageInput, length);
                    try {
                        input.readFully(pRow, 0, pRow.length);
                    }
                    finally {
                        input.close();
                    }
                }
                else {
                    imageInput.readFully(pRow, 0, pRow.length);
                }

                // TODO: Destination offset...??
                int offset = (y - pSource.y) / pYSub * destWidth;
                if (pXSub == 1 && pSource.x % 8 == 0) {
                    // Fast normal case, no sub sampling
                    for (int i = 0; i < destWidth; i++) {
                        byte value = pRow[pSource.x / 8 + i * pXSub];
                        // NOTE: Invert bits to match Java's default monochrome
                        pData.setElem(banded ? pChannel : 0, offset + i, (byte) (~value & 0xff));
                    }
                }
                else {
                    // Copy line sub sampled into real data
                    final int maxX = pSource.x + pSource.width;
                    int x = pSource.x;
                    for (int i = 0; i < destWidth; i++) {
                        byte result = 0;

                        for (int j = 0; j < 8 && x < maxX; j++) {
                            int bytePos = x / 8;

                            int sourceBitOff = 7 - (x % 8);
                            int mask = 1 << sourceBitOff;
                            int destBitOff = 7 - j;

                            // Shift bit into place
                            result |= ((pRow[bytePos] & mask) >> sourceBitOff) << destBitOff;

                            x += pXSub;
                        }

                        // NOTE: Invert bits to match Java's default monochrome
                        pData.setElem(banded ? pChannel : 0, offset + i, (byte) (~result & 0xff));
                    }
                }
            }
            else {
                imageInput.skipBytes(length);
            }

            if (abortRequested()) {
                break;
            }
            processImageProgressForChannel(pChannel, pChannelCount, y, pChannelHeight);
        }
    }

    private void decomposeAlpha(final ColorModel pModel, final DataBuffer pBuffer,
                                final int pWidth, final int pHeight, final int pChannels) {
        // TODO: Is the document background always white!?
        // TODO: What about CMYK + alpha?
        if (pModel.hasAlpha() && pModel.getColorSpace().getType() == ColorSpace.TYPE_RGB) {

            // TODO: Probably faster to do this in line..
            if (pBuffer.getNumBanks() > 1) {

                for (int y = 0; y < pHeight; y++) {
                    for (int x = 0; x < pWidth; x++) {
                        int offset = (x + y * pWidth);
                        // ARGB format
                        int alpha = pBuffer.getElem(pChannels - 1, offset) & 0xff;

                        if (alpha != 0) {
                            double normalizedAlpha = alpha / 255.0;

                            for (int i = 0; i < pChannels - 1; i++) {
                                pBuffer.setElem(i, offset, decompose(pBuffer.getElem(i, offset) & 0xff, normalizedAlpha));
                            }
                        }
                        else {
                            for (int i = 0; i < pChannels - 1; i++) {
                                pBuffer.setElem(i, offset, 0);
                            }
                        }
                    }
                }
            }
            else {
                for (int y = 0; y < pHeight; y++) {
                    for (int x = 0; x < pWidth; x++) {
                        int offset = (x + y * pWidth) * pChannels;
                        // ABGR format
                        int alpha = pBuffer.getElem(offset) & 0xff;

                        if (alpha != 0) {
                            double normalizedAlpha = alpha / 255.0;

                            for (int i = 1; i < pChannels; i++) {
                                pBuffer.setElem(offset + i, decompose(pBuffer.getElem(offset + i) & 0xff, normalizedAlpha));
                            }
                        }
                        else {
                            for (int i = 1; i < pChannels; i++) {
                                pBuffer.setElem(offset + i, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    private static byte decompose(final int pColor, final double pAlpha) {
        // Adapted from Computer Graphics: Principles and Practice (Foley et al.), p. 837
        double color = pColor / 255.0;
        return (byte) ((color / pAlpha - ((1 - pAlpha) / pAlpha)) * 255);
    }

    private void readHeader() throws IOException {
        assertInput();
        if (mHeader == null) {
            mHeader = new PSDHeader(imageInput);

            mMetadata = new PSDMetadata();
            mMetadata.mHeader = mHeader;

            /*
            Contains the required data to define the color mode.

            For indexed color images, the count will be equal to 768, and the mode data
            will contain the color table for the image, in non-interleaved order.

            For duotone images, the mode data will contain the duotone specification, 
            the format of which is not documented.  Non-Photoshop readers can treat
            the duotone image as a grayscale image, and keep the duotone specification
            around as a black box for use when saving the file.
             */
            if (mHeader.mMode == PSD.COLOR_MODE_INDEXED) {
                mMetadata.mColorData = new PSDColorData(imageInput);
            }
            else {
                // TODO: We need to store the duotone spec if we decide to create a writer...
                // Skip color mode data for other modes
                long length = imageInput.readUnsignedInt();
                imageInput.skipBytes(length);
            }

            // Don't need the header again
            imageInput.flushBefore(imageInput.getStreamPosition());
        }
    }

    // TODO: Flags or list of interesting resources to parse
    // TODO: Obey ignoreMetadata
    private void readImageResources(final boolean pParseData) throws IOException {
        // TODO: Avoid unnecessary stream repositioning
        long pos = imageInput.getFlushedPosition();
        imageInput.seek(pos);

        long length = imageInput.readUnsignedInt();

        if (pParseData && length > 0) {
            if (mMetadata.mImageResources == null) {
                mMetadata.mImageResources = new ArrayList<PSDImageResource>();
                long expectedEnd = imageInput.getStreamPosition() + length;

                while (imageInput.getStreamPosition() < expectedEnd) {
                    // TODO: Have PSDImageResources defer actual parsing? (Just store stream offsets)
                    PSDImageResource resource = PSDImageResource.read(imageInput);
                    mMetadata.mImageResources.add(resource);
                }

                if (imageInput.getStreamPosition() != expectedEnd) {
                    throw new IIOException("Corrupt PSD document"); // ..or maybe just a bug in the reader.. ;-)
                }
            }
        }

        imageInput.seek(pos + length + 4);
    }

    // TODO: Flags or list of interesting resources to parse
    // TODO: Obey ignoreMetadata
    private void readLayerAndMaskInfo(final boolean pParseData) throws IOException {
        // TODO: Make sure we are positioned correctly
        long length = imageInput.readUnsignedInt();
        if (pParseData && length > 0) {
            long pos = imageInput.getStreamPosition();

            long layerInfoLength = imageInput.readUnsignedInt();

            /*
             "Layer count. If it is a negative number, its absolute value is the number of
             layers and the first alpha channel contains the transparency data for the
             merged result."
             */
            // TODO: Figure out what the last part of that sentence means in practice...
            int layers = imageInput.readShort();

            PSDLayerInfo[] layerInfos = new PSDLayerInfo[Math.abs(layers)];
            for (int i = 0; i < layerInfos.length; i++) {
                layerInfos[i] = new PSDLayerInfo(imageInput);
            }
            mMetadata.mLayerInfo = Arrays.asList(layerInfos);

            // TODO: Clean-up
            imageInput.mark();
            ImageTypeSpecifier raw = getRawImageTypeInternal(0);
            ImageTypeSpecifier imageType = getImageTypes(0).next();
            imageInput.reset();

            for (PSDLayerInfo layerInfo : layerInfos) {
                // TODO: If not explicitly needed, skip layers...
                BufferedImage layer = readLayerData(layerInfo, raw, imageType);

                // TODO: Don't show! Store in meta data somehow...
//                if (layer != null) {
//                    showIt(layer, layerInfo.mLayerName + " " + layerInfo.mBlendMode.toString());
//                }
            }

            long read = imageInput.getStreamPosition() - pos;

            long diff = layerInfoLength - (read - 4); // - 4 for the layerInfoLength field itself
//            System.out.println("diff: " + diff);
            imageInput.skipBytes(diff);

            // TODO: Global LayerMaskInfo (18 bytes or more..?)
            // 4 (length), 2 (colorSpace), 8 (4 * 2 byte color components), 2 (opacity %), 1 (kind), variable (pad)
            long layerMaskInfoLength = imageInput.readUnsignedInt();
//            System.out.println("GlobalLayerMaskInfo length: " + layerMaskInfoLength);
            if (layerMaskInfoLength > 0) {
                mMetadata.mGlobalLayerMask = new PSDGlobalLayerMask(imageInput);
//                System.out.println("mGlobalLayerMask: " + mGlobalLayerMask);
            }

            read = imageInput.getStreamPosition() - pos;

            long toSkip = length - read;
//            System.out.println("toSkip: " + toSkip);
            imageInput.skipBytes(toSkip);
        }
        else {
            // Skip entire layer and mask section
            imageInput.skipBytes(length);
        }
    }

    private BufferedImage readLayerData(final PSDLayerInfo pLayerInfo, final ImageTypeSpecifier pRawType, final ImageTypeSpecifier pImageType) throws IOException {
        final int width = pLayerInfo.mRight - pLayerInfo.mLeft;
        final int height = pLayerInfo.mBottom - pLayerInfo.mTop;

        // Even if raw/imageType has no alpha, the layers may still have alpha...
        ImageTypeSpecifier imageType = getImageTypeForLayer(pImageType, pLayerInfo);

        // Create image (or dummy, if h/w are <= 0)
        BufferedImage layer = imageType.createBufferedImage(Math.max(1, width), Math.max(1, height));

        // Source/destination area
        Rectangle area = new Rectangle(width, height);

        final int xsub = 1;
        final int ysub = 1;

        final WritableRaster raster = layer.getRaster();
        // TODO: Conversion if destination cm is not compatible
        final ColorModel destCM = layer.getColorModel();

        // TODO: This raster is 3-5 times longer than needed, depending on number of channels...
        ColorModel sourceCM = pRawType.getColorModel();
        final WritableRaster rowRaster = width > 0 ? sourceCM.createCompatibleWritableRaster(width, 1) : null;

//                        final int channels = rowRaster.getNumBands();
        final boolean banded = raster.getDataBuffer().getNumBanks() > 1;
        final int interleavedBands = banded ? 1 : raster.getNumBands();

        for (PSDChannelInfo channelInfo : pLayerInfo.mChannelInfo) {
            int compression = imageInput.readUnsignedShort();

            // Skip layer if we can't read it
            // channelId == -2 means "user supplied layer mask", whatever that is...
            if (width <= 0 || height <= 0 || channelInfo.mChannelId == -2 ||
                    (compression != PSD.COMPRESSION_NONE && compression != PSD.COMPRESSION_RLE)) {
                imageInput.skipBytes(channelInfo.mLength - 2);
            }
            else {
                // 0 = red, 1 = green, etc
                // -1 = transparency mask; -2 = user supplied layer mask
                int c = channelInfo.mChannelId == -1 ? pLayerInfo.mChannelInfo.length - 1 : channelInfo.mChannelId;

                // NOTE: For layers, byte counts are written per channel, while for the composite data
                //       byte counts are written for all channels before the image data.
                //       This is the reason for the current code duplication
                int[] byteCounts = null;

                // 0: None, 1: PackBits RLE, 2: Zip, 3: Zip w/prediction
                switch (compression) {
                    case PSD.COMPRESSION_NONE:
                        break;
                    case PSD.COMPRESSION_RLE:
                        // If RLE, the the image data starts with the byte counts
                        // for all the scan lines in the channel (LayerBottom-LayerTop), with
                        // each count stored as a two*byte value.
                        byteCounts = new int[pLayerInfo.mBottom - pLayerInfo.mTop];
                        for (int i = 0; i < byteCounts.length; i++) {
                            byteCounts[i] = imageInput.readUnsignedShort();
                        }

                        break;
                    case PSD.COMPRESSION_ZIP:
                    case PSD.COMPRESSION_ZIP_PREDICTION:
                    default:
                        // Explicitly skipped above
                        throw new AssertionError(String.format("Unsupported layer data. Compression: %d", compression));
                }

                int bandOffset = banded ? 0 : interleavedBands - 1 - c;

                switch (mHeader.mBits) {
                    case 1:
                        byte[] row1 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
//                        DataBufferByte buffer1 = (DataBufferByte) raster.getDataBuffer();
//                        byte[] data1 = banded ? buffer1.getData(c) : buffer1.getData();

                        read1bitChannel(c, imageType.getNumBands(), raster.getDataBuffer(), interleavedBands, bandOffset, sourceCM, row1, area, area, xsub, ysub, width, height, byteCounts, compression == PSD.COMPRESSION_RLE);
                        break;
                    case 8:
                        byte[] row8 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
//                        DataBufferByte buffer8 = (DataBufferByte) raster.getDataBuffer();
//                        byte[] data8 = banded ? buffer8.getData(c) : buffer8.getData();

//                        read8bitChannel(c, imageType.getNumBands(), data8, interleavedBands, bandOffset, sourceCM, row8, area, area, xsub, ysub, width, height, byteCounts, 0, compression == PSD.COMPRESSION_RLE);
                        read8bitChannel(c, imageType.getNumBands(), raster.getDataBuffer(), interleavedBands, bandOffset, sourceCM, row8, area, area, xsub, ysub, width, height, byteCounts, 0, compression == PSD.COMPRESSION_RLE);
                        break;
                    case 16:
                        short[] row16 = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
//                        DataBufferUShort buffer16 = (DataBufferUShort) raster.getDataBuffer();
//                        short[] data16 = banded ? buffer16.getData(c) : buffer16.getData();

                        read16bitChannel(c, imageType.getNumBands(), raster.getDataBuffer(), interleavedBands, bandOffset, sourceCM, row16, area, area, xsub, ysub, width, height, byteCounts, 0, compression == PSD.COMPRESSION_RLE);
                        break;
                    default:
                        throw new IIOException(String.format("Unknown PSD bit depth: %s", mHeader.mBits));
                }

                if (abortRequested()) {
                    break;
                }
            }
        }

        return layer;
    }

    private ImageTypeSpecifier getImageTypeForLayer(final ImageTypeSpecifier pOriginal, final PSDLayerInfo pLayerInfo) {
        // If layer has more channels than composite data, it's normally extra alpha...
        if (pLayerInfo.mChannelInfo.length > pOriginal.getNumBands()) {
            // ...but, it could also be just the user mask...
            boolean userMask = false;
            for (PSDChannelInfo channelInfo : pLayerInfo.mChannelInfo) {
                if (channelInfo.mChannelId == -2) {
                    userMask = true;
                    break;
                }
            }

            int newBandNum = pLayerInfo.mChannelInfo.length - (userMask ? 1 : 0);

            // If there really is more channels, then create new imageTypeSpec
            if (newBandNum > pOriginal.getNumBands()) {
                int[] offs = new int[newBandNum];
                for (int i = 0, offsLength = offs.length; i < offsLength; i++) {
                    offs[i] = offsLength - i;
                }

                return ImageTypeSpecifier.createInterleaved(pOriginal.getColorModel().getColorSpace(), offs, pOriginal.getSampleModel().getDataType(), true, false);
            }
        }
        return pOriginal;
    }

    /// Layer support
    // TODO: For now, leave as Metadata

    /*
    int getNumLayers(int pImageIndex) throws IOException;

    boolean hasLayers(int pImageIndex) throws IOException;

    BufferedImage readLayer(int pImageIndex, int pLayerIndex, ImageReadParam pParam) throws IOException;

    int getLayerWidth(int pImageIndex, int pLayerIndex) throws IOException;

    int getLayerHeight(int pImageIndex, int pLayerIndex) throws IOException;

    // ?
    Point getLayerOffset(int pImageIndex, int pLayerIndex) throws IOException;

     */

    /// Metadata support
    // TODO

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        // null might be appropriate here
        // "For image formats that contain a single image, only image metadata is used."
        return super.getStreamMetadata();
    }

    @Override
    public IIOMetadata getImageMetadata(final int pImageIndex) throws IOException {
        // TODO: Implement
        checkBounds(pImageIndex);

        readHeader();
        readImageResources(true);
        readLayerAndMaskInfo(true);

        // TODO: Need to make sure compression is set in metadata, even without reading the image data!        
        mMetadata.mCompression = imageInput.readShort();

//        mMetadata.mHeader = mHeader;
//        mMetadata.mColorData = mColorData;
//        mMetadata.mImageResources = mImageResources;

        return mMetadata; // TODO: clone if we change to mutable metadata
    }

    @Override
    public IIOMetadata getImageMetadata(final int imageIndex, final String formatName, final Set<String> nodeNames) throws IOException {
        // TODO: It might make sense to overload this, as there's loads of meta data in the file
        return super.getImageMetadata(imageIndex, formatName, nodeNames);
    }

    /// Thumbnail support
    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    private List<PSDThumbnail> getThumbnailResources(final int pIndex) throws IOException {
        checkBounds(pIndex);

        readHeader();

        List<PSDThumbnail> thumbnails = null;

        if (mMetadata.mImageResources == null) {
            // TODO: Need flag here, to specify what resources to read...
            readImageResources(true);
            // TODO: Skip this, requires storing some stream offsets
            readLayerAndMaskInfo(false);
        }

        for (PSDImageResource resource : mMetadata.mImageResources) {
            if (resource instanceof PSDThumbnail) {
                if (thumbnails == null) {
                    thumbnails = new ArrayList<PSDThumbnail>();
                }

                thumbnails.add((PSDThumbnail) resource);
            }
        }

        return thumbnails;
    }

    @Override
    public int getNumThumbnails(final int pIndex) throws IOException {
        List<PSDThumbnail> thumbnails = getThumbnailResources(pIndex);

        return thumbnails == null ? 0 : thumbnails.size();
    }

    private PSDThumbnail getThumbnailResource(final int pImageIndex, final int pThumbnailIndex) throws IOException {
        List<PSDThumbnail> thumbnails = getThumbnailResources(pImageIndex);

        if (thumbnails == null) {
            throw new IndexOutOfBoundsException(String.format("thumbnail index %d > 0", pThumbnailIndex));
        }

        return thumbnails.get(pThumbnailIndex);
    }

    @Override
    public int getThumbnailWidth(final int pImageIndex, final int pThumbnailIndex) throws IOException {
        return getThumbnailResource(pImageIndex, pThumbnailIndex).getWidth();
    }

    @Override
    public int getThumbnailHeight(final int pImageIndex, final int pThumbnailIndex) throws IOException {
        return getThumbnailResource(pImageIndex, pThumbnailIndex).getHeight();
    }

    @Override
    public BufferedImage readThumbnail(final int pImageIndex, final int pThumbnailIndex) throws IOException {
        // TODO: Thumbnail progress listeners...
        PSDThumbnail thumbnail = getThumbnailResource(pImageIndex, pThumbnailIndex);

        // TODO: Defer decoding
        // TODO: It's possible to attach listeners to the ImageIO reader delegate... But do we really care?
        processThumbnailStarted(pImageIndex, pThumbnailIndex);
        processThumbnailComplete();

        // TODO: Returning a cached mutable thumbnail is not really safe...
        return thumbnail.getThumbnail();
    }

    /// Functional testing
    public static void main(final String[] pArgs) throws IOException {
        int subsampleFactor = 1;
        Rectangle sourceRegion = null;

        int idx = 0;
        while (pArgs[idx].charAt(0) == '-') {
            if (pArgs[idx].equals("-s")) {
                subsampleFactor = Integer.parseInt(pArgs[++idx]);
            }
            else if (pArgs[idx].equals("-r")) {
                int xw = Integer.parseInt(pArgs[++idx]);
                int yh = Integer.parseInt(pArgs[++idx]);

                try {
                    int w = Integer.parseInt(pArgs[idx + 1]);
                    int h = Integer.parseInt(pArgs[idx + 2]);

                    idx += 2;

                    // x y w h
                    sourceRegion = new Rectangle(xw, yh, w, h);
                }
                catch (NumberFormatException e) {
                    // w h
                    sourceRegion = new Rectangle(xw, yh);
                }

                System.out.println("sourceRegion: " + sourceRegion);
            }
            else {
                System.err.println("Usage: java PSDImageReader [-s <subsample factor>] [-r [<x y>] <w h>] <image file>");
                System.exit(1);
            }

            idx++;
        }

        PSDImageReader imageReader = new PSDImageReader(null);

        File file = new File(pArgs[idx]);
        ImageInputStream stream = ImageIO.createImageInputStream(file);
        imageReader.setInput(stream);

        imageReader.readHeader();
//        System.out.println("imageReader.mHeader: " + imageReader.mHeader);

        imageReader.readImageResources(true);
        System.out.println("imageReader.mImageResources: " + imageReader.mMetadata.mImageResources);
        System.out.println();

        imageReader.readLayerAndMaskInfo(true);
        System.out.println("imageReader.mLayerInfo: " + imageReader.mMetadata.mLayerInfo);
//        System.out.println("imageReader.mGlobalLayerMask: " + imageReader.mGlobalLayerMask);
        System.out.println();

        IIOMetadata metadata = imageReader.getImageMetadata(0);
        Node node;
        XMLSerializer serializer;

        node = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
        serializer = new XMLSerializer(System.out, System.getProperty("file.encoding"));
        serializer.setIndentation("   ");
        serializer.serialize(node, true);
        System.out.println();

        node = metadata.getAsTree(PSDMetadata.NATIVE_METADATA_FORMAT_NAME);
//        serializer = new XMLSerializer(System.out, System.getProperty("file.encoding"));
        serializer.serialize(node, true);

        if (imageReader.hasThumbnails(0)) {
            int thumbnails = imageReader.getNumThumbnails(0);
            for (int i = 0; i < thumbnails; i++) {
                showIt(imageReader.readThumbnail(0, i), String.format("Thumbnail %d", i));                
            }
        }

        long start = System.currentTimeMillis();

        ImageReadParam param = imageReader.getDefaultReadParam();

        if (sourceRegion != null) {
            param.setSourceRegion(sourceRegion);
        }

        if (subsampleFactor > 1) {
            param.setSourceSubsampling(subsampleFactor, subsampleFactor, 0, 0);
        }

//        param.setDestinationType(imageReader.getRawImageType(0));

        BufferedImage image = imageReader.read(0, param);
        System.out.println("time: " + (System.currentTimeMillis() - start));
        System.out.println("image: " + image);

        if (image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
            try {
                ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
                image = op.filter(image, new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE));
            }
            catch (Exception e) {
                e.printStackTrace();
                image = ImageUtil.accelerate(image);
            }
            System.out.println("time: " + (System.currentTimeMillis() - start));
            System.out.println("image: " + image);
        }

        showIt(image, file.getName());
    }
}
