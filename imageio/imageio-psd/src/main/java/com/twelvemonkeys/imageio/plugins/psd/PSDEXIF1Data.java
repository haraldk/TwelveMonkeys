package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * EXIF metadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPData.java,v 1.0 Jul 28, 2009 5:50:34 PM haraldk Exp$
 *
 * @see <a href="http://en.wikipedia.org/wiki/Exchangeable_image_file_format">Wikipedia</a>
 * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html">Aware systems TIFF tag reference</a>
 * @see <a href="http://partners.adobe.com/public/developer/tiff/index.html">Adobe TIFF developer resources</a>
 */
final class PSDEXIF1Data extends PSDImageResource {
    protected Directory directory;

    PSDEXIF1Data(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        // This is in essence an embedded TIFF file.
        // TODO: Instead, read the byte data, store for later parsing (or better yet, store offset, and read on request)
        directory = new EXIFReader().read(pInput);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();
        builder.append(", ").append(directory);
        builder.append("]");

        return builder.toString();
    }
}
