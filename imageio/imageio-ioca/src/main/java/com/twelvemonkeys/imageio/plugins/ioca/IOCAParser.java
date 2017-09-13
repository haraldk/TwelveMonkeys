package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.metadata.ioca.IOCA;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

import java.io.EOFException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class IOCAParser {

	private final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.ioca.debug"));

	private final ImageInputStream imageInput;

	private IOCASegment segment;
	private IOCAImageContent imageContent;

	private List<IOCAImageContent> imageContents;

	IOCAParser(final ImageInputStream imageInput) {
		this.imageInput = imageInput;
	}

	List<IOCAImageContent> parse() throws IOException {
		imageContents = new ArrayList<>();

		try {
			int code;

			while (-1 != (code = imageInput.read())) {
				if (IOCA.EXTENDED_CODE_POINT == code) {
					readExtended(imageInput.read());
				} else {
					readLong(code);
				}
			}

			// Return a sequential, flat list of image contents for easy lookup by index.
			return Collections.unmodifiableList(imageContents);
		} finally {
			imageContent = null;
			imageContents = null;
		}
	}

	private void readLong(final int code) throws IOException {
		final int length = imageInput.read();

		switch (code) {
			case IOCA.CODE_POINT_BEGIN_SEGMENT:
				beginSegment(length);
				break;

			case IOCA.CODE_POINT_END_SEGMENT:
				endSegment();
				break;

			case IOCA.CODE_POINT_BEGIN_IMAGE_CONTENT:
				beginImageContent(length);
				break;

			case IOCA.CODE_POINT_END_IMAGE_CONTENT:
				endImageContent();
				break;

			case IOCA.CODE_POINT_IMAGE_SIZE_PARAMETER:
				imageSize(length);
				break;

			case IOCA.CODE_POINT_IMAGE_ENCODING_PARAMETER:
				imageEncoding(length);
				break;

			case IOCA.CODE_POINT_IDE_SIZE_PARAMETER:
				ideSize(length);
				break;

			case IOCA.CODE_POINT_IDE_STRUCTURE_PARAMETER:
				ideStructure(length);
				break;

			case IOCA.CODE_POINT_BEGIN_TILE:
			case IOCA.CODE_POINT_END_TILE:
			case IOCA.CODE_POINT_IMAGE_LUT_ID_PARAMETER:
			case IOCA.CODE_POINT_BAND_IMAGE_PARAMETER:
			case IOCA.CODE_POINT_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER:
			case IOCA.CODE_POINT_TILE_POSITION:
			case IOCA.CODE_POINT_TILE_SIZE:
			case IOCA.CODE_POINT_TILE_SET_COLOR:
			case IOCA.CODE_POINT_BEGIN_TRANSPARENCY_MASK:
			case IOCA.CODE_POINT_END_TRANSPARENCY_MASK:
				throw new IIOException("Not supported.");

			default:
				throw new IIOException(String.format("Unknown code point: %02x", code));
		}
	}

	private void readExtended(final int code) throws IOException {
		final int length = imageInput.readUnsignedShort();

		switch (code) {
			case IOCA.CODE_POINT_IMAGE_DATA:
				imageData(length);
				break;

			case IOCA.CODE_POINT_BAND_IMAGE_DATA:
			case IOCA.CODE_POINT_INCLUDE_TILE:
			case IOCA.CODE_POINT_TILE_TOC:
			case IOCA.CODE_POINT_IMAGE_SUBSAMPLING_PARAMETER:
				throw new IIOException("Not supported.");

			default:
				throw new IIOException(String.format("Unknown code point: %02x", code));
		}
	}

	private void beginSegment(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: begin segment");
		}

		if (length > 0x04) {
			throw new IIOException("EC-0003: invalid length.");
		}

		segment = new IOCASegment();

		if (0x01 == length) {
			segment.setName(imageInput.readUnsignedByte());
		} else if (0x02 == length) {
			segment.setName(imageInput.readUnsignedShort());
		} else if (0x04 == length) {
			segment.setName(imageInput.readUnsignedInt());
		}
	}

	private void endSegment() throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: end segment");
		}

		if (null == segment) {
			throw new IIOException("EC-710F: invalid sequence.");
		}

		segment = null;
	}

	private void beginImageContent(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: begin image content");
		}

		if (0x01 != length) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (0xFF != imageInput.readUnsignedByte()) {
			throw new IIOException("EC-0004: invalid parameter value.");
		}

		if (null == segment || null != imageContent) {
			throw new IIOException("EC-910F: invalid sequence.");
		}

		imageContent = new IOCAImageContent();
		imageContent.setSegment(segment);
	}

	private void endImageContent() throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: end image content");
		}

		if (null == segment || null == imageContent) {
			throw new IIOException("EC-910F: invalid sequence.");
		}

		imageContents.add(imageContent);
		imageContent = null;
	}

	private void imageSize(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: image size");
		}

		if (0x09 != length) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-940F: invalid sequence.");
		}

		final IOCAImageSize imageSize = new IOCAImageSize();

		imageSize.setUnitBase((short) imageInput.readUnsignedByte());

		imageSize.setHResolution(imageInput.readUnsignedShort());
		imageSize.setVResolution(imageInput.readUnsignedShort());

		imageSize.setHSize(imageInput.readUnsignedShort());
		imageSize.setVSize(imageInput.readUnsignedShort());

		imageContent.setImageSize(imageSize);
	}

	private void imageEncoding(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: image encoding");
		}

		if (length < 0x02 || length > 0x03) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-950F: invalid sequence.");
		}

		final IOCAImageEncoding imageEncoding = new IOCAImageEncoding();

		imageEncoding.setCompressionId((short) imageInput.readUnsignedByte());
		imageEncoding.setRecordingId((short) imageInput.readUnsignedByte());

		if (0x03 == length) {
			imageEncoding.setBitOrder((short) imageInput.readUnsignedByte());
		}

		imageContent.setImageEncoding(imageEncoding);
	}

	private void ideSize(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: IDE size");
		}

		if (length != 0x01) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-960F: invalid sequence.");
		}

		imageContent.setIdeSize((short) imageInput.readUnsignedByte());

		if (DEBUG) {
			System.err.println(String.format("IOCA: IDE size set to %02x", imageContent.getIdeSize()));
		}
	}

	private void ideStructure(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: IDE structure");
		}

		if (length < 0x06 || length > 0x09) {
			throw new IIOException("EC-0005: invalid length");
		}

		if (null == imageContent) {
			throw new IIOException("EC-9B0F: invalid sequence.");
		}

		final IOCAIdeStructure ideStructure = new IOCAIdeStructure();

		ideStructure.setFlags(imageInput.readByte());
		ideStructure.setFormat(imageInput.readByte());

		if (0x0000 != imageInput.readShort()) {
			throw new IIOException("EC-9B10: invalid or unsupported IDE Structure parameter value");
		}

		ideStructure.setSize1(imageInput.readByte());

		if (length > 0x06) {
			ideStructure.setSize2(imageInput.readByte());
		}

		if (length > 0x07) {
			ideStructure.setSize3(imageInput.readByte());
		}

		if (length > 0x08) {
			ideStructure.setSize4(imageInput.readByte());
		}

		imageContent.setIdeStructure(ideStructure);
	}

	private void imageData(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: image data");
		}

		if (length < 0x0001 || length > 0xFFFF) {
			throw new IIOException("EC-0003: invalid length.");
		}

		if (null == imageContent) {
			throw new IIOException("EC-920F: invalid sequence.");
		}

		// For cached streams, record only a view onto the data. There's no need to buffer input that can
		// be referenced later.
		if (imageInput.isCached()) {
			final long position = imageInput.getStreamPosition();
			final int skipped = imageInput.skipBytes(length);

			if (length > skipped) {
				throw new EOFException(String.format("Only skipped %d bytes of %d requested.", skipped, length));
			}

			imageContent.recordData(position, length);
		} else {
			final byte[] buffer = new byte[length];

			imageInput.readFully(buffer, 0, length);
			imageContent.recordData(buffer);
		}
	}
}
