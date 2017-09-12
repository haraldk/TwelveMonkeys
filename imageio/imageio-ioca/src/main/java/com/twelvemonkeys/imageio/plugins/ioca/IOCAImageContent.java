package com.twelvemonkeys.imageio.plugins.ioca;

import java.util.LinkedList;
import java.util.List;

final class IOCAImageContent {

	private final List<byte[]> data = new LinkedList<>();

	private IOCAImageSize imageSize;
	private IOCAImageEncoding imageEncoding;
	private IOCAIdeStructure ideStructure;

	private short ideSize = 0x01; // The default is 1 (bilevel image).

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
		if (ideSize < 0x01 || ideSize > 0xFF) {
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

	void addData(final byte[] chunk) {
		data.add(chunk);
	}

	byte[] getData() {
		int total = 0;

		for (byte[] chunk: data) {
			total += chunk.length;
		}

		final byte[] copy = new byte[total];
		int offset = 0;

		for (byte[] chunk: data) {
			System.arraycopy(chunk, 0, copy, offset, chunk.length);
		}

		return copy;
	}
}
