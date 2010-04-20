/*
 * Produced (p) 2002 TwelveMonkeys
 * Address : Svovelstikka 1, Box 6432 Etterstad, 0605 Oslo, Norway.
 * Phone   : +47 22 57 70 00
 * Fax     : +47 22 57 70 70
 */

package com.twelvemonkeys.servlet.jsp.taglib.logic;


import com.twelvemonkeys.lang.StringUtil;

import javax.servlet.http.Cookie;
import javax.servlet.jsp.JspException;


/**
 * <p>
 * Custom tag for testing non-equality of an attribute against a given value.
 * The attribute types supported so far is:
 * <ul>
 *  <li>{@code java.lang.String} (ver. 1.0)
 *  <li>{@code javax.servlet.http.Cookie} (ver. 1.0)
 * </ul>
 * </p>
 * See the implemented <a href="#condition">{@code condition}</a> method for details regarding the non-equality conditions.
 *
 * <p><hr></p>
 *
 * <h3>Tag Reference</h3>
 * <table border="0" cellspacing="3" cellpadding="3" width="90%">
 * <tr bgcolor="#cccccc">
 *  <td colspan="5" class="body"><b>notEqual</b></td>
 *  <td width="17%" align="right" class="body">Availability:&nbsp;1.0</td>
 * </tr>
 * <tr>
 *  <td colspan="6" class="body"><p>Tag for testing if an attribute is NOT equal to a given value.</p></td>
 * </tr>
 * <tr>
 *  <td width="15%" class="body"><b>Tag Body</b></td>
 *  <td width="17%" class="body">JSP</td>
 *  <td width="17%" class="body">&nbsp;</td>
 *  <td width="17%" class="body">&nbsp;</td>
 *  <td width="17%" class="body">&nbsp;</td>
 *  <td width="17%" class="body">&nbsp;</td>
 * </tr>
 * <tr>
 *  <td class="body"><b>Restrictions</b></td>
 *  <td colspan="5" class="body"><p>None</p></td>
 * </tr>
 *
 * <tr>
 *  <td class="body"><b>Attributes</b></td>
 *  <td class="body">Name</td>
 *  <td class="body">Required</td>
 *  <td colspan="2" class="body">Runtime&nbsp;Expression&nbsp;Evaluation</td>
 *  <td class="body">Availability</td>
 * </tr>
 *
 * <tr bgcolor="#cccccc">
 *  <td bgcolor="#ffffff">&nbsp;</td>
 *  <td class="body_grey"><b>name</b></td>
 *  <td class="body_grey">&nbsp;Yes</td>
 *  <td colspan="2" class="body_grey">&nbsp;Yes</td>
 *  <td class="body_grey">&nbsp;1.0</td>
 * </tr>
 * <tr>
 *  <td bgcolor="#ffffff">&nbsp;</td>
 *  <td colspan="5" class="body"><p>The attribute name</p></td>
 * </tr>
 *
 * <tr bgcolor="#cccccc">
 *  <td bgcolor="#ffffff">&nbsp;</td>
 *  <td class="body_grey"><b>value</b></td>
 *  <td class="body_grey">&nbsp;No</td>
 *  <td colspan="2" class="body_grey">&nbsp;Yes</td>
 *  <td class="body_grey">&nbsp;1.0</td>
 * </tr>
 * <tr>
 *  <td bgcolor="#ffffff" class="body">&nbsp;</td>
 *  <td colspan="5" class="body"><p>The value for equality testing</p></td>
 * </tr>
 *
 * <tr>
 *  <td class="body" valign="top"><b>Variables</b></td>
 *  <td colspan="5" class="body">None</td>
 * </tr>
 *
 * <tr>
 *  <td class="body" valign="top"><b>Examples</b></td>
 *  <td colspan="5" class="body">
 *      <pre>
 *&lt;%@ taglib prefix="twelvemonkeys" uri="twelvemonkeys-logic" %&gt;
 *&lt;bean:cookie id="logonUsernameCookie"
 *    name="&lt;%= com.strutscommand.Constants.LOGON_USERNAME_COOKIE_NAME %&gt;"
 *    value="no_username_set" /&gt;
 *&lt;twelvemonkeys:notEqual name="logonUsernameCookie" value="no_username_set"&gt;
 *    &lt;html:text property="username" value="&lt;%= logonUsernameCookie.getValue() %&gt;" /&gt;
 *&lt;/twelvemonkeys:notEqual&gt;
 *      </pre>
 *  </td>
 * </tr>
 * </table>
 *
 * <hr>
 *
 * @version 1.0
 * @author <a href="mailto:eirik.torske@twelvemonkeys.no">Eirik Torske</a>
 * @see <a href="EqualTag.html">equal</a>
 */
public class NotEqualTag extends ConditionalTagBase {

    /**
     * <a name="condition"></a>
     *
     * The condition that must be met in order to display the body of this tag:
     * <ol>
     *  <li>The attribute name property ({@code name} -> {@code mObjectName}) must not be empty.
     *  <li>The attribute must exist.
     *  <li>The attribute must be an instance of one of the supported classes:
     *      <ul>
     *          <li>{@code java.lang.String}
     *          <li>{@code javax.servlet.http.Cookie}
     *      </ul>
     *  <li>The value of the attribute must NOT be equal to the object value property ({@code value} -> {@code mObjectValue}).
     * </ol>
     * <p>
     * NB! If the object value property ({@code value} -> {@code mObjectValue}) is empty than {@code true} will be returned.
     * </p>
     *
     * @return {@code true} if and only if all conditions are met.
     */
    protected boolean condition() throws JspException {

        if (StringUtil.isEmpty(mObjectName)) {
            return false;
        }

        if (StringUtil.isEmpty(mObjectValue)) {
            return true;
        }

        Object pageScopedAttribute = pageContext.getAttribute(mObjectName);
        if (pageScopedAttribute == null) {
            return false;
        }

        String pageScopedStringAttribute;

        // String
        if (pageScopedAttribute instanceof String) {
            pageScopedStringAttribute = (String) pageScopedAttribute;

            // Cookie
        }
        else if (pageScopedAttribute instanceof Cookie) {
            pageScopedStringAttribute = ((Cookie) pageScopedAttribute).getValue();

            // Type not yet supported...
        }
        else {
            return false;
        }

        return (!(pageScopedStringAttribute.equals(mObjectValue)));
    }

}
