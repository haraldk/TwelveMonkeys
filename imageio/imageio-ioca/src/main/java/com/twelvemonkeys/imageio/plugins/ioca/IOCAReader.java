package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.metadata.ioca.IOCA;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

import java.io.EOFException;
import java.io.IOException;

final class IOCAReader {

	private final static boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.ioca.debug"));

	private final ImageInputStream imageInput;
	private final boolean seekForwardOnly;

	private IOCASegment segment;
	private IOCAImageContent imageContent;

	IOCAReader(final ImageInputStream imageInput, final boolean seekForwardOnly) {
		this.imageInput = imageInput;
		this.seekForwardOnly = seekForwardOnly;
	}

	IOCAImageContent read() throws IOException {
		long offset = imageInput.getStreamPosition();
		IOCAImageContent imageContent = null;
		int code;

		while (-1 != (code = imageInput.read())) {
			if (IOCA.EXTENDED_CODE_POINT == code) {
				readExtended(offset, imageInput.read());
			} else {
				imageContent = readLong(offset, code);
			}

			if (null != imageContent) {
				break;
			}

			// Keep track of the current offset for debugging/error messages.
			offset = imageInput.getStreamPosition();
		}

		return imageContent;
	}

	private IOCAImageContent readLong(final long offset, final int code) throws IOException {
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
				return endImageContent();

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

			case IOCA.CODE_POINT_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER:
				externalAlgorithmSpecification(length);
				break;

			case IOCA.CODE_POINT_BEGIN_TILE:
			case IOCA.CODE_POINT_END_TILE:
			case IOCA.CODE_POINT_IMAGE_LUT_ID_PARAMETER:
			case IOCA.CODE_POINT_BAND_IMAGE_PARAMETER:
			case IOCA.CODE_POINT_TILE_POSITION:
			case IOCA.CODE_POINT_TILE_SIZE:
			case IOCA.CODE_POINT_TILE_SET_COLOR:
			case IOCA.CODE_POINT_BEGIN_TRANSPARENCY_MASK:
			case IOCA.CODE_POINT_END_TRANSPARENCY_MASK:
				throw new IIOException(String.format("Unsupported code point: 0x%02x at offset %d",
						code, offset));

			default:
				throw new IIOException(String.format("Unknown code point: 0x%02x at offset %d",
						code, offset));
		}

		return null;
	}

	private void readExtended(final long offset, final int code) throws IOException {
		final int length = imageInput.readUnsignedShort();

		switch (code) {
			case IOCA.CODE_POINT_IMAGE_DATA:
				imageData(length);
				break;

			case IOCA.CODE_POINT_BAND_IMAGE_DATA:
			case IOCA.CODE_POINT_INCLUDE_TILE:
			case IOCA.CODE_POINT_TILE_TOC:
			case IOCA.CODE_POINT_IMAGE_SUBSAMPLING_PARAMETER:
				throw new IIOException(String.format("Unsupported extended code point: 0x%02x at offset %d",
						code, offset));

			default:
				throw new IIOException(String.format("Unknown extended code point: 0x%02x at offset %d",
						code, offset));
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

	private IOCAImageContent endImageContent() throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: end image content");
		}

		if (null == segment || null == imageContent) {
			throw new IIOException("EC-910F: invalid sequence.");
		}

		final IOCAImageContent imageContent = this.imageContent;

		this.imageContent = null;
		return imageContent;
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

		if (DEBUG) {
			System.err.println(String.format("IOCA: hsize set to %d", imageSize.getHSize()));
			System.err.println(String.format("IOCA: vsize set to %d", imageSize.getVSize()));
		}

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

		if (DEBUG) {
			System.err.println(String.format("IOCA: compression ID set to 0x%02x", imageEncoding.getCompressionId()));
			System.err.println(String.format("IOCA: recording ID set to 0x%02x", imageEncoding.getRecordingId()));
			System.err.println(String.format("IOCA: bit order set to 0x%02x", imageEncoding.getBitOrder()));
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
			System.err.println(String.format("IOCA: IDE size set to 0x%02x", imageContent.getIdeSize()));
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

		if (DEBUG) {
			System.err.println(String.format("IOCA: IDE format set to 0x%02x", ideStructure.getFormat()));
		}

		// Next three bytes should be 0x000000.
		if (0x0000 != imageInput.readShort() || 0x00 != imageInput.readByte()) {
			throw new IIOException("EC-9B10: invalid or unsupported IDE Structure parameter value");
		}

		ideStructure.setSize1(imageInput.readByte());
		if (DEBUG) {
			System.err.println(String.format("IOCA: size 1 set to 0x%02x", ideStructure.getSize1()));
		}

		if (length > 0x06) {
			ideStructure.setSize2(imageInput.readByte());
			if (DEBUG) {
				System.err.println(String.format("IOCA: size 2 set to 0x%02x", ideStructure.getSize1()));
			}
		}

		if (length > 0x07) {
			ideStructure.setSize3(imageInput.readByte());
			if (DEBUG) {
				System.err.println(String.format("IOCA: size 3 set to 0x%02x", ideStructure.getSize1()));
			}
		}

		if (length > 0x08) {
			if (ideStructure.getFormat() != IOCA.FORMAT_CMYK) {
				throw new IIOException("EC-9B18: SIZE4 is present and the colour space is not CMYK.");
			}

			ideStructure.setSize4(imageInput.readByte());
			if (DEBUG) {
				System.err.println(String.format("IOCA: size 4 set to 0x%02x", ideStructure.getSize1()));
			}
		} else if (ideStructure.getFormat() == IOCA.FORMAT_CMYK) {
			throw new IIOException("EC-9B18: SIZE4 is not present and the colour space is not CMYK.");
		}

		imageContent.setIdeStructure(ideStructure);
	}

	private void externalAlgorithmSpecification(final int length) throws IOException {
		if (DEBUG) {
			System.err.println("IOCA: external algorithm specification parameter");
		}

		if (length < 0x03 || length > 0xFF) {
			throw new IIOException("EC-0003: invalid length.");
		}

		// TODO: support the specification.
		// For now we just skip it.
		skip(length);
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
		if (!seekForwardOnly && imageInput.isCached()) {
			final long position = imageInput.getStreamPosition();

			skip(length);
			imageContent.recordData(position, length);
		} else {
			final byte[] buffer = new byte[length];

			imageInput.readFully(buffer, 0, length);
			imageContent.recordData(buffer);
		}
	}

	private void skip(final int length) throws IOException {
		final int skipped = imageInput.skipBytes(length);

		if (length > skipped) {
			throw new EOFException(String.format("Only skipped %d bytes of %d requested.", skipped, length));
		}
	}
}
