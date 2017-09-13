package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.color.ColorSpaces;
import com.twelvemonkeys.imageio.metadata.ioca.*;
import com.twelvemonkeys.imageio.plugins.tiff.CCITTFaxDecoderStream;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFBaseline;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFExtension;
import com.twelvemonkeys.imageio.stream.SubInputStream;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import java.awt.image.*;

import java.io.*;
import java.util.*;

public final class IOCAImageReader extends ImageReaderBase {

	private List<IOCAImageContent> imageContents;

	private ImageReader delegate;

	IOCAImageReader(final ImageReaderSpi provider) {
		super(provider);
	}

	@Override
	public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
		readStructure();
		checkBounds(imageIndex);

		final short compressionId = imageContents.get(imageIndex).getImageEncoding().getCompressionId();

		switch (compressionId) {
			case IOCA.COMPRID_G3_MH:
				return readFax(imageIndex, param, TIFFBaseline.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE);

			case IOCA.COMPRID_G3_MR:
				return readFax(imageIndex, param, TIFFExtension.COMPRESSION_CCITT_T4);

			case IOCA.COMPRID_IBM_MMR:
			case IOCA.COMPRID_G4_MMR:
				return readFax(imageIndex, param, TIFFExtension.COMPRESSION_CCITT_T6);

			case IOCA.COMPRID_TIFF_2:
			case IOCA.COMPRID_TIFF_LZW:
			case IOCA.COMPRID_TIFF_PB:
				return readDelegated(imageIndex, param, "tiff");

			case IOCA.COMPRID_JPEG:
				return readDelegated(imageIndex, param, "jpeg");

			case IOCA.COMPRID_JBIG2:
				return readDelegated(imageIndex, param, "jbig2");

			default:
				throw new IIOException(String.format("Unknown compression ID: %02x", compressionId));
		}
	}

	private SequenceInputStream getDataSequence(final int imageIndex) {
		final Enumeration<InputStream> imageInputViews = new StreamEnumeration(imageContents.get(imageIndex)
				.getDataRecords());

		// Return a sequence of views onto the data, tied together in a sequence to appear
		// as a single stream.
		return new SequenceInputStream(imageInputViews);
	}

	private ImageReader delegate(final int imageIndex, final String formatName) throws IOException {
		final ImageReader delegate;
		final Iterator<ImageReader> delegates;

		delegates = ImageIO.getImageReadersByFormatName(formatName);

		if (!delegates.hasNext()) {
			throw new IIOException(String.format("Unable to read image of type: %s", formatName));
		}

		delegate = delegates.next();

		delegate.setInput(new MemoryCacheImageInputStream(getDataSequence(imageIndex)));
		return delegate;
	}

	private BufferedImage readDelegated(final int imageIndex, final ImageReadParam param, final String mimeType)
			throws IOException {
		delegate = delegate(imageIndex, mimeType);

		// Copy progress listeners to the delegate.
		for (final IIOReadProgressListener progressListener : progressListeners) {
			delegate.addIIOReadProgressListener(progressListener);
		}

		// Copy update listeners.
		for (final IIOReadUpdateListener updateListener : updateListeners) {
			delegate.addIIOReadUpdateListener(updateListener);
		}

		// Copy warning listeners.
		for (final IIOReadWarningListener warningListener : warningListeners) {
			delegate.addIIOReadWarningListener(warningListener);
		}

		return delegate.read(0, param);
	}

	private BufferedImage readFax(final int imageIndex, final ImageReadParam param, final int type)
			throws IOException {
		final IOCAImageContent imageContent = imageContents.get(imageIndex);

		int fillOrder;

		int width = imageContent.getImageSize().getHSize();
		int height = imageContent.getImageSize().getVSize();

		if (imageContent.getImageEncoding().getBitOrder() == IOCA.BITORDR_LTR) {
			fillOrder = TIFFBaseline.FILL_LEFT_TO_RIGHT;
		} else {
			fillOrder = TIFFExtension.FILL_RIGHT_TO_LEFT;
		}

		final CCITTFaxDecoderStream decoder = new CCITTFaxDecoderStream(getDataSequence(imageIndex),
				width, type, fillOrder, 0L);

		final BufferedImage destination = getDestination(param, getImageTypes(imageIndex), width, height);
		final DataBuffer raster = destination.getRaster().getDataBuffer();

		processImageStarted(imageIndex);

		for (int b, i = 0, l = raster.getSize(); i < l; i++) {
			b = decoder.read();
			if (b < 0) {
				throw new EOFException();
			}

			if (abortRequested()) {
				break;
			}

			// Invert the colour.
			// For some reason the decoder uses an inverted mapping.
			raster.setElem(i, ((~b) & 0x00FF));
		}

		if (abortRequested()) {
			processReadAborted();
		} else {
			processImageProgress(100F);
		}

		processImageComplete();
		return destination;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (null != delegate) {
			delegate.dispose();
			delegate = null;
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

		final IOCAImageEncoding imageEncoding = imageContents.get(imageIndex).getImageEncoding();

		switch (imageEncoding.getCompressionId()) {
			case IOCA.COMPRID_G3_MH:
			case IOCA.COMPRID_G3_MR:
			case IOCA.COMPRID_IBM_MMR:
			case IOCA.COMPRID_G4_MMR:
			case IOCA.COMPRID_JBIG2:
			case IOCA.COMPRID_ABIC_Q:
				return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY);

			case IOCA.COMPRID_ABIC_C:
				return ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY);

			case IOCA.COMPRID_TIFF_2:
			case IOCA.COMPRID_TIFF_LZW:
			case IOCA.COMPRID_TIFF_PB:
				delegate(imageIndex, "tiff").getRawImageType(0);

			case IOCA.COMPRID_JPEG:
				delegate(imageIndex, "jpeg").getRawImageType(0);
		}

		// For all other types we can't give a response. Return null.
		return null;
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(final int imageIndex) throws IOException {
		readStructure();
		checkBounds(imageIndex);

		final List<ImageTypeSpecifier> types = new ArrayList<>();
		final IOCAImageContent imageContent = imageContents.get(imageIndex);

		final IOCAIdeStructure ideStructure = imageContent.getIdeStructure();
		final short compressionId = imageContent.getImageEncoding().getCompressionId();

		// TIFFs, JPEGs and RL4 images may be RGB.
		if (IOCA.COMPRID_JPEG == compressionId
				|| IOCA.COMPRID_TIFF_2 == compressionId
				|| IOCA.COMPRID_TIFF_LZW == compressionId
				|| IOCA.COMPRID_TIFF_PB == compressionId
				|| IOCA.COMPRID_RL4 == compressionId
				|| (null != ideStructure && ideStructure.getFormat() == IOCA.FORMAT_RGB)) {
			types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
		}

		// JPEGs and any images that specify CMYK as a colour space may be CMYK.
		if (IOCA.COMPRID_JPEG == compressionId
				|| (null != ideStructure && ideStructure.getFormat() == IOCA.FORMAT_CMYK)) {
			types.add(ImageTypeSpecifiers.createInterleaved(ColorSpaces.getColorSpace(ColorSpaces.CS_GENERIC_CMYK),
					new int[] {3, 2, 1, 0}, DataBuffer.TYPE_BYTE, false, false));
		}

		// When no IDE structure is present, any IDE size > 0 implies grayscale.
		// Otherwise bi-level is the default for everything except JPEG.
		// For details see pp. 137: Appendix B. Bilevel, Grayscale, and Color Images.
		if (imageContent.getIdeSize() > IOCA.IDESZ_BILEVEL) {
			types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_GRAY));
		} else if (compressionId != IOCA.COMPRID_JPEG) {
			types.add(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_BYTE_BINARY));
		}

		return types.iterator();
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

	private void readStructure() throws IOException {
		assertInput();

		if (imageContents != null) {
			return;
		}

		imageInput.seek(0);
		imageInput.mark();

		try {
			imageContents = new IOCAParser(imageInput).parse();
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

	private final class StreamEnumeration implements Enumeration<InputStream> {

		private Iterator<IOCAImageContent.DataRecord> dataRecords;

		StreamEnumeration(final List<IOCAImageContent.DataRecord> dataRecords) {
			this.dataRecords = dataRecords.iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return dataRecords.hasNext();
		}

		@Override
		public InputStream nextElement() {
			final IOCAImageContent.DataRecord dataRecord = dataRecords.next();

			try {
				if (imageInput.isCached()) {
					imageInput.reset();
					imageInput.mark();
					imageInput.seek(dataRecord.getOffset());

					return new SubInputStream(imageInput, dataRecord.getLength());
				}

				final byte[] buffer = dataRecord.getBuffer();

				if (null == buffer) {
					throw new IllegalStateException("Expected buffer for non-cached stream but found null.");
				}

				return new ByteArrayInputStream(buffer);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
