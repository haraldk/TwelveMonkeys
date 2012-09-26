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

/**
 * AbstractHelper
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sql/JDBCHelper.java#1 $
 */
public abstract class JDBCHelper {

    private static JDBCHelper[] sHelpers = new JDBCHelper[DatabaseProduct.enumSize()];

    static {
        DatabaseProduct product = DatabaseProduct.resolve(System.getProperty("com.twelvemonkeys.sql.databaseProduct", "Generic"));
        sHelpers[0] = createInstance(product);
    }

    private JDBCHelper() {
    }

    private static JDBCHelper createInstance(DatabaseProduct pProduct) {
        // Get database name
        // Instantiate helper
        if (pProduct == DatabaseProduct.GENERIC) {
            return new GenericHelper();
        }
        else if (pProduct == DatabaseProduct.CACHE) {
            return new CacheHelper();
        }
        else if (pProduct == DatabaseProduct.DB2) {
            return new DB2Helper();
        }
        else if (pProduct == DatabaseProduct.MSSQL) {
            return new MSSQLHelper();
        }
        else if (pProduct == DatabaseProduct.ORACLE) {
            return new OracleHelper();
        }
        else if (pProduct == DatabaseProduct.POSTGRES) {
            return new PostgreSQLHelper();
        }
        else if (pProduct == DatabaseProduct.SYBASE) {
            return new SybaseHelper();
        }
        else {
            throw new IllegalArgumentException("Unknown database product, try any of the known products, or \"generic\"");
        }
    }

    public final static JDBCHelper getInstance() {
        return sHelpers[0];
    }

    public final static JDBCHelper getInstance(DatabaseProduct pProuct) {
        JDBCHelper helper = sHelpers[pProuct.id()];
        if (helper == null) {
            // This is ok, iff sHelpers[pProuct] = helper is an atomic op...
            synchronized (sHelpers) {
                helper = sHelpers[pProuct.id()];
                if (helper == null) {
                    helper = createInstance(pProuct);
                    sHelpers[pProuct.id()] = helper;
                }
            }
        }
        return helper;
    }

    // Abstract or ANSI SQL implementations of different stuff

    public String getDefaultDriverName() {
        return "";
    }

    public String getDefaultURL() {
        return "jdbc:{$DRIVER}://localhost:{$PORT}/{$DATABASE}";
    }

    // Vendor specific concrete implementations

    static class GenericHelper extends JDBCHelper {
        // Nothing here
    }

    static class CacheHelper extends JDBCHelper {
        public String getDefaultDriverName() {
            return "com.intersys.jdbc.CacheDriver";
        }

        public String getDefaultURL() {
            return "jdbc:Cache://localhost:1972/{$DATABASE}";
        }
    }

    static class DB2Helper extends JDBCHelper {
        public String getDefaultDriverName() {
            return "COM.ibm.db2.jdbc.net.DB2Driver";
        }

        public String getDefaultURL() {
            return "jdbc:db2:{$DATABASE}";
        }
    }

    static class MSSQLHelper extends JDBCHelper {
        public String getDefaultDriverName() {
            return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
        }

        public String getDefaultURL() {
            return "jdbc:microsoft:sqlserver://localhost:1433;databasename={$DATABASE};SelectMethod=cursor";
        }
    }

    static class OracleHelper extends JDBCHelper {
        public String getDefaultDriverName() {
            return "oracle.jdbc.driver.OracleDriver";
        }

        public String getDefaultURL() {
            return "jdbc:oracle:thin:@localhost:1521:{$DATABASE}";
        }
    }

    static class PostgreSQLHelper extends JDBCHelper {
        public String getDefaultDriverName() {
            return "org.postgresql.Driver";
        }

        public String getDefaultURL() {
            return "jdbc:postgresql://localhost/{$DATABASE}";
        }
    }

    static class SybaseHelper extends JDBCHelper {
        public String getDefaultDriverName() {
            return "com.sybase.jdbc2.jdbc.SybDriver";
        }

        public String getDefaultURL() {
            return "jdbc:sybase:Tds:localhost:4100/";
        }
    }
}