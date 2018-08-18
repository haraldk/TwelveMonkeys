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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
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
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDImageReader.java,v 1.0 Apr 29, 2008 4:45:52 PM haraldk Exp$
 * @see <a href="http://www.adobe.com/devnet-apps/photoshop/fileformatashtml/">Adobe Photoshop File Formats Specification<a>
 * @see <a href="http://www.fileformat.info/format/psd/egff.htm">Adobe Photoshop File Format Summary<a>
 */
// TODO: Implement ImageIO meta data interface
// TODO: Figure out of we should assume Adobe RGB (1998) color model, if no embedded profile?
// TODO: Support for PSDVersionInfo hasRealMergedData=false (no real composite data, layers will be in index 0)
// TODO: Consider Romain Guy's Java 2D implementation of PS filters for the blending modes in layers
// http://www.curious-creature.org/2006/09/20/new-blendings-modes-for-java2d/
// See http://www.codeproject.com/KB/graphics/PSDParser.aspx
// See http://www.adobeforums.com/webx?14@@.3bc381dc/0  
// Done: Allow reading the extra alpha channels (index after composite data)
public final class PSDImageReader extends ImageReaderBase {

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.psd.debug"));

    private PSDHeader header;
    private ICC_ColorSpace colorSpace;
    private PSDMetadata metadata;

    PSDImageReader(final ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    protected void resetMembers() {
        header = null;
        metadata = null;
        colorSpace = null;
    }

    public int getWidth(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        if (imageIndex > 0) {
            return getLayerWidth(imageIndex - 1);
        }

        return header.width;
    }

    public int getHeight(final int imageIndex) throws IOException {
        checkBounds(imageIndex);
        readHeader();

        if (imageIndex > 0) {
            return getLayerHeight(imageIndex - 1);
        }

        return header.height;
    }

    private int getLayerWidth(int layerIndex) throws IOException {
        readLayerAndMaskInfo(true);

        PSDLayerInfo layerInfo = metadata.layerInfo.get(layerIndex);

        return layerInfo.right - layerInfo.left;
    }

    private int getLayerHeight(int layerIndex) throws IOException {
        readLayerAndMaskInfo(true);

        PSDLayerInfo layerInfo = metadata.layerInfo.get(layerIndex);

        return layerInfo.bottom - layerInfo.top;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        return getRawImageTypeInternal(imageIndex);
    }

    private ImageTypeSpecifier getRawImageTypeInternal(final int imageIndex) throws IOException {
        checkBounds(imageIndex);

        // Image index above 0, means a layer
        if (imageIndex > 0) {
            readLayerAndMaskInfo(true);

            return getRawImageTypeForLayer(imageIndex - 1);
        }

        // Otherwise, get the type specifier for the composite layer
        return getRawImageTypeForCompositeLayer();
    }

    private ImageTypeSpecifier getRawImageTypeForCompositeLayer() throws IOException {
        ColorSpace cs;

        switch (header.mode) {
            case PSD.COLOR_MODE_BITMAP:
                if (header.channels == 1 && header.bits == 1) {
                    return ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);
                }

                throw new IIOException(String.format("Unsupported channel count/bit depth for Monochrome PSD: %d channels/%d bits", header.channels, header.bits));

            case PSD.COLOR_MODE_INDEXED:
                if (header.channels == 1 && header.bits == 8) {
                    return ImageTypeSpecifiers.createFromIndexColorModel(metadata.colorData.getIndexColorModel());
                }

                throw new IIOException(String.format("Unsupported channel count/bit depth for Indexed Color PSD: %d channels/%d bits", header.channels, header.bits));

            case PSD.COLOR_MODE_DUOTONE:
                // NOTE: Duotone (whatever that is) should be treated as gray scale
                // Fall-through
            case PSD.COLOR_MODE_GRAYSCALE:
                cs = getEmbeddedColorSpace();
                if (cs == null) {
                    cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                }

                if (header.channels >= 1) {
                    switch (header.bits) {
                        case 8:
                            return metadata.hasAlpha() && header.channels > 1
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1}, new int[] {0, 0}, DataBuffer.TYPE_BYTE, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0}, new int[] {0}, DataBuffer.TYPE_BYTE, false, false);
                        case 16:
                            return metadata.hasAlpha() && header.channels > 1
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1}, new int[] {0, 0}, DataBuffer.TYPE_USHORT, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0}, new int[] {0}, DataBuffer.TYPE_USHORT, false, false);
                        case 32:
                            return metadata.hasAlpha() && header.channels > 1
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1}, new int[] {0, 0}, DataBuffer.TYPE_INT, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0}, new int[] {0}, DataBuffer.TYPE_INT, false, false);
                    }
                }

                throw new IIOException(String.format("Unsupported channel count/bit depth for Gray Scale PSD: %d channels/%d bits", header.channels, header.bits));

            case PSD.COLOR_MODE_RGB:
                cs = getEmbeddedColorSpace();
                if (cs == null) {
                    cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
                }

                if (header.channels >= 3) {
                    switch (header.bits) {
                        case 8:
                            return metadata.hasAlpha() && header.channels > 3
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_BYTE, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_BYTE, false, false);
                        case 16:
                            return metadata.hasAlpha() && header.channels > 3
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_USHORT, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_USHORT, false, false);
                        case 32:
                            return metadata.hasAlpha() && header.channels > 3
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_INT, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2}, new int[] {0, 0, 0}, DataBuffer.TYPE_INT, false, false);

                    }
                }

                throw new IIOException(String.format("Unsupported channel count/bit depth for RGB PSD: %d channels/%d bits", header.channels, header.bits));

            case PSD.COLOR_MODE_CMYK:
                cs = getEmbeddedColorSpace();
                if (cs == null) {
                    cs = ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK);
                }

                if (header.channels >= 4) {
                    switch (header.bits) {
                        case 8:
                            return metadata.hasAlpha() && header.channels > 4
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3, 4}, new int[] {0, 0, 0, 0, 0}, DataBuffer.TYPE_BYTE, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_BYTE, false, false);
                        case 16:
                            return metadata.hasAlpha() && header.channels > 4
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3, 4}, new int[] {0, 0, 0, 0, 0}, DataBuffer.TYPE_USHORT, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_USHORT, false, false);
                        case 32:
                            return metadata.hasAlpha() && header.channels > 4
                                   ? ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3, 4}, new int[] {0, 0, 0, 0, 0}, DataBuffer.TYPE_INT, true, false)
                                   : ImageTypeSpecifiers.createBanded(cs, new int[] {0, 1, 2, 3}, new int[] {0, 0, 0, 0}, DataBuffer.TYPE_INT, false, false);

                    }
                }

                throw new IIOException(String.format("Unsupported channel count/bit depth for CMYK PSD: %d channels/%d bits", header.channels, header.bits));

            case PSD.COLOR_MODE_MULTICHANNEL:
                // TODO: Implement
            case PSD.COLOR_MODE_LAB:
                // TODO: Implement
                // TODO: If there's a color profile embedded, it should be easy, otherwise we're out of luck...
                // TODO: See the LAB color handling in TIFF
            default:
                throw new IIOException(String.format("Unsupported PSD MODE: %s (%d channels/%d bits)", header.mode, header.channels, header.bits));
        }
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        // TODO: Check out the custom ImageTypeIterator and ImageTypeProducer used in the Sun provided JPEGImageReader
        // Could use similar concept to create lazily-created ImageTypeSpecifiers (util candidate, based on FilterIterator?)

        // Get the raw type. Will fail for unsupported types
        ImageTypeSpecifier rawType = getRawImageTypeInternal(imageIndex);

        ColorSpace cs = rawType.getColorModel().getColorSpace();
        List<ImageTypeSpecifier> types = new ArrayList<>();

        switch (header.mode) {
            case PSD.COLOR_MODE_GRAYSCALE:
                if (rawType.getNumBands() == 1 && rawType.getBitsPerBand(0) == 8) {
                    types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
                }
                else if (rawType.getNumBands() >= 2 && rawType.getBitsPerBand(0) == 8) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {1, 0}, DataBuffer.TYPE_BYTE, true, false));
                }
                else if (rawType.getNumBands() == 1 && rawType.getBitsPerBand(0) == 16) {
                    types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_GRAY));
                }
                else if (rawType.getNumBands() >= 2 && rawType.getBitsPerBand(0) == 16) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {1, 0}, DataBuffer.TYPE_USHORT, true, false));
                }
                break;
            case PSD.COLOR_MODE_RGB:
                // Prefer interleaved versions as they are much faster to display
                if (rawType.getNumBands() == 3 && rawType.getBitsPerBand(0) == 8) {
                    // TODO: Integer raster
                    // types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.INT_RGB));
                    types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));

                    if (!cs.isCS_sRGB()) {
                        // Basically BufferedImage.TYPE_3BYTE_BGR, with corrected ColorSpace. Possibly slow.
                        types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
                    }
                }
                else if (rawType.getNumBands() >= 4 && rawType.getBitsPerBand(0) == 8) {
                    // TODO: Integer raster
                    // types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.INT_ARGB));
                    types.add(ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR));

                    if (!cs.isCS_sRGB()) {
                        // Basically BufferedImage.TYPE_4BYTE_ABGR, with corrected ColorSpace. Possibly slow.
                        types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, true, false));
                    }
                }
                else if (rawType.getNumBands() == 3 && rawType.getBitsPerBand(0) == 16) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {2, 1, 0}, DataBuffer.TYPE_USHORT, false, false));
                }
                else if (rawType.getNumBands() >= 4 && rawType.getBitsPerBand(0) == 16) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_USHORT, true, false));
                }
                break;
            case PSD.COLOR_MODE_CMYK:
                // Prefer interleaved versions as they are much faster to display
                // TODO: We should convert these to their RGB equivalents while reading for the common-case,
                // as Java2D is extremely slow displaying custom images.
                // Converting to RGB is also correct behaviour, according to the docs.
                // Doing this, will require rewriting the image reading, as the raw image data is channelled, not interleaved :-/
                if (rawType.getNumBands() == 4 && rawType.getBitsPerBand(0) == 8) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
                }
                else if (rawType.getNumBands() == 5 && rawType.getBitsPerBand(0) == 8) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {4, 3, 2, 1, 0}, DataBuffer.TYPE_BYTE, true, false));
                }
                else if (rawType.getNumBands() == 4 && rawType.getBitsPerBand(0) == 16) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {3, 2, 1, 0}, DataBuffer.TYPE_USHORT, false, false));
                }
                else if (rawType.getNumBands() == 5 && rawType.getBitsPerBand(0) == 16) {
                    types.add(ImageTypeSpecifiers.createInterleaved(cs, new int[] {4, 3, 2, 1, 0}, DataBuffer.TYPE_USHORT, true, false));
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

        if (colorSpace == null) {
            ICC_Profile profile = null;
            for (PSDImageResource resource : metadata.imageResources) {
                if (resource instanceof ICCProfile) {
                    profile = ((ICCProfile) resource).getProfile();
                    break;
                }
            }

            colorSpace = profile == null ? null : ColorSpaces.createColorSpace(profile);
        }

        return colorSpace;
    }

    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        checkBounds(imageIndex);

        // TODO: What about the extra alpha channels possibly present? Read as gray scale as extra images?

        // Layer hacks... For now, any index above 0 is considered to be a layer...
        // TODO: Support layer in index 0, if "has real merged data" flag is false?
        // TODO: Param support in layer code (more duping/cleanup..)
        if (imageIndex > 0) {
            return readLayerData(imageIndex - 1, param);
        }

        BufferedImage image = getDestination(param, getImageTypes(imageIndex), header.width, header.height);
        ImageTypeSpecifier rawType = getRawImageType(imageIndex);
        checkReadParamBandSettings(param, rawType.getNumBands(), image.getSampleModel().getNumBands());

        final Rectangle source = new Rectangle();
        final Rectangle dest = new Rectangle();
        computeRegions(param, header.width, header.height, image, source, dest);

        final int xSub;
        final int ySub;

        if (param == null) {
            xSub = ySub = 1;
        }
        else {
            xSub = param.getSourceXSubsampling();
            ySub = param.getSourceYSubsampling();
        }

        imageInput.seek(metadata.imageDataStart);
        int compression = imageInput.readShort();
        metadata.compression = compression;

        int[] byteCounts = null;
        switch (compression) {
            case PSD.COMPRESSION_NONE:
                break;
            case PSD.COMPRESSION_RLE:
                // NOTE: Byte counts will allow us to easily skip rows before AOI
                byteCounts = new int[header.channels * header.height];
                for (int i = 0; i < byteCounts.length; i++) {
                    byteCounts[i] = header.largeFormat ? imageInput.readInt() : imageInput.readUnsignedShort();
                }
                break;
            case PSD.COMPRESSION_ZIP:
            case PSD.COMPRESSION_ZIP_PREDICTION:
                // TODO: Could probably use the ZIPDecoder (DeflateDecoder) here.. Look at TIFF prediction reading
                throw new IIOException("PSD with ZIP compression not supported");
            default:
                throw new IIOException(
                        String.format(
                                "Unknown PSD compression: %d. Expected 0 (none), 1 (RLE), 2 (ZIP) or 3 (ZIP w/prediction).",
                                compression
                        )
                );
        }

        processImageStarted(imageIndex);

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

    private long findLayerStartPos(int layerIndex) {
        long layersStart = metadata.layersStart;

        for (int i = 0; i < layerIndex; i++) {
            PSDLayerInfo layerInfo = metadata.layerInfo.get(i);

            for (PSDChannelInfo channelInfo : layerInfo.channelInfo) {
                layersStart += channelInfo.length;
            }
        }

        return layersStart;
    }

    private void readImageData(final BufferedImage destination,
                               final ColorModel pSourceCM, final Rectangle pSource, final Rectangle pDest,
                               final int pXSub, final int pYSub,
                               final int[] pByteCounts, final int pCompression) throws IOException {

        WritableRaster destRaster = destination.getRaster();
        ColorModel destCM = destination.getColorModel();

        int channels = pSourceCM.createCompatibleSampleModel(1, 1).getNumBands();
        ImageTypeSpecifier singleBandRowSpec = ImageTypeSpecifiers.createGrayscale(header.bits, pSourceCM.getTransferType());
        WritableRaster rowRaster = singleBandRowSpec.createBufferedImage(header.width, 1).getRaster();
        boolean banded = destRaster.getDataBuffer().getNumBanks() > 1;
        int interleavedBands = banded ? 1 : destRaster.getNumBands();

        for (int c = 0; c < channels; c++) {
            int bandOffset = banded ? 0 : interleavedBands - 1 - c;

            switch (header.bits) {
                case 1:
                    byte[] row1 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                    read1bitChannel(c, channels, destRaster.getDataBuffer(), interleavedBands, bandOffset, pSourceCM, row1, pSource, pDest, pXSub, pYSub, header.width, header.height, pByteCounts, pCompression == PSD.COMPRESSION_RLE);
                    break;
                case 8:
                    byte[] row8 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                    read8bitChannel(c, channels, destRaster.getDataBuffer(), interleavedBands, bandOffset, pSourceCM, row8, pSource, pDest, pXSub, pYSub, header.width, header.height, pByteCounts, c * header.height, pCompression == PSD.COMPRESSION_RLE);
                    break;
                case 16:
                    short[] row16 = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
                    read16bitChannel(c, channels, destRaster.getDataBuffer(), interleavedBands, bandOffset, pSourceCM, row16, pSource, pDest, pXSub, pYSub, header.width, header.height, pByteCounts, c * header.height, pCompression == PSD.COMPRESSION_RLE);
                    break;
                case 32:
                    int[] row32 = ((DataBufferInt) rowRaster.getDataBuffer()).getData();
                    read32bitChannel(c, channels, destRaster.getDataBuffer(), interleavedBands, bandOffset, pSourceCM, row32, pSource, pDest, pXSub, pYSub, header.width, header.height, pByteCounts, c * header.height, pCompression == PSD.COMPRESSION_RLE);
                    break;
                default:
                    throw new IIOException(String.format("Unsupported PSD bit depth: %s", header.bits));
            }

            if (abortRequested()) {
                break;
            }
        }

        if (header.bits == 8) {
            // Compose out the background of the semi-transparent pixels, as PS somehow has the background composed in
            decomposeAlpha(destCM, destRaster.getDataBuffer(), pDest.width, pDest.height, destRaster.getNumBands());
        }

        // NOTE: ColorSpace uses Object.equals(), so we rely on using same instances!
        if (!pSourceCM.getColorSpace().equals(destCM.getColorSpace())) {
            convertToDestinationCS(pSourceCM, destCM, destRaster);
        }
    }

    private void convertToDestinationCS(final ColorModel sourceCM, ColorModel destinationCM, final WritableRaster raster) {
        long start = DEBUG ? System.currentTimeMillis() : 0;

        // Color conversion from embedded color space, to destination color space
        WritableRaster alphaMaskedRaster = destinationCM.hasAlpha()
                                           ? raster.createWritableChild(0, 0, raster.getWidth(), raster.getHeight(),
                raster.getMinX(), raster.getMinY(),
                createBandList(sourceCM.getColorSpace().getNumComponents()))
                                           : raster;

        new ColorConvertOp(sourceCM.getColorSpace(), destinationCM.getColorSpace(), null)
                .filter(alphaMaskedRaster, alphaMaskedRaster);

        if (DEBUG) {
            System.out.println("Color conversion " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    private int[] createBandList(final int numBands) {
        int[] bands = new int[numBands];

        for (int i = 0; i < numBands; i++) {
            bands[i] = i;
        }

        return bands;
    }

    private void processImageProgressForChannel(int channel, int channelCount, int y, int height) {
        processImageProgress(100f * channel / channelCount + 100f * y / (height * channelCount));
    }

    private void read32bitChannel(final int pChannel, final int pChannelCount,
                                  final DataBuffer pData, final int pBands, final int pBandOffset,
                                  final ColorModel pSourceColorModel,
                                  final int[] pRow,
                                  final Rectangle pSource, final Rectangle pDest,
                                  final int pXSub, final int pYSub,
                                  final int pChannelWidth, final int pChannelHeight,
                                  final int[] pRowByteCounts, final int pRowOffset,
                                  final boolean pRLECompressed) throws IOException {

        boolean isCMYK = pSourceColorModel.getColorSpace().getType() == ColorSpace.TYPE_CMYK;
        int colorComponents = pSourceColorModel.getColorSpace().getNumComponents();
        final boolean invert = isCMYK && pChannel < colorComponents;
        final boolean banded = pData.getNumBanks() > 1;

        for (int y = 0; y < pChannelHeight; y++) {
            int length = (pRLECompressed ? pRowByteCounts[pRowOffset + y] : 4 * pChannelWidth);

            // TODO: Sometimes need to read the line y == source.y + source.height...
            // Read entire line, if within source region and sampling
            if (y >= pSource.y && y < pSource.y + pSource.height && y % pYSub == 0) {
                if (pRLECompressed) {

                    try (DataInputStream input = PSDUtil.createPackBitsStream(imageInput, length)) {
                        for (int x = 0; x < pChannelWidth; x++) {
                            pRow[x] = input.readInt();
                        }
                    }
                }
                else {
                    imageInput.readFully(pRow, 0, pChannelWidth);
                }

                // TODO: Destination offset...??
                // Copy line sub sampled into real data
                int offset = (y - pSource.y) / pYSub * pDest.width * pBands + pBandOffset;
                for (int x = 0; x < pDest.width; x++) {
                    int value = pRow[pSource.x + x * pXSub];

                    // CMYK values are stored inverted, but alpha is not
                    if (invert) {
                        value = 0xffffffff - value;
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

    private void read16bitChannel(final int pChannel, final int pChannelCount,
                                  final DataBuffer pData, final int pBands, final int pBandOffset,
                                  final ColorModel pSourceColorModel,
                                  final short[] pRow,
                                  final Rectangle pSource, final Rectangle pDest,
                                  final int pXSub, final int pYSub,
                                  final int pChannelWidth, final int pChannelHeight,
                                  final int[] pRowByteCounts, final int pRowOffset,
                                  final boolean pRLECompressed) throws IOException {

        boolean isCMYK = pSourceColorModel.getColorSpace().getType() == ColorSpace.TYPE_CMYK;
        int colorComponents = pSourceColorModel.getColorSpace().getNumComponents();
        final boolean invert = isCMYK && pChannel < colorComponents;
        final boolean banded = pData.getNumBanks() > 1;

        for (int y = 0; y < pChannelHeight; y++) {
            int length = (pRLECompressed ? pRowByteCounts[pRowOffset + y] : 2 * pChannelWidth);

            // TODO: Sometimes need to read the line y == source.y + source.height...
            // Read entire line, if within source region and sampling
            if (y >= pSource.y && y < pSource.y + pSource.height && y % pYSub == 0) {
                if (pRLECompressed) {
                    try (DataInputStream input = PSDUtil.createPackBitsStream(imageInput, length)) {
                        for (int x = 0; x < pChannelWidth; x++) {
                            pRow[x] = input.readShort();
                        }
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
                    if (invert) {
                        value = (short) (0xffff - value & 0xffff);
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

        boolean isCMYK = pSourceColorModel.getColorSpace().getType() == ColorSpace.TYPE_CMYK;
        int colorComponents = pSourceColorModel.getColorSpace().getNumComponents();
        final boolean invert = isCMYK && pChannel < colorComponents;
        final boolean banded = pData.getNumBanks() > 1;

        for (int y = 0; y < pChannelHeight; y++) {
            int length = pRLECompressed ? pRowByteCounts[pRowOffset + y] : pChannelWidth;

            // TODO: Sometimes need to read the line y == source.y + source.height...
            // Read entire line, if within source region and sampling
            if (y >= pSource.y && y < pSource.y + pSource.height && y % pYSub == 0) {
                if (pRLECompressed) {
                    try (DataInputStream input = PSDUtil.createPackBitsStream(imageInput, length)) {
                        input.readFully(pRow, 0, pChannelWidth);
                    }
                }
                else {
                    imageInput.readFully(pRow, 0, pChannelWidth);
                }

                // TODO: Destination offset...??
                // Copy line sub sampled into real data
                int offset = (y - pSource.y) / pYSub * pDest.width * pBands + pBandOffset;
                for (int x = 0; x < pDest.width; x++) {
                    byte value = pRow[pSource.x + x * pXSub];

                    // CMYK values are stored inverted, but alpha is not
                    if (invert) {
                        value = (byte) (0xff - value & 0xff);
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
                    try (DataInputStream input = PSDUtil.createPackBitsStream(imageInput, length)) {
                        input.readFully(pRow, 0, pRow.length);
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
        // NOTE: It seems that the document background always white..?!
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

        if (header == null) {
            header = new PSDHeader(imageInput);

            if (!header.hasValidDimensions()) {
                processWarningOccurred(String.format("Dimensions exceed maximum allowed for %s: %dx%d (max %dx%d)",
                        header.largeFormat ? "PSB" : "PSD",
                        header.width, header.height, header.getMaxSize(), header.getMaxSize()));
            }

            metadata = new PSDMetadata();
            metadata.header = header;

            // Contains the required data to define the color mode.
            //
            // For indexed color images, the count will be equal to 768, and the mode data
            // will contain the color table for the image, in non-interleaved order.
            //
            // For duotone images, the mode data will contain the duotone specification,
            // the format of which is not documented.  Non-Photoshop readers can treat
            // the duotone image as a grayscale image, and keep the duotone specification
            // around as a black box for use when saving the file.
            if (header.mode == PSD.COLOR_MODE_INDEXED) {
                metadata.colorData = new PSDColorData(imageInput);
            }
            else {
                // TODO: We need to store the duotone spec if we decide to create a writer...
                // Skip color mode data for other modes
                long length = imageInput.readUnsignedInt();
                imageInput.skipBytes(length);
            }

            metadata.imageResourcesStart = imageInput.getStreamPosition();

            // Don't need the header again
            imageInput.flushBefore(imageInput.getStreamPosition());

            if (DEBUG) {
                System.out.println("header: " + header);
            }
        }
    }

    // TODO: Flags or list of interesting resources to parse
    // TODO: Obey ignoreMetadata
    private void readImageResources(final boolean pParseData) throws IOException {
        readHeader();

        if (pParseData && metadata.imageResources == null || metadata.layerAndMaskInfoStart == 0) {
            imageInput.seek(metadata.imageResourcesStart);

            long imageResourcesLength = imageInput.readUnsignedInt();

            if (pParseData && metadata.imageResources == null && imageResourcesLength > 0) {
                long expectedEnd = imageInput.getStreamPosition() + imageResourcesLength;
                metadata.imageResources = new ArrayList<>();

                while (imageInput.getStreamPosition() < expectedEnd) {
                    PSDImageResource resource = PSDImageResource.read(imageInput);
                    metadata.imageResources.add(resource);
                }

                if (DEBUG) {
                    System.out.println("imageResources: " + metadata.imageResources);
                }

                if (imageInput.getStreamPosition() != expectedEnd) {
                    throw new IIOException("Corrupt PSD document"); // ..or maybe just a bug in the reader.. ;-)
                }
            }

            // TODO: We should now be able to flush input
//                imageInput.flushBefore(metadata.imageResourcesStart + imageResourcesLength + 4);

            metadata.layerAndMaskInfoStart = metadata.imageResourcesStart + imageResourcesLength + 4; // + 4 for the length field itself
        }
    }

    // TODO: Flags or list of interesting resources to parse
    // TODO: Obey ignoreMetadata
    private void readLayerAndMaskInfo(final boolean pParseData) throws IOException {
        readImageResources(false);

        if (pParseData && (metadata.layerInfo == null || metadata.globalLayerMask == null) || metadata.imageDataStart == 0) {
            imageInput.seek(metadata.layerAndMaskInfoStart);

            long layerAndMaskInfoLength = header.largeFormat ? imageInput.readLong() : imageInput.readUnsignedInt();

            // NOTE: The spec says that if this section is empty, the length should be 0.
            // Yet I have a PSB file that has size 12, and both contained lengths set to 0 (which
            // is alo not as per spec, as layer count should be included if there's a layer info
            // block, so minimum size should be either 0 or 14 (or 16 if multiple of 4 for PSB))...

            if (layerAndMaskInfoLength > 0) {
                long pos = imageInput.getStreamPosition();

                //if (metadata.layerInfo == null) {
                long layerInfoLength = header.largeFormat ? imageInput.readLong() : imageInput.readUnsignedInt();

                if (layerInfoLength > 0) {
                    // "Layer count. If it is a negative number, its absolute value is the number of
                    // layers and the first alpha channel contains the transparency data for the
                    // merged result."
                    int layerCount = imageInput.readShort();
                    metadata.layerCount = layerCount;

                    if (pParseData && metadata.layerInfo == null) {
                        PSDLayerInfo[] layerInfos = new PSDLayerInfo[Math.abs(layerCount)];
                        for (int i = 0; i < layerInfos.length; i++) {
                            layerInfos[i] = new PSDLayerInfo(header.largeFormat, imageInput);
                        }

                        metadata.layerInfo = Arrays.asList(layerInfos);
                        metadata.layersStart = imageInput.getStreamPosition();

                    }

                    long read = imageInput.getStreamPosition() - pos;
                    long diff = layerInfoLength - (read - (header.largeFormat ? 8 : 4)); // - 8 or 4 for the layerInfoLength field itself

                    imageInput.skipBytes(diff);
                }
                else {
                    metadata.layerInfo = Collections.emptyList();
                }


                // Global LayerMaskInfo (18 bytes or more..?)
                // 4 (length), 2 (colorSpace), 8 (4 * 2 byte color components), 2 (opacity %), 1 (kind), variable (pad)
                long globalLayerMaskInfoLength = imageInput.readUnsignedInt(); // NOTE: Not long for PSB!

                if (globalLayerMaskInfoLength > 0) {
                    if (pParseData && metadata.globalLayerMask == null) {
                        metadata.globalLayerMask = new PSDGlobalLayerMask(imageInput, globalLayerMaskInfoLength);
                    }
                    // TODO: Else skip?
                }
                else {
                    metadata.globalLayerMask = PSDGlobalLayerMask.NULL_MASK;
                }

                // TODO: Parse "Additional layer information"

                // TODO: We should now be able to flush input
//                    imageInput.seek(metadata.layerAndMaskInfoStart + layerAndMaskInfoLength + (header.largeFormat ? 8 : 4));
//                    imageInput.flushBefore(metadata.layerAndMaskInfoStart + layerAndMaskInfoLength + (header.largeFormat ? 8 : 4));

                if (pParseData && DEBUG) {
                    System.out.println("layerInfo: " + metadata.layerInfo);
                    System.out.println("globalLayerMask: " + (metadata.globalLayerMask != PSDGlobalLayerMask.NULL_MASK ? metadata.globalLayerMask : null));
                }
                //}
            }

            metadata.imageDataStart = metadata.layerAndMaskInfoStart + layerAndMaskInfoLength + (header.largeFormat ? 8 : 4);
        }
    }

    private BufferedImage readLayerData(final int layerIndex, final ImageReadParam param) throws IOException {
        final int width = getLayerWidth(layerIndex);
        final int height = getLayerHeight(layerIndex);

        // TODO: This behaviour must be documented!
        // If layer has no pixel data, return null
        if (width <= 0 || height <= 0) {
            return null;
        }

        PSDLayerInfo layerInfo = metadata.layerInfo.get(layerIndex);

        // Even if raw/imageType has no alpha, the layers may still have alpha...
        ImageTypeSpecifier imageType = getRawImageTypeForLayer(layerIndex);
        BufferedImage layer = getDestination(param, getImageTypes(layerIndex + 1), Math.max(1, width), Math.max(1, height));

        imageInput.seek(findLayerStartPos(layerIndex));

        // Source/destination area
        Rectangle area = new Rectangle(width, height);

        final int xsub = 1;
        final int ysub = 1;

        final WritableRaster raster = layer.getRaster();
        final ColorModel destCM = layer.getColorModel();

        ColorModel sourceCM = imageType.getColorModel();
        final WritableRaster rowRaster = sourceCM.createCompatibleWritableRaster((int) Math.ceil(width / (double) sourceCM.getNumComponents()), 1);

        final boolean banded = raster.getDataBuffer().getNumBanks() > 1;
        final int interleavedBands = banded ? 1 : raster.getNumBands();

        // TODO: progress for layers!
        // TODO: Consider creating a method in PSDLayerInfo that can tell how many channels we really want to decode
        for (PSDChannelInfo channelInfo : layerInfo.channelInfo) {
            int compression = imageInput.readUnsignedShort();

            // Skip layer if we can't read it
            // channelId
            // -1 = transparency mask; -2 = user supplied layer mask, -3 = real user supplied layer mask (when both a user mask and a vector mask are present)
            if (channelInfo.channelId < -1 || (compression != PSD.COMPRESSION_NONE && compression != PSD.COMPRESSION_RLE)) { // TODO: ZIP Compressions!
                imageInput.skipBytes(channelInfo.length - 2);
            }
            else {
                // 0 = red, 1 = green, etc
                // -1 = transparency mask; -2 = user supplied layer mask, -3 = real user supplied layer mask (when both a user mask and a vector mask are present)
                int c = channelInfo.channelId == -1 ? rowRaster.getNumBands() - 1 : channelInfo.channelId;

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
                        // each count stored as a two*byte (four for PSB) value.
                        byteCounts = new int[layerInfo.bottom - layerInfo.top];
                        for (int i = 0; i < byteCounts.length; i++) {
                            byteCounts[i] = header.largeFormat ? imageInput.readInt() : imageInput.readUnsignedShort();
                        }

                        break;
                    case PSD.COMPRESSION_ZIP:
                    case PSD.COMPRESSION_ZIP_PREDICTION:
                    default:
                        // Explicitly skipped above
                        throw new AssertionError(String.format("Unsupported layer data. Compression: %d", compression));
                }

                int bandOffset = banded ? 0 : interleavedBands - 1 - c;

                switch (header.bits) {
                    case 1:
                        byte[] row1 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                        read1bitChannel(c, imageType.getNumBands(), raster.getDataBuffer(), interleavedBands, bandOffset, sourceCM, row1, area, area, xsub, ysub, width, height, byteCounts, compression == PSD.COMPRESSION_RLE);
                        break;
                    case 8:
                        byte[] row8 = ((DataBufferByte) rowRaster.getDataBuffer()).getData();
                        read8bitChannel(c, imageType.getNumBands(), raster.getDataBuffer(), interleavedBands, bandOffset, sourceCM, row8, area, area, xsub,
                                ysub, width, height, byteCounts, 0, compression == PSD.COMPRESSION_RLE);
                        break;
                    case 16:
                        short[] row16 = ((DataBufferUShort) rowRaster.getDataBuffer()).getData();
                        read16bitChannel(c, imageType.getNumBands(), raster.getDataBuffer(), interleavedBands, bandOffset, sourceCM, row16, area, area, xsub,
                                ysub, width, height, byteCounts, 0, compression == PSD.COMPRESSION_RLE);
                        break;
                    case 32:
                        int[] row32 = ((DataBufferInt) rowRaster.getDataBuffer()).getData();
                        read32bitChannel(c, imageType.getNumBands(), raster.getDataBuffer(), interleavedBands, bandOffset, sourceCM, row32, area, area, xsub,
                                ysub, width, height, byteCounts, 0, compression == PSD.COMPRESSION_RLE);
                        break;
                    default:
                        throw new IIOException(String.format("Unknown PSD bit depth: %s", header.bits));
                }

                if (abortRequested()) {
                    break;
                }
            }
        }

        if (!sourceCM.getColorSpace().equals(destCM.getColorSpace())) {
            convertToDestinationCS(sourceCM, destCM, raster);
        }

        return layer;
    }

    private ImageTypeSpecifier getRawImageTypeForLayer(final int layerIndex) throws IOException {
        ImageTypeSpecifier compositeType = getRawImageTypeForCompositeLayer();

        PSDLayerInfo layerInfo = metadata.layerInfo.get(layerIndex);

        // If layer has more channels than composite data, it's normally extra alpha...
        if (layerInfo.channelInfo.length > compositeType.getNumBands()) {
            // ...but, it could also be just one of the user masks...
            int newBandNum = 0;

            for (PSDChannelInfo channelInfo : layerInfo.channelInfo) {
                // -2 = user supplied layer mask, -3 real user supplied layer mask (when both a user mask and a vector mask are present)
                if (channelInfo.channelId >= -1) {
                    newBandNum++;
                }
            }

            // If there really is more channels, then create new imageTypeSpec
            if (newBandNum > compositeType.getNumBands()) {
                int[] indices = new int[newBandNum];
                for (int i = 0, indicesLength = indices.length; i < indicesLength; i++) {
                    indices[i] = i;
                }

                int[] offs = new int[newBandNum];
                for (int i = 0, offsLength = offs.length; i < offsLength; i++) {
                    offs[i] = 0;
                }

                return ImageTypeSpecifiers.createBanded(compositeType.getColorModel().getColorSpace(), indices, offs, compositeType.getSampleModel().getDataType(), true, false);
            }
        }

        return compositeType;
    }

    /// Layer support

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        // NOTE: Spec says this method should throw IllegalStateException if allowSearch && isSeekForwardOnly()
        // But that makes no sense for a format (like PSD) that does not need to search, right?
        readLayerAndMaskInfo(false);

        return metadata.getLayerCount() + 1; // TODO: Only plus one, if "has real merged data"?
    }

    /// Metadata support

    @Override
    public IIOMetadata getStreamMetadata() throws IOException {
        // null might be appropriate here
        // "For image formats that contain a single image, only image metadata is used."
        return super.getStreamMetadata();
    }

    @Override
    public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        checkBounds(imageIndex);

        readImageResources(true);
        readLayerAndMaskInfo(true);

        // NOTE: Need to make sure compression is set in metadata, even without reading the image data!
        // TODO: Move this to readLayerAndMaskInfo?
        if (metadata.compression == -1) {
            imageInput.seek(metadata.imageDataStart);
            metadata.compression = imageInput.readShort();
        }

        // Initialize XMP data etc.
        for (PSDImageResource resource : metadata.imageResources) {
            if (resource instanceof PSDDirectoryResource) {
                PSDDirectoryResource directoryResource = (PSDDirectoryResource) resource;

                try {
                    directoryResource.initDirectory();
                }
                catch (IOException e) {
                    processWarningOccurred(String.format("Error parsing %s: %s", resource.getClass().getSimpleName(), e.getMessage()));
                }
            }
        }

        return metadata; // TODO: clone if we change to mutable metadata
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

    private List<PSDThumbnail> getThumbnailResources(final int imageIndex) throws IOException {
        checkBounds(imageIndex);

        if (imageIndex > 0) {
            return null;
        }

        readHeader();

        List<PSDThumbnail> thumbnails = null;

        if (metadata.imageResources == null) {
            // TODO: Need flag here, to specify what resources to read...
            readImageResources(true);
        }

        for (PSDImageResource resource : metadata.imageResources) {
            if (resource instanceof PSDThumbnail) {
                if (thumbnails == null) {
                    thumbnails = new ArrayList<>();
                }

                thumbnails.add((PSDThumbnail) resource);
            }
        }

        return thumbnails;
    }

    @Override
    public int getNumThumbnails(final int imageIndex) throws IOException {
        List<PSDThumbnail> thumbnails = getThumbnailResources(imageIndex);

        return thumbnails == null ? 0 : thumbnails.size();
    }

    private PSDThumbnail getThumbnailResource(final int imageIndex, final int thumbnailIndex) throws IOException {
        List<PSDThumbnail> thumbnails = getThumbnailResources(imageIndex);

        if (thumbnails == null) {
            throw new IndexOutOfBoundsException(String.format("image index %d > 0", imageIndex));
        }

        return thumbnails.get(thumbnailIndex);
    }

    @Override
    public int getThumbnailWidth(final int imageIndex, final int thumbnailIndex) throws IOException {
        return getThumbnailResource(imageIndex, thumbnailIndex).getWidth();
    }

    @Override
    public int getThumbnailHeight(final int imageIndex, final int thumbnailIndex) throws IOException {
        return getThumbnailResource(imageIndex, thumbnailIndex).getHeight();
    }

    @Override
    public BufferedImage readThumbnail(final int imageIndex, final int thumbnailIndex) throws IOException {
        PSDThumbnail thumbnail = getThumbnailResource(imageIndex, thumbnailIndex);

        // TODO: It's possible to attach listeners to the ImageIO reader delegate... But do we really care?
        processThumbnailStarted(imageIndex, thumbnailIndex);
        processThumbnailProgress(0);
        BufferedImage image = thumbnail.getThumbnail();
        processThumbnailProgress(100);
        processThumbnailComplete();

        return image;
    }

    /// Functional testing
    public static void main(final String[] pArgs) throws IOException {
        int subsampleFactor = 1;
        Rectangle sourceRegion = null;
        boolean readLayers = false;
        boolean readThumbnails = false;

        int idx = 0;
        while (pArgs[idx].charAt(0) == '-') {
            if (pArgs[idx].equals("-s") || pArgs[idx].equals("--subsampling")) {
                subsampleFactor = Integer.parseInt(pArgs[++idx]);
            }
            else if (pArgs[idx].equals("-r") || pArgs[idx].equals("--sourceregion")) {
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
            else if (pArgs[idx].equals("-l") || pArgs[idx].equals("--layers")) {
                readLayers = true;
            }
            else if (pArgs[idx].equals("-t") || pArgs[idx].equals("--thumbnails")) {
                readThumbnails = true;
            }
            else {
                System.err.println("Usage: java PSDImageReader [-s <subsample factor>] [-r [<x y>] <w h>] <image file>");
                System.exit(1);
            }

            idx++;
        }

        PSDImageReader imageReader = new PSDImageReader(null);

        for (; idx < pArgs.length; idx++) {
            File file = new File(pArgs[idx]);
            System.out.println();
            System.out.println("file: " + file.getAbsolutePath());

            ImageInputStream stream = ImageIO.createImageInputStream(file);
            imageReader.setInput(stream);
            imageReader.readHeader();
            System.out.println("imageReader.header: " + imageReader.header);

            imageReader.readImageResources(true);
            System.out.println("imageReader.imageResources: " + imageReader.metadata.imageResources);
            System.out.println();

            imageReader.readLayerAndMaskInfo(true);
            System.out.println("imageReader.layerInfo: " + imageReader.metadata.layerInfo);
/*
    //        System.out.println("imageReader.globalLayerMask: " + imageReader.globalLayerMask);
            System.out.println();

            IIOMetadata metadata = imageReader.getImageMetadata(0);
            Node node;
            XMLSerializer serializer;

            node = metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            serializer = new XMLSerializer(System.out, System.getProperty("file.encoding"));
            serializer.serialize(node, true);
            System.out.println();

            node = metadata.getAsTree(PSDMetadata.NATIVE_METADATA_FORMAT_NAME);
    //        serializer = new XMLSerializer(System.out, System.getProperty("file.encoding"));
            serializer.serialize(node, true);
*/
            if (readThumbnails && imageReader.hasThumbnails(0)) {
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
            System.out.println("read time: " + (System.currentTimeMillis() - start));
            System.out.println("image: " + image);

            if (image.getType() == BufferedImage.TYPE_CUSTOM) {
                try {
                    ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
                    GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
                    image = op.filter(image, gc.createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency()));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    image = ImageUtil.accelerate(image);
                }
                System.out.println("conversion time: " + (System.currentTimeMillis() - start));
                System.out.println("image: " + image);
            }

            showIt(image, file.getName());

            if (readLayers) {
                int images = imageReader.getNumImages(true);
                for (int i = 1; i < images; i++) {
                    start = System.currentTimeMillis();
                    BufferedImage layer = imageReader.read(i);

                    System.out.println("layer read time: " + (System.currentTimeMillis() - start));
                    System.err.println("layer: " + layer);

                    if (layer != null && layer.getType() == BufferedImage.TYPE_CUSTOM) {
                        try {
                            ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
                            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
                            layer = op.filter(layer, gc.createCompatibleImage(layer.getWidth(), layer.getHeight(), layer.getTransparency()));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            layer = ImageUtil.accelerate(layer);
                        }
                        System.out.println("layer conversion time: " + (System.currentTimeMillis() - start));
                        System.out.println("layer: " + layer);
                    }

                    showIt(layer, "layer " + i);
                }
            }
        }
    }
}
