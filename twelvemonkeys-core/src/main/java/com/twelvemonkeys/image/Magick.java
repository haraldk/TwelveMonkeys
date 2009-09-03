package com.twelvemonkeys.image;

/**
 * Magick
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/Magick.java#1 $
 */
final class Magick {
    static final boolean DEBUG = useDebug();

    private static boolean useDebug() {
        try {
            return "TRUE".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.image.magick.debug"));
        }
        catch (Throwable t) {
            // Most probably in case of a SecurityManager
            return false;
        }
    }

    private Magick() {}
}
