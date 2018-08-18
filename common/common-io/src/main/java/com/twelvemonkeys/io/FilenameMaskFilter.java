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

import com.twelvemonkeys.util.regex.WildcardStringParser;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A Java Bean used for approving file names which are to be included in a
 * {@code java.io.File} listing.
 * The mask is given as a well-known DOS filename format, with '*' and '?' as
 * wildcards.
 * All other characters counts as ordinary characters.
 * <p/>
 * The file name masks are used as a filter input and is given to the class via
 * the string array property:<br>
 * <dd>{@code filenameMasksForInclusion} - Filename mask for exclusion of
 * files (default if both properties are defined)
 * <dd>{@code filenameMasksForExclusion} - Filename mask for exclusion of
 * files.
 * <p/>
 * A recommended way of doing this is by referencing to the component which uses
 * this class for file listing. In this way all properties are set in the same
 * component and this utility component is kept in the background with only
 * initial configuration necessary.
 *
 * @author <a href="mailto:eirik.torske@iconmedialab.no">Eirik Torske</a>
 * @see File#list(java.io.FilenameFilter) java.io.File.list
 * @see FilenameFilter java.io.FilenameFilter
 * @see WildcardStringParser
 * @deprecated
 */
public class FilenameMaskFilter implements FilenameFilter {

     // TODO: Rewrite to use regexp, or create new class

    // Members
    private String[] filenameMasksForInclusion;
    private String[] filenameMasksForExclusion;
    private boolean inclusion = true;


    /**
     * Creates a {@code FilenameMaskFilter}
     */
    public FilenameMaskFilter() {
    }

    /**
     * Creates a {@code FilenameMaskFilter}
     *
     * @param pFilenameMask the filename mask
     */
    public FilenameMaskFilter(final String pFilenameMask) {
        String[] filenameMask = {pFilenameMask};
        setFilenameMasksForInclusion(filenameMask);
    }

    /**
     * Creates a {@code FilenameMaskFilter}
     *
     * @param pFilenameMasks the filename masks
     */
    public FilenameMaskFilter(final String[] pFilenameMasks) {
        this(pFilenameMasks, false);
    }

    /**
     * Creates a {@code FilenameMaskFilter}
     *
     * @param pFilenameMask the filename masks
     * @param pExclusion if {@code true}, the masks will be excluded
     */
    public FilenameMaskFilter(final String pFilenameMask, final boolean pExclusion) {
        String[] filenameMask = {pFilenameMask};

        if (pExclusion) {
            setFilenameMasksForExclusion(filenameMask);
        }
        else {
            setFilenameMasksForInclusion(filenameMask);
        }
    }

    /**
     * Creates a {@code FilenameMaskFilter}
     *
     * @param pFilenameMasks the filename masks
     * @param pExclusion if {@code true}, the masks will be excluded
     */
    public FilenameMaskFilter(final String[] pFilenameMasks, final boolean pExclusion) {
        if (pExclusion) {
            setFilenameMasksForExclusion(pFilenameMasks);
        }
        else {
            setFilenameMasksForInclusion(pFilenameMasks);
        }
    }

    /**
     *
     * @param pFilenameMasksForInclusion the filename masks to include
     */
    public void setFilenameMasksForInclusion(String[] pFilenameMasksForInclusion) {
        filenameMasksForInclusion = pFilenameMasksForInclusion;
    }

    /**
     * @return the current inclusion masks
     */
    public String[] getFilenameMasksForInclusion() {
        return filenameMasksForInclusion.clone();
    }

    /**
     * @param pFilenameMasksForExclusion the filename masks to exclude
     */
    public void setFilenameMasksForExclusion(String[] pFilenameMasksForExclusion) {
        filenameMasksForExclusion = pFilenameMasksForExclusion;
        inclusion = false;
    }

    /**
     * @return the current exclusion masks
     */
    public String[] getFilenameMasksForExclusion() {
        return filenameMasksForExclusion.clone();
    }

    /**
     * This method implements the {@code java.io.FilenameFilter} interface.
     *
     * @param pDir  the directory in which the file was found.
     * @param pName the name of the file.
     * @return {@code true} if the file {@code pName} should be included in the file
     *         list; {@code false} otherwise.
     */
    public boolean accept(File pDir, String pName) {
        WildcardStringParser parser;

        // Check each filename string mask whether the file is to be accepted
        if (inclusion) {  // Inclusion
            for (String mask : filenameMasksForInclusion) {
                parser = new WildcardStringParser(mask);
                if (parser.parseString(pName)) {

                    // The filename was accepted by the filename masks provided
                    // - include it in filename list
                    return true;
                }
            }

            // The filename not was accepted by any of the filename masks
            // provided - NOT to be included in the filename list
            return false;
        }
        else {
            // Exclusion
            for (String mask : filenameMasksForExclusion) {
                parser = new WildcardStringParser(mask);
                if (parser.parseString(pName)) {

                    // The filename was accepted by the filename masks provided
                    // - NOT to be included in the filename list
                    return false;
                }
            }

            // The filename was not accepted by any of the filename masks
            // provided - include it in filename list
            return true;
        }
    }

    /**
     * @return a string representation for debug purposes
     */
    public String toString() {
        StringBuilder retVal = new StringBuilder();
        int i;

        if (inclusion) {
            // Inclusion
            if (filenameMasksForInclusion == null) {
                retVal.append("No filename masks set - property filenameMasksForInclusion is null!");
            }
            else {
                retVal.append(filenameMasksForInclusion.length);
                retVal.append(" filename mask(s) - ");
                for (i = 0; i < filenameMasksForInclusion.length; i++) {
                    retVal.append("\"");
                    retVal.append(filenameMasksForInclusion[i]);
                    retVal.append("\", \"");
                }
            }
        }
        else {
            // Exclusion
            if (filenameMasksForExclusion == null) {
                retVal.append("No filename masks set - property filenameMasksForExclusion is null!");
            }
            else {
                retVal.append(filenameMasksForExclusion.length);
                retVal.append(" exclusion filename mask(s) - ");
                for (i = 0; i < filenameMasksForExclusion.length; i++) {
                    retVal.append("\"");
                    retVal.append(filenameMasksForExclusion[i]);
                    retVal.append("\", \"");
                }
            }
        }
        return retVal.toString();
    }
}
