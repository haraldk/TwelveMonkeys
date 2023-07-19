/*
 * Copyright (c) 2009, Harald Kuhr
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

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.util.WeakWeakMap;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.color.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * ImageReader for Microsoft Windows ICO (icon) format.
 * 1, 4, 8 bit palette support with bitmask transparency, and 16, 24 and 32 bit
 * true color support with alpha. Also supports Windows Vista PNG encoded icons.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: ICOImageReader.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
 *
 * @see <a href="http://en.wikipedia.org/wiki/BMP_file_format">BMP file format (Wikipedia)</a>
 * @see <a href="http://en.wikipedia.org/wiki/ICO_(icon_image_file_format)">ICO file format (Wikipedia)</a>
 */
// SEE http://en.wikipedia.org/wiki/ICO_(icon_image_file_format)
// TODO: Decide whether DirectoryEntry or DIBHeader should be primary source for color count/bit count
// TODO: Support loading icons from DLLs, see
// <a href="http://msdn.microsoft.com/library/default.asp?url=/library/en-us/dnwui/html/msdn_icons.asp">MSDN</a>
// Known issue: 256x256 PNG encoded icons does not have IndexColorModel even if stated in DirectoryEntry
// (seem impossible as the PNGs are all true color)
abstract class DIBImageReader extends ImageReaderBase {
    // TODO: Consider moving the reading to inner classes (subclasses of BitmapDescriptor)
    private Directory directory;

    // TODO: Review these, make sure we don't have a memory leak
    private final Map<DirectoryEntry, DIBHeader> headers = new WeakHashMap<>();
    private final Map<DirectoryEntry, BitmapDescriptor> descriptors = new WeakWeakMap<>();

    private ImageReader pngImageReader;

    protected DIBImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    protected void resetMembers() {
        directory = null;

        headers.clear();
        descriptors.clear();

        if (pngImageReader != null) {
            pngImageReader.dispose();
            pngImageReader = null;
        }
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        DirectoryEntry entry = getEntry(imageIndex);

        // NOTE: Delegate to PNG reader
        if (isPNG(entry)) {
            return getImageTypesPNG(entry);
        }

        List<ImageTypeSpecifier> types = new ArrayList<>();
        DIBHeader header = getHeader(entry);

        // Use data from header to create specifier
        ImageTypeSpecifier specifier;
        switch (header.getBitCount()) {
            case 1:
            case 2:
            case 4:
            case 8:
                // TODO: This is slightly QnD...
                int offset = entry.getOffset() + header.getSize();
                if (offset != imageInput.getStreamPosition()) {
                    imageInput.seek(offset);
                }
                BitmapIndexed indexed = new BitmapIndexed(entry, header);
                readColorMap(indexed);
                specifier = ImageTypeSpecifiers.createFromIndexColorModel(indexed.createColorModel());
                break;
            case 16:
                // TODO: May have mask?!
                specifier = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_USHORT_555_RGB);
                break;
            case 24:
                specifier = new BitmapRGB(entry, header).hasMask()
                            ? ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_4BYTE_ABGR)
                            : ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR);
                break;
            case 32:
                specifier = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
                break;
            default:
                throw new IIOException(String.format("Unknown bit depth: %d", header.getBitCount()));
        }

        types.add(specifier);

        return types.iterator();
    }

    @Override
    public int getNumImages(final boolean allowSearch) throws IOException {
        return getDirectory().count();
    }

    public int getWidth(final int imageIndex) throws IOException {
        return getEntry(imageIndex).getWidth();
    }

    public int getHeight(final int imageIndex) throws IOException {
        return getEntry(imageIndex).getHeight();
    }

    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        checkBounds(imageIndex);

        processImageStarted(imageIndex);

        DirectoryEntry entry = getEntry(imageIndex);

        BufferedImage destination;

        if (isPNG(entry)) {
            // NOTE: Special case for Windows Vista, 256x256 PNG encoded images, with no DIB header...
            destination = readPNG(entry, param);
        }
        else {
            // NOTE: If param does not have explicit destination, we'll try to create a BufferedImage later,
            //       to allow for storing the cursor hotspot for CUR images
            destination = hasExplicitDestination(param) ?
                    getDestination(param, getImageTypes(imageIndex), getWidth(imageIndex), getHeight(imageIndex)) : null;

            BufferedImage image = readBitmap(entry);

            // TODO: Handle AOI and subsampling inline, probably not of big importance...
            if (param != null) {
                image = fakeAOI(image, param);
                image = ImageUtil.toBuffered(fakeSubsampling(image, param));
            }

            if (destination == null) {
                // This is okay, as long as the client did not request explicit destination image/type
                destination = image;
            }
            else {
                Graphics2D g = destination.createGraphics();

                try {
                    g.setComposite(AlphaComposite.Src);
                    g.drawImage(image, 0, 0, null);
                }
                finally {
                    g.dispose();
                }
            }
        }

        processImageProgress(100);
        processImageComplete();

        return destination;
    }

    private boolean isPNG(final DirectoryEntry entry) throws IOException {
        long magic;

        imageInput.seek(entry.getOffset());
        imageInput.setByteOrder(ByteOrder.BIG_ENDIAN);

        try {
            magic = imageInput.readLong();
        }
        finally {
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }

        return magic == DIB.PNG_MAGIC;
    }

    private BufferedImage readPNG(final DirectoryEntry entry, final ImageReadParam param) throws IOException {
        // TODO: Consider delegating listener calls
        return initPNGReader(entry).read(0, param);
    }

    private Iterator<ImageTypeSpecifier> getImageTypesPNG(final DirectoryEntry entry) throws IOException {
        return initPNGReader(entry).getImageTypes(0);
    }

    private ImageReader initPNGReader(final DirectoryEntry entry) throws IOException {
        ImageReader pngReader = getPNGReader();

        imageInput.seek(entry.getOffset());
        ImageInputStream stream = new SubImageInputStream(imageInput, entry.getSize());

        // NOTE: Will throw IOException on later reads if input is not PNG
        pngReader.setInput(stream);

        return pngReader;
    }

    private ImageReader getPNGReader() throws IIOException {
        // TODO: Prefer Sun's std JDK PNGImagerReader, because it has known behaviour?
        if (pngImageReader == null) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("PNG");

            if (readers.hasNext()) {
                pngImageReader = readers.next();
            }
            else {
                throw new IIOException("No PNGImageReader found using ImageIO, can't read PNG encoded ICO format.");
            }
        }
        else {
            pngImageReader.reset();
        }

        return pngImageReader;
    }

    private DIBHeader getHeader(final DirectoryEntry entry) throws IOException {
        if (!headers.containsKey(entry)) {
            imageInput.seek(entry.getOffset());
            DIBHeader header = DIBHeader.read(imageInput);
            headers.put(entry, header);
        }

        return headers.get(entry);
    }

    private BufferedImage readBitmap(final DirectoryEntry entry) throws IOException {
        // TODO: Get rid of the caching, as the images are mutable
        BitmapDescriptor descriptor = descriptors.get(entry);

        if (descriptor == null || !descriptors.containsKey(entry)) {
            DIBHeader header = getHeader(entry);

            int offset = entry.getOffset() + header.getSize();
            if (offset != imageInput.getStreamPosition()) {
                imageInput.seek(offset);
            }

            // TODO: Support this, it's already in the BMP reader, spec allows RLE4 and RLE8
            if (header.getCompression() != DIB.COMPRESSION_RGB) {
                descriptor = new BitmapUnsupported(entry, header, String.format("Unsupported compression: %d", header.getCompression()));
            }
            else {
                int bitCount = header.getBitCount();

                switch (bitCount) {
                    // Palette style
                    case 1:
                    case 4:
                    case 8: // TODO: Gray!
                        descriptor = new BitmapIndexed(entry, header);
                        readBitmapIndexed((BitmapIndexed) descriptor);
                        break;
                    // RGB style
                    case 16:
                        descriptor = new BitmapRGB(entry, header);
                        readBitmap16(descriptor);
                        break;
                    case 24:
                        descriptor = new BitmapRGB(entry, header);
                        readBitmap24(descriptor);
                        break;
                    case 32:
                        descriptor = new BitmapRGB(entry, header);
                        readBitmap32(descriptor);
                        break;

                    default:
                        descriptor = new BitmapUnsupported(entry, header, String.format("Unsupported bit count %d", bitCount));
                }
            }

            descriptors.put(entry, descriptor);
        }

        return descriptor.getImage();
    }

    private void readBitmapIndexed(final BitmapIndexed bitmap) throws IOException {
        readColorMap(bitmap);

        switch (bitmap.getBitCount()) {
            case 1:
                readBitmapIndexed1(bitmap, false);
                break;
            case 4:
                readBitmapIndexed4(bitmap);
                break;
            case 8:
                readBitmapIndexed8(bitmap);
                break;
        }

        BitmapMask mask = new BitmapMask(bitmap.entry, bitmap.header);
        readBitmapIndexed1(mask.bitMask, true);
        bitmap.setMask(mask);
    }

    private void readColorMap(final BitmapIndexed bitmap) throws IOException {
        int colorCount = bitmap.getColorCount();

        for (int i = 0; i < colorCount; i++) {
            // aRGB (a is "Reserved")
            bitmap.colors[i] = (imageInput.readInt() & 0xffffff) | 0xff000000;
        }
    }

    private void readBitmapIndexed1(final BitmapIndexed bitmap, final boolean asMask) throws IOException {
        int width = adjustToPadding((bitmap.getWidth() + 7) >> 3);
        byte[] row = new byte[width];

        for (int y = 0; y < bitmap.getHeight(); y++) {
            imageInput.readFully(row, 0, width);
            int rowPos = 0;
            int xOrVal = 0x80;
            int pos = (bitmap.getHeight() - y - 1) * bitmap.getWidth();

            for (int x = 0; x < bitmap.getWidth(); x++) {
                bitmap.bits[pos++] = ((row[rowPos] & xOrVal) / xOrVal) & 0xFF;

                if (xOrVal == 1) {
                    xOrVal = 0x80;
                    rowPos++;
                }
                else {
                    xOrVal >>= 1;
                }
            }

            // NOTE: If we are reading the mask, we can't abort or report progress
            if (!asMask) {
                if (abortRequested()) {
                    processReadAborted();
                    break;
                }

                processImageProgress(100 * y / (float) bitmap.getHeight());
            }
        }
    }

    private void readBitmapIndexed4(final BitmapIndexed bitmap) throws IOException {
        int width = adjustToPadding((bitmap.getWidth() + 1) >> 1);
        byte[] row = new byte[width];

        for (int y = 0; y < bitmap.getHeight(); y++) {
            imageInput.readFully(row, 0, width);
            int rowPos = 0;
            boolean high4 = true;
            int pos = (bitmap.getHeight() - y - 1) * bitmap.getWidth();

            for (int x = 0; x < bitmap.getWidth(); x++) {
                int value;

                if (high4) {
                    value = (row[rowPos] & 0xF0) >> 4;
                }
                else {
                    value = row[rowPos] & 0x0F;
                    rowPos++;
                }

                bitmap.bits[pos++] = value & 0xFF;
                high4 = !high4;
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) bitmap.getHeight());
        }
    }

    private void readBitmapIndexed8(final BitmapIndexed bitmap) throws IOException {
        int width = adjustToPadding(bitmap.getWidth());

        byte[] row = new byte[width];

        for (int y = 0; y < bitmap.getHeight(); y++) {
            imageInput.readFully(row, 0, width);
            int rowPos = 0;
            int pos = (bitmap.getHeight() - y - 1) * bitmap.getWidth();

            for (int x = 0; x < bitmap.getWidth(); x++) {
                bitmap.bits[pos++] = row[rowPos++] & 0xFF;
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) bitmap.getHeight());
        }
    }

    /**
     * @param width Bytes per scan line (i.e., 1BPP, width = 9 -> bytes = 2)
     * @return padded width
     */
    private static int adjustToPadding(final int width) {
        if ((width & 0x03) != 0) {
             return (width & ~0x03) + 4;
        }

        return width;
    }

    private void readBitmap16(final BitmapDescriptor bitmap) throws IOException {
        short[] pixels = new short[bitmap.getWidth() * bitmap.getHeight()];

        // TODO: Support TYPE_USHORT_565 and the RGB 444/ARGB 4444 layouts
        // Will create TYPE_USHORT_555
        DirectColorModel cm = new DirectColorModel(16, 0x7C00, 0x03E0, 0x001F);
        DataBuffer buffer = new DataBufferUShort(pixels, pixels.length);
        WritableRaster raster = Raster.createPackedRaster(
                buffer, bitmap.getWidth(), bitmap.getHeight(), bitmap.getWidth(), cm.getMasks(), null
        );
        bitmap.image = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        for (int y = 0; y < bitmap.getHeight(); y++) {
            int offset = (bitmap.getHeight() - y - 1) * bitmap.getWidth();
            imageInput.readFully(pixels, offset, bitmap.getWidth());


            // Skip to 32 bit boundary
            if (bitmap.getWidth() % 2 != 0) {
                imageInput.readShort();
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) bitmap.getHeight());
        }

        // TODO: Might be mask!?
    }

    private void readBitmap24(final BitmapDescriptor bitmap) throws IOException {
        byte[] pixels = new byte[bitmap.getWidth() * bitmap.getHeight() * 3];

        // Create TYPE_3BYTE_BGR
        DataBuffer buffer = new DataBufferByte(pixels, pixels.length);
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8};
        int[] bOffs = {2, 1, 0};
        ComponentColorModel cm = new ComponentColorModel(
                cs, nBits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
        );

        int scanlineStride = bitmap.getWidth() * 3;
        // BMP rows are padded to 4 byte boundary
        int rowSizeBytes = ((8 * scanlineStride + 31) / 32) * 4;

        WritableRaster raster = Raster.createInterleavedRaster(
                buffer, bitmap.getWidth(), bitmap.getHeight(), scanlineStride, 3, bOffs, null
        );
        bitmap.image = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        for (int y = 0; y < bitmap.getHeight(); y++) {
            int offset = (bitmap.getHeight() - y - 1) * scanlineStride;
            imageInput.readFully(pixels, offset, scanlineStride);
            imageInput.skipBytes(rowSizeBytes - scanlineStride);

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) bitmap.getHeight());
        }

        // 24 bit icons usually have a bit mask
        if (bitmap.hasMask()) {
            BitmapMask mask = new BitmapMask(bitmap.entry, bitmap.header);
            readBitmapIndexed1(mask.bitMask, true);

            bitmap.setMask(mask);
        }
    }

    private void readBitmap32(final BitmapDescriptor bitmap) throws IOException {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];

        // Will create TYPE_INT_ARGB
        DirectColorModel cm = (DirectColorModel) ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(pixels, pixels.length);
        WritableRaster raster = Raster.createPackedRaster(
                buffer, bitmap.getWidth(), bitmap.getHeight(), bitmap.getWidth(), cm.getMasks(), null
        );
        bitmap.image = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        for (int y = 0; y < bitmap.getHeight(); y++) {
            int offset = (bitmap.getHeight() - y - 1) * bitmap.getWidth();
            imageInput.readFully(pixels, offset, bitmap.getWidth());

            if (abortRequested()) {
                processReadAborted();
                break;
            }
            processImageProgress(100 * y / (float) bitmap.getHeight());
        }

        // There might be a mask here as well, but we'll ignore it,
        // and use the 8 bit alpha channel in the ARGB pixel data
    }

    private Directory getDirectory() throws IOException {
        assertInput();

        if (directory == null) {
            readFileHeader();
        }

        return directory;
    }

    private void readFileHeader() throws IOException {
        imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        // Read file header
        imageInput.readUnsignedShort(); // Reserved

        // Should be same as type as the provider
        int type = imageInput.readUnsignedShort();
        int imageCount = imageInput.readUnsignedShort();

        // Read directory
        directory = Directory.read(type, imageCount, imageInput);
    }

    final DirectoryEntry getEntry(final int imageIndex) throws IOException {
        Directory directory = getDirectory();
        if (imageIndex < 0 || imageIndex >= directory.count()) {
            throw new IndexOutOfBoundsException(String.format("Index: %d, ImageCount: %d", imageIndex, directory.count()));
        }

        return directory.getEntry(imageIndex);
    }

    /// Test code below, ignore.. :-)
    public static void main(final String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Please specify the icon file name");
            System.exit(1);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            // Ignore
        }

        String title = new File(args[0]).getName();
        JFrame frame = createWindow(title);
        JPanel root = new JPanel(new FlowLayout());
        JScrollPane scroll =
                new JScrollPane(root, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        frame.setContentPane(scroll);

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("ico");
        if (!readers.hasNext()) {
            System.err.println("No reader for format 'ico' found");
            System.exit(1);
        }

        ImageReader reader = readers.next();

        for (String arg : args) {
            JPanel panel = new JPanel(null);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            readImagesInFile(arg, reader, panel);
            root.add(panel);
        }

        frame.pack();
        frame.setVisible(true);
    }

    private static void readImagesInFile(String fileName, ImageReader reader, final Container container) throws IOException {
        File file = new File(fileName);
        if (!file.isFile()) {
            System.err.println(fileName + " not found, or is no file");
        }

        reader.setInput(ImageIO.createImageInputStream(file));
        int imageCount = reader.getNumImages(true);
        for (int i = 0; i < imageCount; i++) {
            try {
                addImage(container, reader, i);
            }
            catch (Exception e) {
                System.err.println("FileName: " + fileName);
                System.err.println("Icon: " + i);
                e.printStackTrace();
            }
        }
    }

    private static JFrame createWindow(final String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        return frame;
    }

    private static void addImage(final Container parent, final ImageReader reader, final int imageNo) throws IOException {
        final JButton button = new JButton();

        BufferedImage image = reader.read(imageNo);
        button.setIcon(new ImageIcon(image) {
            TexturePaint texture;

            private void createTexture(final GraphicsConfiguration graphicsConfiguration) {
                BufferedImage pattern = graphicsConfiguration.createCompatibleImage(20, 20);
                Graphics2D g = pattern.createGraphics();
                try {
                    g.setColor(Color.LIGHT_GRAY);
                    g.fillRect(0, 0, pattern.getWidth(), pattern.getHeight());
                    g.setColor(Color.GRAY);
                    g.fillRect(0, 0, pattern.getWidth() / 2, pattern.getHeight() / 2);
                    g.fillRect(pattern.getWidth() / 2, pattern.getHeight() / 2, pattern.getWidth() / 2, pattern.getHeight() / 2);
                }
                finally {
                    g.dispose();
                }

                texture = new TexturePaint(pattern, new Rectangle(pattern.getWidth(), pattern.getHeight()));
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                if (texture == null) {
                    createTexture(c.getGraphicsConfiguration());
                }

                Graphics2D gr = (Graphics2D) g;
                gr.setPaint(texture);
                gr.fillRect(x, y, getIconWidth(), getIconHeight());
                super.paintIcon(c, g, x, y);
            }
        });

        button.setText(image.getWidth() + "x" +
                image.getHeight() + ": "
                + ((image.getColorModel() instanceof IndexColorModel) ?
                String.valueOf(((IndexColorModel) image.getColorModel()).getMapSize()) :
                "TrueColor"));

        parent.add(button);
    }
}
