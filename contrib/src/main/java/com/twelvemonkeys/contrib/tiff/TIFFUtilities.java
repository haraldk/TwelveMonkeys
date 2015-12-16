/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.contrib.tiff;

import com.twelvemonkeys.imageio.metadata.AbstractDirectory;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;
import com.twelvemonkeys.imageio.metadata.exif.EXIFWriter;
import com.twelvemonkeys.imageio.metadata.exif.TIFF;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFBaseline;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFExtension;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter.TIFFEntry;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TIFFUtilities for manipulation TIFF Images and Metadata
 *
 * @author <a href="mailto:mail@schmidor.de">Oliver Schmidtmer</a>
 * @author last modified by $Author$
 * @version $Id$
 */
public class TIFFUtilities {
    private TIFFUtilities() {
    }

    ;

    /**
     * Merges all pages from the input TIFF files into one TIFF file at the
     * output location.
     *
     * @param inputFiles
     * @param outputFile
     * @throws IOException
     */
    public static void merge(List<File> inputFiles, File outputFile) throws IOException {
        ImageOutputStream output = null;
        try {
            output = ImageIO.createImageOutputStream(outputFile);

            for (File file : inputFiles) {
                ImageInputStream input = null;
                try {
                    input = ImageIO.createImageInputStream(file);
                    List<TIFFPage> pages = getPages(input);
                    writePages(output, pages);
                }
                finally {
                    if (input != null) {
                        input.close();
                    }
                }
            }
        }
        finally {
            if (output != null) {
                output.flush();
                output.close();
            }
        }
    }

    /**
     * Splits all pages from the input TIFF file to one file per page in the
     * output directory.
     *
     * @param inputFile
     * @param outputDirectory
     * @return generated files
     * @throws IOException
     */
    public static List<File> split(File inputFile, File outputDirectory) throws IOException {
        ImageInputStream input = null;
        List<File> outputFiles = new ArrayList<>();
        try {
            input = ImageIO.createImageInputStream(inputFile);
            List<TIFFPage> pages = getPages(input);
            int pageNo = 1;
            for (TIFFPage tiffPage : pages) {
                ArrayList<TIFFPage> outputPages = new ArrayList<TIFFPage>(1);
                ImageOutputStream outputStream = null;
                try {
                    File outputFile = new File(outputDirectory, String.format("%04d", pageNo) + ".tif");
                    outputStream = ImageIO.createImageOutputStream(outputFile);
                    outputPages.clear();
                    outputPages.add(tiffPage);
                    writePages(outputStream, outputPages);
                    outputFiles.add(outputFile);
                }
                finally {
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                }
                ++pageNo;
            }
        }
        finally {
            if (input != null) {
                input.close();
            }
        }
        return outputFiles;
    }

    /**
     * Rotates all pages of a TIFF file by changing TIFF.TAG_ORIENTATION.
     * <p>
     * NOTICE: TIFF.TAG_ORIENTATION is an advice how the image is meant do be
     * displayed. Other metadata, such as width and height, relate to the image
     * as how it is stored. The ImageIO TIFF plugin does not handle orientation.
     * Use {@link TIFFUtilities#applyOrientation(BufferedImage, int)} for
     * applying TIFF.TAG_ORIENTATION.
     * </p>
     *
     * @param imageInput
     * @param imageOutput
     * @param degree      Rotation amount, supports 90�, 180� and 270�.
     * @throws IOException
     */
    public static void rotatePages(ImageInputStream imageInput, ImageOutputStream imageOutput, int degree)
            throws IOException {
        rotatePage(imageInput, imageOutput, degree, -1);
    }

    /**
     * Rotates a page of a TIFF file by changing TIFF.TAG_ORIENTATION.
     * <p>
     * NOTICE: TIFF.TAG_ORIENTATION is an advice how the image is meant do be
     * displayed. Other metadata, such as width and height, relate to the image
     * as how it is stored. The ImageIO TIFF plugin does not handle orientation.
     * Use {@link TIFFUtilities#applyOrientation(BufferedImage, int)} for
     * applying TIFF.TAG_ORIENTATION.
     * </p>
     *
     * @param imageInput
     * @param imageOutput
     * @param degree      Rotation amount, supports 90�, 180� and 270�.
     * @param pageIndex   page which should be rotated or -1 for all pages.
     * @throws IOException
     */
    public static void rotatePage(ImageInputStream imageInput, ImageOutputStream imageOutput, int degree, int pageIndex)
            throws IOException {
        ImageInputStream input = null;
        try {
            List<TIFFPage> pages = getPages(imageInput);
            if (pageIndex != -1) {
                pages.get(pageIndex).rotate(degree);
            }
            else {
                for (TIFFPage tiffPage : pages) {
                    tiffPage.rotate(degree);
                }
            }
            writePages(imageOutput, pages);
        }
        finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public static List<TIFFPage> getPages(ImageInputStream imageInput) throws IOException {
        ArrayList<TIFFPage> pages = new ArrayList<TIFFPage>();

        CompoundDirectory IFDs = (CompoundDirectory) new EXIFReader().read(imageInput);

        int pageCount = IFDs.directoryCount();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            pages.add(new TIFFPage(IFDs.getDirectory(pageIndex), imageInput));
        }

        return pages;
    }

    public static void writePages(ImageOutputStream imageOutput, List<TIFFPage> pages) throws IOException {
        EXIFWriter exif = new EXIFWriter();
        long nextPagePos = imageOutput.getStreamPosition();
        if (nextPagePos == 0) {
            exif.writeTIFFHeader(imageOutput);
            nextPagePos = imageOutput.getStreamPosition();
            imageOutput.writeInt(0);
        }
        else {
            // already has pages, so remember place of EOF to replace with
            // IFD offset
            nextPagePos -= 4;
        }

        for (TIFFPage tiffPage : pages) {
            long ifdOffset = tiffPage.write(imageOutput, exif);

            long tmp = imageOutput.getStreamPosition();
            imageOutput.seek(nextPagePos);
            imageOutput.writeInt((int) ifdOffset);
            imageOutput.seek(tmp);
            nextPagePos = tmp;
            imageOutput.writeInt(0);
        }
    }

    public static BufferedImage applyOrientation(BufferedImage input, int orientation) {
        boolean flipExtends = false;
        int w = input.getWidth();
        int h = input.getHeight();
        double cW = w / 2.0;
        double cH = h / 2.0;

        AffineTransform orientationTransform = new AffineTransform();
        switch (orientation) {
            case TIFFBaseline.ORIENTATION_TOPLEFT:
                // normal
                return input;
            case TIFFExtension.ORIENTATION_TOPRIGHT:
                // flipped vertically
                orientationTransform.translate(cW, cH);
                orientationTransform.scale(-1, 1);
                orientationTransform.translate(-cW, -cH);
                break;
            case TIFFExtension.ORIENTATION_BOTRIGHT:
                // rotated 180�
                orientationTransform.quadrantRotate(2, cW, cH);
                break;
            case TIFFExtension.ORIENTATION_BOTLEFT:
                // flipped horizontally
                orientationTransform.translate(cW, cH);
                orientationTransform.scale(1, -1);
                orientationTransform.translate(-cW, -cH);
                break;
            case TIFFExtension.ORIENTATION_LEFTTOP:
                orientationTransform.translate(cW, cH);
                orientationTransform.scale(-1, 1);
                orientationTransform.quadrantRotate(1);
                orientationTransform.translate(-cW, -cH);
                flipExtends = true;
                break;
            case TIFFExtension.ORIENTATION_RIGHTTOP:
                // rotated 90�
                orientationTransform.quadrantRotate(1, cW, cH);
                flipExtends = true;
                break;
            case TIFFExtension.ORIENTATION_RIGHTBOT:
                orientationTransform.translate(cW, cH);
                orientationTransform.scale(1, -1);
                orientationTransform.quadrantRotate(1);
                orientationTransform.translate(-cW, -cH);
                flipExtends = true;
                break;
            case TIFFExtension.ORIENTATION_LEFTBOT:
                // rotated 270�
                orientationTransform.quadrantRotate(3, cW, cH);
                flipExtends = true;
                break;
        }

        int newW, newH;
        if (flipExtends) {
            newW = h;
            newH = w;
        }
        else {
            newW = w;
            newH = h;
        }

        AffineTransform transform = AffineTransform.getTranslateInstance((newW - w) / 2.0, (newH - h) / 2.0);
        transform.concatenate(orientationTransform);

        BufferedImage output = new BufferedImage(newW, newH, input.getType());
        ((Graphics2D) output.getGraphics()).drawImage(input, transform, null);

        return output;
    }

    public static class TIFFPage {
        private Directory IFD;
        private ImageInputStream stream;

        private TIFFPage(Directory IFD, ImageInputStream stream) {
            this.IFD = IFD;
            this.stream = stream;
        }

        private long write(ImageOutputStream outputStream, EXIFWriter exifWriter) throws IOException {
            Entry stipOffsetsEntry = IFD.getEntryById(TIFF.TAG_STRIP_OFFSETS);
            long[] offsets;
            if (stipOffsetsEntry.valueCount() == 1) {
                offsets = new long[] {(long) stipOffsetsEntry.getValue()};
            }
            else {
                offsets = (long[]) stipOffsetsEntry.getValue();
            }

            Entry stipByteCountsEntry = IFD.getEntryById(TIFF.TAG_STRIP_BYTE_COUNTS);
            long[] byteCounts;
            if (stipOffsetsEntry.valueCount() == 1) {
                byteCounts = new long[] {(long) stipByteCountsEntry.getValue()};
            }
            else {
                byteCounts = (long[]) stipByteCountsEntry.getValue();
            }

            int[] newOffsets = new int[offsets.length];
            for (int i = 0; i < offsets.length; i++) {
                newOffsets[i] = (int) outputStream.getStreamPosition();
                stream.seek(offsets[i]);

                byte[] buffer = new byte[(int) byteCounts[i]];
                stream.readFully(buffer);
                outputStream.write(buffer);
            }

            ArrayList<Entry> newIFD = new ArrayList<Entry>();
            Iterator<Entry> it = IFD.iterator();
            while (it.hasNext()) {
                newIFD.add(it.next());
            }

            newIFD.remove(stipOffsetsEntry);
            newIFD.add(new TIFFEntry(TIFF.TAG_STRIP_OFFSETS, newOffsets));
            return exifWriter.writeIFD(newIFD, outputStream);
        }

        /**
         * Rotates the image by changing TIFF.TAG_ORIENTATION.
         * <p>
         * NOTICE: TIFF.TAG_ORIENTATION is an advice how the image is meant do
         * be displayed. Other metadata, such as width and height, relate to the
         * image as how it is stored. The ImageIO TIFF plugin does not handle
         * orientation. Use
         * {@link TIFFUtilities#applyOrientation(BufferedImage, int)} for
         * applying TIFF.TAG_ORIENTATION.
         * </p>
         *
         * @param degree Rotation amount, supports 90�, 180� and 270�.
         */
        public void rotate(int degree) {
            Validate.isTrue(degree % 90 == 0 && degree > 0 && degree < 360,
                    "Only rotations by 90, 180 and 270 degree are supported");

            ArrayList<Entry> newIDFData = new ArrayList<>();
            Iterator<Entry> it = IFD.iterator();
            while (it.hasNext()) {
                newIDFData.add(it.next());
            }

            short orientation = TIFFBaseline.ORIENTATION_TOPLEFT;
            Entry orientationEntry = IFD.getEntryById(TIFF.TAG_ORIENTATION);
            if (orientationEntry != null) {
                orientation = ((Number) orientationEntry.getValue()).shortValue();
                newIDFData.remove(orientationEntry);
            }

            int steps = degree / 90;
            for (int i = 0; i < steps; i++) {
                switch (orientation) {
                    case TIFFBaseline.ORIENTATION_TOPLEFT:
                        orientation = TIFFExtension.ORIENTATION_RIGHTTOP;
                        break;
                    case TIFFExtension.ORIENTATION_TOPRIGHT:
                        orientation = TIFFExtension.ORIENTATION_RIGHTBOT;
                        break;
                    case TIFFExtension.ORIENTATION_BOTRIGHT:
                        orientation = TIFFExtension.ORIENTATION_LEFTBOT;
                        break;
                    case TIFFExtension.ORIENTATION_BOTLEFT:
                        orientation = TIFFExtension.ORIENTATION_LEFTTOP;
                        break;
                    case TIFFExtension.ORIENTATION_LEFTTOP:
                        orientation = TIFFExtension.ORIENTATION_TOPRIGHT;
                        break;
                    case TIFFExtension.ORIENTATION_RIGHTTOP:
                        orientation = TIFFExtension.ORIENTATION_BOTRIGHT;
                        break;
                    case TIFFExtension.ORIENTATION_RIGHTBOT:
                        orientation = TIFFExtension.ORIENTATION_BOTLEFT;
                        break;
                    case TIFFExtension.ORIENTATION_LEFTBOT:
                        orientation = TIFFBaseline.ORIENTATION_TOPLEFT;
                        break;
                }
            }
            newIDFData.add(new TIFFImageWriter.TIFFEntry(TIFF.TAG_ORIENTATION, (short) orientation));
            IFD = new AbstractDirectory(newIDFData) {
            };
        }
    }
}
