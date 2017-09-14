package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.metadata.ioca.IOCA;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFBaseline;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFExtension;
import com.twelvemonkeys.io.SubStream;
import com.twelvemonkeys.util.Function;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.*;

final class IOCAImageContent {

	private static final Map<Short, Function<IOCAImageContent, ImageReader>> imageReaderFactory = new HashMap<>();

	private final List<DataRecord> dataRecords = new LinkedList<>();

	private IOCASegment segment;
	private IOCAImageSize imageSize;
	private IOCAImageEncoding imageEncoding;
	private IOCAIdeStructure ideStructure;

	private ImageReader imageReader;

	private short ideSize = IOCA.IDESZ_BILEVEL; // The default is 1 (bilevel image).

	IOCAImageSize getImageSize() {
		return imageSize;
	}

	void setImageSize(final IOCAImageSize imageSize) {
		this.imageSize = imageSize;
	}

	IOCAImageEncoding getImageEncoding() {
		return imageEncoding;
	}

	void setImageEncoding(final IOCAImageEncoding imageEncoding) {
		this.imageEncoding = imageEncoding;
	}

	short getIdeSize() {
		return ideSize;
	}

	void setIdeSize(final short ideSize) {
		if (ideSize >= IOCA.IDESZ_BILEVEL && ideSize < 0xFF) {
			this.ideSize = ideSize;
		} else {
			throw new IllegalArgumentException("EC-0004: invalid parameter value.");
		}
	}

	IOCAIdeStructure getIdeStructure() {
		return ideStructure;
	}

	void setIdeStructure(final IOCAIdeStructure ideStructure) {
		this.ideStructure = ideStructure;
	}

	IOCASegment getSegment() {
		return segment;
	}

	void setSegment(final IOCASegment segment) {
		this.segment = segment;
	}

	private int getWidth() {
		return imageSize.getHSize();
	}

	private int getHeight() {
		return imageSize.getVSize();
	}

	private short getBitOrder() {
		return this.getImageEncoding().getBitOrder();
	}

	void recordData(final long offset, final int length) {
		dataRecords.add(new DataRecord(offset, length));
	}

	void recordData(final byte[] buffer) {
		dataRecords.add(new DataRecord(buffer));
	}

	ImageReader getImageReader(final ImageInputStream imageInput) {
		final Function<IOCAImageContent, ImageReader> factory;

		if (null != imageReader) {
			return imageReader;
		}

		factory = imageReaderFactory.get(getImageEncoding().getCompressionId());
		if (null == factory) {
			return null;
		}

		imageReader = factory.apply(this);

		final InputStream input = getInputStream(imageInput);
		boolean acceptsInputStream = false;

		// Check whether the reader accepts a raw InputStream or whether it needs to be wrapped.
		for (Class<?> c: imageReader.getOriginatingProvider().getInputTypes()) {
			if (c.isAssignableFrom(input.getClass())) {
				acceptsInputStream = true;
			}
		}

		if (acceptsInputStream) {
			imageReader.setInput(input);
		} else {
			imageReader.setInput(new MemoryCacheImageInputStream(input));
		}

		return imageReader;
	}

	void dispose() {
		if (null != imageReader) {
			imageReader.dispose();
		}
	}

	private InputStream getInputStream(final ImageInputStream imageInput) {
		return new SequenceInputStream(new StreamEnumeration(dataRecords, imageInput));
	}

	// Use the strategy pattern to avoid a big switch statement.
	static {

		// Register factories for CCITT fax formats.
		imageReaderFactory.put(IOCA.COMPRID_G4_MMR, new CCITFaxFactory(TIFFExtension.COMPRESSION_CCITT_T4));
		imageReaderFactory.put(IOCA.COMPRID_G3_MR, new CCITFaxFactory(TIFFExtension.COMPRESSION_CCITT_T4));
		imageReaderFactory.put(IOCA.COMPRID_G3_MH, new CCITFaxFactory(TIFFBaseline
				.COMPRESSION_CCITT_MODIFIED_HUFFMAN_RLE));

		// The one and only JPEG format.
		imageReaderFactory.put(IOCA.COMPRID_JPEG, new ImageReaderFactory("JPEG"));

		// Various TIFF formats.
		imageReaderFactory.put(IOCA.COMPRID_TIFF_2, new ImageReaderFactory("TIFF"));
		imageReaderFactory.put(IOCA.COMPRID_TIFF_LZW, new ImageReaderFactory("TIFF"));
		imageReaderFactory.put(IOCA.COMPRID_TIFF_PB, new ImageReaderFactory("TIFF"));

		// JBIG2 - no TwelveMonkeys plugin but at least one free, open source exists.
		imageReaderFactory.put(IOCA.COMPRID_JBIG2, new ImageReaderFactory("JBIG2"));
	}

	private static class ImageReaderFactory implements Function<IOCAImageContent, ImageReader> {

		private final String formatName;

		private ImageReaderFactory(final String formatName) {
			this.formatName = formatName;
		}

		@Override
		public ImageReader apply(final IOCAImageContent imageContent) {
			final Iterator<ImageReader> delegates;

			delegates = ImageIO.getImageReadersByFormatName(formatName);
			if (!delegates.hasNext()) {
				return null;
			}

			return delegates.next();
		}
	}

	private static class CCITFaxFactory implements Function<IOCAImageContent, ImageReader> {

		private final int type;

		private CCITFaxFactory(final int type) {
			this.type = type;
		}

		@Override
		public ImageReader apply(final IOCAImageContent imageContent) {
			return new CCITTFaxImageReader(imageContent.getWidth(), imageContent.getHeight(),
					imageContent.getBitOrder(), type);
		}
	}

	private static class DataRecord {

		private final byte[] buffer;
		private final long offset, length;

		DataRecord(final long offset, final long length) {
			this.offset = offset;
			this.length = length;
			buffer = null;
		}

		DataRecord(final byte[] buffer) {
			this.buffer = buffer;
			offset = length = 0;
		}
	}

	private static class StreamEnumeration implements Enumeration<InputStream> {

		private final ImageInputStream imageInput;
		private final Iterator<DataRecord> dataRecordIterator;

		private StreamEnumeration(final List<DataRecord> dataRecords, final ImageInputStream imageInput) {
			dataRecordIterator = dataRecords.iterator();
			this.imageInput = imageInput;
		}

		@Override
		public boolean hasMoreElements() {
			return dataRecordIterator.hasNext();
		}

		@Override
		public InputStream nextElement() {
			final DataRecord dataRecord = dataRecordIterator.next();

			if (null != dataRecord.buffer) {
				return new ByteArrayInputStream(dataRecord.buffer);
			}

			try {
				imageInput.reset();
				imageInput.mark();
				imageInput.seek(dataRecord.offset);

				return new SubStream(new ProxyStream(imageInput), dataRecord.length);
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class ProxyStream extends InputStream {

		private final ImageInputStream imageInput;

		private ProxyStream(final ImageInputStream imageInput) {
			this.imageInput = imageInput;
		}

		@Override
		public int read() throws IOException {
			return imageInput.read();
		}
	}
}
