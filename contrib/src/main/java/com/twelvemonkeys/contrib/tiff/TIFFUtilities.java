/*
 * Copyright (c) 2013, Oliver Schmidtmer, Harald Kuhr
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

package com.twelvemonkeys.contrib.tiff;

import com.twelvemonkeys.image.AffineTransformOp;
import com.twelvemonkeys.imageio.metadata.AbstractDirectory;
import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.tiff.*;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * TIFFUtilities for manipulation TIFF Images and Metadata
 *
 * @author <a href="mailto:mail@schmidor.de">Oliver Schmidtmer</a>
 * @author last modified by $Author$
 * @version $Id$
 */
public final class TIFFUtilities {
    private TIFFUtilities() {
    }

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
        CompoundDirectory IFDs = (CompoundDirectory) new TIFFReader().read(imageInput);

        final int pageCount = IFDs.directoryCount();
        List<TIFFPage> pages = new ArrayList<>(pageCount);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            pages.add(new TIFFPage(IFDs.getDirectory(pageIndex), imageInput));
        }

        return pages;
    }

    public static void writePages(ImageOutputStream imageOutput, List<TIFFPage> pages) throws IOException {
        TIFFWriter exif = new TIFFWriter();
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
                // rotated 180
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
                // rotated 90
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
                // rotated 270
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
        AffineTransformOp transformOp = new AffineTransformOp(transform, null);
        return transformOp.filter(input, null);
    }

    public static class TIFFPage {
        private Directory IFD;
        private ImageInputStream stream;

        private TIFFPage(Directory IFD, ImageInputStream stream) {
            this.IFD = IFD;
            this.stream = stream;
        }

        private long write(ImageOutputStream outputStream, TIFFWriter tiffWriter) throws IOException {
            List<Entry> newIFD = writeDirectoryData(IFD, outputStream);
            return tiffWriter.writeIFD(newIFD, outputStream);
        }

        private List<Entry> writeDirectoryData(Directory IFD, ImageOutputStream outputStream) throws IOException {
            ArrayList<Entry> newIFD = new ArrayList<Entry>();
            Iterator<Entry> it = IFD.iterator();
            while (it.hasNext()) {
                Entry e = it.next();
                if (e.getValue() instanceof Directory) {
                    List<Entry> subIFD = writeDirectoryData((Directory) e.getValue(), outputStream);
                    new TIFFEntry((Integer) e.getIdentifier(), TIFF.TYPE_IFD, new AbstractDirectory(subIFD) {
                    });
                }

                newIFD.add(e);
            }

            long[] offsets = new long[0];
            long[] byteCounts = new long[0];
            int[] newOffsets = new int[0];
            boolean useTiles = false;
            Entry stripOffsetsEntry = IFD.getEntryById(TIFF.TAG_STRIP_OFFSETS);
            Entry stripByteCountsEntry = IFD.getEntryById(TIFF.TAG_STRIP_BYTE_COUNTS);
            if (stripOffsetsEntry != null && stripByteCountsEntry != null) {
                offsets = getValueAsLongArray(stripOffsetsEntry);
                byteCounts = getValueAsLongArray(stripByteCountsEntry);
            }
            else {
                stripOffsetsEntry = IFD.getEntryById(TIFF.TAG_TILE_OFFSETS);
                stripByteCountsEntry = IFD.getEntryById(TIFF.TAG_TILE_BYTE_COUNTS);
                if (stripOffsetsEntry != null && stripByteCountsEntry != null) {
                    offsets = getValueAsLongArray(stripOffsetsEntry);
                    byteCounts = getValueAsLongArray(stripByteCountsEntry);
                    useTiles = true;
                }
            }

            int compression = -1;
            Entry compressionEntry = IFD.getEntryById(TIFF.TAG_COMPRESSION);
            if (compressionEntry != null && compressionEntry.getValue() instanceof Number) {
                compression = ((Number) compressionEntry.getValue()).shortValue();
            }

            boolean rearrangedByteStrips = false;
            Entry oldJpegData = IFD.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT);
            Entry oldJpegDataLength = IFD.getEntryById(TIFF.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
            long[] jpegByteCounts = null;
            long[] jpegOffsets = null;
            if (oldJpegData != null && oldJpegData.valueCount() > 0) {
                // convert JPEGInterchangeFormat to new-style-JPEG
                jpegByteCounts = new long[0];
                jpegOffsets = getValueAsLongArray(oldJpegData);
                if (oldJpegDataLength != null && oldJpegDataLength.valueCount() > 0) {
                    jpegByteCounts = getValueAsLongArray(oldJpegDataLength);
                }

                if (offsets.length == 1 && offsets[0] == jpegOffsets[0]) {
                    // JPEGInterchangeFormat identical to stripdata
                    newIFD.remove(oldJpegData);
                    newIFD.remove(oldJpegDataLength);
                }
                else if (offsets.length == 1 && oldJpegDataLength != null && offsets[0] == (jpegOffsets[0] + jpegByteCounts[0])) {
                    // prepend JPEGInterchangeFormat to stripdata
                    newOffsets = writeData(jpegOffsets, jpegByteCounts, outputStream);
                    writeData(offsets, byteCounts, outputStream);

                    newIFD.remove(stripOffsetsEntry);
                    newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_OFFSETS : TIFF.TAG_STRIP_OFFSETS, newOffsets));
                    newIFD.remove(stripByteCountsEntry);
                    newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_BYTE_COUNTS : TIFF.TAG_STRIP_BYTE_COUNTS, new int[]{(int) (jpegByteCounts[0] + byteCounts[0])}));

                    newIFD.remove(oldJpegData);
                    newIFD.remove(oldJpegDataLength);
                    rearrangedByteStrips = true;
                }
                else if (offsets.length == 1 && oldJpegDataLength != null && (jpegOffsets[0] < offsets[0]) && (jpegOffsets[0] + jpegByteCounts[0]) > (offsets[0] + byteCounts[0])) {

                    // ByteStrip contains only a part of JPEGInterchangeFormat
                    newOffsets = writeData(jpegOffsets, jpegByteCounts, outputStream);

                    newIFD.remove(stripOffsetsEntry);
                    newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_OFFSETS : TIFF.TAG_STRIP_OFFSETS, newOffsets));
                    newIFD.remove(stripByteCountsEntry);
                    newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_BYTE_COUNTS : TIFF.TAG_STRIP_BYTE_COUNTS, new int[]{(int) (jpegByteCounts[0])}));

                    newIFD.remove(oldJpegData);
                    newIFD.remove(oldJpegDataLength);
                    rearrangedByteStrips = true;
                }
                else if (oldJpegDataLength != null) {
                    // multiple bytestrips
                    // search for SOF on first strip and copy to each if needed
                    newIFD.remove(oldJpegData);
                    newIFD.remove(oldJpegDataLength);
                    stream.seek(jpegOffsets[0]);
                    byte[] jpegInterchangeData = new byte[(int) jpegByteCounts[0]];
                    stream.readFully(jpegInterchangeData);

                    stream.seek(offsets[0]);
                    byte[] sosMarker;
                    if (stream.read() == 0xff && stream.read() == 0xda) {
                        int sosLength = (stream.read() << 8) | stream.read();
                        sosMarker = new byte[sosLength + 2];
                        sosMarker[0] = (byte) 0xff;
                        sosMarker[1] = (byte) 0xda;
                        sosMarker[2] = (byte) ((sosLength & 0xff00) >> 8);
                        sosMarker[3] = (byte) (sosLength & 0xff);
                        stream.readFully(sosMarker, 4, sosLength - 2);
                    }
                    else {
                        throw new IOException("Old-style-JPEG with multiple strips are only supported, if first strip contains SOS");
                    }


                    newOffsets = new int[offsets.length];
                    int[] newByteCounts = new int[byteCounts.length];
                    for (int i = 0; i < offsets.length; i++) {
                        newOffsets[i] = (int) outputStream.getStreamPosition();
                        outputStream.write(jpegInterchangeData);
                        stream.seek(offsets[i]);

                        byte[] buffer = new byte[(int) byteCounts[i]];
                        newByteCounts[i] = (int) (jpegInterchangeData.length + byteCounts[i]);
                        stream.readFully(buffer);
                        if (buffer[0] != ((byte) 0xff) || buffer[1] != ((byte) 0xda)) {
                            outputStream.write(sosMarker);
                            newByteCounts[i] += sosMarker.length;
                        }
                        outputStream.write(buffer);
                    }

                    newIFD.remove(stripOffsetsEntry);
                    newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_OFFSETS : TIFF.TAG_STRIP_OFFSETS, newOffsets));
                    newIFD.remove(stripByteCountsEntry);
                    newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_BYTE_COUNTS : TIFF.TAG_STRIP_BYTE_COUNTS, newByteCounts));

                    newIFD.remove(oldJpegData);
                    newIFD.remove(oldJpegDataLength);
                    rearrangedByteStrips = true;
                }
            }
            else if (compression == TIFFExtension.COMPRESSION_OLD_JPEG) {
                // old-style but no JPEGInterchangeFormat

                long[] yCbCrSubSampling = getValueAsLongArray(IFD.getEntryById(TIFF.TAG_YCBCR_SUB_SAMPLING));
                int subsampling = yCbCrSubSampling != null
                        ? (int) ((yCbCrSubSampling[0] & 0xf) << 4 | yCbCrSubSampling[1] & 0xf)
                        : 0x22;
                int bands = ((Number) IFD.getEntryById(TIFF.TAG_SAMPLES_PER_PIXEL).getValue()).intValue();

                int w = ((Number) IFD.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue()).intValue();
                int h = ((Number) IFD.getEntryById(TIFF.TAG_IMAGE_HEIGHT).getValue()).intValue();

                int r = ((Number) (useTiles ? IFD.getEntryById(TIFF.TAG_TILE_HEIGTH) : IFD.getEntryById(TIFF.TAG_ROWS_PER_STRIP)).getValue()).intValue();
                int c = useTiles ? ((Number) IFD.getEntryById(TIFF.TAG_TILE_WIDTH).getValue()).intValue() : w;

                newOffsets = new int[offsets.length];
                int[] newByteCounts = new int[byteCounts.length];

                // No JPEGInterchangeFormat
                for (int i = 0; i < offsets.length; i++) {
                    byte[] start = new byte[2];
                    stream.seek(offsets[i]);
                    stream.readFully(start);
                    newOffsets[i] = (int) outputStream.getStreamPosition();
                    if (start[0] == ((byte) 0xff) && start[1] == ((byte) 0xd8)) {
                        // full image stream, nothing to do
                        writeData(stream, outputStream, offsets[i], byteCounts[i]);
                    }
                    else if (start[0] == ((byte) 0xff) && start[1] == ((byte) 0xda)) {
                        // starts with SOS
                        outputStream.writeShort(JPEG.SOI);
                        writeSOF0(outputStream, bands, c, r, subsampling);
                        writeData(stream, outputStream, offsets[i], byteCounts[i]);
                        outputStream.writeShort(JPEG.EOI);
                    }
                    else {
                        // raw data
                        outputStream.writeShort(JPEG.SOI);
                        writeSOF0(outputStream, bands, c, r, subsampling);
                        writeSOS(outputStream, bands);
                        writeData(stream, outputStream, offsets[i], byteCounts[i]);
                        outputStream.writeShort(JPEG.EOI);
                    }
                    newByteCounts[i] = ((int) outputStream.getStreamPosition()) - newOffsets[i];
                }

                newIFD.remove(stripOffsetsEntry);
                newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_OFFSETS : TIFF.TAG_STRIP_OFFSETS, newOffsets));
                newIFD.remove(stripByteCountsEntry);
                newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_BYTE_COUNTS : TIFF.TAG_STRIP_BYTE_COUNTS, newByteCounts));
                rearrangedByteStrips = true;
            }

            if (!rearrangedByteStrips && stripOffsetsEntry != null && stripByteCountsEntry != null) {
                newOffsets = writeData(offsets, byteCounts, outputStream);

                newIFD.remove(stripOffsetsEntry);
                newIFD.add(new TIFFEntry(useTiles ? TIFF.TAG_TILE_OFFSETS : TIFF.TAG_STRIP_OFFSETS, newOffsets));
            }

            if ((oldJpegData != null && newIFD.contains(oldJpegData)) || (oldJpegDataLength != null && newIFD.contains(oldJpegDataLength))) {
                throw new IOException("Failed to transform old-style JPEG");
            }

            Entry oldJpegTableQ, oldJpegTableDC, oldJpegTableAC;
            oldJpegTableQ = IFD.getEntryById(TIFF.TAG_OLD_JPEG_Q_TABLES);
            oldJpegTableDC = IFD.getEntryById(TIFF.TAG_OLD_JPEG_DC_TABLES);
            oldJpegTableAC = IFD.getEntryById(TIFF.TAG_OLD_JPEG_AC_TABLES);
            if ((oldJpegTableQ != null) || (oldJpegTableDC != null) || (oldJpegTableAC != null)) {
                if (IFD.getEntryById(TIFF.TAG_JPEG_TABLES) != null) {
                    throw new IOException("Found old-style and new-style JPEGTables");
                }

                boolean tablesInStream = jfifContainsTables(oldJpegTableQ, jpegOffsets, jpegByteCounts);
                tablesInStream &= jfifContainsTables(oldJpegTableDC, jpegOffsets, jpegByteCounts);
                tablesInStream &= jfifContainsTables(oldJpegTableAC, jpegOffsets, jpegByteCounts);
                if (!tablesInStream) {
                    // merge them only to JPEGTables if they are not already contained within the stream
                    Entry jpegTables = mergeTables(oldJpegTableQ, oldJpegTableDC, oldJpegTableAC);
                    if (jpegTables != null) {
                        newIFD.add(jpegTables);
                    }
                }
                if (oldJpegTableQ != null) {
                    newIFD.remove(oldJpegTableQ);
                }
                if (oldJpegTableDC != null) {
                    newIFD.remove(oldJpegTableDC);
                }
                if (oldJpegTableAC != null) {
                    newIFD.remove(oldJpegTableAC);
                }
            }

            if (compressionEntry != null && compression == TIFFExtension.COMPRESSION_OLD_JPEG) {
                newIFD.remove(compressionEntry);
                newIFD.add(new TIFFEntry(TIFF.TAG_COMPRESSION, TIFF.TYPE_SHORT, TIFFExtension.COMPRESSION_JPEG));
            }
            return newIFD;
        }

        //TODO merge/extract from TIFFReader Jpeg/6 stream reconstruction
        private void writeSOF0(ImageOutputStream outputStream, int bands, int width, int height, int subsampling) throws IOException {
            outputStream.writeShort(JPEG.SOF0); // TODO: Use correct process for data
            outputStream.writeShort(2 + 6 + 3 * bands); // SOF0 len
            outputStream.writeByte(8); // bits TODO: Consult raster/transfer type or BitsPerSample for 12/16 bits support
            outputStream.writeShort(height); // height
            outputStream.writeShort(width); // width
            outputStream.writeByte(bands); // Number of components

            for (int comp = 0; comp < bands; comp++) {
                outputStream.writeByte(comp); // Component id
                outputStream.writeByte(comp == 0 ? subsampling : 0x11); // h/v subsampling
                outputStream.writeByte(comp); // Q table selector TODO: Consider merging if tables are equal, correct selection if only 1 or 2 valid tables are contained
            }
        }

        //TODO merge/extract from TIFFReader Jpeg/6 stream reconstruction
        private void writeSOS(ImageOutputStream outputStream, int bands) throws IOException {
            outputStream.writeShort(JPEG.SOS);
            outputStream.writeShort(6 + 2 * bands); // SOS length
            outputStream.writeByte(bands); // Num comp

            for (int component = 0; component < bands; component++) {
                outputStream.writeByte(component); // Comp id
                outputStream.writeByte(component == 0 ? component : 0x10 + (component & 0xf)); // dc/ac selector TODO: correct selection if only 1 or 2 valid tables are contained
            }

            outputStream.writeByte(0); // Spectral selection start
            outputStream.writeByte(0); // Spectral selection end
            outputStream.writeByte(0); // Approx high & low
        }

        private void writeData(ImageInputStream input, ImageOutputStream output, long offset, long length) throws IOException {
            input.seek(offset);
            byte[] buffer = new byte[(int) length];
            stream.readFully(buffer);
            output.write(buffer);
        }

        private boolean jfifContainsTables(Entry tableEntry, long[] jpegOffsets, long[] jpegLengths) throws IOException {
            if (jpegLengths == null || jpegOffsets == null || jpegLengths.length == 0) return false;
            if (tableEntry != null) {
                long[] tableOffsets = getValueAsLongArray(tableEntry);
                for (long offset : tableOffsets) {
                    if (offset < jpegOffsets[0] || offset > (jpegOffsets[0] + jpegLengths[0])) {
                        return false;
                    }
                }
            }
            return true;
        }

        //TODO merge/extract from TIFFReader Jpeg/6 stream reconstruction
        private Entry mergeTables(Entry qEntry, Entry dcEntry, Entry acEntry) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeShort(JPEG.SOI);

            if (qEntry != null && qEntry.valueCount() > 0) {
                long[] off = getValueAsLongArray(qEntry);
                byte[] table = new byte[64];
                for (int tableId = 0; tableId < off.length; tableId++) {
                    try {
                        stream.seek(off[tableId]);
                        stream.readFully(table);
                        dos.writeShort(JPEG.DQT);
                        dos.writeShort(3 + 64);
                        dos.writeByte(tableId);
                        dos.write(table);
                    } catch (EOFException e) {
                        // invalid table pointer, ignore
                    }
                }
            }

            // same marker for AC & DC tables, distinguished by flag in tableId
            if (dcEntry != null && dcEntry.valueCount() > 0) {
                long[] off = getValueAsLongArray(dcEntry);
                for (int tableId = 0; tableId < off.length; tableId++) {
                    try {
                        stream.seek(off[tableId]);
                        byte[] table = readHUFFTable();
                        if (table.length > (16 + 17)) {
                            // to long, table is invalid, just ignoe
                            continue;
                        }
                        dos.writeShort(JPEG.DHT);
                        dos.writeShort(3 + table.length);
                        dos.writeByte(tableId);
                        dos.write(table);
                    } catch (EOFException e) {
                        // invalid table pointer, ignore
                    }
                }
            }

            if (acEntry != null && acEntry.valueCount() > 0) {
                long[] off = getValueAsLongArray(acEntry);
                for (int tableId = 0; tableId < off.length; tableId++) {
                    try {
                        stream.seek(off[tableId]);
                        byte[] table = readHUFFTable();
                        if (table.length > (16 + 256)) {
                            // to long, table is invalid, just ignoe
                            continue;
                        }
                        dos.writeShort(JPEG.DHT);
                        dos.writeShort(3 + table.length);
                        dos.writeByte(16 | tableId);
                        dos.write(table);
                    } catch (EOFException e) {
                        // invalid table pointer, ignore
                    }
                }
            }

            dos.writeShort(JPEG.EOI);

            bos.close();
            if (bos.size() == 4) {
                // no valid tables, don't add
                return null;
            }

            return new TIFFEntry(TIFF.TAG_JPEG_TABLES, TIFF.TYPE_UNDEFINED, bos.toByteArray());
        }

        private byte[] readHUFFTable() throws IOException {
            byte[] lengths = new byte[16];
            stream.readFully(lengths);
            int numCodes = 0;
            for (int i = 0; i < lengths.length; i++) {
                numCodes += ((int) lengths[i]) & 0xff;
            }
            byte table[] = new byte[16 + numCodes];
            System.arraycopy(lengths, 0, table, 0, 16);
            stream.readFully(table, 16, numCodes);
            return table;
        }

        private int[] writeData(long[] offsets, long[] byteCounts, ImageOutputStream outputStream) throws IOException {
            int[] newOffsets = new int[offsets.length];
            for (int i = 0; i < offsets.length; i++) {
                newOffsets[i] = (int) outputStream.getStreamPosition();
                stream.seek(offsets[i]);

                byte[] buffer = new byte[(int) byteCounts[i]];
                try {
                    stream.readFully(buffer);
                } catch (EOFException e) {
                    // invalid strip length
                }
                outputStream.write(buffer);
            }
            return newOffsets;
        }

        private long[] getValueAsLongArray(Entry entry) throws IIOException {
            //TODO: code duplication from TIFFReader, should be extracted to metadata api
            long[] value;

            if (entry.valueCount() == 1) {
                // For single entries, this will be a boxed type
                value = new long[]{((Number) entry.getValue()).longValue()};
            }
            else if (entry.getValue() instanceof short[]) {
                short[] shorts = (short[]) entry.getValue();
                value = new long[shorts.length];

                for (int i = 0, length = value.length; i < length; i++) {
                    value[i] = shorts[i];
                }
            }
            else if (entry.getValue() instanceof int[]) {
                int[] ints = (int[]) entry.getValue();
                value = new long[ints.length];

                for (int i = 0, length = value.length; i < length; i++) {
                    value[i] = ints[i];
                }
            }
            else if (entry.getValue() instanceof long[]) {
                value = (long[]) entry.getValue();
            }
            else {
                throw new IIOException(String.format("Unsupported %s type: %s (%s)", entry.getFieldName(), entry.getTypeName(), entry.getValue().getClass()));
            }

            return value;
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

            newIDFData.add(new TIFFEntry(TIFF.TAG_ORIENTATION, (short) orientation));
            IFD = new IFD(newIDFData);
        }
    }

    public interface TIFFExtension {
        int ORIENTATION_TOPRIGHT = 2;
        int ORIENTATION_BOTRIGHT = 3;
        int ORIENTATION_BOTLEFT = 4;
        int ORIENTATION_LEFTTOP = 5;
        int ORIENTATION_RIGHTTOP = 6;
        int ORIENTATION_RIGHTBOT = 7;
        int ORIENTATION_LEFTBOT = 8;

        /**
         * Deprecated. For backwards compatibility only ("Old-style" JPEG).
         */
        int COMPRESSION_OLD_JPEG = 6;
        /**
         * JPEG Compression (lossy).
         */
        int COMPRESSION_JPEG = 7;
    }

    public interface TIFFBaseline {
        int ORIENTATION_TOPLEFT = 1;
    }
}
