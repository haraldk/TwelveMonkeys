/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.util.WeakWeakMap;

import javax.imageio.*;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;

/**
 * ImageReader for Microsoft Windows ICO (icon) format.
 * 1, 4, 8 bit palette support with bitmask transparency, and 16, 24 and 32 bit
 * true color support with alpha. Also supports Windows Vista PNG encoded icons.
 * <p/>
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
    private Map<DirectoryEntry, DIBHeader> headers = new WeakHashMap<>();
    private Map<DirectoryEntry, BitmapDescriptor> descriptors = new WeakWeakMap<>();

    private ImageReader pngImageReader;

    protected DIBImageReader(final ImageReaderSpi pProvider) {
        super(pProvider);
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

    public Iterator<ImageTypeSpecifier> getImageTypes(final int pImageIndex) throws IOException {
        DirectoryEntry entry = getEntry(pImageIndex);

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

    public int getWidth(final int pImageIndex) throws IOException {
        return getEntry(pImageIndex).getWidth();
    }

    public int getHeight(final int pImageIndex) throws IOException {
        return getEntry(pImageIndex).getHeight();
    }

    public BufferedImage read(final int pImageIndex, final ImageReadParam pParam) throws IOException {
        checkBounds(pImageIndex);

        processImageStarted(pImageIndex);

        DirectoryEntry entry = getEntry(pImageIndex);

        BufferedImage destination;

        if (isPNG(entry)) {
            // NOTE: Special case for Windows Vista, 256x256 PNG encoded images, with no DIB header...
            destination = readPNG(entry, pParam);
        }
        else {
            // NOTE: If param does not have explicit destination, we'll try to create a BufferedImage later,
            //       to allow for storing the cursor hotspot for CUR images
            destination = hasExplicitDestination(pParam) ?
                    getDestination(pParam, getImageTypes(pImageIndex), getWidth(pImageIndex), getHeight(pImageIndex)) : null;

            BufferedImage image = readBitmap(entry);

            // TODO: Handle AOI and subsampling inline, probably not of big importance...
            if (pParam != null) {
                image = fakeAOI(image, pParam);
                image = ImageUtil.toBuffered(fakeSubsampling(image, pParam));
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

    private boolean isPNG(final DirectoryEntry pEntry) throws IOException {
        long magic;

        imageInput.seek(pEntry.getOffset());
        imageInput.setByteOrder(ByteOrder.BIG_ENDIAN);

        try {
            magic = imageInput.readLong();
        }
        finally {
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }

        return magic == DIB.PNG_MAGIC;
    }

    private BufferedImage readPNG(final DirectoryEntry pEntry, final ImageReadParam pParam) throws IOException {
        // TODO: Consider delegating listener calls
        return initPNGReader(pEntry).read(0, pParam);
    }

    private Iterator<ImageTypeSpecifier> getImageTypesPNG(final DirectoryEntry pEntry) throws IOException {
        return initPNGReader(pEntry).getImageTypes(0);
    }

    private ImageReader initPNGReader(final DirectoryEntry pEntry) throws IOException {
        ImageReader pngReader = getPNGReader();

        imageInput.seek(pEntry.getOffset());
        InputStream inputStream = IIOUtil.createStreamAdapter(imageInput, pEntry.getSize());
        ImageInputStream stream = ImageIO.createImageInputStream(inputStream);

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

    private DIBHeader getHeader(final DirectoryEntry pEntry) throws IOException {
        if (!headers.containsKey(pEntry)) {
            imageInput.seek(pEntry.getOffset());
            DIBHeader header = DIBHeader.read(imageInput);
            headers.put(pEntry, header);
        }

        return headers.get(pEntry);
    }

    private BufferedImage readBitmap(final DirectoryEntry pEntry) throws IOException {
        // TODO: Get rid of the caching, as the images are mutable
        BitmapDescriptor descriptor = descriptors.get(pEntry);

        if (descriptor == null || !descriptors.containsKey(pEntry)) {
            DIBHeader header = getHeader(pEntry);

            int offset = pEntry.getOffset() + header.getSize();
            if (offset != imageInput.getStreamPosition()) {
                imageInput.seek(offset);
            }

            // TODO: Support this, it's already in the BMP reader, spec allows RLE4 and RLE8
            if (header.getCompression() != 0) {
                descriptor = new BitmapUnsupported(pEntry, String.format("Unsupported compression: %d", header.getCompression()));
            }
            else {
                int bitCount = header.getBitCount();

                switch (bitCount) {
                    // Palette style
                    case 1:
                    case 4:
                    case 8: // TODO: Gray!
                        descriptor = new BitmapIndexed(pEntry, header);
                        readBitmapIndexed((BitmapIndexed) descriptor);
                        break;
                    // RGB style
                    case 16:
                        descriptor = new BitmapRGB(pEntry, header);
                        readBitmap16(descriptor);
                        break;
                    case 24:
                        descriptor = new BitmapRGB(pEntry, header);
                        readBitmap24(descriptor);
                        break;
                    case 32:
                        descriptor = new BitmapRGB(pEntry, header);
                        readBitmap32(descriptor);
                        break;

                    default:
                        descriptor = new BitmapUnsupported(pEntry, String.format("Unsupported bit count %d", bitCount));
                }
            }

            descriptors.put(pEntry, descriptor);
        }

        return descriptor.getImage();
    }

    private void readBitmapIndexed(final BitmapIndexed pBitmap) throws IOException {
        readColorMap(pBitmap);

        switch (pBitmap.getBitCount()) {
            case 1:
                readBitmapIndexed1(pBitmap, false);
                break;
            case 4:
                readBitmapIndexed4(pBitmap);
                break;
            case 8:
                readBitmapIndexed8(pBitmap);
                break;
        }

        BitmapMask mask = new BitmapMask(pBitmap.entry, pBitmap.header);
        readBitmapIndexed1(mask.bitMask, true);
        pBitmap.setMask(mask);
    }

    private void readColorMap(final BitmapIndexed pBitmap) throws IOException {
        int colorCount = pBitmap.getColorCount();

        for (int i = 0; i < colorCount; i++) {
            // aRGB (a is "Reserved")
            pBitmap.colors[i] = (imageInput.readInt() & 0xffffff) | 0xff000000;
        }
    }

    private void readBitmapIndexed1(final BitmapIndexed pBitmap, final boolean pAsMask) throws IOException {
        int width = adjustToPadding(pBitmap.getWidth() >> 3);
        byte[] row = new byte[width];

        for (int y = 0; y < pBitmap.getHeight(); y++) {
            imageInput.readFully(row, 0, width);
            int rowPos = 0;
            int xOrVal = 0x80;
            int pos = (pBitmap.getHeight() - y - 1) * pBitmap.getWidth();

            for (int x = 0; x < pBitmap.getWidth(); x++) {
                pBitmap.bits[pos++] = ((row[rowPos] & xOrVal) / xOrVal) & 0xFF;

                if (xOrVal == 1) {
                    xOrVal = 0x80;
                    rowPos++;
                }
                else {
                    xOrVal >>= 1;
                }
            }

            // NOTE: If we are reading the mask, we don't abort or report progress
            if (!pAsMask) {
                if (abortRequested()) {
                    processReadAborted();
                    break;
                }

                processImageProgress(100 * y / (float) pBitmap.getHeight());
            }
        }
    }

    private void readBitmapIndexed4(final BitmapIndexed pBitmap) throws IOException {
        int width = adjustToPadding(pBitmap.getWidth() >> 1);
        byte[] row = new byte[width];

        for (int y = 0; y < pBitmap.getHeight(); y++) {
            imageInput.readFully(row, 0, width);
            int rowPos = 0;
            boolean high4 = true;
            int pos = (pBitmap.getHeight() - y - 1) * pBitmap.getWidth();

            for (int x = 0; x < pBitmap.getWidth(); x++) {
                int value;

                if (high4) {
                    value = (row[rowPos] & 0xF0) >> 4;
                }
                else {
                    value = row[rowPos] & 0x0F;
                    rowPos++;
                }

                pBitmap.bits[pos++] = value & 0xFF;
                high4 = !high4;
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) pBitmap.getHeight());
        }
    }

    private void readBitmapIndexed8(final BitmapIndexed pBitmap) throws IOException {
        int width = adjustToPadding(pBitmap.getWidth());

        byte[] row = new byte[width];

        for (int y = 0; y < pBitmap.getHeight(); y++) {
            imageInput.readFully(row, 0, width);
            int rowPos = 0;
            int pos = (pBitmap.getHeight() - y - 1) * pBitmap.getWidth();

            for (int x = 0; x < pBitmap.getWidth(); x++) {
                pBitmap.bits[pos++] = row[rowPos++] & 0xFF;
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) pBitmap.getHeight());
        }
    }

    /**
     * @param pWidth Bytes per scan line (i.e., 1BPP, width = 9 -> bytes = 1)
     * @return padded width
     */
    private static int adjustToPadding(final int pWidth) {
        if ((pWidth & 0x03) != 0) {
             return (pWidth & ~0x03) + 4;
        }
        return pWidth;
    }

    private void readBitmap16(final BitmapDescriptor pBitmap) throws IOException {
        // TODO: No idea if this actually works..
        short[] pixels = new short[pBitmap.getWidth() * pBitmap.getHeight()];

        // TODO: Support TYPE_USHORT_565 and the RGB 444/ARGB 4444 layouts
        // Will create TYPE_USHORT_555
        DirectColorModel cm = new DirectColorModel(16, 0x7C00, 0x03E0, 0x001F);
        DataBuffer buffer = new DataBufferShort(pixels, pixels.length);
        WritableRaster raster = Raster.createPackedRaster(
                buffer, pBitmap.getWidth(), pBitmap.getHeight(), pBitmap.getWidth(), cm.getMasks(), null
        );
        pBitmap.image = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        for (int y = 0; y < pBitmap.getHeight(); y++) {
            int offset = (pBitmap.getHeight() - y - 1) * pBitmap.getWidth();
            imageInput.readFully(pixels, offset, pBitmap.getWidth());


            // Skip to 32 bit boundary
            if (pBitmap.getWidth() % 2 != 0) {
                imageInput.readShort();
            }

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) pBitmap.getHeight());
        }

        // TODO: Might be mask!?
    }

    private void readBitmap24(final BitmapDescriptor pBitmap) throws IOException {
        byte[] pixels = new byte[pBitmap.getWidth() * pBitmap.getHeight() * 3];

        // Create TYPE_3BYTE_BGR
        DataBuffer buffer = new DataBufferByte(pixels, pixels.length);
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8};
        int[] bOffs = {2, 1, 0};
        ComponentColorModel cm = new ComponentColorModel(
                cs, nBits, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
        );

        int scanlineStride = pBitmap.getWidth() * 3;
        // BMP rows are padded to 4 byte boundary
        int rowSizeBytes = ((8 * scanlineStride + 31) / 32) * 4;

        WritableRaster raster = Raster.createInterleavedRaster(
                buffer, pBitmap.getWidth(), pBitmap.getHeight(), scanlineStride, 3, bOffs, null
        );
        pBitmap.image = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        for (int y = 0; y < pBitmap.getHeight(); y++) {
            int offset = (pBitmap.getHeight() - y - 1) * scanlineStride;
            imageInput.readFully(pixels, offset, scanlineStride);
            imageInput.skipBytes(rowSizeBytes - scanlineStride);

            if (abortRequested()) {
                processReadAborted();
                break;
            }

            processImageProgress(100 * y / (float) pBitmap.getHeight());
        }

        // 24 bit icons usually have a bit mask
        if (pBitmap.hasMask()) {
            BitmapMask mask = new BitmapMask(pBitmap.entry, pBitmap.header);
            readBitmapIndexed1(mask.bitMask, true);

            pBitmap.setMask(mask);
        }
    }

    private void readBitmap32(final BitmapDescriptor pBitmap) throws IOException {
        int[] pixels = new int[pBitmap.getWidth() * pBitmap.getHeight()];

        // Will create TYPE_INT_ARGB
        DirectColorModel cm = (DirectColorModel) ColorModel.getRGBdefault();
        DataBuffer buffer = new DataBufferInt(pixels, pixels.length);
        WritableRaster raster = Raster.createPackedRaster(
                buffer, pBitmap.getWidth(), pBitmap.getHeight(), pBitmap.getWidth(), cm.getMasks(), null
        );
        pBitmap.image = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);

        for (int y = 0; y < pBitmap.getHeight(); y++) {
            int offset = (pBitmap.getHeight() - y - 1) * pBitmap.getWidth();
            imageInput.readFully(pixels, offset, pBitmap.getWidth());

            if (abortRequested()) {
                processReadAborted();
                break;
            }            
            processImageProgress(100 * y / (float) pBitmap.getHeight());
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

    final DirectoryEntry getEntry(final int pImageIndex) throws IOException {
        Directory directory = getDirectory();
        if (pImageIndex < 0 || pImageIndex >= directory.count()) {
            throw new IndexOutOfBoundsException(String.format("Index: %d, ImageCount: %d", pImageIndex, directory.count()));
        }

        return directory.getEntry(pImageIndex);
    }
    
    /// Test code below, ignore.. :-)
    public static void main(final String[] pArgs) throws IOException {
        if (pArgs.length == 0) {
            System.err.println("Please specify the icon file name");
            System.exit(1);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            // Ignore
        }

        String title = new File(pArgs[0]).getName();
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

        for (String arg : pArgs) {
            JPanel panel = new JPanel(null);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            readImagesInFile(arg, reader, panel);
            root.add(panel);
        }

        frame.pack();
        frame.setVisible(true);
    }

    private static void readImagesInFile(String pFileName, ImageReader pReader, final Container pContainer) throws IOException {
        File file = new File(pFileName);
        if (!file.isFile()) {
            System.err.println(pFileName + " not found, or is no file");
        }

        pReader.setInput(ImageIO.createImageInputStream(file));
        int imageCount = pReader.getNumImages(true);
        for (int i = 0; i < imageCount; i++) {
            try {
                addImage(pContainer, pReader, i);
            }
            catch (Exception e) {
                System.err.println("FileName: " + pFileName);
                System.err.println("Icon: " + i);
                e.printStackTrace();
            }
        }
    }

    private static JFrame createWindow(final String pTitle) {
        JFrame frame = new JFrame(pTitle);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        return frame;
    }

    private static void addImage(final Container pParent, final ImageReader pReader, final int pImageNo) throws IOException {
        final JButton button = new JButton();

        BufferedImage image = pReader.read(pImageNo);
        button.setIcon(new ImageIcon(image) {
            TexturePaint texture;

            private void createTexture(final GraphicsConfiguration pGraphicsConfiguration) {
                BufferedImage pattern = pGraphicsConfiguration.createCompatibleImage(20, 20);
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

        button.setText("" + image.getWidth() + "x" +
                image.getHeight() + ": "
                + ((image.getColorModel() instanceof IndexColorModel) ?
                "" + ((IndexColorModel) image.getColorModel()).getMapSize() :
                "TrueColor"));

        pParent.add(button);
    }
}
