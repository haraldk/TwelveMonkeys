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

import com.twelvemonkeys.lang.StringUtil;

import java.sql.*;
import java.io.*;
import java.util.Properties;


/**
 * A class used to test a JDBC database connection. It can also be used as a
 * really simple form of command line SQL interface, that passes all command
 * line parameters to the database as plain SQL, and returns all rows to
 * Sytem.out. <EM>Be aware that the wildcard character (*) is intercepted by
 * the console, so you have to quote your string, or escape the wildcard
 * character, otherwise you may get unpredictable results.</EM>
 * <P/>
 * <STRONG>Exmaple use</STRONG>
 * <BR/>
 * <PRE>
 * $ java -cp lib\jconn2.jar;build com.twelvemonkeys.sql.SQLUtil
 * -d com.sybase.jdbc2.jdbc.SybDriver -u "jdbc:sybase:Tds:10.248.136.42:6100"
 * -l scott -p tiger "SELECT * FROM emp"</PRE>
 * <EM>Make sure sure to include the path to your JDBC driver in the java class
 * path!</EM>
 *
 * @author Philippe Béal (phbe@iconmedialab.no)
 * @author Harald Kuhr (haraldk@iconmedialab.no)
 * @author last modified by $author: WMHAKUR $
 * @version $id: $
 * @see DatabaseConnection
 */
public class SQLUtil {
    /**
     * Method main
     *
     * @param pArgs
     * @throws SQLException
     *
     * @todo Refactor the long and ugly main method...
     * Consider: - extract method parserArgs(String[])::Properties (how do we
     *             get the rest of the arguments? getProperty("_ARGV")?
     *             Make the Properties/Map an argument and return int with last
     *             option index?
     *           - extract method getStatementReader(Properties)
     */
    public static void main(String[] pArgs) throws SQLException, IOException {
        String user = null;
        String password = null;
        String url = null;
        String driver = null;
        String configFileName = null;
        String scriptFileName = null;
        String scriptSQLDelim = "go";
        int argIdx = 0;
        boolean errArgs = false;

        while ((argIdx < pArgs.length) && (pArgs[argIdx].charAt(0) == '-') && (pArgs[argIdx].length() >= 2)) {
            if ((pArgs[argIdx].charAt(1) == 'l') || pArgs[argIdx].equals("--login")) {
                argIdx++;
                user = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'p') || pArgs[argIdx].equals("--password")) {
                argIdx++;
                password = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'u') || pArgs[argIdx].equals("--url")) {
                argIdx++;
                url = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'd') || pArgs[argIdx].equals("--driver")) {
                argIdx++;
                driver = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'c') || pArgs[argIdx].equals("--config")) {
                argIdx++;
                configFileName = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 's') || pArgs[argIdx].equals("--script")) {
                argIdx++;
                scriptFileName = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'h') || pArgs[argIdx].equals("--help")) {
                argIdx++;
                errArgs = true;
            }
            else {
                System.err.println("Unknown option \"" + pArgs[argIdx++] + "\"");
            }
        }
        if (errArgs || (scriptFileName == null && (pArgs.length < (argIdx + 1)))) {
            System.err.println("Usage: SQLUtil [--help|-h] [--login|-l <login-name>] [--password|-p <password>] [--driver|-d <jdbc-driver-class>] [--url|-u <connect url>] [--config|-c <config-file>] [--script|-s <script-file>] <sql statement> ");
            System.exit(5);
        }

        // If config file, read config and use as defaults
        // NOTE: Command line options override!
        if (!StringUtil.isEmpty(configFileName)) {
            Properties config = new Properties();
            File configFile = new File(configFileName);
            if (!configFile.exists()) {
                System.err.println("Config file " + configFile.getAbsolutePath() + " does not exist.");
                System.exit(10);
            }

            InputStream in = new FileInputStream(configFile);
            try {
                config.load(in);
            }
            finally {
                in.close();
            }

            if (driver == null) {
                driver = config.getProperty("driver");
            }
            if (url == null) {
                url = config.getProperty("url");
            }
            if (user == null) {
                user = config.getProperty("login");
            }
            if (password == null) {
                password = config.getProperty("password");
            }
        }

        // Register JDBC driver
        if (driver != null) {
            registerDriver(driver);
        }
        Connection conn = null;

        try {
            // Use default connection from DatabaseConnection.properties
            conn = DatabaseConnection.getConnection(user, password, url);
            if (conn == null) {
                System.err.println("No connection.");
                System.exit(10);
            }

            BufferedReader reader;
            if (scriptFileName != null) {
                // Read SQL from file
                File file = new File(scriptFileName);
                if (!file.exists()) {
                    System.err.println("Script file " + file.getAbsolutePath() + " does not exist.");
                    System.exit(10);
                }

                reader = new BufferedReader(new FileReader(file));
            }
            else {
                // Create SQL statement from command line params
                StringBuilder sql = new StringBuilder();
                for (int i = argIdx; i < pArgs.length; i++) {
                    sql.append(pArgs[i]).append(" ");
                }

                reader = new BufferedReader(new StringReader(sql.toString()));
            }

            //reader.mark(10000000);
            //for (int i = 0; i < 5; i++) {
            StringBuilder sql = new StringBuilder();
            while (true) {
                // Read next line
                String line = reader.readLine();
                if (line == null) {
                    // End of file, execute and quit
                    String str = sql.toString();
                    if (!StringUtil.isEmpty(str)) {
                        executeSQL(str, conn);
                    }
                    break;
                }
                else if (line.trim().endsWith(scriptSQLDelim)) {
                    // End of statement, execute and continue
                    sql.append(line.substring(0, line.lastIndexOf(scriptSQLDelim)));
                    executeSQL(sql.toString(), conn);
                    sql.setLength(0);
                }
                else {
                    sql.append(line).append(" ");
                }
            }
            //reader.reset();
            //}
        }
        finally {
            // Close the connection
            if (conn != null) {
                conn.close();
            }
        }
    }

    private static void executeSQL(String pSQL, Connection pConn) throws SQLException {
        System.out.println("Executing: " + pSQL);

        Statement stmt = null;
        try {
            // NOTE: Experimental
            //stmt = pConn.prepareCall(pSQL);
            //boolean results = ((CallableStatement) stmt).execute();

            // Create statement and execute
            stmt = pConn.createStatement();
            boolean results = stmt.execute(pSQL);

            int updateCount = -1;

            SQLWarning warning = stmt.getWarnings();
            while (warning != null) {
                System.out.println("Warning: " + warning.getMessage());
                warning = warning.getNextWarning();
            }

            // More result sets to process?
            while (results || (updateCount = stmt.getUpdateCount()) != -1) {
                // INSERT, UPDATE or DELETE statement (no result set).
                if (!results && (updateCount >= 0)) {
                    System.out.println("Operation successfull. " + updateCount + " row" + ((updateCount != 1) ? "s" : "") + " affected.");
                    System.out.println();
                }
                // SELECT statement or stored procedure
                else {
                    processResultSet(stmt.getResultSet());
                }

                // More results?
                results = stmt.getMoreResults();
            }
        }
        catch (SQLException sqle) {
            System.err.println("Error: " + sqle.getMessage());
            while ((sqle = sqle.getNextException()) != null) {
                System.err.println("       " + sqle);
            }
        }
        finally {
            // Close the statement
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    // TODO: Create interface ResultSetProcessor
    //       -- processWarnings(SQLWarning pWarnings);
    //       -- processMetaData(ResultSetMetaData pMetas); ??
    //       -- processResultSet(ResultSet pResult);
    // TODO: Add parameter pResultSetProcessor to method
    // TODO: Extract contents of this method to class Default/CLIRSP
    // TODO: Create new class JTableRSP that creates (?) and populates a JTable
    //       or a TableModel (?)
    private static void processResultSet(ResultSet pResultSet) throws SQLException {
        try {
            // Get meta data
            ResultSetMetaData meta = pResultSet.getMetaData();

            // Print any warnings that might have occured
            SQLWarning warning = pResultSet.getWarnings();
            while (warning != null) {
                System.out.println("Warning: " + warning.getMessage());
                warning = warning.getNextWarning();
            }

            // Get the number of columns in the result set
            int numCols = meta.getColumnCount();

            for (int i = 1; i <= numCols; i++) {
                boolean prepend = isNumeric(meta.getColumnType(i));

                String label = maybePad(meta.getColumnLabel(i), meta.getColumnDisplaySize(i), " ", prepend);

                System.out.print(label + "\t");
            }
            System.out.println();
            for (int i = 1; i <= numCols; i++) {
                boolean prepend = isNumeric(meta.getColumnType(i));
                String label = maybePad("(" + meta.getColumnTypeName(i) + "/" + meta.getColumnClassName(i) + ")", meta.getColumnDisplaySize(i), " ", prepend);
                System.out.print(label + "\t");
            }
            System.out.println();
            for (int i = 1; i <= numCols; i++) {
                String label = maybePad("", meta.getColumnDisplaySize(i), "-", false);
                System.out.print(label + "\t");
            }
            System.out.println();
            while (pResultSet.next()) {
                for (int i = 1; i <= numCols; i++) {
                    boolean prepend = isNumeric(meta.getColumnType(i));
                    String value = maybePad(String.valueOf(pResultSet.getString(i)), meta.getColumnDisplaySize(i), " ", prepend);
                    System.out.print(value + "\t");
                    //System.out.print(pResultSet.getString(i) + "\t");
                }
                System.out.println();
            }
            System.out.println();
        }
        catch (SQLException sqle) {
            System.err.println("Error: " + sqle.getMessage());
            while ((sqle = sqle.getNextException()) != null) {
                System.err.println("       " + sqle);
            }
            throw sqle;
        }
        finally {
            if (pResultSet != null) {
                pResultSet.close();
            }
        }
    }

    private static String maybePad(String pString, int pColumnDisplaySize, String pPad, boolean pPrepend) {
        String padded;
        if (pColumnDisplaySize < 100) {
             padded = StringUtil.pad(pString, pColumnDisplaySize, pPad, pPrepend);
        }
        else {
            padded = StringUtil.pad(pString, 100, pPad, pPrepend);
        }
        return padded;
    }

    private static boolean isNumeric(int pColumnType) {
        return (pColumnType == Types.INTEGER || pColumnType == Types.DECIMAL
                || pColumnType == Types.TINYINT || pColumnType == Types.BIGINT
                || pColumnType == Types.DOUBLE || pColumnType == Types.FLOAT
                || pColumnType == Types.NUMERIC || pColumnType == Types.REAL
                || pColumnType == Types.SMALLINT);
    }

    public static boolean isDriverAvailable(String pDriver) {
        //ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(pDriver, false, null); // null means the caller's ClassLoader
            return true;
        }
        catch (ClassNotFoundException ignore) {
            // Ignore
        }
        return false;
    }

    public static void registerDriver(String pDriver) {
        // Register JDBC driver
        try {
            Class.forName(pDriver).newInstance();
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver class not found: " + e.getMessage(), e);
            //System.err.println("Driver class not found: " + e.getMessage());
            //System.exit(5);
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Driver class could not be instantiated: " + e.getMessage(), e);
            //System.err.println("Driver class could not be instantiated: " + e.getMessage());
            //System.exit(5);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Driver class could not be instantiated: " + e.getMessage(), e);
            //System.err.println("Driver class could not be instantiated: " + e.getMessage());
            //System.exit(5);
        }
    }
}