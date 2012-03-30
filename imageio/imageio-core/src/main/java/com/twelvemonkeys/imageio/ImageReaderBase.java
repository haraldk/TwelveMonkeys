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

package com.twelvemonkeys.imageio;

import com.twelvemonkeys.image.BufferedImageIcon;
import com.twelvemonkeys.imageio.util.IIOUtil;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
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
        else if (getNumImages(false) != -1 && index >= getNumImages(false)) {
            throw new IndexOutOfBoundsException("index >= numImages (" + index + " >= " + getNumImages(false) + ")");
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
     * Returns the {@code BufferedImage} to which decoded pixel
     * data should be written.
     * <p/>
     * As {@link javax.imageio.ImageReader#getDestination} but tests if the explicit destination
     * image (if set) is valid according to the {@code ImageTypeSpecifier}s given in {@code pTypes}
     *
     *
     * @param pParam an {@code ImageReadParam} to be used to get
     * the destination image or image type, or {@code null}.
     * @param pTypes an {@code Iterator} of
     * {@code ImageTypeSpecifier}s indicating the legal image
     * types, with the default first.
     * @param pWidth the true width of the image or tile begin decoded.
     * @param pHeight the true width of the image or tile being decoded.
     *
     * @return the {@code BufferedImage} to which decoded pixel
     * data should be written.
     *
     * @exception IIOException if the {@code ImageTypeSpecifier} or {@code BufferedImage}
     * specified by {@code pParam} does not match any of the legal
     * ones from {@code pTypes}.
     * @throws IllegalArgumentException if {@code pTypes}
     * is {@code null} or empty, or if an object not of type
     * {@code ImageTypeSpecifier} is retrieved from it.
     * Or, if the resulting image would have a width or height less than 1,
     * or if the product of {@code pWidth} and {@code pHeight} is greater than
     * {@code Integer.MAX_VALUE}.
     */
    public static BufferedImage getDestination(final ImageReadParam pParam, final Iterator<ImageTypeSpecifier> pTypes,
                                               final int pWidth, final int pHeight) throws IIOException {
        BufferedImage image = ImageReader.getDestination(pParam, pTypes, pWidth, pHeight);

        if (pParam != null) {
            BufferedImage dest = pParam.getDestination();
            if (dest != null) {
                boolean found = false;

                // NOTE: This is bad, as it relies on implementation details of "super" method...
                // We know that the iterator has not been touched if explicit destination..
                while (pTypes.hasNext()) {
                    ImageTypeSpecifier specifier = pTypes.next();
                    int imageType = specifier.getBufferedImageType();

                    if (imageType != 0 && imageType == dest.getType()) {
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
                    throw new IIOException(String.format("Illegal explicit destination image %s", dest));
                }
            }
        }

        return image;
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
     * {@code getDestinationType} or {@code getDestinationOffset} returns a non-{@code null} value.
     */
    protected static boolean hasExplicitDestination(final ImageReadParam pParam) {
        return (pParam != null && (pParam.getDestination() != null || pParam.getDestinationType() != null || pParam.getDestinationOffset() != null));
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
        Paint backgroundPaint;

        final Paint checkeredBG;
        final Color defaultBG;

        public ImageLabel(final BufferedImage pImage) {
            super(new BufferedImageIcon(pImage));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            checkeredBG = createTexture();

            // For indexed color, default to the color of the transparent pixel, if any 
            defaultBG = getDefaultBackground(pImage);

            backgroundPaint = defaultBG != null ? defaultBG : checkeredBG;

            JPopupMenu popup = createBackgroundPopup();

            setComponentPopupMenu(popup);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        getComponentPopupMenu().show(ImageLabel.this, e.getX(), e.getY());
                    }
                }
            });
        }

        private JPopupMenu createBackgroundPopup() {
            JPopupMenu popup = new JPopupMenu();
            ButtonGroup group = new ButtonGroup();

            addCheckBoxItem(new ChangeBackgroundAction("Checkered", checkeredBG), popup, group);
            popup.addSeparator();
            addCheckBoxItem(new ChangeBackgroundAction("White", Color.WHITE), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Light", Color.LIGHT_GRAY), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Gray", Color.GRAY), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Dark", Color.DARK_GRAY), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Black", Color.BLACK), popup, group);
            popup.addSeparator();
            addCheckBoxItem(new ChooseBackgroundAction("Choose...", defaultBG != null ? defaultBG : Color.BLUE), popup, group);

            return popup;
        }

        private void addCheckBoxItem(final Action pAction, final JPopupMenu pPopup, final ButtonGroup pGroup) {
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
