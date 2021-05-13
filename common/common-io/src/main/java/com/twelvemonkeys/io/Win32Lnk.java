/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.io;

import java.io.*;
import java.util.Arrays;

/**
 * A {@code File} implementation that resolves the Windows {@code .lnk} files as symbolic links.
 * <p>
 * This class is based on example code from
 * <a href="http://www.oreilly.com/catalog/swinghks/index.html">Swing Hacks</a>,
 * By Joshua Marinacci, Chris Adamson (O'Reilly, ISBN: 0-596-00907-0), Hack 30.
 * </p>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/Win32Lnk.java#2 $
 */
final class Win32Lnk extends File {
    private final static byte[] LNK_MAGIC = {
            'L', 0x00, 0x00, 0x00, // Magic
    };
    private final static byte[] LNK_GUID = {
            0x01, 0x14, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, // Shell Link GUID
            (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 'F'
    };

    private final File target;

    private static final int FLAG_ITEM_ID_LIST = 0x01;
    private static final int FLAG_FILE_LOC_INFO = 0x02;
    private static final int FLAG_DESC_STRING = 0x04;
    private static final int FLAG_REL_PATH_STRING = 0x08;
    private static final int FLAG_WORKING_DIRECTORY = 0x10;
    private static final int FLAG_COMMAND_LINE_ARGS = 0x20;
    private static final int FLAG_ICON_FILENAME = 0x40;
    private static final int FLAG_ADDITIONAL_INFO = 0x80;

    private Win32Lnk(final String pPath) throws IOException {
        super(pPath);
        File target = parse(this);
        if (target == this) {
            // NOTE: This is a workaround
            // target = this causes infinite loops in some methods
            target = new File(pPath);
        }
        this.target = target;
    }

    Win32Lnk(final File pPath) throws IOException {
        this(pPath.getPath());
    }

    /**
     * Parses a {@code .lnk} file to find the real file.
     *
     * @param pPath the path to the {@code .lnk} file
     * @return a new file object that
     * @throws java.io.IOException if the {@code .lnk} cannot be parsed
     */
    static File parse(final File pPath) throws IOException {
        if (!pPath.getName().endsWith(".lnk")) {
            return pPath;
        }

        File result = pPath;

        LittleEndianDataInputStream in = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(pPath)));
        try {
            byte[] magic = new byte[4];
            in.readFully(magic);

            byte[] guid = new byte[16];
            in.readFully(guid);

            if (!(Arrays.equals(LNK_MAGIC, magic) && Arrays.equals(LNK_GUID, guid))) {
                //System.out.println("Not a symlink");
                // Not a symlink
                return pPath;
            }

            // Get the flags
            int flags = in.readInt();
            //System.out.println("flags: " + Integer.toBinaryString(flags & 0xff));

            // Get to the file settings
            /*int attributes = */in.readInt();

            // File attributes
            // 0 Target is read only.
            // 1 Target is hidden.
            // 2 Target is a system file.
            // 3 Target is a volume label. (Not possible)
            // 4 Target is a directory.
            // 5 Target has been modified since last backup. (archive)
            // 6 Target is encrypted (NTFS EFS)
            // 7 Target is Normal??
            // 8 Target is temporary.
            // 9 Target is a sparse file.
            // 10 Target has reparse point data.
            // 11 Target is compressed.
            // 12 Target is offline.
            //System.out.println("attributes: " + Integer.toBinaryString(attributes));
            // NOTE: Cygwin .lnks are not directory links, can't rely on this.. :-/

            in.skipBytes(48); // TODO: Make sense of this data...

            // Skipped data:
            // long time 1 (creation)
            // long time 2 (modification)
            // long time 3 (last access)
            // int file length
            // int icon number
            // int ShowVnd value
            // int hotkey
            // int, int - unknown: 0,0

            // If the shell settings are present, skip them
            if ((flags & FLAG_ITEM_ID_LIST) != 0) {
                // Shell Item Id List present
                //System.out.println("Shell Item Id List present");
                int shellLen = in.readShort(); // Short
                //System.out.println("shellLen: " + shellLen);

                // TODO: Probably need to parse this data, to determine
                // Cygwin folders...

                /*
                int read = 2;
                int itemLen = in.readShort();
                while (itemLen > 0) {
                    System.out.println("--> ITEM: " + itemLen);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(new SubStream(in, itemLen - 2)));
                    //byte[] itemBytes = new byte[itemLen - 2]; // NOTE: Lenght included
                    //in.readFully(itemBytes);

                    String item = reader.readLine();
                    System.out.println("item: \"" + item + "\"");

                    itemLen = in.readShort();
                    read += itemLen;
                }

                System.out.println("read: " + read);
                */

                in.skipBytes(shellLen);
            }

            if ((flags & FLAG_FILE_LOC_INFO) != 0) {
                // File Location Info Table present
                //System.out.println("File Location Info Table present");

                // 0h 1 dword This is the total length of this structure and all following data
                // 4h 1 dword This is a pointer to first offset after this structure. 1Ch
                // 8h 1 dword Flags
                // Ch 1 dword Offset of local volume info
                // 10h 1 dword Offset of base pathname on local system
                // 14h 1 dword Offset of network volume info
                // 18h 1 dword Offset of remaining pathname

                // Flags:
                // Bit Meaning
                // 0 Available on a local volume
                // 1 Available on a network share
                // TODO: Make sure the path is on a local disk, etc..

                int tableLen = in.readInt(); // Int
                //System.out.println("tableLen: " + tableLen);

                in.readInt(); // Skip

                int locFlags = in.readInt();
                //System.out.println("locFlags: " + Integer.toBinaryString(locFlags));
                if ((locFlags & 0x01) != 0) {
                    //System.out.println("Available local");
                }
                if ((locFlags & 0x02) != 0) {
                    //System.err.println("Available on network path");
                }

                // Get the local volume and local system values
                in.skipBytes(4); // TODO: see above for structure

                int localSysOff = in.readInt();
                //System.out.println("localSysOff: " + localSysOff);
                in.skipBytes(localSysOff - 20);  // Relative to start of chunk

                byte[] pathBytes = new byte[tableLen - localSysOff - 1];
                in.readFully(pathBytes, 0, pathBytes.length);
                String path = new String(pathBytes, 0, pathBytes.length - 1);
                /*
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte read;
                // Read bytes until the null (0) character
                while (true) {
                    read = in.readByte();
                    if (read == 0) {
                        break;
                    }
                    bytes.write(read & 0xff);
                }

                String path = new String(bytes.toByteArray(), 0, bytes.size());
                //*/

                // Recurse to end of link chain
                // TODO: This may cause endless loop if cyclic chain...
                //System.out.println("path: \"" + path + "\"");
                try {
                    result = parse(new File(path));
                }
                catch (StackOverflowError e) {
                    throw new IOException("Cannot resolve cyclic link: " + e.getMessage());
                }
            }

            if ((flags & FLAG_DESC_STRING) != 0) {
                // Description String present, skip it.
                //System.out.println("Description String present");

                // The string length is the first word which must also be skipped.
                int descLen = in.readShort();
                //System.out.println("descLen: " + descLen);

                byte[] descBytes = new byte[descLen];
                in.readFully(descBytes, 0, descLen);

                //String desc = new String(descBytes, 0, descLen);
                //System.out.println("desc: " + desc);
            }

            if ((flags & FLAG_REL_PATH_STRING) != 0) {
                // Relative Path String present
                //System.out.println("Relative Path String present");

                // The string length is the first word which must also be skipped.
                int pathLen = in.readShort();
                //System.out.println("pathLen: " + pathLen);

                byte[] pathBytes = new byte[pathLen];
                in.readFully(pathBytes, 0, pathLen);

                String path = new String(pathBytes, 0, pathLen);

                // TODO: This may cause endless loop if cyclic chain...
                //System.out.println("path: \"" + path + "\"");
                if (result == pPath) {
                    try {
                        result = parse(new File(pPath.getParentFile(), path));
                    }
                    catch (StackOverflowError e) {
                        throw new IOException("Cannot resolve cyclic link: " + e.getMessage());
                    }
                }
            }

            if ((flags & FLAG_WORKING_DIRECTORY) != 0) {
                //System.out.println("Working Directory present");
            }
            if ((flags & FLAG_COMMAND_LINE_ARGS) != 0) {
                //System.out.println("Command Line Arguments present");
                // NOTE: This means this .lnk is not a folder, don't follow
                result = pPath;
            }
            if ((flags & FLAG_ICON_FILENAME) != 0) {
                //System.out.println("Icon Filename present");
            }
            if ((flags & FLAG_ADDITIONAL_INFO) != 0) {
                //System.out.println("Additional Info present");
            }
        }
        finally {
            in.close();
        }

        return result;
    }

    /*
    private static String getNullDelimitedString(byte[] bytes, int off) {
        int len = 0;
        // Count bytes until the null (0) character
        while (true) {
            if (bytes[off + len] == 0) {
                break;
            }
            len++;
        }

        System.err.println("--> " + len);

        return new String(bytes, off, len);
    }
    */

    /**
     * Converts two bytes into a short.
     * <p>
     * NOTE: this is little endian because it's for an
     * Intel only OS
     * </p>
     *
     * @ param bytes
     * @ param off
     * @return the bytes as a short.
     */
    /*
    private static int bytes2short(byte[] bytes, int off) {
        return ((bytes[off + 1] & 0xff) << 8) | (bytes[off] & 0xff);
    }
    */

    public File getTarget() {
        return target;
    }

    // java.io.File overrides below

    @Override
    public boolean isDirectory() {
        return target.isDirectory();
    }

    @Override
    public boolean canRead() {
        return target.canRead();
    }

    @Override
    public boolean canWrite() {
        return target.canWrite();
    }

    // NOTE: equals is implemented using compareto == 0
    /*
    public int compareTo(File pathname) {
        // TODO: Verify this
        // Probably not a good idea, as it IS NOT THE SAME file
        // It's probably better to not override
        return target.compareTo(pathname);
    }
    */

    // Should probably never allow creating a new .lnk
    // public boolean createNewFile() throws IOException

    // Deletes only the .lnk
    // public boolean delete() {
    //public void deleteOnExit() {

    @Override
    public boolean exists() {
        return target.exists();
    }

    // A .lnk may be absolute
    //public File getAbsoluteFile() {
    //public String getAbsolutePath() {

    // Theses should be resolved according to the API (for Unix).
    @Override
    public File getCanonicalFile() throws IOException {
        return target.getCanonicalFile();
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return target.getCanonicalPath();
    }

    //public String getName() {

    // I guess the parent should be the parent of the .lnk, not the target
    //public String getParent() {
    //public File getParentFile() {

    // public boolean isAbsolute() {
    @Override
    public boolean isFile() {
        return target.isFile();
    }

    @Override
    public boolean isHidden() {
        return target.isHidden();
    }

    @Override
    public long lastModified() {
        return target.lastModified();
    }

    @Override
    public long length() {
        return target.length();
    }

    @Override
    public String[] list() {
        return target.list();
    }

    @Override
    public String[] list(final FilenameFilter filter) {
        return target.list(filter);
    }

    @Override
    public File[] listFiles() {
        return Win32File.wrap(target.listFiles());
    }

    @Override
    public File[] listFiles(final FileFilter filter) {
        return Win32File.wrap(target.listFiles(filter));
    }

    @Override
    public File[] listFiles(final FilenameFilter filter) {
        return Win32File.wrap(target.listFiles(filter));
    }

    // Makes no sense, does it?
    //public boolean mkdir() {
    //public boolean mkdirs() {

    // Only rename the lnk
    //public boolean renameTo(File dest) {

    @Override
    public boolean setLastModified(long time) {
        return target.setLastModified(time);
    }

    @Override
    public boolean setReadOnly() {
        return target.setReadOnly();
    }

    @Override
    public String toString() {
        if (target.equals(this)) {
            return super.toString();
        }
        return super.toString() + " -> " + target.toString();
    }
}
