/*
 * Copyright (c) 2015, Harald Kuhr
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

package com.twelvemonkeys.imageio.spi;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import static com.twelvemonkeys.lang.Validate.notNull;

/**
 * ReaderWriterProviderInfo.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ReaderWriterProviderInfo.java,v 1.0 20/03/15 harald.kuhr Exp$
 */
public abstract class ReaderWriterProviderInfo extends ProviderInfo {

    private final String[] formatNames;
    private final String[] suffixes;
    private final String[] mimeTypes;
    private final String readerClassName;
    private final String[] readerSpiClassNames;
    private final Class[] inputTypes = new Class[] {ImageInputStream.class};
    private final String writerClassName;
    private final String[] writerSpiClassNames;
    private final Class[] outputTypes = new Class[] {ImageOutputStream.class};
    private final boolean supportsStandardStreamMetadata;
    private final String nativeStreamMetadataFormatName;
    private final String nativeStreamMetadataFormatClassName;
    private final String[] extraStreamMetadataFormatNames;
    private final String[] extraStreamMetadataFormatClassNames;
    private final boolean supportsStandardImageMetadata;
    private final String nativeImageMetadataFormatName;
    private final String nativeImageMetadataFormatClassName;
    private final String[] extraImageMetadataFormatNames;
    private final String[] extraImageMetadataFormatClassNames;

    /**
     * Creates a provider information instance based on the given class.
     *
     * @param infoClass the class to get provider information from.
     *                  The provider info will be taken from the class' package.
     * @throws IllegalArgumentException if {@code pPackage == null}
     */
    protected ReaderWriterProviderInfo(final Class<? extends ReaderWriterProviderInfo> infoClass,
                                       final String[] formatNames,
                                       final String[] suffixes,
                                       final String[] mimeTypes,
                                       final String readerClassName,
                                       final String[] readerSpiClassNames,
                                       final String writerClassName,
                                       final String[] writerSpiClassNames,
                                       final boolean supportsStandardStreamMetadata,
                                       final String nativeStreameMetadataFormatName,
                                       final String nativeStreamMetadataFormatClassName,
                                       final String[] extraStreamMetadataFormatNames,
                                       final String[] extraStreamMetadataFormatClassNames,
                                       final boolean supportsStandardImageMetadata,
                                       final String nativeImageMetadataFormatName,
                                       final String nativeImageMetadataFormatClassName,
                                       final String[] extraImageMetadataFormatNames,
                                       final String[] extraImageMetadataFormatClassNames) {
        super(notNull(infoClass).getPackage());

        this.formatNames = formatNames;
        this.suffixes = suffixes;
        this.mimeTypes = mimeTypes;
        this.readerClassName = readerClassName;
        this.readerSpiClassNames = readerSpiClassNames;
        this.writerClassName = writerClassName;
        this.writerSpiClassNames = writerSpiClassNames;
        this.supportsStandardStreamMetadata = supportsStandardStreamMetadata;
        this.nativeStreamMetadataFormatName = nativeStreameMetadataFormatName;
        this.nativeStreamMetadataFormatClassName = nativeStreamMetadataFormatClassName;
        this.extraStreamMetadataFormatNames = extraStreamMetadataFormatNames;
        this.extraStreamMetadataFormatClassNames = extraStreamMetadataFormatClassNames;
        this.supportsStandardImageMetadata = supportsStandardImageMetadata;
        this.nativeImageMetadataFormatName = nativeImageMetadataFormatName;
        this.nativeImageMetadataFormatClassName = nativeImageMetadataFormatClassName;
        this.extraImageMetadataFormatNames = extraImageMetadataFormatNames;
        this.extraImageMetadataFormatClassNames = extraImageMetadataFormatClassNames;
    }

    public String[] formatNames() {
        return formatNames;
    }

    public String[] suffixes() {
        return suffixes;
    }

    public String[] mimeTypes() {
        return mimeTypes;
    }

    public String readerClassName() {
        return readerClassName;
    }

    public String[] readerSpiClassNames() {
        return readerSpiClassNames;
    }

    public Class[] inputTypes() {
        return inputTypes;
    }

    public String writerClassName() {
        return writerClassName;
    }

    public String[] writerSpiClassNames() {
        return writerSpiClassNames;
    }

    public Class[] outputTypes() {
        return outputTypes;
    }

    public boolean supportsStandardStreamMetadataFormat() {
        return supportsStandardStreamMetadata;
    }

    public String nativeStreamMetadataFormatName() {
        return nativeStreamMetadataFormatName;
    }

    public String nativeStreamMetadataFormatClassName() {
        return nativeStreamMetadataFormatClassName;
    }

    public String[] extraStreamMetadataFormatNames() {
        return extraStreamMetadataFormatNames;
    }

    public String[] extraStreamMetadataFormatClassNames() {
        return extraStreamMetadataFormatClassNames;
    }

    public boolean supportsStandardImageMetadataFormat() {
        return supportsStandardImageMetadata;
    }

    public String nativeImageMetadataFormatName() {
        return nativeImageMetadataFormatName;
    }

    public String nativeImageMetadataFormatClassName() {
        return nativeImageMetadataFormatClassName;
    }

    public String[] extraImageMetadataFormatNames() {
        return extraImageMetadataFormatNames;
    }

    public String[] extraImageMetadataFormatClassNames() {
        return extraImageMetadataFormatClassNames;
    }
}
