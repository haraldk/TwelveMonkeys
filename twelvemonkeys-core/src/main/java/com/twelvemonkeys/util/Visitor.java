package com.twelvemonkeys.util;

/**
 * A generic visitor.
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/Visitor.java#1 $
 *
 * @see <a href="http://en.wikipedia.org/wiki/Visitor_pattern">Visitor Patter</a>
 */
public interface Visitor<T> {
    /**
      * Visits an element.
      *
      * @param pElement the element to visit
      */
    void visit(T pElement);
}
