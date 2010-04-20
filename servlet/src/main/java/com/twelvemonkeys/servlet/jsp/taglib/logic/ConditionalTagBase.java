/****************************************************
 *                                                  *
 *             (c) 2000-2003 TwelveMonkeys                *
 *             All rights reserved                  *
 *             http://www.twelvemonkeys.no                 *
 *                                                  *
 *   $RCSfile: ConditionalTagBase.java,v $                
 *   @version  $Revision: #1 $                            
 *   $Date: 2008/05/05 $                    
 *                                                  * 
 *   @author  Last modified by: $Author: haku $                            
 *                                                  *
 ****************************************************/



/*
 * Produced (p) 2002 TwelveMonkeys
 * Address : Svovelstikka 1, Box 6432 Etterstad, 0605 Oslo, Norway.
 * Phone   : +47 22 57 70 00
 * Fax     : +47 22 57 70 70
 */
package com.twelvemonkeys.servlet.jsp.taglib.logic;


import java.lang.*;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;


/**
 * <p>An abstract base class for tags with some kind of conditional presentation of the tag body.</p>
 *
 * @version 1.0
 * @author <a href="mailto:eirik.torske@twelvemonkeys.no">Eirik Torske</a>
 */
public abstract class ConditionalTagBase extends TagSupport {

  // Members
  protected String mObjectName;
  protected String mObjectValue;

  // Properties

  /**
   * Method getName
   *
   *
   * @return
   *
   */
  public String getName() {
    return mObjectName;
  }

  /**
   * Method setName
   *
   *
   * @param pObjectName
   *
   */
  public void setName(String pObjectName) {
    this.mObjectName = pObjectName;
  }

  /**
   * Method getValue
   *
   *
   * @return
   *
   */
  public String getValue() {
    return mObjectValue;
  }

  /**
   * Method setValue
   *
   *
   * @param pObjectValue
   *
   */
  public void setValue(String pObjectValue) {
    this.mObjectValue = pObjectValue;
  }

  /**
   * <p>Perform the test required for this particular tag, and either evaluate or skip the body of this tag.</p>
   *
   *
   * @return
   * @exception JspException if a JSP exception occurs.
   */
  public int doStartTag() throws JspException {

    if (condition()) {
      return (EVAL_BODY_INCLUDE);
    } else {
      return (SKIP_BODY);
    }
  }

  /**
   * <p>Evaluate the remainder of the current page as normal.</p>
   *
   *
   * @return
   * @exception JspException if a JSP exception occurs.
   */
  public int doEndTag() throws JspException {
    return (EVAL_PAGE);
  }

  /**
   * <p>Release all allocated resources.</p>
   */
  public void release() {

    super.release();
    mObjectName  = null;
    mObjectValue = null;
  }

  /**
   * <p>The condition that must be met in order to display the body of this tag.</p>
   *
   * @exception JspException if a JSP exception occurs.
   * @return {@code true} if and only if all conditions are met.
   */
  protected abstract boolean condition() throws JspException;
}


/*--- Formatted in Sun Java Convention Style on ma, des 1, '03 ---*/


/*------ Formatted by Jindent 3.23 Basic 1.0 --- http://www.jindent.de ------*/
