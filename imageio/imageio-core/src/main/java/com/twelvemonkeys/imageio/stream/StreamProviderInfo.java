package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;

final class StreamProviderInfo extends ProviderInfo {
    StreamProviderInfo() {
        super(StreamProviderInfo.class.getPackage());
    }
}
