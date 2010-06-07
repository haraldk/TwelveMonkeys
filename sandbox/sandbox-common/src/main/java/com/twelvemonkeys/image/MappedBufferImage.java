package com.twelvemonkeys.image;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/**
 * MappedBufferImage
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedBufferImage.java,v 1.0 May 26, 2010 5:07:01 PM haraldk Exp$
 */
public class MappedBufferImage extends BufferedImage {
    private static final boolean ALPHA = true;

    public MappedBufferImage(ColorModel cm, MappedFileRaster raster, boolean isRasterPremultiplied) {
        super(cm, raster, isRasterPremultiplied, null);
    }

    public static void main(String[] args) throws IOException {
        int w = args.length > 0 ? Integer.parseInt(args[0]) : 6000;
        int h = args.length > 1 ? Integer.parseInt(args[1]) : (args.length > 0 ? w * 2 / 3 : 4000);

        DataBuffer buffer = new MappedFileBuffer(w, h, ALPHA ? 4 : 3, 1);

        // Mix in some nice colors  
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = (int) ((x * y * 255.0) / (h * w));
                int g = (int) (((w - x) * y * 255.0) / (h * w));
                int b = (int) ((x * (h - y) * 255.0) / (h * w));

                int off = (y * w + x) * (ALPHA ? 4 : 3);

                if (ALPHA) {
                    int a = (int) (((w - x) * (h - y) * 255.0) / (h * w));
                    buffer.setElem(off++, 255 - a);
                }

                buffer.setElem(off++, b);
                buffer.setElem(off++, g);
                buffer.setElem(off, r);

            }
        }

        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ComponentColorModel model = new ComponentColorModel(cs, ALPHA, false, ALPHA ? TRANSLUCENT : OPAQUE, DataBuffer.TYPE_BYTE);
        BufferedImage image = new MappedBufferImage(model, new MappedFileRaster(w, h, buffer), false);

        // Add some random dots (get out the coffee)
//        int s = 300;
//        int ws = w / s;
//        int hs = h / s;
//
//        Color[] colors = new Color[] {
//                Color.WHITE, Color.ORANGE, Color.BLUE, Color.MAGENTA, Color.BLACK, Color.RED, Color.CYAN,
//                Color.GRAY, Color.GREEN, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY
//        };
//
//        Random r = new Random();
//
//        for (int y = 0; y < hs - 1; y++) {
//            for (int x = 0; x < ws - 1; x++) {
//                Graphics2D g = image.getSubimage(x * s, y * s, 2 * s, 2 * s).createGraphics();
//                try {
//                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//                    g.setComposite(AlphaComposite.SrcOver.derive(r.nextFloat()));
//                    g.setColor(colors[r.nextInt(colors.length)]);
//                    int o = r.nextInt(s) + s / 10;
//                    int c = (2 * s - o) / 2;
//                    g.fillOval(c, c, o, o);
//                }
//                finally {
//                    g.dispose();
//                }
//            }
//        }

        System.out.println("image = " + image);

        JFrame frame = new JFrame(String.format("Test [%s x %s] (%s)", w, h, toHumanReadableSize(w * h * (ALPHA ? 4 : 3))));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JScrollPane scroll = new JScrollPane(new ImageComponent(image));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getViewport().setDoubleBuffered(false);
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
        private final Paint texture;

        public ImageComponent(BufferedImage image) {
            setDoubleBuffered(false);
            this.image = image;

            texture = createTexture();
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
            Insets insets = getInsets();

            // We ant to paint only the visible part of the image
            Rectangle rect = getVisibleRect();

            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(texture);
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);

            try {
                // Paint slices of the image, to preserve memory
                // Make slices wide to conform to memory alignment of buffer
                int sliceHeight = 200;
                int slices = rect.height / sliceHeight;
                for (int i = 0; i <= slices; i++) {
                    int h = i == slices ? Math.min(sliceHeight, image.getHeight() - (rect.y + i * sliceHeight)) : sliceHeight;
                    if (h == 0) {
                        break;
                    }
                    BufferedImage img = image.getSubimage(rect.x, rect.y + i * sliceHeight, rect.width, h);
                    g2.drawImage(img, insets.left + rect.x, insets.top + rect.y + i * sliceHeight, null);
                }
//                BufferedImage img = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
//                g2.drawImage(img, insets.left + rect.x, insets.top + rect.y, null);
            }
            catch (NullPointerException e) {
                e.printStackTrace();
                // Happens whenever apple.awt.OSXCachingSufraceManager runs out of memory
                repaint(); // NOTE: Will cause a brief flash while the component is redrawn
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            return new Dimension(image.getWidth() + insets.left + insets.right, image.getHeight() + insets.top + insets.bottom);
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

    private static class MappedFileBuffer extends DataBuffer {
        final ByteBuffer buffer;

        public MappedFileBuffer(final int width, final int height, final int numComponents, final int numBanks) throws IOException {
            super(DataBuffer.TYPE_BYTE, width * height * numComponents, numBanks);

            if (size < 0) {
                throw new IllegalArgumentException("Integer overflow");
            }

            File tempFile = File.createTempFile(String.format("%s-", getClass().getSimpleName()), ".tmp");
            tempFile.deleteOnExit();

            RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
            raf.setLength(size * banks);
            FileChannel channel = raf.getChannel();

            // Map entire file into memory, let OS virtual memory/paging do the heavy lifting
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size * banks);

            // According to the docs, we can safely close the channel and delete the file now
            channel.close();

            if (!tempFile.delete()) {
                System.err.println("Could not delete temp file: " + tempFile.getAbsolutePath());
            }
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, (byte) val);
        }
    }

    private static class MappedFileRaster extends WritableRaster {
        public MappedFileRaster(int w, int h, DataBuffer buffer) {
            super(
                    new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, w, h, ALPHA ? 4 : 3, w * (ALPHA ? 4 : 3), ALPHA ? new int[]{3, 2, 1, 0} : new int[]{2, 1, 0}),
                    buffer, new Point()
            );
        }

        @Override
        public String toString() {
            return String.format("%s@%s: w = %s h = %s", getClass().getSimpleName(), System.identityHashCode(this), getWidth(), getHeight());
        }
    }
}
