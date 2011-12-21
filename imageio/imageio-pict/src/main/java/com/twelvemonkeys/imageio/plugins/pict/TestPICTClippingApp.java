package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.image.BufferedImageIcon;
import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.net.MIMEUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TestPICTClippingApp
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TestPICTClippingApp.java,v 1.0 Feb 16, 2009 3:05:16 PM haraldk Exp$
 */
public class TestPICTClippingApp {
    public static void main(final String[] pArgs) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Exception ignore) {
                }

                JFrame frame = new JFrame("PICTClipping test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                JLabel dropZone = new JLabel("Drop images here", JLabel.CENTER) {
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(320, 200);
                    }
                };

                dropZone.setTransferHandler(new ImageDropHandler(dropZone));
                frame.add(dropZone);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    private static class ImageDropHandler extends TransferHandler {
        private final JLabel label;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        public ImageDropHandler(JLabel pLabel) {
            super(null);
            label = pLabel;
        }

        private DataFlavor getSupportedFlavor(final DataFlavor[] transferFlavors) {
            for (DataFlavor flavor : transferFlavors) {
                String type = MIMEUtil.bareMIME(flavor.getMimeType());
                if (InputStream.class.isAssignableFrom(flavor.getDefaultRepresentationClass()) && ImageIO.getImageReadersByMIMEType(type).hasNext()) {
                    return flavor;
                }
                else if (flavor.equals(DataFlavor.javaFileListFlavor)) {
                    return flavor;
                }
            }

            for (DataFlavor flavor : transferFlavors) {
                System.err.printf("flavor: %s%n", flavor);
            }

            return null;
        }

        @Override
        public boolean canImport(final JComponent comp, final DataFlavor[] transferFlavors) {
            return getSupportedFlavor(transferFlavors) != null;
        }

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            DataFlavor[] flavors = t.getTransferDataFlavors();
            DataFlavor flavor = getSupportedFlavor(flavors);
            if (flavor != null) {
                try {
                    InputStream input;
                    if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                        List files = (List) t.getTransferData(flavor);
                        if (files.isEmpty()) {
                            return false;
                        }
                        else {
                            input = new FileInputStream((File) files.get(0));
                        }
                    }
                    else {
                        Object data = t.getTransferData(flavor);
                        input = (InputStream) data;
                    }

                    final ImageInputStream stream = ImageIO.createImageInputStream(input);
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

                    if (!readers.hasNext()) {
                        String mimeType = MIMEUtil.bareMIME(flavor.getMimeType());
                        System.out.printf("Getting reader by MIME type (%s)...%n", mimeType);
                        readers = ImageIO.getImageReadersByMIMEType(mimeType);
                    }

                    if (readers.hasNext()) {
                        final ImageReader imageReader = readers.next();

                        executor.execute(new Runnable() {
                            public void run() {
                                try {
                                    readAndInstallImage(stream, imageReader);
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        return true;
                    }
                    else {
                        System.err.println("No reader found!");
                    }

                }
                catch (UnsupportedFlavorException ignore) {
                    ignore.printStackTrace();
                }
                catch (IOException ignore) {
                    ignore.printStackTrace();
                }
                catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

            return false;
        }

        private void readAndInstallImage(final ImageInputStream pStream, final ImageReader reader) throws IOException {
            reader.setInput(pStream);

            final int maxDimension = 200;
            int w = reader.getWidth(0);
            int h = reader.getHeight(0);

            ImageReadParam param = null;
            if (w > maxDimension && h > maxDimension) {
                int sub = (int) Math.ceil((Math.max(w, h) / (double) maxDimension) / 3.0);
                if (sub > 1) {
                    param = reader.getDefaultReadParam();
                    param.setSourceSubsampling(sub, sub, 0, 0);
                }
            }

            System.out.printf("Reading %s format%s... ", reader.getFormatName(), (param != null ? ", sampling every " + param.getSourceXSubsampling() + "th pixel" : ""));
            final BufferedImage image = reader.read(0, param);
            System.out.printf("Done (%dx%d).%n", image.getWidth(), image.getHeight());

            reader.dispose();

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    System.out.print("Scaling image... ");
                    BufferedImage scaled = box(image, maxDimension);
                    System.out.printf("Done (%dx%d).%n", scaled.getWidth(), scaled.getHeight());
                    label.setIcon(new BufferedImageIcon(scaled));
                }
            });
        }

        private BufferedImage box(final BufferedImage pImage, final int pMaxDimension) {
            // TODO: ImageUtil.toRGB method? ColorConvertOp MUCH faster than ImageUtil.toBuffered(img, type)
            BufferedImage image = pImage;
            if (image.getType() == 0) {
                try {
                    ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
                    image = op.filter(image, new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    image = ImageUtil.accelerate(image);
                }
            }

            if (image.getWidth() > pMaxDimension || image.getHeight() > pMaxDimension) {
                int w, h;

                if (image.getWidth() > image.getHeight()) {
                    w = pMaxDimension;
                    h = (int) Math.round(w / (image.getWidth() / (double) image.getHeight()));
                }
                else {
                    h = pMaxDimension;
                    w = (int) Math.round(h * (image.getWidth() / (double) image.getHeight()));
                }

                return ImageUtil.createResampled(image, w, h, Image.SCALE_DEFAULT);
            }
            return image;
        }
    }
}