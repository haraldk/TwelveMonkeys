package com.twelvemonkeys.imageio.plugins.jpeg;

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;

public class ImageFormatFilter implements ServiceRegistry.Filter {
    String name;

    public ImageFormatFilter (String name) {
        this.name = name;
    }

    public boolean filter (Object obj) {
        try {
            if (obj instanceof ImageReaderSpi) {
                return contains(((ImageReaderSpi) obj).getFormatNames(), name);
            } else if (obj instanceof ImageWriterSpi) {
                return contains(((ImageWriterSpi) obj).getFormatNames(), name);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean contains (String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (name.equalsIgnoreCase(names[i])) {
                return true;
            }
        }
        return false;
    }
}