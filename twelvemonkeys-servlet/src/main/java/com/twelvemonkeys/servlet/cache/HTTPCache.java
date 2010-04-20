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

package com.twelvemonkeys.servlet.cache;

import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.net.MIMEUtil;
import com.twelvemonkeys.net.NetUtil;
import com.twelvemonkeys.util.LRUHashMap;
import com.twelvemonkeys.util.LinkedMap;
import com.twelvemonkeys.util.NullMap;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A "simple" HTTP cache.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/cache/HTTPCache.java#4 $
 * @todo OMPTIMIZE: Cache parsed vary-info objects, not the properties-files
 * @todo BUG: Better filename handling, as some filenames become too long..
 * - Use a mix of parameters and hashcode + lenght with fixed (max) lenght?
 * (Hashcodes of Strings are constant).
 * - Store full filenames in .vary, instead of just extension, and use
 * short filenames? (and only one .vary per dir).
 * <p/>
 * <!-- TODO: Fix the caching algortim
 * Squid-2 algorithm
 * <p/>
 * For Squid-2 the refresh algorithm has been slightly modified to give the EXPIRES value a higher precedence, and the CONF_MIN value lower precedence:
 * <p/>
 * if (EXPIRES) {
 * if (EXPIRES <= NOW)
 * return STALE
 * else
 * return FRESH
 * }
 * if (CLIENT_MAX_AGE)
 * if (OBJ_AGE > CLIENT_MAX_AGE)
 * return STALE
 * if (OBJ_AGE > CONF_MAX)
 * return STALE
 * if (OBJ_DATE > OBJ_LASTMOD) {
 * if (LM_FACTOR < CONF_PERCENT)
 * return FRESH
 * else
 * return STALE
 * }
 * if (OBJ_AGE <= CONF_MIN)
 * return FRESH
 * return STALE
 * -->
 * @todo TEST: Battle-testing using some URL-hammer tool and maybe a profiler
 * @todo ETag/Conditional (If-None-Match) support!
 * @todo Rewrite to use java.util.concurrent Locks (if possible) for performance
 *       Maybe use ConcurrentHashMap instead fo synchronized HashMap?
 * @todo Rewrite to use NIO for performance
 * @todo Allow no tempdir for in-memory only cache
 * @todo Specify max size of disk-cache
 */
public class HTTPCache {
    /**
     * The HTTP header {@code "Cache-Control"}
     */
    protected static final String HEADER_CACHE_CONTROL = "Cache-Control";
    /**
     * The HTTP header {@code "Content-Type"}
     */
    protected static final String HEADER_CONTENT_TYPE = "Content-Type";
    /**
     * The HTTP header {@code "Date"}
     */
    protected static final String HEADER_DATE = "Date";
    /**
     * The HTTP header {@code "ETag"}
     */
    protected static final String HEADER_ETAG = "ETag";
    /**
     * The HTTP header {@code "Expires"}
     */
    protected static final String HEADER_EXPIRES = "Expires";
    /**
     * The HTTP header {@code "If-Modified-Since"}
     */
    protected static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    /**
     * The HTTP header {@code "If-None-Match"}
     */
    protected static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    /**
     * The HTTP header {@code "Last-Modified"}
     */
    protected static final String HEADER_LAST_MODIFIED = "Last-Modified";
    /**
     * The HTTP header {@code "Pragma"}
     */
    protected static final String HEADER_PRAGMA = "Pragma";
    /**
     * The HTTP header {@code "Vary"}
     */
    protected static final String HEADER_VARY = "Vary";
    /**
     * The HTTP header {@code "Warning"}
     */
    protected static final String HEADER_WARNING = "Warning";
    /**
     * HTTP extension header {@code "X-Cached-At"}
     */
    protected static final String HEADER_CACHED_TIME = "X-Cached-At";

    /**
     * The file extension for header files ({@code ".headers"})
     */
    protected static final String FILE_EXT_HEADERS = ".headers";
    /**
     * The file extension for varation-info files ({@code ".vary"})
     */
    protected static final String FILE_EXT_VARY = ".vary";

    protected static final int STATUS_OK = 200;

    /**
     * The directory used for the disk-based cache
     */
    private File mTempDir;

    /**
     * Indicates wether the disk-based cache should be deleted when the
     * container shuts down/VM exits
     */
    private boolean mDeleteCacheOnExit;

    /**
     * In-memory content cache
     */
    private final Map<String, CachedResponse> mContentCache;
    /**
     * In-memory enity cache
     */
    private final Map<String, CachedEntity> mEntityCache;
    /**
     * In-memory varyiation-info cache
     */
    private final Map<String, Properties> mVaryCache;

    private long mDefaultExpiryTime = -1;

    private final Logger mLogger;

    // Internal constructor for sublcasses only
    protected HTTPCache(
            final File pTempFolder,
            final long pDefaultCacheExpiryTime,
            final int pMaxMemCacheSize,
            final int pMaxCachedEntites,
            final boolean pDeleteCacheOnExit,
            final Logger pLogger
    ) {
        if (pTempFolder == null) {
            throw new IllegalArgumentException("temp folder == null");
        }
        if (!pTempFolder.exists() && !pTempFolder.mkdirs()) {
            throw new IllegalArgumentException("Could not create required temp directory: " + mTempDir.getAbsolutePath());
        }
        if (!(pTempFolder.canRead() && pTempFolder.canWrite())) {
            throw new IllegalArgumentException("Must have read/write access to temp folder: " + mTempDir.getAbsolutePath());
        }
        if (pDefaultCacheExpiryTime < 0) {
            throw new IllegalArgumentException("Negative expiry time");
        }
        if (pMaxMemCacheSize < 0) {
            throw new IllegalArgumentException("Negative maximum memory cache size");
        }
        if (pMaxCachedEntites < 0) {
            throw new IllegalArgumentException("Negative maximum number of cached entries");
        }

        mDefaultExpiryTime = pDefaultCacheExpiryTime;

        if (pMaxMemCacheSize > 0) {
//            Map backing = new SizedLRUMap(pMaxMemCacheSize); // size in bytes
//            mContentCache = new TimeoutMap(backing, null, pDefaultCacheExpiryTime);
            mContentCache = new SizedLRUMap<String, CachedResponse>(pMaxMemCacheSize); // size in bytes
        }
        else {
            mContentCache = new NullMap<String, CachedResponse>();
        }

        mEntityCache = new LRUHashMap<String, CachedEntity>(pMaxCachedEntites);
        mVaryCache = new LRUHashMap<String, Properties>(pMaxCachedEntites);

        mDeleteCacheOnExit = pDeleteCacheOnExit;

        mTempDir = pTempFolder;

        mLogger = pLogger != null ? pLogger : Logger.getLogger(getClass().getName());
    }

    /**
     * Creates an {@code HTTPCache}.
     *
     * @param pTempFolder             the temp folder for this cache.
     * @param pDefaultCacheExpiryTime Default expiry time for cached entities,
     *                                {@code &gt;= 0}
     * @param pMaxMemCacheSize        Maximum size of in-memory cache for content
     *                                <em>in bytes</em>, {@code &gt;= 0} ({@code 0} means no
     *                                in-memory cache)
     * @param pMaxCachedEntites       Maximum number of entities in cache
     * @param pDeleteCacheOnExit      specifies wether the file cache should be
     *                                deleted when the application or VM shuts down
     * @throws IllegalArgumentException if {@code pName} or {@code pContext} is
     *                                  {@code null} or if any of {@code pDefaultCacheExpiryTime},
     *                                  {@code pMaxMemCacheSize} or {@code pMaxCachedEntites} are
     *                                  negative,
     *                                  or if the directory as given in the context attribute
     *                                  {@code "javax.servlet.context.tempdir"} does not exist, and
     *                                  cannot be created.
     */
    public HTTPCache(final File pTempFolder,
                        final long pDefaultCacheExpiryTime,
                        final int pMaxMemCacheSize, final int pMaxCachedEntites,
                        final boolean pDeleteCacheOnExit) {
        this(pTempFolder, pDefaultCacheExpiryTime, pMaxMemCacheSize, pMaxCachedEntites, pDeleteCacheOnExit, null);
    }


    /**
     * Creates an {@code HTTPCache}.
     *
     * @param pName                   Name of this cache (should be unique per application).
     *                                Used for temp folder
     * @param pContext                Servlet context for the application.
     * @param pDefaultCacheExpiryTime Default expiry time for cached entities,
     *                                {@code &gt;= 0}
     * @param pMaxMemCacheSize        Maximum size of in-memory cache for content
     *                                <em>in bytes</em>, {@code &gt;= 0} ({@code 0} means no
     *                                in-memory cache)
     * @param pMaxCachedEntites       Maximum number of entities in cache
     * @param pDeleteCacheOnExit      specifies wether the file cache should be
     *                                deleted when the application or VM shuts down
     * @throws IllegalArgumentException if {@code pName} or {@code pContext} is
     *                                  {@code null} or if any of {@code pDefaultCacheExpiryTime},
     *                                  {@code pMaxMemCacheSize} or {@code pMaxCachedEntites} are
     *                                  negative,
     *                                  or if the directory as given in the context attribute
     *                                  {@code "javax.servlet.context.tempdir"} does not exist, and
     *                                  cannot be created.
     * @deprecated Use {@link #HTTPCache(File, long, int, int, boolean)} instead.
     */
    public HTTPCache(final String pName, final ServletContext pContext,
            final int pDefaultCacheExpiryTime, final int pMaxMemCacheSize,
            final int pMaxCachedEntites, final boolean pDeleteCacheOnExit) {
        this(
                getTempFolder(pName, pContext),
                pDefaultCacheExpiryTime, pMaxMemCacheSize, pMaxCachedEntites, pDeleteCacheOnExit,
                new CacheFilter.ServletContextLoggerAdapter(pName, pContext)
        );
    }

    private static File getTempFolder(String pName, ServletContext pContext) {
        if (pName == null) {
            throw new IllegalArgumentException("name == null");
        }
        if (pName.trim().length() == 0) {
            throw new IllegalArgumentException("Empty name");
        }
        if (pContext == null) {
            throw new IllegalArgumentException("servlet context == null");
        }
        File tempRoot = (File) pContext.getAttribute("javax.servlet.context.tempdir");
        if (tempRoot == null) {
            throw new IllegalStateException("Missing context attribute \"javax.servlet.context.tempdir\"");
        }
        return new File(tempRoot, pName);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName());
        buf.append("[");
        buf.append("Temp dir: ");
        buf.append(mTempDir.getAbsolutePath());
        if (mDeleteCacheOnExit) {
            buf.append(" (non-persistent)");
        }
        else {
            buf.append(" (persistent)");            
        }
        buf.append(", EntityCache: {");
        buf.append(mEntityCache.size());
        buf.append(" entries in a ");
        buf.append(mEntityCache.getClass().getName());
        buf.append("}, VaryCache: {");
        buf.append(mVaryCache.size());
        buf.append(" entries in a ");
        buf.append(mVaryCache.getClass().getName());
        buf.append("}, ContentCache: {");
        buf.append(mContentCache.size());
        buf.append(" entries in a ");
        buf.append(mContentCache.getClass().getName());
        buf.append("}]");

        return buf.toString();
    }

    void log(final String pMessage) {
        mLogger.log(Level.INFO, pMessage);
    }

    void log(final String pMessage, Throwable pException) {
        mLogger.log(Level.WARNING, pMessage, pException);
    }

    /**
     * Looks up the {@code CachedEntity} for the given request.
     *
     * @param pRequest the request
     * @param pResponse the response
     * @param pResolver the resolver
     * @throws java.io.IOException if an I/O error occurs
     * @throws CacheException if the cached entity can't be resolved for some reason
     */
    public void doCached(final CacheRequest pRequest, final CacheResponse pResponse, final ResponseResolver pResolver) throws IOException, CacheException {
        // TODO: Expire cached items on PUT/POST/DELETE/PURGE
        // If not cachable request, resolve directly
        if (!isCacheable(pRequest)) {
            pResolver.resolve(pRequest, pResponse);
        }
        else {
            // Generate cacheURI
            String cacheURI = generateCacheURI(pRequest);
//             System.out.println(" ## HTTPCache ## Request Id (cacheURI): " + cacheURI);

            // Get/create cached entity
            CachedEntity cached;
            synchronized (mEntityCache) {
                cached = mEntityCache.get(cacheURI);
                if (cached == null) {
                    cached = new CachedEntityImpl(cacheURI, this);
                    mEntityCache.put(cacheURI, cached);
                }
            }


            // else if (not cached || stale), resolve through wrapped (caching) response
            // else render to response

            // TODO: This is a bottleneck for uncachable resources. Should not
            // synchronize, if we know (HOW?) the resource is not cachable.
            synchronized (cached) {
                if (cached.isStale(pRequest) /* TODO: NOT CACHED?! */) {
                    // Go fetch...
                    WritableCachedResponse cachedResponse = cached.createCachedResponse();
                    pResolver.resolve(pRequest, cachedResponse);

                    if (isCachable(cachedResponse)) {
//                        System.out.println("Registering content: " + cachedResponse.getCachedResponse());
                        registerContent(cacheURI, pRequest, cachedResponse.getCachedResponse());
                    }
                    else {
                        // TODO: What about non-cachable responses? We need to either remove them from cache, or mark them as stale...
                        // Best is probably to mark as non-cacheable for later, and NOT store content (performance)
//                        System.out.println("Non-cacheable response: " + cachedResponse);

                        // TODO: Write, but should really do this unbuffered.... And some resolver might be able to do just that?
                        // Might need a resolver.isWriteThroughForUncachableResources() method...
                        pResponse.setStatus(cachedResponse.getStatus());
                        cachedResponse.writeHeadersTo(pResponse);
                        cachedResponse.writeContentsTo(pResponse.getOutputStream());
                        return;
                    }
                }
            }

            cached.render(pRequest, pResponse);
        }
    }

    protected void invalidate(CacheRequest pRequest) {
        // Generate cacheURI
        String cacheURI = generateCacheURI(pRequest);

        // Get/create cached entity
        CachedEntity cached;
        synchronized (mEntityCache) {
            cached = mEntityCache.get(cacheURI);
            if (cached != null) {
                // TODO; Remove all variants
                mEntityCache.remove(cacheURI);
            }
        }

    }

    private boolean isCacheable(final CacheRequest pRequest) {
        // TODO: Support public/private cache (a cache probably have to be one of the two, when created)
        // TODO: Only private caches should cache requests with Authorization

        // TODO: OptimizeMe!
        // It's probably best to cache the "cacheableness" of a request and a resource separately
        List<String> cacheControlValues = pRequest.getHeaders().get(HEADER_CACHE_CONTROL);
        if (cacheControlValues != null) {
            Map<String, String> cacheControl = new HashMap<String, String>();
            for (String cc : cacheControlValues) {
                List<String> directives = Arrays.asList(cc.split(","));
                for (String directive : directives) {
                    directive = directive.trim();
                    if (directive.length() > 0) {
                        String[] directiveParts = directive.split("=", 2);
                        cacheControl.put(directiveParts[0], directiveParts.length > 1 ? directiveParts[1] : null);
                    }
                }
            }

            if (cacheControl.containsKey("no-cache") || cacheControl.containsKey("no-store")) {
                return false;
            }

            /*
            "no-cache"                          ; Section 14.9.1
         | "no-store"                          ; Section 14.9.2
         | "max-age" "=" delta-seconds         ; Section 14.9.3, 14.9.4
         | "max-stale" [ "=" delta-seconds ]   ; Section 14.9.3
         | "min-fresh" "=" delta-seconds       ; Section 14.9.3
         | "no-transform"                      ; Section 14.9.5
         | "only-if-cached"
             */
        }

        return true;
    }

    private boolean isCachable(final CacheResponse pResponse) {
        if (pResponse.getStatus() != STATUS_OK) {
            return false;
        }

        // Vary: *
        List<String> values = pResponse.getHeaders().get(HTTPCache.HEADER_VARY);
        if (values != null) {
            for (String value : values) {
                if ("*".equals(value)) {
                    return false;
                }
            }
        }

        // Cache-Control: no-cache, no-store, must-revalidate
        values = pResponse.getHeaders().get(HTTPCache.HEADER_CACHE_CONTROL);
        if (values != null) {
            for (String value : values) {
                if (StringUtil.contains(value, "no-cache")
                        || StringUtil.contains(value, "no-store")
                        || StringUtil.contains(value, "must-revalidate")) {
                    return false;
                }
            }
        }

        // Pragma: no-cache
        values = pResponse.getHeaders().get(HTTPCache.HEADER_PRAGMA);
        if (values != null) {
            for (String value : values) {
                if (StringUtil.contains(value, "no-cache")) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Allows a server-side cache mechanism to peek at the real file.
     * Default implementation return {@code null}.
     *
     * @param pRequest the request
     * @return {@code null}, always
     */
    protected File getRealFile(final CacheRequest pRequest) {
        // TODO: Create callback for this? Only possible for server-side cache... Maybe we can get away without this?
        // For now: Default implementation that returns null
        return null;
/*
        String contextRelativeURI = ServletUtil.getContextRelativeURI(pRequest);
        // System.out.println(" ## HTTPCache ## Context relative URI: " + contextRelativeURI);

        String path = mContext.getRealPath(contextRelativeURI);
        // System.out.println(" ## HTTPCache ## Real path: " + path);

        if (path != null) {
            return new File(path);
        }

        return null;
*/
    }

    private File getCachedFile(final String pCacheURI, final CacheRequest pRequest) {
        File file = null;

        // Get base dir
        File base = new File(mTempDir, "./" + pCacheURI);
        final String basePath = base.getAbsolutePath();
        File directory = base.getParentFile();

        // Get list of files that are candidates
        File[] candidates = directory.listFiles(new FileFilter() {
            public boolean accept(File pFile) {
                return pFile.getAbsolutePath().startsWith(basePath)
                        && !pFile.getName().endsWith(FILE_EXT_HEADERS)
                        && !pFile.getName().endsWith(FILE_EXT_VARY);
            }
        });

        // Negotiation
        if (candidates != null) {
            String extension = getVaryExtension(pCacheURI, pRequest);
            //System.out.println("-- Vary ext: " + extension);
            if (extension != null) {
                for (File candidate : candidates) {
                    //System.out.println("-- Candidate: " + candidates[i]);

                    if (extension.equals("ANY") || extension.equals(FileUtil.getExtension(candidate))) {
                        //System.out.println("-- Candidate selected");
                        file = candidate;
                        break;
                    }
                }
            }
        }
        else if (base.exists()) {
            //System.out.println("-- File not a directory: " + directory);
            log("File not a directory: " + directory);
        }

        return file;
    }

    private String getVaryExtension(final String pCacheURI, final CacheRequest pRequest) {
        Properties variations = getVaryProperties(pCacheURI);

        String[] varyHeaders = StringUtil.toStringArray(variations.getProperty(HEADER_VARY, ""));
//        System.out.println("-- Vary: \"" + variations.getProperty(HEADER_VARY) + "\"");

        String varyKey = createVaryKey(varyHeaders, pRequest);
//        System.out.println("-- Vary key: \"" + varyKey + "\"");

        // If no vary, just go with any version...
        return StringUtil.isEmpty(varyKey) ? "ANY" : variations.getProperty(varyKey, null);
    }

    private String createVaryKey(final String[] pVaryHeaders, final CacheRequest pRequest) {
        if (pVaryHeaders == null) {
            return null;
        }

        StringBuilder headerValues = new StringBuilder();
        for (String varyHeader : pVaryHeaders) {
            List<String> varies = pRequest.getHeaders().get(varyHeader);
            String headerValue = varies != null && varies.size() > 0 ? varies.get(0) : null;

            headerValues.append(varyHeader);
            headerValues.append("__V_");
            headerValues.append(createSafeHeader(headerValue));
        }

        return headerValues.toString();
    }

    private void storeVaryProperties(final String pCacheURI, final Properties pVariations) {
        synchronized (pVariations) {
            try {
                File file = getVaryPropertiesFile(pCacheURI);
                if (!file.exists() && mDeleteCacheOnExit) {
                    file.deleteOnExit();
                }

                FileOutputStream out = new FileOutputStream(file);
                try {
                    pVariations.store(out, pCacheURI + " Vary info");
                }
                finally {
                    out.close();
                }
            }
            catch (IOException ioe) {
                log("Error: Could not store Vary info: " + ioe);
            }
        }
    }

    private Properties getVaryProperties(final String pCacheURI) {
        Properties variations;

        synchronized (mVaryCache) {
            variations = mVaryCache.get(pCacheURI);
            if (variations == null) {
                variations = loadVaryProperties(pCacheURI);
                mVaryCache.put(pCacheURI, variations);
            }
        }

        return variations;
    }

    private Properties loadVaryProperties(final String pCacheURI) {
        // Read Vary info, for content negotiation
        Properties variations = new Properties();
        File vary = getVaryPropertiesFile(pCacheURI);
        if (vary.exists()) {
            try {
                FileInputStream in = new FileInputStream(vary);
                try {
                    variations.load(in);
                }
                finally {
                    in.close();
                }
            }
            catch (IOException ioe) {
                log("Error: Could not load Vary info: " + ioe);
            }
        }
        return variations;
    }

    private File getVaryPropertiesFile(final String pCacheURI) {
        return new File(mTempDir, "./" + pCacheURI + FILE_EXT_VARY);
    }

    private static String generateCacheURI(final CacheRequest pRequest) {
        StringBuilder buffer = new StringBuilder();

        // Note: As the '/'s are not replaced, the directory structure will be recreated
        // TODO: Old mehtod relied on context relativization, that must now be handled byt the ServletCacheRequest
//        String contextRelativeURI = ServletUtil.getContextRelativeURI(pRequest);
        String contextRelativeURI = pRequest.getRequestURI().getPath();
        buffer.append(contextRelativeURI);

        // Create directory for all resources
        if (contextRelativeURI.charAt(contextRelativeURI.length() - 1) != '/') {
            buffer.append('/');
        }

        // Get parameters from request, and recreate query to avoid unneccessary
        // regeneration/caching when parameters are out of order
        // Also makes caching work for POST
        appendSortedRequestParams(pRequest, buffer);

        return buffer.toString();
    }

    private static void appendSortedRequestParams(final CacheRequest pRequest, final StringBuilder pBuffer) {
        Set<String> names = pRequest.getParameters().keySet();
        if (names.isEmpty()) {
            pBuffer.append("defaultVersion");
            return;
        }

        // We now have parameters
        pBuffer.append('_'); // append '_' for '?', to avoid clash with default

        // Create a sorted map
        SortedMap<String, List<String>> sortedQueryMap = new TreeMap<String, List<String>>();
        for (String name : names) {
            List<String> values = pRequest.getParameters().get(name);

            sortedQueryMap.put(name, values);
        }

        // Iterate over sorted map, and append to stringbuffer
        for (Iterator<Map.Entry<String,List<String>>> iterator = sortedQueryMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String,List<String>> entry = iterator.next();
            pBuffer.append(createSafe(entry.getKey()));

            List<String> values = entry.getValue();
            if (values != null && values.size() > 0) {
                pBuffer.append("_V"); // =
                for (int i = 0; i < values.size(); i++) {
                    String value = values.get(i);
                    if (i != 0) {
                        pBuffer.append(',');
                    }
                    pBuffer.append(createSafe(value));
                }
            }

            if (iterator.hasNext()) {
                pBuffer.append("_P"); // &
            }
        }
    }

    private static String createSafe(final String pKey) {
        return pKey.replace('/', '-')
                .replace('&', '-') // In case they are encoded
                .replace('#', '-')
                .replace(';', '-');
    }

    private static String createSafeHeader(final String pHeaderValue) {
        if (pHeaderValue == null) {
            return "NULL";
        }

        return pHeaderValue.replace(' ', '_')
                .replace(':', '_')
                .replace('=', '_');
    }

    /**
     * Registers content for the given URI in the cache.
     *
     * @param pCacheURI       the cache URI
     * @param pRequest        the request
     * @param pCachedResponse the cached response
     * @throws IOException if the content could not be cached
     */
    void registerContent(
            final String pCacheURI,
            final CacheRequest pRequest,
            final CachedResponse pCachedResponse
    ) throws IOException {
        // System.out.println(" ## HTTPCache ## Registering content for " + pCacheURI);

//        pRequest.removeAttribute(ATTRIB_IS_STALE);
//        pRequest.setAttribute(ATTRIB_CACHED_RESPONSE, pCachedResponse);

        if ("HEAD".equals(pRequest.getMethod())) {
            // System.out.println(" ## HTTPCache ## Was HEAD request, will NOT store content.");
            return;
        }

        // TODO: Several resources may have same extension...
        String extension = MIMEUtil.getExtension(pCachedResponse.getHeaderValue(HEADER_CONTENT_TYPE));
        if (extension == null) {
            extension = "[NULL]";
        }

        synchronized (mContentCache) {
            mContentCache.put(pCacheURI + '.' + extension, pCachedResponse);

            // This will be the default version
            if (!mContentCache.containsKey(pCacheURI)) {
                mContentCache.put(pCacheURI, pCachedResponse);
            }
        }

        // Write the cached content to disk
        File content = new File(mTempDir, "./" + pCacheURI + '.' + extension);
        if (mDeleteCacheOnExit && !content.exists()) {
            content.deleteOnExit();
        }

        File parent = content.getParentFile();
        if (!(parent.exists() || parent.mkdirs())) {
            log("Could not create directory " + parent.getAbsolutePath());

            // TODO: Make sure vary-info is still created in memory

            return;
        }

        OutputStream mContentStream = new BufferedOutputStream(new FileOutputStream(content));

        try {
            pCachedResponse.writeContentsTo(mContentStream);
        }
        finally {
            try {
                mContentStream.close();
            }
            catch (IOException e) {
                log("Error closing content stream: " + e.getMessage(), e);
            }
        }

        // Write the cached headers to disk (in pseudo-properties-format)
        File headers = new File(content.getAbsolutePath() + FILE_EXT_HEADERS);
        if (mDeleteCacheOnExit && !headers.exists()) {
            headers.deleteOnExit();
        }

        FileWriter writer = new FileWriter(headers);
        PrintWriter headerWriter = new PrintWriter(writer);
        try {
            String[] names = pCachedResponse.getHeaderNames();

            for (String name : names) {
                String[] values = pCachedResponse.getHeaderValues(name);

                headerWriter.print(name);
                headerWriter.print(": ");
                headerWriter.println(StringUtil.toCSVString(values, "\\"));
            }
        }
        finally {
            headerWriter.flush();
            try {
                writer.close();
            }
            catch (IOException e) {
                log("Error closing header stream: " + e.getMessage(), e);
            }
        }

        // TODO: Make this more robust, if some weird entity is not
        // consistent in it's vary-headers..
        // (sometimes Vary, sometimes not, or somtimes different Vary headers).

        // Write extra Vary info to disk
        String[] varyHeaders = pCachedResponse.getHeaderValues(HEADER_VARY);

        // If no variations, then don't store vary info
        if (varyHeaders != null && varyHeaders.length > 0) {
            Properties variations = getVaryProperties(pCacheURI);

            String vary = StringUtil.toCSVString(varyHeaders);
            variations.setProperty(HEADER_VARY, vary);

            // Create Vary-key and map to file extension...
            String varyKey = createVaryKey(varyHeaders, pRequest);
//            System.out.println("varyKey: " + varyKey);
//            System.out.println("extension: " + extension);
            variations.setProperty(varyKey, extension);

            storeVaryProperties(pCacheURI, variations);
        }
    }

    /**
     * @param pCacheURI the cache URI
     * @param pRequest  the request
     * @return a {@code CachedResponse} object
     */
    CachedResponse getContent(final String pCacheURI, final CacheRequest pRequest) {
//         System.err.println(" ## HTTPCache ## Looking up content for " + pCacheURI);
//        Thread.dumpStack();

        String extension = getVaryExtension(pCacheURI, pRequest);

        CachedResponse response;
        synchronized (mContentCache) {
//             System.out.println(" ## HTTPCache ## Looking up content with ext: \"" + extension + "\" from memory cache (" + mContentCache /*.size()*/ + " entries)...");
            if ("ANY".equals(extension)) {
                response = mContentCache.get(pCacheURI);
            }
            else {
                response = mContentCache.get(pCacheURI + '.' + extension);
            }

            if (response == null) {
//                 System.out.println(" ## HTTPCache ## Content not found in memory cache.");
//
//                 System.out.println(" ## HTTPCache ## Looking up content from disk cache...");
                // Read from disk-cache
                response = readFromDiskCache(pCacheURI, pRequest);
            }

//            if (response == null) {
//                System.out.println(" ## HTTPCache ## Content not found in disk cache.");
//            }
//            else {
//                System.out.println(" ## HTTPCache ## Content for " + pCacheURI + " found: " + response);
//            }
        }

        return response;
    }

    private CachedResponse readFromDiskCache(String pCacheURI, CacheRequest pRequest) {
        CachedResponse response = null;
        try {
            File content = getCachedFile(pCacheURI, pRequest);
            if (content != null && content.exists()) {
                // Read contents
                byte[] contents = FileUtil.read(content);

                // Read headers
                File headers = new File(content.getAbsolutePath() + FILE_EXT_HEADERS);
                int headerSize = (int) headers.length();

                BufferedReader reader = new BufferedReader(new FileReader(headers));
                LinkedMap<String, List<String>> headerMap = new LinkedMap<String, List<String>>();
                String line;
                while ((line = reader.readLine()) != null) {
                    int colIdx = line.indexOf(':');
                    String name;
                    String value;
                    if (colIdx >= 0) {
                        name = line.substring(0, colIdx);
                        value = line.substring(colIdx + 2); // ": "
                    }
                    else {
                        name = line;
                        value = "";
                    }

                    headerMap.put(name, Arrays.asList(StringUtil.toStringArray(value, "\\")));
                }

                response = new CachedResponseImpl(STATUS_OK, headerMap, headerSize, contents);
                mContentCache.put(pCacheURI + '.' + FileUtil.getExtension(content), response);
            }
        }
        catch (IOException e) {
            log("Error reading from cache: " + e.getMessage(), e);
        }
        return response;
    }

    boolean isContentStale(final String pCacheURI, final CacheRequest pRequest) {
        // NOTE: Content is either stale or not, for the duration of one request, unless re-fetched
        // Means that we must retry after a registerContent(), if caching as request-attribute
        Boolean stale;
//        stale = (Boolean) pRequest.getAttribute(ATTRIB_IS_STALE);
//        if (stale != null) {
//            return stale;
//        }

        stale = isContentStaleImpl(pCacheURI, pRequest);
//        pRequest.setAttribute(ATTRIB_IS_STALE, stale);

        return stale;
    }

    private boolean isContentStaleImpl(final String pCacheURI, final CacheRequest pRequest) {
        CachedResponse response = getContent(pCacheURI, pRequest);

        if (response == null) {
            // System.out.println(" ## HTTPCache ## Content is stale (no content).");
            return true;
        }

        // TODO: Get max-age=... from REQUEST too!

        // TODO: What about time skew? Now should be (roughly) same as:
        // long now = pRequest.getDateHeader("Date");
        // TODO: If the time differs (server "now" vs client "now"), should we
        // take that into consideration when testing for stale content?
        // Probably, yes.
        // TODO: Define rules for how to handle time skews

        // Set timestamp check
        // NOTE: HTTP Dates are always in GMT time zone
        long now = (System.currentTimeMillis() / 1000L) * 1000L;
        long expires = getDateHeader(response.getHeaderValue(HEADER_EXPIRES));
        //long lastModified = getDateHeader(response, HEADER_LAST_MODIFIED);
        long lastModified = getDateHeader(response.getHeaderValue(HEADER_CACHED_TIME));

        // If expires header is not set, compute it
        if (expires == -1L) {
            /*
            // Note: Not all content has Last-Modified header. We should then
            // use lastModified() of the cached file, to compute expires time.
            if (lastModified == -1L) {
                File cached = getCachedFile(pCacheURI, pRequest);
                if (cached != null && cached.exists()) {
                    lastModified = cached.lastModified();
                    //// System.out.println(" ## HTTPCache ## Last-Modified is " + NetUtil.formatHTTPDate(lastModified) + ", using cachedFile.lastModified()");
                }
            }
            */

            // If Cache-Control: max-age is present, use it, otherwise default
            int maxAge = getIntHeader(response, HEADER_CACHE_CONTROL, "max-age");
            if (maxAge == -1) {
                expires = lastModified + mDefaultExpiryTime;
                //// System.out.println(" ## HTTPCache ## Expires is " + NetUtil.formatHTTPDate(expires) + ", using lastModified + defaultExpiry");
            }
            else {
                expires = lastModified + (maxAge * 1000L); // max-age is seconds
                //// System.out.println(" ## HTTPCache ## Expires is " + NetUtil.formatHTTPDate(expires) + ", using lastModified + maxAge");
            }
        }
        /*
        else {
            // System.out.println(" ## HTTPCache ## Expires header is " + response.getHeaderValue(HEADER_EXPIRES));
        }
        */

        // Expired?
        if (expires < now) {
            // System.out.println(" ## HTTPCache ## Content is stale (content expired: "
            //        + NetUtil.formatHTTPDate(expires) + " before " + NetUtil.formatHTTPDate(now) + ").");
            return true;
        }

        /*
        if (lastModified == -1L) {
            // Note: Not all content has Last-Modified header. We should then
            // use lastModified() of the cached file, to compute expires time.
            File cached = getCachedFile(pCacheURI, pRequest);
            if (cached != null && cached.exists()) {
                lastModified = cached.lastModified();
                //// System.out.println(" ## HTTPCache ## Last-Modified is " + NetUtil.formatHTTPDate(lastModified) + ", using cachedFile.lastModified()");
            }
        }
        */

        // Get the real file for this request, if any
        File real = getRealFile(pRequest);
        //noinspection RedundantIfStatement
        if (real != null && real.exists() && real.lastModified() > lastModified) {
            // System.out.println(" ## HTTPCache ## Content is stale (new content"
            //        + NetUtil.formatHTTPDate(lastModified) + " before " + NetUtil.formatHTTPDate(real.lastModified()) + ").");
            return true;
        }

        return false;
    }

    /**
     * Parses a cached header with directive to an int.
     * E.g: Cache-Control: max-age=60, returns 60
     *
     * @param pCached     the cached response
     * @param pHeaderName the header name (e.g: {@code CacheControl})
     * @param pDirective  the directive (e.g: {@code max-age}
     * @return the int value, or {@code -1} if not found
     */
    private int getIntHeader(final CachedResponse pCached, final String pHeaderName, final String pDirective) {
        String[] headerValues = pCached.getHeaderValues(pHeaderName);
        int value = -1;

        if (headerValues != null) {
            for (String headerValue : headerValues) {
                if (pDirective == null) {
                    if (!StringUtil.isEmpty(headerValue)) {
                        value = Integer.parseInt(headerValue);
                    }
                    break;
                }
                else {
                    int start = headerValue.indexOf(pDirective);

                    // Directive found
                    if (start >= 0) {

                        int end = headerValue.lastIndexOf(',');
                        if (end < start) {
                            end = headerValue.length();
                        }

                        headerValue = headerValue.substring(start, end);

                        if (!StringUtil.isEmpty(headerValue)) {
                            value = Integer.parseInt(headerValue);
                        }

                        break;
                    }
                }
            }
        }

        return value;
    }

    /**
     * Utility to read a date header from a cached response.
     *
     * @param pHeaderValue the header value
     * @return the parsed date as a long, or {@code -1L} if not found
     * @see javax.servlet.http.HttpServletRequest#getDateHeader(String)
     */
    static long getDateHeader(final String pHeaderValue) {
        long date = -1L;
        if (pHeaderValue != null) {
            date = NetUtil.parseHTTPDate(pHeaderValue);
        }
        return date;
    }

    // TODO: Extract and make public?
    final static class SizedLRUMap<K, V> extends LRUHashMap<K, V> {
        int mSize;
        int mMaxSize;

        public SizedLRUMap(int pMaxSize) {
            //super(true);
            super(); // Note: super.mMaxSize doesn't count...
            mMaxSize = pMaxSize;
        }


        // In super (LRUMap?) this could just return 1...
        protected int sizeOf(Object pValue) {
            // HACK: As this is used as a backing for a TimeoutMap, the values
            // will themselves be Entries...
            while (pValue instanceof Map.Entry) {
                pValue = ((Map.Entry) pValue).getValue();
            }

            CachedResponse cached = (CachedResponse) pValue;
            return (cached != null ? cached.size() : 0);
        }

        @Override
        public V put(K pKey, V pValue) {
            mSize += sizeOf(pValue);

            V old = super.put(pKey, pValue);
            if (old != null) {
                mSize -= sizeOf(old);
            }
            return old;
        }

        @Override
        public V remove(Object pKey) {
            V old = super.remove(pKey);
            if (old != null) {
                mSize -= sizeOf(old);
            }
            return old;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> pEldest) {
            if (mMaxSize <= mSize) { // NOTE: mMaxSize here is mem size
                removeLRU();
            }
            return false;
        }

        @Override
        public void removeLRU() {
            while (mMaxSize <= mSize) { // NOTE: mMaxSize here is mem size
                super.removeLRU();
            }
        }
    }

}