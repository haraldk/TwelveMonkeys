package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.metadata.ioca.IOCA;

import javax.imageio.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import java.awt.color.ColorSpace;
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

		// Copy progress listeners to the delegate.
		// TODO: in Java 8 this could be reduced to one line of code.
		if (null != progressListeners) {
			delegate.removeAllIIOReadProgressListeners();
			for (final IIOReadProgressListener progressListener : progressListeners) {
				delegate.addIIOReadProgressListener(new IIOReadProgressListenerProxy(this, progressListener));
			}
		}

		// Copy update listeners.
		if (null != updateListeners) {
			delegate.removeAllIIOReadProgressListeners();
			for (final IIOReadUpdateListener updateListener : updateListeners) {
				delegate.addIIOReadUpdateListener(new IIOReadUpdateListenerProxy(this, updateListener));
			}
		}

		// Copy warning listeners.
		if (null != warningListeners) {
			delegate.removeAllIIOReadProgressListeners();
			for (final IIOReadWarningListener warningListener : warningListeners) {
				delegate.addIIOReadWarningListener(new IIOReadWarningListenerProxy(this, warningListener));
			}
		}

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

		final ImageTypeSpecifier typeSpecifier = getDelegate(imageIndex).getRawImageType(0);

		if (null != typeSpecifier) {
			return typeSpecifier;
		}

		// Try and get the type from the IDE structure parameters.
		// This works around a problem with the JPEGImageReader, which doesn't return a specifier for images
		// with no embedded metadata.
		return getImageTypeSpecifierFromIdeStructure(imageIndex);
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
		readStructure();
		checkBounds(imageIndex);

		final Iterator<ImageTypeSpecifier> specifierIterator = getDelegate(imageIndex).getImageTypes(0);
		final ImageTypeSpecifier specifierFromStructure = getImageTypeSpecifierFromIdeStructure(imageIndex);

		// Some IOCA images have no IDE structure parameter.
		// Nothing else can be done.
		if (null == specifierFromStructure) {
			return specifierIterator;
		}

		// Use the type specifier implied by the structure if the delegate is unable to read one.
		if (null == specifierIterator || !specifierIterator.hasNext()) {
			return Collections.singletonList(specifierFromStructure).iterator();
		}

		// If the delegate produced a list of type specifiers, it may not necessarily include the one from the
		// structure. Add that to the iterator.
		final List<ImageTypeSpecifier> typeSpecifiers = new ArrayList<>();

		// Add the implied type first.
		typeSpecifiers.add(specifierFromStructure);

		// Add all other suggested ones afterwards.
		while (specifierIterator.hasNext()) {
			typeSpecifiers.add(specifierIterator.next());
		}

		return typeSpecifiers.iterator();
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

	private ImageTypeSpecifier getImageTypeSpecifierFromIdeStructure(final int imageIndex) {
		final IOCAIdeStructure ideStructure = imageContents.get(imageIndex).getIdeStructure();

		if (null == ideStructure) {
			return null;
		}

		switch (ideStructure.getFormat()) {
			case IOCA.FORMAT_RGB:

				// 8-bit RGB (all channels packed into one byte).
				if (ideStructure.is8Bit()) {
					return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
				}

				// 24-bit RGB (each channel in a separate byte).
				if (ideStructure.is16Bit()) {
					return ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.CS_sRGB),
							new int[] { 0, 1, 2 }, DataBuffer.TYPE_BYTE, false, false);
				}

				break;

			case IOCA.FORMAT_CMYK:

				// Not sure about the band offsets here. Need a CMYK image to test.
				return ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.TYPE_CMYK),
						new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false);

			case IOCA.FORMAT_YCBCR:
			case IOCA.FORMAT_YCRCB:

				// Ibid. Need an image to test.
				return ImageTypeSpecifier.createInterleaved(ColorSpace.getInstance(ColorSpace.TYPE_YCbCr),
						new int[] { 0, 1, 2 }, DataBuffer.TYPE_BYTE, false, false);
		}

		// Give up.
		return null;
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

	private static class IIOReadProgressListenerProxy implements IIOReadProgressListener {

		private final IIOReadProgressListener target;
		private final ImageReader source;

		private IIOReadProgressListenerProxy(final ImageReader source, final IIOReadProgressListener target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public void sequenceStarted(ImageReader source, int minIndex) {
			target.sequenceStarted(this.source, minIndex);
		}

		@Override
		public void sequenceComplete(final ImageReader source) {
			target.sequenceComplete(this.source);
		}

		@Override
		public void imageStarted(final ImageReader source, final int imageIndex) {
			target.imageStarted(this.source, imageIndex);
		}

		@Override
		public void imageProgress(final ImageReader source, final float percentageDone) {
			target.imageProgress(this.source, percentageDone);
		}

		@Override
		public void imageComplete(final ImageReader source) {
			target.imageComplete(this.source);
		}

		@Override
		public void thumbnailStarted(final ImageReader source, final int imageIndex, final int thumbnailIndex) {
			target.thumbnailStarted(this.source, imageIndex, thumbnailIndex);
		}

		@Override
		public void thumbnailProgress(final ImageReader source, final float percentageDone) {
			target.thumbnailProgress(this.source, percentageDone);
		}

		@Override
		public void thumbnailComplete(final ImageReader source) {
			target.thumbnailComplete(this.source);
		}

		@Override
		public void readAborted(final ImageReader source) {
			target.readAborted(this.source);
		}
	}

	private static class IIOReadUpdateListenerProxy implements IIOReadUpdateListener {

		private final IIOReadUpdateListener target;
		private final ImageReader source;

		private IIOReadUpdateListenerProxy(final ImageReader source, final IIOReadUpdateListener target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public void passStarted(final ImageReader source,
		                        final BufferedImage theImage,
		                        final int pass, final int minPass, final int maxPass,
		                        final int minX, final int minY,
		                        final int periodX, final int periodY,
		                        final int[] bands) {
			target.passStarted(this.source, theImage, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
		}

		@Override
		public void imageUpdate(final ImageReader source,
		                        final BufferedImage theImage,
		                        final int minX, int minY,
		                        final int width, int height,
		                        final int periodX, int periodY,
		                        final int[] bands) {
			target.imageUpdate(this.source, theImage, minX, minY, width, height, periodX, periodY, bands);
		}

		@Override
		public void passComplete(final ImageReader source, final BufferedImage theImage) {
			target.passComplete(this.source, theImage);
		}

		@Override
		public void thumbnailPassStarted(final ImageReader source,
		                                 final BufferedImage theThumbnail,
		                                 final int pass,
		                                 final int minPass, final int maxPass,
		                                 final int minX, final int minY,
		                                 final int periodX, final int periodY,
		                                 final int[] bands) {
			target.thumbnailPassStarted(this.source,
					theThumbnail, pass, minPass, maxPass, minX, minY, periodX, periodY, bands);
		}

		@Override
		public void thumbnailUpdate(final ImageReader source,
		                            final BufferedImage theThumbnail,
		                            final int minX, final int minY,
		                            final int width, final int height,
		                            final int periodX, final int periodY,
		                            final int[] bands) {
			target.thumbnailUpdate(this.source, theThumbnail, minX, minY, width, height, periodX, periodY, bands);
		}

		@Override
		public void thumbnailPassComplete(final ImageReader source, final BufferedImage theThumbnail) {
			target.thumbnailPassComplete(this.source, theThumbnail);
		}
	}

	private static class IIOReadWarningListenerProxy implements IIOReadWarningListener {

		private final IIOReadWarningListener target;
		private final ImageReader source;

		private IIOReadWarningListenerProxy(final ImageReader source, final IIOReadWarningListener target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public void warningOccurred(final ImageReader source, final String warning) {
			target.warningOccurred(this.source, warning);
		}
	}
}
