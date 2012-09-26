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

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * MappedBufferImage
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedBufferImage.java,v 1.0 Jun 13, 2010 7:33:19 PM haraldk Exp$
 */
public class MappedBufferImage {
    private static int threads = Runtime.getRuntime().availableProcessors();
    private static ExecutorService executorService = Executors.newFixedThreadPool(threads);

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

                w = reader.getWidth(0);
                h = reader.getHeight(0);

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

                ImageReadParam param = reader.getDefaultReadParam();
                param.setDestination(image);

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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

        final int inStep = (int) Math.ceil(image.getHeight() / (double) threads);
        final int outStep = (int) Math.ceil(height / (double) threads);

        final CountDownLatch latch = new CountDownLatch(threads);

        // Resample image in slices
        for (int i = 0; i < threads; i++) {
            final int inY = i * inStep;
            final int outY = i * outStep;
            final int inHeight = Math.min(inStep, image.getHeight() - inY);
            final int outHeight = Math.min(outStep, output.getHeight() - outY);
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        BufferedImage in = image.getSubimage(0, inY, image.getWidth(), inHeight);
                        BufferedImage out = output.getSubimage(0, outY, width, outHeight);
                        new ResampleOp(width, outHeight, ResampleOp.FILTER_LANCZOS).filter(in, out);
//                        new ResampleOp(width, outHeight, ResampleOp.FILTER_LANCZOS).resample(in, out, ResampleOp.createFilter(ResampleOp.FILTER_LANCZOS));
//                        BufferedImage out = new ResampleOp(width, outHeight, ResampleOp.FILTER_LANCZOS).filter(in, null);
//                        ImageUtil.drawOnto(output.getSubimage(0, outY, width, outHeight), out);
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

//        System.out.println("Starting image scale on single thread, waiting for execution to complete...");
//        BufferedImage output = new ResampleOp(width, height, ResampleOp.FILTER_LANCZOS).filter(image, null);
        System.out.printf("Started image scale on %d threads, waiting for execution to complete...%n", threads);

        Boolean done = null;
        try {
            done = latch.await(5L, TimeUnit.MINUTES);
        }
        catch (InterruptedException ignore) {
        }

        System.out.printf("%s scaling image in %d ms%n", (done == null ? "Interrupted" : !done ? "Timed out" : "Done"), System.currentTimeMillis() - start);
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
        double zoom = 1;

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

            if (zoom != 1) {
                AffineTransform transform = AffineTransform.getScaleInstance(zoom, zoom);
                g2.setTransform(transform);
            }

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
            return false;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
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
