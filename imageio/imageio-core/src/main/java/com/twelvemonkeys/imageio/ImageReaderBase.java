/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio;

import com.twelvemonkeys.image.BufferedImageIcon;
import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Abstract base class for image readers.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageReaderBase.java,v 1.0 Sep 20, 2007 5:28:37 PM haraldk Exp$
 */
public abstract class ImageReaderBase extends ImageReader {

    private static final Point ORIGIN = new Point(0, 0);

    /**
     * For convenience. Only set if the input is an {@code ImageInputStream}.
     * @see #setInput(Object, boolean, boolean)
     */
    protected ImageInputStream imageInput;

    /**
     * Constructs an {@code ImageReader} and sets its
     * {@code originatingProvider} field to the supplied value.
     * <p/>
     * <p> Subclasses that make use of extensions should provide a
     * constructor with signature {@code (ImageReaderSpi,
     * Object)} in order to retrieve the extension object.  If
     * the extension object is unsuitable, an
     * {@code IllegalArgumentException} should be thrown.
     *
     * @param provider the {@code ImageReaderSpi} that is invoking this constructor, or {@code null}.
     */
    protected ImageReaderBase(final ImageReaderSpi provider) {
        super(provider);
    }

    /**
     * Overrides {@code setInput}, to allow easy access to the input, in case
     * it is an {@code ImageInputStream}.
     *
     * @param input the {@code ImageInputStream} or other
     * {@code Object} to use for future decoding.
     * @param seekForwardOnly if {@code true}, images and metadata
     * may only be read in ascending order from this input source.
     * @param ignoreMetadata if {@code true}, metadata
     * may be ignored during reads.
     *
     * @exception IllegalArgumentException if {@code input} is
     * not an instance of one of the classes returned by the
     * originating service provider's {@code getInputTypes}
     * method, or is not an {@code ImageInputStream}.
     *
     * @see ImageInputStream
     */
    @Override
    public void setInput(final Object input, final boolean seekForwardOnly, final boolean ignoreMetadata) {
        resetMembers();
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        if (input instanceof ImageInputStream) {
            imageInput = (ImageInputStream) input;
        }
        else {
            imageInput = null;
        }
    }

    @Override
    public void dispose() {
        resetMembers();
        super.dispose();
    }

    @Override
    public void reset() {
        resetMembers();
        super.reset();
    }

    /**
     * Resets all member variables. This method is by default invoked from:
     * <ul>
     *  <li>{@link #setInput(Object, boolean, boolean)}</li>
     *  <li>{@link #dispose()}</li>
     *  <li>{@link #reset()}</li>
     * </ul>
     *
     */
    protected abstract void resetMembers();

    /**
     * Default implementation that always returns {@code null}.
     *
     * @param imageIndex ignored, unless overridden
     * @return {@code null}, unless overridden
     * @throws IOException never, unless overridden.
     */
    public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
        return null;
    }

    /**
     * Default implementation that always returns {@code null}.
     *
     * @return {@code null}, unless overridden
     * @throws IOException never, unless overridden.
     */
    public IIOMetadata getStreamMetadata() throws IOException {
        return null;
    }

    /**
     * Default implementation that always returns {@code 1}.
     *
     * @param allowSearch ignored, unless overridden
     * @return {@code 1}, unless overridden
     * @throws IOException never, unless overridden
     */
    public int getNumImages(boolean allowSearch) throws IOException {
        assertInput();
        return 1;
    }

    /**
     * Convenience method to make sure image index is within bounds.
     *
     * @param index the image index
     *
     * @throws java.io.IOException if an error occurs during reading
     * @throws IndexOutOfBoundsException if not {@code minIndex <= index < numImages}
     */
    protected void checkBounds(int index) throws IOException {
        assertInput();
        if (index < getMinIndex()) {
            throw new IndexOutOfBoundsException("index < minIndex");
        }

        int numImages = getNumImages(false);
        if (numImages != -1 && index >= numImages) {
            throw new IndexOutOfBoundsException("index >= numImages (" + index + " >= " + numImages + ")");
        }
    }

    /**
     * Makes sure input is set.
     *
     * @throws IllegalStateException if {@code getInput() == null}.
     */
    protected void assertInput() {
        if (getInput() == null) {
            throw new IllegalStateException("getInput() == null");
        }
    }

    /**
     * Returns the {@code BufferedImage} to which decoded pixel data should be written.
     * <p/>
     * As {@link javax.imageio.ImageReader#getDestination} but tests if the explicit destination
     * image (if set) is valid according to the {@code ImageTypeSpecifier}s given in {@code types}.
     *
     * @param param an {@code ImageReadParam} to be used to get
     * the destination image or image type, or {@code null}.
     * @param types an {@code Iterator} of
     * {@code ImageTypeSpecifier}s indicating the legal image
     * types, with the default first.
     * @param width the true width of the image or tile begin decoded.
     * @param height the true width of the image or tile being decoded.
     *
     * @return the {@code BufferedImage} to which decoded pixel
     * data should be written.
     *
     * @exception javax.imageio.IIOException if the {@code ImageTypeSpecifier} or {@code BufferedImage}
     * specified by {@code param} does not match any of the legal
     * ones from {@code types}.
     * @throws IllegalArgumentException if {@code types}
     * is {@code null} or empty, or if an object not of type
     * {@code ImageTypeSpecifier} is retrieved from it.
     * Or, if the resulting image would have a width or height less than 1,
     * or if the product of {@code width} and {@code height} of the resulting image is greater than
     * {@code Integer.MAX_VALUE}.
     */
    public static BufferedImage getDestination(final ImageReadParam param, final Iterator<ImageTypeSpecifier> types,
                                               final int width, final int height) throws IIOException {
        // Adapted from http://java.net/jira/secure/attachment/29712/TIFFImageReader.java.patch,
        // to allow reading parts/tiles of huge images.

        if (types == null || !types.hasNext()) {
            throw new IllegalArgumentException("imageTypes null or empty!");
        }

        ImageTypeSpecifier imageType = null;

        // If param is non-null, use it
        if (param != null) {
            // Try to get the explicit destination image
            BufferedImage dest = param.getDestination();

            if (dest != null) {
                boolean found = false;

                while (types.hasNext()) {
                    ImageTypeSpecifier specifier = types.next();
                    int bufferedImageType = specifier.getBufferedImageType();

                    if (bufferedImageType != 0 && bufferedImageType == dest.getType()) {
                        // Known types equal, perfect match
                        found = true;
                        break;
                    }
                    else {
                        // If types are different, or TYPE_CUSTOM, test if
                        // - transferType is ok
                        // - bands are ok
                        // TODO: Test if color model is ok?
                        if (specifier.getSampleModel().getTransferType() == dest.getSampleModel().getTransferType() &&
                                specifier.getNumBands() <= dest.getSampleModel().getNumBands()) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    throw new IIOException(String.format("Destination image from ImageReadParam does not match legal imageTypes from reader: %s", dest));
                }

                return dest;
            }

            // No image, get the image type
            imageType = param.getDestinationType();
        }

        // No info from param, use fallback image type
        if (imageType == null) {
            imageType = types.next();
        }
        else {
            boolean foundIt = false;

            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();

                if (type.equals(imageType)) {
                    foundIt = true;
                    break;
                }
            }

            if (!foundIt) {
                throw new IIOException(String.format("Destination type from ImageReadParam does not match legal imageTypes from reader: %s", imageType));
            }
        }

        Rectangle srcRegion = new Rectangle(0, 0, 0, 0);
        Rectangle destRegion = new Rectangle(0, 0, 0, 0);
        computeRegions(param, width, height, null, srcRegion, destRegion);

        int destWidth = destRegion.x + destRegion.width;
        int destHeight = destRegion.y + destRegion.height;

        if ((long) destWidth * destHeight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("destination width * height > Integer.MAX_VALUE: %d", (long) destWidth * destHeight));
        }

        // Create a new image based on the type specifier
        return imageType.createBufferedImage(destWidth, destHeight);
    }

    /**
     * Utility method for getting the area of interest (AOI) of an image.
     * The AOI is defined by the {@link javax.imageio.IIOParam#setSourceRegion(java.awt.Rectangle)}
     * method.
     * <p/>
     * Note: If it is possible for the reader to read the AOI directly, such a
     * method should be used instead, for efficiency.
     *
     * @param pImage the image to get AOI from
     * @param pParam the param optionally specifying the AOI
     *
     * @return a {@code BufferedImage} containing the area of interest (source
     * region), or the original image, if no source region was set, or
     * {@code pParam} was {@code null}
     */
    protected static BufferedImage fakeAOI(BufferedImage pImage, ImageReadParam pParam) {
        return IIOUtil.fakeAOI(pImage, getSourceRegion(pParam, pImage.getWidth(), pImage.getHeight()));
    }

    /**
     * Utility method for getting the subsampled image.
     * The subsampling is defined by the
     * {@link javax.imageio.IIOParam#setSourceSubsampling(int, int, int, int)}
     * method.
     * <p/>
     * NOTE: This method does not take the subsampling offsets into
     * consideration.
     * <p/>
     * Note: If it is possible for the reader to subsample directly, such a
     * method should be used instead, for efficiency.
     *
     * @param pImage the image to subsample
     * @param pParam the param optionally specifying subsampling
     *
     * @return an {@code Image} containing the subsampled image, or the
     * original image, if no subsampling was specified, or
     * {@code pParam} was {@code null}
     */
    protected static Image fakeSubsampling(Image pImage, ImageReadParam pParam) {
        return IIOUtil.fakeSubsampling(pImage, pParam);
    }

    /**
     * Tests if param has explicit destination.
     *
     * @param pParam the image read parameter, or {@code null}
     * @return true if {@code pParam} is non-{@code null} and either its {@code getDestination},
     * {@code getDestinationType} returns a non-{@code null} value,
     * or {@code getDestinationOffset} returns a {@link Point} that is not the upper left corner {@code (0, 0)}.
     */
    protected static boolean hasExplicitDestination(final ImageReadParam pParam) {
        return pParam != null &&
                (
                        pParam.getDestination() != null || pParam.getDestinationType() != null ||
                                !ORIGIN.equals(pParam.getDestinationOffset())
                );
    }

    public static void main(String[] pArgs) throws IOException {
        BufferedImage image = ImageIO.read(new File(pArgs[0]));
        if (image == null) {
            System.err.println("Supported formats: " + Arrays.toString(IIOUtil.getNormalizedReaderFormatNames()));
            System.exit(1);
        }
        showIt(image, pArgs[0]);
    }

    protected static void showIt(final BufferedImage pImage, final String pTitle) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JFrame frame = new JFrame(pTitle);

                    frame.getRootPane().getActionMap().put("window-close", new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            Window window = SwingUtilities.getWindowAncestor((Component) e.getSource());
                            window.setVisible(false);
                            window.dispose();
                        }
                    });
                    frame.getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "window-close");
                    frame.addWindowListener(new ExitIfNoWindowPresentHandler());
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                    frame.setLocationByPlatform(true);
                    JPanel pane = new JPanel(new BorderLayout());
                    JScrollPane scroll = new JScrollPane(pImage != null ? new ImageLabel(pImage) : new JLabel("(no image data)", JLabel.CENTER));
                    scroll.setBorder(null);
                    pane.add(scroll);
                    frame.setContentPane(pane);
                    frame.pack();
                    frame.setVisible(true);
                }
            });
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }

            throw new RuntimeException(e);
        }
    }

    private static class ImageLabel extends JLabel {
        static final String ZOOM_IN = "zoom-in";
        static final String ZOOM_OUT = "zoom-out";
        static final String ZOOM_ACTUAL = "zoom-actual";

        private BufferedImage image;

        Paint backgroundPaint;

        final Paint checkeredBG;
        final Color defaultBG;

        public ImageLabel(final BufferedImage pImage) {
            super(new BufferedImageIcon(pImage));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            image = pImage;
            checkeredBG = createTexture();

            // For indexed color, default to the color of the transparent pixel, if any 
            defaultBG = getDefaultBackground(pImage);

            backgroundPaint = defaultBG != null ? defaultBG : checkeredBG;

            setupActions();
            setComponentPopupMenu(createPopupMenu());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        getComponentPopupMenu().show(ImageLabel.this, e.getX(), e.getY());
                    }
                }
            });

            setTransferHandler(new TransferHandler() {
                @Override
                public int getSourceActions(JComponent c) {
                    return COPY;
                }

                @Override
                protected Transferable createTransferable(JComponent c) {
                    return new ImageTransferable(image);
                }

                @Override
                public boolean importData(JComponent comp, Transferable t) {
                    if (canImport(comp, t.getTransferDataFlavors())) {
                        try {
                            Image transferData = (Image) t.getTransferData(DataFlavor.imageFlavor);
                            image = ImageUtil.toBuffered(transferData);
                            setIcon(new BufferedImageIcon(image));

                            return true;
                        }
                        catch (UnsupportedFlavorException | IOException ignore) {
                        }
                    }

                    return false;
                }

                @Override
                public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                    for (DataFlavor flavor : transferFlavors) {
                        if (flavor.equals(DataFlavor.imageFlavor)) {
                            return true;
                        }
                    }

                    return false;
                }
            });
        }

        private void setupActions() {
            // Mac weirdness... VK_MINUS/VK_PLUS seems to map to english key map always...
            bindAction(new ZoomAction("Zoom in", 2), ZOOM_IN, KeyStroke.getKeyStroke('+'), KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0));
            bindAction(new ZoomAction("Zoom out", .5), ZOOM_OUT, KeyStroke.getKeyStroke('-'), KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0));
            bindAction(new ZoomAction("Zoom actual"), ZOOM_ACTUAL, KeyStroke.getKeyStroke('0'), KeyStroke.getKeyStroke(KeyEvent.VK_0, 0));

            bindAction(TransferHandler.getCopyAction(), (String) TransferHandler.getCopyAction().getValue(Action.NAME), KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            bindAction(TransferHandler.getPasteAction(), (String) TransferHandler.getPasteAction().getValue(Action.NAME), KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }

        private void bindAction(final Action action, final String key, final KeyStroke... keyStrokes) {
            for (KeyStroke keyStroke : keyStrokes) {
                getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, key);
            }

            getActionMap().put(key, action);
        }

        private JPopupMenu createPopupMenu() {
            JPopupMenu popup = new JPopupMenu();

            popup.add(getActionMap().get(ZOOM_ACTUAL));
            popup.add(getActionMap().get(ZOOM_IN));
            popup.add(getActionMap().get(ZOOM_OUT));
            popup.addSeparator();

            ButtonGroup group = new ButtonGroup();

            JMenu background = new JMenu("Background");
            popup.add(background);

            ChangeBackgroundAction checkered = new ChangeBackgroundAction("Checkered", checkeredBG);
            checkered.putValue(Action.SELECTED_KEY, backgroundPaint == checkeredBG);
            addCheckBoxItem(checkered, background, group);
            background.addSeparator();
            addCheckBoxItem(new ChangeBackgroundAction("White", Color.WHITE), background, group);
            addCheckBoxItem(new ChangeBackgroundAction("Light", Color.LIGHT_GRAY), background, group);
            addCheckBoxItem(new ChangeBackgroundAction("Gray", Color.GRAY), background, group);
            addCheckBoxItem(new ChangeBackgroundAction("Dark", Color.DARK_GRAY), background, group);
            addCheckBoxItem(new ChangeBackgroundAction("Black", Color.BLACK), background, group);
            background.addSeparator();
            ChooseBackgroundAction chooseBackgroundAction = new ChooseBackgroundAction("Choose...", defaultBG != null ? defaultBG : Color.BLUE);
            chooseBackgroundAction.putValue(Action.SELECTED_KEY, backgroundPaint == defaultBG);
            addCheckBoxItem(chooseBackgroundAction, background, group);

            return popup;
        }

        private void addCheckBoxItem(final Action pAction, final JMenu pPopup, final ButtonGroup pGroup) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(pAction);
            pGroup.add(item);
            pPopup.add(item);
        }

        private static Color getDefaultBackground(BufferedImage pImage) {
            if (pImage.getColorModel() instanceof IndexColorModel) {
                IndexColorModel cm = (IndexColorModel) pImage.getColorModel();
                int transparent = cm.getTransparentPixel();
                if (transparent >= 0) {
                    return new Color(cm.getRGB(transparent), false);
                }
            }

            return null;
        }

        private static Paint createTexture() {
            GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
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

            return new TexturePaint(pattern, new Rectangle(pattern.getWidth(), pattern.getHeight()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D gr = (Graphics2D) g;
            gr.setPaint(backgroundPaint);
            gr.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }

        private class ChangeBackgroundAction extends AbstractAction {
            protected Paint paint;

            public ChangeBackgroundAction(final String pName, final Paint pPaint) {
                super(pName);
                paint = pPaint;
            }

            public void actionPerformed(ActionEvent e) {
                backgroundPaint = paint;
                repaint();
            }
        }

        private class ChooseBackgroundAction extends ChangeBackgroundAction {
            public ChooseBackgroundAction(final String pName, final Color pColor) {
                super(pName, pColor);
                putValue(Action.SMALL_ICON, new Icon() {
                    public void paintIcon(Component c, Graphics pGraphics, int x, int y) {
                        Graphics g = pGraphics.create();
                        g.setColor((Color) paint);
                        g.fillRect(x, y, 16, 16);
                        g.dispose();
                    }

                    public int getIconWidth() {
                        return 16;
                    }

                    public int getIconHeight() {
                        return 16;
                    }
                });
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                Color selected = JColorChooser.showDialog(ImageLabel.this, "Choose background", (Color) paint);
                if (selected != null) {
                    paint = selected;
                    super.actionPerformed(e);
                }
            }
        }

        private class ZoomAction extends AbstractAction {
            private final double zoomFactor;

            public ZoomAction(final String name, final double zoomFactor) {
                super(name);
                this.zoomFactor = zoomFactor;
            }

            public ZoomAction(final String name) {
                this(name, 0);
            }

            public void actionPerformed(final ActionEvent e) {
                if (zoomFactor <= 0) {
                    setIcon(new BufferedImageIcon(image));
                }
                else {
                    Icon current = getIcon();
                    int w = (int) Math.max(Math.min(current.getIconWidth() * zoomFactor, image.getWidth() * 16), image.getWidth() / 16);
                    int h = (int) Math.max(Math.min(current.getIconHeight() * zoomFactor, image.getHeight() * 16), image.getHeight() / 16);

                    setIcon(new BufferedImageIcon(image, Math.max(w, 2), Math.max(h, 2), w > image.getWidth() || h > image.getHeight()));
                }
            }
        }

        private static class ImageTransferable implements Transferable {
            private final BufferedImage image;

            public ImageTransferable(final BufferedImage image) {
                this.image = image;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(final DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                if (isDataFlavorSupported(flavor)) {
                    return image;
                }

                throw new UnsupportedFlavorException(flavor);
            }
        }
    }

    private static class ExitIfNoWindowPresentHandler extends WindowAdapter {
        @Override
        public void windowClosed(final WindowEvent e) {
            Window[] windows = Window.getWindows();

            if (windows == null || windows.length == 0) {
                System.exit(0);
            }
        }
    }
}
