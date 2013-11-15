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

package com.twelvemonkeys.image;

import magick.*;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;

/**
 * Utility for converting JMagick {@code MagickImage}s to standard Java
 * {@code BufferedImage}s and back.
 * <p/>
 * <em>NOTE: This class is considered an implementation detail and not part of
 * the public API. This class is subject to change without further notice. 
 * You have been warned. :-)</em>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/MagickUtil.java#4 $
 */
public final class MagickUtil {
    // IMPORTANT NOTE: Disaster happens if any of these constants are used outside this class
    // because you then have a dependency on MagickException (this is due to Java class loading
    // and initialization magic).
    // Do not use outside this class. If the constants need to be shared, move to Magick or ImageUtil.

    /** Color Model usesd for bilevel (B/W) */
    private static final IndexColorModel CM_MONOCHROME = MonochromeColorModel.getInstance();

    /** Color Model usesd for raw ABGR */
    private static final ColorModel CM_COLOR_ALPHA =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 8},
                            true, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

    /** Color Model usesd for raw BGR */
    private static final ColorModel CM_COLOR_OPAQUE =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8},
                            false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    /** Color Model usesd for raw RGB */
    //private static final ColorModel CM_COLOR_RGB = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x0);

    /** Color Model usesd for raw GRAY + ALPHA */
    private static final ColorModel CM_GRAY_ALPHA =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                            true, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

    /** Color Model usesd for raw GRAY */
    private static final ColorModel CM_GRAY_OPAQUE =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                            false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    /** Band offsets for raw ABGR */
    private static final int[] BAND_OFF_TRANS = new int[] {3, 2, 1, 0};

    /** Band offsets for raw BGR */
    private static final int[] BAND_OFF_OPAQUE = new int[] {2, 1, 0};

    /** The point at {@code 0, 0} */
    private static final Point LOCATION_UPPER_LEFT = new Point(0, 0);

    private static final boolean DEBUG = Magick.DEBUG;

    // Only static members and methods
    private MagickUtil() {}

    /**
     * Converts a {@code MagickImage} to a {@code BufferedImage}.
     * <p/>
     * The conversion depends on {@code pImage}'s {@code ImageType}:
     * <dl>
     * <dt>{@code ImageType.BilevelType}</dt>
     * <dd>{@code BufferedImage} of type {@code TYPE_BYTE_BINARY}</dd>
     *
     * <dt>{@code ImageType.GrayscaleType}</dt>
     * <dd>{@code BufferedImage} of type {@code TYPE_BYTE_GRAY}</dd>
     * <dt>{@code ImageType.GrayscaleMatteType}</dt>
     * <dd>{@code BufferedImage} of type {@code TYPE_USHORT_GRAY}</dd>
     *
     * <dt>{@code ImageType.PaletteType}</dt>
     * <dd>{@code BufferedImage} of type {@code TYPE_BYTE_BINARY} (for images
     * with a palette of <= 16 colors) or {@code TYPE_BYTE_INDEXED}</dd>
     * <dt>{@code ImageType.PaletteMatteType}</dt>
     * <dd>{@code BufferedImage} of type {@code TYPE_BYTE_BINARY} (for images
     * with a palette of <= 16 colors) or {@code TYPE_BYTE_INDEXED}</dd>
     *
     * <dt>{@code ImageType.TrueColorType}</dt>
     * <dd>{@code BufferedImage} of type {@code TYPE_3BYTE_BGR}</dd>
     * <dt>{@code ImageType.TrueColorPaletteType}</dt>
     * <dd>{@code BufferedImage} of type {@code TYPE_4BYTE_ABGR}</dd>
     *
     * @param pImage the original {@code MagickImage}
     * @return a new {@code BufferedImage}
     *
     * @throws IllegalArgumentException if {@code pImage} is {@code null}
     * or if the {@code ImageType} is not one mentioned above.
     * @throws MagickException if an exception occurs during conversion
     *
     * @see BufferedImage
     */
    public static BufferedImage toBuffered(MagickImage pImage) throws MagickException {
        if (pImage == null) {
            throw new IllegalArgumentException("image == null");
        }

        long start = 0L;
        if (DEBUG) {
            start = System.currentTimeMillis();
        }

        BufferedImage image = null;
        try {
            switch (pImage.getImageType()) {
                case ImageType.BilevelType:
                    image = bilevelToBuffered(pImage);
                    break;
                case ImageType.GrayscaleType:
                    image = grayToBuffered(pImage, false);
                    break;
                case ImageType.GrayscaleMatteType:
                    image = grayToBuffered(pImage, true);
                    break;
                case ImageType.PaletteType:
                    image = paletteToBuffered(pImage, false);
                    break;
                case ImageType.PaletteMatteType:
                    image = paletteToBuffered(pImage, true);
                    break;
                case ImageType.TrueColorType:
                    image = rgbToBuffered(pImage, false);
                    break;
                case ImageType.TrueColorMatteType:
                    image = rgbToBuffered(pImage, true);
                    break;
                case ImageType.ColorSeparationType:
                    image = cmykToBuffered(pImage, false);
                    break;
                case ImageType.ColorSeparationMatteType:
                    image = cmykToBuffered(pImage, true);
                    break;
                case ImageType.OptimizeType:
                default:
                    throw new IllegalArgumentException("Unknown JMagick image type: " + pImage.getImageType());
            }

        }
        finally {
            if (DEBUG) {
                long time = System.currentTimeMillis() - start;
                System.out.println("Converted JMagick image type: " + pImage.getImageType() + " to BufferedImage: " + image);
                System.out.println("Conversion to BufferedImage: " + time + " ms");
            }
        }

        return image;
    }

    /**
     * Converts a {@code BufferedImage} to a {@code MagickImage}.
     * <p/>
     * The conversion depends on {@code pImage}'s {@code ColorModel}:
     * <dl>
     * <dt>{@code IndexColorModel} with 1 bit b/w</dt>
     * <dd>{@code MagickImage} of type {@code ImageType.BilevelType}</dd>
     * <dt>{@code IndexColorModel} &gt; 1 bit,</dt>
     * <dd>{@code MagickImage} of type {@code ImageType.PaletteType}
     * or {@code MagickImage} of type {@code ImageType.PaletteMatteType}
     * depending on <tt>ColorModel.getAlpha()</dd>
     *
     * <dt>{@code ColorModel.getColorSpace().getType() == ColorSpace.TYPE_GRAY}</dt>
     * <dd>{@code MagickImage} of type {@code ImageType.GrayscaleType}
     * or {@code MagickImage} of type {@code ImageType.GrayscaleMatteType}
     * depending on <tt>ColorModel.getAlpha()</dd>
     *
     * <dt>{@code ColorModel.getColorSpace().getType() == ColorSpace.TYPE_RGB}</dt>
     * <dd>{@code MagickImage} of type {@code ImageType.TrueColorType}
     * or {@code MagickImage} of type {@code ImageType.TrueColorPaletteType}</dd>
     *
     * @param pImage the original {@code BufferedImage}
     * @return a new {@code MagickImage}
     *
     * @throws IllegalArgumentException if {@code pImage} is {@code null}
     * or if the {@code ColorModel} is not one mentioned above.
     * @throws MagickException if an exception occurs during conversion
     *
     * @see BufferedImage
     */
    public static MagickImage toMagick(BufferedImage pImage) throws MagickException {
        if (pImage == null) {
            throw new IllegalArgumentException("image == null");
        }

        long start = 0L;
        if (DEBUG) {
            start = System.currentTimeMillis();
        }

        try {
            ColorModel cm = pImage.getColorModel();
            if (cm instanceof IndexColorModel) {
                // Handles both BilevelType, PaletteType and PaletteMatteType
                return indexedToMagick(pImage, (IndexColorModel) cm, cm.hasAlpha());
            }

            switch (cm.getColorSpace().getType()) {
                case ColorSpace.TYPE_GRAY:
                    // Handles GrayType and GrayMatteType
                    return grayToMagick(pImage, cm.hasAlpha());
                case ColorSpace.TYPE_RGB:
                    // Handles TrueColorType and TrueColorMatteType
                    return rgbToMagic(pImage, cm.hasAlpha());
                case ColorSpace.TYPE_CMY:
                case ColorSpace.TYPE_CMYK:
                case ColorSpace.TYPE_HLS:
                case ColorSpace.TYPE_HSV:
                    // Other types not supported yet
                default:
                    throw new IllegalArgumentException("Unknown buffered image type: " + pImage);
            }
        }
        finally {
            if (DEBUG) {
                long time = System.currentTimeMillis() - start;
                System.out.println("Conversion to MagickImage: " + time + " ms");
            }
        }
    }

    private static MagickImage rgbToMagic(BufferedImage pImage, boolean pAlpha) throws MagickException {
        MagickImage image = new MagickImage();

        BufferedImage buffered = ImageUtil.toBuffered(pImage, pAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);

        // Need to get data of sub raster, not the full data array, this is
        // just a convenient way
        Raster raster;
        if (buffered.getRaster().getParent() != null) {
            raster = buffered.getData(new Rectangle(buffered.getWidth(), buffered.getHeight()));
        }
        else {
            raster = buffered.getRaster();
        }

        image.constituteImage(buffered.getWidth(), buffered.getHeight(), pAlpha ? "ABGR" : "BGR",
                              ((DataBufferByte) raster.getDataBuffer()).getData());

        return image;
    }

    private static MagickImage grayToMagick(BufferedImage pImage, boolean pAlpha) throws MagickException {
        MagickImage image = new MagickImage();

        // TODO: Make a fix for TYPE_USHORT_GRAY
        // The code below does not seem to work (JMagick issues?)...
        /*
        if (pImage.getType() == BufferedImage.TYPE_USHORT_GRAY) {
            short[] data = ((DataBufferUShort) pImage.getRaster().getDataBuffer()).getData();
            int[] intData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                intData[i] = (data[i] & 0xffff) * 0xffff;
            }
            image.constituteImage(pImage.getWidth(), pImage.getHeight(), "I", intData);

            System.out.println("storageClass: " + image.getStorageClass());
            System.out.println("depth: " + image.getDepth());
            System.out.println("imageType: " + image.getImageType());
        }
        else {
        */
        BufferedImage buffered = ImageUtil.toBuffered(pImage, pAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_BYTE_GRAY);

        // Need to get data of sub raster, not the full data array, this is
        // just a convenient way
        Raster raster;
        if (buffered.getRaster().getParent() != null) {
            raster = buffered.getData(new Rectangle(buffered.getWidth(), buffered.getHeight()));
        }
        else {
            raster = buffered.getRaster();
        }

        image.constituteImage(buffered.getWidth(), buffered.getHeight(), pAlpha ? "ABGR" : "I", ((DataBufferByte) raster.getDataBuffer()).getData());
        //}

        return image;
    }

    private static MagickImage indexedToMagick(BufferedImage pImage, IndexColorModel pColorModel, boolean pAlpha) throws MagickException {
        MagickImage image = rgbToMagic(pImage, pAlpha);

        int mapSize = pColorModel.getMapSize();
        image.setNumberColors(mapSize);

        return image;
    }

    /*
    public static MagickImage toMagick(BufferedImage pImage) throws MagickException {
        if (pImage == null) {
            throw new IllegalArgumentException("image == null");
        }
        
        final int width = pImage.getWidth();
        final int height = pImage.getHeight();

        // int ARGB -> byte RGBA conversion
        // NOTE: This is ImageMagick Q16 compatible raw RGBA format with 16 bits/sample...
        //       For a Q8 build, we could probably go with half the space...
        // NOTE: This is close to insanity, as it wastes extreme ammounts of memory
        final int[] argb = new int[width];
        final byte[] raw16 = new byte[width * height * 8];
        for (int y = 0; y < height; y++) {
            // Fetch one line of ARGB data
            pImage.getRGB(0, y, width, 1, argb, 0, width);

            for (int x = 0; x < width; x++) {
                int pixel = (x + (y * width)) * 8;
                raw16[pixel    ] = (byte) ((argb[x] >> 16) & 0xff); // R
                raw16[pixel + 2] = (byte) ((argb[x] >>  8) & 0xff); // G
                raw16[pixel + 4] = (byte) ((argb[x]      ) & 0xff); // B
                raw16[pixel + 6] = (byte) ((argb[x] >> 24) & 0xff); // A
            }
        }

        // Create magick image
        ImageInfo info = new ImageInfo();
        info.setMagick("RGBA"); // Raw RGBA samples
        info.setSize(width + "x" + height); // String?!?

        MagickImage image = new MagickImage(info);
        image.setImageAttribute("depth", "8");

        // Set pixel data in 16 bit raw RGBA format
        image.blobToImage(info, raw16);

        return image;
    }
    */

    /**
     * Converts a bi-level {@code MagickImage} to a {@code BufferedImage}, of
     * type {@code TYPE_BYTE_BINARY}.
     *
     * @param pImage the original {@code MagickImage}
     * @return a new {@code BufferedImage}
     *
     * @throws MagickException if an exception occurs during conversion
     *
     * @see BufferedImage
     */
    private static BufferedImage bilevelToBuffered(MagickImage pImage) throws MagickException {
        // As there is no way to get the binary representation of the image,
        // convert to gray, and the create a binary image from it
        BufferedImage temp = grayToBuffered(pImage, false);

        BufferedImage image = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_BYTE_BINARY, CM_MONOCHROME);

        ImageUtil.drawOnto(image, temp);

        return image;
    }

    /**
     * Converts a gray {@code MagickImage} to a {@code BufferedImage}, of
     * type {@code TYPE_USHORT_GRAY} or {@code TYPE_BYTE_GRAY}.
     *
     * @param pImage the original {@code MagickImage}
     * @param pAlpha keep alpha channel
     * @return a new {@code BufferedImage}
     *
     * @throws MagickException if an exception occurs during conversion
     *
     * @see BufferedImage
     */
    private static BufferedImage grayToBuffered(MagickImage pImage, boolean pAlpha) throws MagickException {
        Dimension size = pImage.getDimension();
        int length = size.width * size.height;
        int bands = pAlpha ? 2 : 1;
        byte[] pixels = new byte[length * bands];

        // TODO: Make a fix for 16 bit TYPE_USHORT_GRAY?!
        // Note: The ordering AI or I corresponds to BufferedImage
        // TYPE_CUSTOM and TYPE_BYTE_GRAY respectively
        pImage.dispatchImage(0, 0, size.width, size.height, pAlpha ? "AI" : "I", pixels);

        // Init databuffer with array, to avoid allocation of empty array
        DataBuffer buffer = new DataBufferByte(pixels, pixels.length);

        int[] bandOffsets = pAlpha ? new int[] {1, 0} : new int[] {0};

        WritableRaster raster =
                Raster.createInterleavedRaster(buffer, size.width, size.height,
                        size.width * bands, bands, bandOffsets, LOCATION_UPPER_LEFT);

        return new BufferedImage(pAlpha ? CM_GRAY_ALPHA : CM_GRAY_OPAQUE, raster, pAlpha, null);
    }

    /**
     * Converts a palette-based {@code MagickImage} to a
     * {@code BufferedImage}, of type {@code TYPE_BYTE_BINARY} (for images
     * with a palette of <= 16 colors) or {@code TYPE_BYTE_INDEXED}.
     *
     * @param pImage the original {@code MagickImage}
     * @param pAlpha keep alpha channel
     * @return a new {@code BufferedImage}
     *
     * @throws MagickException if an exception occurs during conversion
     *
     * @see BufferedImage
     */
    private static BufferedImage paletteToBuffered(MagickImage pImage, boolean pAlpha) throws MagickException {
        // Create indexcolormodel for the image
        IndexColorModel cm;

        try {
            cm = createIndexColorModel(pImage.getColormap(), pAlpha);
        }
        catch (MagickException e) {
            // NOTE: Some MagickImages incorrecly (?) reports to be paletteType,
            //       but does not have a colormap, this is a workaround.
            return rgbToBuffered(pImage, pAlpha);
        }

        // As there is no way to get the indexes of an indexed image, convert to
        // RGB, and the create an indexed image from it
        BufferedImage temp = rgbToBuffered(pImage, pAlpha);

        BufferedImage image;
        if (cm.getMapSize() <= 16) {
            image = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_BYTE_BINARY, cm);
        }
        else {
            image = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, cm);
        }

        // Create transparent background for images containing alpha
        if (pAlpha) {
            Graphics2D g = image.createGraphics();
            try {
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 0, temp.getWidth(), temp.getHeight());
            }
            finally {
                g.dispose();
            }
        }

        // NOTE: This is (surprisingly) much faster than using g2d.drawImage()..
        // (Tests shows 20-30ms, vs. 600-700ms on the same image)
        BufferedImageOp op = new CopyDither(cm);
        op.filter(temp, image);

        return image;
    }

    /**
     * Creates an {@code IndexColorModel} from an array of
     * {@code PixelPacket}s.
     *
     * @param pColormap the original colormap as a {@code PixelPacket} array
     * @param pAlpha keep alpha channel
     *
     * @return a new {@code IndexColorModel}
     */
    public static IndexColorModel createIndexColorModel(PixelPacket[] pColormap, boolean pAlpha) {
        int[] colors = new int[pColormap.length];

        // TODO: Verify if this is correct for alpha...?
        int trans = pAlpha ? colors.length - 1 : -1;

        //for (int i = 0; i < pColormap.length; i++) {
        for (int i = pColormap.length - 1; i != 0; i--) {
            PixelPacket color = pColormap[i];
            if (pAlpha) {
                colors[i] = (0xff - (color.getOpacity() & 0xff)) << 24 |
                            (color.getRed()     & 0xff) << 16 |
                            (color.getGreen()   & 0xff) <<  8 |
                            (color.getBlue()    & 0xff);
            }
            else {
                colors[i] = (color.getRed()     & 0xff) << 16 |
                            (color.getGreen()   & 0xff) <<  8 |
                            (color.getBlue()    & 0xff);
            }
        }

        return new InverseColorMapIndexColorModel(8, colors.length, colors, 0, pAlpha, trans, DataBuffer.TYPE_BYTE);
    }

    /**
     * Converts an (A)RGB {@code MagickImage} to a {@code BufferedImage}, of
     * type {@code TYPE_4BYTE_ABGR} or {@code TYPE_3BYTE_BGR}.
     *
     * @param pImage the original {@code MagickImage}
     * @param pAlpha keep alpha channel
     * @return a new {@code BufferedImage}
     *
     * @throws MagickException if an exception occurs during conversion
     *
     * @see BufferedImage
     */
    private static BufferedImage rgbToBuffered(MagickImage pImage, boolean pAlpha) throws MagickException {
        Dimension size = pImage.getDimension();
        int length = size.width * size.height;
        int bands = pAlpha ? 4 : 3;
        byte[] pixels = new byte[length * bands];

        // TODO: If we do multiple dispatches (one per line, typically), we could provide listener
        //       feedback. But it's currently a lot slower than fetching all the pixels in one go.

        // Note: The ordering ABGR or BGR corresponds to BufferedImage
        // TYPE_4BYTE_ABGR and TYPE_3BYTE_BGR respectively
        pImage.dispatchImage(0, 0, size.width, size.height, pAlpha ? "ABGR" : "BGR", pixels);

        // Init databuffer with array, to avoid allocation of empty array
        DataBuffer buffer = new DataBufferByte(pixels, pixels.length);

        int[] bandOffsets = pAlpha ? BAND_OFF_TRANS : BAND_OFF_OPAQUE;

        WritableRaster raster =
                Raster.createInterleavedRaster(buffer, size.width, size.height,
                        size.width * bands, bands, bandOffsets, LOCATION_UPPER_LEFT);

        return new BufferedImage(pAlpha ? CM_COLOR_ALPHA : CM_COLOR_OPAQUE, raster, pAlpha, null);
    }
	
	
	

    /**
     * Converts an {@code MagickImage} to a {@code BufferedImage} which holds an CMYK ICC profile
     *
     * @param pImage the original {@code MagickImage}
     * @param pAlpha keep alpha channel
     * @return a new {@code BufferedImage}
     *
     * @throws MagickException if an exception occurs during conversion
     *
     * @see BufferedImage
     */
    private static BufferedImage cmykToBuffered(MagickImage pImage, boolean pAlpha) throws MagickException {
		Dimension size = pImage.getDimension();
		int length = size.width * size.height;
		
		// Retreive the ICC profile
		ICC_Profile profile = ICC_Profile.getInstance(pImage.getColorProfile().getInfo());
		ColorSpace cs = new ICC_ColorSpace(profile);
		
		int bands = cs.getNumComponents() + (pAlpha ? 1 : 0);
		
		int[] bits = new int[bands];
		for (int i = 0; i < bands; i++) {
			bits[i] = 8;
		}

        ColorModel cm = pAlpha ?
                new ComponentColorModel(cs, bits, true, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE) :
                new ComponentColorModel(cs, bits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        byte[] pixels = new byte[length * bands];

		// TODO: If we do multiple dispatches (one per line, typically), we could provide listener
		//       feedback. But it's currently a lot slower than fetching all the pixels in one go.
		// TODO: handle more generic cases if profile is not CMYK
		// TODO: Test "ACMYK"
		pImage.dispatchImage(0, 0, size.width, size.height, pAlpha ? "ACMYK" : "CMYK", pixels);

        // Init databuffer with array, to avoid allocation of empty array
        DataBuffer buffer = new DataBufferByte(pixels, pixels.length);

		// TODO: build array from bands variable, here it just works for CMYK
		// The values has not been tested with an alpha picture actually...
        int[] bandOffsets = pAlpha ? new int[] {0, 1, 2, 3, 4} : new int[] {0, 1, 2, 3};

        WritableRaster raster =
                Raster.createInterleavedRaster(buffer, size.width, size.height,
                        size.width * bands, bands, bandOffsets, LOCATION_UPPER_LEFT);
		
        return new BufferedImage(cm, raster, pAlpha, null);
		
    }
}
