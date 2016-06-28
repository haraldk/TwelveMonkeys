package com.twelvemonkeys.net;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.util.CollectionUtil;

import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class with network related methods.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/net/NetUtil.java#2 $
 */
public final class NetUtil {

    private final static String VERSION_ID = "NetUtil/2.1";
    
    private static Authenticator sAuthenticator = null;

    private final static int BUF_SIZE = 8192;
    private final static String HTTP = "http://";
    private final static String HTTPS = "https://";

    /**
     * Field HTTP_PROTOCOL
     */
    public final static String HTTP_PROTOCOL = "http";

    /**
     * Field HTTPS_PROTOCOL
     */
    public final static String HTTPS_PROTOCOL = "https";

    /**
     * Field HTTP_GET
     */
    public final static String HTTP_GET = "GET";

    /**
     * Field HTTP_POST
     */
    public final static String HTTP_POST = "POST";

    /**
     * Field HTTP_HEAD
     */
    public final static String HTTP_HEAD = "HEAD";

    /**
     * Field HTTP_OPTIONS
     */
    public final static String HTTP_OPTIONS = "OPTIONS";

    /**
     * Field HTTP_PUT
     */
    public final static String HTTP_PUT = "PUT";

    /**
     * Field HTTP_DELETE
     */
    public final static String HTTP_DELETE = "DELETE";

    /**
     * Field HTTP_TRACE
     */
    public final static String HTTP_TRACE = "TRACE";

    /**
     * Creates a NetUtil.
     * This class has only static methods and members, and should not be
     * instantiated.
     */
    private NetUtil() {
    }

    /**
     * Main method, reads data from a URL and, optionally, writes it to stdout or a file.
     * @param pArgs command line arguemnts
     * @throws java.io.IOException if an I/O exception occurs
     */
    public static void main(String[] pArgs) throws IOException {
        // params:
        int timeout = 0;
        boolean followRedirects = true;
        boolean debugHeaders = false;
        String requestPropertiesFile = null;
        String requestHeaders = null;
        String postData = null;
        File putData = null;
        int argIdx = 0;
        boolean errArgs = false;
        boolean writeToFile = false;
        boolean writeToStdOut = false;
        String outFileName = null;

        while ((argIdx < pArgs.length) && (pArgs[argIdx].charAt(0) == '-') && (pArgs[argIdx].length() >= 2)) {
            if ((pArgs[argIdx].charAt(1) == 't') || pArgs[argIdx].equals("--timeout")) {
                argIdx++;
                try {
                    timeout = Integer.parseInt(pArgs[argIdx++]);
                }
                catch (NumberFormatException nfe) {
                    errArgs = true;
                    break;
                }
            }
            else if ((pArgs[argIdx].charAt(1) == 'd') || pArgs[argIdx].equals("--debugheaders")) {
                debugHeaders = true;
                argIdx++;
            }
            else if ((pArgs[argIdx].charAt(1) == 'n') || pArgs[argIdx].equals("--nofollowredirects")) {
                followRedirects = false;
                argIdx++;
            }
            else if ((pArgs[argIdx].charAt(1) == 'r') || pArgs[argIdx].equals("--requestproperties")) {
                argIdx++;
                requestPropertiesFile = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'p') || pArgs[argIdx].equals("--postdata")) {
                argIdx++;
                postData = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'u') || pArgs[argIdx].equals("--putdata")) {
                argIdx++;
                putData = new File(pArgs[argIdx++]);
                if (!putData.exists()) {
                    errArgs = true;
                    break;
                }
            }
            else if ((pArgs[argIdx].charAt(1) == 'h') || pArgs[argIdx].equals("--header")) {
                argIdx++;
                requestHeaders = pArgs[argIdx++];
            }
            else if ((pArgs[argIdx].charAt(1) == 'f') || pArgs[argIdx].equals("--file")) {
                argIdx++;
                writeToFile = true;

                // Get optional file name
                if (!((argIdx >= (pArgs.length - 1)) || (pArgs[argIdx].charAt(0) == '-'))) {
                    outFileName = pArgs[argIdx++];
                }
            }
            else if ((pArgs[argIdx].charAt(1) == 'o') || pArgs[argIdx].equals("--output")) {
                argIdx++;
                writeToStdOut = true;
            }
            else {
                System.err.println("Unknown option \"" + pArgs[argIdx++] + "\"");
            }
        }
        if (errArgs || (pArgs.length < (argIdx + 1))) {
            System.err.println("Usage: java  NetUtil [-f|--file [<file name>]] [-d|--debugheaders] [-h|--header <header data>] [-p|--postdata <URL-encoded postdata>] [-u|--putdata <file name>] [-r|--requestProperties <properties file>] [-t|--timeout <miliseconds>] [-n|--nofollowredirects] fromUrl");
            System.exit(5);
        }
        String url = pArgs[argIdx/*++*/];

        // DONE ARGS
        // Get request properties
        Properties requestProperties = new Properties();

        if (requestPropertiesFile != null) {

            // Just read, no exception handling...
            requestProperties.load(new FileInputStream(new File(requestPropertiesFile)));
        }
        if (requestHeaders != null) {

            // Get request headers
            String[] headerPairs = StringUtil.toStringArray(requestHeaders, ",");

            for (String headerPair : headerPairs) {
                String[] pair = StringUtil.toStringArray(headerPair, ":");
                String key = (pair.length > 0)
                        ? pair[0].trim()
                        : null;
                String value = (pair.length > 1)
                        ? pair[1].trim()
                        : "";

                if (key != null) {
                    requestProperties.setProperty(key, value);
                }
            }
        }
        HttpURLConnection conn;

        // Create connection
        URL reqURL = getURLAndSetAuthorization(url, requestProperties);

        conn = createHttpURLConnection(reqURL, requestProperties, followRedirects, timeout);

        // POST
        if (postData != null) {
            // HTTP POST method
            conn.setRequestMethod(HTTP_POST);

            // Set entity headers
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postData.length()));
            conn.setRequestProperty("Content-Encoding", "ISO-8859-1");

            // Get outputstream (this is where the connect actually happens)
            OutputStream os = conn.getOutputStream();

            System.err.println("OutputStream: " + os.getClass().getName() + "@" + System.identityHashCode(os));
            OutputStreamWriter writer = new OutputStreamWriter(os, "ISO-8859-1");

            // Write post data to the stream
            writer.write(postData);
            writer.write("\r\n");

            //writer.flush();
            writer.close();  // Does this close the underlying stream?
        }
        // PUT
        else if (putData != null) {
            // HTTP PUT method
            conn.setRequestMethod(HTTP_PUT);

            // Set entity headers
            //conn.setRequestProperty("Content-Type", "???");
            // TODO: Set Content-Type to correct type?
            // TODO: Set content-encoding? Or can binary data be sent directly?
            conn.setRequestProperty("Content-Length", String.valueOf(putData.length()));

            // Get outputstream (this is where the connect actually happens)
            OutputStream os = conn.getOutputStream();

            System.err.println("OutputStream: " + os.getClass().getName() + "@" + System.identityHashCode(os));

            // Write put data to the stream
            FileUtil.copy(new FileInputStream(putData), os);

            os.close();
        }

        //
        InputStream is;

        if (conn.getResponseCode() == 200) {

            // Connect and get stream
            is = conn.getInputStream();
        }
        else {
            is = conn.getErrorStream();
        }

        //
        if (debugHeaders) {
            System.err.println("Request (debug):");
            System.err.println(conn.getClass());
            System.err.println("Response (debug):");

            // Headerfield 0 is response code
            System.err.println(conn.getHeaderField(0));

            // Loop from 1, as headerFieldKey(0) == null...
            for (int i = 1; ; i++) {
                String key = conn.getHeaderFieldKey(i);

                // Seems to be the way to loop through them all...
                if (key == null) {
                    break;
                }
                System.err.println(key + ": " + conn.getHeaderField(key));
            }
        }

        // Create output file if specified
        OutputStream os;

        if (writeToFile) {
            if (outFileName == null) {
                outFileName = reqURL.getFile();
                if (StringUtil.isEmpty(outFileName)) {
                    outFileName = conn.getHeaderField("Location");
                    if (StringUtil.isEmpty(outFileName)) {
                        outFileName = "index";

                        // Find a suitable extension
                        // TODO: Replace with MIME-type util with MIME/file ext mapping
                        String ext = conn.getContentType();

                        if (!StringUtil.isEmpty(ext)) {
                            int idx = ext.lastIndexOf('/');

                            if (idx >= 0) {
                                ext = ext.substring(idx + 1);
                            }
                            idx = ext.indexOf(';');
                            if (idx >= 0) {
                                ext = ext.substring(0, idx);
                            }
                            outFileName += "." + ext;
                        }
                    }
                }
                int idx = outFileName.lastIndexOf('/');

                if (idx >= 0) {
                    outFileName = outFileName.substring(idx + 1);
                }
                idx = outFileName.indexOf('?');
                if (idx >= 0) {
                    outFileName = outFileName.substring(0, idx);
                }
            }
            File outFile = new File(outFileName);

            if (!outFile.createNewFile()) {
                if (outFile.exists()) {
                    System.err.println("Cannot write to file " + outFile.getAbsolutePath() + ", file allready exists.");
                }
                else {
                    System.err.println("Cannot write to file " + outFile.getAbsolutePath() + ", check write permissions.");
                }
                System.exit(5);
            }
            os = new FileOutputStream(outFile);
        }
        else if (writeToStdOut) {
            os = System.out;
        }
        else {
            os = null;
        }

        // Get data.
        if ((writeToFile || writeToStdOut) && is != null) {
            FileUtil.copy(is, os);
        }

        /*
                    Hashtable postData = new Hashtable();
                    postData.put("SearchText", "condition");

                    try {
                        InputStream in = getInputStreamHttpPost(pArgs[argIdx], postData,
                                                             props, true, 0);
                        out = new FileOutputStream(file);
                        FileUtil.copy(in, out);
                    }
                    catch (Exception e) {
                        System.err.println("Error: " + e);
                        e.printStackTrace(System.err);
                        continue;
                    }
         */
    }

    /*
      public static class Cookie {
      String mName = null;
      String mValue = null;

      public Cookie(String pName, String pValue) {
      mName = pName;
      mValue = pValue;
      }

      public String toString() {
      return mName + "=" + mValue;
      }
    */

    /*
    // Just a way to set cookies..
    if (pCookies != null) {
  String cookieStr = "";
  for (int i = 0; i < pCookies.length; i++)
  cookieStr += ((i == pCookies.length) ? pCookies[i].toString()
  : pCookies[i].toString() + ";");

  // System.out.println("Cookie: " + cookieStr);

  conn.setRequestProperty("Cookie", cookieStr);
    }
    */

    /*
  }
    */

    /**
     * Test if the given URL is using HTTP protocol.
     *
     * @param pURL the url to condition
     * @return true if the protocol is HTTP.
     */
    public static boolean isHttpURL(String pURL) {
        return ((pURL != null) && pURL.startsWith(HTTP));
    }

    /**
     * Test if the given URL is using HTTP protocol.
     *
     * @param pURL the url to condition
     * @return true if the protocol is HTTP.
     */
    public static boolean isHttpURL(URL pURL) {
        return ((pURL != null) && pURL.getProtocol().equals("http"));
    }

    /**
     * Gets the content from a given URL, and returns it as a byte array.
     * Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>NOTE: If you supply a username and password for HTTP
     * authentication, this method uses the java.net.Authenticator's static
     * {@code setDefault()} method, that can only be set ONCE. This
     * means that if the default Authenticator is allready set, this method
     * will fail.
     * It also means if any other piece of code tries to register a new default
     * Authenticator within the current VM, it will fail.</SMALL>
     *
     * @param pURL A String containing the URL, on the form
     *             <CODE>[http://][<username>:<password>@]servername[/file.ext]</CODE>
     *             where everything in brackets are optional.
     * @return a byte array with the URL contents. If an error occurs, the
     *         returned array may be zero-length, but not null.
     * @throws MalformedURLException if the urlName parameter is not a valid
     *                               URL. Note that the protocol cannot be anything but HTTP.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see java.net.Authenticator
     * @see SimpleAuthenticator
     */
    public static byte[] getBytesHttp(String pURL) throws IOException {
        return getBytesHttp(pURL, 0);
    }

    /**
     * Gets the content from a given URL, and returns it as a byte array.
     *
     * @param pURL the URL to get.
     * @return a byte array with the URL contents. If an error occurs, the
     *         returned array may be zero-length, but not null.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see #getBytesHttp(String)
     */
    public static byte[] getBytesHttp(URL pURL) throws IOException {
        return getBytesHttp(pURL, 0);
    }

    /**
     * Gets the InputStream from a given URL. Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>NOTE: If you supply a username and password for HTTP
     * authentication, this method uses the java.net.Authenticator's static
     * {@code setDefault()} method, that can only be set ONCE. This
     * means that if the default Authenticator is allready set, this method
     * will fail.
     * It also means if any other piece of code tries to register a new default
     * Authenticator within the current VM, it will fail.</SMALL>
     *
     * @param pURL A String containing the URL, on the form
     *             <CODE>[http://][<username>:<password>@]servername[/file.ext]</CODE>
     *             where everything in brackets are optional.
     * @return an input stream that reads from the connection created by the
     *         given URL.
     * @throws MalformedURLException if the urlName parameter specifies an
     *                               unknown protocol, or does not form a valid URL.
     *                               Note that the protocol cannot be anything but HTTP.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see java.net.Authenticator
     * @see SimpleAuthenticator
     */
    public static InputStream getInputStreamHttp(String pURL) throws IOException {
        return getInputStreamHttp(pURL, 0);
    }

    /**
     * Gets the InputStream from a given URL.
     *
     * @param pURL the URL to get.
     * @return an input stream that reads from the connection created by the
     *         given URL.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see #getInputStreamHttp(String)
     */
    public static InputStream getInputStreamHttp(URL pURL) throws IOException {
        return getInputStreamHttp(pURL, 0);
    }

    /**
     * Gets the InputStream from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout. Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised. This
     * might happen BEFORE OR AFTER this method returns, as the HTTP headers
     * will be read and parsed from the InputStream before this method returns,
     * while further read operations on the returned InputStream might be
     * performed at a later stage.
     * <BR/>
     * </SMALL>
     *
     * @param pURL     the URL to get.
     * @param pTimeout the specified timeout, in milliseconds.
     * @return an input stream that reads from the socket connection, created
     *         from the given URL.
     * @throws MalformedURLException if the url parameter specifies an
     *                               unknown protocol, or does not form a valid URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see #getInputStreamHttp(URL,int)
     * @see java.net.Socket
     * @see java.net.Socket#setSoTimeout(int) setSoTimeout
     * @see java.io.InterruptedIOException
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    public static InputStream getInputStreamHttp(String pURL, int pTimeout) throws IOException {
        return getInputStreamHttp(pURL, null, true, pTimeout);
    }

    /**
     * Gets the InputStream from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout. Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised. This
     * might happen BEFORE OR AFTER this method returns, as the HTTP headers
     * will be read and parsed from the InputStream before this method returns,
     * while further read operations on the returned InputStream might be
     * performed at a later stage.
     * <BR/>
     * </SMALL>
     *
     * @param pURL             the URL to get.
     * @param pProperties      the request header properties.
     * @param pFollowRedirects specifying wether redirects should be followed.
     * @param pTimeout         the specified timeout, in milliseconds.
     * @return an input stream that reads from the socket connection, created
     *         from the given URL.
     * @throws MalformedURLException if the url parameter specifies an
     *                               unknown protocol, or does not form a valid URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see #getInputStreamHttp(URL,int)
     * @see java.net.Socket
     * @see java.net.Socket#setSoTimeout(int) setSoTimeout
     * @see java.io.InterruptedIOException
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    public static InputStream getInputStreamHttp(final String pURL, final Properties pProperties, final boolean pFollowRedirects, final int pTimeout)
            throws IOException {

        // Make sure we have properties
        Properties properties = pProperties != null ? pProperties : new Properties();

        //URL url = getURLAndRegisterPassword(pURL);
        URL url = getURLAndSetAuthorization(pURL, properties);

        //unregisterPassword(url);
        return getInputStreamHttp(url, properties, pFollowRedirects, pTimeout);
    }

    /**
     * Registers the password from the URL string, and returns the URL object.
     *
     * @param pURL the string representation of the URL, possibly including authorization part
     * @param pProperties the
     * @return the URL created from {@code pURL}.
     * @throws java.net.MalformedURLException if there's a syntax error in {@code pURL}
     */
    private static URL getURLAndSetAuthorization(final String pURL, final Properties pProperties) throws MalformedURLException {
        String url = pURL;
        // Split user/password away from url
        String userPass = null;
        String protocolPrefix = HTTP;
        int httpIdx = url.indexOf(HTTPS);

        if (httpIdx >= 0) {
            protocolPrefix = HTTPS;
            url = url.substring(httpIdx + HTTPS.length());
        }
        else {
            httpIdx = url.indexOf(HTTP);
            if (httpIdx >= 0) {
                url = url.substring(httpIdx + HTTP.length());
            }
        }

        // Get authorization part
        int atIdx = url.indexOf("@");

        if (atIdx >= 0) {
            userPass = url.substring(0, atIdx);
            url = url.substring(atIdx + 1);
        }

        // Set authorization if user/password is present
        if (userPass != null) {
            //      System.out.println("Setting password ("+ userPass + ")!");
            pProperties.setProperty("Authorization", "Basic " + BASE64.encode(userPass.getBytes()));
        }

        // Return URL
        return new URL(protocolPrefix + url);
    }

    /**
     * Gets the InputStream from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised. This
     * might happen BEFORE OR AFTER this method returns, as the HTTP headers
     * will be read and parsed from the InputStream before this method returns,
     * while further read operations on the returned InputStream might be
     * performed at a later stage.
     * <BR/>
     * </SMALL>
     *
     * @param pURL     the URL to get.
     * @param pTimeout the specified timeout, in milliseconds.
     * @return an input stream that reads from the socket connection, created
     *         from the given URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see com.twelvemonkeys.net.HttpURLConnection
     * @see java.net.Socket
     * @see java.net.Socket#setSoTimeout(int) setSoTimeout
     * @see HttpURLConnection
     * @see java.io.InterruptedIOException
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    public static InputStream getInputStreamHttp(URL pURL, int pTimeout) throws IOException {
        return getInputStreamHttp(pURL, null, true, pTimeout);
    }

    /**
     * Gets the InputStream from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout. Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised. This
     * might happen BEFORE OR AFTER this method returns, as the HTTP headers
     * will be read and parsed from the InputStream before this method returns,
     * while further read operations on the returned InputStream might be
     * performed at a later stage.
     * <BR/>
     * </SMALL>
     *
     * @param pURL             the URL to get.
     * @param pProperties      the request header properties.
     * @param pFollowRedirects specifying wether redirects should be followed.
     * @param pTimeout         the specified timeout, in milliseconds.
     * @return an input stream that reads from the socket connection, created
     *         from the given URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see #getInputStreamHttp(URL,int)
     * @see java.net.Socket
     * @see java.net.Socket#setSoTimeout(int) setSoTimeout
     * @see java.io.InterruptedIOException
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    public static InputStream getInputStreamHttp(URL pURL, Properties pProperties, boolean pFollowRedirects, int pTimeout)
            throws IOException {

        // Open the connection, and get the stream
        HttpURLConnection conn = createHttpURLConnection(pURL, pProperties, pFollowRedirects, pTimeout);

        // HTTP GET method
        conn.setRequestMethod(HTTP_GET);

        // This is where the connect happens
        InputStream is = conn.getInputStream();

        // We only accept the 200 OK message
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("The request gave the response: " + conn.getResponseCode() + ": " + conn.getResponseMessage());
        }
        return is;
    }

    /**
     * Gets the InputStream from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout. Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised. This
     * might happen BEFORE OR AFTER this method returns, as the HTTP headers
     * will be read and parsed from the InputStream before this method returns,
     * while further read operations on the returned InputStream might be
     * performed at a later stage.
     * <BR/>
     * </SMALL>
     *
     * @param pURL             the URL to get.
     * @param pPostData        the post data.
     * @param pProperties      the request header properties.
     * @param pFollowRedirects specifying wether redirects should be followed.
     * @param pTimeout         the specified timeout, in milliseconds.
     * @return an input stream that reads from the socket connection, created
     *         from the given URL.
     * @throws MalformedURLException if the url parameter specifies an
     *                               unknown protocol, or does not form a valid URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     */
    public static InputStream getInputStreamHttpPost(String pURL, Map pPostData, Properties pProperties, boolean pFollowRedirects, int pTimeout)
            throws IOException {

        pProperties = pProperties != null ? pProperties : new Properties();

        //URL url = getURLAndRegisterPassword(pURL);
        URL url = getURLAndSetAuthorization(pURL, pProperties);

        //unregisterPassword(url);
        return getInputStreamHttpPost(url, pPostData, pProperties, pFollowRedirects, pTimeout);
    }

    /**
     * Gets the InputStream from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout. Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised. This
     * might happen BEFORE OR AFTER this method returns, as the HTTP headers
     * will be read and parsed from the InputStream before this method returns,
     * while further read operations on the returned InputStream might be
     * performed at a later stage.
     * <BR/>
     * </SMALL>
     *
     * @param pURL             the URL to get.
     * @param pPostData        the post data.
     * @param pProperties      the request header properties.
     * @param pFollowRedirects specifying wether redirects should be followed.
     * @param pTimeout         the specified timeout, in milliseconds.
     * @return an input stream that reads from the socket connection, created
     *         from the given URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     */
    public static InputStream getInputStreamHttpPost(URL pURL, Map pPostData, Properties pProperties, boolean pFollowRedirects, int pTimeout)
            throws IOException {
        // Open the connection, and get the stream
        HttpURLConnection conn = createHttpURLConnection(pURL, pProperties, pFollowRedirects, pTimeout);

        // HTTP POST method
        conn.setRequestMethod(HTTP_POST);

        // Iterate over and create post data string
        StringBuilder postStr = new StringBuilder();

        if (pPostData != null) {
            Iterator data = pPostData.entrySet().iterator();

            while (data.hasNext()) {
                Map.Entry entry = (Map.Entry) data.next();

                // Properties key/values can be safely cast to strings
                // Encode the string
                postStr.append(URLEncoder.encode((String) entry.getKey(), "UTF-8"));
                postStr.append('=');
                postStr.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));

                if (data.hasNext()) {
                    postStr.append('&');
                }
            }
        }

        // Set entity headers
        String encoding = conn.getRequestProperty("Content-Encoding");
        if (StringUtil.isEmpty(encoding)) {
            encoding = "UTF-8";
        }
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postStr.length()));
        conn.setRequestProperty("Content-Encoding", encoding);

        // Get outputstream (this is where the connect actually happens)
        OutputStream os = conn.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(os, encoding);

        // Write post data to the stream
        writer.write(postStr.toString());
        writer.write("\r\n");
        writer.close();  // Does this close the underlying stream?

        // Get the inputstream
        InputStream is = conn.getInputStream();

        // We only accept the 200 OK message
        // TODO: Accept all 200 messages, like ACCEPTED, CREATED or NO_CONTENT?
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("The request gave the response: " + conn.getResponseCode() + ": " + conn.getResponseMessage());
        }
        return is;
    }

    /**
     * Creates a HTTP connection to the given URL.
     *
     * @param pURL             the URL to get.
     * @param pProperties      connection properties.
     * @param pFollowRedirects specifies whether we should follow redirects.
     * @param pTimeout         the specified timeout, in milliseconds.
     * @return a HttpURLConnection
     * @throws UnknownHostException if the hostname in the URL cannot be found.
     * @throws IOException          if an I/O exception occurs.
     */
    public static HttpURLConnection createHttpURLConnection(URL pURL, Properties pProperties, boolean pFollowRedirects, int pTimeout)
            throws IOException {

        // Open the connection, and get the stream
        HttpURLConnection conn;

        if (pTimeout > 0) {
            // Supports timeout
            conn = new com.twelvemonkeys.net.HttpURLConnection(pURL, pTimeout);
        }
        else {
            // Faster, more compatible
            conn = (HttpURLConnection) pURL.openConnection();
        }

        // Set user agent
        if ((pProperties == null) || !pProperties.containsKey("User-Agent")) {
            conn.setRequestProperty("User-Agent",
                    VERSION_ID
                    + " (" + System.getProperty("os.name") + "/" + System.getProperty("os.version") + "; "
                    + System.getProperty("os.arch") + "; "
                    + System.getProperty("java.vm.name") + "/" + System.getProperty("java.vm.version") + ")");
        }

        // Set request properties
        if (pProperties != null) {
            for (Map.Entry<Object, Object> entry : pProperties.entrySet()) {
                // Properties key/values can be safely cast to strings
                conn.setRequestProperty((String) entry.getKey(), entry.getValue().toString());
            }
        }

        try {
            // Breaks with JRE1.2?
            conn.setInstanceFollowRedirects(pFollowRedirects);
        }
        catch (LinkageError le) {
            // This is the best we can do...
            HttpURLConnection.setFollowRedirects(pFollowRedirects);
            System.err.println("You are using an old Java Spec, consider upgrading.");
            System.err.println("java.net.HttpURLConnection.setInstanceFollowRedirects(" + pFollowRedirects + ") failed.");

            //le.printStackTrace(System.err);
        }

        conn.setDoInput(true);
        conn.setDoOutput(true);

        //conn.setUseCaches(true);
        return conn;
    }

    /**
     * This is a hack to get around the protected constructors in
     * HttpURLConnection, should maybe consider registering and do things
     * properly...
     */

    /*
    private static class TimedHttpURLConnection
        extends com.twelvemonkeys.net.HttpURLConnection {
        TimedHttpURLConnection(URL pURL, int pTimeout) {
            super(pURL, pTimeout);
        }
    }
    */

    /**
     * Gets the content from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout. Supports basic HTTP
     * authentication, using a URL string similar to most browsers.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised.
     * <BR/>
     * </SMALL>
     *
     * @param pURL     the URL to get.
     * @param pTimeout the specified timeout, in milliseconds.
     * @return a byte array that is read from the socket connection, created
     *         from the given URL.
     * @throws MalformedURLException if the url parameter specifies an
     *                               unknown protocol, or does not form a valid URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see #getBytesHttp(URL,int)
     * @see java.net.Socket
     * @see java.net.Socket#setSoTimeout(int) setSoTimeout
     * @see java.io.InterruptedIOException
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    public static byte[] getBytesHttp(String pURL, int pTimeout) throws IOException {
        // Get the input stream from the url
        InputStream in = new BufferedInputStream(getInputStreamHttp(pURL, pTimeout), BUF_SIZE * 2);

        // Get all the bytes in loop
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int count;
        byte[] buffer = new byte[BUF_SIZE];

        try {
            while ((count = in.read(buffer)) != -1) {
                // NOTE: According to the J2SE API doc, read(byte[]) will read
                // at least 1 byte, or return -1, if end-of-file is reached.
                bytes.write(buffer, 0, count);
            }
        }
        finally {

            // Close the buffer
            in.close();
        }
        return bytes.toByteArray();
    }

    /**
     * Gets the content from a given URL, with the given timeout.
     * The timeout must be > 0. A timeout of zero is interpreted as an
     * infinite timeout.
     * <P/>
     * <SMALL>Implementation note: If the timeout parameter is greater than 0,
     * this method uses my own implementation of
     * java.net.HttpURLConnection, that uses plain sockets, to create an
     * HTTP connection to the given URL. The {@code read} methods called
     * on the returned InputStream, will block only for the specified timeout.
     * If the timeout expires, a java.io.InterruptedIOException is raised.
     * <BR/>
     * </SMALL>
     *
     * @param pURL     the URL to get.
     * @param pTimeout the specified timeout, in milliseconds.
     * @return an input stream that reads from the socket connection, created
     *         from the given URL.
     * @throws UnknownHostException  if the IP address for the given URL cannot
     *                               be resolved.
     * @throws FileNotFoundException if there is no file at the given URL.
     * @throws IOException           if an error occurs during transfer.
     * @see #getInputStreamHttp(URL,int)
     * @see com.twelvemonkeys.net.HttpURLConnection
     * @see java.net.Socket
     * @see java.net.Socket#setSoTimeout(int) setSoTimeout
     * @see HttpURLConnection
     * @see java.io.InterruptedIOException
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    public static byte[] getBytesHttp(URL pURL, int pTimeout) throws IOException {
        // Get the input stream from the url
        InputStream in = new BufferedInputStream(getInputStreamHttp(pURL, pTimeout), BUF_SIZE * 2);

        // Get all the bytes in loop
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int count;
        byte[] buffer = new byte[BUF_SIZE];

        try {
            while ((count = in.read(buffer)) != -1) {
                // NOTE: According to the J2SE API doc, read(byte[]) will read
                // at least 1 byte, or return -1, if end-of-file is reached.
                bytes.write(buffer, 0, count);
            }
        }
        finally {

            // Close the buffer
            in.close();
        }
        return bytes.toByteArray();
    }

    /**
     * Unregisters the password asscociated with this URL
     */

    /*
    private static void unregisterPassword(URL pURL) {
        Authenticator auth = registerAuthenticator();
        if (auth != null && auth instanceof SimpleAuthenticator)
            ((SimpleAuthenticator) auth)
                .unregisterPasswordAuthentication(pURL);
    }
    */

    /**
     * Registers the password from the URL string, and returns the URL object.
     */

    /*
    private static URL getURLAndRegisterPassword(String pURL)
        throws MalformedURLException
    {
        // Split user/password away from url
        String userPass = null;
        String protocolPrefix = HTTP;

        int httpIdx = pURL.indexOf(HTTPS);
        if (httpIdx >= 0) {
            protocolPrefix = HTTPS;
            pURL = pURL.substring(httpIdx + HTTPS.length());
        }
        else {
            httpIdx = pURL.indexOf(HTTP);
            if (httpIdx >= 0)
                pURL = pURL.substring(httpIdx + HTTP.length());
        }

        int atIdx = pURL.indexOf("@");
        if (atIdx >= 0) {
            userPass = pURL.substring(0, atIdx);
            pURL = pURL.substring(atIdx + 1);
        }

        // Set URL
        URL url = new URL(protocolPrefix + pURL);

        // Set Authenticator if user/password is present
        if (userPass != null) {
            //      System.out.println("Setting password ("+ userPass + ")!");

            int colIdx = userPass.indexOf(":");
            if (colIdx < 0)
                throw new MalformedURLException("Error in username/password!");

            String userName = userPass.substring(0, colIdx);
            String passWord = userPass.substring(colIdx + 1);

            // Try to register the authenticator
            //      System.out.println("Trying to register authenticator!");
            Authenticator auth = registerAuthenticator();

            //      System.out.println("Got authenticator " + auth + ".");

            // Register our username/password with it
            if (auth != null && auth instanceof SimpleAuthenticator) {
                ((SimpleAuthenticator) auth)
                    .registerPasswordAuthentication(url,
                                                    new PasswordAuthentication(userName,
                                                                               passWord.toCharArray()));
            }
            else {
                // Not supported!
                throw new RuntimeException("Could not register PasswordAuthentication");
            }
        }

        return url;
    }
    */

    /**
     * Registers the Authenticator given in the system property
     * {@code java.net.Authenticator}, or the default implementation
     * ({@code com.twelvemonkeys.net.SimpleAuthenticator}).
     * <P/>
     * BUG: What if authenticator has allready been set outside this class?
     *
     * @return The Authenticator created and set as default, or null, if it
     *         was not set as the default. However, there is no (clean) way to
     *         be sure the authenticator was set (the SimpleAuthenticator uses
     *         a hack to get around this), so it might be possible that the
     *         returned authenticator was not set as default...
     * @see Authenticator#setDefault(Authenticator)
     * @see SimpleAuthenticator
     */
    public synchronized static Authenticator registerAuthenticator() {
        if (sAuthenticator != null) {
            return sAuthenticator;
        }

        // Get the system property
        String authenticatorName = System.getProperty("java.net.Authenticator");

        // Try to get the Authenticator from the system property
        if (authenticatorName != null) {
            try {
                Class authenticatorClass = Class.forName(authenticatorName);

                sAuthenticator = (Authenticator) authenticatorClass.newInstance();
            }
            catch (ClassNotFoundException cnfe) {
                // We should maybe rethrow this?
            }
            catch (InstantiationException ie) {
                // Ignore
            }
            catch (IllegalAccessException iae) {
                // Ignore
            }
        }

        // Get the default authenticator
        if (sAuthenticator == null) {
            sAuthenticator = SimpleAuthenticator.getInstance();
        }

        // Register authenticator as default
        Authenticator.setDefault(sAuthenticator);
        return sAuthenticator;
    }

    /**
     * Creates the InetAddress object from the given URL.
     * Equivalent to calling {@code InetAddress.getByName(URL.getHost())}
     * except that it returns null, instead of throwing UnknownHostException.
     *
     * @param pURL the URL to look up.
     * @return the createad InetAddress, or null if the host was unknown.
     * @see java.net.InetAddress
     * @see java.net.URL
     */
    public static InetAddress createInetAddressFromURL(URL pURL) {
        try {
            return InetAddress.getByName(pURL.getHost());
        }
        catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * Creates an URL from the given InetAddress object, using the given
     * protocol.
     * Equivalent to calling
     * {@code new URL(protocol, InetAddress.getHostName(), "")}
     * except that it returns null, instead of throwing MalformedURLException.
     *
     * @param pIP       the IP address to look up
     * @param pProtocol the protocol to use in the new URL
     * @return the created URL or null, if the URL could not be created.
     * @see java.net.URL
     * @see java.net.InetAddress
     */
    public static URL createURLFromInetAddress(InetAddress pIP, String pProtocol) {
        try {
            return new URL(pProtocol, pIP.getHostName(), "");
        }
        catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Creates an URL from the given InetAddress object, using HTTP protocol.
     * Equivalent to calling
     * {@code new URL("http", InetAddress.getHostName(), "")}
     * except that it returns null, instead of throwing MalformedURLException.
     *
     * @param pIP the IP address to look up
     * @return the created URL or null, if the URL could not be created.
     * @see java.net.URL
     * @see java.net.InetAddress
     */
    public static URL createURLFromInetAddress(InetAddress pIP) {
        return createURLFromInetAddress(pIP, HTTP);
    }

    /*
     * TODO: Benchmark!
     */
    static byte[] getBytesHttpOld(String pURL) throws IOException {
        // Get the input stream from the url
        InputStream in = new BufferedInputStream(getInputStreamHttp(pURL), BUF_SIZE * 2);

        // Get all the bytes in loop
        byte[] bytes = new byte[0];
        int count;
        byte[] buffer = new byte[BUF_SIZE];

        try {
            while ((count = in.read(buffer)) != -1) {

                // NOTE: According to the J2SE API doc, read(byte[]) will read
                // at least 1 byte, or return -1, if end-of-file is reached.
                bytes = (byte[]) CollectionUtil.mergeArrays(bytes, 0, bytes.length, buffer, 0, count);
            }
        }
        finally {

            // Close the buffer
            in.close();
        }
        return bytes;
    }
}