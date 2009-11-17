package com.twelvemonkeys.imageio.metadata.exif;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;

/**
 * EXIFEntry
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFEntry.java,v 1.0 Nov 13, 2009 5:47:35 PM haraldk Exp$
 */
final class EXIFEntry extends AbstractEntry {
    final private short mType;

    EXIFEntry(final int pIdentifier, final Object pValue, final short pType) {
        super(pIdentifier, pValue);

        if (pType < 1 || pType > TIFF.TYPE_NAMES.length) {
            throw new IllegalArgumentException(String.format("Illegal EXIF type: %s", pType));
        }
        
        mType = pType;
    }

    @Override
    public String getFieldName() {
        switch ((Integer) getIdentifier()) {
            case TIFF.TAG_SOFTWARE:
                return "Software";
            case TIFF.TAG_DATE_TIME:
                return "DateTime";
            case TIFF.TAG_ARTIST:
                return "Artist";
            case TIFF.TAG_COPYRIGHT:
                return "Copyright";

            case EXIF.TAG_COLOR_SPACE:
                return "ColorSpace";
            case EXIF.TAG_PIXEL_X_DIMENSION:
                return "PixelXDimension";
            case EXIF.TAG_PIXEL_Y_DIMENSION:
                return "PixelYDimension";
            // TODO: More field names
        }

        return null;
    }

    @Override
    public String getTypeName() {
        return TIFF.TYPE_NAMES[mType - 1];
    }
}
