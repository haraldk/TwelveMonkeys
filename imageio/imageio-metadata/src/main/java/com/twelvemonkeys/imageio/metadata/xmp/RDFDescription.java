/*
 * Copyright (c) 2012, Harald Kuhr
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

import com.twelvemonkeys.imageio.metadata.AbstractDirectory;
import com.twelvemonkeys.imageio.metadata.Entry;

import java.util.Collection;

/**
* RDFDescription
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: RDFDescription.java,v 1.0 Nov 17, 2009 9:38:58 PM haraldk Exp$
*/
final class RDFDescription extends AbstractDirectory {
    private final String namespace;

    // TODO: Store size of root directory, to allow serializing
    // TODO: XMPDirectory, maybe not even an AbstractDirectory
    //       - Keeping the Document would allow for easier serialization
    // TODO: Or use direct SAX parsing
    public RDFDescription(Collection<? extends Entry> entries) {
        this(null, entries);
    }

    public RDFDescription(String key, Collection<? extends Entry> entries) {
        super(entries);
        
        namespace = key;
    }

    @Override
    public String toString() {
        return namespace != null ? 
                super.toString().replaceAll("^RDFDescription\\[", String.format("%s[%s|%s, ", getClass().getSimpleName(), XMP.DEFAULT_NS_MAPPING.get(namespace), namespace)) :
                super.toString();
    }
}
