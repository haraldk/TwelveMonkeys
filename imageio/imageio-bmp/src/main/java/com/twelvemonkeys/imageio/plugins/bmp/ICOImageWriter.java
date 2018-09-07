/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.stream.SubImageOutputStream;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;

import javax.imageio.*;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static com.twelvemonkeys.imageio.plugins.bmp.DirectoryEntry.ICOEntry;

/**
 * ImageWriter implementation for Windows Icon (ICO) format.
 */
public final class ICOImageWriter extends DIBImageWriter {

    // TODO: Support appending/updating an existing ICO file?
    // - canInsertImage/canRemoveImage

    private static final int ENTRY_SIZE = 16;
    private static final int ICO_MAX_DIMENSION = 256;
    private static final int INITIAL_ENTRY_COUNT = 8;

    private int sequenceIndex = -1;

    private ImageWriter pngDelegate;

    protected ICOImageWriter(final ImageWriterSpi provider) {
        super(provider);
    }

    @Override
    protected void resetMembers() {
        sequenceIndex = -1;

        if (pngDelegate != null) {
            pngDelegate.dispose();
            pngDelegate = null;
        }
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(final IIOMetadata inData, final ImageTypeSpecifier imageType, final ImageWriteParam param) {
        return null;
    }

    @Override
    public void write(final IIOMetadata streamMetadata, final IIOImage image, final ImageWriteParam param) throws IOException {
        prepareWriteSequence(streamMetadata);
        writeToSequence(image, param);
        endWriteSequence();
    }

    @Override
    public boolean canWriteSequence() {
        return true;
    }

    @Override
    public void prepareWriteSequence(final IIOMetadata streamMetadata) throws IOException {
        assertOutput();

        if (sequenceIndex >= 0) {
            throw new IllegalStateException("writeSequence already started");
        }

        writeICOHeader();

        // Count: Needs to be updated for each new image
        imageOutput.writeShort(0);
        sequenceIndex = 0;

        // TODO: Allow passing the initial size of the directory in the stream metadata?
        // - as this is much more efficient than growing...
        // How do we update the "image directory" containing "image entries",
        // and which must be written *before* the image data?
        // - Allocate a block of N * 16 bytes
        //   - If image count % N > N, we need to move the first image backwards in the file and allocate another N items...
        imageOutput.write(new byte[INITIAL_ENTRY_COUNT * ENTRY_SIZE]); // Allocate room for 8 entries for now
    }

    @Override
    public void endWriteSequence() throws IOException {
        assertOutput();

        if (sequenceIndex < 0) {
            throw new IllegalStateException("prepareWriteSequence not called");
        }

        sequenceIndex = -1;
    }

    @Override
    public void writeToSequence(final IIOImage image, final ImageWriteParam param) throws IOException {
        assertOutput();

        if (sequenceIndex < 0) {
            throw new IllegalStateException("prepareWriteSequence not called");
        }

        if (image.hasRaster()) {
            throw new UnsupportedOperationException("image has a Raster");
        }

        if (sequenceIndex >= INITIAL_ENTRY_COUNT) {
            growIfNecessary();
        }

        int width = image.getRenderedImage().getWidth();
        int height = image.getRenderedImage().getHeight();
        ColorModel colorModel = image.getRenderedImage().getColorModel();

        // TODO: The output size may depend on the param (subsampling, source region, etc)
        if (width > ICO_MAX_DIMENSION && height > ICO_MAX_DIMENSION) {
            throw new IIOException(String.format("ICO maximum width or height (%d) exceeded", ICO_MAX_DIMENSION));
        }

        long imageOffset = imageOutput.getStreamPosition();

        if (imageOffset > Integer.MAX_VALUE) {
            throw new IIOException("ICO file too large");
        }

        // Uncompressed, RLE4/RLE8 or PNG compressed
        boolean pngCompression = param != null && "BI_PNG".equals(param.getCompressionType());

        processImageStarted(sequenceIndex);

        if (pngCompression) {
            // NOTE: Embedding a PNG in a ICO is slightly different than a BMP with BI_PNG compression,
            // so we'll just handle it directly
            ImageWriter writer = getPNGDelegate();
            writer.setOutput(new SubImageOutputStream(imageOutput));
            writer.write(null, image, copyParam(param, writer));
        }
        else {
            RenderedImage img = image.getRenderedImage();
            // ICO needs height to include height of mask, even if mask isn't written
            writeDIBHeader(DIB.BITMAP_INFO_HEADER_SIZE, img.getWidth(), img.getHeight() * 2,
                    false, img.getColorModel().getPixelSize(), DIB.COMPRESSION_RGB);
            writeUncompressed(false, (BufferedImage) img, img.getWidth(), img.getHeight());
            // TODO: Write mask
            imageOutput.write(new byte[((width * height + 31) / 32) * 4]);
//            writeUncompressed(false, new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY), img.getWidth(), img.getHeight());
        }

        processImageComplete();

        long nextPosition = imageOutput.getStreamPosition();

        // Update count
        imageOutput.seek(4);
        imageOutput.writeShort(sequenceIndex + 1);

        // Write entry
        int entryPosition = 6 + sequenceIndex * ENTRY_SIZE;
        imageOutput.seek(entryPosition);

        long size = nextPosition - imageOffset;
        writeEntry(width,  height, colorModel, (int) size, (int) imageOffset);

        sequenceIndex++;

        imageOutput.seek(nextPosition);
    }

    private void writeICOHeader() throws IOException {
        if (imageOutput.getStreamPosition() != 0) {
            throw new IllegalStateException("Stream already written to");
        }

        imageOutput.writeShort(0);
        imageOutput.writeShort(DIB.TYPE_ICO);
        imageOutput.flushBefore(imageOutput.getStreamPosition());
    }

    private void growIfNecessary() {
        // TODO: Allow growing the directory index...
        // Move the first icon to the back, update offset
        throw new IllegalStateException(String.format("Maximum number of icons supported (%d) exceeded", INITIAL_ENTRY_COUNT));
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new ICOImageWriteParam(getLocale());
    }

    private ImageWriteParam copyParam(final ImageWriteParam param, ImageWriter writer) {
        if (param == null) {
            return null;
        }

        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setSourceSubsampling(param.getSourceXSubsampling(), param.getSourceYSubsampling(), param.getSubsamplingXOffset(), param.getSubsamplingYOffset());
        writeParam.setSourceRegion(param.getSourceRegion());
        writeParam.setSourceBands(param.getSourceBands());

        return writeParam;
    }

    private ImageWriter getPNGDelegate() {
        if (pngDelegate == null) {
            // There's always a PNG writer...
            pngDelegate = ImageIO.getImageWritersByFormatName("PNG").next();
            pngDelegate.setLocale(getLocale());
            pngDelegate.addIIOWriteProgressListener(new ProgressListenerBase() {
                @Override
                public void imageProgress(ImageWriter source, float percentageDone) {
                    processImageProgress(percentageDone);
                }

                @Override
                public void writeAborted(ImageWriter source) {
                    processWriteAborted();
                }
            });
            pngDelegate.addIIOWriteWarningListener(new IIOWriteWarningListener() {
                @Override
                public void warningOccurred(ImageWriter source, int imageIndex, String warning) {
                    processWarningOccurred(sequenceIndex, warning);
                }
            });
        }

        return pngDelegate;
    }

    private void writeEntry(final int width, final int height, final ColorModel colorModel, int size, final int offset) throws IOException {
        new ICOEntry(width, height, colorModel, size, offset)
                .write(imageOutput);
    }

    public static void main(String[] args) throws IOException {
        boolean pngCompression = false;
        int firstArg = 0;

        while (args.length > firstArg && args[firstArg].charAt(0) == '-') {
            if (args[firstArg].equals("-p") || args[firstArg].equals("--png")) {
                pngCompression = true;
            }

            firstArg++;
        }

        if (args.length - firstArg < 2) {
            System.err.println("Usage: command [-p|--png] <output.ico> <input> [<input>...]");
            System.exit(1);
        }

        try (ImageOutputStream out = ImageIO.createImageOutputStream(new File(args[firstArg++]))) {
            ImageWriter writer = new ICOImageWriter(null);
            writer.setOutput(out);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionType(pngCompression ? "BI_PNG" : "BI_RGB");

            writer.prepareWriteSequence(null);

            for (int i = firstArg; i < args.length; i++) {
                File inFile = new File(args[i]);
                try (ImageInputStream input = ImageIO.createImageInputStream(inFile)) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

                    if (!readers.hasNext()) {
                        System.err.printf("Can't read %s\n", inFile.getAbsolutePath());
                    }
                    else {
                        ImageReader reader = readers.next();
                        reader.setInput(input);
                        for (int j = 0; j < reader.getNumImages(true); j++) {
                            IIOImage image = reader.readAll(j, null);
                            writer.writeToSequence(image, param);
                        }
                    }
                }
            }

            writer.endWriteSequence();
            writer.dispose();
        }
    }
}
