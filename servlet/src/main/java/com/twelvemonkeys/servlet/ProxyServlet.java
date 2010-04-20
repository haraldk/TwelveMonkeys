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

package com.twelvemonkeys.servlet;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

/**
 * A simple proxy servlet implementation. Supports HTTP and HTTPS.
 * <p/>
 * Note: The servlet is not a true HTTP proxy as described in
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">RFC 2616</a>,
 * instead it passes on all incoming HTTP requests to the configured remote
 * server.
 * Useful for bypassing firewalls or to avoid exposing internal network
 * infrastructure to external clients.
 * <p/>
 * At the moment, no caching of content is implemented.
 * <p/>
 * If the {@code remoteServer} init parameter is not set, the servlet will
 * respond by sending a {@code 500 Internal Server Error} response to the client.
 * If the configured remote server is down, or unreachable, the servlet will
 * respond by sending a {@code 502 Bad Gateway} response to the client.
 * Otherwise, the response from the remote server will be tunneled unmodified
 * to the client.
 * 
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 *
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/ProxyServlet.java#1 $
 */
public class ProxyServlet extends GenericServlet {

    /** Remote server host name or IP address */
    protected String mRemoteServer = null;
    /** Remote server port */
    protected int mRemotePort = 80;
    /** Remote server "mount" path */
    protected String mRemotePath = "";

    private static final String HTTP_REQUEST_HEADER_HOST = "host";
    private static final String HTTP_RESPONSE_HEADER_SERVER = "server";
    private static final String MESSAGE_REMOTE_SERVER_NOT_CONFIGURED = "Remote server not configured.";

    /**
     * Called by {@code init} to set the remote server. Must be a valid host
     * name or IP address. No default.
     *
     * @param pRemoteServer
     */
    public void setRemoteServer(String pRemoteServer) {
        mRemoteServer = pRemoteServer;
    }

    /**
     * Called by {@code init} to set the remote port. Must be a number.
     * Default is {@code 80}.
     *
     * @param pRemotePort
     */
    public void setRemotePort(String pRemotePort) {
        try {
            mRemotePort = Integer.parseInt(pRemotePort);
        }
        catch (NumberFormatException e) {
            log("RemotePort must be a number!", e);
        }
    }

    /**
     * Called by {@code init} to set the remote path. May be an empty string
     * for the root path, or any other valid path on the remote server.
     * Default is {@code ""}.
     *
     * @param pRemotePath
     */
    public void setRemotePath(String pRemotePath) {
        if (StringUtil.isEmpty(pRemotePath)) {
            pRemotePath = "";
        }
        else if (pRemotePath.charAt(0) != '/') {
            pRemotePath = "/" + pRemotePath;
        }

        mRemotePath = pRemotePath;
    }

    /**
     * Override {@code service} to use HTTP specifics.
     *
     * @param pRequest
     * @param pResponse
     *
     * @throws ServletException
     * @throws IOException
     *
     * @see #service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public final void service(ServletRequest pRequest, ServletResponse pResponse) throws ServletException, IOException {
        service((HttpServletRequest) pRequest, (HttpServletResponse) pResponse);
    }

    /**
     * Services a single request.
     * Supports HTTP and HTTPS.
     *
     * @param pRequest
     * @param pResponse
     *
     * @throws ServletException
     * @throws IOException
     *
     * @see ProxyServlet Class descrition
     */
    protected void service(HttpServletRequest pRequest, HttpServletResponse pResponse) throws ServletException, IOException {
        // Sanity check configuration
        if (mRemoteServer == null) {
            log(MESSAGE_REMOTE_SERVER_NOT_CONFIGURED);
            pResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    MESSAGE_REMOTE_SERVER_NOT_CONFIGURED);
            return;
        }

        HttpURLConnection remoteConnection = null;
        try {
            // Recreate request URI for remote request
            String requestURI = createRemoteRequestURI(pRequest);
            URL remoteURL = new URL(pRequest.getScheme(), mRemoteServer, mRemotePort, requestURI);

            // Get connection, with method from original request
            // NOTE: The actual connection is not done before we ask for streams...
            // NOTE: The HttpURLConnection is supposed to handle multiple
            // requests to the same server internally
            String method = pRequest.getMethod();
            remoteConnection = (HttpURLConnection) remoteURL.openConnection();
            remoteConnection.setRequestMethod(method);

            // Copy header fields
            copyHeadersFromClient(pRequest, remoteConnection);

            // Do proxy specifc stuff?
            // TODO: Read up the specs from RFC 2616 (HTTP) on proxy behaviour
            // TODO: RFC 2616 says "[a] proxy server MUST NOT establish an HTTP/1.1
            // persistent connection with an HTTP/1.0 client"

            // Copy message body from client to remote server
            copyBodyFromClient(pRequest, remoteConnection);

            // Set response status code from remote server to client
            int responseCode = remoteConnection.getResponseCode();
            pResponse.setStatus(responseCode);
            //System.out.println("Response is: " + responseCode + " " + remoteConnection.getResponseMessage());

            // Copy header fields back
            copyHeadersToClient(remoteConnection, pResponse);

            // More proxy specific stuff?

            // Copy message body from remote server to client
            copyBodyToClient(remoteConnection, pResponse);
        }
        catch (ConnectException e) {
            // In case we could not connecto to the remote server
            log("Could not connect to remote server.", e);
            pResponse.sendError(HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
        }
        finally {
            // Disconnect from server
            // TODO: Should we actually do this?
            if (remoteConnection != null) {
                remoteConnection.disconnect();
            }
        }
    }

    /**
     * Copies the message body from the remote server to the client (outgoing
     * {@code HttpServletResponse}).
     *
     * @param pRemoteConnection
     * @param pResponse
     *
     * @throws IOException
     */
    private void copyBodyToClient(HttpURLConnection pRemoteConnection, HttpServletResponse pResponse) throws IOException {
        InputStream fromRemote = null;
        OutputStream toClient = null;

        try {
            // Get either input or error stream
            try {
                fromRemote = pRemoteConnection.getInputStream();
            }
            catch (IOException e) {
                // If exception, use errorStream instead
                fromRemote = pRemoteConnection.getErrorStream();
            }

            // I guess the stream might be null if there is no response other
            // than headers (Continue, No Content, etc).
            if (fromRemote != null) {
                toClient = pResponse.getOutputStream();
                FileUtil.copy(fromRemote, toClient);
            }
        }
        finally {
            if (fromRemote != null) {
                try {
                    fromRemote.close();
                }
                catch (IOException e) {
                    log("Stream from remote could not be closed.", e);
                }
            }
            if (toClient != null) {
                try {
                    toClient.close();
                }
                catch (IOException e) {
                    log("Stream to client could not be closed.", e);
                }
            }
        }
    }

    /**
     * Copies the message body from the client (incomming
     * {@code HttpServletRequest}) to the remote server if the request method
     * is {@code POST} or <tt>PUT<tt>.
     * Otherwise this method does nothing.
     *
     * @param pRequest
     * @param pRemoteConnection
     *
     * @throws java.io.IOException
     */
    private void copyBodyFromClient(HttpServletRequest pRequest, HttpURLConnection pRemoteConnection) throws IOException {
        // If this is a POST or PUT, copy message body from client remote server
        if (!("POST".equals(pRequest.getMethod()) || "PUT".equals(pRequest.getMethod()))) {
            return;
        }

        // NOTE: Setting doOutput to true, will make it a POST request (why?)...
        pRemoteConnection.setDoOutput(true);

        // Get streams and do the copying
        InputStream fromClient = null;
        OutputStream toRemote = null;
        try {
            fromClient = pRequest.getInputStream();
            toRemote = pRemoteConnection.getOutputStream();
            FileUtil.copy(fromClient, toRemote);
        }
        finally {
            if (fromClient != null) {
                try {
                    fromClient.close();
                }
                catch (IOException e) {
                    log("Stream from client could not be closed.", e);
                }
            }
            if (toRemote != null) {
                try {
                    toRemote.close();
                }
                catch (IOException e) {
                    log("Stream to remote could not be closed.", e);
                }
            }
        }
    }

    /**
     * Creates the remote request URI based on the incoming request.
     * The URI will include any query strings etc.
     *
     * @param pRequest
     *
     * @return a {@code String} representing the remote request URI
     */
    private String createRemoteRequestURI(HttpServletRequest pRequest) {
        StringBuilder requestURI = new StringBuilder(mRemotePath);
        requestURI.append(pRequest.getPathInfo());

        if (!StringUtil.isEmpty(pRequest.getQueryString())) {
            requestURI.append("?");
            requestURI.append(pRequest.getQueryString());
        }

        return requestURI.toString();
    }

    /**
     * Copies headers from the remote connection back to the client
     * (the outgoing HttpServletResponse). All headers except the "Server"
     * header are copied.
     *
     * @param pRemoteConnection
     * @param pResponse
     */
    private void copyHeadersToClient(HttpURLConnection pRemoteConnection, HttpServletResponse pResponse) {
        // NOTE: There is no getHeaderFieldCount method or similar...
        // Also, the getHeaderFields() method was introduced in J2SE 1.4, and
        // we want to be 1.2 compatible.
        // So, just try to loop until there are no more headers.
        int i = 0;
        while (true) {
            String key = pRemoteConnection.getHeaderFieldKey(i);
            // NOTE: getHeaderField(String) returns only the last value
            String value = pRemoteConnection.getHeaderField(i);

            // If the key is not null, life is simple, and Sun is shining
            // However, the default implementations includes the HTTP response
            // code ("HTTP/1.1 200 Ok" or similar) as a header field with
            // key "null" (why..?)...
            // In addition, we want to skip the original "Server" header
            if (key != null && !HTTP_RESPONSE_HEADER_SERVER.equalsIgnoreCase(key)) {
                //System.out.println("client <<<-- remote: " + key + ": " + value);
                pResponse.setHeader(key, value);
            }
            else if (value == null) {
                // If BOTH key and value is null, there are no more header fields
                break;
            }

            i++;
        }

        /* 1.4+ version below....
        Map headers = pRemoteConnection.getHeaderFields();
        for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry header = (Map.Entry) iterator.next();

            List values = (List) header.getValue();

            for (Iterator valueIter = values.iterator(); valueIter.hasNext();) {
                String value = (String) valueIter.next();
                String key = (String) header.getKey();

                // Skip the server header
                if (HTTP_RESPONSE_HEADER_SERVER.equalsIgnoreCase(key)) {
                    key = null;
                }

                // The default implementations includes the HTTP response code
                // ("HTTP/1.1 200 Ok" or similar) as a header field with
                // key "null" (why..?)...
                if (key != null) {
                    //System.out.println("client <<<-- remote: " + key + ": " + value);
                    pResponse.setHeader(key, value);
                }
            }
        }
        */
    }

    /**
     * Copies headers from the client (the incoming {@code HttpServletRequest})
     * to the outgoing connection.
     * All headers except the "Host" header are copied.
     *
     * @param pRequest
     * @param pRemoteConnection
     */
    private void copyHeadersFromClient(HttpServletRequest pRequest, HttpURLConnection pRemoteConnection) {
        Enumeration headerNames = pRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            Enumeration headerValues = pRequest.getHeaders(headerName);

            // Skip the "host" header, as we want something else
            if (HTTP_REQUEST_HEADER_HOST.equalsIgnoreCase(headerName)) {
                // Skip this header
                headerName = null;
            }

            // Set the the header to the remoteConnection
            if (headerName != null) {
                // Convert from multiple line to single line, comma separated, as
                // there seems to be a shortcoming in the URLConneciton API...
                StringBuilder headerValue = new StringBuilder();
                while (headerValues.hasMoreElements()) {
                    String value = (String) headerValues.nextElement();
                    headerValue.append(value);
                    if (headerValues.hasMoreElements()) {
                        headerValue.append(", ");
                    }
                }

                //System.out.println("client -->>> remote: " + headerName + ": " + headerValue);
                pRemoteConnection.setRequestProperty(headerName, headerValue.toString());
            }
        }
    }
}
