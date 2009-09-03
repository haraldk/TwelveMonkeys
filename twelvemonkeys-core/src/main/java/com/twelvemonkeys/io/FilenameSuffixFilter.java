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


import com.twelvemonkeys.lang.StringUtil;

import java.io.File;
import java.io.FilenameFilter;


/**
 * A Java Bean used for approving file names which are to be included in a
 * {@code java.io.File} listing. The file name suffixes are used as a
 * filter input and is given to the class via the string array property:<br>
 * <dd>{@code filenameSuffixesToExclude}
 * <p>
 * A recommended way of doing this is by referencing to the component which uses
 * this class for file listing. In this way all properties are set in the same
 * component and this utility component is kept in the background with only
 * initial configuration necessary.
 *
 * @author <a href="mailto:eirik.torske@iconmedialab.no">Eirik Torske</a>
 * @see File#list(java.io.FilenameFilter) java.io.File.list
 * @see FilenameFilter java.io.FilenameFilter
 */
public class FilenameSuffixFilter implements FilenameFilter {

    // Members
    String[] mFilenameSuffixesToExclude;

    /** Creates a {@code FileNameSuffixFilter} */
    public FilenameSuffixFilter() {
    }

    public void setFilenameSuffixesToExclude(String[] pFilenameSuffixesToExclude) {
        mFilenameSuffixesToExclude = pFilenameSuffixesToExclude;
    }

    public String[] getFilenameSuffixesToExclude() {
        return mFilenameSuffixesToExclude;
    }

    /**
     * This method implements the {@code java.io.FilenameFilter} interface.
     * <p/>
     *
     * @param pDir  the directory in which the file was found.
     * @param pName the pName of the file.
     * @return {@code true} if the pName should be included in the file list;
     *         {@code false} otherwise.
     */
    public boolean accept(final File pDir, final String pName) {
        if (StringUtil.isEmpty(mFilenameSuffixesToExclude)) {
            return true;
        }

        for (String aMFilenameSuffixesToExclude : mFilenameSuffixesToExclude) {
            // -- Edit by haraldK, to make interfaces more consistent
            // if (StringUtil.filenameSuffixIs(pName, mFilenameSuffixesToExclude[i])) {
            if (aMFilenameSuffixesToExclude.equals(FileUtil.getExtension(pName))) {
                return false;
            }
        }
        return true;
    }
}
