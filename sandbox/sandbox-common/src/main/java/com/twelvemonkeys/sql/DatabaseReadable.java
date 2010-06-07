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

package com.twelvemonkeys.sql;

import java.util.Hashtable;

/**
 * Interface for classes that is to be read from a database, using the 
 * ObjectReader class.  
 *
 * @author Harald Kuhr (haraldk@iconmedialab.no)
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sql/DatabaseReadable.java#1 $
 *
 * @todo Use JDK logging instead of proprietary logging.
 *
 */
public interface DatabaseReadable {
    
    /**
     * Gets the unique identifier of this DatabaseReadable object.
     *
     * @return An object that uniqely identifies this DatabaseReadable.
     */

    public Object getId();

    /**
     * Sets the unique identifier of this DatabaseReadable object.
     *
     * @param id An object that uniqely identifies this DatabaseReadable.
     */

    public void setId(Object id);
 
    /**
     * Gets the object to database mapping of this DatabaseReadable.
     *
     * @return A Hashtable cotaining the database mapping for this 
     * DatabaseReadable.
     */
    
    public Hashtable getMapping();
}
