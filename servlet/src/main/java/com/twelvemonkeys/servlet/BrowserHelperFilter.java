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

import com.twelvemonkeys.lang.StringUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * BrowserHelperFilter
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: BrowserHelperFilter.java#1 $
 */
public class BrowserHelperFilter extends GenericFilter {
    private static final String HTTP_HEADER_ACCEPT = "Accept";
    protected static final String HTTP_HEADER_USER_AGENT = "User-Agent";

    // TODO: Consider using unmodifiable LinkedHashMap<Pattern, String> instead
    private Pattern[] knownAgentPatterns;
    private String[] knownAgentAccepts;

    /**
     * Sets the accept-mappings for this filter
     * @param pPropertiesFile name of accept-mappings properties files
     * @throws ServletConfigException if the accept-mappings properties
     *                                file cannot be read.
     */
    @InitParam(name = "accept-mappings-file")
    public void setAcceptMappingsFile(String pPropertiesFile) throws ServletConfigException {
        // NOTE: Format is:
        // <agent-name>=<reg-exp>
        // <agent-name>.accept=<http-accept-header>

        Properties mappings = new Properties();

        try {
            log("Reading Accept-mappings properties file: " + pPropertiesFile);
            mappings.load(getServletContext().getResourceAsStream(pPropertiesFile));

            //System.out.println("--> Loaded file: " + pPropertiesFile);
        }
        catch (IOException e) {
            throw new ServletConfigException("Could not read Accept-mappings properties file: " + pPropertiesFile, e);
        }

        parseMappings(mappings);
    }

    private void parseMappings(Properties mappings) {
        List<Pattern> patterns = new ArrayList<Pattern>();
        List<String> accepts = new ArrayList<String>();

        for (Object key : mappings.keySet()) {
            String agent = (String) key;

            if (agent.endsWith(".accept")) {
                continue;
            }

            //System.out.println("--> Adding accept-mapping for User-Agent: " + agent);

            try {
                String accept = (String) mappings.get(agent + ".accept");

                if (!StringUtil.isEmpty(accept)) {
                    patterns.add(Pattern.compile((String) mappings.get(agent)));
                    accepts.add(accept);
                    //System.out.println("--> " + agent + " accepts: " + accept);
                }
                else {
                    log("Missing Accept mapping for User-Agent: " + agent);
                }
            }
            catch (PatternSyntaxException e) {
                log("Could not parse User-Agent identification for " + agent, e);
            }

            knownAgentPatterns = patterns.toArray(new Pattern[patterns.size()]);
            knownAgentAccepts = accepts.toArray(new String[accepts.size()]);
        }
    }

    public void init() throws ServletException {
        if (knownAgentAccepts == null || knownAgentAccepts.length == 0) {
            throw new ServletConfigException("No User-Agent/Accept mappings for filter: " + getFilterName());
        }
    }

    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
        if (pRequest instanceof HttpServletRequest) {
            //System.out.println("--> Trying to find User-Agent/Accept headers...");
            HttpServletRequest request = (HttpServletRequest) pRequest;

            // Check if User-Agent is in list of known agents
            if (knownAgentPatterns != null && knownAgentPatterns.length > 0) {
                String agent = request.getHeader(HTTP_HEADER_USER_AGENT);
                //System.out.println("--> User-Agent: " + agent);

                for (int i = 0; i < knownAgentPatterns.length; i++) {
                    Pattern pattern = knownAgentPatterns[i];
                    //System.out.println("--> Pattern: " + pattern);

                    if (pattern.matcher(agent).matches()) {
                        // TODO: Consider merge known with real accept, in case plugins add extra capabilities?
                        final String fakeAccept = knownAgentAccepts[i];
                        //System.out.println("--> User-Agent: " + agent + " accepts: " + fakeAccept);

                        pRequest = new HttpServletRequestWrapper(request) {
                            public String getHeader(String pName) {
                                if (HTTP_HEADER_ACCEPT.equals(pName)) {
                                    return fakeAccept;
                                }

                                return super.getHeader(pName);
                            }
                        };

                        break;
                    }
                }
            }
        }

        pChain.doFilter(pRequest, pResponse);
    }
}
