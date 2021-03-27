package com.twelvemonkeys.imageio.plugins.pntg;

import com.twelvemonkeys.imageio.spi.ImageReaderSpiBase;

import javax.imageio.stream.ImageInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Locale;

/**
 * PNTGImageReaderSpi.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PNTGImageReaderSpi.java,v 1.0 23/03/2021 haraldk Exp$
 */
public final class PNTGImageReaderSpi extends ImageReaderSpiBase {
    public PNTGImageReaderSpi() {
        super(new PNTGProviderInfo());
    }

    @Override
    public boolean canDecodeInput(final Object source) throws IOException {
        if (!(source instanceof ImageInputStream)) {
            return false;
        }

        ImageInputStream stream = (ImageInputStream) source;
        stream.mark();

        try {
            // TODO: Figure out how to read the files without the MacBinary header...
            //   Probably not possible, as it's just 512 bytes of nulls OR pattern information
            return isMacBinaryPNTG(stream);
        }
        catch (EOFException ignore) {
            return false;
        }
        finally {
            stream.reset();
        }
    }

    static boolean isMacBinaryPNTG(final ImageInputStream stream) throws IOException {
        stream.seek(0);

        if (stream.readByte() != 0) {
            return false;
        }

        byte nameLen = stream.readByte();
        if (nameLen < 0 || nameLen > 63) {
            return false;
        }

        stream.skipBytes(63);

        // Validate that type is PNTG and that next 4 bytes are all within the ASCII range, typically 'MPNT'
        return stream.readInt() == ('P' << 24 | 'N' << 16 | 'T' << 8 | 'G') && (stream.readInt() & 0x80808080) == 0;
    }

    @Override
    public PNTGImageReader createReaderInstance(final Object extension) {
        return new PNTGImageReader(this);
    }

    @Override
    public String getDescription(final Locale locale) {
        return "Apple MacPaint Painting (PNTG) image reader";
    }
}
