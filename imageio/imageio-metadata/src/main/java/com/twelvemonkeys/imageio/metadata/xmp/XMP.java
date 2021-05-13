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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * XMP
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMP.java,v 1.0 Nov 12, 2009 12:19:32 AM haraldk Exp$
 *
 * @see <a href="http://www.adobe.com/products/xmp/">Extensible Metadata Platform (XMP)</a>
 */
public interface XMP {
    /** W3C Resource Description Format namespace */
    String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /** Dublin Core Metadata Initiative namespace */
    String NS_DC = "http://purl.org/dc/elements/1.1/";

    String NS_EXIF = "http://ns.adobe.com/exif/1.0/";

    String NS_PHOTOSHOP = "http://ns.adobe.com/photoshop/1.0/";

    String NS_ST_REF = "http://ns.adobe.com/xap/1.0/sType/ResourceRef#";

    String NS_TIFF = "http://ns.adobe.com/tiff/1.0/";

    String NS_XAP = "http://ns.adobe.com/xap/1.0/";

    String NS_XAP_MM = "http://ns.adobe.com/xap/1.0/mm/";

    String NS_X = "adobe:ns:meta/";

    /** Contains the mapping from URI to default namespace prefix. */
    Map<String, String> DEFAULT_NS_MAPPING = Collections.unmodifiableMap(new XMPNamespaceMapping(true));

    Set<String> ELEMENTS = Collections.unmodifiableSet(new XMPNamespaceMapping(false).keySet());
}
