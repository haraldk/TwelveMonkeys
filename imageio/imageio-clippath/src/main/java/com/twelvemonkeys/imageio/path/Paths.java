/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.path;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.metadata.psd.PSD;
import com.twelvemonkeys.imageio.metadata.psd.PSDReader;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.twelvemonkeys.lang.Validate.isTrue;
import static com.twelvemonkeys.lang.Validate.notNull;
import static java.util.Collections.singletonList;

/**
 * Support for various Adobe Photoshop Path related operations:
 * <ul>
 * <li>Extract a path from an image input stream, {@link #readPath}</li>
 * <li>Apply a given path to a given {@code BufferedImage} {@link #applyClippingPath}</li>
 * <li>Read an image with path applied {@link #readClipped}</li>
 * </ul>
 *
 * @see <a href="http://www.adobe.com/devnet-apps/photoshop/fileformatashtml/#50577409_17587">Adobe Photoshop Path resource format</a>
 * @see com.twelvemonkeys.imageio.path.AdobePathBuilder
 * @author <a href="mailto:jpalmer@itemmaster.com">Jason Palmer, itemMaster LLC</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: Paths.java,v 1.0 08/12/14 harald.kuhr Exp$
 */
public final class Paths {

    private Paths() {}

    /**
     * Reads the clipping path from the given input stream, if any.
     * Supports PSD, JPEG and TIFF as container formats for Photoshop resources,
     * or a "bare" PSD Image Resource Block.
     *
     * @param stream the input stream to read from, not {@code null}.
     * @return the path, or {@code null} if no path is found
     * @throws IOException if a general I/O exception occurs during reading.
     * @throws javax.imageio.IIOException if the input contains a bad path data.
     * @throws java.lang.IllegalArgumentException is {@code stream} is {@code null}.
     *
     * @see com.twelvemonkeys.imageio.path.AdobePathBuilder
     */
    public static Path2D readPath(final ImageInputStream stream) throws IOException {
        notNull(stream, "stream");

        int magic = readMagic(stream);

        if (magic == PSD.RESOURCE_TYPE) {
            // This is a PSD Image Resource Block, we can parse directly
            return buildPathFromPhotoshopResources(stream);
        }
        else if (magic == PSD.SIGNATURE_8BPS) {
            // PSD version
            // 4 byte magic, 2 byte version, 6 bytes reserved, 2 byte channels,
            // 4 byte height, 4 byte width, 2 byte bit depth, 2 byte mode
            stream.skipBytes(26);

            // 4 byte color mode data length + n byte color mode data
            long colorModeLen = stream.readUnsignedInt();
            stream.skipBytes(colorModeLen);

            // 4 byte image resources length
            long imageResourcesLen = stream.readUnsignedInt();

            // Image resources
            return buildPathFromPhotoshopResources(new SubImageInputStream(stream, imageResourcesLen));
        }
        else if (magic >>> 16 == JPEG.SOI && (magic & 0xff00) == 0xff00) {
            // JPEG version
            Map<Integer, java.util.List<String>> segmentIdentifiers = new LinkedHashMap<>();
            segmentIdentifiers.put(JPEG.APP13, singletonList("Photoshop 3.0"));

            List<JPEGSegment> photoshop = JPEGSegmentUtil.readSegments(stream, segmentIdentifiers);

            if (!photoshop.isEmpty()) {
                return buildPathFromPhotoshopResources(new MemoryCacheImageInputStream(photoshop.get(0).data()));
            }
        }
        else if (magic >>> 16 == TIFF.BYTE_ORDER_MARK_BIG_ENDIAN && (magic & 0xffff) == TIFF.TIFF_MAGIC
                || magic >>> 16 == TIFF.BYTE_ORDER_MARK_LITTLE_ENDIAN && (magic & 0xffff) == TIFF.TIFF_MAGIC << 8) {
            // TIFF version
            CompoundDirectory IFDs = (CompoundDirectory) new TIFFReader().read(stream);

            Directory directory = IFDs.getDirectory(0);
            Entry photoshop = directory.getEntryById(TIFF.TAG_PHOTOSHOP);

            if (photoshop != null) {
                return buildPathFromPhotoshopResources(new ByteArrayImageInputStream((byte[]) photoshop.getValue()));
            }
        }

        // Unknown file format, or no path found
        return null;
    }

    private static int readMagic(final ImageInputStream stream) throws IOException {
        stream.mark();

        try {
            return stream.readInt();
        }
        finally {
            stream.reset();
        }
    }

    private static Path2D buildPathFromPhotoshopResources(final ImageInputStream stream) throws IOException {
        Directory resourceBlocks = new PSDReader().read(stream);

        if (AdobePathBuilder.DEBUG) {
            System.out.println("resourceBlocks: " + resourceBlocks);
        }

        Entry resourceBlock = resourceBlocks.getEntryById(PSD.RES_CLIPPING_PATH);

        if (resourceBlock != null) {
            return new AdobePathBuilder((byte[]) resourceBlock.getValue()).path();
        }

        return null;
    }

    /**
     * Applies the clipping path to the given image.
     * All pixels outside the path will be transparent.
     *
     * @param clip the clipping path, not {@code null}
     * @param image the image to clip, not {@code null}
     * @return the clipped image.
     *
     * @throws java.lang.IllegalArgumentException if {@code clip} or {@code image} is {@code null}.
     */
    public static BufferedImage applyClippingPath(final Shape clip, final BufferedImage image) {
        return applyClippingPath(clip, notNull(image, "image"), new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB));
    }

    /**
     * Applies the clipping path to the given image.
     * Client code may decide the type of the {@code destination} image.
     * The {@code destination} image is assumed to be fully transparent,
     * and have same dimensions as {@code image}.
     * All pixels outside the path will be transparent.
     *
     * @param clip the clipping path, not {@code null}.
     * @param image the image to clip, not {@code null}.
     * @param destination the destination image, may not be {@code null} or same instance as {@code image}.
     * @return the clipped image.
     *
     * @throws java.lang.IllegalArgumentException if {@code clip}, {@code image} or {@code destination} is {@code null},
     * or if {@code destination} is the same instance as {@code image}.
     */
    public static BufferedImage applyClippingPath(final Shape clip, final BufferedImage image, final BufferedImage destination) {
        notNull(clip, "clip");
        notNull(image, "image");
        isTrue(destination != null && destination != image, "destination may not be null or same instance as image");

        Graphics2D g = destination.createGraphics();

        try {
            AffineTransform originalTransform = g.getTransform();

            // Fill the clip shape, with antialias, scaled up to the image's size
            g.scale(image.getWidth(), image.getHeight());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fill(clip);

            // Draw the image inside the clip shape
            g.setTransform(originalTransform);
            g.setComposite(AlphaComposite.SrcIn);
            g.drawImage(image, 0, 0, null);
        }
        finally {
            g.dispose();
        }

        return destination;
    }

    /**
     * Reads the clipping path from the given input stream, if any,
     * and applies it to the first image in the stream.
     * If no path was found, the image is returned without any clipping.
     * Supports PSD, JPEG and TIFF as container formats for Photoshop resources.
     *
     * @param stream the stream to read from, not {@code null}
     * @return the clipped image
     *
     * @throws IOException if a general I/O exception occurs during reading.
     * @throws javax.imageio.IIOException if the input contains a bad image or path data.
     * @throws java.lang.IllegalArgumentException is {@code stream} is {@code null}.
     */
    public static BufferedImage readClipped(final ImageInputStream stream) throws IOException {
        Shape clip = readPath(stream);

        stream.seek(0);
        BufferedImage image = ImageIO.read(stream);

        if (clip == null) {
            return image;
        }

        return applyClippingPath(clip, image);
    }

    // Test code
    public static void main(final String[] args) throws IOException, InterruptedException {
        BufferedImage destination = readClipped(ImageIO.createImageInputStream(new File(args[0])));

        File tempFile = File.createTempFile("clipped-", ".png");
        tempFile.deleteOnExit();
        ImageIO.write(destination, "PNG", tempFile);

        Desktop.getDesktop().open(tempFile);

        Thread.sleep(3000L);

        if (!tempFile.delete()) {
            System.err.printf("%s not deleted\n", tempFile);
        }
    }

}
