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

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * ImageReaderBase
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
    protected ImageInputStream mImageInput;

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
     * @param pOriginatingProvider the {@code ImageReaderSpi} that is
     *                            invoking this constructor, or {@code null}.
     */
    protected ImageReaderBase(final ImageReaderSpi pOriginatingProvider) {
        super(pOriginatingProvider);
    }

    /**
     * Overrides {@code setInput}, to allow easy access to the input, in case
     * it is an {@code ImageInputStream}.
     *
     * @param pInput the {@code ImageInputStream} or other
     * {@code Object} to use for future decoding.
     * @param pSeekForwardOnly if {@code true}, images and metadata
     * may only be read in ascending order from this input source.
     * @param pIgnoreMetadata if {@code true}, metadata
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
    public void setInput(Object pInput, boolean pSeekForwardOnly, boolean pIgnoreMetadata) {
        resetMembers();
        super.setInput(pInput, pSeekForwardOnly, pIgnoreMetadata);
        if (pInput instanceof ImageInputStream) {
            mImageInput = (ImageInputStream) pInput;
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
     * Defaul implementation that always return {@code null}.
     *
     * @param pImageIndex ignored, unless overriden
     * @return {@code null}, unless overriden
     * @throws IOException never, unless overriden.
     */
    public IIOMetadata getImageMetadata(int pImageIndex) throws IOException {
        return null;
    }

    /**
     * Defaul implementation that always return {@code null}.
     *
     * @return {@code null}, unless overriden
     * @throws IOException never, unless overriden.
     */
    public IIOMetadata getStreamMetadata() throws IOException {
        return null;
    }

    /**
     * Default implementation that always returns {@code 1}.
     *
     * @param pAllowSearch ignored, unless overriden
     * @return {@code 1}, unless overriden
     * @throws IOException never, unless overriden
     */
    public int getNumImages(boolean pAllowSearch) throws IOException {
        assertInput();
        return 1;
    }

    /**
     * Convenience method to make sure image index is within bounds.
     *
     * @param pIndex the image index
     *
     * @throws java.io.IOException if an error occurs during reading
     * @throws IndexOutOfBoundsException if not
     * <tt>minIndex <= pIndex < numImages</tt>
     */
    protected void checkBounds(int pIndex) throws IOException {
        assertInput();
        if (pIndex < getMinIndex()) {
            throw new IndexOutOfBoundsException("index < minIndex");
        }
        else if (getNumImages(false) != -1 && pIndex >= getNumImages(false)) {
            throw new IndexOutOfBoundsException("index >= numImages (" + pIndex + " >= " + getNumImages(false) + ")");
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

    public static void main(String[] pArgs) throws IOException {
        BufferedImage image = ImageIO.read(new File(pArgs[0]));
        showIt(image, pArgs[0]);
    }

    protected static void showIt(final BufferedImage pImage, final String pTitle) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JFrame frame = new JFrame(pTitle);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setLocationByPlatform(true);
                    JPanel pane = new JPanel(new BorderLayout());
                    JScrollPane scroll = new JScrollPane(new ImageLabel(pImage));
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
            throw new RuntimeException(e);
        }
    }

    private static class ImageLabel extends JLabel {
        Paint mBackground;

        public ImageLabel(BufferedImage pImage) {
            super(new BufferedImageIcon(pImage));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            mBackground = createTexture();

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
            addCheckBoxItem(new ChangeBackgroundAction("Default", mBackground), popup, group);
            popup.addSeparator();
            addCheckBoxItem(new ChangeBackgroundAction("White", Color.WHITE), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Light", Color.LIGHT_GRAY), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Gray", Color.GRAY), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Dark", Color.DARK_GRAY), popup, group);
            addCheckBoxItem(new ChangeBackgroundAction("Black", Color.BLACK), popup, group);
            popup.addSeparator();
            addCheckBoxItem(new ChooseBackgroundAction("Choose...", Color.BLUE), popup, group);
            return popup;
        }

        private void addCheckBoxItem(final Action pAction, final JPopupMenu pPopup, final ButtonGroup pGroup) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(pAction);
            pGroup.add(item);
            pPopup.add(item);
        }

        private Paint createTexture() {
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
            gr.setPaint(mBackground);
            gr.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }

        private class ChangeBackgroundAction extends AbstractAction {
            protected Paint mPaint;

            public ChangeBackgroundAction(final String pName, final Paint pPaint) {
                super(pName);
                mPaint = pPaint;
            }

            public void actionPerformed(ActionEvent e) {
                mBackground = mPaint;
                repaint();
            }
        }

        private class ChooseBackgroundAction extends ChangeBackgroundAction {
            public ChooseBackgroundAction(final String pName, final Color pColor) {
                super(pName, pColor);
                putValue(Action.SMALL_ICON, new Icon() {
                    public void paintIcon(Component c, Graphics pGraphics, int x, int y) {
                        Graphics g = pGraphics.create();
                        g.setColor((Color) mPaint);
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
                Color selected = JColorChooser.showDialog(ImageLabel.this, "Choose background", (Color) mPaint);
                if (selected != null) {
                    mPaint = selected;
                    super.actionPerformed(e);
                }
            }
        }
    }
}
