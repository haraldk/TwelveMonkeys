package com.twelvemonkeys.imageio.metadata.ioca;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;

public final class IOCAEntry extends AbstractEntry {

	final private short type;

	IOCAEntry(final short identifier, final short type, final Object value) {
		super(identifier, value);

		if (identifier < 0 || identifier >= IOCA.FIELD_NAMES.length) {
			throw new IllegalArgumentException(String.format("Illegal IOCA identifier: %s", identifier));
		}

		if (type < 0 || type >= IOCA.TYPE_NAMES.length) {
			throw new IllegalArgumentException(String.format("Illegal IOCA type: %s", type));
		}

		// TODO: Validate that type is applicable to value?
		this.type = type;
	}

	public short getType() {
		return type;
	}

	@Override
	public String getFieldName() {
		return IOCA.FIELD_NAMES[(short) getIdentifier()];
	}

	@Override
	public String getTypeName() {
		return IOCA.TYPE_NAMES[type];
	}

}
