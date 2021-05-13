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

package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.imageio.ImageWriterBase;
import com.twelvemonkeys.imageio.stream.SubImageOutputStream;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;

import javax.imageio.*;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * ICNSImageWriter
 */
public final class ICNSImageWriter extends ImageWriterBase {

    private int sequenceIndex = -1;
    private ImageWriter pngDelegate;

    ICNSImageWriter(ImageWriterSpi provider) {
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

        // TODO: Allow TOC resource to be passed as stream metadata?
        // - We only need number of icons to be written later
        // - The contents of the TOC could be updated while adding to the sequence

        if (sequenceIndex >= 0) {
            throw new IllegalStateException("writeSequence already started");
        }

        writeICNSHeader();
        sequenceIndex = 0;
    }

    @Override
    public void endWriteSequence() throws IOException {
        assertOutput();

        if (sequenceIndex < 0) {
            throw new IllegalStateException("prepareWriteSequence not called");
        }

        // TODO: Now that we know the number of icon resources, we could move all data backwards
        // and write a TOC... But I don't think the benefit will outweigh the cost.

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

        long resourceStart = imageOutput.getStreamPosition();

        // TODO: Allow for other formats based on param?
        // - Uncompressed/RLE (only allowed up to 128x128)?
        // - JPEG2000 not very likely...

        // Validate icon size, get icon resource type based on size and compression
        // TODO: Allow smaller, centered in larger square? Resize?
        imageOutput.writeInt(IconResource.typeFromImage(image.getRenderedImage(), "PNG"));
        imageOutput.writeInt(0); // Size, update later

        processImageStarted(sequenceIndex);

        // Write icon in PNG format
        ImageWriter writer = getPNGDelegate();
        writer.setOutput(new SubImageOutputStream(imageOutput));
        writer.write(null, image, copyParam(param, writer));

        processImageComplete();

        long resourceEnd = imageOutput.getStreamPosition();
        if (resourceEnd > Integer.MAX_VALUE) {
            throw new IIOException("File too large for ICNS");
        }

        int length = (int) (resourceEnd - resourceStart);

        // Update file length field
        imageOutput.seek(4);
        imageOutput.writeInt((int) resourceEnd);

        // Update resource length field
        imageOutput.seek(resourceStart + 4);
        imageOutput.writeInt((length));

        // Prepare for next iteration
        imageOutput.seek(resourceEnd);
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

    private void writeICNSHeader() throws IOException {
        if (imageOutput.getStreamPosition() != 0) {
            throw new IllegalStateException("Stream already written to");
        }

        imageOutput.writeInt(ICNS.MAGIC);
        imageOutput.writeInt(8); // Length of file, in bytes, must be updated while writing
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
            ImageWriter writer = new ICNSImageWriter(null);
            writer.setOutput(out);

            ImageWriteParam param = writer.getDefaultWriteParam();
            // For now, we only support PNG...
//            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//            param.setCompressionType(pngCompression ? "BI_PNG" : "BI_RGB");

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
