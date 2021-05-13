package com.twelvemonkeys.imageio.plugins.crw.ciff;

import java.lang.reflect.Field;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;
import com.twelvemonkeys.lang.StringUtil;

/**
 * CIFFEntry
 */
public final class CIFFEntry extends AbstractEntry {

    private final long offset;
    private final long length;

    /**
     * Creates a CIFFEntry for "in-record" storage.
     *
     * @param identifier
     * @param value
     */
    CIFFEntry(int identifier, Object value) {
        super(identifier, value);
        this.offset = -1;
        this.length = -1;
    }

    /**
     * Creates a CIFFEntry for "in-heap" storage.
     *
     * @param identifier
     * @param value
     */
    CIFFEntry(int identifier, Object value, long offset, long length) {
        super(identifier, value);
        this.offset = offset;
        this.length = length;
    }

    int tagId() {
        return (int) getIdentifier();
    }

    boolean isHeapStorage() {
        return offset != -1 && length != -1;
    }

    public long offset() {
        return offset;
    }

    public long length() {
        return length;
    }

    @Override
    public String getFieldName() {
        switch ((Integer) getIdentifier()) {
            case CIFF.TAG_RAW_DATA:
                return "RawData";
            case CIFF.TAG_JPEG_PREVIEW:
                return "JPEGPreview";
            case CIFF.TAG_THUMBNAIL:
                return "Thumbnail";
            case CIFF.TAG_IMAGE_PROPERTIES:
                return "ImageProperties";
            case CIFF.TAG_EXIF_INFORMATION:
                return "ExifInformation";
            default:
                Field[] fields = CIFF.class.getFields();

                for (Field field : fields) {
                    try {
                        if (field.getType() == Integer.TYPE && field.getName().startsWith("TAG_")) {
                            if (field.get(null).equals(getIdentifier())) {
                                return StringUtil.lispToCamel(field.getName().substring(4).replace("_", "-").toLowerCase(), true);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        // Should never happen, but in case, abort
                        break;
                    }
                }

                return null;
        }
    }

    protected String getNativeIdentifier() {
        return String.format("0x%04x", (Integer) getIdentifier());
    }

    @Override
    public String getTypeName() {
        int dataType = tagId() & 0x3800;

        switch (dataType) {
            case CIFF.DATA_TYPE_BYTE:
                return "BYTE";
            case CIFF.DATA_TYPE_ASCII:
                return "ASCII";
            case CIFF.DATA_TYPE_WORD:
                return "SHORT";
            case CIFF.DATA_TYPE_DWORD:
                return "LONG";
            case CIFF.DATA_TYPE_UNDEFINED:
                return "UNDEFINED";
            case CIFF.DATA_TYPE_HEAP_1:
            case CIFF.DATA_TYPE_HEAP_2:
                return super.getTypeName();
            default:
                throw new IllegalStateException(String.format("Unsupported data type: 0x%04x", dataType));
        }
    }
}
