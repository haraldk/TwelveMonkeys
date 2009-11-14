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

    EXIFEntry(final Object pIdentifier, final Object pValue, final short pType) {
        super(pIdentifier, pValue);
        mType = pType;
    }

    @Override
    public String getFieldName() {
        // TODO: Need tons of constants... ;-)
        return super.getFieldName();
    }

    @Override
    public String getTypeName() {
        return EXIF.TYPE_NAMES[mType];
    }
}
