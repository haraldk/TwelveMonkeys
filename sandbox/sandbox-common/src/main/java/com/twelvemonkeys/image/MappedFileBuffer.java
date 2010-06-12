package com.twelvemonkeys.image;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.*;
import java.nio.channels.FileChannel;

/**
 * MappedFileBuffer
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: MappedFileBuffer.java,v 1.0 Jun 12, 2010 4:56:51 PM haraldk Exp$
 */
public abstract class MappedFileBuffer extends DataBuffer {
    final Buffer buffer;

    private MappedFileBuffer(final int type, final int size, final int numBanks) throws IOException {
        super(type, size, numBanks);

        if (size < 0) {
            throw new IllegalArgumentException("Integer overflow for size: " + size);
        }

        int componentSize;
        switch (type) {
            case DataBuffer.TYPE_BYTE:
                componentSize = 1;
                break;
            case DataBuffer.TYPE_USHORT:
                componentSize = 2;
                break;
            case DataBuffer.TYPE_INT:
                componentSize = 4;
                break;
            default:
                throw new IllegalArgumentException("Unsupported data type: " + type);
        }

        File tempFile = File.createTempFile(String.format("%s-", getClass().getSimpleName()), ".tmp");
        tempFile.deleteOnExit();

        try {
            RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");

            long length = ((long) size) * componentSize * numBanks;

            raf.setLength(length);
            FileChannel channel = raf.getChannel();

            // Map entire file into memory, let OS virtual memory/paging do the heavy lifting
            MappedByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, length);
//            MappedByteBuffer byteBuffer = channel.map(FileChannel.MapMode.PRIVATE, 0, length);

            switch (type) {
                case DataBuffer.TYPE_BYTE:
                    buffer = byteBuffer;
                    break;
                case DataBuffer.TYPE_USHORT:
                    buffer = byteBuffer.asShortBuffer();
                    break;
                case DataBuffer.TYPE_INT:
                    buffer = byteBuffer.asIntBuffer();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + type);
            }

            // According to the docs, we can safely close the channel and delete the file now
            channel.close();
        }
        finally {
            if (!tempFile.delete()) {
                System.err.println("Could not delete temp file: " + tempFile.getAbsolutePath());
            }
        }
    }

    // TODO: Is throws IOException a good idea?

    public static MappedFileBuffer create(final int type, final int size, final int numBanks) throws IOException {
        switch (type) {
            case DataBuffer.TYPE_BYTE:
                return new DataBufferByte(size, numBanks);
            case DataBuffer.TYPE_USHORT:
                return new DataBufferUShort(size, numBanks);
            case DataBuffer.TYPE_INT:
                return new DataBufferInt(size, numBanks);
            default:
                throw new IllegalArgumentException("Unsupported data type: " + type);
        }
    }

    final static class DataBufferByte extends MappedFileBuffer {
        private final ByteBuffer buffer;

        public DataBufferByte(int size, int numBanks) throws IOException {
            super(DataBuffer.TYPE_BYTE, size, numBanks);
            buffer = (ByteBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, (byte) val);
        }
    }

    final static class DataBufferUShort extends MappedFileBuffer {
        private final ShortBuffer buffer;

        public DataBufferUShort(int size, int numBanks) throws IOException {
            super(DataBuffer.TYPE_USHORT, size, numBanks);
            buffer = (ShortBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, (short) (val & 0xffff));
        }
    }

    final static class DataBufferInt extends MappedFileBuffer {
        private final IntBuffer buffer;

        public DataBufferInt(int size, int numBanks) throws IOException {
            super(DataBuffer.TYPE_INT, size, numBanks);
            buffer = (IntBuffer) super.buffer;
        }

        @Override
        public int getElem(int bank, int i) {
            return buffer.get(bank * size + i);
        }

        @Override
        public void setElem(int bank, int i, int val) {
            buffer.put(bank * size + i, val);
        }
    }
}
