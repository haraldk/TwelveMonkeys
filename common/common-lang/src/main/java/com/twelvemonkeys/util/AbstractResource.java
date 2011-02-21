/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.util;

/**
 * AbstractResource class description.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/AbstractResource.java#1 $
 */
abstract class AbstractResource implements Resource {
    protected final Object resourceId;
    protected final Object wrappedResource;

    /**
     * Creates a {@code Resource}.
     *
     * @param pResourceId
     * @param pWrappedResource
     */
    protected AbstractResource(Object pResourceId, Object pWrappedResource) {
        if (pResourceId == null) {
            throw new IllegalArgumentException("id == null");
        }
        if (pWrappedResource == null) {
            throw new IllegalArgumentException("resource == null");
        }

        resourceId = pResourceId;
        wrappedResource = pWrappedResource;
    }

    public final Object getId() {
        return resourceId;
    }

    /**
     * Default implementation simply returns {@code asURL().toExternalForm()}.
     *
     * @return a string representation of this resource
     */
    public String toString() {
        return asURL().toExternalForm();
    }

    /**
     * Defautl implementation returns {@code mWrapped.hashCode()}.
     *
     * @return {@code mWrapped.hashCode()}
     */
    public int hashCode() {
        return wrappedResource.hashCode();
    }

    /**
     * Default implementation
     *
     * @param pObject
     * @return
     */
    public boolean equals(Object pObject) {
        return pObject instanceof AbstractResource
                && wrappedResource.equals(((AbstractResource) pObject).wrappedResource);
    }
}
