package com.twelvemonkeys.imageio.plugins.crw.ciff;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.metadata.MetadataReader;

/**
 * CIFFReader
 */
public final class CIFFReader extends MetadataReader {

    @Override
    public CIFFDirectory read(ImageInputStream input) throws IOException {
        long start = readCIFFHeader(input);

        // Spec: "No information regarding the length of the heap is given within the actual heap data structure itself"
        return readHeap(input, start, input.length() - start); // TODO: If length is unknown, we'll have to search for it...
    }

    private int readCIFFHeader(ImageInputStream input) throws IOException {
        byte[] bom = new byte[2];
        input.readFully(bom);

        // CIFF byte order mark II (Intel) or MM (Motorola), just like TIFF
        if (bom[0] == 'I' && bom[1] == 'I') {
            input.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        }
        else if (bom[0] == 'M' && bom[1] == 'M') {
            input.setByteOrder(ByteOrder.BIG_ENDIAN);
        }
        else {
            throw new IIOException("No CIFF byte order mark found, expected 'II' or 'MM'");
        }

        int size = input.readInt();
        if (size != CIFF.HEADER_SIZE) {
            // TODO: Other sizes?
            throw new IIOException(String.format("Unexpected CIFF header size, expected %d: %d ", CIFF.HEADER_SIZE, size));
        }

        byte[] typeInfo = new byte[8];
        input.readFully(typeInfo);

        byte[] type = Arrays.copyOfRange(typeInfo, 0, 4);
        if (!Arrays.equals(CIFF.TYPE_HEAP, type)) {
            throw new IIOException(String.format("Unexpected CIFF type, expected 'HEAP': '%s'", new String(type, StandardCharsets.US_ASCII)));
        }

        byte[] subtype = Arrays.copyOfRange(typeInfo, 4, 8);
        if (!(Arrays.equals(CIFF.SUBTYPE_ARCH, subtype) || Arrays.equals(CIFF.SUBTYPE_CCDR, subtype)
              || Arrays.equals(CIFF.SUBTYPE_JPGM, subtype) ||Arrays.equals(CIFF.SUBTYPE_TIFP, subtype))) {
            throw new IIOException(String.format("Unsupported CIFF subtype, expected 'ARCH', 'CCDR', 'JPGM' or 'TIFP': '%s'",
                                                 new String(subtype, StandardCharsets.US_ASCII)));
        }

        // Version 1.2
        long version = input.readUnsignedInt();
        if (version != CIFF.VERSION_1_2) {
            throw new IIOException(String.format("Unsupported CIFF version, expected 1.2: %d.%d", version >> 16, version & 0xffff));
        }

        return size;
    }

    private CIFFDirectory readHeap(ImageInputStream input, long heapOffset, long heapLength) throws IOException {
        input.seek(heapOffset + heapLength - 4);
        long offsetTableOffset = input.readUnsignedInt();

        return readOffsetTable(input, heapOffset, offsetTableOffset + heapOffset);
    }

    private CIFFDirectory readOffsetTable(ImageInputStream input, long heapOffset, long tableOffset) throws IOException {
        input.seek(tableOffset);

        // DC_UINT16 numRecords;/* the number tblArray elements */
        // DC_RECORD_ENTRY tblArray[1];/* Array of the record entries */
        int count = input.readUnsignedShort(); // 0 entries is allowed
        CIFFEntry[] entries = new CIFFEntry[count];

        // TYPECODE typeCode;/* type code of the record */
        // DC_UINT32 length;/* record length */
        // DC_UINT32 offset;/* offset of the record in the heap*/
        for (int i = 0; i < count; i++) {
            short typeCode = input.readShort();

            long entryLength = input.readUnsignedInt();
            long entryOffset = input.readUnsignedInt();

            int recordType = typeCode & 0xc000;
            int dataType = typeCode & 0x3800;
            int idCode = typeCode & 0x7ff;

            // typeIdCode = dataType + idCode
            int typeIdCode = typeCode & 0x3fff;


            if (recordType == CIFF.STORAGE_RECORD) {
                Object value = null;
                // TODO: Even for these records, we need to know the data structure for each tag... :-P
                switch (dataType) {
                    case CIFF.DATA_TYPE_BYTE:
                        value = (byte) entryLength & 0xff;
                        break;
                    case CIFF.DATA_TYPE_ASCII:
                        ByteBuffer buffer = ByteBuffer.allocate(8);
                        buffer.putInt((int) entryLength);
                        buffer.putInt((int) entryOffset);

                        value = toNullTerminatedStrings(buffer.array());
                        break;
                    case CIFF.DATA_TYPE_WORD:
                        value = (short) entryLength & 0xffff;
                        break;
                    case CIFF.DATA_TYPE_DWORD:
                        value = (int) entryLength;
                        break;
                    case CIFF.DATA_TYPE_UNDEFINED:
                        value = entryLength << 32L | entryOffset;
                        break;
                }

                entries[i] = new CIFFEntry(typeIdCode, value);
            }
            else {
                entries[i] = new CIFFEntry(typeIdCode, new long[] {heapOffset + entryOffset, entryLength}, heapOffset + entryOffset, entryLength);
            }
        }

        // Fill inn sub entries
        input.mark();
        try {
            for (int i = 0; i < count; i++) {
                CIFFEntry entry = entries[i];
                int dataType = entry.tagId() & 0x3800;

                Object value;
                if (entry.isHeapStorage()) {
                    switch (dataType) {
                        case CIFF.DATA_TYPE_HEAP_1:
                        case CIFF.DATA_TYPE_HEAP_2:
                            value = readHeap(input, entry.offset(), entry.length());
                            break;

                        case CIFF.DATA_TYPE_BYTE:
                        case CIFF.DATA_TYPE_UNDEFINED:
                        case CIFF.DATA_TYPE_ASCII:
                            byte[] bytes = new byte[(int) entry.length()];
                            input.seek(entry.offset());
                            input.readFully(bytes);
                            value = dataType == CIFF.DATA_TYPE_ASCII ? toNullTerminatedStrings(bytes) : bytes;
                            break;

                        case CIFF.DATA_TYPE_WORD:
                            short[] shorts = new short[(int) (entry.length() / 2)];
                            input.seek(entry.offset());
                            input.readFully(shorts, 0, shorts.length);
                            value = shorts;
                            break;

                        case CIFF.DATA_TYPE_DWORD:
                            int[] ints = new int[(int) (entry.length() / 4)];
                            input.seek(entry.offset());
                            input.readFully(ints, 0, ints.length);
                            value = ints;
                            break;

                        default:
                            throw new IIOException(String.format("Unsupported data type: 0x%04x", dataType));
                    }

                    entries[i] = new CIFFEntry(entry.tagId(), value, entry.offset(), entry.length());
                }
            }
        }
        finally {
            input.reset();
        }

        return new CIFFDirectory(asList(entries));
    }

    private String[] toNullTerminatedStrings(byte[] buffer) {
        int len = buffer.length;

        while (len > 0 && buffer[len - 1] == 0) {
            len--;
        }

        return new String(buffer, 0, len, StandardCharsets.US_ASCII).split("\u0000");
    }
}
