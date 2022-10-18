package com.twelvemonkeys.imageio.stream;

import java.nio.channels.SeekableByteChannel;

interface Cache extends SeekableByteChannel {
    void flushBefore(long pos);
}
