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

package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.lang.StringUtil;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * This filter implements server side content negotiation and transcoding for
 * images.
 *
 * @todo Add support for automatic recognition of known browsers, to avoid
 * unneccessary conversion (as IE supports PNG, the latests FireFox supports
 * JPEG and GIF, etc. even though they both don't explicitly list these formats
 * in their Accept headers).
 */
public class ContentNegotiationFilter extends ImageFilter {

    private final static String MIME_TYPE_IMAGE_PREFIX = "image/";
    private static final String MIME_TYPE_IMAGE_ANY = MIME_TYPE_IMAGE_PREFIX + "*";
    private static final String MIME_TYPE_ANY = "*/*";
    private static final String HTTP_HEADER_ACCEPT = "Accept";
    private static final String HTTP_HEADER_VARY = "Vary";
    protected static final String HTTP_HEADER_USER_AGENT = "User-Agent";

    private static final String FORMAT_JPEG = "image/jpeg";
    private static final String FORMAT_WBMP = "image/wbmp";
    private static final String FORMAT_GIF = "image/gif";
    private static final String FORMAT_PNG = "image/png";

    private final static String[] sKnownFormats = new String[] {
        FORMAT_JPEG, FORMAT_PNG, FORMAT_GIF, FORMAT_WBMP
    };
    private float[] mKnownFormatQuality = new float[] {
        1f, 1f, 0.99f, 0.5f
    };

    private HashMap<String, Float> mFormatQuality; // HashMap, as I need to clone this for each request
    private final Object mLock = new Object();

    /*
    private Pattern[] mKnownAgentPatterns;
    private String[] mKnownAgentAccpets;
    */
    {
        // Hack: Make sure the filter don't trigger all the time
        // See: super.trigger(ServletRequest)
        mTriggerParams = new String[] {};
    }

    /*
    public void setAcceptMappings(String pPropertiesFile) {
        // NOTE: Supposed to be:
        // <agent-name>=<reg-exp>
        // <agent-name>.accept=<http-accept-header>

        Properties mappings = new Properties();
        try {
            mappings.load(getServletContext().getResourceAsStream(pPropertiesFile));

            List patterns = new ArrayList();
            List accepts = new ArrayList();

            for (Iterator iterator = mappings.keySet().iterator(); iterator.hasNext();) {
                String agent = (String) iterator.next();
                if (agent.endsWith(".accept")) {
                    continue;
                }

                try {
                    patterns.add(Pattern.compile((String) mappings.get(agent)));

                    // TODO: Consider preparsing ACCEPT header??
                    accepts.add(mappings.get(agent + ".accept"));
                }
                catch (PatternSyntaxException e) {
                    log("Could not parse User-Agent identification for " + agent, e);
                }

                mKnownAgentPatterns = (Pattern[]) patterns.toArray(new Pattern[patterns.size()]);
                mKnownAgentAccpets = (String[]) accepts.toArray(new String[accepts.size()]);
            }
        }
        catch (IOException e) {
            log("Could not read accetp-mappings properties file: " + pPropertiesFile, e);
        }
    }
    */

    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
        // NOTE: super invokes trigger() and image specific doFilter() if needed
        super.doFilterImpl(pRequest, pResponse, pChain);

        if (pResponse instanceof HttpServletResponse) {
            // Update the Vary HTTP header field
            ((HttpServletResponse) pResponse).addHeader(HTTP_HEADER_VARY, HTTP_HEADER_ACCEPT);
            //((HttpServletResponse) pResponse).addHeader(HTTP_HEADER_VARY, HTTP_HEADER_USER_AGENT);
        }
    }

    /**
     * Makes sure the filter triggers for unknown file formats.
     *
     * @param pRequest the request
     * @return {@code true} if the filter should execute, {@code false}
     *         otherwise
     */
    protected boolean trigger(ServletRequest pRequest) {
        boolean trigger = false;

        if (pRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) pRequest;
            String accept = getAcceptedFormats(request);
            String originalFormat = getServletContext().getMimeType(request.getRequestURI());

            //System.out.println("Accept: " + accept);
            //System.out.println("Original format: " + originalFormat);

            // Only override original format if it is not accpeted by the client
            // Note: Only explicit matches are okay, */* or image/* is not.
            if (!StringUtil.contains(accept, originalFormat)) {
                trigger = true;
            }
        }

        // Call super, to allow content negotiation even though format is supported
        return trigger || super.trigger(pRequest);
    }

    private String getAcceptedFormats(HttpServletRequest pRequest) {
        return pRequest.getHeader(HTTP_HEADER_ACCEPT);
    }

    /*
    private String getAcceptedFormats(HttpServletRequest pRequest) {
        String accept = pRequest.getHeader(HTTP_HEADER_ACCEPT);

        // Check if User-Agent is in list of known agents
        if (mKnownAgentPatterns != null) {
            String agent = pRequest.getHeader(HTTP_HEADER_USER_AGENT);
            for (int i = 0; i < mKnownAgentPatterns.length; i++) {
                Pattern pattern = mKnownAgentPatterns[i];
                if (pattern.matcher(agent).matches()) {
                    // Merge known with real accpet, in case plugins add extra capabilities
                    accept = mergeAccept(mKnownAgentAccpets[i], accept);
                    System.out.println("--> User-Agent: " + agent + " accepts: " + accept);
                    return accept;
                }
            }
        }

        System.out.println("No agent match, defaulting to Accept header: " + accept);
        return accept;
    }

    private String mergeAccept(String pKnown, String pAccept) {
        // TODO: Make sure there are no duplicates...
        return  pKnown + ", " + pAccept;
    }
    */

    protected RenderedImage doFilter(BufferedImage pImage, ServletRequest pRequest, ImageServletResponse pResponse) throws IOException {
        if (pRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) pRequest;

            Map<String, Float> formatQuality = getFormatQualityMapping();

            // TODO: Consider adding original format, and use as fallback in worst case?
            // TODO: Original format should have some boost, to avoid unneccesary convertsion?

            // Update source quality settings from image properties
            adjustQualityFromImage(formatQuality, pImage);
            //System.out.println("Source quality mapping: " + formatQuality);

            adjustQualityFromAccept(formatQuality, request);
            //System.out.println("Final media scores: " + formatQuality);

            // Find the formats with the highest quality factor, and use the first (predictable)
            String acceptable = findBestFormat(formatQuality);

            //System.out.println("Acceptable: " + acceptable);

            // Send HTTP 406 Not Acceptable
            if (acceptable == null) {
                if (pResponse instanceof HttpServletResponse) {
                    ((HttpServletResponse) pResponse).sendError(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
                }
                return null;
            }
            else {
                // TODO: Only if the format was changed!
                // Let other filters/caches/proxies know we changed the image
            }

            // Set format
            pResponse.setOutputContentType(acceptable);
            //System.out.println("Set format: " + acceptable);
        }

        return pImage;
    }

    private Map<String, Float> getFormatQualityMapping() {
        synchronized(mLock) {
            if (mFormatQuality == null) {
                mFormatQuality = new HashMap<String, Float>();

                // Use ImageIO to find formats we can actually write
                String[] formats = ImageIO.getWriterMIMETypes();

                // All known formats qs are initially 1.0
                // Others should be 0.1 or something like that...
                for (String format : formats) {
                    mFormatQuality.put(format, getKnownFormatQuality(format));
                }
            }
        }
        //noinspection unchecked
        return (Map<String, Float>) mFormatQuality.clone();
    }

    /**
     * Finds the best available format.
     *
     * @param pFormatQuality the format to quality mapping
     * @return the mime type of the best available format
     */
    private static String findBestFormat(Map<String, Float> pFormatQuality) {
        String acceptable = null;
        float acceptQuality = 0.0f;
        for (Map.Entry<String, Float> entry : pFormatQuality.entrySet()) {
            float qValue = entry.getValue();
            if (qValue > acceptQuality) {
                acceptQuality = qValue;
                acceptable = entry.getKey();
            }
        }

        //System.out.println("Accepted format: " + acceptable);
        //System.out.println("Accepted quality: " + acceptQuality);
        return acceptable;
    }

    /**
     * Adjust quality from HTTP Accept header
     *
     * @param pFormatQuality the format to quality mapping
     * @param pRequest the request
     */
    private void adjustQualityFromAccept(Map<String, Float> pFormatQuality, HttpServletRequest pRequest) {
        // Multiply all q factors with qs factors
        // No q=.. should be interpreted as q=1.0

        // Apache does some extras; if both explicit types and wildcards
        // (without qaulity factor) are present, */* is interpreted as
        // */*;q=0.01 and image/* is interpreted as image/*;q=0.02
        // See: http://httpd.apache.org/docs-2.0/content-negotiation.html

        String accept = getAcceptedFormats(pRequest);
        //System.out.println("Accept: " + accept);

        float anyImageFactor = getQualityFactor(accept, MIME_TYPE_IMAGE_ANY);
        anyImageFactor = (anyImageFactor == 1) ? 0.02f : anyImageFactor;

        float anyFactor = getQualityFactor(accept, MIME_TYPE_ANY);
        anyFactor = (anyFactor == 1) ? 0.01f : anyFactor;

        for (String format : pFormatQuality.keySet()) {
            //System.out.println("Trying format: " + format);

            String formatMIME = MIME_TYPE_IMAGE_PREFIX + format;
            float qFactor = getQualityFactor(accept, formatMIME);
            qFactor = (qFactor == 0f) ? Math.max(anyFactor, anyImageFactor) : qFactor;
            adjustQuality(pFormatQuality, format, qFactor);
        }
    }

    /**
     *
     * @param pAccept the accpet header value
     * @param pContentType the content type to get the quality factor for
     * @return the q factor of the given format, according to the accept header
     */
    private static float getQualityFactor(String pAccept, String pContentType) {
        float qFactor = 0;
        int foundIndex = pAccept.indexOf(pContentType);
        if (foundIndex >= 0) {
            int startQIndex = foundIndex + pContentType.length();
            if (startQIndex < pAccept.length() && pAccept.charAt(startQIndex) == ';') {
                while (startQIndex < pAccept.length() && pAccept.charAt(startQIndex++) == ' ') {
                    // Skip over whitespace
                }

                if (pAccept.charAt(startQIndex++) == 'q' && pAccept.charAt(startQIndex++) == '=') {
                    int endQIndex = pAccept.indexOf(',', startQIndex);
                    if (endQIndex < 0) {
                        endQIndex = pAccept.length();
                    }

                    try {
                        qFactor = Float.parseFloat(pAccept.substring(startQIndex, endQIndex));
                        //System.out.println("Found qFactor " + qFactor);
                    }
                    catch (NumberFormatException e) {
                        // TODO: Determine what to do here.. Maybe use a very low value?
                        // Ahem.. The specs don't say anything about how to interpret a wrong q factor..
                        //System.out.println("Unparseable q setting; " + e.getMessage());
                    }
                }
                // TODO: Determine what to do here.. Maybe use a very low value?
                // Unparseable q value, use 0
            }
            else {
                // Else, assume quality is 1.0
                qFactor = 1;
            }
        }
        return qFactor;
    }


    /**
     * Adjusts source quality settings from image properties.
     *
     * @param pFormatQuality the format to quality mapping
     * @param pImage the image
     */
    private static void adjustQualityFromImage(Map<String, Float> pFormatQuality, BufferedImage pImage) {
        // NOTE: The values are all made-up. May need tuning.

        // If pImage.getColorModel() instanceof IndexColorModel
        //    JPEG qs*=0.6
        //    If NOT binary or 2 color index
        //        WBMP qs*=0.5
        // Else
        //    GIF qs*=0.02
        //    PNG qs*=0.9 // JPEG is smaller/faster
        if (pImage.getColorModel() instanceof IndexColorModel) {
            adjustQuality(pFormatQuality, FORMAT_JPEG, 0.6f);

            if (pImage.getType() != BufferedImage.TYPE_BYTE_BINARY ||  ((IndexColorModel) pImage.getColorModel()).getMapSize() != 2) {
                adjustQuality(pFormatQuality, FORMAT_WBMP, 0.5f);
            }
        }
        else {
            adjustQuality(pFormatQuality, FORMAT_GIF, 0.01f);
            adjustQuality(pFormatQuality, FORMAT_PNG, 0.99f); // JPEG is smaller/faster
        }

        // If pImage.getColorModel().hasTransparentPixels()
        //    JPEG qs*=0.05
        //    WBMP qs*=0.05
        //    If NOT transparency == BITMASK
        //        GIF qs*=0.8
        if (ImageUtil.hasTransparentPixels(pImage, true)) {
            adjustQuality(pFormatQuality, FORMAT_JPEG, 0.009f);
            adjustQuality(pFormatQuality, FORMAT_WBMP, 0.009f);

            if (pImage.getColorModel().getTransparency() != Transparency.BITMASK) {
                adjustQuality(pFormatQuality, FORMAT_GIF, 0.8f);
            }
        }
    }

    /**
     * Updates the quality in the map.
     *
     * @param pFormatQuality Map<String,Float>
     * @param pFormat the format
     * @param pFactor the quality factor
     */
    private static void adjustQuality(Map<String, Float> pFormatQuality, String pFormat, float pFactor) {
        Float oldValue = pFormatQuality.get(pFormat);
        if (oldValue != null) {
            pFormatQuality.put(pFormat, oldValue * pFactor);
            //System.out.println("New vallue after multiplying with " + pFactor + " is " + pFormatQuality.get(pFormat));
        }
    }


    /**
     * Gets the initial quality if this is a known format, otherwise 0.1
     *
     * @param pFormat the format name
     * @return the q factor of the given format
     */
    private float getKnownFormatQuality(String pFormat) {
        for (int i = 0; i < sKnownFormats.length; i++) {
            if (pFormat.equals(sKnownFormats[i])) {
                return mKnownFormatQuality[i];
            }
        }
        return 0.1f;
    }
}
