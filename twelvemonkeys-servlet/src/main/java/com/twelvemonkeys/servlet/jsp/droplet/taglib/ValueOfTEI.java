/*
 * Copyright (c) 2002 TwelveMonkeys.
 * All rights reserved.
 *
 * $Log: ValueOfTEI.java,v $
 * Revision 1.3  2003/10/06 14:26:07  WMHAKUR
 * Code clean-up only.
 *
 * Revision 1.2  2002/10/18 14:28:07  WMHAKUR
 * Fixed package error.
 *
 * Revision 1.1  2002/10/18 14:03:52  WMHAKUR
 * Moved to com.twelvemonkeys.servlet.jsp.droplet.taglib
 *
 *
 */

package com.twelvemonkeys.servlet.jsp.droplet.taglib;

import java.io.IOException;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * TagExtraInfo for ValueOf.
 * @todo More meaningful response to the user.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Revision: #1 $, ($Date: 2008/05/05 $)
 *
 */

public class ValueOfTEI extends TagExtraInfo {

    public boolean isValid(TagData pTagData) {
        Object nameAttr = pTagData.getAttribute("name");
        Object paramAttr = pTagData.getAttribute("param");

        if ((nameAttr != null && paramAttr == null) ||
                (nameAttr == null && paramAttr != null)) {
            return true; // Exactly one of name or param set
        }

        // Either both or none,
        return false;
    }
}
