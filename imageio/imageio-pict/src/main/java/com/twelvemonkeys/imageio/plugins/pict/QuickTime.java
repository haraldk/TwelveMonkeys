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

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * QuickTime
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <a href="mailto:matthias.wiesmann@a3.epfl.ch">Matthias Wiesmann</a> (original embedded QuickTime parsing)
 * @author last modified by $Author: haraldk$
 * @version $Id: QT.java,v 1.0 Feb 16, 2009 7:20:59 PM haraldk Exp$
 */
final class QuickTime {
    public static final String VENDOR_APPLE = "appl";

    // TODO: Consider SPI for this in the future, however not very likely
    private static final List<QTDecompressor> sDecompressors = Arrays.asList(
            new QTBMPDecompressor(),
            new QTRAWDecompressor(),
            // The GenericDecompressor must be the last in the list
            new QTGenericDecompressor()
    );

    /*
   Apple compressor id's (vendor 'appl'):

   http://developer.apple.com/DOCUMENTATION/quicktime/Reference/QTRef_Constants/Reference/reference.html
   kAnimationCodecType          ='rle '
   kAVRJPEGCodecType            ='avr '
   kBaseCodecType               ='base'
   kBMPCodecType                ='WRLE' -> BMP without header, SUPPORTED
   kCinepakCodecType            ='cvid'
   kCloudCodecType              ='clou'
   kCMYKCodecType               ='cmyk' -> Is this raw CMYK data?
   kComponentVideoCodecType     ='yuv2'
   kComponentVideoSigned        ='yuvu'
   kComponentVideoUnsigned      ='yuvs'
   kDVCNTSCCodecType            ='dvc '
   kDVCPALCodecType             ='dvcp'
   kDVCProNTSCCodecType         ='dvpn'
   kDVCProPALCodecType          ='dvpp'
   kFireCodecType               ='fire'
   kFLCCodecType                ='flic'
   k48RGBCodecType              ='b48r' -> 48 bit (12 bpp) raw color data?
   kGIFCodecType                ='gif ' -> GIF, should work, but lacks test data
   kGraphicsCodecType           ='smc '
   kH261CodecType               ='h261'
   kH263CodecType               ='h263'
   kIndeo4CodecType             ='IV41'
   kJPEGCodecType               ='jpeg' -> JPEG, SUPPORTED
   kMacPaintCodecType           ='PNTG' -> Isn't this the PICT format itself? Does that make sense?! ;-)
   kMicrosoftVideo1CodecType    ='msvc'
   kMotionJPEGACodecType        ='mjpa'
   kMotionJPEGBCodecType        ='mjpb'
   kMpegYUV420CodecType         ='myuv'
   kOpenDMLJPEGCodecType        ='dmb1'
   kPhotoCDCodecType            ='kpcd' -> Could potentially use JMagick/JUI plugin
   kPlanarRGBCodecType          ='8BPS' -> Use (parts of) Photoshop plugin?
   kPNGCodecType                ='png ' -> PNG, SUPPORTED
   kQuickDrawCodecType          ='qdrw' -> QD?
   kQuickDrawGXCodecType        ='qdgx' -> QD?
   kRawCodecType                ='raw ' -> Raw (A)RGB pixel data
   kSGICodecType                ='.SGI'
   k16GrayCodecType             ='b16g' -> Raw 16 bit gray data?
   k64ARGBCodecType             ='b64a' -> Raw 64 bit (16 bpp) color data?
   kSorensonCodecType           ='SVQ1'
   kSorensonYUV9CodecType       ='syv9'
   kTargaCodecType              ='tga ' -> TGA, maybe create a plugin for that
   k32AlphaGrayCodecType        ='b32a' -> 16 bit gray + 16 bit alpha raw data?
   kTIFFCodecType               ='tiff' -> TIFF, SUPPORTED
   kVectorCodecType             ='path'
   kVideoCodecType              ='rpza'
   kWaterRippleCodecType        ='ripl'
   kWindowsRawCodecType         ='WRAW' -> Raw pixels with reverse byte order ((A)BGR vs (A)RGB)?
   kYUV420CodecType             ='y420'
    */

    /**
     * Gets a decompressor that can decompress the described data.
     *
     * @param pDescription the image description ({@code 'idsc'} Atom).
     * @return a decompressor that can decompress data decribed by the given {@link ImageDesc description},
     *         or {@code null} if no decompressor is found
     */
    private static QTDecompressor getDecompressor(final ImageDesc pDescription) {
        for (QTDecompressor decompressor : sDecompressors) {
            if (decompressor.canDecompress(pDescription)) {
                return decompressor;
            }
        }

        return null;
    }

    /**
     * Decompresses the QuickTime image data from the given stream.  
     *
     * @param pStream the image input stream
     * @return a {@link BufferedImage} containing the image data, or {@code null} if no decompressor is capable of
     *         decompressing the image.
     * @throws IOException if an I/O exception occurs during read
     */
    public static BufferedImage decompress(final ImageInputStream pStream) throws IOException {
        ImageDesc description = ImageDesc.read(pStream);

        if (PICTImageReader.DEBUG) {
            System.out.println(description);
        }

        QTDecompressor decompressor = getDecompressor(description);

        if (decompressor == null) {
            return null;
        }

        InputStream streamAdapter = IIOUtil.createStreamAdapter(pStream, description.dataSize);
        try {
           return decompressor.decompress(description, streamAdapter);
        }
        finally {
            streamAdapter.close();
        }
    }

    /**
     * Class representing the {@code 'idsc'} QuickTime Atom.
     */
    static final class ImageDesc /*extends QTAtom*/ {
        private static final int SIZE = 86;

        // 'idsc' Atom size
        int size;

        String compressorIdentifer;
        short version;
        short revision;
        String compressorVendor;

        int temporalQuality;    
        int spatialQuality;

        int width;
        int height;

        double horizontalRes;
        double verticalRes;

        // Size of image data following 'idsc'
        int dataSize;
        int frameCount;

        String compressorName;

        short depth;
        short colorLUTId;

        byte[] extraDesc;

        private ImageDesc() {}

        public static ImageDesc read(final DataInput pStream) throws IOException {
            // The following looks like the 'idsc' Atom (as described in the QuickTime File Format)
            ImageDesc description = new ImageDesc();

            description.size = pStream.readInt();
            description.compressorIdentifer = PICTUtil.readIdString(pStream);

            pStream.skipBytes(4); // Reserved, should be 0
            pStream.skipBytes(2); // Reserved, should be 0
            pStream.skipBytes(2); // Reserved, should be 0

            description.version = pStream.readShort(); // Major version, 0 if not applicable
            description.revision = pStream.readShort(); // Minor version, 0 if not applicable
            description.compressorVendor = PICTUtil.readIdString(pStream);

            description.temporalQuality = pStream.readInt(); // Temporal quality, 0 means "no temporal compression"
            description.spatialQuality = pStream.readInt(); // Spatial quality, 0x0000 0200 is codecNormalQuality

            description.width = pStream.readShort(); // Width (short)
            description.height = pStream.readShort(); // Height (short)

            description.horizontalRes = PICTUtil.readFixedPoint(pStream); // Horizontal resolution, FP, 0x0048 0000 means 72 DPI
            description.verticalRes = PICTUtil.readFixedPoint(pStream); // Vertical resolution, FP, 0x0048 0000 means 72 DPI

            // TODO: Handle 0 data size as unknown
            description.dataSize = pStream.readInt(); // Data size, may be 0, if unknown
            description.frameCount = pStream.readShort(); // Frame count

            description.compressorName = PICTUtil.readStr31(pStream); // Compresor name, 32 byte null-terminated Pascal String
            description.depth = pStream.readShort(); // Image bit depth
            description.colorLUTId = pStream.readShort(); // Color Lookup Table Id, -1 means none

            int extraDescSize = description.size - ImageDesc.SIZE;
            if (extraDescSize < 0) {
                throw new IIOException("Negative array size in 'idsc' Atom: " + extraDescSize);
            }
            description.extraDesc = new byte[extraDescSize];
            pStream.readFully(description.extraDesc);

            return description;
        }

        @Override
        public String toString() {
            return String.format("'idsc', size: %s, id: '%s', ver: %s, rev: %s, vendor: '%s', " +
//                    "tempQ: %s, spatQ: %s, " +
                    "w: %d, h: %d, " +
//                    "horiz: %s, vert: %s, " +
                    "data-size: %d, frame-count: %s, " +
                    "name: '%s', depth: %d, lut: %s, extra: %d",
                    size, compressorIdentifer, version, revision, compressorVendor,
//                    temporalQuality, spatialQuality,
                    width, height,
//                    horizontalRes, verticalRes,
                    dataSize, frameCount,
                    compressorName, depth,
                    colorLUTId, extraDesc != null ? extraDesc.length : 0
            );
        }
    }
}
