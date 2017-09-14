package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.metadata.ioca.IOCA;

final class IOCAIdeStructure {

	private short flags, format, size1, size2, size3, size4;

	short getFlags() {
		return flags;
	}

	void setFlags(final short flags) {
		this.flags = flags;
	}

	short getFormat() {
		return format;
	}

	void setFormat(final short format) {
		if (IOCA.FORMAT_RGB != format && IOCA.FORMAT_YCRCB != format && IOCA.FORMAT_CMYK != format
				&& IOCA.FORMAT_YCBCR != format) {
			throw new IllegalArgumentException("EC-9B10: invalid or unsupported IDE Structure parameter value.");
		}

		this.format = format;
	}

	boolean is8Bit() {
		return 0x08 == size1 && 0x08 == size2 && 0x08 == size3;
	}

	boolean is16Bit() {
		return 0x10 == size1 && 0x10 == size2 && 0x10 == size3;
	}

	short getSize1() {
		return size1;
	}

	void setSize1(final short size1) {
		this.size1 = size1;
	}

	short getSize2() {
		return size2;
	}

	void setSize2(final short size2) {
		this.size2 = size2;
	}

	short getSize3() {
		return size3;
	}

	void setSize3(final short size3) {
		this.size3 = size3;
	}

	short getSize4() {
		return size4;
	}

	void setSize4(final short size4) {
		this.size4 = size4;
	}

	private int verifyRange(final short value) {
		if (value < 0x00 || value > 0xFF) {
			throw new IllegalArgumentException("EC-9B10: invalid or unsupported IDE Structure parameter value.");
		}

		return value;
	}
}
