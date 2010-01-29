package com.twelvemonkeys.imageio.plugins.psd;

import org.w3c.dom.Node;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import java.util.Arrays;

/**
 * AbstractMetadata
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: AbstractMetadata.java,v 1.0 Nov 13, 2009 1:02:12 AM haraldk Exp$
 */
abstract class AbstractMetadata extends IIOMetadata implements Cloneable {
    // TODO: Move to core...

    protected AbstractMetadata(final boolean pStandardFormatSupported,
                               final String pNativeFormatName, final String pNativeFormatClassName,
                               final String[] pExtraFormatNames, final String[] pExtraFormatClassNames) {
        super(pStandardFormatSupported, pNativeFormatName, pNativeFormatClassName, pExtraFormatNames, pExtraFormatClassNames);
    }

    /**
     * Default implementation returns {@code true}.
     * Mutable subclasses should override this method.
     *
     * @return {@code true}.
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public Node getAsTree(final String pFormatName) {
        validateFormatName(pFormatName);

        if (pFormatName.equals(nativeMetadataFormatName)) {
            return getNativeTree();
        }
        else if (pFormatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return getStandardTree();
        }

        // TODO: What about extra formats??
        throw new AssertionError("Unreachable");
    }

    @Override
    public void mergeTree(final String pFormatName, final Node pRoot) throws IIOInvalidTreeException {
        assertMutable();

        validateFormatName(pFormatName);

        if (!pRoot.getNodeName().equals(nativeMetadataFormatName)) {
            throw new IIOInvalidTreeException("Root must be " + nativeMetadataFormatName, pRoot);
        }

        Node node = pRoot.getFirstChild();
        while (node != null) {
            // TODO: Merge values from node into this

            // Move to the next sibling
            node = node.getNextSibling();
        }
    }

    @Override
    public void reset() {
        assertMutable();
    }

    /**
     * Asserts that this meta data is mutable.
     *
     * @throws IllegalStateException if {@link #isReadOnly()} returns {@code true}.
     */
    protected final void assertMutable() {
        if (isReadOnly()) {
            throw new IllegalStateException("Metadata is read-only");
        }
    }

    protected abstract Node getNativeTree();

    protected final void validateFormatName(final String pFormatName) {
        String[] metadataFormatNames = getMetadataFormatNames();

        if (metadataFormatNames != null) {
            for (String metadataFormatName : metadataFormatNames) {
                if (metadataFormatName.equals(pFormatName)) {
                    return; // Found, we're ok!
                }
            }
        }

        throw new IllegalArgumentException(
                String.format("Bad format name: \"%s\". Expected one of %s", pFormatName, Arrays.toString(metadataFormatNames))
        );
    }
}
