package com.twelvemonkeys.imageio.plugins.crw.ciff;

import java.util.Collection;

import com.twelvemonkeys.imageio.metadata.AbstractDirectory;

/**
 * CIFFDirectory
 */
public final class CIFFDirectory extends AbstractDirectory {

    CIFFDirectory(Collection<CIFFEntry> entries) {
        super(entries);
    }

    @Override
    public CIFFEntry getEntryById(Object identifier) {
        return (CIFFEntry) super.getEntryById(identifier);
    }

    @Override
    public CIFFEntry getEntryByFieldName(String fieldName) {
        return (CIFFEntry) super.getEntryByFieldName(fieldName);
    }

    public CIFFDirectory getSubDirectory(int tagId) {
        CIFFEntry entry = getEntryById(tagId);
        return entry == null ? null : (CIFFDirectory) entry.getValue();
    }
}
