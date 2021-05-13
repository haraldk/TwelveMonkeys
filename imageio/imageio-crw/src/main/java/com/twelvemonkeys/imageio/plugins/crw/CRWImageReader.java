/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.crw;

import static java.util.Collections.singletonList;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.plugins.crw.ciff.CIFF;
import com.twelvemonkeys.imageio.plugins.crw.ciff.CIFFDirectory;
import com.twelvemonkeys.imageio.plugins.crw.ciff.CIFFEntry;
import com.twelvemonkeys.imageio.plugins.crw.ciff.CIFFReader;
import com.twelvemonkeys.imageio.stream.BufferedImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Canon CRW RAW ImageReader.
 * <p/>
 *
 * @see <a href="http://cybercom.net/~dcoffin/dcraw/">Decoding raw digital photos in Linux</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author CRW C reference decoder written by Dave Coffin.
 * @author last modified by $Author: haraldk$
 * @version $Id: CRWImageReader.java,v 1.0 07.04.14 21:31 haraldk Exp$
 */
public final class CRWImageReader extends ImageReaderBase {
    // TODO: Avoid duped code from TIFFImageReader, create a ExifRAWBaseImageReader something
    // TODO: Probably a good idea to move some of the getAsShort/Int/Long/Array to TIFF/EXIF metadata module
    // TODO: Automatic EXIF rotation, if we find a good way to do that for JPEG/EXIF/TIFF and keeping the metadata sane...

    final static boolean DEBUG =  true; //"true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.crw.debug"));

    // TODO: Thumbnail may or may not be present

    private CIFFDirectory heap;
    private boolean hasJPEGPreview;
    private boolean hasRAWData;

    CRWImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        heap = null;
        hasJPEGPreview = false;
        hasRAWData = false;
    }

    private void readMetadata() throws IOException {
        if (imageInput == null) {
            throw new IllegalStateException("input not set");
        }

        if (heap == null) {
            heap = new CIFFReader().read(imageInput);

            hasJPEGPreview = heap.getEntryById(CIFF.TAG_JPEG_PREVIEW) != null;
            hasRAWData = heap.getEntryById(CIFF.TAG_RAW_DATA) != null;

            if (DEBUG) {
                System.err.println("directory: " + heap);
            }
        }
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        readMetadata();

        return (hasJPEGPreview ? 1 : 0) + (hasRAWData ? 1 : 0);
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        // TODO: Count thumbnails!
        return 0;
    }

    @Override
    public boolean readerSupportsThumbnails() {
        return true;
    }

    @Override
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        // TODO: Need to get from JPEGImageReader (no ImageWidth tag), this is an ok (but lame) implementation for now
        return super.getThumbnailWidth(imageIndex, thumbnailIndex);
    }

    @Override
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        readMetadata();
        checkBounds(imageIndex);

        // TODO: Need to get from JPEGImageReader (no ImageHeight tag), this is an ok (but lame) implementation for now
        return super.getThumbnailHeight(imageIndex, thumbnailIndex);
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        readMetadata();

        if (imageIndex != 0) {
            throw new IndexOutOfBoundsException("No thumbnail for imageIndex: " + imageIndex);
        }
        if (thumbnailIndex >= getNumThumbnails(0)) {
            throw new IndexOutOfBoundsException("thumbnailIndex out of bounds: " + thumbnailIndex);
        }

        throw new UnsupportedOperationException("readThumbnail");
    }

    @Override
    public int getWidth(int imageIndex) throws IOException {
        readMetadata();

        if (imageIndex == 0 && hasJPEGPreview) {
            return getJPEGPreviewDimension().width;
        }

        return getRawImageDimension().width;
    }

    @Override
    public int getHeight(int imageIndex) throws IOException {
        readMetadata();

        if (imageIndex == 0 && hasJPEGPreview) {
            return getJPEGPreviewDimension().height;
        }

        return getRawImageDimension().height;
    }

    private Dimension getJPEGPreviewDimension() {
        // TODO: This is incorrect for the G1 sample, which stores the RAW size here...
        CIFFDirectory imageProperties = heap.getSubDirectory(CIFF.TAG_IMAGE_PROPERTIES);
        CIFFEntry imageSpec = imageProperties.getEntryById(CIFF.TAG_IMAGE_SPEC);
        int[] imageSpecValue = (int[]) imageSpec.getValue();

        return new Dimension(imageSpecValue[0], imageSpecValue[1]);
    }

    private Dimension getRawImageDimension() {
        CIFFDirectory exifInfo = getExifInfo();
        CIFFEntry sensorInfo = exifInfo.getEntryById(CIFF.TAG_SENSOR_INFO);

        if (sensorInfo != null) {
            short[] sensorInfoValue = (short[]) sensorInfo.getValue();

            return new Dimension(sensorInfoValue[1], sensorInfoValue[2]);
        }

        // PowerShot Pro70 et al, don't have a JPEG preview and contains the dimensions in the ImageSpec tag
        return getJPEGPreviewDimension();
    }

    private CIFFDirectory getExifInfo() {
        CIFFDirectory imageProperties = heap.getSubDirectory(CIFF.TAG_IMAGE_PROPERTIES);
        return imageProperties.getSubDirectory(CIFF.TAG_EXIF_INFORMATION);
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        readMetadata();

        if (imageIndex == 0 && hasJPEGPreview) {
            BufferedImage image = readJPEGPreview();
            return singletonList(ImageTypeSpecifier.createFromRenderedImage(image)).iterator();
        }

        throw new IndexOutOfBoundsException();
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        readMetadata();

        if (imageIndex == 0 && hasJPEGPreview) {
            return readJPEGPreview();
        }

        return readRAWData();
    }

    private BufferedImage readJPEGPreview() throws IOException {
        CIFFEntry jpegPreview = heap.getEntryById(CIFF.TAG_JPEG_PREVIEW);

        imageInput.seek(jpegPreview.offset());

        return ImageIO.read(new SubImageInputStream(imageInput, jpegPreview.length()));
    }

    private BufferedImage readRAWData() throws IOException {
        CIFFDirectory exifInfo = getExifInfo();
        CIFFEntry decoderTable = exifInfo.getEntryById(CIFF.TAG_DECODER_TABLE);
        int[] decoderTableValue = decoderTable != null ? (int[]) decoderTable.getValue() : null;
        int table = decoderTableValue != null ? decoderTableValue[0] : 0;
        long offset = decoderTableValue != null ? decoderTableValue[2] : 0;

        Dimension size = getRawImageDimension();
        int width = size.width;
        int height = size.height;

        // TODO: This is probably not the best way to get the bits/pixel, but seems to be correct (when it's there).
        // However,
        CIFFEntry whiteSample = exifInfo.getEntryById(CIFF.TAG_WHITE_SAMPLE);
        short[] whiteSampleValue = whiteSample != null ? (short[]) whiteSample.getValue() : null;
        short bitsPerSample = whiteSampleValue != null && whiteSample.length() > 5 ? whiteSampleValue[5] : 12;

        CIFFEntry rawData = heap.getEntryById(CIFF.TAG_RAW_DATA);

        imageInput.seek(rawData.offset() + offset);

        ImageInputStream stream = new BufferedImageInputStream(new SubImageInputStream(imageInput, rawData.length()));

        CRWDecoder decoder = new CRWDecoder(stream, width, height, table);
        short[] data = decoder.decode();

        DataBuffer dataBuffer = new DataBufferUShort(data, data.length);
        WritableRaster raster = Raster.createInterleavedRaster(dataBuffer, width, height, width, 1, new int[] {0}, null);

        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[]{bitsPerSample}, false, false, Transparency.OPAQUE, dataBuffer.getDataType());

        return new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
    }

    public static void main(String[] args) throws IOException {
        CRWImageReader reader = new CRWImageReader(new CRWImageReaderSpi());

        for (String arg : args) {
            ImageInputStream stream = ImageIO.createImageInputStream(new File(arg));
            System.err.println("canDecode: " + reader.getOriginatingProvider().canDecodeInput(stream));

            reader.setInput(stream);

            int numImages = reader.getNumImages(true);
            for (int i = 0; i < numImages; i++) {
                int numThumbnails = reader.getNumThumbnails(i);
                for (int n = 0; n < numThumbnails; n++) {
                    showIt(reader.readThumbnail(i, n), arg + " image " + i + " thumbnail " + n);
                }

                showIt(reader.read(i), arg + " image " + i);
            }
        }
    }
}
