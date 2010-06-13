package com.twelvemonkeys.image;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.util.Random;

/**
 * MappedBufferImage
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedBufferImage.java,v 1.0 Jun 13, 2010 7:33:19 PM haraldk Exp$
 */
public class MappedBufferImage {
    private static final boolean ALPHA = true;

    public static void main(String[] args) throws IOException {
        int w = args.length > 0 ? Integer.parseInt(args[0]) : 6000;
        int h = args.length > 1 ? Integer.parseInt(args[1]) : w * 2 / 3;

        GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage image = MappedImageFactory.createCompatibleMappedImage(w, h, configuration, MappedBufferImage.ALPHA ? Transparency.TRANSLUCENT : Transparency.OPAQUE);

        System.out.println("image = " + image);

        DataBuffer buffer = image.getRaster().getDataBuffer();

        // Mix in some nice colors
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = (int) ((x * y * 255.0) / (h * w));
                int g = (int) (((w - x) * y * 255.0) / (h * w));
                int b = (int) ((x * (h - y) * 255.0) / (h * w));
                int a = ALPHA ? (int) (((w - x) * (h - y) * 255.0) / (h * w)) : 0;

                switch (buffer.getDataType()) {
                    case DataBuffer.TYPE_BYTE:
                        int off = (y * w + x) * (ALPHA ? 4 : 3);
                        if (ALPHA) {
                            buffer.setElem(off++, 255 - a);
                            buffer.setElem(off++, b);
                            buffer.setElem(off++, g);
                            buffer.setElem(off, r);
                        }
                        else {
                            // TODO: Why the RGB / ABGR byte order inconsistency??
                            buffer.setElem(off++, r);
                            buffer.setElem(off++, g);
                            buffer.setElem(off, b);
                        }
                        break;
                    case DataBuffer.TYPE_INT:
                        buffer.setElem(y * w + x, (255 - a) << 24 | r << 16 | g << 8 | b);
                        break;
                    default:
                        System.err.println("Transfer type not supported: " + buffer.getDataType());
                }
            }
        }

        // Add some random dots (get out the coffee)
        int s = 300;
        int ws = w / s;
        int hs = h / s;

        Color[] colors = new Color[] {
                Color.WHITE, Color.ORANGE, Color.BLUE, Color.MAGENTA, Color.BLACK, Color.RED, Color.CYAN,
                Color.GRAY, Color.GREEN, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY
        };

        Random r = new Random();

        long start = System.currentTimeMillis();
        for (int y = 0; y < hs - 1; y++) {
            for (int x = 0; x < ws - 1; x++) {
                BufferedImage tile = image.getSubimage(x * s, y * s, 2 * s, 2 * s);
                Graphics2D g;
                try {
                    g = tile.createGraphics();
                }
                catch (OutOfMemoryError e) {
                    System.gc();
                    System.err.println("Out of memory: " + e.getMessage());
                    g = tile.createGraphics(); // If this fails, give up
                }

                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setComposite(AlphaComposite.SrcOver.derive(r.nextFloat()));
                    g.setColor(colors[r.nextInt(colors.length)]);
                    int o = r.nextInt(s) + s / 10;
                    int c = (2 * s - o) / 2;
                    g.fillOval(c, c, o, o);
                }
                finally {
                    g.dispose();
                }
            }
        }

        System.out.printf("Done painting %d dots in %d ms%n", hs * ws, System.currentTimeMillis() - start);

        JFrame frame = new JFrame(String.format("Test [%s x %s] (%s)", w, h, toHumanReadableSize(w * h * (ALPHA ? 4 : 3))));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JScrollPane scroll = new JScrollPane(new ImageComponent(image));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        frame.add(scroll);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static String toHumanReadableSize(long size) {
        return String.format("%,d MB", (int) (size / (double) (1024L << 10)));
    }

    private static class ImageComponent extends JComponent implements Scrollable {
        private final BufferedImage image;
        private Paint texture;

        public ImageComponent(final BufferedImage image) {
            setOpaque(true); // Very important when subclassing JComponent...
            this.image = image;
        }

        @Override
        public void addNotify() {
            super.addNotify();

            texture = createTexture();
        }

        private Paint createTexture() {
            BufferedImage pattern = getGraphicsConfiguration().createCompatibleImage(20, 20);
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
            // TODO: Figure out why mouse wheel/track pad scroll repaints entire component,
            // unlike using the scroll bars of the JScrollPane.
            // Consider creating a custom mouse wheel listener as a workaround.

            // We want to paint only the visible part of the image
            Rectangle visible = getVisibleRect();
            Rectangle clip = g.getClipBounds();
            Rectangle rect = clip == null ? visible : visible.intersection(clip);

            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(texture);
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);

            long start = System.currentTimeMillis();
            repaintImage(rect, g2);
            System.err.println("repaint: " + (System.currentTimeMillis() - start) + " ms");
        }

        private void repaintImage(Rectangle rect, Graphics2D g2) {
            try {
                // Paint tiles of the image, to preserve memory
                int sliceSize = 200;

                int slicesW = rect.width / sliceSize;
                int slicesH = rect.height / sliceSize;

                for (int sliceY = 0; sliceY <= slicesH; sliceY++) {
                    for (int sliceX = 0; sliceX <= slicesW; sliceX++) {
                        int x = rect.x + sliceX * sliceSize;
                        int y = rect.y + sliceY * sliceSize;

                        int w = sliceX == slicesW ? Math.min(sliceSize, rect.x + rect.width - x) : sliceSize;
                        int h = sliceY == slicesH ? Math.min(sliceSize, rect.y + rect.height - y) : sliceSize;

                        if (w == 0 || h == 0) {
                            continue;
                        }

//                        System.err.printf("%04d, %04d, %04d, %04d%n", x, y, w, h);
                        BufferedImage img = image.getSubimage(x, y, w, h);
                        g2.drawImage(img, x, y, null);
                    }
                }

//                BufferedImage img = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
//                g2.drawImage(img, rect.x, rect.y, null);
            }
            catch (NullPointerException e) {
//                e.printStackTrace();
                // Happens whenever apple.awt.OSXCachingSufraceManager runs out of memory
                // TODO: Figure out why repaint(x,y,w,h) doesn't work any more..?
//                repaint(rect.x, rect.y, rect.width, rect.height); // NOTE: Will cause a brief flash while the component is redrawn
                repaint(); // NOTE: Might cause a brief flash while the component is redrawn
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            switch (orientation) {
                case SwingConstants.HORIZONTAL:
                    return visibleRect.width * 3 / 4;
                case SwingConstants.VERTICAL:
                default:
                    return visibleRect.height * 3 / 4;
            }
        }

        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
