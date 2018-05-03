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

package com.twelvemonkeys.imageio.plugins.thumbsdb;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.io.ole2.CompoundDocument;
import com.twelvemonkeys.io.ole2.Entry;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * ThumbsDBImageReader
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku$
 * @version $Id: ThumbsDBImageReader.java,v 1.0 22.jan.2007 18:49:38 haku Exp$
 * @see com.twelvemonkeys.io.ole2.CompoundDocument
 * @see <a href="http://en.wikipedia.org/wiki/Thumbs.db>Wikipedia: Thumbs.db</a>
 */
public final class ThumbsDBImageReader extends ImageReaderBase {
    private static final int THUMBNAIL_OFFSET = 12;
    private Entry root;
    private Catalog catalog;

    private BufferedImage[] thumbnails;
    private final ImageReader reader;
    private int currentImage = -1;

    private boolean loadEagerly;

    public ThumbsDBImageReader() {
        this(new ThumbsDBImageReaderSpi());
    }

    protected ThumbsDBImageReader(final ThumbsDBImageReaderSpi pProvider) {
        super(pProvider);
        reader = createJPEGReader(pProvider);
        initReaderListeners();
    }

    protected void resetMembers() {
        root = null;
        catalog = null;
        thumbnails = null;
    }

    private static ImageReader createJPEGReader(final ThumbsDBImageReaderSpi pProvider) {
        return pProvider.createJPEGReader();
    }

    public void dispose() {
        reader.dispose();
        super.dispose();
    }

    public boolean isLoadEagerly() {
        return loadEagerly;
    }

    /**
     * Instructs the reader wether it should read and cache alle thumbnails
     * in sequence, during the first read operation.
     * <p/>
     * This is useful mainly if you need to read all the thumbnails, and you
     * need them in random order, as it requires less repositioning in the
     * underlying stream.
     *
     * @param pLoadEagerly {@code true} if the reader should read all thumbs on first read
     */
    public void setLoadEagerly(final boolean pLoadEagerly) {
        loadEagerly = pLoadEagerly;
    }

    /**
     * Reads the image data from the given input stream, and returns it as a
     * {@code BufferedImage}.
     *
     * @param pIndex the index of the image to read
     * @param pParam additional parameters used while decoding, may be
     *               {@code null}, in which case defaults will be used
     * @return a {@code BufferedImage}
     * @throws IndexOutOfBoundsException if {@code pIndex} is out of bounds
     * @throws IllegalStateException if the input source has not been set
     * @throws java.io.IOException           if an error occurs during reading
     */
    @Override
    public BufferedImage read(final int pIndex, final ImageReadParam pParam) throws IOException {
        init();
        checkBounds(pIndex);

        // Quick look-up
        BufferedImage image = null;
        if (pIndex < thumbnails.length) {
            image = thumbnails[pIndex];
        }

        if (image == null) {
            // Read the image, it's a JFIF stream, inside the OLE 2 CompoundDocument
            init(pIndex);

            image = reader.read(0, pParam);
            reader.reset();

            if (pParam == null) {
                thumbnails[pIndex] = image; // TODO: Caching is not kosher, as images are mutable!!
            }
        }
        else {
            // Keep progress listeners happy
            processImageStarted(pIndex);
            processImageProgress(100);
            processImageComplete();
        }

        // Fake destination support
        if (pParam != null && (pParam.getDestination() != null && pParam.getDestination() != image ||
                pParam.getDestinationType() != null && pParam.getDestinationType().getBufferedImageType() != image.getType())) {
            BufferedImage destination = getDestination(pParam, getImageTypes(pIndex), getWidth(pIndex), getHeight(pIndex));

            Graphics2D g = destination.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.drawImage(image, 0, 0, null);
            }
            finally {
                g.dispose();
            }

            image = destination;
        }

        return image;
    }

    /**
     * Reads the image data from the given input stream, and returns it as a
     * {@code BufferedImage}.
     *
     * @param pName  the name of the image to read
     * @param pParam additional parameters used while decoding, may be
     *               {@code null}, in which case defaults will be used
     * @return a {@code BufferedImage}
     * @throws java.io.FileNotFoundException if the given file name is not found in the
     *                               "Catalog" entry of the {@code CompoundDocument}
     * @throws IllegalStateException if the input source has not been set
     * @throws java.io.IOException           if an error occurs during reading
     */
    public BufferedImage read(final String pName, final ImageReadParam pParam) throws IOException {
        initCatalog();

        int index = catalog.getIndex(pName);
        if (index < 0) {
            throw new FileNotFoundException("Name not found in \"Catalog\" entry: " + pName);
        }

        return read(index, pParam);
    }

    public void abort() {
        super.abort();
        reader.abort();
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        if (imageInput != null) {
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
    }

    private void init(final int pIndex) throws IOException {
        if (currentImage == -1 || pIndex != currentImage || reader.getInput() == null) {
            init();
            checkBounds(pIndex);
            currentImage = pIndex;

            initReader(pIndex);
        }
    }

    private void initReader(final int pIndex) throws IOException {
        init();
        String name = catalog.getStreamName(pIndex);
        Entry entry = root.getChildEntry(name);
        // TODO: It might be possible to speed this up, with less wrapping...
        // Use in-memory input stream for max speed, images are small
        ImageInputStream input = new MemoryCacheImageInputStream(entry.getInputStream());
        input.skipBytes(THUMBNAIL_OFFSET);
        reader.setInput(input);
    }

    private void initReaderListeners() {
        reader.addIIOReadProgressListener(new ProgressListenerBase() {
            @Override
            public void imageComplete(ImageReader pSource) {
                processImageComplete();
            }

            @Override
            public void imageStarted(ImageReader pSource, int pImageIndex) {
                processImageStarted(currentImage);
            }

            @Override
            public void imageProgress(ImageReader pSource, float pPercentageDone) {
                processImageProgress(pPercentageDone);
            }

            @Override
            public void readAborted(ImageReader pSource) {
                processReadAborted();
            }
        });
        // TODO: Update listeners
        // TODO: Warning listeners
    }

    private void init() throws IOException {
        assertInput();
        if (root == null) {
            root = new CompoundDocument(imageInput).getRootEntry();
            SortedSet children = root.getChildEntries();

            thumbnails = new BufferedImage[children.size() - 1];

            initCatalog();

            // NOTE: This is usually slower, unless you need all images
            // TODO: Use as many threads as there are CPU cores? :-)
            if (loadEagerly) {
                for (int i = 0; i < thumbnails.length; i++) {
                    initReader(i);
                    ImageReader reader = this.reader;
                    // TODO: If stream was detached, we could probably create a
                    // new reader, then fire this off in a separate thread...
                    thumbnails[i] = reader.read(0, null);
                }
            }
        }
    }

    private void initCatalog() throws IOException {
        if (catalog == null) {
            Entry catalog = root.getChildEntry("Catalog");

            if (catalog.length() <= 16L) {
                // TODO: Throw exception? Return empty catalog?
            }

            this.catalog = Catalog.read(catalog.getInputStream());
        }
    }

    public int getNumImages(boolean allowSearch) throws IOException {
        if (allowSearch) {
            init();
      }

        return catalog != null ? catalog.getThumbnailCount() : super.getNumImages(false);
    }

    public int getWidth(int pIndex) throws IOException {
        init(pIndex);

        BufferedImage image = thumbnails[pIndex];
        return image != null ? image.getWidth() : reader.getWidth(0);
    }

    public int getHeight(int pIndex) throws IOException {
        init(pIndex);

        BufferedImage image = thumbnails[pIndex];
        return image != null ? image.getHeight() : reader.getHeight(0);
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(int pIndex) throws IOException {
        init(pIndex);
        initReader(pIndex);
        return reader.getImageTypes(0);
    }

    public boolean isPresent(final String pFileName) {
        try {
            init();
        }
        catch (IOException e) {
            resetMembers();
            return false;
        }

        // TODO: Rethink this...
        // Seems to be up to Windows and the installed programs what formats
        // are supported...
        // Some thumbs are just icons, and it might be better to use ImageIO to create thumbs for these... :-/ 
        // At least this seems fine for now
        String extension = FileUtil.getExtension(pFileName);
        if (StringUtil.isEmpty(extension)) {
            return false;
        }
        extension = extension.toLowerCase();

        return !"psd".equals(extension) && !"svg".equals(extension) && catalog != null && catalog.getIndex(pFileName) != -1;
    }

    /// Test code below 

    public static void main(String[] pArgs) throws IOException {
        ThumbsDBImageReader reader = new ThumbsDBImageReader();
        reader.setInput(ImageIO.createImageInputStream(new File(pArgs[0])));
//        reader.setLoadEagerly(true);

        if (pArgs.length > 1) {
            long start = System.currentTimeMillis();
            reader.init();
            for (Catalog.CatalogItem item : reader.catalog) {
                reader.read(item.getName(), null);
            }
            long end = System.currentTimeMillis();
            System.out.println("Time: " + (end - start) + " ms");
        }
        else {
            JFrame frame = createWindow(pArgs[0]);
            JPanel panel = new JPanel();
            panel.setBackground(Color.WHITE);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            long start = System.currentTimeMillis();
            reader.init();
            for (Catalog.CatalogItem item : reader.catalog) {
                addImage(panel, reader, reader.catalog.getIndex(item.getName()), item.getName());
            }
            long end = System.currentTimeMillis();
            System.out.println("Time: " + (end - start) + " ms");

            frame.getContentPane().add(new JScrollPane(panel));
            frame.pack();
            frame.setVisible(true);
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

    private static void addImage(Container pParent, ImageReader pReader, int pImageNo, String pName) throws IOException {
        final JLabel label = new JLabel();
        final BufferedImage image = pReader.read(pImageNo);
        label.setIcon(new Icon() {
            private static final int SIZE = 110;

            public void paintIcon(Component c, Graphics g, int x, int y) {
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.DARK_GRAY);
                g.fillRoundRect(x, y, SIZE, SIZE, 10, 10);
                g.drawImage(image, (SIZE - image.getWidth()) / 2 +  x,  (SIZE - image.getHeight()) / 2 + y, null);
            }

            public int getIconWidth() {
                return SIZE;
            }

            public int getIconHeight() {
                return SIZE;
            }
        });
        label.setText("" + image.getWidth() + "x" + image.getHeight() + ": " + pName);
        label.setToolTipText(image.toString());
        pParent.add(label);
    }

}
