/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.xmp;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* XMPEntry
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: XMPEntry.java,v 1.0 Nov 17, 2009 9:38:39 PM haraldk Exp$
*/
final class XMPEntry extends AbstractEntry {
    private final String fieldName;

    // TODO: Rewrite to use namespace + field instead of identifier (for the nativeIdentifier) method
    public XMPEntry(final String identifier, final Object pValue) {
        this(identifier, null, pValue);
    }

    public XMPEntry(final String identifier, final String fieldName, final Object value) {
        super(identifier, value);
        this.fieldName = fieldName;
    }

    @Override
    protected String getNativeIdentifier() {
        String identifier = (String) getIdentifier();
        String namespace = fieldName != null && identifier.endsWith(fieldName) ? XMP.DEFAULT_NS_MAPPING.get(identifier.substring(0, identifier.length() - fieldName.length())) : null;
        return namespace != null ?  namespace + ":" + fieldName : identifier;
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
    public String getFieldName() {
        return fieldName != null ? fieldName : XMP.DEFAULT_NS_MAPPING.get(getIdentifier());
    }

    @Override
    public String getTypeName() {
        // Special handling for collections
        Object value = getValue();
        if (value instanceof List) {
            return "List";
        }
        else if (value instanceof Set) {
            return "Set";
        }
        else if (value instanceof Map) {
            return "Map";
        }

        // Fall back to class name
        return super.getTypeName();
    }
    @Override
    public String toString() {
        String type = getTypeName();
        String typeStr = type != null ? " (" + type + ")" : "";

        return String.format("%s: %s%s", getNativeIdentifier(), getValueAsString(), typeStr);
    }
}
