package com.twelvemonkeys.imageio.metadata.xmp;

import java.util.HashMap;

/**
 * XMPNamespaceMapping
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPNamespaceMapping.java,v 1.0 Nov 17, 2009 6:35:21 PM haraldk Exp$
 */
final class XMPNamespaceMapping extends HashMap<String, String> {
    public XMPNamespaceMapping() {
        put(XMP.NS_RDF, "rdf");
        put(XMP.NS_DC, "dc");
        put(XMP.NS_EXIF, "exif");
        put(XMP.NS_PHOTOSHOP, "photoshop");
        put(XMP.NS_ST_REF, "stRef");
        put(XMP.NS_TIFF, "tiff");
        put(XMP.NS_XAP, "xap");
        put(XMP.NS_XAP_MM, "xapMM");
    }
}
