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

import com.twelvemonkeys.util.StringTokenIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * UnixFileSystem
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/UnixFileSystem.java#1 $
 */
final class UnixFileSystem extends FileSystem {
    long getFreeSpace(File pPath) {
        try {
            return getNumber(pPath, 3);
        }
        catch (IOException e) {
            return 0l;
        }
    }

    long getTotalSpace(File pPath) {
        try {
            return getNumber(pPath, 5);
        }
        catch (IOException e) {
            return 0l;
        }
    }

    private long getNumber(File pPath, int pIndex) throws IOException {
        // TODO: Test on other platforms
        // Tested on Mac OS X, CygWin
        BufferedReader reader = exec(new String[] {"df", "-k", pPath.getAbsolutePath()});

        String last = null;
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                last = line;
            }
        }
        finally {
            FileUtil.close(reader);
        }

        if (last != null) {
            String blocks = null;
            StringTokenIterator tokens = new StringTokenIterator(last, " ", StringTokenIterator.REVERSE);
            int count = 0;
            // We want the 3rd last token
            while (count < pIndex && tokens.hasNext()) {
                blocks = tokens.nextToken();
                count++;
            }

            if (blocks != null) {
                try {
                    return Long.parseLong(blocks) * 1024L;
                }
                catch (NumberFormatException ignore) {
                    // Ignore
                }
            }
        }

        return 0l;
    }

    String getName() {
        return "Unix";
    }
}
