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

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.lang.Validate;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * A faster implementation of {@code IndexColorModel}, that is backed by an
 * inverse color-map, for fast look-ups.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/InverseColorMapIndexColorModel.java#1 $
 *
 */
public class InverseColorMapIndexColorModel extends IndexColorModel {

    protected int rgbs[];
    protected int mapSize;

    protected InverseColorMap inverseMap = null;
    private final static int ALPHA_THRESHOLD = 0x80;

    private int whiteIndex = -1;
    private final static int WHITE = 0x00FFFFFF;
    private final static int RGB_MASK = 0x00FFFFFF;

    /**
     * Creates an {@code InverseColorMapIndexColorModel} from an existing
     * {@code IndexColorModel}.
     *
     * @param pColorModel the color model to create from.
     * @throws IllegalArgumentException if {@code pColorModel} is {@code null}
     */
    public InverseColorMapIndexColorModel(final IndexColorModel pColorModel) {
        this(Validate.notNull(pColorModel, "color model"), getRGBs(pColorModel));
    }

    // NOTE: The pRGBs parameter is used to get around invoking getRGBs two
    // times. What is wrong with protected?!
    private InverseColorMapIndexColorModel(IndexColorModel pColorModel, int[] pRGBs) {
        super(pColorModel.getComponentSize()[0], pColorModel.getMapSize(), pRGBs, 0, pColorModel.getTransferType(), pColorModel.getValidPixels());

        rgbs = pRGBs;
        mapSize = rgbs.length;

        inverseMap = new InverseColorMap(rgbs);
        whiteIndex = getWhiteIndex();
    }

    /**
     * Creates a defensive copy of the RGB color map in the given
     * {@code IndexColorModel}.
     *
     * @param pColorModel the indexed color model to get RGB values from
     * @return the RGB color map
     */
    private static int[] getRGBs(IndexColorModel pColorModel) {
        int[] rgb = new int[pColorModel.getMapSize()];
        pColorModel.getRGBs(rgb);

        return rgb;
    }

    /**
     * Creates an {@code InverseColorMapIndexColorModel} from the given array
     * of RGB components, plus one transparent index.
     *
     * @param pNumBits the number of bits each pixel occupies
     * @param pSize the size of the color component arrays
     * @param pRGBs the array of packed RGB color components
     * @param pStart the starting offset of the first color component
     * @param pAlpha indicates whether alpha values are contained in {@code pRGBs}
     * @param pTransparentIndex the index of the transparent pixel
     * @param pTransferType the data type of the array used to represent pixels
     *
     * @throws IllegalArgumentException if bits is less than 1 or greater than 16,
     * or if size is less than 1
     *
     * @see IndexColorModel#IndexColorModel(int, int, int[], int, boolean, int, int)
     */
    public InverseColorMapIndexColorModel(int pNumBits, int pSize, int[] pRGBs, int pStart, boolean pAlpha, int pTransparentIndex, int pTransferType) {
        super(pNumBits, pSize, pRGBs, pStart, pAlpha, pTransparentIndex, pTransferType);
        rgbs = getRGBs(this);
        mapSize = rgbs.length;

        inverseMap = new InverseColorMap(rgbs, pTransparentIndex);
        whiteIndex = getWhiteIndex();
    }

    /**
     * Creates an {@code InverseColorMapIndexColorModel} from the given arrays
     * of red, green, and blue components, plus one transparent index.
     *
     * @param pNumBits the number of bits each pixel occupies
     * @param pSize the size of the color component arrays
     * @param pReds the array of red color components
     * @param pGreens the array of green color components
     * @param pBlues the array of blue color components
     * @param pTransparentIndex the index of the transparent pixel
     *
     * @throws IllegalArgumentException if bits is less than 1 or greater than 16,
     * or if size is less than 1
     *
     * @see IndexColorModel#IndexColorModel(int, int, byte[], byte[], byte[], int)
     */
    public InverseColorMapIndexColorModel(int pNumBits, int pSize, byte[] pReds, byte[] pGreens, byte[] pBlues, int pTransparentIndex) {
        super(pNumBits, pSize, pReds, pGreens, pBlues, pTransparentIndex);
        rgbs = getRGBs(this);
        mapSize = rgbs.length;

        inverseMap = new InverseColorMap(rgbs, pTransparentIndex);
        whiteIndex = getWhiteIndex();
    }

    /**
     * Creates an {@code InverseColorMapIndexColorModel} from the given arrays
     * of red, green, and blue components.
     *
     * @param pNumBits the number of bits each pixel occupies
     * @param pSize the size of the color component arrays
     * @param pReds the array of red color components
     * @param pGreens the array of green color components
     * @param pBlues the array of blue color components
     *
     * @throws IllegalArgumentException if bits is less than 1 or greater than 16,
     * or if size is less than 1
     *
     * @see IndexColorModel#IndexColorModel(int, int, byte[], byte[], byte[])
     */
    public InverseColorMapIndexColorModel(int pNumBits, int pSize, byte[] pReds, byte[] pGreens, byte[] pBlues) {
        super(pNumBits, pSize, pReds, pGreens, pBlues);
        rgbs = getRGBs(this);
        mapSize = rgbs.length;

        inverseMap = new InverseColorMap(rgbs);
        whiteIndex = getWhiteIndex();
    }

    private int getWhiteIndex() {
        for (int i = 0; i < rgbs.length; i++) {
            int color = rgbs[i];
            if ((color & RGB_MASK) == WHITE) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Creates an {@code IndexColorModel} optimized for the given {@code Image}.
     *
     * @param pImage the {@code Image} containing the RGB samples
     * @param pNumCols the maximum number of colors in the {@code IndexColorModel}
     * @param pFlags flags
     *
     * @return a new optimized {@code IndexColorModel}
     */
    public static IndexColorModel create(Image pImage, int pNumCols, int pFlags) {
        // TODO: Inline and deprecate IndexImage.getIndexColorModel!?
        IndexColorModel icm = IndexImage.getIndexColorModel(pImage, pNumCols, pFlags);

        InverseColorMapIndexColorModel cm;
        if (icm instanceof InverseColorMapIndexColorModel) {
            cm = (InverseColorMapIndexColorModel) icm;
        }
        else {
            cm = new InverseColorMapIndexColorModel(icm);
        }

        return cm;
    }

    /**
     * Returns a data element array representation of a pixel in this
     * ColorModel, given an integer pixel representation in the
     * default RGB color model.  This array can then be passed to the
     * {@link java.awt.image.WritableRaster#setDataElements(int, int, Object) setDataElements}
     * method of a {@link java.awt.image.WritableRaster} object.  If the pixel variable is
     * {@code null}, a new array is allocated.  If {@code pixel}
     * is not {@code null}, it must be
     * a primitive array of type {@code transferType}; otherwise, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is
     * thrown if {@code pixel} is not large enough to hold a pixel
     * value for this {@code ColorModel}.  The pixel array is returned.
     * <p>
     * Since {@code OpaqueIndexColorModel} can be subclassed, subclasses
     * inherit the implementation of this method and if they don't
     * override it then they throw an exception if they use an
     * unsupported {@code transferType}.
     *
     * #param rgb the integer pixel representation in the default RGB
     * color model
     * #param pixel the specified pixel
     * #return an array representation of the specified pixel in this
     *  {@code OpaqueIndexColorModel}.
     * #throws ClassCastException if {@code pixel}
     *  is not a primitive array of type {@code transferType}
     * #throws ArrayIndexOutOfBoundsException if
     *  {@code pixel} is not large enough to hold a pixel value
     *  for this {@code ColorModel}
     * #throws UnsupportedOperationException if {@code transferType}
     *         is invalid
     * @see java.awt.image.WritableRaster#setDataElements
     * @see java.awt.image.SampleModel#setDataElements
     *
     */
    public Object getDataElements(int rgb, Object pixel) {
        int alpha = (rgb>>>24);

        int pix;
        if (alpha < ALPHA_THRESHOLD && getTransparentPixel() != -1) {
            pix = getTransparentPixel();
        }
        else {
            int color = rgb & RGB_MASK;
            if (color == WHITE && whiteIndex != -1) {
                pix = whiteIndex;
            }
            else {
                pix = inverseMap.getIndexNearest(color);
            }
        }

        return installpixel(pixel, pix);
    }

    private Object installpixel(Object pixel, int pix) {
        switch (transferType) {
            case DataBuffer.TYPE_INT:
                int[] intObj;
                if (pixel == null) {
                    pixel = intObj = new int[1];
                }
                else {
                    intObj = (int[]) pixel;
                }
                intObj[0] = pix;
                break;
            case DataBuffer.TYPE_BYTE:
                byte[] byteObj;
                if (pixel == null) {
                    pixel = byteObj = new byte[1];
                }
                else {
                    byteObj = (byte[]) pixel;
                }
                byteObj[0] = (byte) pix;
                break;
            case DataBuffer.TYPE_USHORT:
                short[] shortObj;
                if (pixel == null) {
                    pixel = shortObj = new short[1];
                }
                else {
                    shortObj = (short[]) pixel;
                }
                shortObj[0] = (short) pix;
                break;
            default:
                throw new UnsupportedOperationException("This method has not been implemented for transferType " + transferType);
        }
        return pixel;
    }

    public String toString() {
        // Just a workaround to ease debugging
        return StringUtil.replace(super.toString(), "IndexColorModel: ", getClass().getName() + ": ");
    }
}
