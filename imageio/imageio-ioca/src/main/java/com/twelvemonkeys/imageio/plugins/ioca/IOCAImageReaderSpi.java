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
	public boolean canDecodeInput(final Object pSource) throws IOException {
		return canDecodeAs(pSource);
	}

	private static boolean canDecodeAs(final Object pSource) throws IOException {
		if (!(pSource instanceof ImageInputStream)) {
			return false;
		}

		try (final ImageInputStream stream = (ImageInputStream) pSource) {
			byte[] magic = new byte[2];

			stream.mark();
			stream.readFully(magic);

			if (magic[0] != 0x70) {
				return false;
			}

			if (magic[1] < 0x00 || magic[1] > 0x04) {
				return false;
			}
		}

		return false;
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
