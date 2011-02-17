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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * WriterFileSuffixFilter
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku$
 * @version $Id: WriterFileSuffixFilter.java,v 1.0 11.okt.2006 20:05:36 haku Exp$
 */
public final class WriterFileSuffixFilter extends FileFilter implements java.io.FileFilter {
    private final String description;
    private Map<String, Boolean> knownSuffixes = new HashMap<String, Boolean>(32);

    public WriterFileSuffixFilter() {
        this("Images (all supported output formats)");
    }

    public WriterFileSuffixFilter(String pDescription) {
        description = pDescription;
    }

    public boolean accept(File pFile) {
        // Directories are always supported
        if (pFile.isDirectory()) {
            return true;
        }

        // Test if we have an ImageWriter for this suffix
        String suffix = FileUtil.getExtension(pFile);

        return !StringUtil.isEmpty(suffix) && hasWriterForSuffix(suffix);
    }

    private boolean hasWriterForSuffix(String pSuffix) {
        if (knownSuffixes.get(pSuffix) == Boolean.TRUE) {
            return true;
        }

        try {
            // Cahce lookup
            Iterator iterator = ImageIO.getImageWritersBySuffix(pSuffix);

            if (iterator.hasNext()) {
                knownSuffixes.put(pSuffix, Boolean.TRUE);
                return true;
            }
            else {
                knownSuffixes.put(pSuffix, Boolean.FALSE);
                return false;
            }
        }
        catch (IllegalArgumentException iae) {
            return false;
        }
    }

    public String getDescription() {
        return description;
    }
}
