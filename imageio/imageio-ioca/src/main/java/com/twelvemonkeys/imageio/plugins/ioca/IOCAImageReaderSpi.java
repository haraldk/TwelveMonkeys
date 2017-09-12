package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.stream.ImageInputStream;

import java.io.IOException;

import java.util.Locale;

public final class IOCAImageReaderSpi extends ImageReaderSpiBase {

	/**
	 * Creates a {@code IOCAImageReaderSpi}.
	 */
	public IOCAImageReaderSpi() {
		super(new IOCAProviderInfo());
	}

	@Override
	public boolean canDecodeInput(final Object source) throws IOException {
		return canDecodeAs(source);
	}

	private static boolean canDecodeAs(final Object source) throws IOException {
		if (!(source instanceof ImageInputStream)) {
			return false;
		}

		final ImageInputStream iis = (ImageInputStream) source;
		final byte[] header = new byte[2];

		iis.mark();
		iis.readFully(header);

		// Look for the "begin segment" marker.
		return header[0] == 0x70 && header[1] >= 0x00 && header[1] <= 0x04;
	}

	@Override
	public IOCAImageReader createReaderInstance(final Object pExtension) {
		return new IOCAImageReader(this);
	}

	@Override
	public String getDescription(final Locale pLocale) {
		return "Image Object Content Architecture (IOCA) image reader";
	}
}
