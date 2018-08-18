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

package com.twelvemonkeys.imageio.metadata;

/**
 * Entry
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Entry.java,v 1.0 Nov 11, 2009 4:21:08 PM haraldk Exp$
 */
public interface Entry {
    // "tag" identifier from spec
    Object getIdentifier();

    // Human readable "tag" (field) name from spec
    String getFieldName();

    // The internal "tag" value as stored in the stream, may be a Directory
    Object getValue();

    // Human readable "tag" value
    String getValueAsString();
    
    //void setValue(Object pValue); // TODO: qualifiers...

    // Optional, implementation/spec specific type, describing the object returned from getValue
    String getTypeName();

    // TODO: Or something like getValue(qualifierType, qualifierValue) + getQualifiers()/getQualifierValues
    // TODO: The problem with current model is getEntry() which only has single value support

    // Optional, xml:lang-support
    //String getLanguage();

    // Optional, XMP alt-support. TODO: Do we need both?
    //Object getQualifier();

    // For arrays only
    int valueCount();

    // TODO: getValueAsInt, UnsignedInt, Short, UnsignedShort, Byte, UnsignedByte etc
    // TODO: getValueAsIntArray, ShortArray, ByteArray, StringArray etc (also for non-arrays, to return a single element array)

}
