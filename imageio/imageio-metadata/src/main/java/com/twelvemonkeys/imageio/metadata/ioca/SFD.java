package com.twelvemonkeys.imageio.metadata.ioca;

import com.twelvemonkeys.imageio.metadata.AbstractDirectory;
import com.twelvemonkeys.imageio.metadata.Entry;

import java.util.Arrays;
import java.util.Collection;

public final class SFD extends AbstractDirectory {

	private final short code;

	SFD(final short code, final Collection<IOCAEntry> parameters) {
		super(parameters);

		this.code = code;
		if (code < 0 || code >= IOCA.CODE_POINTS.length + IOCA.EXTENDED_CODE_POINTS.length) {
			throw new IllegalArgumentException(String.format("Illegal SFD code: %s", code));
		}
	}

	SFD(final short code, final IOCAEntry... parameters) {
		this(code, Arrays.asList(parameters));
	}

	public short getCode() {
		return code;
	}

	public int getCodePoint() {
		return code > IOCA.CODE_POINTS.length? IOCA.EXTENDED_CODE_POINTS[code - IOCA.CODE_POINTS.length] :
				IOCA.CODE_POINTS[code];
	}
}
