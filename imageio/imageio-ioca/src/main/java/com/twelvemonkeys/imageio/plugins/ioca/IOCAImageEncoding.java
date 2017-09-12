package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.metadata.ioca.IOCA;

final class IOCAImageEncoding {

	private short compressionId, recordingId, bitOrder;

	short getCompressionId() {
		return compressionId;
	}

	void setCompressionId(final short compressionId) {
		if (compressionId < 0x00 || (compressionId > 0x0D && compressionId < 0x80) ||
				(compressionId > 0x84 && compressionId < 0xA0) || compressionId > 0xAF) {
			throw new IllegalArgumentException("EC-9510: invalid or unsupported Image Data parameter value.");
		}

		this.compressionId = compressionId;
	}

	short getRecordingId() {
		return recordingId;
	}

	void setRecordingId(final short recordingId) {
		if (recordingId < 0x00 || (recordingId > 0x04 && recordingId != 0xFE)) {
			throw new IllegalArgumentException("EC-9510: invalid or unsupported Image Data parameter value.");
		}

		this.recordingId = recordingId;
	}

	short getBitOrder() {
		return bitOrder;
	}

	void setBitOrder(final short bitOrder) {
		if (bitOrder != IOCA.BITORDR_LTR && bitOrder != IOCA.BITORDR_RTL) {
			throw new IllegalArgumentException("EC-9510: invalid or unsupported Image Data parameter value.");
		}

		this.bitOrder = bitOrder;
	}
}
