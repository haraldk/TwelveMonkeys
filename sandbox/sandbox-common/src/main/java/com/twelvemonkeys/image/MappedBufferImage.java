/*
 * Copyright (c) 2010, Harald Kuhr
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

package com.twelvemonkeys.image;

import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.LRUHashMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * MappedBufferImage
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedBufferImage.java,v 1.0 Jun 13, 2010 7:33:19 PM haraldk Exp$
 */
public class MappedBufferImage {
    private static int threads = Runtime.getRuntime().availableProcessors();
    private static ExecutorService executorService = Executors.newFixedThreadPool(threads * 4);
    private static ExecutorService executorService2 = Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws IOException {
        int argIndex = 0;
        File file = args.length > 0 ? new File(args[argIndex]) : null;
        
        int w;
        int h;
        BufferedImage image;

        if (file != null && file.exists()) {
            argIndex++;

            // Load image using ImageIO
            ImageInputStream input = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                System.err.println("No image reader found for input: " + file.getAbsolutePath());
                System.exit(0);
                return;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input);

                Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
                ImageTypeSpecifier type = types.next();

                // TODO: Negotiate best layout according to the GraphicsConfiguration.

                int sub = 1;
                w = reader.getWidth(0) / sub;
                h = reader.getHeight(0) / sub;

    //            GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    //            ColorModel cm2 = configuration.getColorModel(cm.getTransparency());

    //            image = MappedImageFactory.createCompatibleMappedImage(w, h, cm2);
    //            image = MappedImageFactory.createCompatibleMappedImage(w, h, cm);
    //            image = MappedImageFactory.createCompatibleMappedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
    //            image = MappedImageFactory.createCompatibleMappedImage(w, h, BufferedImage.TYPE_INT_BGR);
//                image = MappedImageFactory.createCompatibleMappedImage(w, h, type);
//                if (w > 1024 || h > 1024) {
                    image = MappedImageFactory.createCompatibleMappedImage(w, h, type);
//                }
//                else {
//                    image = type.createBufferedImage(w, h);
//                }

                System.out.println("image = " + image);

                // TODO: Display image while reading

                ImageReadParam param = reader.getDefaultReadParam();
                param.setDestination(image);
                param.setSourceSubsampling(sub, sub, 0, 0);

                reader.addIIOReadProgressListener(new ConsoleProgressListener());
                reader.read(0, param);
            }
            finally {
                reader.dispose();
            }
        }
        else {
            w = args.length > argIndex && StringUtil.isNumber(args[argIndex]) ? Integer.parseInt(args[argIndex++]) : 6000;
            h = args.length > argIndex && StringUtil.isNumber(args[argIndex]) ? Integer.parseInt(args[argIndex++]) : w * 2 / 3;

            GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            image = MappedImageFactory.createCompatibleMappedImage(w, h, configuration, Transparency.TRANSLUCENT);
//            image = MappedImageFactory.createCompatibleMappedImage(w, h, configuration, Transparency.OPAQUE);
//            image = MappedImageFactory.createCompatibleMappedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);

            System.out.println("image = " + image);

            DataBuffer buffer = image.getRaster().getDataBuffer();
            final boolean alpha = image.getColorModel().hasAlpha();

            // Mix in some nice colors
            createBackground(w, h, buffer, alpha);

            // Add some random dots (get out the coffee)
            paintDots(w, h, image);
        }

        // Resample down to some fixed size
        if (args.length > argIndex && "-scale".equals(args[argIndex++])) {
            image = resampleImage(image, 800);
        }

        int bytesPerPixel = image.getColorModel().getPixelSize() / 8; // Calculate first to avoid overflow
        String size = toHumanReadableSize(w * h * bytesPerPixel);
        showIt(w, h, image, size);
    }

    private static void showIt(final int w, final int h, BufferedImage image, final String size) {
        JFrame frame = new JFrame(String.format("Test [%s x %s] (%s)", w, h, size)) {
            @Override
            public Dimension getPreferredSize() {
                // TODO: This looks like a useful util method...
                DisplayMode displayMode = getGraphicsConfiguration().getDevice().getDisplayMode();
                Dimension size = super.getPreferredSize();

                size.width = Math.min(size.width, displayMode.getWidth());
                size.height = Math.min(size.height, displayMode.getHeight());

                return size;
            }
        };
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JScrollPane scroll = new JScrollPane(new ImageComponent(image));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        frame.add(scroll);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static BufferedImage resampleImage(final BufferedImage image, final int width) {
        long start = System.currentTimeMillis();

        float aspect = image.getHeight() / (float) image.getWidth();
        int height = Math.round(width * aspect);

        // NOTE: The createCompatibleDestImage takes the byte order/layout into account, unlike the cm.createCompatibleWritableRaster
        final BufferedImage output = new ResampleOp(width, height).createCompatibleDestImage(image, null);

        final int steps = threads * height / 100;
        final int inStep = (int) Math.ceil(image.getHeight() / (double) steps);
        final int outStep = (int) Math.ceil(height / (double) steps);

        final CountDownLatch latch = new CountDownLatch(steps);

        //        System.out.println("Starting image scale on single thread, waiting for execution to complete...");
//        BufferedImage output = new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS).filter(image, null);
        System.out.printf("Started image scale on %d threads, waiting for execution to complete...\n", threads);

        System.out.print("[");
        final int dotsPerStep = 78 / steps;
        for (int j = 0; j < 78 - (steps * dotsPerStep); j++) {
            System.out.print(".");
        }

        // Resample image in slices
        for (int i = 0; i < steps; i++) {
            final int inY = i * inStep;
            final int outY = i * outStep;
            final int inHeight = Math.min(inStep, image.getHeight() - inY);
            final int outHeight = Math.min(outStep, output.getHeight() - outY);
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        BufferedImage in = image.getSubimage(0, inY, image.getWidth(), inHeight);
                        BufferedImage out = output.getSubimage(0, outY, width, outHeight);
                        new ResampleOp(width, outHeight, ResampleOp.FILTER_TRIANGLE).filter(in, out);
//                        new ResampleOp(width, outHeight, ResampleOp.FILTER_LANCZOS).filter(in, out);

                        for (int j = 0; j < dotsPerStep; j++) {
                            System.out.print(".");
                        }
                    }
                    catch (RuntimeException e) {
                        e.printStackTrace();
                        throw e;
                    }
                    finally {
                        latch.countDown();
                    }
                }
            });
        }

        Boolean done = null;
        try {
            done = latch.await(5L, TimeUnit.MINUTES);
        }
        catch (InterruptedException ignore) {
        }
        System.out.println("]");

        System.out.printf("%s scaling image in %d ms\n", (done == null ? "Interrupted" : !done ? "Timed out" : "Done"), System.currentTimeMillis() - start);
        System.out.println("image = " + output);

        return output;
    }

    private static void paintDots(int width, int height, final BufferedImage image) {
        long start = System.currentTimeMillis();

        int s = 300;
        int ws = width / s;
        int hs = height / s;

        Color[] colors = new Color[] {
                Color.WHITE, Color.ORANGE, Color.BLUE, Color.MAGENTA, Color.BLACK, Color.RED, Color.CYAN,
                Color.GRAY, Color.GREEN, Color.YELLOW, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY
        };

        CountDownLatch latch = new CountDownLatch(threads);
        int step = (int) Math.ceil(hs / (double) threads);
        Random r = new Random();

        for (int i = 0; i < threads; i++) {
            executorService.submit(new PaintDotsTask(image, s, ws, colors, r, i * step, i * step + step, latch));
        }

        System.err.printf("Started painting in %d threads, waiting for execution to complete...%n", threads);

        Boolean done = null;
        try {
            done = latch.await(3L, TimeUnit.MINUTES);
        }
        catch (InterruptedException ignore) {
        }

        System.out.printf("%s painting %d dots in %d ms%n", (done == null ? "Interrupted" : !done ? "Timed out" : "Done"), Math.max(0, hs - 1) * Math.max(0, ws - 1), System.currentTimeMillis() - start);
    }

    private static void paintDots0(BufferedImage image, int s, int ws, Color[] colors, Random r, final int first, final int last) {
        for (int y = first; y < last; y++) {
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
    }

    private static void createBackground(int w, int h, DataBuffer buffer, boolean alpha) {
        long start = System.currentTimeMillis();

        int step = (int) Math.ceil(h / (double) threads);

        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executorService.submit(new PaintBackgroundTask(w, h, buffer, alpha, i * step, i * step + step, latch));
        }
        System.err.printf("Started painting in %d threads, waiting for execution to complete...%n", threads);

        Boolean done = null;
        try {
            done = latch.await(3L, TimeUnit.MINUTES);
        }
        catch (InterruptedException ignore) {
        }

        System.out.printf("%s creating background in %d ms%n", (done == null ? "Interrupted" : !done ? "Timed out" : "Done"), System.currentTimeMillis() - start);
    }

    private static void paintBackground0(int w, int h, DataBuffer buffer, boolean alpha, final int first, final int last) {
        for (int y = first; y < last; y++) {
            for (int x = 0; x < w; x++) {
                int r = (int) ((x * y * 255.0) / (h * w));
                int g = (int) (((w - x) * y * 255.0) / (h * w));
                int b = (int) ((x * (h - y) * 255.0) / (h * w));
                int a = alpha ? (int) (((w - x) * (h - y) * 255.0) / (h * w)) : 0;

                switch (buffer.getDataType()) {
                    case DataBuffer.TYPE_BYTE:
                        int off = (y * w + x) * (alpha ? 4 : 3);
                        if (alpha) {
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
    }

    private static String toHumanReadableSize(long size) {
        return String.format("%,d MB", (long) (size / (double) (1024L << 10)));
    }

    /**
     * A fairly optimized component for displaying a BufferedImage
     */
    private static class ImageComponent extends JComponent implements Scrollable {
        private final BufferedImage image;
        private Paint texture;
        private double zoom = 1;

        public ImageComponent(final BufferedImage image) {
            setOpaque(true); // Very important when sub classing JComponent...
            setDoubleBuffered(true);

            this.image = image;
        }

        @Override
        public void addNotify() {
            super.addNotify();

            texture = createTexture();

            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            zoom = Math.min(1.0, Math.min(bounds.getWidth() / (double) image.getWidth(), bounds.getHeight() / (double) image.getHeight()));

            // TODO: Take scroll pane into account when zooming (center around center point)
            AbstractAction zoomIn = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    System.err.println("ZOOM IN");
                    setZoom(zoom * 2);
                }
            };

            addAction(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, getToolkit().getMenuShortcutKeyMask()), zoomIn);
            addAction(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, getToolkit().getMenuShortcutKeyMask()), zoomIn);
            addAction(KeyStroke.getKeyStroke(Character.valueOf('+'), 0), zoomIn);
            addAction(KeyStroke.getKeyStroke(Character.valueOf('+'), getToolkit().getMenuShortcutKeyMask()), zoomIn);
            AbstractAction zoomOut = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    System.err.println("ZOOM OUT");
                    setZoom(zoom / 2);
                }
            };
            addAction(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, getToolkit().getMenuShortcutKeyMask()), zoomOut);
            addAction(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, getToolkit().getMenuShortcutKeyMask()), zoomOut);
            addAction(KeyStroke.getKeyStroke(Character.valueOf('-'), 0), zoomOut);
            addAction(KeyStroke.getKeyStroke(Character.valueOf('-'), getToolkit().getMenuShortcutKeyMask()), zoomOut);
            AbstractAction zoomFit = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    System.err.println("ZOOM FIT");
//                    Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    Rectangle bounds = getVisibleRect();
                    setZoom(Math.min(1.0, Math.min(bounds.getWidth() / (double) image.getWidth(), bounds.getHeight() / (double) image.getHeight())));
                }
            };
            addAction(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, getToolkit().getMenuShortcutKeyMask()), zoomFit);
            addAction(KeyStroke.getKeyStroke(KeyEvent.VK_9, getToolkit().getMenuShortcutKeyMask()), zoomFit);
            addAction(KeyStroke.getKeyStroke(KeyEvent.VK_0, getToolkit().getMenuShortcutKeyMask()), new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    System.err.println("ZOOM ACTUAL");
                    setZoom(1);
                }
            });
        }

        private void setZoom(final double newZoom) {
            if (newZoom != zoom) {
                zoom = newZoom;
                // TODO: Add PCL support for zoom and discard tiles cache based on property change
                tiles = createTileCache();
                revalidate();
                repaint();
            }
        }

        private Map<Point, Tile> createTileCache() {
            return Collections.synchronizedMap(new SizedLRUMap<Point, Tile>(16 * 1024 * 1024));
        }

        private void addAction(final KeyStroke keyStroke, final AbstractAction action) {
            UUID key = UUID.randomUUID();
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, key);
            getActionMap().put(key, action);
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
            // TODO: Java 7 kills the performance from our custom painting... :-(

            // TODO: Figure out why mouse wheel/track pad scroll repaints entire component,
            // unlike using the scroll bars of the JScrollPane.
            // Consider creating a custom mouse wheel listener as a workaround.

            // TODO: Cache visible rect content in buffered/volatile image (s) + visible rect (+ zoom) to speed up repaints
            //      - Blit the cahced image (possibly translated) (onto itself?)
            //      - Paint only the necessary parts outside the cached image
            //      - Async rendering into cached image

            // We want to paint only the visible part of the image
            Rectangle visible = getVisibleRect();
            Rectangle clip = g.getClipBounds();
            Rectangle rect = clip == null ? visible : visible.intersection(clip);

            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(texture);
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);

            /*
            // Center image (might not be the best way to cooperate with the scroll pane)
            Rectangle imageSize = new Rectangle((int) Math.round(image.getWidth() * zoom), (int) Math.round(image.getHeight() * zoom));
            if (imageSize.width < getWidth()) {
                g2.translate((getWidth() - imageSize.width) / 2, 0);
            }
            if (imageSize.height < getHeight()) {
                g2.translate(0, (getHeight() - imageSize.height) / 2);
            }
            */

            // Zoom
            if (zoom != 1) {
                // NOTE: This helps mostly when scaling up, or scaling down less than 50%
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                rect = new Rectangle(
                        (int) Math.round(rect.x / zoom), (int) Math.round(rect.y / zoom),
                        (int) Math.round(rect.width / zoom), (int) Math.round(rect.height / zoom)
                );

                rect = rect.intersection(new Rectangle(image.getWidth(), image.getHeight()));
            }

            long start = System.currentTimeMillis();
            repaintImage(rect, g2);
            System.err.println("repaint: " + (System.currentTimeMillis() - start) + " ms");
        }

        static class Tile {
            private final int size;

            private final int x;
            private final int y;

            private final Reference<BufferedImage> data;
            private final BufferedImage hardRef;

            Tile(int x, int y, BufferedImage data) {
                this.x = x;
                this.y = y;
                this.data = new SoftReference<BufferedImage>(data);

                hardRef = data;

                size = 16 + data.getWidth() * data.getHeight() * data.getRaster().getNumDataElements() * sizeOf(data.getRaster().getTransferType());
            }

            private static int sizeOf(final int transferType) {
                switch (transferType) {
                    case DataBuffer.TYPE_INT:
                        return 4;
                    case DataBuffer.TYPE_SHORT:
                        return 2;
                    case DataBuffer.TYPE_BYTE:
                        return 1;
                    default:
                        throw new IllegalArgumentException("Unsupported transfer type: " + transferType);
                }
            }

            public boolean drawTo(Graphics2D g) {
                BufferedImage img = data.get();

                if (img != null) {
                    g.drawImage(img, x, y, null);
                    return true;
                }

                return false;
            }

            public int getX() {
                return x;
            }

            public int getY() {
                return y;
            }

            public int getWidth() {
                BufferedImage img = data.get();
                return img != null ? img.getWidth() : -1;
            }

            public int getHeight() {
                BufferedImage img = data.get();
                return img != null ? img.getHeight() : -1;
            }

            public Rectangle getRect() {
                BufferedImage img = data.get();
                return img != null ? new Rectangle(x, y, img.getWidth(), img.getHeight()) : null;
            }

            public Point getLocation() {
                return new Point(x, y);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                }

                if (other == null || getClass() != other.getClass()) {
                    return false;
                }

                Tile tile = (Tile) other;

                return x == tile.x && y == tile.y;
            }

            @Override
            public int hashCode() {
                return 997 * x + y;
            }

            @Override
            public String toString() {
                return String.format("Tile[%d, %d, %d, %d]", x, y, getWidth(), getHeight());
            }

            public int size() {
                return size;
            }
        }

        // TODO: Consider a fixed size (mem) LRUCache instead
        // TODO: Better yet, re-use tiles
        Map<Point, Tile> tiles = createTileCache();

        private void repaintImage(final Rectangle rect, final Graphics2D g2) {
//            System.err.println("rect: " + rect);
//            System.err.println("tiles: " + tiles.size());
            // TODO: Fix rounding errors
            // FIx repaint bugs

            try {
                // Paint tiles of the image, to preserve memory
                final int tileSize = 200;

                // Calculate relative to image(0,0), rather than rect(x, y)
                int xOff = rect.x % tileSize;
                int yOff = rect.y % tileSize;

                rect.x -= xOff;
                rect.y -= yOff;
                rect.width += xOff;
                rect.height += yOff;

                int tilesW = 1 + rect.width / tileSize;
                int tilesH = 1 + rect.height / tileSize;

                for (int yTile = 0; yTile <= tilesH; yTile++) {
                    for (int xTile = 0; xTile <= tilesW; xTile++) {
                        // Image (source) coordinates
                        int x = rect.x + xTile * tileSize;
                        int y = rect.y + yTile * tileSize;

                        int w = xTile == tilesW ? Math.min(tileSize, rect.x + rect.width - x) : tileSize;
                        int h = yTile == tilesH ? Math.min(tileSize, rect.y + rect.height - y) : tileSize;

                        if (w == 0 || h == 0) {
                            continue;
                        }

//                        System.err.printf("%04d, %04d, %04d, %04d%n", x, y, w, h);

                        //  - Get tile from cache
                        //  - If non-null, paint
                        //  - If null, request data for later use, with callback, and return
                        // TODO: Could we use ImageProducer/ImageConsumer/ImageObserver interface??

                        // Destination (display) coordinates
                        int dstX = (int) Math.floor(x * zoom);
                        int dstY = (int) Math.floor(y * zoom);
                        int dstW = (int) Math.ceil(w * zoom);
                        int dstH = (int) Math.ceil(h * zoom);

                        if (dstW == 0 || dstH == 0) {
                            continue;
                        }

                            // Don't create overlapping/duplicate tiles...
                            //  - Always start tile grid at 0,0
                            //  - Always occupy entire tile, unless edge

                            // Source (original) coordinates
                            int tileSrcX = x - x % tileSize;
                            int tileSrcY = y - y % tileSize;
//                            final int tileSrcW = Math.min(tileSize, image.getWidth() - tileSrcX);
//                            final int tileSrcH = Math.min(tileSize, image.getHeight() - tileSrcY);

                            // Destination (display) coordinates
                            int tileDstX = (int) Math.floor(tileSrcX * zoom);
                            int tileDstY = (int) Math.floor(tileSrcY * zoom);
//                            final int tileDstW = (int) Math.round(tileSrcW * zoom);
//                            final int tileDstH = (int) Math.round(tileSrcH * zoom);

                        List<Point> points = new ArrayList<Point>(4);
                        points.add(new Point(tileDstX, tileDstY));
                        if (tileDstX != dstX) {
                            points.add(new Point(tileDstX + tileSize, tileDstY));
                        }
                        if (tileDstY != dstY) {
                            points.add(new Point(tileDstX, tileDstY + tileSize));
                        }
                        if (tileDstX != dstX && tileDstY != dstY) {
                            points.add(new Point(tileDstX + tileSize, tileDstY + tileSize));
                        }

                        for (final Point point : points) {
                            Tile tile = tiles.get(point);

                            if (tile != null) {
                                if (tile.drawTo(g2)) {
                                    continue;
                                }
                                else {
                                    tiles.remove(point);
                                }
                            }

//                            System.err.printf("Tile miss: [%d, %d]\n", dstX, dstY);

                            // Dispatch to off-thread worker
                            final Map<Point, Tile> localTiles = tiles;
                            executorService2.submit(new Runnable() {
                                public void run() {
                                    int tileSrcX = (int) Math.round(point.x / zoom);
                                    int tileSrcY = (int) Math.round(point.y / zoom);
                                    int tileSrcW = Math.min(tileSize, image.getWidth() - tileSrcX);
                                    int tileSrcH = Math.min(tileSize, image.getHeight() - tileSrcY);
                                    int tileDstW = (int) Math.round(tileSrcW * zoom);
                                    int tileDstH = (int) Math.round(tileSrcH * zoom);

                                    try {
                                        // TODO: Consider comparing zoom/local zoom
                                        if (localTiles != tiles) {
                                            return; // Return early after re-zoom
                                        }

                                        if (localTiles.containsKey(point)) {
//                                            System.err.println("Skipping tile, already producing...");
                                            return;
                                        }

                                        // Test against current view rect, to avoid computing tiles that will be thrown away immediately
                                        final Rectangle visibleRect = new Rectangle();
                                        SwingUtilities.invokeAndWait(new Runnable() {
                                            public void run() {
                                                visibleRect.setBounds(getVisibleRect());
                                            }
                                        });

                                        if (!visibleRect.intersects(new Rectangle(point.x, point.y, tileDstW, tileDstH))) {
                                            return;
                                        }

//                                        System.err.printf("Creating tile: [%d, %d]\n", tileDstX, tileDstY);

                                        BufferedImage temp = getGraphicsConfiguration().createCompatibleImage(tileDstW, tileDstH);
                                        final Tile tile = new Tile(point.x, point.y, temp);
                                        localTiles.put(point, tile);

                                        Graphics2D graphics = temp.createGraphics();
                                        try {
                                            Object hint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);

                                            if (hint != null) {
                                                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
                                            }

                                            graphics.scale(zoom, zoom);
                                            graphics.drawImage(image.getSubimage(tileSrcX, tileSrcY, tileSrcW, tileSrcH), 0, 0, null);
                                        }
                                        finally {
                                            graphics.dispose();
                                        }

                                        SwingUtilities.invokeLater(new Runnable() {
                                            public void run() {
                                                repaint(10, tile.x, tile.y, tile.getWidth(), tile.getHeight());
                                            }
                                        });
                                    }
                                    catch (Throwable t) {
                                        localTiles.remove(point);
                                        System.err.println("Boooo: " + t.getMessage());
                                    }
                                }
                            });
                        }

                    }
                }
            }
            catch (NullPointerException e) {
//                e.printStackTrace();
                // Happens whenever apple.awt.OSXCachingSurfaceManager runs out of memory
                // TODO: Figure out why repaint(x,y,w,h) doesn't work any more..?
                System.err.println("Full repaint due to NullPointerException (probably out of memory).");
                repaint(); // NOTE: Might cause a brief flash while the component is redrawn
            }
        }

        private void repaintImage0(final Rectangle rect, final Graphics2D g2) {
            g2.scale(zoom, zoom);

            try {
                // Paint tiles of the image, to preserve memory
                final int tileSize = 200;

                int tilesW = rect.width / tileSize;
                int tilesH = rect.height / tileSize;

                for (int yTile = 0; yTile <= tilesH; yTile++) {
                    for (int xTile = 0; xTile <= tilesW; xTile++) {
                        // Image (source) coordinates
                        final int x = rect.x + xTile * tileSize;
                        final int y = rect.y + yTile * tileSize;

                        final int w = xTile == tilesW ? Math.min(tileSize, rect.x + rect.width - x) : tileSize;
                        final int h = yTile == tilesH ? Math.min(tileSize, rect.y + rect.height - y) : tileSize;

                        if (w == 0 || h == 0) {
                            continue;
                        }

//                        System.err.printf("%04d, %04d, %04d, %04d%n", x, y, w, h);

                        BufferedImage img = image.getSubimage(x, y, w, h);
                        g2.drawImage(img, x, y, null);

                    }
                }
            }
            catch (NullPointerException e) {
//                e.printStackTrace();
                // Happens whenever apple.awt.OSXCachingSurfaceManager runs out of memory
                // TODO: Figure out why repaint(x,y,w,h) doesn't work any more..?
                System.err.println("Full repaint due to NullPointerException (probably out of memory).");
                repaint(); // NOTE: Might cause a brief flash while the component is redrawn
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension((int) (image.getWidth() * zoom), (int) (image.getHeight() * zoom));
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
            return getWidth() > getPreferredSize().width;
        }

        public boolean getScrollableTracksViewportHeight() {
            return getHeight() > getPreferredSize().height;
        }
    }

    final static class SizedLRUMap<K, V> extends LRUHashMap<K, V> {
        int currentSize;
        int maxSize;

        public SizedLRUMap(int pMaxSize) {
            super(); // Note: super.maxSize doesn't count...
            maxSize = pMaxSize;
        }


        protected int sizeOf(final Object pValue) {
            ImageComponent.Tile cached = (ImageComponent.Tile) pValue;

            if (cached == null) {
                return 0;
            }

            return cached.size();
        }

        @Override
        public V put(K pKey, V pValue) {
            currentSize += sizeOf(pValue);

            V old = super.put(pKey, pValue);
            if (old != null) {
                currentSize -= sizeOf(old);
            }
            return old;
        }

        @Override
        public V remove(Object pKey) {
            V old = super.remove(pKey);
            if (old != null) {
                currentSize -= sizeOf(old);
            }
            return old;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> pEldest) {
            if (maxSize <= currentSize) { // NOTE: maxSize here is mem size
                removeLRU();
            }
            return false;
        }

        @Override
        public void removeLRU() {
            while (maxSize <= currentSize) { // NOTE: maxSize here is mem size
                super.removeLRU();
            }
        }
    }

    private static class PaintDotsTask implements Runnable {
        private final BufferedImage image;
        private final int s;
        private final int wstep;
        private final Color[] colors;
        private final Random random;
        private final int last;
        private final int first;
        private final CountDownLatch latch;

        public PaintDotsTask(BufferedImage image, int s, int wstep, Color[] colors, Random random, int first, int last, CountDownLatch latch) {
            this.image = image;
            this.s = s;
            this.wstep = wstep;
            this.colors = colors;
            this.random = random;
            this.last = last;
            this.first = first;
            this.latch = latch;
        }

        public void run() {
            try {
                paintDots0(image, s, wstep, colors, random, first, last);
            }
            finally {
                latch.countDown();
            }
        }
    }

    private static class PaintBackgroundTask implements Runnable {
        private final int w;
        private final int h;
        private final DataBuffer buffer;
        private final boolean alpha;
        private final int first;
        private final int last;
        private final CountDownLatch latch;

        public PaintBackgroundTask(int w, int h, DataBuffer buffer, boolean alpha, int first, int last, CountDownLatch latch) {
            this.w = w;
            this.h = h;
            this.buffer = buffer;
            this.alpha = alpha;
            this.first = first;
            this.last = last;
            this.latch = latch;
        }

        public void run() {
            try {
                paintBackground0(w, h, buffer, alpha, first, last);
            }
            finally {
                latch.countDown();
            }
        }
    }

    private static class ConsoleProgressListener extends ProgressListenerBase {
        static final int COLUMNS = System.getenv("COLUMNS") != null ? Integer.parseInt(System.getenv("COLUMNS")) - 2 : 78;
        int left = COLUMNS;

        @Override
        public void imageComplete(ImageReader source) {
            for (; left > 0; left--) {
                System.out.print(".");
            }
            System.out.println("]");
        }

        @Override
        public void imageProgress(ImageReader source, float percentageDone) {
            int progress = COLUMNS - Math.round(COLUMNS * percentageDone / 100f);
            if (progress < left) {
                for (; left > progress; left--) {
                    System.out.print(".");
                }
            }
        }

        @Override
        public void imageStarted(ImageReader source, int imageIndex) {
            System.out.print("[");
        }
    }
}
