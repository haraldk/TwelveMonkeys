
package com.twelvemonkeys.servlet.jsp.taglib;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * TagExtraInfo for LastModifiedTag
 *
 * @author Harald Kuhr
 *
 * @version 1.1
 */

public class LastModifiedTEI extends TagExtraInfo {
    public VariableInfo[] getVariableInfo(TagData pData) {
        return new VariableInfo[]{
            new VariableInfo("lastModified", "java.lang.String", true, VariableInfo.NESTED),
        };
    }
}
