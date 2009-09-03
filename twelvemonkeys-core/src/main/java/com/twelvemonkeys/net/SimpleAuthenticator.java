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

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Hashtable;

/**
 * A simple Authenticator implementation.
 * Singleton class, obtain reference through the static 
 * <CODE>getInstance</CODE> method.
 * <P>
 * <EM>After swearing, sweating, pulling my hair, banging my head repeatedly
 * into the walls and reading the java.net.Authenticator API documentation 
 * once more, an idea came to my mind. This is the result. I hope you find it
 * useful. -- Harald K.</EM>
 *
 * @see java.net.Authenticator
 *
 * @author Harald Kuhr (haraldk@iconmedialab.no)
 * @version 1.0 
 */
public class SimpleAuthenticator extends Authenticator {
    
    /** The reference to the single instance of this class. */
    private static SimpleAuthenticator sInstance = null;
    /** Keeps track of the state of this class. */
    private static boolean sInitialized = false;

    // These are used for the identification hack.
    private final static String MAGIC = "magic";
    private final static int FOURTYTWO = 42;

    /** Basic authentication scheme. */
    public final static String BASIC = "Basic";

    /**
     * The hastable that keeps track of the PasswordAuthentications.
     */

    protected Hashtable mPasswordAuthentications = null;

    /**
     * The hastable that keeps track of the Authenticators.
     */

    protected Hashtable mAuthenticators = null;
    	
    /**
     * Creates a SimpleAuthenticator.
     */

    private SimpleAuthenticator() {
    	mPasswordAuthentications = new Hashtable();
    	mAuthenticators = new Hashtable();
    }

    /**
     * Gets the SimpleAuthenticator instance and registers it through the 
     * Authenticator.setDefault(). If there is no current instance
     * of the SimpleAuthenticator in the VM, one is created. This method will
     * try to figure out if the setDefault() succeeded (a hack), and will 
     * return null if it was not able to register the instance as default.
     *
     * @return The single instance of this class, or null, if another 
     * Authenticator is allready registered as default.
     */

    public static synchronized SimpleAuthenticator getInstance() {
    	if (!sInitialized) {
    	    // Create an instance
    	    sInstance = new SimpleAuthenticator();

    	    // Try to set default (this may quietly fail...)
    	    Authenticator.setDefault(sInstance);

    	    // A hack to figure out if we really did set the authenticator
    	    PasswordAuthentication pa = 
    	    	Authenticator.requestPasswordAuthentication(null, FOURTYTWO,
    	    	                                            null, null, MAGIC);

    	    // If this test returns false, we didn't succeed, so we set the 
    	    // instance back to null.
    	    if (pa == null || !MAGIC.equals(pa.getUserName()) ||
    	        !("" + FOURTYTWO).equals(new String(pa.getPassword())))
    	    	sInstance = null;

    	    // Done
    	    sInitialized = true;
    	}

    	return sInstance;    	
    }

    /**
     * Gets the PasswordAuthentication for the request. Called when password 
     * authorization is needed.
     *
     * @return The PasswordAuthentication collected from the user, or null if 
     *         none is provided.
     */
    
    protected PasswordAuthentication getPasswordAuthentication() {
    	// Don't worry, this is just a hack to figure out if we were able
    	// to set this Authenticator through the setDefault method. 
    	if (!sInitialized && MAGIC.equals(getRequestingScheme()) 
    	    && getRequestingPort() == FOURTYTWO)
    	    return new PasswordAuthentication(MAGIC, ("" + FOURTYTWO)
        	    .toCharArray());
	/*
	System.err.println("getPasswordAuthentication");
	System.err.println(getRequestingSite());
	System.err.println(getRequestingPort());
	System.err.println(getRequestingProtocol());
	System.err.println(getRequestingPrompt());
	System.err.println(getRequestingScheme());
	*/

    	// TODO: 
    	// Look for a more specific PasswordAuthenticatior before using
    	// Default:
    	// 
    	// if (...)
    	//    return pa.requestPasswordAuthentication(getRequestingSite(),
    	//    	                                      getRequestingPort(), 
    	//                                            getRequestingProtocol(), 
    	//                                            getRequestingPrompt(),
    	//                                    	      getRequestingScheme());

    	return (PasswordAuthentication) 
    	    mPasswordAuthentications.get(new AuthKey(getRequestingSite(),
    	                                             getRequestingPort(), 
    	                                             getRequestingProtocol(), 
    	                                             getRequestingPrompt(),
    	                                             getRequestingScheme()));
    }

    /**
     * Registers a PasswordAuthentication with a given URL address.
     *
     */

    public PasswordAuthentication registerPasswordAuthentication(URL pURL, 
                                                  PasswordAuthentication pPA) {
    	return  registerPasswordAuthentication(NetUtil.createInetAddressFromURL(pURL),
    	                                       pURL.getPort(),
    	                                       pURL.getProtocol(),
    	                                       null, // Prompt/Realm
    	                                       BASIC,
    	                                       pPA);
    }

    /**
     * Registers a PasswordAuthentication with a given net address.
     *
     */

    public PasswordAuthentication registerPasswordAuthentication(
    	InetAddress pAddress, int pPort, String pProtocol,
    	String pPrompt, String pScheme, PasswordAuthentication pPA)
    {
	/*
	System.err.println("registerPasswordAuthentication");
	System.err.println(pAddress);
	System.err.println(pPort);
	System.err.println(pProtocol);
	System.err.println(pPrompt);
	System.err.println(pScheme);
	*/

    	return (PasswordAuthentication) 
    	    mPasswordAuthentications.put(new AuthKey(pAddress, pPort, 
    	                                             pProtocol, pPrompt, 
    	                                             pScheme),
    	                                 pPA);
    }

    /**
     * Unregisters a PasswordAuthentication with a given URL address.
     *
     */
    
    public PasswordAuthentication unregisterPasswordAuthentication(URL pURL) {
    	return unregisterPasswordAuthentication(NetUtil.createInetAddressFromURL(pURL),
    	                                        pURL.getPort(),
    	                                        pURL.getProtocol(),
    	                                        null,
    	                                        BASIC);
    }

    /**
     * Unregisters a PasswordAuthentication with a given net address.
     *
     */
    
    public PasswordAuthentication unregisterPasswordAuthentication(
        InetAddress pAddress, int pPort, String pProtocol,
    	String pPrompt, String pScheme)
    {
    	return (PasswordAuthentication) 
    	    mPasswordAuthentications.remove(new AuthKey(pAddress, pPort,
    	                                                pProtocol, pPrompt,
    	                                                pScheme));
    }

    /**
     * TODO: Registers a PasswordAuthenticator that can answer authentication
     * requests.
     * 
     * @see PasswordAuthenticator
     */

    public void registerPasswordAuthenticator(PasswordAuthenticator pPA,
                                              AuthenticatorFilter pFilter) {
    	mAuthenticators.put(pPA, pFilter);
    }

    /**
     * TODO: Unregisters a PasswordAuthenticator that can answer authentication
     * requests.
     * 
     * @see PasswordAuthenticator
     */
    
    public void unregisterPasswordAuthenticator(PasswordAuthenticator pPA) {
    	mAuthenticators.remove(pPA);
    }
    
}

/**
 * Utility class, used for caching the PasswordAuthentication objects.
 * Everything but address may be null
 */

class AuthKey {
    
    InetAddress mAddress = null;
    int mPort = -1;
    String mProtocol = null;
    String mPrompt = null;
    String mScheme = null;

    AuthKey(InetAddress pAddress, int pPort, String pProtocol,
            String pPrompt, String pScheme) {
    	if (pAddress == null)
    	    throw new IllegalArgumentException("Address argument can't be null!");

    	mAddress = pAddress;
    	mPort = pPort;
    	mProtocol = pProtocol;
    	mPrompt = pPrompt;
    	mScheme = pScheme;

    	//    	System.out.println("Created: " + this); 
    }

    /**
     * Creates a string representation of this object.
     */

    public String toString() {
    	return "AuthKey[" + mAddress + ":"  + mPort + "/"  + mProtocol + " \"" + mPrompt + "\" (" + mScheme + ")]";
    }

    public boolean equals(Object pObj) {
    	return (pObj instanceof AuthKey ? equals((AuthKey) pObj) : false);
    }

    // Ahem.. Breaks the rule from Object.equals(Object):
    // It is transitive: for any reference values x, y, and z, if x.equals(y) 
    // returns true and y.equals(z) returns true, then x.equals(z) 
    // should return true. 

    public boolean equals(AuthKey pKey) {
    	// Maybe allow nulls, and still be equal?
    	return (mAddress.equals(pKey.mAddress)
    	        && (mPort == -1
    	            || pKey.mPort == -1
    	            || mPort == pKey.mPort)
    	        && (mProtocol == null 
    	            || pKey.mProtocol == null
    	            || mProtocol.equals(pKey.mProtocol))
    	        && (mPrompt == null 
    	            || pKey.mPrompt == null
    	            || mPrompt.equals(pKey.mPrompt))
    	        && (mScheme == null
    	            || pKey.mScheme == null
    	            || mScheme.equalsIgnoreCase(pKey.mScheme)));
    }

    public int hashCode() {
    	// There won't be too many pr address, will it? ;-)
    	return mAddress.hashCode();
    }
}

