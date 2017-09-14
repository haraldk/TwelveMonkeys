package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

import java.io.InputStream;

public class CCITTFaxProviderInfo extends ReaderWriterProviderInfo {

	CCITTFaxProviderInfo() {
		super(
				CCITTFaxProviderInfo.class,
				new String[] {"G4", "G3"},
				null,
				null,
				"com.twelvemonkeys.imageio.plugins.ioca.CCITTFaxImageReader",
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null
		);
	}

	@Override
	public Class[] inputTypes() {
		return new Class[] {InputStream.class};
	}
}
