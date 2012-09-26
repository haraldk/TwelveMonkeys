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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

/**
 * AreaAverageOp
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/AreaAverageOp.java#2 $
 */
public class AreaAverageOp implements BufferedImageOp, RasterOp {

    final private int width;
    final private int height;

    private Rectangle sourceRegion;

    public AreaAverageOp(final int pWidth, final int pHeight) {
        width = pWidth;
        height = pHeight;
    }

    public Rectangle getSourceRegion() {
        if (sourceRegion == null) {
            return null;
        }

        return new Rectangle(sourceRegion);
    }

    public void setSourceRegion(final Rectangle pSourceRegion) {
        if (pSourceRegion == null) {
            sourceRegion = null;
        }
        else {
            if (sourceRegion == null) {
                sourceRegion = new Rectangle(pSourceRegion);
            }
            else {
                sourceRegion.setBounds(pSourceRegion);
            }
        }
    }

    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage result = dest != null ? dest : createCompatibleDestImage(src, null);

        // TODO: src and dest can't be the same

        // TODO: Do some type checking here..
        // Should work with
        // * all BYTE types, unless sub-byte packed rasters/IndexColorModel
        // * all INT types (even custom, as long as they use 8bit/componnet)
        // * all USHORT types (even custom)

        // TODO: Also check if the images are really compatible!?

        long start = System.currentTimeMillis();
        // Straight-forward version
        //Image scaled = src.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        //ImageUtil.drawOnto(result, scaled);
        //result = new BufferedImageFactory(scaled).getBufferedImage();

        /*
        // Try: Use bilinear/bicubic and half the image down until it's less than
        // twice as big, then use bicubic for the last step?
        BufferedImage temp = null;
        AffineTransform xform = null;
        int w = src.getWidth();
        int h = src.getHeight();
        while (w / 2 > width && h / 2 > height) {
            w /= 2;
            h /= 2;

            if (temp == null) {
                xform = AffineTransform.getScaleInstance(.5, .5);
                ColorModel cm = src.getColorModel();
                temp = new BufferedImage(cm,
                                         ImageUtil.createCompatibleWritableRaster(src, cm, w, h),
                                         cm.isAlphaPremultiplied(), null);

                resample(src, temp, xform);
            }
            else {
                resample(temp, temp, xform);
            }

            System.out.println("w: " + w);
            System.out.println("h: " + h);
        }

        if (temp != null) {
            src = temp.getSubimage(0, 0, w, h);
        }

        resample(src, result, AffineTransform.getScaleInstance(width / (double) w, height / (double) h));
        */

        // The real version
        filterImpl(src.getRaster(), result.getRaster());

        long time = System.currentTimeMillis() - start;
        System.out.println("time: " + time);

        return result;
    }

    private void resample(final BufferedImage pSrc, final BufferedImage pDest, final AffineTransform pXform) {
        Graphics2D d = pDest.createGraphics();
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        try {
            d.drawImage(pSrc, pXform, null);
        }
        finally {
            d.dispose();
        }
    }

    public WritableRaster filter(Raster src, WritableRaster dest) {
        WritableRaster result = dest != null ? dest : createCompatibleDestRaster(src);
        return filterImpl(src, result);
    }

    private WritableRaster filterImpl(Raster src, WritableRaster dest) {
        //System.out.println("src: " + src);
        //System.out.println("dest: " + dest);
        if (sourceRegion != null) {
            int cx = sourceRegion.x;
            int cy = sourceRegion.y;
            int cw = sourceRegion.width;
            int ch = sourceRegion.height;

            boolean same = src == dest;
            dest = dest.createWritableChild(cx, cy, cw, ch, 0, 0, null);
            src = same ? dest : src.createChild(cx, cy, cw, ch, 0, 0, null);
            //System.out.println("src: " + src);
            //System.out.println("dest: " + dest);
        }

        final int width = src.getWidth();
        final int height = src.getHeight();

        // TODO: This don't work too well..
        // The thing is that the step length and the scan length will vary, for
        // non-even (1/2, 1/4, 1/8 etc) resampling
        int widthSteps = (width + this.width - 1) / this.width;
        int heightSteps = (height + this.height - 1) / this.height;

        final boolean oddX = width % this.width != 0;
        final boolean oddY = height % this.height != 0;

        final int dataElements = src.getNumDataElements();
        final int bands = src.getNumBands();
        final int dataType = src.getTransferType();

        Object data = null;
        int scanW;
        int scanH;

        // TYPE_USHORT setup
        int[] bitMasks = null;
        int[] bitOffsets = null;
        if (src.getTransferType() == DataBuffer.TYPE_USHORT) {
            if (src.getSampleModel() instanceof SinglePixelPackedSampleModel) {
                // DIRECT
                SinglePixelPackedSampleModel sampleModel = (SinglePixelPackedSampleModel) src.getSampleModel();
                bitMasks = sampleModel.getBitMasks();
                bitOffsets = sampleModel.getBitOffsets();
            }
            else {
                // GRAY
                bitMasks = new int[]{0xffff};
                bitOffsets = new int[]{0};
            }
        }

        for (int y = 0; y < this.height; y++) {
            if (!oddY || y < this.height) {
                scanH = heightSteps;
            }
            else {
                scanH = height - (y * heightSteps);
            }

            for (int x = 0; x < this.width; x++) {
                if (!oddX || x < this.width) {
                    scanW = widthSteps;
                }
                else {
                    scanW = width - (x * widthSteps);
                }
                final int pixelCount = scanW * scanH;
                final int pixelLength = pixelCount * dataElements;

                try {
                    data = src.getDataElements(x * widthSteps, y * heightSteps, scanW, scanH, data);
                }
                catch (IndexOutOfBoundsException e) {
                    // TODO: FixMe!
                    // The bug is in the steps... 
                    //System.err.println("x: " + x);
                    //System.err.println("y: " + y);
                    //System.err.println("widthSteps: " + widthSteps);
                    //System.err.println("heightSteps: " + heightSteps);
                    //System.err.println("scanW: " + scanW);
                    //System.err.println("scanH: " + scanH);
                    //
                    //System.err.println("width: " + width);
                    //System.err.println("height: " + height);
                    //System.err.println("width: " + width);
                    //System.err.println("height: " + height);
                    //
                    //e.printStackTrace();
                    continue;
                }

                // TODO: Might need more channels... Use an array?
                // NOTE: These are not neccessarily ARGB..
                double valueA = 0.0;
                double valueR = 0.0;
                double valueG = 0.0;
                double valueB = 0.0;

                switch (dataType) {
                    case DataBuffer.TYPE_BYTE:
                        // TODO: Doesn't hold for index color models...
                        // For index color, the best bet is probably convert to
                        // true color, then convert back to the same index color 
                        // model
                        byte[] bytePixels = (byte[]) data;
                        for (int i = 0; i < pixelLength; i += dataElements) {
                            valueA += bytePixels[i] & 0xff;
                            if (bands > 1) {
                                valueR += bytePixels[i + 1] & 0xff;
                                valueG += bytePixels[i + 2] & 0xff;
                                if (bands > 3) {
                                    valueB += bytePixels[i + 3] & 0xff;
                                }
                            }
                        }

                        // Average
                        valueA /= pixelCount;
                        if (bands > 1) {
                            valueR /= pixelCount;
                            valueG /= pixelCount;
                            if (bands > 3) {
                                valueB /= pixelCount;
                            }
                        }

                        //for (int i = 0; i < pixelLength; i += dataElements) {
                        bytePixels[0] = (byte) clamp((int) valueA);
                        if (bands > 1) {
                            bytePixels[1] = (byte) clamp((int) valueR);
                            bytePixels[2] = (byte) clamp((int) valueG);
                            if (bands > 3) {
                                bytePixels[3] = (byte) clamp((int) valueB);
                            }
                        }
                        //}
                        break;

                    case DataBuffer.TYPE_INT:
                        int[] intPixels = (int[]) data;
                        // TODO: Rewrite to use bit offsets and masks from
                        // color model (see TYPE_USHORT) in case of a non-
                        // 888 or 8888 colormodel?
                        for (int i = 0; i < pixelLength; i += dataElements) {
                            valueA += (intPixels[i] & 0xff000000) >> 24;
                            valueR += (intPixels[i] & 0xff0000) >> 16;
                            valueG += (intPixels[i] & 0xff00) >> 8;
                            valueB += (intPixels[i] & 0xff);
                        }

                        // Average
                        valueA /= pixelCount;
                        valueR /= pixelCount;
                        valueG /= pixelCount;
                        valueB /= pixelCount;

                        //for (int i = 0; i < pixelLength; i += dataElements) {
                        intPixels[0] = clamp((int) valueA) << 24;
                        intPixels[0] |= clamp((int) valueR) << 16;
                        intPixels[0] |= clamp((int) valueG) << 8;
                        intPixels[0] |= clamp((int) valueB);
                        //}
                        break;

                    case DataBuffer.TYPE_USHORT:
                        if (bitMasks != null) {
                            short[] shortPixels = (short[]) data;
                            for (int i = 0; i < pixelLength; i += dataElements)
                            {
                                valueA += (shortPixels[i] & bitMasks[0]) >> bitOffsets[0];
                                if (bitMasks.length > 1) {
                                    valueR += (shortPixels[i] & bitMasks[1]) >> bitOffsets[1];
                                    valueG += (shortPixels[i] & bitMasks[2]) >> bitOffsets[2];
                                    if (bitMasks.length > 3) {
                                        valueB += (shortPixels[i] & bitMasks[3]) >> bitOffsets[3];
                                    }
                                }
                            }

                            // Average
                            valueA /= pixelCount;
                            valueR /= pixelCount;
                            valueG /= pixelCount;
                            valueB /= pixelCount;

                            //for (int i = 0; i < pixelLength; i += dataElements) {
                            shortPixels[0] = (short) (((int) valueA << bitOffsets[0]) & bitMasks[0]);
                            if (bitMasks.length > 1) {
                                shortPixels[0] |= (short) (((int) valueR << bitOffsets[1]) & bitMasks[1]);
                                shortPixels[0] |= (short) (((int) valueG << bitOffsets[2]) & bitMasks[2]);
                                if (bitMasks.length > 3) {
                                    shortPixels[0] |= (short) (((int) valueB << bitOffsets[3]) & bitMasks[3]);
                                }
                            }
                            //}
                            break;
                        }
                    default:
                        throw new IllegalArgumentException("TransferType not supported: " + dataType);

                }

                dest.setDataElements(x, y, 1, 1, data);
            }
        }

        return dest;
    }

    private static int clamp(final int pValue) {
        return pValue > 255 ? 255 : pValue;
    }

    public RenderingHints getRenderingHints() {
        return null;
    }

    // TODO: Refactor boilerplate to AbstractBufferedImageOp or use a delegate?
    // Delegate is maybe better as we won't always implement both BIOp and RasterOP
    // (but are there ever any time we want to implemnet RasterOp and not BIOp?)
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel cm = destCM != null ? destCM : src.getColorModel();
        return new BufferedImage(cm,
                                 ImageUtil.createCompatibleWritableRaster(src, cm, width, height),
                                 cm.isAlphaPremultiplied(), null);
    }

    public WritableRaster createCompatibleDestRaster(Raster src) {
        return src.createCompatibleWritableRaster(width, height);
    }

    public Rectangle2D getBounds2D(Raster src) {
        return new Rectangle(width, height);
    }

    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(width, height);
    }

    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        // TODO: This is wrong!
        if (dstPt == null) {
            if (srcPt instanceof Point2D.Double) {
                dstPt = new Point2D.Double();
            }
            else {
                dstPt = new Point2D.Float();
            }
        }
        dstPt.setLocation(srcPt);

        return dstPt;
    }

    public static void main(String[] pArgs) throws IOException {
        BufferedImage image = ImageIO.read(new File("2006-Lamborghini-Gallardo-Spyder-Y-T-1600x1200.png"));
        //BufferedImage image = ImageIO.read(new File("focus-rs.jpg"));
        //BufferedImage image = ImageIO.read(new File("blauesglas_16_bitmask444.bmp"));
        //image = ImageUtil.toBuffered(image, BufferedImage.TYPE_USHORT_GRAY);

        for (int i = 0; i < 100; i++) {
            //new PixelizeOp(10).filter(image, null);
            //new AffineTransformOp(AffineTransform.getScaleInstance(.1, .1), AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, null);
            //ImageUtil.toBuffered(image.getScaledInstance(image.getWidth() / 4, image.getHeight() / 4, Image.SCALE_AREA_AVERAGING));
            //new ResampleOp(image.getWidth() / 10, image.getHeight() / 10, ResampleOp.FILTER_BOX).filter(image, null);
            //new ResampleOp(image.getWidth() / 10, image.getHeight() / 10, ResampleOp.FILTER_QUADRATIC).filter(image, null);
            //new AreaAverageOp(image.getWidth() / 10, image.getHeight() / 10).filter(image, null);
        }

        long start = System.currentTimeMillis();
        //PixelizeOp pixelizer = new PixelizeOp(image.getWidth() / 10, 1);
        //pixelizer.setSourceRegion(new Rectangle(0, 2 * image.getHeight() / 3, image.getWidth(), image.getHeight() / 4));
        //PixelizeOp pixelizer = new PixelizeOp(4);
        //image = pixelizer.filter(image, image); // Filter in place, that's cool
        //image = new AffineTransformOp(AffineTransform.getScaleInstance(.25, .25), AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, null);
        //image = ImageUtil.toBuffered(image.getScaledInstance(image.getWidth() / 4, image.getHeight() / 4, Image.SCALE_AREA_AVERAGING));
        //image = new ResampleOp(image.getWidth() / 4, image.getHeight() / 4, ResampleOp.FILTER_BOX).filter(image, null);
        //image = new ResampleOp(image.getWidth() / 4, image.getHeight() / 4, ResampleOp.FILTER_QUADRATIC).filter(image, null);
        //image = new AreaAverageOp(image.getWidth() / 7, image.getHeight() / 4).filter(image, null);
        image = new AreaAverageOp(500, 600).filter(image, null);
        //image = new ResampleOp(500, 600, ResampleOp.FILTER_BOX).filter(image, null);
        long time = System.currentTimeMillis() - start;

        System.out.println("time: " + time + " ms");

        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new JScrollPane(new JLabel(new BufferedImageIcon(image))));
        frame.pack();
        frame.setVisible(true);
    }
}
