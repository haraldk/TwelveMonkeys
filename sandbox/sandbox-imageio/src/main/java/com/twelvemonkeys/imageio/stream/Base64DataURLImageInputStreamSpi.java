package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.io.StringInputStream;
import com.twelvemonkeys.io.enc.Base64Decoder;
import com.twelvemonkeys.io.enc.DecoderStream;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Base64DataURLImageInputStreamSpi
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Base64DataURLImageInputStreamSpi.java,v 1.0 03.09.13 09:35 haraldk Exp$
 */
public class Base64DataURLImageInputStreamSpi extends ImageInputStreamSpi {
    // This is generally a bad idea, because:
    //  - It is bound to String.class, and not all strings are base64 encoded data URLs.
    //  - It's better to just create a decoder stream from the base64 stream, and use what's already in ImageIO....

    public Base64DataURLImageInputStreamSpi() {
        super("TwelveMonkeys", "0.1-BETA", String.class);
    }

    @Override
    public ImageInputStream createInputStreamInstance(final Object input, final boolean useCache, final File cacheDir) throws IOException {
        String string = (String) input;

        InputStream stream = createStreamFromBase64(string);

        return useCache && cacheDir != null ? new FileCacheImageInputStream(stream, cacheDir) : new MemoryCacheImageInputStream(stream);
    }

    private InputStream createStreamFromBase64(String string) {
        if (!string.startsWith("data:")) {
            throw new IllegalArgumentException(String.format("Not a data URL: %s", string));
        }

        int index = string.indexOf(';');
        if (index < 0 || !string.regionMatches(index + 1, "base64,", 0, "base64,".length())) {
            throw new IllegalArgumentException(String.format("Not base64 encoded: %s", string));
        }

        int offset = index + "base64,".length() + 1;
        return new DecoderStream(new StringInputStream(string.substring(offset), Charset.forName("UTF-8")), new Base64Decoder());
    }

    @Override
    public boolean canUseCacheFile() {
        return true;
    }

    @Override
    public String getDescription(Locale locale) {
        return "Service provider that instantiates a FileCacheImageInputStream or MemoryCacheImageInputStream from a Base64 encoded data string";
    }
}
