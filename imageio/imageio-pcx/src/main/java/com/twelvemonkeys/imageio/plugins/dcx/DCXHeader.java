package com.twelvemonkeys.imageio.plugins.dcx;

import java.io.IOException;
import java.util.Arrays;

import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

final class DCXHeader {
    private final int[] offsetTable;

    DCXHeader(final int[] offsetTable) {
        this.offsetTable = offsetTable;
    }

    public int getCount() {
        return offsetTable.length;
    }

    public long getOffset(final int index) {
        return (0xffffffffL & offsetTable[index]);
    }

    public static DCXHeader read(final ImageInputStream imageInput) throws IOException {
        // typedef struct _DcxHeader
        // {
        //     DWORD Id;                      /* DCX Id number */
        //     DWORD PageTable[1024];         /* Image offsets */
        // } DCXHEAD;

        int magic = imageInput.readInt();
        if (magic != DCX.MAGIC) {
            throw new IIOException(String.format("Not a DCX file. Expected DCX magic %02x, read %02x", DCX.MAGIC, magic));
        }

        int[] offsets = new int[1024];

        int count = 0;
        do {
            offsets[count] = imageInput.readInt();
            count++;
        }
        while (offsets[count - 1] != 0 && count < offsets.length);

        return new DCXHeader(count == offsets.length ? offsets : Arrays.copyOf(offsets, count));
    }

    @Override public String toString() {
        return "DCXHeader{" +
                "offsetTable=" + Arrays.toString(offsetTable) +
                '}';
    }
}
