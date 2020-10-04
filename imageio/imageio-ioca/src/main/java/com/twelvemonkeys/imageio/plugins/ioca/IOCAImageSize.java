package com.twelvemonkeys.imageio.plugins.ioca;

final class IOCAImageSize {

	private short unitBase;
	private int hResolution, vResolution, hSize, vSize;

	short getUnitBase() {
		return unitBase;
	}

	void setUnitBase(final short unitBase) {
		if (unitBase < 0x00 || unitBase > 0x02) {
			throw new IllegalArgumentException("EC-9410: invalid or unsupported Image Data parameter value.");
		}

		this.unitBase = unitBase;
	}

	int getHResolution() {
		return hResolution;
	}

	void setHResolution(final int hResolution) {
		this.hResolution = verifyRange(hResolution);
	}

	int getVResolution() {
		return vResolution;
	}

	void setVResolution(final int vResolution) {
		this.vResolution = verifyRange(vResolution);
	}

	int getHSize() {
		return hSize;
	}

	void setHSize(final int hSize) {
		this.hSize = verifyRange(hSize);
	}

	int getVSize() {
		return vSize;
	}

	void setVSize(final int vSize) {
		this.vSize = verifyRange(vSize);
	}

	private int verifyRange(final int value) {
		if (value < 0x0000 || value > 0x7FFF) {
			throw new IllegalArgumentException("EC-0004: invalid parameter value.");
		}

		return value;
	}
}
