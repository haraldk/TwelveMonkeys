package com.twelvemonkeys.imageio.metadata;

/**
 * Directory
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Directory.java,v 1.0 Nov 11, 2009 4:20:58 PM haraldk Exp$
 */
public interface Directory extends Iterable<Entry> {
    // TODO: Spec when more entries exist? Or make Entry support multi-values!?
    // For multiple entries with same id in directory, the first entry (using the order from the stream) will be returned
    Entry getEntryById(Object pIdentifier);

    Entry getEntryByName(String pName);

    // Iterator containing the entries in
    //Iterator<Entry> getBestEntries(Object pIdentifier, Object pQualifier, String pLanguage);


    /// Collection-like API
    // TODO: addOrReplaceIfPresent... (trouble for multi-values)  Or mutable entries?
    // boolean replace(Entry pEntry)??
    // boolean contains(Object pIdentifier)?

    boolean add(Entry pEntry);

    boolean remove(Object pEntry); // Object in case we retro-fit Collection/Map..

    int size();
        
    boolean isReadOnly();
}
