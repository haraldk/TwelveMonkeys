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

import java.io.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Class used for logging. 
 * The class currently supports four levels of logging (debug, warning, error 
 * and info).
 * <P>
 * The class maintains a cahce of OutputStreams, to avoid more than one stream
 * logging to a specific file. The class should also be thread safe, in that no
 * more than one instance of the class can log to the same OuputStream.
 * <P>
 * <STRONG>
 * WARNING: The uniqueness of logfiles is based on filenames alone, meaning
 * "info.log" and "./info.log" will probably be treated as different files, 
 * and have different streams attatched to them.
 * </STRONG>
 * <P>
 * <STRONG>
 * WARNING: The cached OutputStreams can possibly be in error state or be 
 * closed without warning. Should be fixed in later versions!
 * </STRONG>
 *
 * @author Harald Kuhr (haraldk@iconmedialab.no)
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-sandbox/src/main/java/com/twelvemonkeys/sql/Log.java#1 $
 *
 * @deprecated Use the JDK java.util.logging for logging.
 * This class is old and outdated, and is here only for compatibility. It will 
 * be removed from the library in later releases.
 * <P>
 * All new code are strongly encouraged to use the org.apache.commons.logging
 * package for logging.
 * 
 * @see java.util.logging.Logger
 *
 */

class Log {
    private static Hashtable streamCache = new Hashtable();

    static {
        streamCache.put("System.out", System.out);
        streamCache.put("System.err", System.err);
    }

    private static Log globalLog = null;

    private String owner = null;

    private boolean logDebug = false;
    private boolean logWarning = false;
    private boolean logError = true; // Log errors!
    private boolean logInfo = false;

    private PrintStream debugLog = null;
    private PrintStream warningLog = null;
    private PrintStream errorLog = null;
    private PrintStream infoLog = null;

    /**
     * Init global log
     */

    static {
        Properties config = null;
        try {
            config = SystemUtil.loadProperties(Log.class);
        }
        catch (FileNotFoundException fnf) {
            // That's okay.
        }
        catch (IOException ioe) {
            // Not so good
            log(System.err, "ERROR", Log.class.getName(), null, ioe);
        }

        globalLog = new Log(new Log(), config);

        // Defaults
        if (globalLog.debugLog == null)
            globalLog.setDebugLog(System.out);
        if (globalLog.warningLog == null)
            globalLog.setWarningLog(System.err);
        if (globalLog.errorLog == null)
            globalLog.setErrorLog(System.err);
        if (globalLog.infoLog == null)
            globalLog.setInfoLog(System.out);

        // Info
        globalLog.logDebug("Logging system started.");
        log(globalLog.infoLog, "INFO", Log.class.getName(), 
            "Logging system started.", null);
    }

    /**
     * Internal use only
     */

    private Log() {
    }

    /**
     * Creates a log
     */

    public Log(Object owner) {
        this.owner = owner.getClass().getName();
    }

    /**
     * Creates a log
     */

    public Log(Object owner, Properties config) {
        this(owner);

        if (config == null)
            return;

        // Set logging levels
        logDebug = new Boolean(config.getProperty("logDebug", 
                                                  "false")).booleanValue();
        logWarning = new Boolean(config.getProperty("logWarning", 
                                                    "false")).booleanValue();
        logError = new Boolean(config.getProperty("logError", 
                                                  "true")).booleanValue();
        logInfo = new Boolean(config.getProperty("logInfo", 
                                                 "true")).booleanValue();

        // Set logging streams
        String fileName;
        try {
            if ((fileName = config.getProperty("debugLog")) != null)
                setDebugLog(fileName);

            if ((fileName = config.getProperty("warningLog")) != null)
                setWarningLog(fileName);

            if ((fileName = config.getProperty("errorLog")) != null)
                setErrorLog(fileName);

            if ((fileName = config.getProperty("infoLog")) != null)
                setInfoLog(fileName);
        }
        catch (IOException ioe) {
            if (errorLog == null) 
                setErrorLog(System.err);
            logError("Could not create one or more logging streams! ", ioe);
        }
    }

    /**
     * Checks if we log debug info
     *
     * @return True if logging
     */

    public boolean getLogDebug() {
        return logDebug;
    }

    /**
     * Sets wheter we are to log debug info
     *
     * @param logDebug Boolean, true if we want to log debug info
     */

    public void setLogDebug(boolean logDebug) {
        this.logDebug = logDebug;
    }

    /**
     * Checks if we globally log debug info
     *
     * @return True if global logging
     */
    /*
      public static boolean getGlobalDebug() {
      return globalDebug;
      }
    */
    /**
     * Sets wheter we are to globally log debug info
     *
     * @param logDebug Boolean, true if we want to globally log debug info
     */
    /*
      public static void setGlobalDebug(boolean globalDebug) {
      Log.globalDebug = globalDebug;
      }
      /*
      /**
      * Sets the OutputStream we want to print to
      *
      * @param os The OutputStream we will use for logging
      */

    public void setDebugLog(OutputStream os) {
        debugLog = new PrintStream(os, true);
    }

    /**
     * Sets the filename of the File we want to print to. Equivalent to 
     * setDebugLog(new FileOutputStream(fileName, true))
     *
     * @param file The File we will use for logging
     * @see #setDebugLog(OutputStream)
     */

    public void setDebugLog(String fileName) throws IOException {
        setDebugLog(getStream(fileName));
    }

    /**
     * Prints debug info to the current debugLog
     *
     * @param message The message to log
     * @see #logDebug(String, Exception)
     */

    public void logDebug(String message) {
        logDebug(message, null);
    }
    
    /**
     * Prints debug info to the current debugLog
     *
     * @param exception An Exception
     * @see #logDebug(String, Exception)
     */

    public void logDebug(Exception exception) {
        logDebug(null, exception);
    }


    /**
     * Prints debug info to the current debugLog
     *
     * @param message The message to log
     * @param exception An Exception
     */

    public void logDebug(String message, Exception exception) {
        if (!(logDebug || globalLog.logDebug))
            return;

        if (debugLog != null)
            log(debugLog, "DEBUG", owner, message, exception);
        else 
            log(globalLog.debugLog, "DEBUG", owner, message, exception);
    }
    
    // WARNING

    /**
     * Checks if we log warning info
     *
     * @return True if logging
     */

    public boolean getLogWarning() {
        return logWarning;
    }

    /**
     * Sets wheter we are to log warning info
     *
     * @param logWarning Boolean, true if we want to log warning info
     */

    public void setLogWarning(boolean logWarning) {
        this.logWarning = logWarning;
    }

    /**
     * Checks if we globally log warning info
     *
     * @return True if global logging
     */
    /*
      public static boolean getGlobalWarning() {
      return globalWarning;
      }
    */
    /**
     * Sets wheter we are to globally log warning info
     *
     * @param logWarning Boolean, true if we want to globally log warning info
     */
    /*
      public static void setGlobalWarning(boolean globalWarning) {
      Log.globalWarning = globalWarning;
      }
    */
    /**
     * Sets the OutputStream we want to print to
     *
     * @param os The OutputStream we will use for logging
     */

    public void setWarningLog(OutputStream os) {
        warningLog = new PrintStream(os, true);
    }
    
    /**
     * Sets the filename of the File we want to print to. Equivalent to 
     * setWarningLog(new FileOutputStream(fileName, true))
     *
     * @param file The File we will use for logging
     * @see #setWarningLog(OutputStream)
     */

    public void setWarningLog(String fileName) throws IOException {
        setWarningLog(getStream(fileName));
    }

    /**
     * Prints warning info to the current warningLog
     *
     * @param message The message to log
     * @see #logWarning(String, Exception)
     */

    public void logWarning(String message) {
        logWarning(message, null);
    }
    
    /**
     * Prints warning info to the current warningLog
     *
     * @param exception An Exception
     * @see #logWarning(String, Exception)
     */

    public void logWarning(Exception exception) {
        logWarning(null, exception);
    }


    /**
     * Prints warning info to the current warningLog
     *
     * @param message The message to log
     * @param exception An Exception
     */

    public void logWarning(String message, Exception exception) {
        if (!(logWarning || globalLog.logWarning))
            return;

        if (warningLog != null)
            log(warningLog, "WARNING", owner, message, exception);
        else 
            log(globalLog.warningLog, "WARNING", owner, message, exception);
    }

    // ERROR

    /**
     * Checks if we log error info
     *
     * @return True if logging
     */

    public boolean getLogError() {
        return logError;
    }

    /**
     * Sets wheter we are to log error info
     *
     * @param logError Boolean, true if we want to log error info
     */

    public void setLogError(boolean logError) {
        this.logError = logError;
    }

    /**
     * Checks if we globally log error info
     *
     * @return True if global logging
     */
    /*
      public static boolean getGlobalError() {
      return globalError;
      }
    */
    /**
     * Sets wheter we are to globally log error info
     *
     * @param logError Boolean, true if we want to globally log error info
     */
    /*
      public static void setGlobalError(boolean globalError) {
      Log.globalError = globalError;
      }
    */
    /**
     * Sets the OutputStream we want to print to
     *
     * @param os The OutputStream we will use for logging
     */

    public void setErrorLog(OutputStream os) {
        errorLog = new PrintStream(os, true);
    }

    /**
     * Sets the filename of the File we want to print to. Equivalent to 
     * setErrorLog(new FileOutputStream(fileName, true))
     *
     * @param file The File we will use for logging
     * @see #setErrorLog(OutputStream)
     */

    public void setErrorLog(String fileName) throws IOException {
        setErrorLog(getStream(fileName));
    }

    /**
     * Prints error info to the current errorLog
     *
     * @param message The message to log
     * @see #logError(String, Exception)
     */

    public void logError(String message) {
        logError(message, null);
    }

    /**
     * Prints error info to the current errorLog
     *
     * @param exception An Exception
     * @see #logError(String, Exception)
     */

    public void logError(Exception exception) {
        logError(null, exception);
    }
    
    /**
     * Prints error info to the current errorLog
     *
     * @param message The message to log
     * @param exception An Exception 
     */

    public void logError(String message, Exception exception) {
        if (!(logError || globalLog.logError))
            return;
	
        if (errorLog != null)
            log(errorLog, "ERROR", owner, message, exception);
        else 
            log(globalLog.errorLog, "ERROR", owner, message, exception);
    }

    // INFO

    /**
     * Checks if we log info info
     *
     * @return True if logging
     */

    public boolean getLogInfo() {
        return logInfo;
    }

    /**
     * Sets wheter we are to log info info
     *
     * @param logInfo Boolean, true if we want to log info info
     */

    public void setLogInfo(boolean logInfo) {
        this.logInfo = logInfo;
    }

    /**
     * Checks if we globally log info info
     *
     * @return True if global logging
     */
    /*
      public static boolean getGlobalInfo() {
      return globalInfo;
      }
    */
    /**
     * Sets wheter we are to globally log info info
     *
     * @param logInfo Boolean, true if we want to globally log info info
     */
    /*
      public static void setGlobalInfo(boolean globalInfo) {
      Log.globalInfo = globalInfo;
      }
    */
    /**
     * Sets the OutputStream we want to print to
     *
     * @param os The OutputStream we will use for logging
     */

    public void setInfoLog(OutputStream os) {
        infoLog = new PrintStream(os, true);
    }

    /**
     * Sets the filename of the File we want to print to. Equivalent to 
     * setInfoLog(new FileOutputStream(fileName, true))
     *
     * @param file The File we will use for logging
     * @see #setInfoLog(OutputStream)
     */

    public void setInfoLog(String fileName) throws IOException {
        setInfoLog(getStream(fileName));
    }

    /**
     * Prints info info to the current infoLog
     *
     * @param message The message to log
     * @see #logInfo(String, Exception)
     */

    public void logInfo(String message) {
        logInfo(message, null);
    }

    /**
     * Prints info info to the current infoLog
     *
     * @param exception An Exception
     * @see #logInfo(String, Exception)
     */

    public void logInfo(Exception exception) {
        logInfo(null, exception);
    }
    
    /**
     * Prints info info to the current infoLog
     *
     * @param message The message to log
     * @param exception An Exception 
     */

    public void logInfo(String message, Exception exception) {
        if (!(logInfo || globalLog.logInfo))
            return;
	
        if (infoLog != null)
            log(infoLog, "INFO", owner, message, exception);
        else
            log(globalLog.infoLog, "INFO", owner, message, exception);
    }

    // LOG

    /**
     * Internal method to get a named stream
     */

    private static OutputStream getStream(String name) throws IOException {
        OutputStream os = null;

        synchronized (streamCache) {
            if ((os = (OutputStream) streamCache.get(name)) != null)
                return os;
	    
            os = new FileOutputStream(name, true);
            streamCache.put(name, os);
        }

        return os;
    }

    /**
     * Internal log method
     */

    private static void log(PrintStream ps, String header, 
                            String owner, String message, Exception ex) {
        // Only allow one instance to print to the given stream.
        synchronized (ps) {
            // Create output stream for logging
            LogStream logStream = new LogStream(ps);
	    
            logStream.time = new Date(System.currentTimeMillis());
            logStream.header = header;
            logStream.owner = owner;
	    
            if (message != null)
                logStream.println(message);
	    
            if (ex != null) {
                logStream.println(ex.getMessage());
                ex.printStackTrace(logStream);
            }
        }
    }
}

/**
 * Utility class for logging.
 *
 * Minimal overloading of PrintStream
 */

class LogStream extends PrintStream {
    Date time = null;
    String header = null;
    String owner = null;
    
    public LogStream(OutputStream ps) {
        super(ps);
    }
    
    public void println(Object o) {
        if (o == null)
            println("null");
        else 
            println(o.toString());
    }
    
    public void println(String str) {
        super.println("*** " + header + " (" + time + ", " + time.getTime() 
                      + ") " + owner + ": " + str);
    } 
}
