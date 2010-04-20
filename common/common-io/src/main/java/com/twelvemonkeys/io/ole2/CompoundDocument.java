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
    
    private static final byte[] MAGIC = new byte[]{
            (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1,
    };
    public static final int HEADER_SIZE = 512;

    private final DataInput mInput;

    private UUID mUID;

    private int mSectorSize;
    private int mShortSectorSize;

    private int mDirectorySId;

    private int mMinStreamSize;

    private int mShortSATSID;
    private int mShortSATSize;

    // Master Sector Allocation Table
    private int[] mMasterSAT;
    private int[] mSAT;
    private int[] mShortSAT;

    private Entry mRootEntry;
    private SIdChain mShortStreamSIdChain;
    private SIdChain mDirectorySIdChain;

    private static final int END_OF_CHAIN_SID = -2;
    private static final int FREE_SID = -1;

    /** The epoch offset of CompoundDocument time stamps */
    public final static long EPOCH_OFFSET = -11644477200000L;

    /**
     * Creates a (for now) read only {@code CompoundDocument}.
     *
     * @param pFile the file to read from
     *
     * @throws IOException if an I/O exception occurs while reading the header
     */
    public CompoundDocument(final File pFile) throws IOException {
        mInput = new LittleEndianRandomAccessFile(FileUtil.resolve(pFile), "r");

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
        mInput = new SeekableLittleEndianDataInputStream(pInput);

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
        mInput = pInput;

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
        if (mMasterSAT != null) {
            return;
        }

        if (!canRead(mInput, false)) {
            throw new CorruptDocumentException("Not an OLE 2 Compound Document");
        }

        // UID (seems to be all 0s)
        mUID = new UUID(mInput.readLong(), mInput.readLong());

        /*int version = */mInput.readUnsignedShort();
        //System.out.println("version: " + version);
        /*int revision = */mInput.readUnsignedShort();
        //System.out.println("revision: " + revision);

        int byteOrder = mInput.readUnsignedShort();
        if (byteOrder != 0xfffe) {
            // Reversed, as I'm allready reading little-endian
            throw new CorruptDocumentException("Cannot read big endian OLE 2 Compound Documents");
        }

        mSectorSize = 1 << mInput.readUnsignedShort();
        //System.out.println("sectorSize: " + mSectorSize + " bytes");
        mShortSectorSize = 1 << mInput.readUnsignedShort();
        //System.out.println("shortSectorSize: " + mShortSectorSize + " bytes");

        // Reserved
        if (mInput.skipBytes(10) != 10) {
            throw new CorruptDocumentException();
        }

        int SATSize = mInput.readInt();
        //System.out.println("normalSATSize: " + mSATSize);

        mDirectorySId = mInput.readInt();
        //System.out.println("directorySId: " + mDirectorySId);

        // Reserved
        if (mInput.skipBytes(4) != 4) {
            throw new CorruptDocumentException();
        }

        mMinStreamSize = mInput.readInt();
        //System.out.println("minStreamSize: " + mMinStreamSize + " bytes");

        mShortSATSID = mInput.readInt();
        //System.out.println("shortSATSID: " + mShortSATSID);
        mShortSATSize = mInput.readInt();
        //System.out.println("shortSATSize: " + mShortSATSize);
        int masterSATSId = mInput.readInt();
        //System.out.println("masterSATSId: " + mMasterSATSID);
        int masterSATSize = mInput.readInt();
        //System.out.println("masterSATSize: " + mMasterSATSize);

        // Read masterSAT: 436 bytes, containing up to 109 SIDs
        //System.out.println("MSAT:");
        mMasterSAT = new int[SATSize];
        final int headerSIds = Math.min(SATSize, 109);
        for (int i = 0; i < headerSIds; i++) {
            mMasterSAT[i] = mInput.readInt();
            //System.out.println("\tSID(" + i + "): " + mMasterSAT[i]);
        }

        if (masterSATSId == END_OF_CHAIN_SID) {
            // End of chain
            int freeSIdLength = 436 - (SATSize * 4);
            if (mInput.skipBytes(freeSIdLength) != freeSIdLength) {
                throw new CorruptDocumentException();
            }
        }
        else {
            // Parse the SIDs in the extended MasterSAT sectors...
            seekToSId(masterSATSId, FREE_SID);

            int index = headerSIds;
            for (int i = 0; i < masterSATSize; i++) {
                for (int j = 0; j < 127; j++) {
                    int sid = mInput.readInt();
                    switch (sid) {
                        case FREE_SID:// Free
                            break;
                        default:
                            mMasterSAT[index++] = sid;
                            break;
                    }
                }

                int next = mInput.readInt();
                if (next == END_OF_CHAIN_SID) {// End of chain
                    break;
                }

                seekToSId(next, FREE_SID);
            }
        }
    }

    private void readSAT() throws IOException {
        if (mSAT != null) {
            return;
        }

        final int intsPerSector = mSectorSize / 4;

        // Read the Sector Allocation Table
        mSAT = new int[mMasterSAT.length * intsPerSector];

        for (int i = 0; i < mMasterSAT.length; i++) {
            seekToSId(mMasterSAT[i], FREE_SID);

            for (int j = 0; j < intsPerSector; j++) {
                int nextSID = mInput.readInt();
                int index = (j + (i * intsPerSector));

                mSAT[index] = nextSID;
            }
        }

        // Read the short-stream Sector Allocation Table
        SIdChain chain = getSIdChain(mShortSATSID, FREE_SID);
        mShortSAT = new int[mShortSATSize * intsPerSector];
        for (int i = 0; i < mShortSATSize; i++) {
            seekToSId(chain.get(i), FREE_SID);

            for (int j = 0; j < intsPerSector; j++) {
                int nextSID = mInput.readInt();
                int index = (j + (i * intsPerSector));

                mShortSAT[index] = nextSID;
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

        int[] sat = isShortStream(pStreamSize) ? mShortSAT : mSAT;

        int sid = pSId;
        while (sid != END_OF_CHAIN_SID && sid != FREE_SID) {
            chain.addSID(sid);
            sid = sat[sid];
        }

        return chain;
    }

    private boolean isShortStream(final long pStreamSize) {
        return pStreamSize != FREE_SID && pStreamSize < mMinStreamSize;
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
            // The short-stream is not continouos...
            Entry root = getRootEntry();
            if (mShortStreamSIdChain == null) {
                mShortStreamSIdChain = getSIdChain(root.startSId, root.streamSize);
            }

            int shortPerStd = mSectorSize / mShortSectorSize;
            int offset = pSId / shortPerStd;
            int shortOffset = pSId - (offset * shortPerStd);

            pos = HEADER_SIZE
                    + (mShortStreamSIdChain.get(offset) * (long) mSectorSize)
                    + (shortOffset * (long) mShortSectorSize);
        }
        else {
            pos = HEADER_SIZE + pSId * (long) mSectorSize;
        }

        if (mInput instanceof LittleEndianRandomAccessFile) {
            ((LittleEndianRandomAccessFile) mInput).seek(pos);
        }
        else if (mInput instanceof ImageInputStream) {
            ((ImageInputStream) mInput).seek(pos);
        }
        else {
            ((SeekableLittleEndianDataInputStream) mInput).seek(pos);
        }
    }

    private void seekToDId(final int pDId) throws IOException {
        if (mDirectorySIdChain == null) {
            mDirectorySIdChain = getSIdChain(mDirectorySId, FREE_SID);
        }

        int dIdsPerSId = mSectorSize / Entry.LENGTH;

        int sIdOffset = pDId / dIdsPerSId;
        int dIdOffset = pDId - (sIdOffset * dIdsPerSId);

        int sId = mDirectorySIdChain.get(sIdOffset);

        seekToSId(sId, FREE_SID);
        if (mInput instanceof LittleEndianRandomAccessFile) {
            LittleEndianRandomAccessFile input = (LittleEndianRandomAccessFile) mInput;
            input.seek(input.getFilePointer() + dIdOffset * Entry.LENGTH);
        }
        else if (mInput instanceof ImageInputStream) {
            ImageInputStream input = (ImageInputStream) mInput;
            input.seek(input.getStreamPosition() + dIdOffset * Entry.LENGTH);
        }
        else {
            SeekableLittleEndianDataInputStream input = (SeekableLittleEndianDataInputStream) mInput;
            input.seek(input.getStreamPosition() + dIdOffset * Entry.LENGTH);
        }
    }

    SeekableInputStream  getInputStreamForSId(final int pStreamId, final int pStreamSize) throws IOException {
        SIdChain chain = getSIdChain(pStreamId, pStreamSize);

        // TODO: Detach? Means, we have to copy to a byte buffer, or keep track of
        // positions, and seek back and forth (would be cool, but difficult)..
        int sectorSize = pStreamSize < mMinStreamSize ? mShortSectorSize : mSectorSize;

        return new Stream(chain, pStreamSize, sectorSize, this);
    }

    private InputStream getDirectoryStreamForDId(final int pDirectoryId) throws IOException {
        // This is always exactly 128 bytes, so we'll just read it all,
        // and buffer (we might want to optimize this later).
        byte[] bytes = new byte[Entry.LENGTH];

        seekToDId(pDirectoryId);
        mInput.readFully(bytes);

        return new ByteArrayInputStream(bytes);
    }

    Entry getEntry(final int pDirectoryId, Entry pParent) throws IOException {
        Entry entry = Entry.readEntry(new LittleEndianDataInputStream(
                getDirectoryStreamForDId(pDirectoryId)
        ));
        entry.mParent = pParent;
        entry.mDocument = this;
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
        if (mRootEntry == null) {
            readSAT();

            mRootEntry = getEntry(0, null);

            if (mRootEntry.type != Entry.ROOT_STORAGE) {
                throw new CorruptDocumentException("Invalid root storage type: " + mRootEntry.type);
            }
        }
        return mRootEntry;
    }

//    @Override
//    public int hashCode() {
//        return mUID.hashCode();
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
//            return mUID.equals(((CompoundDocument) pOther).mUID);
//        }
//
//        return false;
//    }

    @Override
    public String toString() {
        return String.format(
                "%s[uuid: %s, sector size: %d/%d bytes, directory SID: %d, master SAT: %s entries]", 
                getClass().getSimpleName(), mUID, mSectorSize, mShortSectorSize, mDirectorySId, mMasterSAT.length
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

    // TODO: Enforce stream length!
    static class Stream extends SeekableInputStream {
        private SIdChain mChain;
        int mNextSectorPos;
        byte[] mBuffer;
        int mBufferPos;

        private final CompoundDocument mDocument;
        private final long mLength;

        public Stream(final SIdChain pChain, final long pLength, final int pSectorSize, final CompoundDocument pDocument) {
            mChain = pChain;
            mLength = pLength;

            mBuffer = new byte[pSectorSize];
            mBufferPos = mBuffer.length;

            mDocument = pDocument;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(mBuffer.length - mBufferPos, mLength - getStreamPosition());
        }

        public int read() throws IOException {
            if (available() <= 0) {
                if (!fillBuffer()) {
                    return -1;
                }
            }

            return mBuffer[mBufferPos++] & 0xff;
        }

        private boolean fillBuffer() throws IOException {
            if (mNextSectorPos < mChain.length()) {
                // TODO: Sync on mDocument.mInput here, and we are completely detached... :-)
                // TODO: We also need to sync other places...
                synchronized (mDocument) {
                    mDocument.seekToSId(mChain.get(mNextSectorPos), mLength);
                    mDocument.mInput.readFully(mBuffer);
                }

                mNextSectorPos++;
                mBufferPos = 0;
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

            System.arraycopy(mBuffer, mBufferPos, b, off, toRead);
            mBufferPos += toRead;

            return toRead;
        }

        public boolean isCached() {
            return true;
        }

        public boolean isCachedMemory() {
            return false;
        }

        public boolean isCachedFile() {
            return true;
        }

        protected void closeImpl() throws IOException {
            mBuffer = null;
            mChain = null;
        }

        protected void seekImpl(final long pPosition) throws IOException {
            long pos = getStreamPosition();

            if (pos - mBufferPos >= pPosition && pPosition <= pos + available()) {
                // Skip inside buffer only
                mBufferPos += (pPosition - pos);
            }
            else {
                // Skip outside buffer
                mNextSectorPos = (int) (pPosition / mBuffer.length);
                if (!fillBuffer()) {
                    throw new EOFException();
                }
                mBufferPos = (int) (pPosition % mBuffer.length);
            }
        }

        protected void flushBeforeImpl(long pPosition) throws IOException {
            // No need to do anything here
        }
    }

    // TODO: Add test case for this class!!!
    static class SeekableLittleEndianDataInputStream extends LittleEndianDataInputStream implements Seekable {
        private final SeekableInputStream mSeekable;

        public SeekableLittleEndianDataInputStream(final SeekableInputStream pInput) {
            super(pInput);
            mSeekable = pInput;
        }

        public void seek(final long pPosition) throws IOException {
            mSeekable.seek(pPosition);
        }

        public boolean isCachedFile() {
            return mSeekable.isCachedFile();
        }

        public boolean isCachedMemory() {
            return mSeekable.isCachedMemory();
        }

        public boolean isCached() {
            return mSeekable.isCached();
        }

        public long getStreamPosition() throws IOException {
            return mSeekable.getStreamPosition();
        }

        public long getFlushedPosition() throws IOException {
            return mSeekable.getFlushedPosition();
        }

        public void flushBefore(final long pPosition) throws IOException {
            mSeekable.flushBefore(pPosition);
        }

        public void flush() throws IOException {
            mSeekable.flush();
        }

        @Override
        public void reset() throws IOException {
            mSeekable.reset();
        }

        public void mark() {
            mSeekable.mark();
        }
    }
}
