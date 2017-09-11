package com.twelvemonkeys.imageio.metadata.ioca;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.MetadataReader;
import com.twelvemonkeys.lang.Validate;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public final class IOCAReader extends MetadataReader {

	private final LinkedList<SFD> definitions = new LinkedList<>();
	private final List<IOCAEntry> entries = new LinkedList<>();

	@Override
	public Directory read(final ImageInputStream input) throws IOException {
		Validate.notNull(input, "input");

		int code;

		while (-1 != (code = input.read())) {
			if (IOCA.EXTENDED_CODE_POINT == code) {
				readExtended(((code & 0xff) << 8) | (input.readByte() & 0xff), input);
			} else {
				readLong((byte) code, input);
			}

			entries.clear();
		}

		final Directory directory = new IOCADirectory(definitions);

		definitions.clear();
		return directory;
	}

	private void readLong(final byte code, final ImageInputStream input) throws IOException {
		final byte length = input.readByte();

		switch (code) {
			case IOCA.CODE_POINT_BEGIN_SEGMENT:
				beginSegment(length, input);
				break;

			case IOCA.CODE_POINT_END_SEGMENT:
				end(length, IOCA.CODE_END_SEGMENT);
				break;

			case IOCA.CODE_POINT_BEGIN_TILE:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_END_TILE:
				end(length, IOCA.CODE_END_TILE);
				break;

			case IOCA.CODE_POINT_BEGIN_TRANSPARENCY_MASK:
				break;

			case IOCA.CODE_POINT_END_TRANSPARENCY_MASK:
				end(length, IOCA.CODE_END_TRANSPARENCY_MASK);
				break;

			case IOCA.CODE_POINT_BEGIN_IMAGE_CONTENT:
				beginImageContent(length, input);
				break;

			case IOCA.CODE_POINT_END_IMAGE_CONTENT:
				end(length, IOCA.CODE_END_IMAGE_CONTENT);
				break;

			case IOCA.CODE_POINT_IMAGE_SIZE_PARAMETER:
				imageSize(length, input);
				break;

			case IOCA.CODE_POINT_IMAGE_ENCODING_PARAMETER:
				imageEncoding(length, input);
				break;

			case IOCA.CODE_POINT_IDE_SIZE_PARAMETER:
				ideSize(length, input);
				break;

			case IOCA.CODE_POINT_IMAGE_LUT_ID_PARAMETER:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_BAND_IMAGE_PARAMETER:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_IDE_STRUCTURE_PARAMETER:
				ideStructure(length, input);
				break;

			case IOCA.CODE_POINT_EXTERNAL_ALGORITHM_SPECIFICATION_PARAMETER:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_TILE_POSITION:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_TILE_SIZE:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_TILE_SET_COLOR:
				throw new IOException("Not supported.");

			default:
				throw new IOException(String.format("Unknown code point: %d", code));
		}
	}

	private void readExtended(final int code, final ImageInputStream input) throws IOException {
		int length = input.readUnsignedShort();

		switch (code) {
			case IOCA.CODE_POINT_IMAGE_DATA:
				imageData(length, input);
				break;

			case IOCA.CODE_POINT_BAND_IMAGE_DATA:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_INCLUDE_TILE:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_TILE_TOC:
				throw new IOException("Not supported.");

			case IOCA.CODE_POINT_IMAGE_SUBSAMPLING_PARAMETER:
				throw new IOException("Not supported.");
		}
	}


	private void beginSegment(final byte length, final ImageInputStream input) throws IOException {
		final Object value;

		if (0x00 == length) {
			value = null;
		} else if (0x01 == length) {
			value = input.readByte();
		} else if (0x02 == length) {
			value = input.readShort();
		} else if (0x04 == length) {
			value = input.readInt();
		} else {
			throw new IOException("EC-0003: Invalid length");
		}

		definitions.add(new SFD(IOCA.CODE_BEGIN_SEGMENT,
				new IOCAEntry(IOCA.FIELD_NAME, IOCA.TYPE_UBIN, value)));
	}

	private void beginImageContent(final byte length, final ImageInputStream input) throws IOException {
		if (0x01 != length) {
			throw new IOException("EC-0003: Invalid length");
		}

		if (0xFF != input.readUnsignedByte()) {
			throw new IOException("EC-0004: Invalid parameter value");
		}

		definitions.add(new SFD(IOCA.CODE_BEGIN_IMAGE_CONTENT));
	}

	private void imageSize(final byte length, final ImageInputStream input) throws IOException {
		if (0x09 != length) {
			throw new IOException("EC-0003: Invalid length");
		}

		byte unitBase = input.readByte();

		if (unitBase < 0x00 || unitBase > 0x02) {
			throw new IOException("EC-9410: Invalid or unsupported Image Data parameter value");
		}

		entries.add(new IOCAEntry(IOCA.FIELD_UNITBASE, IOCA.TYPE_CODE, unitBase));

		short[] identifiers = new short[] {
				IOCA.FIELD_HRESOL, IOCA.FIELD_VRESOL, IOCA.FIELD_HSIZE, IOCA.FIELD_VSIZE
		};

		for (short identifier : identifiers) {
			final int s = input.readUnsignedShort();

			if (s < 0x0000 || s > 0x7FFF) {
				throw new IOException("EC-0004: Invalid parameter value");
			}

			entries.add(new IOCAEntry(identifier, IOCA.TYPE_UBIN, (short) s));
		}

		definitions.add(new SFD(IOCA.CODE_IMAGE_SIZE_PARAMETER, entries));
	}

	private void imageEncoding(final byte length, final ImageInputStream input) throws IOException {
		if (length < 0x02 || length > 0x03) {
			throw new IOException("EC-0003: Invalid length");
		}

		int compressionId = input.readUnsignedByte();

		if (compressionId < 0x00 || (compressionId > 0x0D && compressionId < 0x80) ||
				(compressionId > 0x84 && compressionId < 0xA0) || compressionId > 0xAF) {
			throw new IOException("EC-9510: Invalid or unsupported Image Data parameter value");
		}

		entries.add(new IOCAEntry(IOCA.FIELD_COMPRID, IOCA.TYPE_CODE, (byte) compressionId));

		int recId = input.readUnsignedByte();

		if (recId < 0x00 || (recId > 0x04 && 0xFE != recId)) {
			throw new IOException("EC-9510: Invalid or unsupported Image Data parameter value");
		}

		entries.add(new IOCAEntry(IOCA.FIELD_RECID, IOCA.TYPE_CODE, (byte) recId));

		if (0x03 == length) {
			byte bitOrder = input.readByte();

			if (bitOrder < 0x00 || bitOrder > 0x01) {
				throw new IOException("EC-9510: Invalid or unsupported Image Data parameter value");
			}

			entries.add(new IOCAEntry(IOCA.FIELD_BITORDR, IOCA.TYPE_CODE, bitOrder));
		}

		definitions.add(new SFD(IOCA.CODE_IMAGE_ENCODING_PARAMETER, entries));
	}

	private void ideSize(final byte length, final ImageInputStream input) throws IOException {
		if (length != 0x01) {
			throw new IOException("EC-0003: Invalid length");
		}

		int ideSize = input.readUnsignedByte();

		if (ideSize < 0x01 || ideSize > 0xFF) {
			throw new IOException("EC-0004: Invalid parameter value");
		}

		definitions.add(new SFD(IOCA.CODE_IDE_SIZE_PARAMETER,
				new IOCAEntry(IOCA.FIELD_IDESZ, IOCA.TYPE_UBIN, (byte) ideSize)));
	}

	private void ideStructure(final byte length, final ImageInputStream input) throws IOException {
		if (length < 0x06 || length > 0x09) {
			throw new IOException("EC-0005: Invalid length");
		}

		entries.add(new IOCAEntry(IOCA.FIELD_FLAGS, IOCA.TYPE_BITS, input.readByte()));

		byte format = input.readByte();

		if (0x01 != format && 0x02 != format && 0x04 != format && 0x12 != format) {
			throw new IOException("EC-9B10: Invalid or unsupported IDE Structure parameter value");
		}

		entries.add(new IOCAEntry(IOCA.FIELD_FORMAT, IOCA.TYPE_CODE, format));

		if (0x0000 != input.readShort()) {
			throw new IOException("EC-9B10: Invalid or unsupported IDE Structure parameter value");
		}

		entries.add(new IOCAEntry(IOCA.FIELD_SIZE1, IOCA.TYPE_UBIN, input.readByte()));

		if (length > 0x06) {
			entries.add(new IOCAEntry(IOCA.FIELD_SIZE2, IOCA.TYPE_UBIN, input.readByte()));
		}

		if (length > 0x07) {
			entries.add(new IOCAEntry(IOCA.FIELD_SIZE3, IOCA.TYPE_UBIN, input.readByte()));
		}

		if (length > 0x08) {
			entries.add(new IOCAEntry(IOCA.FIELD_SIZE4, IOCA.TYPE_UBIN, input.readByte()));
		}

		definitions.add(new SFD(IOCA.CODE_IDE_STRUCTURE_PARAMETER, entries));
	}

	private void imageData(final int length, final ImageInputStream input) throws IOException {
		if (length < 0x0001 || length > 0xFFFF) {
			throw new IOException("EC-0003: Invalid length");
		}

		final byte[] buffer = new byte[length];

		if (length != input.read(buffer, 0, length)) {
			throw new EOFException();
		}

		definitions.add(new SFD(IOCA.CODE_IMAGE_DATA,
				new IOCAEntry(IOCA.FIELD_DATA, IOCA.TYPE_BLOB, buffer)));
	}

	private void end(final byte length, final short code) throws IOException {
		if (0x00 != length) {
			throw new IOException("EC-0003: Invalid length");
		}

		definitions.add(new SFD(code));
	}
}
