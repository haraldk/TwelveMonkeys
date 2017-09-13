package com.twelvemonkeys.imageio.plugins.ioca;

final class IOCASegment {

	private long name;

	long getName() {
		return name;
	}

	void setName(final long name) {
		if (name < 0x00000000 || name > 0xFFFFFFFFL) {
			throw new IllegalArgumentException("EC-0005: invalid length.");
		}

		this.name = name;
	}
}
