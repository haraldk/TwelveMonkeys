/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.io.ole2;

import com.twelvemonkeys.io.*;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Represents a read-only OLE2 compound document.
 * <p/>
 * <!-- TODO: Consider really detaching the entries, as this is hard for users to enforce... -->
 * <em>NOTE: This class is not synchronized. Accessing the document or its
 * entries from different threads, will need synchronization on the document
 * instance.</em>
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/ole2/CompoundDocument.java#4 $
 */
public final class CompoundDocument {
    // TODO: Write support...
    // TODO: Properties: http://support.microsoft.com/kb/186898
    
    static final byte[] MAGIC = new byte[]{
            (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1,
    };

    private static final int FREE_SID = -1;
    private static final int END_OF_CHAIN_SID = -2;
    private static final int SAT_SECTOR_SID = -3; // Sector used by SAT
    private static final int MSAT_SECTOR_SID = -4; // Sector used my Master SAT

    public static final int HEADER_SIZE = 512;

    /** The epoch offset of CompoundDocument time stamps */
    public final static long EPOCH_OFFSET = -11644477200000L;

    private final DataInput input;

    private UUID uUID;

    private int sectorSize;
    private int shortSectorSize;

    private int directorySId;

    private int minStreamSize;

    private int shortSATSId;
    private int shortSATSize;

    // Master Sector Allocation Table
    private int[] masterSAT;
    private int[] SAT;
    private int[] shortSAT;

    private Entry rootEntry;
    private SIdChain shortStreamSIdChain;
    private SIdChain directorySIdChain;

    /**
     * Creates a (for now) read only {@code CompoundDocument}.
     *
     * @param pFile the file to read from
     *
     * @throws IOException if an I/O exception occurs while reading the header
     */
    public CompoundDocument(final File pFile) throws IOException {
        input = new LittleEndianRandomAccessFile(FileUtil.resolve(pFile), "r");

        // TODO: Might be better to read header on first read operation?!
        // OTOH: It's also good to be fail-fast, so at least we should make
        // sure we're reading a valid document
        readHeader();
    }

    /**
     * Creates a read only {@code CompoundDocument}.
     *
     * @param pInput the input to read from
     *
     * @throws IOException if an I/O exception occurs while reading the header
     */
    public CompoundDocument(final InputStream pInput) throws IOException {
        this(new FileCacheSeekableStream(pInput));
    }

    // For testing only, consider exposing later
    CompoundDocument(final SeekableInputStream pInput) throws IOException {
        input = new SeekableLittleEndianDataInputStream(pInput);

        // TODO: Might be better to read header on first read operation?!
        // OTOH: It's also good to be fail-fast, so at least we should make
        // sure we're reading a valid document
        readHeader();
    }

    /**
     * Creates a read only {@code CompoundDocument}.
     *
     * @param pInput the input to read from
     *
     * @throws IOException if an I/O exception occurs while reading the header
     */
    public CompoundDocument(final ImageInputStream pInput) throws IOException {
        input = pInput;

        // TODO: Might be better to read header on first read operation?!
        // OTOH: It's also good to be fail-fast, so at least we should make
        // sure we're reading a valid document
        readHeader();
    }

    public static boolean canRead(final DataInput pInput) {
        return canRead(pInput, true);
    }

    // TODO: Refactor.. Figure out what we really need to expose to ImageIO for
    // easy reading of the Thumbs.db file
    // It's probably safer to create one version for InputStream and one for File
    private static boolean canRead(final DataInput pInput, final boolean pReset) {
        long pos = FREE_SID;
        if (pReset) {
            try {
                if (pInput instanceof InputStream && ((InputStream) pInput).markSupported()) {
                    ((InputStream) pInput).mark(8);
                }
                else if (pInput instanceof ImageInputStream) {
                    ((ImageInputStream) pInput).mark();
                }
                else if (pInput instanceof RandomAccessFile) {
                    pos = ((RandomAccessFile) pInput).getFilePointer();
                }
                else if (pInput instanceof LittleEndianRandomAccessFile) {
                    pos = ((LittleEndianRandomAccessFile) pInput).getFilePointer();
                }
                else {
                    return false;
                }
            }
            catch (IOException ignore) {
                return false;
            }
        }

        try {
            byte[] magic = new byte[8];
            pInput.readFully(magic);
            return Arrays.equals(magic, MAGIC);
        }
        catch (IOException ignore) {
            // Ignore
        }
        finally {
            if (pReset) {
                try {
                    if (pInput instanceof InputStream && ((InputStream) pInput).markSupported()) {
                        ((InputStream) pInput).reset();
                    }
                    else if (pInput instanceof ImageInputStream) {
                        ((ImageInputStream) pInput).reset();
                    }
                    else if (pInput instanceof RandomAccessFile) {
                        ((RandomAccessFile) pInput).seek(pos);
                    }
                    else if (pInput instanceof LittleEndianRandomAccessFile) {
                        ((LittleEndianRandomAccessFile) pInput).seek(pos);
                    }
                }
                catch (IOException e) {
                    // TODO: This isn't actually good enough...
                    // Means something fucked up, and will fail...
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    private void readHeader() throws IOException {
        if (masterSAT != null) {
            return;
        }

        if (!canRead(input, false)) {
            throw new CorruptDocumentException("Not an OLE 2 Compound Document");
        }

        // UID (seems to be all 0s)
        uUID = new UUID(input.readLong(), input.readLong());
//        System.out.println("uUID: " + uUID);

        // int version =
        input.readUnsignedShort();
//        System.out.println("version: " + version);
        // int revision =
        input.readUnsignedShort();
//        System.out.println("revision: " + revision);

        int byteOrder = input.readUnsignedShort();
//        System.out.printf("byteOrder: 0x%04x\n", byteOrder);
        if (byteOrder == 0xffff) {
            throw new CorruptDocumentException("Cannot read big endian OLE 2 Compound Documents");
        }
        else if (byteOrder != 0xfffe) {
            // Reversed, as I'm already reading little-endian
            throw new CorruptDocumentException(String.format("Unknown byte order marker: 0x%04x, expected 0xfffe or 0xffff", byteOrder));
        }

        sectorSize = 1 << input.readUnsignedShort();
//        System.out.println("sectorSize: " + sectorSize + " bytes");
        shortSectorSize = 1 << input.readUnsignedShort();
//        System.out.println("shortSectorSize: " + shortSectorSize + " bytes");

        // Reserved
        if (skipBytesFully(10) != 10) {
            throw new CorruptDocumentException();
        }

        int SATSize = input.readInt();
//        System.out.println("normalSATSize: " + SATSize);

        directorySId = input.readInt();
//        System.out.println("directorySId: " + directorySId);

        // Reserved
        if (skipBytesFully(4) != 4) {
            throw new CorruptDocumentException();
        }

        minStreamSize = input.readInt();
//        System.out.println("minStreamSize: " + minStreamSize + " bytes");

        shortSATSId = input.readInt();
//        System.out.println("shortSATSId: " + shortSATSId);
        shortSATSize = input.readInt();
//        System.out.println("shortSATSize: " + shortSATSize);
        int masterSATSId = input.readInt();
//        System.out.println("masterSATSId: " + masterSATSId);
        int masterSATSize = input.readInt();
//        System.out.println("masterSATSize: " + masterSATSize);

        // Read masterSAT: 436 bytes, containing up to 109 SIDs
        //System.out.println("MSAT:");
        masterSAT = new int[SATSize];
        final int headerSIds = Math.min(SATSize, 109);
        for (int i = 0; i < headerSIds; i++) {
            masterSAT[i] = input.readInt();
            //System.out.println("\tSID(" + i + "): " + masterSAT[i]);
        }

        if (masterSATSId == END_OF_CHAIN_SID) {
            // End of chain
            int freeSIdLength = 436 - (SATSize * 4);
            if (skipBytesFully(freeSIdLength) != freeSIdLength) {
                throw new CorruptDocumentException();
            }
        }
        else {
            // Parse the SIDs in the extended MasterSAT sectors...
            seekToSId(masterSATSId, FREE_SID);

            int index = headerSIds;
            for (int i = 0; i < masterSATSize; i++) {
                for (int j = 0; j < 127; j++) {
                    int sid = input.readInt();
                    switch (sid) {
                        case FREE_SID:// Free
                            break;
                        default:
                            masterSAT[index++] = sid;
                            break;
                    }
                }

                int next = input.readInt();
                if (next == END_OF_CHAIN_SID) {// End of chain
                    break;
                }

                seekToSId(next, FREE_SID);
            }
        }
    }

    private int skipBytesFully(final int n) throws IOException {
        int toSkip = n;

        while (toSkip > 0) {
            int skipped = input.skipBytes(n);
            if (skipped <= 0) {
                break;
            }

            toSkip -= skipped;
        }

        return n - toSkip;
    }

    private void readSAT() throws IOException {
        if (SAT != null) {
            return;
        }

        final int intsPerSector = sectorSize / 4;

        // Read the Sector Allocation Table
        SAT = new int[masterSAT.length * intsPerSector];

        for (int i = 0; i < masterSAT.length; i++) {
            seekToSId(masterSAT[i], FREE_SID);

            for (int j = 0; j < intsPerSector; j++) {
                int nextSID = input.readInt();
                int index = (j + (i * intsPerSector));

                SAT[index] = nextSID;
            }
        }

        // Read the short-stream Sector Allocation Table
        SIdChain chain = getSIdChain(shortSATSId, FREE_SID);
        shortSAT = new int[shortSATSize * intsPerSector];
        for (int i = 0; i < shortSATSize; i++) {
            seekToSId(chain.get(i), FREE_SID);

            for (int j = 0; j < intsPerSector; j++) {
                int nextSID = input.readInt();
                int index = (j + (i * intsPerSector));

                shortSAT[index] = nextSID;
            }
        }
    }

    /**
     * Gets the SIdChain for the given stream Id
     *
     * @param pSId        the stream Id
     * @param pStreamSize the size of the stream, or -1 for system control streams
     * @return the SIdChain for the given stream Id
     * @throws IOException if an I/O exception occurs
     */
    private SIdChain getSIdChain(final int pSId, final long pStreamSize) throws IOException {
        SIdChain chain = new SIdChain();

        int[] sat = isShortStream(pStreamSize) ? shortSAT : SAT;

        int sid = pSId;
        while (sid != END_OF_CHAIN_SID && sid != FREE_SID) {
            chain.addSID(sid);
            sid = sat[sid];
        }

        return chain;
    }

    private boolean isShortStream(final long pStreamSize) {
        return pStreamSize != FREE_SID && pStreamSize < minStreamSize;
    }

    /**
     * Seeks to the start pos for the given stream Id
     *
     * @param pSId        the stream Id
     * @param pStreamSize the size of the stream, or -1 for system control streams
     * @throws IOException if an I/O exception occurs
     */
    private void seekToSId(final int pSId, final long pStreamSize) throws IOException {
        long pos;

        if (isShortStream(pStreamSize)) {
            // The short stream is not continuous...
            Entry root = getRootEntry();
            if (shortStreamSIdChain == null) {
                shortStreamSIdChain = getSIdChain(root.startSId, root.streamSize);
            }
            
//            System.err.println("pSId: " + pSId);
            int shortPerSId = sectorSize / shortSectorSize;
//            System.err.println("shortPerSId: " + shortPerSId);
            int offset = pSId / shortPerSId;
//            System.err.println("offset: " + offset);
            int shortOffset = pSId - (offset * shortPerSId);
//            System.err.println("shortOffset: " + shortOffset);
//            System.err.println("shortStreamSIdChain.offset: " + shortStreamSIdChain.get(offset));

            pos = HEADER_SIZE
                    + (shortStreamSIdChain.get(offset) * (long) sectorSize)
                    + (shortOffset * (long) shortSectorSize);
//            System.err.println("pos: " + pos);
        }
        else {
            pos = HEADER_SIZE + pSId * (long) sectorSize;
        }

        if (input instanceof LittleEndianRandomAccessFile) {
            ((LittleEndianRandomAccessFile) input).seek(pos);
        }
        else if (input instanceof ImageInputStream) {
            ((ImageInputStream) input).seek(pos);
        }
        else {
            ((SeekableLittleEndianDataInputStream) input).seek(pos);
        }
    }

    private void seekToDId(final int pDId) throws IOException {
        if (directorySIdChain == null) {
            directorySIdChain = getSIdChain(directorySId, FREE_SID);
        }

        int dIdsPerSId = sectorSize / Entry.LENGTH;

        int sIdOffset = pDId / dIdsPerSId;
        int dIdOffset = pDId - (sIdOffset * dIdsPerSId);

        int sId = directorySIdChain.get(sIdOffset);

        seekToSId(sId, FREE_SID);
        if (input instanceof LittleEndianRandomAccessFile) {
            LittleEndianRandomAccessFile input = (LittleEndianRandomAccessFile) this.input;
            input.seek(input.getFilePointer() + dIdOffset * Entry.LENGTH);
        }
        else if (input instanceof ImageInputStream) {
            ImageInputStream input = (ImageInputStream) this.input;
            input.seek(input.getStreamPosition() + dIdOffset * Entry.LENGTH);
        }
        else {
            SeekableLittleEndianDataInputStream input = (SeekableLittleEndianDataInputStream) this.input;
            input.seek(input.getStreamPosition() + dIdOffset * Entry.LENGTH);
        }
    }

    SeekableInputStream getInputStreamForSId(final int pStreamId, final int pStreamSize) throws IOException {
        SIdChain chain = getSIdChain(pStreamId, pStreamSize);

        // TODO: Detach? Means, we have to copy to a byte buffer, or keep track of
        // positions, and seek back and forth (would be cool, but difficult)..
        int sectorSize = pStreamSize < minStreamSize ? shortSectorSize : this.sectorSize;

        return new MemoryCacheSeekableStream(new Stream(chain, pStreamSize, sectorSize, this));
    }

    private InputStream getDirectoryStreamForDId(final int pDirectoryId) throws IOException {
        // This is always exactly 128 bytes, so we'll just read it all,
        // and buffer (we might want to optimize this later).
        byte[] bytes = new byte[Entry.LENGTH];

        seekToDId(pDirectoryId);
        input.readFully(bytes);

        return new ByteArrayInputStream(bytes);
    }

    Entry getEntry(final int pDirectoryId, Entry pParent) throws IOException {
        Entry entry = Entry.readEntry(new LittleEndianDataInputStream(
                getDirectoryStreamForDId(pDirectoryId)
        ));
        entry.parent = pParent;
        entry.document = this;
        return entry;
    }

    SortedSet<Entry> getEntries(final int pDirectoryId, final Entry pParent)
            throws IOException {
        return getEntriesRecursive(pDirectoryId, pParent, new TreeSet<Entry>());
    }

    private SortedSet<Entry> getEntriesRecursive(final int pDirectoryId, final Entry pParent, final SortedSet<Entry> pEntries)
            throws IOException {

        //System.out.println("pDirectoryId: " + pDirectoryId);

        Entry entry = getEntry(pDirectoryId, pParent);

        //System.out.println("entry: " + entry);

        if (!pEntries.add(entry)) {
            // TODO: This occurs in some Thumbs.db files, and Windows will
            // still parse the file gracefully somehow...
            // Deleting and regenerating the file will remove the cyclic
            // references, but... How can Windows parse this file?
            throw new CorruptDocumentException("Cyclic chain reference for entry: " + pDirectoryId);
        }

        if (entry.prevDId != FREE_SID) {
            //System.out.println("prevDId: " + entry.prevDId);
            getEntriesRecursive(entry.prevDId, pParent, pEntries);
        }
        if (entry.nextDId != FREE_SID) {
            //System.out.println("nextDId: " + entry.nextDId);
            getEntriesRecursive(entry.nextDId, pParent, pEntries);
        }

        return pEntries;
    }

    /*public*/ Entry getEntry(String pPath) throws IOException {
        if (StringUtil.isEmpty(pPath) || !pPath.startsWith("/")) {
            throw new IllegalArgumentException("Path must be absolute, and contain a valid path: " + pPath);
        }

        Entry entry = getRootEntry();
        if (pPath.equals("/")) {
            // '/' means root entry
            return entry;
        }
        else {
            // Otherwise get children recursively:
            String[] pathElements = StringUtil.toStringArray(pPath, "/");
            for (String pathElement : pathElements) {
                entry = entry.getChildEntry(pathElement);

                // No such child...
                if (entry == null) {
                    break;// TODO: FileNotFoundException? Should behave like Entry.getChildEntry!!
                }
            }
            return entry;
        }
    }

    public Entry getRootEntry() throws IOException {
        if (rootEntry == null) {
            readSAT();

            rootEntry = getEntry(0, null);

            if (rootEntry.type != Entry.ROOT_STORAGE) {
                throw new CorruptDocumentException("Invalid root storage type: " + rootEntry.type);
            }
        }

        return rootEntry;
    }

    // This is useless, as most documents on file have all-zero UUIDs...
//    @Override
//    public int hashCode() {
//        return uUID.hashCode();
//    }
//
//    @Override
//    public boolean equals(final Object pOther) {
//        if (pOther == this) {
//            return true;
//        }
//
//        if (pOther == null) {
//            return true;
//        }
//
//        if (pOther.getClass() == getClass()) {
//            return uUID.equals(((CompoundDocument) pOther).uUID);
//        }
//
//        return false;
//    }

    @Override
    public String toString() {
        return String.format(
                "%s[uuid: %s, sector size: %d/%d bytes, directory SID: %d, master SAT: %s entries]", 
                getClass().getSimpleName(), uUID, sectorSize, shortSectorSize, directorySId, masterSAT.length
        );
    }

    /**
     * Converts the given time stamp to standard Java time representation,
     * milliseconds since January 1, 1970.
     * The time stamp parameter is assumed to be in units of
     * 100 nano seconds since January 1, 1601.
     * <p/>
     * If the timestamp is {@code 0L} (meaning not specified), no conversion
     * is done, to behave like {@code java.io.File}.
     *
     * @param pMSTime an unsigned long value representing the time stamp (in
     * units of 100 nano seconds since January 1, 1601).
     *
     * @return the time stamp converted to Java time stamp in milliseconds,
     * or {@code 0L} if {@code pMSTime == 0L}
     */
    public static long toJavaTimeInMillis(final long pMSTime) {
        // NOTE: The time stamp field is an unsigned 64-bit integer value that
        // contains the time elapsed since 1601-Jan-01 00:00:00 (Gregorian
        // calendar).
        // One unit of this value is equal to 100 nanoseconds).
        // That means, each second the time stamp value will be increased by
        // 10 million units.

        if (pMSTime == 0L) {
            return 0L; // This is just less confusing...
        }

        // Convert to milliseconds (signed),
        // then convert to Java std epoch (1970-Jan-01 00:00:00)
        return ((pMSTime >> 1) / 5000) + EPOCH_OFFSET;
    }

    static class Stream extends InputStream {
        private final SIdChain chain;
        private final CompoundDocument document;
        private final long length;

        private long streamPos;
        private int nextSectorPos;
        private byte[] buffer;
        private int bufferPos;

        public Stream(SIdChain chain, int streamSize, int sectorSize, CompoundDocument document) {
            this.chain = chain;
            this.length = streamSize;

            this.buffer = new byte[sectorSize];
            this.bufferPos = buffer.length;

            this.document = document;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(buffer.length - bufferPos, length - streamPos);
        }

        public int read() throws IOException {
            if (available() <= 0) {
                if (!fillBuffer()) {
                    return -1;
                }
            }

            streamPos++;

            return buffer[bufferPos++] & 0xff;
        }

        private boolean fillBuffer() throws IOException {
            if (streamPos < length && nextSectorPos < chain.length()) {
                // TODO: Sync on document.input here, and we are completely detached... :-)
                // TODO: Update: We also need to sync other places... :-P
                synchronized (document) {
                    document.seekToSId(chain.get(nextSectorPos), length);
                    document.input.readFully(buffer);
                }

                nextSectorPos++;
                bufferPos = 0;

                return true;
            }

            return false;
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            if (available() <= 0) {
                if (!fillBuffer()) {
                    return -1;
                }
            }

            int toRead = Math.min(len, available());

            System.arraycopy(buffer, bufferPos, b, off, toRead);
            bufferPos += toRead;
            streamPos += toRead;

            return toRead;
        }

        @Override
        public void close() throws IOException {
            buffer = null;
        }
    }

    static class SeekableLittleEndianDataInputStream extends LittleEndianDataInputStream implements Seekable {
        private final SeekableInputStream seekable;

        public SeekableLittleEndianDataInputStream(final SeekableInputStream pInput) {
            super(pInput);
            seekable = pInput;
        }

        public void seek(final long pPosition) throws IOException {
            seekable.seek(pPosition);
        }

        public boolean isCachedFile() {
            return seekable.isCachedFile();
        }

        public boolean isCachedMemory() {
            return seekable.isCachedMemory();
        }

        public boolean isCached() {
            return seekable.isCached();
        }

        public long getStreamPosition() throws IOException {
            return seekable.getStreamPosition();
        }

        public long getFlushedPosition() throws IOException {
            return seekable.getFlushedPosition();
        }

        public void flushBefore(final long pPosition) throws IOException {
            seekable.flushBefore(pPosition);
        }

        public void flush() throws IOException {
            seekable.flush();
        }

        @Override
        public void reset() throws IOException {
            seekable.reset();
        }

        public void mark() {
            seekable.mark();
        }
    }
}
