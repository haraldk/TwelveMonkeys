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

import com.twelvemonkeys.lang.SystemUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A class that holds a JDBC Connection. The class can be configured by a 
 * properties file. However, the approach is rather lame, and only lets you
 * configure one connection... 
 * <P/>
 * Tested with jConnect (Sybase), I-net Sprinta2000 (MS SQL) and Oracle.
 * <P/>
 * @todo be able to register more drivers, trough properties and runtime
 * @todo be able to register more connections, trough properties and runtime
 * <P/>
 * <STRONG>Example properties file</STRONG></BR>
 * # filename: com.twelvemonkeys.sql.DatabaseConnection.properties
 * driver=com.inet.tds.TdsDriver
 * url=jdbc:inetdae7:127.0.0.1:1433?database\=mydb
 * user=scott
 * password=tiger
 * # What do you expect, really?
 * logDebug=true
 *
 * @author Philippe Béal (phbe@iconmedialab.no)
 * @author Harald Kuhr (haraldk@iconmedialab.no)
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sql/DatabaseConnection.java#1 $
 *
 * @todo Use org.apache.commons.logging instead of proprietary logging.
 *
 */

public class DatabaseConnection {

    // Default driver
    public final static String DEFAULT_DRIVER = "NO_DRIVER";
    // Default URL
    public final static String DEFAULT_URL = "NO_URL";

    protected static String mDriver = null;
    protected static String mUrl = null;

    // Default debug is true
    //    private static boolean debug = true;
    
    protected static Properties mConfig = null;
    //protected static Log mLog = null;
    protected static Log mLog = null;

    protected static boolean mInitialized = false;

    // Must be like this...
    // http://www.javaworld.com/javaworld/jw-02-2001/jw-0209-double.html :-)
    private static DatabaseConnection sInstance = new DatabaseConnection();

    /**
     * Creates the DatabaseConnection.
     */

    private DatabaseConnection() {
        init();
    }

    /**
     * Gets the single DatabaseConnection instance.
     */

    protected static DatabaseConnection getInstance() {
        /*
          if (sInstance == null) {
          sInstance = new DatabaseConnection();
          sInstance.init();
          }
        */
        return sInstance;
    }

    /**
     * Initializes the DatabaseConnection, called from the constructor.
     *
     * @exception IllegalStateException if an attempt to call init() is made
     * after the instance is allready initialized.
     */

    protected synchronized void init() {
        // Make sure init is executed only once!
        if (mInitialized) {
            throw new IllegalStateException("init() may only be called once!");
        }

        mInitialized = true;

        try {
            mConfig = SystemUtil.loadProperties(DatabaseConnection.class);
        }
        catch (FileNotFoundException fnf) {
            // Ignore
        }
        catch (IOException ioe) {
            //LogFactory.getLog(getClass()).error("Caught IOException: ", ioe);
            new Log(this).logError(ioe);
            //ioe.printStackTrace();
        }
        finally {
            if (mConfig == null) {
                mConfig = new Properties();
            }
        }

        mLog = new Log(this, mConfig);
        //mLog = LogFactory.getLog(getClass());
        // debug = new Boolean(config.getProperty("debug", "true")).booleanValue();	
        // config.list(System.out);

        mDriver = mConfig.getProperty("driver", DEFAULT_DRIVER);
        mUrl = mConfig.getProperty("url", DEFAULT_URL);
    }

    /**
     * Gets the default JDBC Connection. The connection is configured through 
     * the properties file.
     *
     * @return the default jdbc Connection
     */

    public static Connection getConnection() {
        return getConnection(null, null, getInstance().mUrl);
    }

    /**
     * Gets a JDBC Connection with the given parameters. The connection is 
     * configured through the properties file.
     *
     * @param pUser the database user name
     * @param pPassword the password of the database user
     * @param pURL the url to connect to
     *
     * @return a jdbc Connection
     */

    public static Connection getConnection(String pUser,
                                           String pPassword,
                                           String pURL) {
        return getInstance().getConnectionInstance(pUser, pPassword, pURL);
	
    }

    /**
     * Gets a JDBC Connection with the given parameters. The connection is 
     * configured through the properties file.
     *
     * @param pUser the database user name
     * @param pPassword the password of the database user
     * @param pURL the url to connect to
     *
     * @return a jdbc Connection
     */    

    protected Connection getConnectionInstance(String pUser,
                                               String pPassword,
                                               String pURL) {
        Properties props = (Properties) mConfig.clone();

        if (pUser != null) {
            props.put("user", pUser);
        }
        if (pPassword != null) {
            props.put("password", pPassword);
        }

        // props.list(System.out);

        try {
            // Load & register the JDBC Driver
            if (!DEFAULT_DRIVER.equals(mDriver)) {
                Class.forName(mDriver).newInstance();
            }

            Connection conn = DriverManager.getConnection(pURL, props);

            if (mLog.getLogDebug()) {
                //if (mLog.isDebugEnabled()) {
                DatabaseMetaData dma = conn.getMetaData();
                mLog.logDebug("Connected to " + dma.getURL());
                mLog.logDebug("Driver       " + dma.getDriverName());
                mLog.logDebug("Version      " + dma.getDriverVersion());

                //mLog.debug("Connected to " + dma.getURL());
                //mLog.debug("Driver       " + dma.getDriverName());
                //mLog.debug("Version      " + dma.getDriverVersion());
            }
	    
            return conn;
        }
        catch (Exception e) {
            mLog.logError(e.getMessage());

            // Get chained excpetions
            if (e instanceof SQLException) {
                SQLException sqle = (SQLException) e;
                while ((sqle = sqle.getNextException()) != null) {
                    mLog.logWarning(sqle);
                }
            }
        }
        return null;
    }

}

