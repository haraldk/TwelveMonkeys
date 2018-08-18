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

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.lang.Validate;
import com.twelvemonkeys.util.Visitor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.text.NumberFormat;

/**
 * A utility class with some useful file and i/o related methods.
 * <P>
 * Versions exists take Input and OutputStreams as parameters, to
 * allow for copying streams (URL's etc.).
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author <A href="mailto:eirik.torske@iconmedialab.no">Eirik Torske</A>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/FileUtil.java#3 $
 */
public final class FileUtil {
    // TODO: Be more cosequent using resolve() all places where File objects are involved
    // TODO: Parameter handling (allow null vs IllegalArgument)
    // TODO: Exception handling
    
    /**
     * The size of the buffer used for copying
     */
    public final static int BUF_SIZE = 1024;
    private static String TEMP_DIR = null;

    private final static FileSystem FS = FileSystem.get();

    public static void main(String[] pArgs) throws IOException {
        File file;
        if (pArgs[0].startsWith("file:")) {
            file = toFile(new URL(pArgs[0]));
            System.out.println(file);
        }
        else {
            file = new File(pArgs[0]);
            System.out.println(file.toURL());
        }

        System.out.println("Free space: " + getFreeSpace(file) + "/" + getTotalSpace(file) + " bytes");
    }

    /*
     * Method main for test only.
     *
    public static void main0(String[] pArgs) {
        if (pArgs.length != 2) {
            System.out.println("usage: java Copy in out");
            return;
        }
        try {
            if (!copy(pArgs[0], pArgs[1])) {
                System.out.println("Error copying");
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    //*/

    // Avoid instances/constructor showing up in API doc
    private FileUtil() {}

    /**
     * Copies the fromFile to the toFile location. If toFile is a directory, a
     * new file is created in that directory, with the name of the fromFile.
     * If the toFile exists, the file will not be copied, unless owerWrite is
     * true.
     *
     * @param pFromFileName The name of the file to copy from
     * @param pToFileName   The name of the file to copy to
     * @return true if the file was copied successfully,
     *         false if the output file exists. In all other cases, an
     *         IOException is thrown, and the method does not return a value.
     * @throws IOException if an i/o error occurs during copy
     */
    public static boolean copy(String pFromFileName, String pToFileName) throws IOException {
        return copy(new File(pFromFileName), new File(pToFileName), false);
    }

    /**
     * Copies the fromFile to the toFile location. If toFile is a directory, a
     * new file is created in that directory, with the name of the fromFile.
     * If the toFile exists, the file will not be copied, unless owerWrite is
     * true.
     *
     * @param pFromFileName The name of the file to copy from
     * @param pToFileName   The name of the file to copy to
     * @param pOverWrite    Specifies if the toFile should be overwritten, if it
     *                      exists.
     * @return true if the file was copied successfully,
     *         false if the output file exists, and the owerWrite parameter is
     *         false. In all other cases, an
     *         IOException is thrown, and the method does not return a value.
     * @throws IOException if an i/o error occurs during copy
     */
    public static boolean copy(String pFromFileName, String pToFileName, boolean pOverWrite) throws IOException {
        return copy(new File(pFromFileName), new File(pToFileName), pOverWrite);
    }

    /**
     * Copies the fromFile to the toFile location. If toFile is a directory, a
     * new file is created in that directory, with the name of the fromFile.
     * If the toFile exists, the file will not be copied, unless owerWrite is
     * true.
     *
     * @param pFromFile The file to copy from
     * @param pToFile   The file to copy to
     * @return true if the file was copied successfully,
     *         false if the output file exists. In all other cases, an
     *         IOException is thrown, and the method does not return a value.
     * @throws IOException if an i/o error occurs during copy
     */
    public static boolean copy(File pFromFile, File pToFile) throws IOException {
        return copy(pFromFile, pToFile, false);
    }

    /**
     * Copies the fromFile to the toFile location. If toFile is a directory, a
     * new file is created in that directory, with the name of the fromFile.
     * If the toFile exists, the file will not be copied, unless owerWrite is
     * true.
     *
     * @param pFromFile  The file to copy from
     * @param pToFile    The file to copy to
     * @param pOverWrite Specifies if the toFile should be overwritten, if it
     *                   exists.
     * @return {@code true} if the file was copied successfully,
     *         {@code false} if the output file exists, and the
     *         {@code pOwerWrite} parameter is
     *         {@code false}. In all other cases, an
     *         {@code IOExceptio}n is thrown, and the method does not return.
     * @throws IOException if an i/o error occurs during copy
     * @todo Test copyDir functionality!
     */
    public static boolean copy(File pFromFile, File pToFile, boolean pOverWrite) throws IOException {
        // Copy all directory structure
        if (pFromFile.isDirectory()) {
            return copyDir(pFromFile, pToFile, pOverWrite);
        }

        // Check if destination is a directory
        if (pToFile.isDirectory()) {
            // Create a new file with same name as from
            pToFile = new File(pToFile, pFromFile.getName());
        }

        // Check if file exists, and return false if overWrite is false
        if (!pOverWrite && pToFile.exists()) {
            return false;
        }

        InputStream in = null;
        OutputStream out = null;

        try {
            // Use buffer size two times byte array, to avoid i/o bottleneck
            in = new FileInputStream(pFromFile);
            out = new FileOutputStream(pToFile);

            // Copy from inputStream to outputStream
            copy(in, out);
        }
        //Just pass any IOException on up the stack
        finally {
            close(in);
            close(out);
        }

        return true;  // If we got here, everything is probably okay.. ;-)
    }

    /**
     * Tries to close the given stream.
     * NOTE: If the stream cannot be closed, the IOException thrown is silently
     * ignored.
     * 
     * @param pInput the stream to close
     */
    public static void close(InputStream pInput) {
        try {
            if (pInput != null) {
                pInput.close();
            }
        }
        catch (IOException ignore) {
            // Non critical error
        }
    }

    /**
     * Tries to close the given stream.
     * NOTE: If the stream cannot be closed, the IOException thrown is silently
     * ignored.
     *
     * @param pOutput the stream to close
     */
    public static void close(OutputStream pOutput) {
        try {
            if (pOutput != null) {
                pOutput.close();
            }
        }
        catch (IOException ignore) {
            // Non critical error
        }
    }

    static void close(Reader pReader) {
        try {
            if (pReader != null) {
                pReader.close();
            }
        }
        catch (IOException ignore) {
            // Non critical error
        }
    }

    static void close(Writer pWriter) {
        try {
            if (pWriter != null) {
                pWriter.close();
            }
        }
        catch (IOException ignore) {
            // Non critical error
        }
    }

    /**
     * Copies a directory recursively. If the destination folder does not exist,
     * it is created
     *
     * @param pFrom the source directory
     * @param pTo the destination directory
     * @param pOverWrite {@code true} if we should allow overwrting existing files
     * @return {@code true} if all files were copied sucessfully
     * @throws IOException if {@code pTo} exists, and it not a directory,
     *          or if copying of any of the files in the folder fails
     */
    private static boolean copyDir(File pFrom, File pTo, boolean pOverWrite) throws IOException {
        if (pTo.exists() && !pTo.isDirectory()) {
            throw new IOException("A directory may only be copied to another directory, not to a file");
        }
        pTo.mkdirs();  // mkdir?
        boolean allOkay = true;
        File[] files = pFrom.listFiles();

        for (File file : files) {
            if (!copy(file, new File(pTo, file.getName()), pOverWrite)) {
                allOkay = false;
            }
        }
        return allOkay;
    }

    /**
     * Copies all data from one stream to another.
     * The data is copied from the fromStream to the toStream using buffered
     * streams for efficiency.
     *
     * @param pFrom The input srteam to copy from
     * @param pTo   The output stream to copy to
     * @return true. Otherwise, an
     *         IOException is thrown, and the method does not return a value.
     * @throws IOException if an i/o error occurs during copy
     * @throws IllegalArgumentException if either {@code pFrom} or {@code pTo} is
     *         {@code null}
     */
    public static boolean copy(InputStream pFrom, OutputStream pTo) throws IOException {
        Validate.notNull(pFrom, "from");
        Validate.notNull(pTo, "to");

        // TODO: Consider using file channels for faster copy where possible

        // Use buffer size two times byte array, to avoid i/o bottleneck
        // TODO: Consider letting the client decide as this is sometimes not a good thing!
        InputStream in = new BufferedInputStream(pFrom, BUF_SIZE * 2);
        OutputStream out = new BufferedOutputStream(pTo, BUF_SIZE * 2);

        byte[] buffer = new byte[BUF_SIZE];
        int count;

        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }

        // Flush out stream, to write any remaining buffered data
        out.flush();

        return true;  // If we got here, everything is probably okay.. ;-)
    }

    /**
     * Gets the file (type) extension of the given file.
     * A file extension is the part of the filename, after the last occurence
     * of a period {@code '.'}.
     * If the filename contains no period, {@code null} is returned.
     *
     * @param pFileName the full filename with extension
     * @return the extension (type) of the file, or {@code null}
     */
    public static String getExtension(final String pFileName) {
        return getExtension0(getFilename(pFileName));
    }

    /**
     * Gets the file (type) extension of the given file.
     * A file extension is the part of the filename, after the last occurence
     * of a period {@code '.'}.
     * If the filename contains no period, {@code null} is returned.
     *
     * @param pFile the file
     * @return the extension (type) of the file, or {@code null}
     */
    public static String getExtension(final File pFile) {
        return getExtension0(pFile.getName());
    }

    // NOTE: Assumes filename and no path
    private static String getExtension0(final String pFileName) {
        int index = pFileName.lastIndexOf('.');

        if (index >= 0) {
            return pFileName.substring(index + 1);
        }

        // No period found
        return null;
    }


    /**
     * Gets the file name of the given file, without the extension (type).
     * A file extension is the part of the filename, after the last occurence
     * of a period {@code '.'}.
     * If the filename contains no period, the complete file name is returned
     * (same as {@code pFileName}, if the string contains no path elements).
     *
     * @param pFileName the full filename with extension
     * @return the base name of the file
     */
    public static String getBasename(final String pFileName) {
        return getBasename0(getFilename(pFileName));
    }

    /**
     * Gets the file name of the given file, without the extension (type).
     * A file extension is the part of the filename, after the last occurence
     * of a period {@code '.'}.
     * If the filename contains no period, {@code pFile.getName()} is returned.
     *
     * @param pFile the file
     * @return the base name of the file
     */
    public static String getBasename(final File pFile) {
        return getBasename0(pFile.getName());
    }

    // NOTE: Assumes filename and no path
    public static String getBasename0(final String pFileName) {
        int index = pFileName.lastIndexOf('.');

        if (index >= 0) {
            return pFileName.substring(0, index);
        }

        // No period found
        return pFileName;
    }

    /**
     * Extracts the directory path without the filename, from a complete
     * filename path.
     *
     * @param pPath The full filename path.
     * @return the path without the filename.
     * @see File#getParent
     * @see #getFilename
     */
    public static String getDirectoryname(final String pPath) {
        return getDirectoryname(pPath, File.separatorChar);
    }

    /**
     * Extracts the directory path without the filename, from a complete
     * filename path.
     *
     * @param pPath The full filename path.
     * @param pSeparator the separator char used in {@code pPath}
     * @return the path without the filename.
     * @see File#getParent
     * @see #getFilename
     */
    public static String getDirectoryname(final String pPath, final char pSeparator) {
        int index = pPath.lastIndexOf(pSeparator);

        if (index < 0) {
            return "";  // Assume only filename
        }
        return pPath.substring(0, index);
    }

    /**
     * Extracts the filename of a complete filename path.
     *
     * @param pPath The full filename path.
     * @return the extracted filename.
     * @see File#getName
     * @see #getDirectoryname
     */
    public static String getFilename(final String pPath) {
        return getFilename(pPath, File.separatorChar);
    }

    /**
     * Extracts the filename of a complete filename path.
     *
     * @param pPath      The full filename path.
     * @param pSeparator The file separator.
     * @return the extracted filename.
     * @see File#getName
     * @see #getDirectoryname
     */
    public static String getFilename(final String pPath, final char pSeparator) {
        int index = pPath.lastIndexOf(pSeparator);

        if (index < 0) {
            return pPath; // Assume only filename
        }

        return pPath.substring(index + 1);
    }


    /**
     * Tests if a file or directory has no content.
     * A file is empty if it has a length of 0L. A non-existing file is also
     * considered empty.
     * A directory is considered empty if it contains no files.
     *
     * @param pFile The file to test
     * @return {@code true} if the file is empty, otherwise
     *         {@code false}.
     */
    public static boolean isEmpty(File pFile) {
        if (pFile.isDirectory()) {
            return (pFile.list().length == 0);
        }
        return (pFile.length() == 0);
    }

    /**
     * Gets the default temp directory for the system as a File.
     *
     * @return a {@code File}, representing the default temp directory.
     * @see File#createTempFile
     */
    public static File getTempDirFile() {
        return new File(getTempDir());
    }

    /**
     * Gets the default temp directory for the system.
     *
     * @return a {@code String}, representing the path to the default temp
     *         directory.
     * @see File#createTempFile
     */
    public static String getTempDir() {
        synchronized (FileUtil.class) {
            if (TEMP_DIR == null) {
                // Get the 'java.io.tmpdir' property
                String tmpDir = System.getProperty("java.io.tmpdir");

                if (StringUtil.isEmpty(tmpDir)) {
                    // Stupid fallback...
                    // TODO: Delegate to FileSystem?
                    if (new File("/temp").exists()) {
                        tmpDir = "/temp";  // Windows
                    }
                    else {
                        tmpDir = "/tmp";   // Unix
                    }
                }
                TEMP_DIR = tmpDir;
            }
        }
        return TEMP_DIR;
    }

    /**
     * Gets the contents of the given file, as a byte array.
     *
     * @param pFilename the name of the file to get content from
     * @return the content of the file as a byte array.
     * @throws IOException if the read operation fails
     */
    public static byte[] read(String pFilename) throws IOException {
        return read(new File(pFilename));
    }

    /**
     * Gets the contents of the given file, as a byte array.
     *
     * @param pFile the file to get content from
     * @return the content of the file as a byte array.
     * @throws IOException if the read operation fails
     */
    public static byte[] read(File pFile) throws IOException {
        // Custom implementation, as we know the size of a file
        if (!pFile.exists()) {
            throw new FileNotFoundException(pFile.toString());
        }

        byte[] bytes = new byte[(int) pFile.length()];
        InputStream in = null;

        try {
            // Use buffer size two times byte array, to avoid i/o bottleneck
            in = new BufferedInputStream(new FileInputStream(pFile), BUF_SIZE * 2);

            int off = 0;
            int len;
            while ((len = in.read(bytes, off, in.available())) != -1 && (off < bytes.length)) {
                off += len;
                //              System.out.println("read:" + len);
            }
        }
        // Just pass any IOException on up the stack
        finally {
            close(in);
        }

        return bytes;
    }

    /**
     * Reads all data from the input stream to a byte array.
     *
     * @param pInput The input stream to read from
     * @return The content of the stream as a byte array.
     * @throws IOException if an i/o error occurs during read.
     */
    public static byte[] read(InputStream pInput) throws IOException {
        // Create byte array
        ByteArrayOutputStream bytes = new FastByteArrayOutputStream(BUF_SIZE);

        // Copy from stream to byte array
        copy(pInput, bytes);

        return bytes.toByteArray();
    }
    
    /**
     * Writes the contents from a byte array to an output stream.
     *
     * @param pOutput   The output stream to write to
     * @param pData The byte array to write
     * @return {@code true}, otherwise an IOException is thrown.
     * @throws IOException if an i/o error occurs during write.
     */
    public static boolean write(OutputStream pOutput, byte[] pData) throws IOException {
        // Write data
        pOutput.write(pData);

        // If we got here, all is okay
        return true;
    }

    /**
     * Writes the contents from a byte array to a file.
     *
     * @param pFile The file to write to
     * @param pData The byte array to write
     * @return {@code true}, otherwise an IOException is thrown.
     * @throws IOException if an i/o error occurs during write.
     */
    public static boolean write(File pFile, byte[] pData) throws IOException {
        boolean success = false;
        OutputStream out = null;

        try {
            out = new BufferedOutputStream(new FileOutputStream(pFile));
            success = write(out, pData);
        }
        finally {
            close(out);
        }
        return success;
    }

    /**
     * Writes the contents from a byte array to a file.
     *
     * @param pFilename The name of the file to write to
     * @param pData     The byte array to write
     * @return {@code true}, otherwise an IOException is thrown.
     * @throws IOException if an i/o error occurs during write.
     */
    public static boolean write(String pFilename, byte[] pData) throws IOException {
        return write(new File(pFilename), pData);
    }

    /**
     * Deletes the specified file.
     *
     * @param pFile  The file to delete
     * @param pForce Forces delete, even if the parameter is a directory, and
     *               is not empty. Be careful!
     * @return {@code true}, if the file existed and was deleted.
     * @throws IOException if an i/o error occurs during delete.
     */
    public static boolean delete(final File pFile, final boolean pForce) throws IOException {
        if (pForce && pFile.isDirectory()) {
            return deleteDir(pFile);
        }
        return pFile.exists() && pFile.delete();
    }

    /**
     * Deletes a directory recursively.
     *
     * @param pFile the file to delete
     * @return {@code true} if the file was deleted sucessfully
     * @throws IOException if an i/o error occurs during delete.
     */
    private static boolean deleteDir(final File pFile) throws IOException {
        // Recusively delete all files/subfolders
        // Deletes the files using visitor pattern, to avoid allocating
        // a file array, which may throw OutOfMemoryExceptions for
        // large directories/in low memory situations
        class DeleteFilesVisitor implements Visitor<File> {
            private int failedCount = 0;
            private IOException exception = null;

            public void visit(final File pFile) {
                try {
                    if (!delete(pFile, true)) {
                        failedCount++;
                    }
                }
                catch (IOException e) {
                    failedCount++;
                    if (exception == null) {
                        exception = e;
                    }
                }
            }

            boolean succeeded() throws IOException {
                if (exception != null) {
                    throw exception;
                }
                return failedCount == 0;
            }
        }
        DeleteFilesVisitor fileDeleter = new DeleteFilesVisitor();
        visitFiles(pFile, null, fileDeleter);

        // If any of the deletes above failed, this will fail (or return false)
        return fileDeleter.succeeded() && pFile.delete();
    }

    /**
     * Deletes the specified file.
     *
     * @param pFilename The name of file to delete
     * @param pForce    Forces delete, even if the parameter is a directory, and
     *                  is not empty. Careful!
     * @return {@code true}, if the file existed and was deleted.
     * @throws java.io.IOException if deletion fails
     */
    public static boolean delete(String pFilename, boolean pForce) throws IOException {
        return delete(new File(pFilename), pForce);
    }

    /**
     * Deletes the specified file.
     *
     * @param pFile The file to delete
     * @return {@code true}, if the file existed and was deleted.
     * @throws java.io.IOException if deletion fails
     */
    public static boolean delete(File pFile) throws IOException {
        return delete(pFile, false);
    }

    /**
     * Deletes the specified file.
     *
     * @param pFilename The name of file to delete
     * @return {@code true}, if the file existed and was deleted.
     * @throws java.io.IOException if deletion fails
     */
    public static boolean delete(String pFilename) throws IOException {
        return delete(new File(pFilename), false);
    }

    /**
     * Renames the specified file.
     * If the destination is a directory (and the source is not), the source
     * file is simply moved to the destination directory.
     *
     * @param pFrom      The file to rename
     * @param pTo        The new file
     * @param pOverWrite Specifies if the tofile should be overwritten, if it
     *                   exists
     * @return {@code true}, if the file was renamed.
     *
     * @throws FileNotFoundException if {@code pFrom} does not exist.
     */
    public static boolean rename(File pFrom, File pTo, boolean pOverWrite) throws IOException {
        if (!pFrom.exists()) {
            throw new FileNotFoundException(pFrom.getAbsolutePath());
        }

        if (pFrom.isFile() && pTo.isDirectory()) {
            pTo = new File(pTo, pFrom.getName());
        }
        return (pOverWrite || !pTo.exists()) && pFrom.renameTo(pTo);

    }

    /**
     * Renames the specified file, if the destination does not exist.
     * If the destination is a directory (and the source is not), the source
     * file is simply moved to the destination directory.
     *
     * @param pFrom The file to rename
     * @param pTo   The new file
     * @return {@code true}, if the file was renamed.
     * @throws java.io.IOException if rename fails
     */
    public static boolean rename(File pFrom, File pTo) throws IOException {
        return rename(pFrom, pTo, false);
    }

    /**
     * Renames the specified file.
     * If the destination is a directory (and the source is not), the source
     * file is simply moved to the destination directory.
     *
     * @param pFrom      The file to rename
     * @param pTo        The new name of the file
     * @param pOverWrite Specifies if the tofile should be overwritten, if it
     *                   exists
     * @return {@code true}, if the file was renamed.
     * @throws java.io.IOException if rename fails
     */
    public static boolean rename(File pFrom, String pTo, boolean pOverWrite) throws IOException {
        return rename(pFrom, new File(pTo), pOverWrite);
    }

    /**
     * Renames the specified file, if the destination does not exist.
     * If the destination is a directory (and the source is not), the source
     * file is simply moved to the destination directory.
     *
     * @param pFrom The file to rename
     * @param pTo   The new name of the file
     * @return {@code true}, if the file was renamed.
     * @throws java.io.IOException if rename fails
     */
    public static boolean rename(File pFrom, String pTo) throws IOException {
        return rename(pFrom, new File(pTo), false);
    }

    /**
     * Renames the specified file.
     * If the destination is a directory (and the source is not), the source
     * file is simply moved to the destination directory.
     *
     * @param pFrom      The name of the file to rename
     * @param pTo        The new name of the file
     * @param pOverWrite Specifies if the tofile should be overwritten, if it
     *                   exists
     * @return {@code true}, if the file was renamed.
     * @throws java.io.IOException if rename fails
     */
    public static boolean rename(String pFrom, String pTo, boolean pOverWrite) throws IOException {
        return rename(new File(pFrom), new File(pTo), pOverWrite);
    }

    /**
     * Renames the specified file, if the destination does not exist.
     * If the destination is a directory (and the source is not), the source
     * file is simply moved to the destination directory.
     *
     * @param pFrom The name of the file to rename
     * @param pTo   The new name of the file
     * @return {@code true}, if the file was renamed.
     * @throws java.io.IOException if rename fails
     */
    public static boolean rename(String pFrom, String pTo) throws IOException {
        return rename(new File(pFrom), new File(pTo), false);
    }

    /**
     * Lists all files (and directories) in a specific folder.
     *
     * @param pFolder The folder to list
     * @return a list of {@code java.io.File} objects.
     * @throws FileNotFoundException if {@code pFolder} is not a readable file
     */
    public static File[] list(final String pFolder) throws FileNotFoundException {
        return list(pFolder, null);
    }

    /**
     * Lists all files (and directories) in a specific folder which are
     * embraced by the wildcard filename mask provided.
     *
     * @param pFolder       The folder to list
     * @param pFilenameMask The wildcard filename mask
     * @return a list of {@code java.io.File} objects.
     * @see File#listFiles(FilenameFilter)
     * @throws FileNotFoundException if {@code pFolder} is not a readable file
     */
    public static File[] list(final String pFolder, final String pFilenameMask) throws FileNotFoundException {
        if (StringUtil.isEmpty(pFolder)) {
            return null;
        }

        File folder = resolve(pFolder);
        if (!(/*folder.exists() &&*/folder.isDirectory() && folder.canRead())) {
            // NOTE: exists is implicitly called by isDirectory
            throw new FileNotFoundException("\"" + pFolder + "\" is not a directory or is not readable.");
        }

        if (StringUtil.isEmpty(pFilenameMask)) {
            return folder.listFiles();
        }

        // TODO: Rewrite to use regexp

        FilenameFilter filter = new FilenameMaskFilter(pFilenameMask);
        return folder.listFiles(filter);
    }

    /**
     * Creates a {@code File} based on the path part of the URL, for
     * file-protocol ({@code file:}) based URLs.
     *
     * @param pURL the {@code file:} URL
     * @return a new {@code File} object representing the URL
     *
     * @throws NullPointerException if {@code pURL} is {@code null}
     * @throws IllegalArgumentException if {@code pURL} is
     * not a file-protocol URL.
     *
     * @see java.io.File#toURI()
     * @see java.io.File#File(java.net.URI)
     */
    public static File toFile(URL pURL) {
        if (pURL == null) {
            throw new NullPointerException("URL == null");
        }

        // NOTE: Precondition tests below is based on the File(URI) constructor,
        //   and is most likely overkill...
        // NOTE: A URI is absolute iff it has a scheme component
        //   As the scheme has to be "file", this is implicitly tested below
        // NOTE: A URI is opaque iff it is absolute and it's shceme-specific
        //   part does not begin with a '/', see below
        if (!"file".equals(pURL.getProtocol())) {
            // URL protocol => URI scheme
            throw new IllegalArgumentException("URL scheme is not \"file\"");
        }
        if (pURL.getAuthority() != null) {
            throw new IllegalArgumentException("URL has an authority component");
        }
        if (pURL.getRef() != null) {
            // URL ref (anchor) => URI fragment
            throw new IllegalArgumentException("URI has a fragment component");
        }
        if (pURL.getQuery() != null) {
            throw new IllegalArgumentException("URL has a query component");
        }
        String path = pURL.getPath();
        if (!path.startsWith("/")) {
            // A URL should never be able to represent an opaque URI, test anyway
            throw new IllegalArgumentException("URI is not hierarchical");
        }
        if (path.equals("")) {
            throw new IllegalArgumentException("URI path component is empty");
        }

        // Convert separator, doesn't seem to be neccessary on Windows/Unix,
        // but do it anyway to be compatible...
        if (File.separatorChar != '/') {
            path = path.replace('/', File.separatorChar);
        }

        return resolve(path);
    }

    public static File resolve(String pPath) {
        return Win32File.wrap(new File(pPath));
    }

    public static File resolve(File pPath) {
        return Win32File.wrap(pPath);
    }

    public static File resolve(File pParent, String pChild) {
        return Win32File.wrap(new File(pParent, pChild));
    }

    public static File[] resolve(File[] pPaths) {
        return Win32File.wrap(pPaths);
    }

    // TODO: Handle SecurityManagers in a deterministic way
    // TODO: Exception handling
    // TODO: What happens if the file does not exist?
    public static long getFreeSpace(final File pPath) {
        // NOTE: Allow null, to get space in current/system volume
        File path = pPath != null ? pPath : new File(".");

        Long space = getSpace16("getFreeSpace", path);
        if (space != null) {
            return space;
        }

        return FS.getFreeSpace(path);
    }

    public static long getUsableSpace(final File pPath) {
        // NOTE: Allow null, to get space in current/system volume
        File path = pPath != null ? pPath : new File(".");

        Long space = getSpace16("getUsableSpace", path);
        if (space != null) {
            return space;
        }

        return getTotalSpace(path);
    }

    // TODO: FixMe for Windows, before making it public...
    public static long getTotalSpace(final File pPath) {
        // NOTE: Allow null, to get space in current/system volume
        File path = pPath != null ? pPath : new File(".");

        Long space = getSpace16("getTotalSpace", path);
        if (space != null) {
            return space;
        }

        return FS.getTotalSpace(path);
    }

    private static Long getSpace16(final String pMethodName, final File pPath) {
        try {
            Method freeSpace = File.class.getMethod(pMethodName);
            return (Long) freeSpace.invoke(pPath);
        }
        catch (NoSuchMethodException ignore) {}
        catch (IllegalAccessException ignore) {}
        catch (InvocationTargetException e) {
            Throwable throwable = e.getTargetException();
            if (throwable instanceof SecurityException) {
                throw (SecurityException) throwable;
            }
            throw new UndeclaredThrowableException(throwable);
        }

        return null;
    }

    /**
     * Formats the given number to a human readable format.
     * Kind of like {@code df -h}.
     *
     * @param pSizeInBytes the size in byte
     * @return a human readable string representation
     */
    public static String toHumanReadableSize(final long pSizeInBytes) {
        // TODO: Rewrite to use String.format?
        if (pSizeInBytes < 1024L) {
            return pSizeInBytes + " Bytes";
        }
        else if (pSizeInBytes < (1024L << 10)) {
            return getSizeFormat().format(pSizeInBytes / (double) (1024L)) + " KB";
        }
        else if (pSizeInBytes < (1024L << 20)) {
            return getSizeFormat().format(pSizeInBytes / (double) (1024L << 10)) + " MB";
        }
        else if (pSizeInBytes < (1024L << 30)) {
            return getSizeFormat().format(pSizeInBytes / (double) (1024L << 20)) + " GB";
        }
        else  if (pSizeInBytes < (1024L << 40)) {
            return getSizeFormat().format(pSizeInBytes / (double) (1024L << 30)) + " TB";
        }
        else {
            return getSizeFormat().format(pSizeInBytes / (double) (1024L << 40)) + " PB";
        }
    }

    // NumberFormat is not thread-safe, so we stick to thread-confined instances
    private static ThreadLocal<NumberFormat> sNumberFormat = new ThreadLocal<NumberFormat>() {
        protected NumberFormat initialValue() {
            NumberFormat format = NumberFormat.getNumberInstance();
            // TODO: Consider making this locale/platform specific, OR a method parameter...
//            format.setMaximumFractionDigits(2);
            format.setMaximumFractionDigits(0);
            return format;
        }
    };

    private static NumberFormat getSizeFormat() {
        return sNumberFormat.get();
    }

    /**
     * Visits all files in {@code pDirectory}. Optionally filtered through a {@link FileFilter}.
     *
     * @param pDirectory the directory to visit files in
     * @param pFilter the filter, may be {@code null}, meaning all files will be visited
     * @param pVisitor the visitor
     *
     * @throws IllegalArgumentException if either {@code pDirectory} or {@code pVisitor} are {@code null}
     *
     * @see com.twelvemonkeys.util.Visitor
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void visitFiles(final File pDirectory, final FileFilter pFilter, final Visitor<File> pVisitor) {
        Validate.notNull(pDirectory, "directory");
        Validate.notNull(pVisitor, "visitor");

        pDirectory.listFiles(new FileFilter() {
            public boolean accept(final File pFile) {
                if (pFilter == null || pFilter.accept(pFile)) {
                    pVisitor.visit(pFile);
                }

                return false;
            }
        });
    }
}
