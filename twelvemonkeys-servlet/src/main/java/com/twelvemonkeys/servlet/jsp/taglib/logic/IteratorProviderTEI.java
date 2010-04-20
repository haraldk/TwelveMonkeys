
package com.twelvemonkeys.servlet.jsp.taglib.logic;

import javax.servlet.jsp.tagext.*;

/**
 * TagExtraInfo class for IteratorProvider tags.
 *
 * @author Harald Kuhr
 * @version $id: $
 */
public class IteratorProviderTEI extends TagExtraInfo {
    /**
     * Gets the variable info for IteratorProvider tags. The attribute with the
     * name defined by the "id" attribute and type defined by the "type"
     * attribute is declared with scope {@code VariableInfo.AT_END}.
     *
     * @param pData TagData instance provided by container
     * @return an VariableInfo array of lenght 1, containing the attribute
     * defined by the id parameter, declared, and with scope
     * {@code VariableInfo.AT_END}.
     */
    public VariableInfo[] getVariableInfo(TagData pData) {
        // Get attribute name
        String attributeName = pData.getId();
        if (attributeName == null) {
            attributeName = IteratorProviderTag.getDefaultIteratorName();
        }

        // Get type
        String type = pData.getAttributeString(IteratorProviderTag.ATTRIBUTE_TYPE);
        if (type == null) {
            type = IteratorProviderTag.getDefaultIteratorType();
        }

        // Return the variable info
        return new VariableInfo[]{
            new VariableInfo(attributeName, type, true, VariableInfo.AT_END),
        };
    }
}
