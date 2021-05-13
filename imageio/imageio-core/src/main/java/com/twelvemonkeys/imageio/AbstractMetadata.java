/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio;

import org.w3c.dom.Node;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import java.util.Arrays;

/**
 * AbstractMetadata
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractMetadata.java,v 1.0 Nov 13, 2009 1:02:12 AM haraldk Exp$
 */
public abstract class AbstractMetadata extends IIOMetadata implements Cloneable {
    protected AbstractMetadata(final boolean standardFormatSupported,
                               final String nativeFormatName, final String nativeFormatClassName,
                               final String[] extraFormatNames, final String[] extraFormatClassNames) {
        super(standardFormatSupported, nativeFormatName, nativeFormatClassName, extraFormatNames, extraFormatClassNames);
    }

    protected AbstractMetadata() {
        super(true, null, null, null, null);
    }

    /**
     * Default implementation returns {@code true}.
     * Mutable subclasses should override this method.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public Node getAsTree(final String formatName) {
        validateFormatName(formatName);

        if (formatName.equals(nativeMetadataFormatName)) {
            return getNativeTree();
        }
        else if (formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        }

        // Subclasses that supports extra formats need to check for these formats themselves...
        return null;
    }

    /**
     * Default implementation that throws {@code UnsupportedOperationException}.
     * Subclasses that supports formats other than standard metadata should override this method.
     *
     * @throws UnsupportedOperationException
     */
    protected Node getNativeTree() {
        throw new UnsupportedOperationException("getNativeTree");
    }

    @Override
    public void mergeTree(final String formatName, final Node root) throws IIOInvalidTreeException {
        assertMutable();

        validateFormatName(formatName);

        if (!root.getNodeName().equals(formatName)) {
            throw new IIOInvalidTreeException("Root must be " + formatName, root);
        }
    }

    @Override
    public void reset() {
        assertMutable();
    }

    /**
     * Asserts that this meta data is mutable.
     *
     * @throws IllegalStateException if {@link #isReadOnly()} returns {@code true}.
     */
    protected final void assertMutable() {
        if (isReadOnly()) {
            throw new IllegalStateException("Metadata is read-only");
        }
    }

    protected final void validateFormatName(final String formatName) {
        String[] metadataFormatNames = getMetadataFormatNames();

        if (metadataFormatNames != null) {
            for (String metadataFormatName : metadataFormatNames) {
                if (metadataFormatName.equals(formatName)) {
                    return; // Found, we're ok!
                }
            }
        }

        throw new IllegalArgumentException(
                String.format("Unsupported format name: \"%s\". Expected one of %s", formatName, Arrays.toString(metadataFormatNames))
        );
    }

    protected static String toListString(short[] values) {
        String string = Arrays.toString(values);
        return string.substring(1, string.length() - 1);
    }
}
