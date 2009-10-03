package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
//    protected byte[] mData;
    protected Directory mDirectory;

    PSDEXIF1Data(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        // This is in essence an embedded TIFF file.
        // TODO: Extract TIFF parsing to more general purpose package
        // TODO: Instead, read the byte data, store for later parsing
        MemoryCacheImageInputStream stream = new MemoryCacheImageInputStream(IIOUtil.createStreamAdapter(pInput, mSize));

        byte[] bom = new byte[2];
        stream.readFully(bom);
        if (bom[0] == 'I' && bom[1] == 'I') {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
        else if (!(bom[0] == 'M' && bom[1] == 'M')) {
            throw new IIOException(String.format("Invalid byte order marker '%s'", StringUtil.decode(bom, 0, bom.length, "ASCII")));
        }

        if (stream.readUnsignedShort() != 42) {
            throw new IIOException("Wrong TIFF magic in EXIF data.");
        }

        long directoryOffset = stream.readUnsignedInt();
        mDirectory = Directory.read(stream, directoryOffset);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", ").append(mDirectory);

        builder.append("]");

        return builder.toString();
    }

    // TIFF Image file directory (IFD)
    private static class Directory {
        List<Entry> mEntries = new ArrayList<Entry>();

        private Directory() {}

        public static Directory read(final ImageInputStream pInput, final long pOffset) throws IOException {
            Directory directory = new Directory();

            pInput.seek(pOffset);
            int entryCount = pInput.readUnsignedShort();
            for (int i = 0; i < entryCount; i++) {
                directory.mEntries.add(Entry.read(pInput));
            }

            long nextOffset = pInput.readUnsignedInt();
            if (nextOffset != 0) {
                Directory next = Directory.read(pInput, nextOffset);
                directory.mEntries.addAll(next.mEntries);
            }

            return directory;
        }

        @Override
        public String toString() {
            return String.format("Directory%s", mEntries);
        }
    }

    // TIFF IFD Entry
    private static class Entry {
        private static final int EXIF_IFD = 0x8769;

        private final static String[] TYPE_NAMES = {
                "BYTE", "ASCII", "SHORT", "LONG", "RATIONAL",

                "SBYTE", "UNDEFINED", "SSHORT", "SLONG", "SRATIONAL", "FLOAT", "DOUBLE",
        };

        private final static int[] TYPE_LENGTHS = {
                1, 1, 2, 4, 8,

                1, 1, 2, 4, 8, 4, 8,
        };

        private int mTag;
        /*
        1 = BYTE 8-bit unsigned integer.
        2 = ASCII 8-bit byte that contains a 7-bit ASCII code; the last byte
        must be NUL (binary zero).
        3 = SHORT 16-bit (2-byte) unsigned integer.
        4 = LONG 32-bit (4-byte) unsigned integer.
        5 = RATIONAL Two LONGs:  the first represents the numerator of a
        fraction; the second, the denominator.

         TIFF 6.0 and above:
        6 = SBYTE An 8-bit signed (twos-complement) integer.
        7 = UNDEFINED An 8-bit byte that may contain anything, depending on
        the definition of the field.
        8 = SSHORT A 16-bit (2-byte) signed (twos-complement) integer.
        9 = SLONG A 32-bit (4-byte) signed (twos-complement) integer.
        10 = SRATIONAL Two SLONGs:  the first represents the numerator of a
        fraction, the second the denominator.
        11 = FLOAT Single precision (4-byte) IEEE format.
        12 = DOUBLE Double precision (8-byte) IEEE format.
         */
        private short mType;
        private int mCount;
        private long mValueOffset;
        private Object mValue;

        private Entry() {}

        public static Entry read(final ImageInputStream pInput) throws IOException {
            Entry entry = new Entry();

            entry.mTag = pInput.readUnsignedShort();
            entry.mType = pInput.readShort();
            entry.mCount = pInput.readInt(); // Number of values

            // TODO: Handle other sub-IFDs
            if (entry.mTag == EXIF_IFD) {
                long offset = pInput.readUnsignedInt();
                pInput.mark();
                try {
                    entry.mValue = Directory.read(pInput, offset);
                }
                finally {
                    pInput.reset();
                }
            }
            else {
                int valueLength = entry.getValueLength();
                if (valueLength > 0 && valueLength <= 4) {
                    entry.readValueInLine(pInput);
                    pInput.skipBytes(4 - valueLength);
                }
                else {
                    entry.mValueOffset = pInput.readUnsignedInt(); // This is the *value* iff the value size is <= 4 bytes
                    entry.readValue(pInput);
                }
            }

            return entry;
        }

        private void readValue(final ImageInputStream pInput) throws IOException {
            long pos = pInput.getStreamPosition();
            try {
                pInput.seek(mValueOffset);
                readValueInLine(pInput);
            }
            finally {
                pInput.seek(pos);
            }
        }

        private void readValueInLine(ImageInputStream pInput) throws IOException {
            mValue = readValueDirect(pInput, mType, mCount);
        }

        private static Object readValueDirect(final ImageInputStream pInput, final short pType, final int pCount) throws IOException {
            switch (pType) {
                case 2:
                    // TODO: This might be UTF-8 or ISO-8859-1, even though against the spec
                    byte[] ascii = new byte[pCount];
                    pInput.readFully(ascii);
                    return StringUtil.decode(ascii, 0, ascii.length, "ASCII");
                case 1:
                    if (pCount == 1) {
                        return pInput.readUnsignedByte();
                    }
                case 6:
                    if (pCount == 1) {
                        return pInput.readByte();
                    }
                case 7:
                    byte[] bytes = new byte[pCount];
                    pInput.readFully(bytes);
                    return bytes;
                case 3:
                    if (pCount == 1) {
                        return pInput.readUnsignedShort();
                    }
                case 8:
                    if (pCount == 1) {
                        return pInput.readShort();
                    }

                    short[] shorts = new short[pCount];
                    pInput.readFully(shorts, 0, shorts.length);
                    return shorts;
                case 4:
                    if (pCount == 1) {
                        return pInput.readUnsignedInt();
                    }
                case 9:
                    if (pCount == 1) {
                        return pInput.readInt();
                    }

                    int[] ints = new int[pCount];
                    pInput.readFully(ints, 0, ints.length);
                    return ints;
                case 11:
                    if (pCount == 1) {
                        return pInput.readFloat();
                    }

                    float[] floats = new float[pCount];
                    pInput.readFully(floats, 0, floats.length);
                    return floats;
                case 12:
                    if (pCount == 1) {
                        return pInput.readDouble();
                    }

                    double[] doubles = new double[pCount];
                    pInput.readFully(doubles, 0, doubles.length);
                    return doubles;

                // TODO: Consider using a Rational class
                case 5:
                    if (pCount == 1) {
                        return pInput.readUnsignedInt() / (double) pInput.readUnsignedInt();
                    }

                    double[] rationals = new double[pCount];
                    for (int i = 0; i < rationals.length; i++) {
                        rationals[i] = pInput.readUnsignedInt() / (double) pInput.readUnsignedInt();
                    }

                    return rationals;
                case 10:
                    if (pCount == 1) {
                        return pInput.readInt() / (double) pInput.readInt();
                    }

                    double[] srationals = new double[pCount];
                    for (int i = 0; i < srationals.length; i++) {
                        srationals[i] = pInput.readInt() / (double) pInput.readInt();
                    }

                    return srationals;

                default:
                    throw new IIOException(String.format("Unknown EXIF type '%s'", pType));
            }
        }

        private int getValueLength() {
            if (mType > 0 && mType <= TYPE_LENGTHS.length) {
                return TYPE_LENGTHS[mType - 1] * mCount;
            }
            return -1;
        }

        private String getTypeName() {
            if (mType > 0 && mType <= TYPE_NAMES.length) {
                return TYPE_NAMES[mType - 1];
            }
            return "Unknown type";
        }

        // TODO: Tag names!
        @Override
        public String toString() {
            return String.format("0x%04x: %s (%s, %d)", mTag, getValueAsString(), getTypeName(), mCount);
        }

        public String getValueAsString() {
            if (mValue instanceof String) {
                return String.format("\"%s\"", mValue);
            }

            if (mValue != null && mValue.getClass().isArray()) {
                Class<?> type = mValue.getClass().getComponentType();
                if (byte.class == type) {
                    return Arrays.toString((byte[]) mValue);
                }
                if (short.class == type) {
                    return Arrays.toString((short[]) mValue);
                }
                if (int.class == type) {
                    return Arrays.toString((int[]) mValue);
                }
                if (float.class == type) {
                    return Arrays.toString((float[]) mValue);
                }
                if (double.class == type) {
                    return Arrays.toString((double[]) mValue);
                }
            }

            return String.valueOf(mValue);
        }
    }
}
