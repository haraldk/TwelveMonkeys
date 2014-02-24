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

package com.twelvemonkeys.net;

import com.twelvemonkeys.lang.StringUtil;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A URLConnection with support for HTTP-specific features. See
 * <A HREF="http://www.w3.org/pub/WWW/Protocols/">the spec</A> for details.
 * This version also supports read and connect timeouts, making it more useful
 * for clients with limitted time.
 * <P/>
 * Note that the timeouts are created on the socket level, and that
 * <P/>
 * Note: This class should now work as expected, but it needs more testing before
 * it can enter production release.
 * <BR/>
 * --.k
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/net/HttpURLConnection.java#1 $
 * @todo Write JUnit TestCase
 * @todo ConnectionMananger!
 * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
 */
public class HttpURLConnection extends java.net.HttpURLConnection {
    /**
     * HTTP Status-Code 307: Temporary Redirect
     */
    public final static int HTTP_REDIRECT = 307;
    private final static int HTTP_DEFAULT_PORT = 80;
    private final static String HTTP_HEADER_END = "\r\n\r\n";
    private static final String HEADER_WWW_AUTH = "WWW-Authenticate";
    private final static int BUF_SIZE = 8192;
    private int maxRedirects = (System.getProperty("http.maxRedirects") != null)
            ? Integer.parseInt(System.getProperty("http.maxRedirects"))
            : 20;
    protected int timeout = -1;
    protected int connectTimeout = -1;
    private Socket socket = null;
    protected InputStream errorStream = null;
    protected InputStream inputStream = null;
    protected OutputStream outputStream = null;
    private String[] responseHeaders = null;
    protected Properties responseHeaderFields = null;
    protected Properties requestProperties = new Properties();

    /**
     * Creates a HttpURLConnection.
     *
     * @param pURL the URL to connect to.
     */
    protected HttpURLConnection(URL pURL) {
        this(pURL, 0, 0);
    }

    /**
     * Creates a HttpURLConnection with a given read and connect timeout.
     * A timeout value of zero is interpreted as an
     * infinite timeout.
     *
     * @param pURL     the URL to connect to.
     * @param pTimeout the maximum time the socket will block for read
     *                 and connect operations.
     */
    protected HttpURLConnection(URL pURL, int pTimeout) {
        this(pURL, pTimeout, pTimeout);
    }

    /**
     * Creates a HttpURLConnection with a given read and connect timeout.
     * A timeout value of zero is interpreted as an
     * infinite timeout.
     *
     * @param pURL            the URL to connect to.
     * @param pTimeout        the maximum time the socket will block for read
     *                        operations.
     * @param pConnectTimeout the maximum time the socket will block for
     *                        connection.
     */
    protected HttpURLConnection(URL pURL, int pTimeout, int pConnectTimeout) {
        super(pURL);
        setTimeout(pTimeout);
        connectTimeout = pConnectTimeout;
    }

    /**
     * Sets the general request property. If a property with the key already
     * exists, overwrite its value with the new value.
     * <p/>
     * <p> NOTE: HTTP requires all request properties which can
     * legally have multiple instances with the same key
     * to use a comma-seperated list syntax which enables multiple
     * properties to be appended into a single property.
     *
     * @param pKey   the keyword by which the request is known
     *               (e.g., "{@code accept}").
     * @param pValue the value associated with it.
     * @see #getRequestProperty(java.lang.String)
     */
    public void setRequestProperty(String pKey, String pValue) {
        if (connected) {
            throw new IllegalAccessError("Already connected");
        }
        String oldValue = requestProperties.getProperty(pKey);

        if (oldValue == null) {
            requestProperties.setProperty(pKey, pValue);
        }
        else {
            requestProperties.setProperty(pKey, oldValue + ", " + pValue);
        }
    }

    /**
     * Returns the value of the named general request property for this
     * connection.
     *
     * @param pKey the keyword by which the request is known (e.g., "accept").
     * @return the value of the named general request property for this
     *         connection.
     * @see #setRequestProperty(java.lang.String, java.lang.String)
     */
    public String getRequestProperty(String pKey) {
        if (connected) {
            throw new IllegalAccessError("Already connected");
        }
        return requestProperties.getProperty(pKey);
    }

    /**
     * Gets HTTP response status from responses like:
     * <PRE>
     * HTTP/1.0 200 OK
     * HTTP/1.0 401 Unauthorized
     * </PRE>
     * Extracts the ints 200 and 401 respectively.
     * Returns -1 if none can be discerned
     * from the response (i.e., the response is not valid HTTP).
     * <p/>
     * <!-- This is the J2SE 1.3 implementation... -->
     *
     * @return the HTTP Status-Code
     * @throws IOException if an error occurred connecting to the server.
     */
    public int getResponseCode() throws IOException {
        if (responseCode != -1) {
            return responseCode;
        }

        // Make sure we've gotten the headers
        getInputStream();
        String resp = getHeaderField(0);

        // should have no leading/trailing LWS
        // expedite the typical case by assuming it has the
        // form "HTTP/1.x <WS> 2XX <mumble>"
        int ind;

        try {
            ind = resp.indexOf(' ');
            while (resp.charAt(ind) == ' ') {
                ind++;
            }
            responseCode = Integer.parseInt(resp.substring(ind, ind + 3));
            responseMessage = resp.substring(ind + 4).trim();
            return responseCode;
        }
        catch (Exception e) {
            return responseCode;
        }
    }

    /**
     * Returns the name of the specified header field.
     *
     * @param pName the name of a header field.
     * @return the value of the named header field, or {@code null}
     *         if there is no such field in the header.
     */
    public String getHeaderField(String pName) {
        return responseHeaderFields.getProperty(StringUtil.toLowerCase(pName));
    }

    /**
     * Returns the value for the {@code n}<sup>th</sup> header field.
     * It returns {@code null} if there are fewer than
     * {@code n} fields.
     * <p/>
     * This method can be used in conjunction with the
     * {@code getHeaderFieldKey} method to iterate through all
     * the headers in the message.
     *
     * @param pIndex an index.
     * @return the value of the {@code n}<sup>th</sup> header field.
     * @see java.net.URLConnection#getHeaderFieldKey(int)
     */
    public String getHeaderField(int pIndex) {
        // TODO: getInputStream() first, to make sure we have header fields
        if (pIndex >= responseHeaders.length) {
            return null;
        }
        String field = responseHeaders[pIndex];

        // pIndex == 0, means the response code etc (i.e. "HTTP/1.1 200 OK").
        if ((pIndex == 0) || (field == null)) {
            return field;
        }
        int idx = field.indexOf(':');

        return ((idx > 0)
                ? field.substring(idx).trim()
                : "");  // TODO: ""  or null?
    }

    /**
     * Returns the key for the {@code n}<sup>th</sup> header field.
     *
     * @param pIndex an index.
     * @return the key for the {@code n}<sup>th</sup> header field,
     *         or {@code null} if there are fewer than {@code n}
     *         fields.
     */
    public String getHeaderFieldKey(int pIndex) {
        // TODO: getInputStream() first, to make sure we have header fields
        if (pIndex >= responseHeaders.length) {
            return null;
        }
        String field = responseHeaders[pIndex];

        if (StringUtil.isEmpty(field)) {
            return null;
        }
        int idx = field.indexOf(':');

        return StringUtil.toLowerCase(((idx > 0)
                ? field.substring(0, idx)
                : field));
    }

    /**
     * Sets the read timeout for the undelying socket.
     * A timeout of zero is interpreted as an
     * infinite timeout.
     *
     * @param pTimeout the maximum time the socket will block for read
     *                 operations, in milliseconds.
     */
    public void setTimeout(int pTimeout) {
        if (pTimeout < 0) {  // Must be positive
            throw new IllegalArgumentException("Timeout must be positive.");
        }
        timeout = pTimeout;
        if (socket != null) {
            try {
                socket.setSoTimeout(pTimeout);
            }
            catch (SocketException se) {
                // Not much to do about that...
            }
        }
    }

    /**
     * Gets the read timeout for the undelying socket.
     *
     * @return the maximum time the socket will block for read operations, in
     *         milliseconds.
     *         The default value is zero, which is interpreted as an
     *         infinite timeout.
     */
    public int getTimeout() {

        try {
            return ((socket != null)
                    ? socket.getSoTimeout()
                    : timeout);
        }
        catch (SocketException se) {
            return timeout;
        }
    }

    /**
     * Returns an input stream that reads from this open connection.
     *
     * @return an input stream that reads from this open connection.
     * @throws IOException if an I/O error occurs while
     *                     creating the input stream.
     */
    public synchronized InputStream getInputStream() throws IOException {
        if (!connected) {
            connect();
        }

        // Nothing to return
        if (responseCode == HTTP_NOT_FOUND) {
            throw new FileNotFoundException(url.toString());
        }
        int length;

        if (inputStream == null) {
            return null;
        }

        // "De-chunk" the output stream
        else if ("chunked".equalsIgnoreCase(getHeaderField("Transfer-Encoding"))) {
            if (!(inputStream instanceof ChunkedInputStream)) {
                inputStream = new ChunkedInputStream(inputStream);
            }
        }

        // Make sure we don't wait forever, if the content-length is known
        else if ((length = getHeaderFieldInt("Content-Length", -1)) >= 0) {
            if (!(inputStream instanceof FixedLengthInputStream)) {
                inputStream = new FixedLengthInputStream(inputStream, length);
            }
        }
        return inputStream;
    }

    /**
     * Returns an output stream that writes to this connection.
     *
     * @return an output stream that writes to this connection.
     * @throws IOException if an I/O error occurs while
     *                     creating the output stream.
     */
    public synchronized OutputStream getOutputStream() throws IOException {

        if (!connected) {
            connect();
        }
        return outputStream;
    }

    /**
     * Indicates that other requests to the server
     * are unlikely in the near future. Calling disconnect()
     * should not imply that this HttpURLConnection
     * instance can be reused for other requests.
     */
    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            }
            catch (IOException ioe) {

                // Does not matter, I guess.
            }
            socket = null;
        }
        connected = false;
    }

    /**
     * Internal connect method.
     */
    private void connect(final URL pURL, PasswordAuthentication pAuth, String pAuthType, int pRetries) throws IOException {
        // Find correct port
        final int port = (pURL.getPort() > 0)
                ? pURL.getPort()
                : HTTP_DEFAULT_PORT;

        // Create socket if we don't have one
        if (socket == null) {
            //socket = new Socket(pURL.getHost(), port); // Blocks...
            socket = createSocket(pURL, port, connectTimeout);
            socket.setSoTimeout(timeout);
        }

        // Get Socket output stream
        OutputStream os = socket.getOutputStream();

        // Connect using HTTP
        writeRequestHeaders(os, pURL, method, requestProperties, usingProxy(), pAuth, pAuthType);

        // Get response input stream
        InputStream sis = socket.getInputStream();
        BufferedInputStream is = new BufferedInputStream(sis);

        // Detatch reponse headers from reponse input stream
        InputStream header = detatchResponseHeader(is);

        // Parse headers and set response code/message
        responseHeaders = parseResponseHeader(header);
        responseHeaderFields = parseHeaderFields(responseHeaders);

        //System.err.println("Headers fields:");
        //responseHeaderFields.list(System.err);
        // Test HTTP response code, to see if further action is needed
        switch (getResponseCode()) {
            case HTTP_OK:
                // 200 OK
                inputStream = is;
                errorStream = null;
                break;

                /*
                  case HTTP_PROXY_AUTH:
                  // 407 Proxy Authentication Required
                  */
            case HTTP_UNAUTHORIZED:
                // 401 Unauthorized
                // Set authorization and try again.. Slightly more compatible
                responseCode = -1;

                // IS THIS REDIRECTION??
                //if (instanceFollowRedirects) { ???
                String auth = getHeaderField(HEADER_WWW_AUTH);

                // Missing WWW-Authenticate header for 401 response is an error
                if (StringUtil.isEmpty(auth)) {
                    throw new ProtocolException("Missing \"" + HEADER_WWW_AUTH + "\" header for response: 401 " + responseMessage);
                }

                // Get real mehtod from WWW-Authenticate header
                int SP = auth.indexOf(" ");
                String method;
                String realm = null;

                if (SP >= 0) {
                    method = auth.substring(0, SP);
                    if (auth.length() >= SP + 7) {
                        realm = auth.substring(SP + 7);  // " realm=".lenght() == 7
                    }

                    // else no realm
                }
                else {
                    // Default mehtod is Basic
                    method = SimpleAuthenticator.BASIC;
                }

                // Get PasswordAuthentication
                PasswordAuthentication pa = Authenticator.requestPasswordAuthentication(NetUtil.createInetAddressFromURL(pURL), port,
                        pURL.getProtocol(), realm, method);

                // Avoid infinite loop
                if (pRetries++ <= 0) {
                    throw new ProtocolException("Server redirected too many times (" + maxRedirects + ") (Authentication required: " + auth + ")");  // This is what sun.net.www.protocol.http.HttpURLConnection does
                }
                else if (pa != null) {
                    connect(pURL, pa, method, pRetries);
                }
                break;
            case HTTP_MOVED_PERM:
                // 301 Moved Permanently
            case HTTP_MOVED_TEMP:
                // 302 Found
            case HTTP_SEE_OTHER:
                // 303 See Other
                /*
                  case HTTP_USE_PROXY:
                  // 305 Use Proxy
                  // How do we handle this?
                  */
            case HTTP_REDIRECT:
                // 307 Temporary Redirect
                //System.err.println("Redirecting " + getResponseCode());
                if (instanceFollowRedirects) {
                    // Redirect
                    responseCode = -1;                 // Because of the java.net.URLConnection

                    // getResponseCode implementation...
                    // ---
                    // I think redirects must be get?
                    //setRequestMethod("GET");
                    // ---
                    String location = getHeaderField("Location");
                    URL newLoc = new URL(pURL, location);

                    // Test if we can reuse the Socket
                    if (!(newLoc.getAuthority().equals(pURL.getAuthority()) && (newLoc.getPort() == pURL.getPort()))) {
                        socket.close();                 // Close the socket, won't need it anymore
                        socket = null;
                    }
                    if (location != null) {
                        //System.err.println("Redirecting to " + location);
                        // Avoid infinite loop
                        if (--pRetries <= 0) {
                            throw new ProtocolException("Server redirected too many times (5)");
                        }
                        else {
                            connect(newLoc, pAuth, pAuthType, pRetries);
                        }
                    }
                    break;
                }

                // ...else, fall through default (if no Location: header)
            default :
                // Not 200 OK, or any of the redirect responses
                // Probably an error...
                errorStream = is;
                inputStream = null;
        }

        // --- Need rethinking...
        // No further questions, let the Socket wait forever (until the server
        // closes the connection)
        //socket.setSoTimeout(0);
        // Probably not... The timeout should only kick if the read BLOCKS.
        // Shutdown output, meaning any writes to the outputstream below will
        // probably fail...
        //socket.shutdownOutput();
        // Not a good idea at all... POSTs need the outputstream to send the
        // form-data.
        // --- /Need rethinking.
        outputStream = os;
    }

    private static interface SocketConnector extends Runnable {

        /**
         * Method getSocket
         *
         * @return the socket
         * @throws IOException
         */
        public Socket getSocket() throws IOException;
    }

    /**
     * Creates a socket to the given URL and port, with the given connect
     * timeout. If the socket waits more than the given timout to connect,
     * an ConnectException is thrown.
     *
     * @param pURL            the URL to connect to
     * @param pPort           the port to connect to
     * @param pConnectTimeout the connect timeout
     * @return the created Socket.
     * @throws ConnectException     if the connection is refused or otherwise
     *                              times out.
     * @throws UnknownHostException if the IP address of the host could not be
     *                              determined.
     * @throws IOException          if an I/O error occurs when creating the socket.
     * @todo Move this code to a SocetImpl or similar?
     * @see Socket#Socket(String,int)
     */
    private Socket createSocket(final URL pURL, final int pPort, int pConnectTimeout) throws IOException {
        Socket socket;
        final Object current = this;
        SocketConnector connector;
        Thread t = new Thread(connector = new SocketConnector() {

            private IOException mConnectException = null;
            private Socket mLocalSocket = null;

            public Socket getSocket() throws IOException {

                if (mConnectException != null) {
                    throw mConnectException;
                }
                return mLocalSocket;
            }

            // Run method
            public void run() {

                try {
                    mLocalSocket = new Socket(pURL.getHost(), pPort);  // Blocks...
                }
                catch (IOException ioe) {

                    // Store this exception for later
                    mConnectException = ioe;
                }

                // Signal that we are done
                synchronized (current) {
                    current.notify();
                }
            }
        });

        t.start();

        // Wait for connect
        synchronized (this) {
            try {

                /// Only wait if thread is alive!
                if (t.isAlive()) {
                    if (pConnectTimeout > 0) {
                        wait(pConnectTimeout);
                    }
                    else {
                        wait();
                    }
                }
            }
            catch (InterruptedException ie) {

                // Continue excecution on interrupt? Hmmm..
            }
        }

        // Throw exception if the socket didn't connect fast enough
        if ((socket = connector.getSocket()) == null) {
            throw new ConnectException("Socket connect timed out!");
        }
        return socket;
    }

    /**
     * Opens a communications link to the resource referenced by this
     * URL, if such a connection has not already been established.
     * <p/>
     * If the {@code connect} method is called when the connection
     * has already been opened (indicated by the {@code connected}
     * field having the value {@code true}), the call is ignored.
     * <p/>
     * URLConnection objects go through two phases: first they are
     * created, then they are connected.  After being created, and
     * before being connected, various options can be specified
     * (e.g., doInput and UseCaches).  After connecting, it is an
     * error to try to set them.  Operations that depend on being
     * connected, like getContentLength, will implicitly perform the
     * connection, if necessary.
     *
     * @throws IOException if an I/O error occurs while opening the
     *                     connection.
     * @see java.net.URLConnection#connected
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    public void connect() throws IOException {
        if (connected) {
            return;  // Ignore
        }
        connected = true;
        connect(url, null, null, maxRedirects);
    }

    /**
     * TODO: Proxy support is still missing.
     *
     * @return this method returns false, as proxy suport is not implemented.
     */
    public boolean usingProxy() {
        return false;
    }

    /**
     * Writes the HTTP request headers, for HTTP GET method.
     *
     * @see <A href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</A>
     */
    private static void writeRequestHeaders(OutputStream pOut, URL pURL, String pMethod, Properties pProps, boolean pUsingProxy,
                                            PasswordAuthentication pAuth, String pAuthType) {
        PrintWriter out = new PrintWriter(pOut, true);  // autoFlush

        if (!pUsingProxy) {
            out.println(pMethod + " " + (!StringUtil.isEmpty(pURL.getPath())
                    ? pURL.getPath()
                    : "/") + ((pURL.getQuery() != null)
                    ? "?" + pURL.getQuery()
                    : "") + " HTTP/1.1");  // HTTP/1.1

            // out.println("Connection: close"); // No persistent connections yet

            /*
              System.err.println(pMethod + " "
              + (!StringUtil.isEmpty(pURL.getPath()) ? pURL.getPath() : "/")
              + (pURL.getQuery() != null ? "?" + pURL.getQuery() : "")
              + " HTTP/1.1"); // HTTP/1.1
            */

            // Authority (Host: HTTP/1.1 field, but seems to work for HTTP/1.0)
            out.println("Host: " + pURL.getHost() + ((pURL.getPort() != -1)
                    ? ":" + pURL.getPort()
                    : ""));

            /*
              System.err.println("Host: " + pURL.getHost()
                        + (pURL.getPort() != -1 ? ":" + pURL.getPort() : ""));
            */
        }
        else {

            ////-- PROXY (absolute) VERSION
            out.println(pMethod + " " + pURL.getProtocol() + "://" + pURL.getHost() + ((pURL.getPort() != -1)
                    ? ":" + pURL.getPort()
                    : "") + pURL.getPath() + ((pURL.getQuery() != null)
                    ? "?" + pURL.getQuery()
                    : "") + " HTTP/1.1");
        }

        // Check if we have authentication
        if (pAuth != null) {

            // If found, set Authorization header
            byte[] userPass = (pAuth.getUserName() + ":" + new String(pAuth.getPassword())).getBytes();

            // "Authorization" ":" credentials
            out.println("Authorization: " + pAuthType + " " + BASE64.encode(userPass));

            /*
              System.err.println("Authorization: " + pAuthType + " "
              + BASE64.encode(userPass));
            */
        }

        // Iterate over properties

        for (Map.Entry<Object, Object> property : pProps.entrySet()) {
            out.println(property.getKey() + ": " + property.getValue());

            //System.err.println(property.getKey() + ": " + property.getValue());
        }
        out.println();  // Empty line, marks end of request-header
    }

    /**
     * Finds the end of the HTTP response header in an array of bytes.
     *
     * @todo This one's a little dirty...
     */
    private static int findEndOfHeader(byte[] pBytes, int pEnd) {
        byte[] header = HTTP_HEADER_END.getBytes();

        // Normal condition, check all bytes
        for (int i = 0; i < pEnd - 4; i++) {  // Need 4 bytes to match
            if ((pBytes[i] == header[0]) && (pBytes[i + 1] == header[1]) && (pBytes[i + 2] == header[2]) && (pBytes[i + 3] == header[3])) {

                //System.err.println("FOUND END OF HEADER!");
                return i + 4;
            }
        }

        // Check last 3 bytes, to check if we have a partial match
        if ((pEnd - 1 >= 0) && (pBytes[pEnd - 1] == header[0])) {

            //System.err.println("FOUND LAST BYTE");
            return -2;  // LAST BYTE
        }
        else if ((pEnd - 2 >= 0) && (pBytes[pEnd - 2] == header[0]) && (pBytes[pEnd - 1] == header[1])) {

            //System.err.println("FOUND LAST TWO BYTES");
            return -3;  // LAST TWO BYTES
        }
        else if ((pEnd - 3 >= 0) && (pBytes[pEnd - 3] == header[0]) && (pBytes[pEnd - 2] == header[1]) && (pBytes[pEnd - 1] == header[2])) {

            //System.err.println("FOUND LAST THREE BYTES");
            return -4;  // LAST THREE BYTES
        }
        return -1;  // NO BYTES MATCH
    }

    /**
     * Reads the header part of the response, and copies it to a different
     * InputStream.
     */
    private static InputStream detatchResponseHeader(BufferedInputStream pIS) throws IOException {
        // Store header in byte array
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        pIS.mark(BUF_SIZE);
        byte[] buffer = new byte[BUF_SIZE];
        int length;
        int headerEnd;

        // Read from iput, store in bytes
        while ((length = pIS.read(buffer)) != -1) {

            // End of header?
            headerEnd = findEndOfHeader(buffer, length);
            if (headerEnd >= 0) {

                // Write rest
                bytes.write(buffer, 0, headerEnd);

                // Go back to last mark
                pIS.reset();

                // Position stream to right after header, and exit loop
                pIS.skip(headerEnd);
                break;
            }
            else if (headerEnd < -1) {

                // Write partial (except matching header bytes)
                bytes.write(buffer, 0, length - 4);

                // Go back to last mark
                pIS.reset();

                // Position stream to right before potential header end
                pIS.skip(length - 4);
            }
            else {

                // Write all
                bytes.write(buffer, 0, length);
            }

            // Can't read more than BUF_SIZE ahead anyway
            pIS.mark(BUF_SIZE);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    /**
     * Pareses the response header fields.
     */
    private static Properties parseHeaderFields(String[] pHeaders) {
        Properties headers = new Properties();

        // Get header information
        int split;
        String field;
        String value;

        for (String header : pHeaders) {
            //System.err.println(pHeaders[i]);
            if ((split = header.indexOf(":")) > 0) {

                // Read & parse..?
                field = header.substring(0, split);
                value = header.substring(split + 1);

                //System.err.println(field + ": " + value.trim());
                headers.setProperty(StringUtil.toLowerCase(field), value.trim());
            }
        }
        return headers;
    }

    /**
     * Parses the response headers.
     */
    private static String[] parseResponseHeader(InputStream pIS) throws IOException {
        List<String> headers = new ArrayList<String>();

        // Wrap Stream in Reader
        BufferedReader in = new BufferedReader(new InputStreamReader(pIS));

        // Get response status
        String header;

        while ((header = in.readLine()) != null) {
            //System.err.println(header);
            headers.add(header);
        }
        return headers.toArray(new String[headers.size()]);
    }

    /**
     * A FilterInputStream that wraps HTTP streams, with given content-length.
     */
    protected static class FixedLengthInputStream extends FilterInputStream {

        private int mBytesLeft = 0;

        protected FixedLengthInputStream(InputStream pIS, int pLength) {
            super(pIS);
            mBytesLeft = pLength;
        }

        public int available() throws IOException {
            int available = in.available();

            return ((available < mBytesLeft)
                    ? available
                    : mBytesLeft);
        }

        public int read() throws IOException {
            if (mBytesLeft-- > 0) {
                return in.read();
            }
            return -1;
        }

        public int read(byte[] pBytes, int pOffset, int pLength) throws IOException {
            int read;

            if (mBytesLeft <= 0) {
                return -1;  // EOF
            }
            else if (mBytesLeft < pLength) {

                // Read all available
                read = in.read(pBytes, pOffset, mBytesLeft);

                //System.err.println("Reading partial: " + read);
                mBytesLeft -= read;
                return read;
            }

            // Just read
            read = in.read(pBytes, pOffset, pLength);

            //System.err.println("Reading all avail: " + read);
            mBytesLeft -= read;
            return read;
        }
    }

    /**
     * A FilterInputStream that wraps HTTP 1.1 "chunked" transfer mode.
     */
    protected static class ChunkedInputStream extends FilterInputStream {

        private int mAvailableInCurrentChunk = 0;

        /**
         * Creates an input streams that removes the "chunk-headers" and
         * makes it look like any other input stream.
         */
        protected ChunkedInputStream(InputStream pIS) {

            super(pIS);
            if (pIS == null) {
                throw new IllegalArgumentException("InputStream may not be null!");
            }
        }

        /**
         * Returns the number of bytes that can be read from this input stream
         * without blocking.
         * <P>
         * This version returns whatever is less of in.available() and the
         * length of the current chunk.
         *
         * @return the number of bytes that can be read from the input stream
         *         without blocking.
         * @throws IOException if an I/O error occurs.
         * @see #in
         */
        public int available() throws IOException {

            if (mAvailableInCurrentChunk == 0) {
                mAvailableInCurrentChunk = parseChunkSize();
            }
            int realAvail = in.available();

            return (mAvailableInCurrentChunk < realAvail)
                    ? mAvailableInCurrentChunk
                    : realAvail;
        }

        /**
         * Reads up to len bytes of data from this input stream into an array
         * of bytes. This method blocks until some input is available.
         * <P>
         * This version will read up to len bytes of data, or as much as is
         * available in the current chunk. If there is no more data in the
         * curernt chunk, the method will read the size of the next chunk, and
         * read from that, until the last chunk is read (a chunk with a size of
         * 0).
         *
         * @param pBytes  the buffer into which the data is read.
         * @param pOffset the start offset of the data.
         * @param pLength the maximum number of bytes read.
         * @return the total number of bytes read into the buffer, or -1 if
         *         there is no more data because the end of the stream has been
         *         reached.
         * @throws IOException if an I/O error occurs.
         * @see #in
         */
        public int read(byte[] pBytes, int pOffset, int pLength) throws IOException {

            //System.err.println("Avail: " + mAvailableInCurrentChunk
            //         + " length: " + pLength);
            int read;

            if (mAvailableInCurrentChunk == -1) {
                return -1;  // EOF
            }
            if (mAvailableInCurrentChunk == 0) {

                //System.err.println("Nothing to read, parsing size!");
                // If nothing is read so far, read chunk header
                mAvailableInCurrentChunk = parseChunkSize();
                return read(pBytes, pOffset, pLength);
            }
            else if (mAvailableInCurrentChunk < pLength) {

                // Read all available
                read = in.read(pBytes, pOffset, mAvailableInCurrentChunk);

                //System.err.println("Reading partial: " + read);
                mAvailableInCurrentChunk -= read;
                return read;
            }

            // Just read
            read = in.read(pBytes, pOffset, pLength);

            //System.err.println("Reading all avail: " + read);
            mAvailableInCurrentChunk -= read;
            return read;
        }

        /**
         * Reads the next byte of data from this input stream. The value byte
         * is returned as an int in the range 0 to 255. If no byte is available
         * because the end of the stream has been reached, the value -1 is
         * returned. This method blocks until input data is available, the end
         * of the stream is detected, or an exception is thrown.
         * <P>
         * This version reads one byte of data from the current chunk as long
         * as there is more data in the chunk. If there is no more data in the
         * curernt chunk, the method will read the size of the next chunk, and
         * read from that, until the last chunk is read (a chunk with a size of
         * 0).
         *
         * @return the next byte of data, or -1 if the end of the stream is
         *         reached.
         * @see #in
         */
        public int read() throws IOException {

            // We have no data, parse chunk header
            if (mAvailableInCurrentChunk == -1) {
                return -1;
            }
            else if (mAvailableInCurrentChunk == 0) {

                // Next chunk!
                mAvailableInCurrentChunk = parseChunkSize();
                return read();
            }
            mAvailableInCurrentChunk--;
            return in.read();
        }

        /**
         * Reads the chunk size from the chunk header
         * {@code chunk-size [SP chunk-extension] CRLF}.
         * The chunk-extension is simply discarded.
         *
         * @return the length of the current chunk, or -1 if the current chunk
         *         is the last-chunk (a chunk with the size of 0).
         */
        protected int parseChunkSize() throws IOException {

            StringBuilder buf = new StringBuilder();
            int b;

            // read chunk-size, chunk-extension (if any) and CRLF
            while ((b = in.read()) > 0) {
                if ((b == '\r') && (in.read() == '\n')) {  // Should be no CR or LF
                    break;                                   // except for this one...
                }
                buf.append((char) b);
            }
            String line = buf.toString();

            // Happens, as we don't read CRLF off the end of the chunk data...
            if (line.length() == 0) {
                return 0;
            }

            // Discard any chunk-extensions, and read size (HEX).
            int spIdx = line.indexOf(' ');
            int size = Integer.parseInt(((spIdx >= 0)
                    ? line.substring(0, spIdx)
                    : line), 16);

            // This is the last chunk (=EOF)
            if (size == 0) {
                return -1;
            }
            return size;
        }
    }
}
