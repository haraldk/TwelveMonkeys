package com.twelvemonkeys.imageio.plugins.dds;

import com.twelvemonkeys.imageio.spi.ReaderWriterProviderInfo;

final class DDSProviderInfo extends ReaderWriterProviderInfo {
	DDSProviderInfo() {
		super(
				DDSProviderInfo.class,
				new String[] {"DDS", "dds"},
				new String[] {"dds"},
				new String[] {"image/vnd-ms.dds"},
				"com.twelvemonkeys.imageio.plugins.dds.DDSImageReader",
				new String[]{"com.twelvemonkeys.imageio.plugins.dds.DDSImageReaderSpi"},
				null,
				null,
				false, null, null, null, null,
				true, null, null, null, null
		);
	}
}
