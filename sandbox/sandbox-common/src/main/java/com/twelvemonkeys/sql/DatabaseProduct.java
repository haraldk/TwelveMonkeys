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

import java.io.Serializable;

/**
 * DatabaseProduct
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sql/DatabaseProduct.java#1 $
 */
public final class DatabaseProduct implements Serializable {
    private static final String UNKNOWN_NAME = "Unknown";
    private static final String GENERIC_NAME = "Generic";
    private static final String CACHE_NAME = "Caché";
    private static final String DB2_NAME = "DB2";
    private static final String MSSQL_NAME = "MSSQL";
    private static final String ORACLE_NAME = "Oracle";
    private static final String POSTGRESS_NAME = "PostgreSQL";
    private static final String SYBASE_NAME = "Sybase";

    /*public*/ static final DatabaseProduct UNKNOWN = new DatabaseProduct(UNKNOWN_NAME);
    public static final DatabaseProduct GENERIC = new DatabaseProduct(GENERIC_NAME);
    public static final DatabaseProduct CACHE = new DatabaseProduct(CACHE_NAME);
    public static final DatabaseProduct DB2 = new DatabaseProduct(DB2_NAME);
    public static final DatabaseProduct MSSQL = new DatabaseProduct(MSSQL_NAME);
    public static final DatabaseProduct ORACLE = new DatabaseProduct(ORACLE_NAME);
    public static final DatabaseProduct POSTGRES = new DatabaseProduct(POSTGRESS_NAME);
    public static final DatabaseProduct SYBASE = new DatabaseProduct(SYBASE_NAME);

    private static final DatabaseProduct[] VALUES = {
        GENERIC, CACHE, DB2, MSSQL, ORACLE, POSTGRES, SYBASE,
    };

    private static int sNextOrdinal = -1;
    private final int mOrdinal = sNextOrdinal++;

    private final String mKey;

    private DatabaseProduct(String pName) {
        mKey = pName;
    }

    static int enumSize() {
        return sNextOrdinal;
    }

    final int id() {
        return mOrdinal;
    }

    final String key() {
        return mKey;
    }

    public String toString() {
        return mKey + " [id=" + mOrdinal+ "]";
    }

    /**
     * Gets the {@code DatabaseProduct} known by the given name.
     *
     * @param pName
     * @return the {@code DatabaseProduct} known by the given name
     * @throws IllegalArgumentException if there's no such name
     */
    public static DatabaseProduct resolve(String pName) {
        if ("ANSI".equalsIgnoreCase(pName) || GENERIC_NAME.equalsIgnoreCase(pName)) {
            return GENERIC;
        }
        else if ("Cache".equalsIgnoreCase(pName) || CACHE_NAME.equalsIgnoreCase(pName)) {
            return CACHE;
        }
        else if (DB2_NAME.equalsIgnoreCase(pName)) {
            return DB2;
        }
        else if (MSSQL_NAME.equalsIgnoreCase(pName)) {
            return MSSQL;
        }
        else if (ORACLE_NAME.equalsIgnoreCase(pName)) {
            return ORACLE;
        }
        else if ("Postgres".equalsIgnoreCase(pName) || POSTGRESS_NAME.equalsIgnoreCase(pName)) {
            return POSTGRES;
        }
        else if (SYBASE_NAME.equalsIgnoreCase(pName)) {
            return SYBASE;
        }
        else {
            throw new IllegalArgumentException("Unknown database product \"" + pName
                    + "\", try any of the known products, or \"Generic\"");
        }
    }

    private Object readResolve() {
        return VALUES[mOrdinal]; // Canonicalize
    }
}
