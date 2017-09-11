package com.twelvemonkeys.imageio.plugins.ioca;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

class IOCAProviderInfo extends ReaderWriterProviderInfo {
	IOCAProviderInfo() {
		super(
				IOCAProviderInfo.class,
				new String[] {"ioca", "IOCA", "ica", "ICA"},
				new String[] {"ica", "ioca"},
				new String[] {
						"image/x-afp+fs10", "image/x-afp+fs11", "image/x-afp+fs45"
				},
				"com.twelvemonkeys.imageio.plugins.ioca.IOCAImageReader",
				new String[] {"com.twelvemonkeys.imageio.plugins.ioca.IOCAImageReaderSpi"},
				null,
				new String[] {},
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
}
