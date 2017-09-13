package com.twelvemonkeys.imageio.stream;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public final class SubInputStream extends InputStream {

	private ImageInputStream parent;
	private long remaining;

	public SubInputStream(final ImageInputStream parent, final long length) throws IOException {
		if (parent.length() < (parent.getStreamPosition() + length)) {
			throw new IndexOutOfBoundsException();
		}

		remaining = length;
		this.parent = parent;
	}

	@Override
	public int read() throws IOException {
		if (remaining > 0) {
			final int read = parent.read();

			if (read < 0) {
				throw new EOFException();
			}

			remaining--;
			return read;
		} else {
			return -1;
		}
	}
}
