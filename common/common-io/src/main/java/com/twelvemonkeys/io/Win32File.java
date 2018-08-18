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

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Win32File
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/Win32File.java#2 $
 */
final class Win32File extends File {
    private final static boolean IS_WINDOWS = isWindows();

    private static boolean isWindows() {
        try {
            String os = System.getProperty("os.name");
            return os.toLowerCase().indexOf("windows") >= 0;
        }
        catch (Throwable t) {
            // Ignore
        }
        return false;
    }

    private Win32File(File pPath) {
        super(pPath.getPath());
    }

    public static void main(String[] pArgs) {
        int argIdx = 0;
        boolean recursive = false;
        while (pArgs.length > argIdx + 1 && pArgs[argIdx].charAt(0) == '-' && pArgs[argIdx].length() > 1) {
            if (pArgs[argIdx].charAt(1) == 'R' || pArgs[argIdx].equals("--recursive")) {
                recursive = true;
            }
            else {
                System.err.println("Unknown option: " + pArgs[argIdx]);
            }
            argIdx++;
        }

        File file = wrap(new File(pArgs[argIdx]));
        System.out.println("file: " + file);
        System.out.println("file.getClass(): " + file.getClass());

        listFiles(file, 0, recursive);
    }

    private static void listFiles(File pFile, int pLevel, boolean pRecursive) {
        if (pFile.isDirectory()) {
            File[] files = pFile.listFiles();
            for (int l = 0; l < pLevel; l++) {
                System.out.print(" ");
            }
            System.out.println("Contents of " + pFile + ": ");
            for (File file : files) {
                for (int l = 0; l < pLevel; l++) {
                    System.out.print(" ");
                }
                System.out.println("  " + file);
                if (pRecursive) {
                    listFiles(file, pLevel + 1, pLevel < 4);
                }
            }
        }
    }

    /**
     * Wraps a {@code File} object pointing to a Windows symbolic link
     * ({@code .lnk} file) in a {@code Win32Lnk}.
     * If the operating system is not Windows, the
     * {@code pPath} parameter is returned unwrapped.
     *
     * @param pPath any path, possibly pointing to a Windows symbolic link file.
     * May be {@code null}, in which case {@code null} is returned.
     *
     * @return a new {@code Win32Lnk} object if the current os is Windows, and
     * the file is a Windows symbolic link ({@code .lnk} file), otherwise
     * {@code pPath}
     */
    public static File wrap(final File pPath) {
        if (pPath == null) {
            return null;
        }

        if (IS_WINDOWS) {
            // Don't wrap if allready wrapped
            if (pPath instanceof Win32File || pPath instanceof Win32Lnk) {
                return pPath;
            }

            if (pPath.exists() && pPath.getName().endsWith(".lnk")) {
                // If Win32 .lnk, let's wrap
                try {
                    return new Win32Lnk(pPath);
                }
                catch (IOException e) {
                    // TODO: FixMe!
                    e.printStackTrace();
                }
            }

            // Wwrap even if not a .lnk, as the listFiles() methods etc,
            // could potentially return .lnk's, that we want to wrap later...
            return new Win32File(pPath);
        }

        return pPath;
    }

    /**
     * Wraps a {@code File} array, possibly pointing to Windows symbolic links
     * ({@code .lnk} files) in {@code Win32Lnk}s.
     *
     * @param pPaths an array of {@code File}s, possibly pointing to Windows
     * symbolic link files.
     * May be {@code null}, in which case {@code null} is returned.
     *
     * @return {@code pPaths}, with any {@code File} representing a Windows
     * symbolic link ({@code .lnk} file) wrapped in a {@code Win32Lnk}.
     */
    public static File[] wrap(File[] pPaths) {
        if (IS_WINDOWS) {
            for (int i = 0; pPaths != null && i < pPaths.length; i++) {
                pPaths[i] = wrap(pPaths[i]);
            }
        }
        return pPaths;
    }

    // File overrides
    @Override
    public File getAbsoluteFile() {
        return wrap(super.getAbsoluteFile());
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return wrap(super.getCanonicalFile());
    }

    @Override
    public File getParentFile() {
        return wrap(super.getParentFile());
    }

    @Override
    public File[] listFiles() {
        return wrap(super.listFiles());
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        return wrap(super.listFiles(filter));
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        return wrap(super.listFiles(filter));
    }
}
