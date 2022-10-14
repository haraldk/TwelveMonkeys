package com.twelvemonkeys.imageio.stream;

import com.twelvemonkeys.imageio.spi.ProviderInfo;

import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import java.util.Locale;

/**
 * BufferedInputStreamImageInputStreamSpi.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: BufferedInputStreamImageInputStreamSpi.java,v 1.0 08/09/2022 haraldk Exp$
 */
public final class BufferedInputStreamImageInputStreamSpi extends ImageInputStreamSpi {
    public BufferedInputStreamImageInputStreamSpi() {
        this(new StreamProviderInfo());
    }

    private BufferedInputStreamImageInputStreamSpi(ProviderInfo providerInfo) {
        super(providerInfo.getVendorName(), providerInfo.getVersion(), InputStream.class);
    }

    @Override
    public void onRegistration(final ServiceRegistry registry, final Class<?> category) {
        Iterator<ImageInputStreamSpi> providers = registry.getServiceProviders(ImageInputStreamSpi.class, new InputStreamFilter(), true);

        while (providers.hasNext()) {
            ImageInputStreamSpi provider = providers.next();
            if (provider != this) {
                registry.setOrdering(ImageInputStreamSpi.class, this, provider);
            }
        }
    }

    @Override
    public ImageInputStream createInputStreamInstance(final Object input, final boolean useCacheFile, final File cacheDir) throws IOException {
        if (input instanceof InputStream) {
            ReadableByteChannel channel = Channels.newChannel((InputStream) input);

            if (channel instanceof SeekableByteChannel) {
                // Special case for FileInputStream/FileChannel, we can get a seekable channel directly
                return new BufferedChannelImageInputStream((SeekableByteChannel) channel);
            }

            // Otherwise, create a cache for backwards seeking
            return new BufferedChannelImageInputStream(useCacheFile ? new DiskCache(channel, cacheDir): new MemoryCache(channel));
        }

        throw new IllegalArgumentException("Expected input of type InputStream: " + input);
    }

    @Override
    public boolean canUseCacheFile() {
        return true;
    }

    @Override
    public String getDescription(final Locale locale) {
        return "Service provider that instantiates an ImageInputStream from an InputStream";
    }

    private static class InputStreamFilter implements ServiceRegistry.Filter {
        @Override
        public boolean filter(final Object provider) {
            return ((ImageInputStreamSpi) provider).getInputClass() == InputStream.class;
        }
    }
}
