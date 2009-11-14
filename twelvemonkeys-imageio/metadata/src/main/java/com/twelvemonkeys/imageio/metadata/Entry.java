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

    // Human readable "tag" (field) name from sepc
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
}
