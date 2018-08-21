/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Directory
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: Directory.java,v 1.0 25.feb.2006 00:29:44 haku Exp$
 */
class Directory {
    private final List<DirectoryEntry> entries;

    private Directory(int pImageCount) {
        entries = Arrays.asList(new DirectoryEntry[pImageCount]);
    }

    public static Directory read(final int pType, final int pImageCount, final DataInput pStream) throws IOException {
        Directory directory = new Directory(pImageCount);
        directory.readEntries(pType,  pStream);
        return directory;
    }

    private void readEntries(final int pType, final DataInput pStream) throws IOException {
        for (int i = 0; i < entries.size(); i++) {
            entries.set(i, DirectoryEntry.read(pType, pStream));
        }
    }

    public DirectoryEntry getEntry(final int pEntryIndex) {
        return entries.get(pEntryIndex);
    }

    public int count() {
        return entries.size();
    }

    @Override
    public String toString() {
        return String.format("%s%s", getClass().getSimpleName(), entries);
    }
}
