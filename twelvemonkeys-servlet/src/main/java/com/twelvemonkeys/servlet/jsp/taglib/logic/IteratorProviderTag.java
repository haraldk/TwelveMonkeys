
package com.twelvemonkeys.servlet.jsp.taglib.logic;

import java.util.Iterator;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.*;

/**
 * Abstract base class for adding iterators to a page.
 *
 * @todo Possible to use same strategy for all types of objects? Rename class
 * to ObjectProviderTag? Hmmm... Might work.
 *
 * @author Harald Kuhr
 * @version $id: $
 */
public abstract class IteratorProviderTag extends TagSupport {
    /** {@code iterator} */
    protected final static String DEFAULT_ITERATOR_NAME = "iterator";
    /** {@code java.util.iterator} */
    protected final static String DEFAULT_ITERATOR_TYPE = "java.util.Iterator";
    /** {@code type} */
    public final static String ATTRIBUTE_TYPE = "type";

    /** */
    private String mType = null;

    /**
     * Gets the type.
     *
     * @return the type (class name)
     */
    public String getType() {
        return mType;
    }

    /**
     * Sets the type.
     *
     * @param pType
     */

    public void setType(String pType) {
        mType = pType;
    }

    /**
     * doEndTag implementation.
     *
     * @return {@code Tag.EVAL_PAGE}
     * @throws JspException
     */

    public int doEndTag() throws JspException {
        // Set the iterator
        pageContext.setAttribute(getId(), getIterator());

        return Tag.EVAL_PAGE;
    }

    /**
     * Gets the iterator for this tag.
     *
     * @return an {@link java.util.Iterator}
     */
    protected abstract Iterator getIterator();

    /**
     * Gets the default iterator name.
     *
     * @return {@link #DEFAULT_ITERATOR_NAME}
     */
    protected static String getDefaultIteratorName() {
        return DEFAULT_ITERATOR_NAME;
    }

    /**
     * Gets the default iterator type.
     *
     * @return {@link #DEFAULT_ITERATOR_TYPE}
     */
    protected static String getDefaultIteratorType() {
        return DEFAULT_ITERATOR_TYPE;
    }

}
