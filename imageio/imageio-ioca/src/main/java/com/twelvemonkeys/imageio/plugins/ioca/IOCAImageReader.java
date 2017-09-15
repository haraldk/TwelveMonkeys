package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.ImageReaderBase;

import javax.imageio.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import java.awt.image.*;

import java.io.*;
import java.util.*;

public final class IOCAImageReader extends ImageReaderBase {

	// TODO: support metadata that may be contained in the compressed format e.g. JPEG (use the delegate to get this).
	// TODO: use strategy pattern for delegates when this plugin supports Java 8.

	private List<IOCAImageContent> imageContents;
	private ImageReader delegate;

	IOCAImageReader(final ImageReaderSpi provider) {
		super(provider);
	}

	@Override
	public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
		readStructure();
		checkBounds(imageIndex);

		final ImageReader delegate = getDelegate(imageIndex);

		// Remove all listeners from the delegate before installing our own.
		delegate.removeAllIIOReadProgressListeners();
		delegate.removeAllIIOReadUpdateListeners();
		delegate.removeAllIIOReadWarningListeners();

		// Install listeners that will delegate events back to the parent.
		delegate.addIIOReadProgressListener(new IIOReadProgressListenerDelegate());
		delegate.addIIOReadUpdateListener(new IIOReadUpdateListenerDelegate());
		delegate.addIIOReadWarningListener(new IIOReadWarningListenerDelegate());

		this.delegate = delegate;
		return delegate.read(0, param);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (null != imageContents) {
			for (IOCAImageContent imageContent : imageContents) {
				imageContent.dispose();
			}
		}
	}

	@Override
	public void abort() {
		super.abort();
		if (null != delegate) {
			delegate.abort();
		}
	}

	@Override
	public boolean canReadRaster() {
		return true;
	}

	@Override
	public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
		return read(imageIndex, param).getData();
	}

	@Override
	public ImageTypeSpecifier getRawImageType(final int imageIndex) throws IOException {
		readStructure();
		checkBounds(imageIndex);

		return getDelegate(imageIndex).getRawImageType(0);
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
		readStructure();
		checkBounds(imageIndex);

		return getDelegate(imageIndex).getImageTypes(0);
	}

	@Override
	public int getNumImages(final boolean allowSearch) throws IOException {
		if (!allowSearch && null == imageContents) {
			return -1;
		}

		readStructure();
		return imageContents.size();
	}

	@Override
	public int getHeight(int imageIndex) throws IOException {
		readStructure();
		checkBounds(imageIndex);
		return imageContents.get(imageIndex).getImageSize().getVSize();
	}

	@Override
	public int getWidth(final int imageIndex) throws IOException {
		readStructure();
		checkBounds(imageIndex);
		return imageContents.get(imageIndex).getImageSize().getHSize();
	}

	@Override
	protected void resetMembers() {
		imageContents = null;
	}

	private ImageReader getDelegate(final int imageIndex) throws IIOException {
		final IOCAImageContent imageContent = imageContents.get(imageIndex);
		final ImageReader delegate = imageContent.getImageReader(imageInput);

		if (null == delegate) {
			throw new IIOException(String.format("Unsupported compression 0x%02x.",
					imageContent.getImageEncoding().getCompressionId()));
		}

		return delegate;
	}

	private void readStructure() throws IOException {
		assertInput();

		if (imageContents != null) {
			return;
		}

		imageInput.seek(0);
		imageInput.mark();

		imageContents = new ArrayList<>();

		try {
			final IOCAReader reader = new IOCAReader(imageInput, seekForwardOnly);
			IOCAImageContent imageContent;

			// Collect a sequential, flat list of image contents for easy lookup by index.
			while (null != (imageContent = reader.read())) {
				imageContents.add(imageContent);
			}
		} finally {
			imageInput.reset();
		}
	}

	// To test:
	// $ mvn package -DskipTest
	// $ cd imageio/imageio-ioca/target
	// $ java -cp imageio-ioca-3.4-SNAPSHOT-shaded.jar com.twelvemonkeys.imageio.plugins.ioca.IOCAImageReader \
	//      path/to/file.ica
	public static void main(final String[] args) throws IOException {
		ImageIO.setUseCache(true);

		for (String path : args) {
			final ImageInputStream iis = ImageIO.createImageInputStream(new File(path));
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

			if (!readers.hasNext()) {
				System.err.println("Error: no reader for " + path);
				return;
			}

			final ImageReader reader = readers.next();

			reader.setInput(iis);

			try {
				for (int i = 0; i < reader.getNumImages(true); i++) {
					showIt(reader.read(i), "");
				}
			} finally {
				iis.close();
				reader.dispose();
			}
		}
	}

	private class IIOReadProgressListenerDelegate implements IIOReadProgressListener {

		@Override
		public void sequenceStarted(ImageReader source, int minIndex) {
			processSequenceStarted(minIndex);
		}

		@Override
		public void sequenceComplete(final ImageReader source) {
			processSequenceComplete();
		}

		@Override
		public void imageStarted(final ImageReader source, final int imageIndex) {
			processImageStarted(imageIndex);
		}

		@Override
		public void imageProgress(final ImageReader source, final float percentageDone) {
			processImageProgress(percentageDone);
		}

		@Override
		public void imageComplete(final ImageReader source) {
			processImageComplete();
		}

		@Override
		public void thumbnailStarted(final ImageReader source, final int imageIndex, final int thumbnailIndex) {
			processThumbnailStarted(imageIndex, thumbnailIndex);
		}

		@Override
		public void thumbnailProgress(final ImageReader source, final float percentageDone) {
			processThumbnailProgress(percentageDone);
		}

		@Override
		public void thumbnailComplete(final ImageReader source) {
			processThumbnailComplete();
		}

		@Override
		public void readAborted(final ImageReader source) {
			processReadAborted();
		}
	}

	private class IIOReadUpdateListenerDelegate implements IIOReadUpdateListener {

		@Override
		public void passStarted(final ImageReader source,
		                        final BufferedImage theImage,
		                        final int pass, final int minPass, final int maxPass,
		                        final int minX, final int minY,
		                        final int periodX, final int periodY,
		                        final int[] bands) {
			processPassStarted(theImage, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
		}

		@Override
		public void imageUpdate(final ImageReader source,
		                        final BufferedImage theImage,
		                        final int minX, int minY,
		                        final int width, int height,
		                        final int periodX, int periodY,
		                        final int[] bands) {
			processImageUpdate(theImage, minX, minY, width, height, periodX, periodY, bands);
		}

		@Override
		public void passComplete(final ImageReader source, final BufferedImage theImage) {
			processPassComplete(theImage);
		}

		@Override
		public void thumbnailPassStarted(final ImageReader source,
		                                 final BufferedImage theThumbnail,
		                                 final int pass,
		                                 final int minPass, final int maxPass,
		                                 final int minX, final int minY,
		                                 final int periodX, final int periodY,
		                                 final int[] bands) {
			processThumbnailPassStarted(theThumbnail, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
		}

		@Override
		public void thumbnailUpdate(final ImageReader source,
		                            final BufferedImage theThumbnail,
		                            final int minX, final int minY,
		                            final int width, final int height,
		                            final int periodX, final int periodY,
		                            final int[] bands) {
			processThumbnailUpdate(theThumbnail, minX, minY, width, height, periodX, periodY, bands);
		}

		@Override
		public void thumbnailPassComplete(final ImageReader source, final BufferedImage theThumbnail) {
			processThumbnailPassComplete(theThumbnail);
		}
	}

	private class IIOReadWarningListenerDelegate implements IIOReadWarningListener {

		@Override
		public void warningOccurred(final ImageReader source, final String warning) {
			processWarningOccurred(warning);
		}
	}
}
