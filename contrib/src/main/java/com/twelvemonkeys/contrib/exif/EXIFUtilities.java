package com.twelvemonkeys.contrib.exif;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageReaderBase;

import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import static com.twelvemonkeys.contrib.tiff.TIFFUtilities.applyOrientation;

/**
 * EXIFUtilities.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version : EXIFUtilities.java,v 1.0 23/06/2020
 */
public class EXIFUtilities {
    /**
     * Reads image and metadata, applies Exif orientation to image, and returns everything as an {@code IIOImage}.
     * The returned {@code IIOImage} will always contain an image and no raster, and
     * the {@code RenderedImage} may be safely cast to a {@code BufferedImage}.
     *
     * If no registered {@code ImageReader} claims to be able to read the input, {@code null} is returned.
     *
     * @param input a {@code URL}
     * @return an {@code IIOImage} containing the correctly oriented image and metadata including rotation info, or
     * {@code null}.
     * @throws IOException if an error occurs during reading.
     */
    public static IIOImage readWithOrientation(final URL input) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            return readWithOrientation(stream);
        }
    }

    /**
     * Reads image and metadata, applies Exif orientation to image, and returns everything as an {@code IIOImage}.
     * The returned {@code IIOImage} will always contain an image and no raster, and
     * the {@code RenderedImage} may be safely cast to a {@code BufferedImage}.
     *
     * If no registered {@code ImageReader} claims to be able to read the input, {@code null} is returned.
     *
     * @param input an {@code InputStream}
     * @return an {@code IIOImage} containing the correctly oriented image and metadata including rotation info, or
     * {@code null}.
     * @throws IOException if an error occurs during reading.
     */
    public static IIOImage readWithOrientation(final InputStream input) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            return readWithOrientation(stream);
        }
    }

    /**
     * Reads image and metadata, applies Exif orientation to image, and returns everything as an {@code IIOImage}.
     * The returned {@code IIOImage} will always contain an image and no raster, and
     * the {@code RenderedImage} may be safely cast to a {@code BufferedImage}.
     *
     * If no registered {@code ImageReader} claims to be able to read the input, {@code null} is returned.
     *
     * @param input a {@code File}
     * @return an {@code IIOImage} containing the correctly oriented image and metadata including rotation info or
     * {@code null}.
     * @throws IOException if an error occurs during reading.
     */
    public static IIOImage readWithOrientation(final File input) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            return readWithOrientation(stream);
        }
    }

    /**
     * Reads image and metadata, applies Exif orientation to image, and returns everything as an {@code IIOImage}.
     * The returned {@code IIOImage} will always contain an image and no raster, and
     * the {@code RenderedImage} may be safely cast to a {@code BufferedImage}.
     *
     * If no registered {@code ImageReader} claims to be able to read the input, {@code null} is returned.
     *
     * @param input an {@code ImageInputStream}
     * @return an {@code IIOImage} containing the correctly oriented image and metadata including rotation info, or
     * {@code null}.
     * @throws IOException if an error occurs during reading.
     */
    public static IIOImage readWithOrientation(final ImageInputStream input) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) {
            return null;
        }

        ImageReader reader = readers.next();
        try {
            reader.setInput(input, true, false);

            IIOMetadata metadata = reader.getImageMetadata(0);
            BufferedImage bufferedImage = applyOrientation(reader.read(0), findImageOrientation(metadata).value());

            return new IIOImage(bufferedImage, null, metadata);
        }
        finally {
            reader.dispose();
        }
    }

    /**
     * Finds the {@code ImageOrientation} tag, if any, and returns an {@link Orientation} based on its
     * {@code value} attribute.
     * If no match is found or the tag is not present, {@code Normal} (the default orientation) is returned.
     *
     * @param metadata an {@code IIOMetadata} object
     * @return the {@code Orientation} matching the {@code value} attribute of the {@code ImageOrientation} tag,
     * or {@code Normal}, never {@code null}.
     * @see Orientation
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/standard_metadata.html">Standard (Plug-in Neutral) Metadata Format Specification</a>
     */
    public static Orientation findImageOrientation(final IIOMetadata metadata) {
        if (metadata != null) {
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName);
            NodeList imageOrientations = root.getElementsByTagName("ImageOrientation");

            if (imageOrientations != null && imageOrientations.getLength() > 0) {
                IIOMetadataNode imageOrientation = (IIOMetadataNode) imageOrientations.item(0);
                return Orientation.fromMetadataOrientation(imageOrientation.getAttribute("value"));
            }
        }

        return Orientation.Normal;
    }

    public static void main(String[] args) throws IOException {
        for (String arg : args) {
            File input = new File(arg);

            // Read everything but thumbnails (similar to ImageReader.readAll(0, null)),
            // and applies the correct image orientation
            IIOImage image = readWithOrientation(input);

            if (image == null) {
                System.err.printf("No reader for %s%n", input);
                continue;
            }

            // Finds the orientation as defined by the javax_imageio_1.0 format
            Orientation orientation = findImageOrientation(image.getMetadata());

            // Retrieve the image as a BufferedImage. The image is already rotated by the readWithOrientation method
            // In this case it will already be a BufferedImage, so using a cast will also do
            // (i.e.: BufferedImage bufferedImage = (BufferedImage) image.getRenderedImage())
            BufferedImage bufferedImage = ImageUtil.toBuffered(image.getRenderedImage());

            // Demo purpose only, show image with orientation details in title
            DisplayHelper.showIt(bufferedImage, input.getName() + ": " + orientation.name() + "/" + orientation.value());
        }
    }

    // Don't do this... :-) Provided for convenience/demo only!
    static abstract class DisplayHelper extends ImageReaderBase {
        private DisplayHelper() {
            super(null);
        }

        protected static void showIt(BufferedImage image, String title) {
            ImageReaderBase.showIt(image, title);
        }
    }
}
