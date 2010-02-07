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

package com.twelvemonkeys.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * FileSystem
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/FileSystem.java#1 $
 */
abstract class FileSystem {
    abstract long getFreeSpace(File pPath);

    abstract long getTotalSpace(File pPath);

    abstract String getName();

    static BufferedReader exec(String[] pArgs) throws IOException {
        Process cmd = Runtime.getRuntime().exec(pArgs);
        return new BufferedReader(new InputStreamReader(cmd.getInputStream()));
    }

    static FileSystem get() {
        String os = System.getProperty("os.name");
        //System.out.println("os = " + os);

        os = os.toLowerCase();
        if (os.indexOf("windows") != -1) {
            return new Win32FileSystem();
        }
        else if (os.indexOf("linux") != -1 ||
                os.indexOf("sun os") != -1 ||
                os.indexOf("sunos") != -1 ||
                os.indexOf("solaris") != -1 ||
                os.indexOf("mpe/ix") != -1 ||
                os.indexOf("hp-ux") != -1 ||
                os.indexOf("aix") != -1 ||
                os.indexOf("freebsd") != -1 ||
                os.indexOf("irix") != -1 ||
                os.indexOf("digital unix") != -1 ||
                os.indexOf("unix") != -1 ||
                os.indexOf("mac os x") != -1) {
            return new UnixFileSystem();
        }
        else {
            return new UnknownFileSystem(os);
        }
    }

    private static class UnknownFileSystem extends FileSystem {
        private final String mOSName;

        UnknownFileSystem(String pOSName) {
            mOSName = pOSName;
        }

        long getFreeSpace(File pPath) {
            return 0l;
        }

        long getTotalSpace(File pPath) {
            return 0l;
        }

        String getName() {
            return "Unknown (" + mOSName + ")";
        }
    }
}
