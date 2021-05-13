/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata.psd;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;
import com.twelvemonkeys.lang.StringUtil;

import java.lang.reflect.Field;

/**
 * PhotoshopEntry
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PhotoshopEntry.java,v 1.0 04.01.12 11:58 haraldk Exp$
 */
class PSDEntry extends AbstractEntry {
    private final String name;

    public PSDEntry(final int resourceId, String name, final Object value) {
        super(resourceId, value);
        this.name = StringUtil.isEmpty(name) ? null : name;
    }

    @Override
    protected String getNativeIdentifier() {
        return String.format("0x%04x", (Integer) getIdentifier());
    }

    @Override
    public String getFieldName() {
        Class[] classes = new Class[] {getPSDClass()};

        for (Class cl : classes) {
            Field[] fields = cl.getDeclaredFields();

            for (Field field : fields) {
                try {
                    if (field.getType() == Integer.TYPE && field.getName().startsWith("RES_")) {
                        field.setAccessible(true);

                        if (field.get(null).equals(getIdentifier())) {
                            String fieldName = StringUtil.lispToCamel(field.getName().substring(4).replace("_", "-").toLowerCase(), true);
                            return name != null ? fieldName + ": " + name : fieldName;
                        }
                    }
                }
                catch (IllegalAccessException e) {
                    // Should never happen, but in case, abort
                    break;
                }
            }
        }

        return name;
    }

    private Class<?> getPSDClass() {
        // TODO: Instead, move members to metadata module PSD class!
        try {
            return Class.forName("com.twelvemonkeys.imageio.plugins.psd.PSD");
        }
        catch (ClassNotFoundException ignore) {
        }
        
        return PSD.class;
    }
}
