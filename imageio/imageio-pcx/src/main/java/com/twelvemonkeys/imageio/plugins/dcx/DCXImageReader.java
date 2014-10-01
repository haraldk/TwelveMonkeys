package com.twelvemonkeys.imageio.plugins.dcx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageReaderSpi;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.plugins.pcx.PCXImageReader;
import com.twelvemonkeys.imageio.util.ProgressListenerBase;
import com.twelvemonkeys.xml.XMLSerializer;

public final class DCXImageReader extends ImageReaderBase {
    // TODO: Delegate listeners with correct index!

    private DCXHeader header;

    private PCXImageReader readerDelegate;
    private ProgressDelegator progressDelegator;

    public DCXImageReader(final ImageReaderSpi provider) {
        super(provider);
        readerDelegate = new PCXImageReader(provider);

        progressDelegator = new ProgressDelegator();
        installListeners();
    }

    private void installListeners() {
        readerDelegate.addIIOReadProgressListener(progressDelegator);
        readerDelegate.addIIOReadWarningListener(progressDelegator);
    }

    @Override protected void resetMembers() {
        header = null;

        readerDelegate.reset();
        installListeners();
    }

    @Override public void dispose() {
        super.dispose();

        readerDelegate.dispose();
        readerDelegate = null;
    }

    @Override public int getWidth(final int imageIndex) throws IOException {
        initIndex(imageIndex);

        return readerDelegate.getWidth(0);
    }

    @Override public int getHeight(final int imageIndex) throws IOException {
        initIndex(imageIndex);

        return readerDelegate.getHeight(0);
    }

    @Override public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
        initIndex(imageIndex);

        return readerDelegate.getRawImageType(0);
    }

    @Override public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
        initIndex(imageIndex);

        return readerDelegate.getImageTypes(0);
    }

    @Override public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        initIndex(imageIndex);

        return readerDelegate.read(imageIndex, param);
    }

    @Override public IIOMetadata getImageMetadata(final int imageIndex) throws IOException {
        initIndex(imageIndex);

        return readerDelegate.getImageMetadata(0);
    }

    @Override public synchronized void abort() {
        super.abort();
        readerDelegate.abort();
    }

    @Override public int getNumImages(final boolean allowSearch) throws IOException {
        readHeader();

        return header.getCount();
    }

    private void initIndex(final int imageIndex) throws IOException {
        checkBounds(imageIndex);

        imageInput.seek(header.getOffset(imageIndex));
        progressDelegator.index = imageIndex;
        readerDelegate.setInput(imageInput);
    }

    private void readHeader() throws IOException {
        assertInput();

        if (header == null) {
            imageInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            header = DCXHeader.read(imageInput);
//            System.err.println("header: " + header);
            imageInput.flushBefore(imageInput.getStreamPosition());
        }

        imageInput.seek(imageInput.getFlushedPosition());
    }

    private class ProgressDelegator extends ProgressListenerBase implements IIOReadWarningListener {
        private int index;

        @Override
        public void imageComplete(ImageReader source) {
                processImageComplete();
        }

        @Override
        public void imageProgress(ImageReader source, float percentageDone) {
            processImageProgress(percentageDone);
        }

        @Override
        public void imageStarted(ImageReader source, int imageIndex) {
            processImageStarted(index);
        }

        @Override
        public void readAborted(ImageReader source) {
            processReadAborted();
        }

        @Override
        public void sequenceComplete(ImageReader source) {
            processSequenceComplete();
        }

        @Override
        public void sequenceStarted(ImageReader source, int minIndex) {
            processSequenceStarted(index);
        }

        public void warningOccurred(ImageReader source, String warning) {
            processWarningOccurred(warning);
        }
    }


    public static void main(String[] args) throws IOException {
        DCXImageReader reader = new DCXImageReader(null);

        for (String arg : args) {
            File in = new File(arg);
            reader.setInput(ImageIO.createImageInputStream(in));

            ImageReadParam param = reader.getDefaultReadParam();
            param.setDestinationType(reader.getImageTypes(0).next());
//            param.setSourceSubsampling(2, 3, 0, 0);
//            param.setSourceSubsampling(2, 1, 0, 0);
//
//            int width = reader.getHdpi(0);
//            int height = reader.getVdpi(0);
//
//            param.setSourceRegion(new Rectangle(width / 4, height / 4, width / 2, height / 2));
//            param.setSourceRegion(new Rectangle(width / 2, height / 2));
//            param.setSourceRegion(new Rectangle(width / 2, height / 2, width / 2, height / 2));

            System.err.println("header: " + reader.header);

            BufferedImage image = reader.read(0, param);

            System.err.println("image: " + image);

            showIt(image, in.getName());

            new XMLSerializer(System.out, System.getProperty("file.encoding"))
                    .serialize(reader.getImageMetadata(0).getAsTree(IIOMetadataFormatImpl.standardMetadataFormatName), false);

//            File reference = new File(in.getParent() + "/../reference", in.getName().replaceAll("\\.p(a|b|g|p)m", ".png"));
//            if (reference.exists()) {
//                System.err.println("reference.getAbsolutePath(): " + reference.getAbsolutePath());
//                showIt(ImageIO.read(reference), reference.getName());
//            }

//            break;
        }

    }
}
