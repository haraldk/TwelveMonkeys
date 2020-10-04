package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import java.io.IOException;
import java.util.Locale;

public class CCITTFaxImageReaderSpi extends ImageReaderSpiBase {

	CCITTFaxImageReaderSpi() {
		super(new CCITTFaxProviderInfo());
	}

	@Override
	public boolean canDecodeInput(final Object source) throws IOException {
		return false;
	}

	@Override
	public IOCAImageReader createReaderInstance(final Object pExtension) {
		return null;
	}

	@Override
	public String getDescription(final Locale pLocale) {
		return "CCITT fax image reader";
	}
}
