package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.metadata.ioca.IOCA;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

final class IOCAImageContent {

	private final List<DataRecord> dataRecords = new LinkedList<>();

	private IOCASegment segment;
	private IOCAImageSize imageSize;
	private IOCAImageEncoding imageEncoding;
	private IOCAIdeStructure ideStructure;

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
		if (ideSize < IOCA.IDESZ_BILEVEL || ideSize > 0xFF) {
			throw new IllegalArgumentException("EC-0004: Invalid parameter value");
		}

		this.ideSize = ideSize;
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

	void recordData(final long offset, final int length) {
		dataRecords.add(new DataRecord(offset, length));
	}

	void recordData(final byte[] buffer) {
		dataRecords.add(new DataRecord(buffer, 0, buffer.length));
	}

	List<DataRecord> getDataRecords() {
		return dataRecords;
	}

	static class DataRecord {

		private final byte[] buffer;

		private final long offset;
		private final int length;

		DataRecord(final long offset, final int length) {
			this.offset = offset;
			this.length = length;
			this.buffer = null;
		}

		DataRecord(final byte[] buffer, final long offset, final int length) {
			this.offset = offset;
			this.buffer = Objects.requireNonNull(buffer);
			this.length = length;
		}

		byte[] getBuffer() {
			return buffer;
		}

		long getOffset() {
			return offset;
		}

		int getLength() {
			return length;
		}
	}
}
