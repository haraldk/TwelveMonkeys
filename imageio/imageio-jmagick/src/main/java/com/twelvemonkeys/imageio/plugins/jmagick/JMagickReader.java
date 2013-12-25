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

package com.twelvemonkeys.imageio.plugins.jmagick;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.image.MagickUtil;
import com.twelvemonkeys.image.MonochromeColorModel;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IndexedImageTypeSpecifier;
import com.twelvemonkeys.io.FileUtil;
import magick.ImageInfo;
import magick.ImageType;
import magick.MagickException;
import magick.MagickImage;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * JMagickReader, uses JMagick, an open source Java interface of ImageMagick to
 * read images. This {@code ImageReader} has certain limitations (see below),
 * but the number of formats supported should by far outweigh these
 * limitatations.
 * <p/>
 * Limitations: JMagick is <em>not</em> stream-based, meaning image (file) data
 * must be read into a temporary byte array, potentially causing performance
 * penalties.
 * ImageMagick itself might even use the file extension to determine file
 * format, in such cases a temporary file must be written to disk before
 * the image data can be read. While this is perfomed transparently to the user
 * there are still performance penalties related to the extra disk I/O.
 * <p/>
 * <small>
 * Note: This class relies on JMagick, which ues JNI and native code. You need
 * to have the JMagick and ImageMagick shared libraries (or DLLs) in Java's
 * {@code java.library.path} for this class to work.
 * </small>
 *
 * @see <a href="http://www.imagemagick.org/">ImageMagick homepage</a>
 * @see <a href="http://www.yeo.id.au/jmagick/">JMagick homepage</a>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: JMagickReader.java,v 1.0 24.jan.2005 10:33:05 haku Exp$
 *
 * @todo System property to allow all images be read using temp file (reducing
 * memory consumption): "com.twelvemonkeys.imageio.plugins.jmagick.useTempFile"
 */
abstract class JMagickReader extends ImageReaderBase {

    // Make sure the JMagick init is run, or class init will fail
    static {
        JMagick.init();
    }

    private static final ColorModel CM_GRAY_ALPHA =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true, true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

    private final boolean useTempFile;
    private File tempFile;

    private MagickImage image;
    private Dimension size;

    protected JMagickReader(final JMagickImageReaderSpiSupport pProvider) {
        this(pProvider, pProvider.useTempFile());
    }

    protected JMagickReader(final ImageReaderSpi pProvider, final boolean pUseTemp) {
        super(pProvider);
        useTempFile = pUseTemp;
    }

    @Override
    protected void resetMembers() {
        if (tempFile != null&& !tempFile.delete()) {
            tempFile.deleteOnExit();
        }

        tempFile = null;

        if (image != null) {
            image.destroyImages();
        }

        image = null;
        size = null;
    }    

    // TODO: Handle multi-image formats
    // if (image.hasFrames()) {
    //    int count = image.getNumFrames();
    //    MagickImage[] images = image.breakFrames();
    // }

    public Iterator<ImageTypeSpecifier> getImageTypes(int pIndex) throws IOException {
        checkBounds(pIndex);

        init(pIndex);

        // TODO: FIX ME!
        // - Use factory methods for ImageTypeSpecifier, create factory methods if necessary
        List<ImageTypeSpecifier> specs = new ArrayList<ImageTypeSpecifier>();
        try {
            ColorModel cm;
            // NOTE: These are all fall-through by intention 
            switch (image.getImageType()) {
                case ImageType.BilevelType:
                    specs.add(IndexedImageTypeSpecifier.createFromIndexColorModel(MonochromeColorModel.getInstance()));
                case ImageType.GrayscaleType:
//                    cm = MagickUtil.CM_GRAY_OPAQUE;
//                    specs.add(new ImageTypeSpecifier(
//                            cm,
//                            cm.createCompatibleSampleModel(1, 1)
//                    ));
                    specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
                case ImageType.GrayscaleMatteType:
                    cm = CM_GRAY_ALPHA;
                    specs.add(new ImageTypeSpecifier(
                            cm,
                            cm.createCompatibleSampleModel(1, 1)
                    ));
                case ImageType.PaletteType:
                    specs.add(IndexedImageTypeSpecifier.createFromIndexColorModel(
                            MagickUtil.createIndexColorModel(image.getColormap(), false)
                    ));
                case ImageType.PaletteMatteType:
                    specs.add(IndexedImageTypeSpecifier.createFromIndexColorModel(
                            MagickUtil.createIndexColorModel(image.getColormap(), true)
                    ));
                case ImageType.TrueColorType:
//                    cm = MagickUtil.CM_COLOR_OPAQUE;
//                    specs.add(new ImageTypeSpecifier(
//                            cm,
//                            cm.createCompatibleSampleModel(1, 1)
//                    ));
                    specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR));
                case ImageType.TrueColorMatteType:
//                    cm = MagickUtil.CM_COLOR_ALPHA;
//                    specs.add(new ImageTypeSpecifier(
//                            cm,
//                            cm.createCompatibleSampleModel(1, 1)
//                    ));
                    specs.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR_PRE));
                    break;
                case ImageType.ColorSeparationType:
                case ImageType.ColorSeparationMatteType:
                case ImageType.OptimizeType:
                default:
                    throw new MagickException("Unknown JMagick image type: " + image.getImageType());
            }
        }
        catch (MagickException e) {
            throw new IIOException(e.getMessage(), e);
        }

        return specs.iterator();
    }

    public int getWidth(int pIndex) throws IOException {
        checkBounds(pIndex);

        if (size == null) {
            init(0);
        }
        return size != null ? size.width : -1;
    }

    public int getHeight(int pIndex) throws IOException {
        checkBounds(pIndex);

        if (size == null) {
            init(0);
        }
        return size != null ? size.height : -1;
    }

    public BufferedImage read(int pIndex, ImageReadParam pParam) throws IOException {
        try {
            init(pIndex);

            processImageStarted(pIndex);

            // Some more waste of time and space...
            Dimension size = this.size;

            if (pParam != null) {
                // Source region
                // TODO: Maybe have to do some tests, to check if we are within bounds...
                Rectangle sourceRegion = pParam.getSourceRegion();
                if (sourceRegion != null) {
                    image = image.cropImage(sourceRegion);
                    size = sourceRegion.getSize();
                }

                // Subsampling
                if (pParam.getSourceXSubsampling() > 1 || pParam.getSourceYSubsampling() > 1) {
                    int w = size.width / pParam.getSourceXSubsampling();
                    int h = size.height / pParam.getSourceYSubsampling();

                    image = image.sampleImage(w, h);
                    size = new Dimension(w, h);
                }
            }

            if (abortRequested()) {
                processReadAborted();
                return ImageUtil.createClear(size.width, size.height, null);
            }

            processImageProgress(10f);
            BufferedImage buffered = MagickUtil.toBuffered(image);
            processImageProgress(100f);

            /**/
            //System.out.println("Created image: " + buffered);
            //System.out.println("ColorModel: " + buffered.getColorModel().getClass().getName());
            //if (buffered.getColorModel() instanceof java.awt.image.IndexColorModel) {
            //    java.awt.image.IndexColorModel cm = (java.awt.image.IndexColorModel) buffered.getColorModel();
            //    for (int i = 0; i < cm.getMapSize(); i++) {
            //        System.out.println("0x" + Integer.toHexString(cm.getRGB(i)));
            //    }
            //}
            //*/

            /**
            System.out.println("Colorspace: " + image.getColorspace());
            System.out.println("Depth: " + image.getDepth());
            System.out.println("Format: " + image.getImageFormat());
            System.out.println("Type: " + image.getImageType());
            System.out.println("IPTCProfile: " + StringUtil.deepToString(image.getIptcProfile()));
            System.out.println("StorageClass: " + image.getStorageClass());
            //*/

            processImageComplete();

            return buffered;
        }
        catch (MagickException e) {
            // Wrap in IIOException
            throw new IIOException(e.getMessage(), e);
        }
    }

    private synchronized void init(int pIndex) throws IOException {
        checkBounds(pIndex);

        try {
            if (image == null) {
                // TODO: If ImageInputStream is already file-backed, maybe we can peek into that file?
                //       At the moment, the cache/file is not accessible, but we could create our own
                //       FileImageInputStream provider that gives us this access.
                if (!useTempFile && imageInput.length() >= 0 && imageInput.length() <= Integer.MAX_VALUE) {
                    // This works for most file formats, as long as ImageMagick
                    // uses the file magic to decide file format
                    byte[] bytes = new byte[(int) imageInput.length()];
                    imageInput.readFully(bytes);

                    // Unfortunately, this is a waste of space & time...
                    ImageInfo info = new ImageInfo();
                    image = new MagickImage(info);
                    image.blobToImage(info, bytes);
                }
                else {
                    // Quirks mode: Use temp file to get correct file extension
                    // (which is even more waste of space & time, but might save memory)
                    String ext = getFormatName().toLowerCase();

                    tempFile = File.createTempFile("jmagickreader", "." + ext);
                    tempFile.deleteOnExit();
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));

                    try {
                        byte[] buffer = new byte[FileUtil.BUF_SIZE];
                        int count;

                        while ((count = imageInput.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                        }

                        // Flush out stream, to write any remaining buffered data
                        out.flush();
                    }
                    finally {
                        out.close();
                    }

                    ImageInfo info = new ImageInfo(tempFile.getAbsolutePath());
                    image = new MagickImage(info);
                }

                size = image.getDimension();
            }
        }
        catch (MagickException e) {
            // Wrap in IIOException
            throw new IIOException(e.getMessage(), e);
        }
    }
}