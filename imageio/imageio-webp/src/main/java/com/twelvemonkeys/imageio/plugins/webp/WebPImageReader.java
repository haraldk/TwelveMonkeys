/*
 * Copyright (c) 2017, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.webp;

import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorProfiles;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.metadata.xmp.XMPReader;
import com.twelvemonkeys.imageio.plugins.webp.lossless.VP8LDecoder;
import com.twelvemonkeys.imageio.plugins.webp.vp8.VP8Frame;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.imageio.util.RasterUtils;

/**
 * WebPImageReader
 */
final class WebPImageReader extends ImageReaderBase {

    final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.webp.debug"));

    private LSBBitReader lsbBitReader;

    // Either VP8_, VP8L or VP8X chunk
    private long fileSize;
    private VP8xChunk header;
    private ICC_Profile iccProfile;
    private final List<AnimationFrame> frames = new ArrayList<>();

    WebPImageReader(ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        fileSize = -1;
        header = null;
        iccProfile = null;
        lsbBitReader = null;
        frames.clear();
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        lsbBitReader = new LSBBitReader(imageInput);
    }

    private void readHeader(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        readHeader();

        if (header.containsANIM) {
            readFrame(imageIndex);
        }
    }

    private void readFrame(int frameIndex) throws IOException {
        if (!header.containsANIM) {
            throw new IndexOutOfBoundsException("imageIndex >= 1 for non-animated WebP: " + frameIndex);
        }

        if (frameIndex < frames.size()) {
            return;
        }

        // Note: Always extended format if we have animation
        // Seek to last frame, or end of header if no frames...
        RIFFChunk frame = frames.isEmpty() ? header : frames.get(frames.size() - 1);
        imageInput.seek(frame.offset + frame.length);

        while (imageInput.getStreamPosition() < fileSize) {
            int nextChunk = imageInput.readInt();
            long chunkLength = imageInput.readUnsignedInt();
            long chunkStart = imageInput.getStreamPosition();

            if (DEBUG) {
                System.out.printf("chunk: '%s'\n", fourCC(nextChunk));
                System.out.println("chunkLength: " + chunkLength);
                System.out.println("chunkStart: " + chunkStart);
            }

            switch (nextChunk) {
                case WebP.CHUNK_ANIM:
                    // TODO: 32 bit bg color (hint!) + 16 bit loop count
                    //  + expose bg color in std image metadata...
                    break;

                case WebP.CHUNK_ANMF:
                    // TODO: Expose x/y offset in std image metadata
                    int x = 2 * (int) lsbBitReader.readBits(24); // Might be more efficient to read as 3 bytes...
                    int y = 2 * (int) lsbBitReader.readBits(24);
                    int w = 1 + (int) lsbBitReader.readBits(24);
                    int h = 1 + (int) lsbBitReader.readBits(24);

                    Rectangle bounds = new Rectangle(x, y, w, h);

                    // TODO: Expose duration/flags in image metadata
                    int duration = (int) imageInput.readBits(24);
                    int flags = imageInput.readUnsignedByte(); // 6 bit reserved + blend mode + disposal mode

                    frames.add(new AnimationFrame(chunkLength, chunkStart, bounds, duration, flags));

                    break;

                default:
                    // Skip
                    break;
            }

            if (frameIndex < frames.size()) {
                return;
            }

            imageInput.seek(chunkStart + chunkLength + (chunkLength & 1)); // Padded to even length
        }

        throw new IndexOutOfBoundsException(String.format("imageIndex > %d: %d", frames.size(), frameIndex));
    }

    private void readHeader() throws IOException {
        if (header != null) {
            return;
        }

        // TODO: Generalize RIFF chunk parsing! Visitor?

        // RIFF native order is Little Endian
        imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        imageInput.seek(0);

        int riff = imageInput.readInt();
        if (riff != WebP.RIFF_MAGIC) {
            throw new IIOException(String.format("Not a WebP file, invalid 'RIFF' magic: '%s'", fourCC(riff)));
        }

        fileSize = 8 + imageInput.readUnsignedInt(); // 8 + RIFF container length (LITTLE endian) == file size

        int webp = imageInput.readInt();
        if (webp != WebP.WEBP_MAGIC) {
            throw new IIOException(String.format("Not a WebP file, invalid 'WEBP' magic: '%s'", fourCC(webp)));
        }

        int chunk = imageInput.readInt();
        long chunkLen = imageInput.readUnsignedInt();

        header = new VP8xChunk(chunk, chunkLen, imageInput.getStreamPosition());

        switch (chunk) {
            case WebP.CHUNK_VP8_:
                //https://tools.ietf.org/html/rfc6386#section-9.1
                int frameType = lsbBitReader.readBit(); // 0 = key frame, 1 = inter frame (not used in WebP)

                if (frameType != 0) {
                    throw new IIOException("Unexpected WebP frame type, expected key frame (0): " + frameType);
                }

                int versionNumber = (int) lsbBitReader.readBits(3); // 0 - 3 = different profiles (see spec)
                int showFrame = lsbBitReader.readBit(); // 0 = don't show, 1 = show

                if (DEBUG) {
                    System.out.println("versionNumber: " + versionNumber);
                    System.out.println("showFrame: " + showFrame);
                }

                // 19 bit field containing the size of the first data partition in bytes
                lsbBitReader.readBits(19);

                // StartCode 0, 1, 2
                imageInput.readUnsignedByte();
                imageInput.readUnsignedByte();
                imageInput.readUnsignedByte();

                // (2 bits Horizontal Scale << 14) | Width (14 bits)
                int hBytes = imageInput.readUnsignedShort();
                header.width = (hBytes & 0x3fff);

                // (2 bits Vertical Scale << 14) | Height (14 bits)
                int vBytes = imageInput.readUnsignedShort();
                header.height = (vBytes & 0x3fff);

                break;

            case WebP.CHUNK_VP8L:
                byte signature = imageInput.readByte();
                if (signature != WebP.LOSSLESSS_SIG) {
                    throw new IIOException(String.format("Unexpected 'VP8L' signature, expected 0x0x%2x: 0x%2x", WebP.LOSSLESSS_SIG, signature));
                }

                header.isLossless = true;

                // 14 bit width, 14 bit height, 1 bit alpha, 3 bit version
                header.width = 1 + (int) lsbBitReader.readBits(14);
                header.height = 1 + (int) lsbBitReader.readBits(14);
                header.containsALPH = lsbBitReader.readBit() == 1;

                int version = (int) lsbBitReader.readBits(3);

                if (version != 0) {
                    throw new IIOException(String.format("Unexpected 'VP8L' version, expected 0: %d", version));
                }

                break;

            case WebP.CHUNK_VP8X:
                if (chunkLen != 10) {
                    throw new IIOException("Unexpected 'VP8X' chunk length, expected 10: " + chunkLen);
                }

                // RsV|I|L|E|X|A|R
                int reserved = (int) imageInput.readBits(2);
                if (reserved != 0) {
                    // Spec says SHOULD be 0
                    throw new IIOException(String.format("Unexpected 'VP8X' chunk reserved value, expected 0: %d", reserved));
                }

                header.containsICCP = imageInput.readBit() == 1;
                header.containsALPH = imageInput.readBit() == 1; // L -> aLpha
                header.containsEXIF = imageInput.readBit() == 1;
                header.containsXMP_ = imageInput.readBit() == 1;
                header.containsANIM = imageInput.readBit() == 1; // A -> Anim

                reserved = (int) imageInput.readBits(25); // 1 + 24 bits reserved
                if (reserved != 0) {
                    // Spec says SHOULD be 0
                    throw new IIOException(String.format("Unexpected 'VP8X' chunk reserved value, expected 0: %d", reserved));
                }

                // NOTE: Spec refers to this as *Canvas* size, as opposed to *Image* size for the lossless chunk
                header.width = 1 + (int) lsbBitReader.readBits(24);
                header.height = 1 + (int) lsbBitReader.readBits(24);

                if (header.containsICCP) {
                    // ICCP chunk must be first chunk, if present
                    while (iccProfile == null && imageInput.getStreamPosition() < fileSize) {
                        int nextChunk = imageInput.readInt();
                        long chunkLength = imageInput.readUnsignedInt();
                        long chunkStart = imageInput.getStreamPosition();

                        if (nextChunk == WebP.CHUNK_ICCP) {
                            iccProfile = ColorProfiles.readProfile(IIOUtil.createStreamAdapter(imageInput, chunkLength));
                        }
                        else {
                            processWarningOccurred(String.format("Expected 'ICCP' chunk, '%s' chunk encountered", fourCC(nextChunk)));
                        }

                        imageInput.seek(chunkStart + chunkLength + (chunkLength & 1)); // Padded to even length
                    }
                }

                break;

            default:
                throw new IIOException(String.format("Unknown WebP chunk: '%s'", fourCC(chunk)));
        }

        if (DEBUG) {
            System.out.println("file size: " + fileSize + " (stream length: " + imageInput.length() + ")");
            System.out.println("header: " + header);
        }
    }

    static String fourCC(int value) {
        // NOTE: Little Endian
        return new String(
                new byte[]{
                        (byte) ((value & 0x000000ff)       ),
                        (byte) ((value & 0x0000ff00) >>   8),
                        (byte) ((value & 0x00ff0000) >>  16),
                        (byte) ((value & 0xff000000) >>> 24),
                },
                StandardCharsets.US_ASCII
        );
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IOException {
        assertInput();
        readHeader();

        if (header.containsANIM && allowSearch) {
            if (isSeekForwardOnly()) {
                throw new IllegalStateException("Illegal combination of allowSearch with seekForwardOnly");
            }

            readAllFrames();
            return frames.size();
        }

        return header.containsANIM ? -1 : 1;
    }

    private void readAllFrames() throws IOException {
        try {
            readFrame(Integer.MAX_VALUE);
        }
        catch (IndexOutOfBoundsException ignore) {}
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readHeader(imageIndex);

        return header.containsANIM ? frames.get(imageIndex).bounds.width
                                   : header.width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readHeader(imageIndex);

        return header.containsANIM ? frames.get(imageIndex).bounds.height
                                   : header.height;
    }

    @Override
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        readHeader(imageIndex);

        if (iccProfile != null && !ColorProfiles.isCS_sRGB(iccProfile)) {
            ICC_ColorSpace colorSpace = ColorSpaces.createColorSpace(iccProfile);
            int[] bandOffsets = header.containsALPH ? new int[] {0, 1, 2, 3} : new int[] {0, 1, 2};
            return ImageTypeSpecifiers.createInterleaved(colorSpace, bandOffsets, DataBuffer.TYPE_BYTE, header.containsALPH, false);
        }

        return ImageTypeSpecifiers.createFromBufferedImageType(header.containsALPH ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        ImageTypeSpecifier rawImageType = getRawImageType(imageIndex);

        List<ImageTypeSpecifier> types = new ArrayList<>();

        if (rawImageType.getBufferedImageType() == BufferedImage.TYPE_CUSTOM) {
            types.add(ImageTypeSpecifiers.createFromBufferedImageType(header.containsALPH ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR));
        }

        types.add(rawImageType);
        types.add(ImageTypeSpecifiers.createFromBufferedImageType(header.containsALPH ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB));
        types.add(ImageTypeSpecifiers.createFromBufferedImageType(header.containsALPH ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_BGR));

        return types.iterator();
    }

    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        int width = getWidth(imageIndex);
        int height = getHeight(imageIndex);
        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);

        processImageStarted(imageIndex);

        switch (header.fourCC) {
            case WebP.CHUNK_VP8_:
                imageInput.seek(header.offset);
                readVP8(RasterUtils.asByteRaster(destination.getRaster()), param);

                break;

            case WebP.CHUNK_VP8L:
                imageInput.seek(header.offset);
                readVP8Lossless(RasterUtils.asByteRaster(destination.getRaster()), param);

                break;

            case WebP.CHUNK_VP8X:
                if (header.containsANIM) {
                    AnimationFrame frame = frames.get(imageIndex);
                    imageInput.seek(frame.offset + 16);
                    opaqueAlpha(destination.getAlphaRaster());
                    readVP8Extended(destination, param, frame.offset + frame.length);
                }
                else {
                    imageInput.seek(header.offset + header.length);
                    readVP8Extended(destination, param, fileSize);
                }

                break;

            default:
                throw new IIOException("Unknown first chunk for WebP: " + fourCC(header.fourCC));
        }

        applyICCProfileIfNeeded(destination);

        if (abortRequested()) {
            processReadAborted();
        }
        else {
            processImageComplete();
        }

        return destination;
    }

    private void readVP8Extended(BufferedImage destination, ImageReadParam param, long streamEnd) throws IOException {
        while (imageInput.getStreamPosition() < streamEnd) {
            int nextChunk = imageInput.readInt();
            long chunkLength = imageInput.readUnsignedInt();
            long chunkStart = imageInput.getStreamPosition();

            if (DEBUG) {
                System.out.printf("chunk: '%s'\n", fourCC(nextChunk));
                System.out.println("chunkLength: " + chunkLength);
                System.out.println("chunkStart: " + chunkStart);
            }

            switch (nextChunk) {
                case WebP.CHUNK_ALPH:
                    int reserved = (int) imageInput.readBits(2);
                    if (reserved != 0) {
                        // Spec says SHOULD be 0
                        processWarningOccurred(String.format("Unexpected 'ALPH' chunk reserved value, expected 0: %d", reserved));
                    }

                    int preProcessing = (int) imageInput.readBits(2);
                    int filtering = (int) imageInput.readBits(2);
                    int compression = (int) imageInput.readBits(2);

                    if (DEBUG) {
                        System.out.println("preProcessing: " + preProcessing);
                        System.out.println("filtering: " + filtering);
                        System.out.println("compression: " + compression);
                    }

                    switch (compression) {
                        case 0:
                            readUncompressedAlpha(destination.getAlphaRaster());
                            break;
                        case 1:
                            opaqueAlpha(destination.getAlphaRaster()); // TODO: Remove when correctly implemented!
//                            readVP8Lossless(destination.getAlphaRaster(), param);
                            break;
                        default:
                            processWarningOccurred("Unknown WebP alpha compression: " + compression);
                            opaqueAlpha(destination.getAlphaRaster());
                            break;
                    }

                    break;

                case WebP.CHUNK_VP8_:
                    readVP8(RasterUtils.asByteRaster(destination.getRaster())
                            .createWritableChild(0, 0, destination.getWidth(), destination.getHeight(), 0, 0, new int[]{ 0, 1, 2}), param);
                    break;

                case WebP.CHUNK_VP8L:
                    readVP8Lossless(RasterUtils.asByteRaster(destination.getRaster()), param);
                    break;

                case WebP.CHUNK_ANIM:
                case WebP.CHUNK_ANMF:
                    if (!header.containsANIM) {
                        processWarningOccurred("Ignoring unsupported chunk: " + fourCC(nextChunk));
                    }
                case WebP.CHUNK_ICCP:
                    // Ignore, we already read this
                case WebP.CHUNK_EXIF:
                case WebP.CHUNK_XMP_:
                    // Ignore, we'll read these later
                    break;

                default:
                    processWarningOccurred("Ignoring unexpected chunk: " + fourCC(nextChunk));
                    break;
            }

            imageInput.seek(chunkStart + chunkLength + (chunkLength & 1)); // Padded to even length
        }
    }

    private void applyICCProfileIfNeeded(final BufferedImage destination) {
        if (iccProfile != null) {
            ColorModel colorModel = destination.getColorModel();
            ICC_Profile destinationProfile = ((ICC_ColorSpace) colorModel.getColorSpace()).getProfile();

            if (!iccProfile.equals(destinationProfile)) {
                if (DEBUG) {
                    System.err.println("Converting from " + iccProfile + " to " + (ColorProfiles.isCS_sRGB(destinationProfile) ? "sRGB" : destinationProfile));
                }

                WritableRaster raster = colorModel.hasAlpha()
                                        ? destination.getRaster().createWritableChild(0, 0, destination.getWidth(), destination.getHeight(), 0, 0, new int[] {0, 1, 2})
                                        : destination.getRaster();

                new ColorConvertOp(new ICC_Profile[] {iccProfile, destinationProfile}, null)
                        .filter(raster, raster);
            }
        }
    }

    private void opaqueAlpha(final WritableRaster alphaRaster) {
        int h = alphaRaster.getHeight();
        int w = alphaRaster.getWidth();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                alphaRaster.setSample(x, y, 0, 0xff);
            }
        }
    }

    private void readUncompressedAlpha(final WritableRaster alphaRaster) throws IOException {
        // Hardly used in practice, need to find a sample file
        processWarningOccurred("Uncompressed WebP alpha not implemented");
        opaqueAlpha(alphaRaster);
    }

    private void readVP8Lossless(final WritableRaster raster, final ImageReadParam param) throws IOException {
        VP8LDecoder decoder = new VP8LDecoder(imageInput, DEBUG);
        decoder.readVP8Lossless(raster, true);
    }

    private void readVP8(final WritableRaster raster, final ImageReadParam param) throws IOException {
        VP8Frame frame = new VP8Frame(imageInput, DEBUG);

        frame.setProgressListener(new ProgressListenerBase() {
            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                processImageProgress(percentageDone);
            }
        });

        if (!frame.decode(raster, param)) {
            processWarningOccurred("Nothing to decode");
        }
    }

    // Metadata

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        readHeader(imageIndex);
        readMeta();

        return new WebPImageMetadata(header);
    }

    private void readMeta() throws IOException {
        if (header.containsEXIF || header.containsXMP_) {
            // TODO: WebP spec says possible EXIF and XMP chunks are always AFTER image data
            imageInput.seek(header.offset + header.length);

            while (imageInput.getStreamPosition() < fileSize) {
                int nextChunk = imageInput.readInt();
                long chunkLength = imageInput.readUnsignedInt();
                long chunkStart = imageInput.getStreamPosition();

//                System.err.printf("chunk: '%s'\n", fourCC(nextChunk));
//                System.err.println("chunkLength: " + chunkLength);
//                System.err.println("chunkStart: " + chunkStart);

                switch (nextChunk) {
                    case WebP.CHUNK_EXIF:
                        // TODO: Figure out if the following is correct
                        // The (only) sample image contains 'Exif\0\0', just like the JPEG APP1/Exif segment...
                        // However, I cannot see this documented anywhere? Probably this is bogus...
                        // For now, we'll support both cases for compatibility.
                        int skippedCount = 0;
                        byte[] bytes = new byte[6];
                        imageInput.readFully(bytes);
                        if (bytes[0] == 'E' && bytes[1] == 'x' && bytes[2] == 'i' && bytes[3] == 'f' && bytes[4] == 0 && bytes[5] == 0) {
                            skippedCount = 6;
                        }
                        else {
                            imageInput.seek(chunkStart);
                        }

                        SubImageInputStream input = new SubImageInputStream(imageInput, chunkLength - skippedCount);
                        Directory exif = new TIFFReader().read(input);

//                        Entry jpegOffsetTag = exif.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT);
//                        if (jpegOffsetTag != null) {
//                            // Wohoo.. There's a JPEG inside the EIXF inside the WEBP...
//                            long jpegOffset = ((Number) jpegOffsetTag.getValue()).longValue();
//                            input.seek(jpegOffset);
//                            BufferedImage thumb = ImageIO.read(new SubImageInputStream(input, chunkLength - jpegOffset));
//                            System.err.println("thumb: " + thumb);
//                            showIt(thumb, "EXIF thumb");
//                        }

                        if (DEBUG) {
                            System.out.println("exif: " + exif);
                        }

                        break;

                    case WebP.CHUNK_XMP_:
                        Directory xmp = new XMPReader().read(new SubImageInputStream(imageInput, chunkLength));

                        if (DEBUG) {
                            System.out.println("xmp: " + xmp);
                        }

                        break;

                    default:
                }

                imageInput.seek(chunkStart + chunkLength + (chunkLength & 1)); // Padded to even length
            }
        }
    }
}
