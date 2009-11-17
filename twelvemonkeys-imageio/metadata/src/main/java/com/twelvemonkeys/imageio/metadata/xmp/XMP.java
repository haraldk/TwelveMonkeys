package com.twelvemonkeys.imageio.metadata.xmp;

import java.util.Collections;
import java.util.Map;

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

    /** Contains the mapping from URI to default namespace prefix. */
    Map<String, String> DEFAULT_NS_MAPPING = Collections.unmodifiableMap(new XMPNamespaceMapping());
}
