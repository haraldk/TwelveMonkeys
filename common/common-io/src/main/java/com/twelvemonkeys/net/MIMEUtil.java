/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.net;

import com.twelvemonkeys.lang.StringUtil;
import com.twelvemonkeys.lang.SystemUtil;

import java.io.IOException;
import java.util.*;

/**
 * Contains mappings from file extension to mime-types and from mime-type to file-types.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/net/MIMEUtil.java#5 $
 *
 * @see <A href="http://www.iana.org/assignments/media-types/">MIME Media Types</A>
 */
public final class MIMEUtil {
    // TODO: Piggy-back on the mappings form the JRE? (1.6 comes with javax.activation)
    // TODO: Piggy-back on mappings from javax.activation?
    // See: http://java.sun.com/j2ee/sdk_1.3/techdocs/api/javax/activation/MimetypesFileTypeMap.html
    // See: http://java.sun.com/javase/6/docs/api/javax/activation/MimetypesFileTypeMap.html
    // TODO: Use the format (and lookup) specified by the above URLs
    // TODO: Allow 3rd party to add mappings? Will need application context support to do it safe.. :-P

    private static Map<String, List<String>> sExtToMIME = new HashMap<String, List<String>>();
    private static Map<String, List<String>> sUnmodifiableExtToMIME = Collections.unmodifiableMap(sExtToMIME);

    private static Map<String, List<String>> sMIMEToExt = new HashMap<String, List<String>>();
    private static Map<String, List<String>> sUnmodifiableMIMEToExt = Collections.unmodifiableMap(sMIMEToExt);

    static {
        // Load mapping for MIMEUtil
        try {
            Properties mappings = SystemUtil.loadProperties(MIMEUtil.class);

            for (Map.Entry entry : mappings.entrySet()) {
                // Convert and break up extensions and mimeTypes
                String extStr = StringUtil.toLowerCase((String) entry.getKey());
                List<String> extensions =
                        Collections.unmodifiableList(Arrays.asList(StringUtil.toStringArray(extStr, ";, ")));

                String typeStr = StringUtil.toLowerCase((String) entry.getValue());
                List<String> mimeTypes =
                        Collections.unmodifiableList(Arrays.asList(StringUtil.toStringArray(typeStr, ";, ")));

                // TODO: Handle duplicates in MIME to extension mapping, like
                // xhtml=application/xhtml+xml;application/xml
                // xml=text/xml;application/xml

                // Populate normal and reverse MIME-mappings
                for (String extension : extensions) {
                    sExtToMIME.put(extension, mimeTypes);
                }

                for (String mimeType : mimeTypes) {
                    sMIMEToExt.put(mimeType, extensions);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Could not read properties for MIMEUtil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Disallow construction
    private MIMEUtil() {
    }

    /**
     * Returns the default MIME type for the given file extension.
     *
     * @param pFileExt the file extension
     *
     * @return a {@code String} containing the MIME type, or {@code null} if
     * there are no known MIME types for the given file extension.
     */
    public static String getMIMEType(final String pFileExt) {
        List<String> types = sExtToMIME.get(StringUtil.toLowerCase(pFileExt));
        return (types == null || types.isEmpty()) ? null : types.get(0);
    }

    /**
     * Returns all MIME types for the given file extension.
     *
     * @param pFileExt the file extension
     *
     * @return a {@link List} of {@code String}s containing the MIME types, or an empty
     * list, if there are no known MIME types for the given file extension.
     */
    public static List<String> getMIMETypes(final String pFileExt) {
        List<String> types = sExtToMIME.get(StringUtil.toLowerCase(pFileExt));
        return maskNull(types);
    }

    /**
     * Returns an unmodifiabale {@link Map} view of the extension to
     * MIME mapping, to use as the default mapping in client applications.
     *
     * @return an unmodifiabale {@code Map} view of the extension to
     * MIME mapping.
     */
    public static Map<String, List<String>> getMIMETypeMappings() {
        return sUnmodifiableExtToMIME;
    }

    /**
     * Returns the default file extension for the given MIME type.
     * Specifying a wildcard type will return {@code null}.
     *
     * @param pMIME the MIME type
     *
     * @return a {@code String} containing the file extension, or {@code null}
     * if there are no known file extensions for the given MIME type.
     */
    public static String getExtension(final String pMIME) {
        String mime = bareMIME(StringUtil.toLowerCase(pMIME));
        List<String> extensions = sMIMEToExt.get(mime);
        return (extensions == null || extensions.isEmpty()) ? null : extensions.get(0);
    }

    /**
     * Returns all file extension for the given MIME type.
     * The default extension will be the first in the list.
     * Note that no specific order is given for wildcard types (image/*, *&#47;* etc).
     *
     * @param pMIME the MIME type
     *
     * @return a {@link List} of {@code String}s containing the MIME types, or an empty
     * list, if there are no known file extensions for the given MIME type.
     */
    public static List<String> getExtensions(final String pMIME) {
        String mime = bareMIME(StringUtil.toLowerCase(pMIME));
        if (mime.endsWith("/*")) {
            return getExtensionForWildcard(mime);
        }
        List<String> extensions = sMIMEToExt.get(mime);
        return maskNull(extensions);
    }

    // Gets all extensions for a wildcard MIME type
    private static List<String> getExtensionForWildcard(final String pMIME) {
        final String family = pMIME.substring(0, pMIME.length() - 1);
        Set<String> extensions = new LinkedHashSet<String>();
        for (Map.Entry<String, List<String>> mimeToExt : sMIMEToExt.entrySet()) {
            if ("*/".equals(family) || mimeToExt.getKey().startsWith(family)) {
                extensions.addAll(mimeToExt.getValue());
            }
        }
        return Collections.unmodifiableList(new ArrayList<String>(extensions));
    }

    /**
     * Returns an unmodifiabale {@link Map} view of the MIME to
     * extension mapping, to use as the default mapping in client applications.
     *
     * @return an unmodifiabale {@code Map} view of the MIME to
     * extension mapping.
     */
    public static Map<String, List<String>> getExtensionMappings() {
        return sUnmodifiableMIMEToExt;
    }

    /**
     * Tests wehter the type is a subtype of the type family.
     *
     * @param pTypeFamily the MIME type family ({@code image/*, *&#47;*}, etc)
     * @param pType the MIME type
     * @return {@code true} if {@code pType} is a subtype of {@code pTypeFamily}, otherwise {@code false}
     */
    // TODO: Rename? isSubtype?
    // TODO: Make public
    static boolean includes(final String pTypeFamily, final String pType) {
        // TODO: Handle null in a well-defined way
        // - Is null family same as */*?
        // - Is null subtype of any family? Subtype of no family?

        String type = bareMIME(pType);
        return type.equals(pTypeFamily)
                || "*/*".equals(pTypeFamily)
                || pTypeFamily.endsWith("/*") && pTypeFamily.startsWith(type.substring(0, type.indexOf('/')));
    }

    /**
     * Removes any charset or extra info from the mime-type string (anything after a semicolon, {@code ;}, inclusive).
     *
     * @param pMIME the mime-type string
     * @return the bare mime-type
     */
    public static String bareMIME(final String pMIME) {
        int idx;
        if (pMIME != null && (idx = pMIME.indexOf(';')) >= 0) {
            return pMIME.substring(0, idx);
        }
        return pMIME;
    }

    // Returns the list or empty list if list is null
    private static List<String> maskNull(List<String> pTypes) {
        return (pTypes == null) ? Collections.<String>emptyList() : pTypes;
    }

    /**
     * For debugging. Prints all known MIME types and file extensions.
     *
     * @param pArgs command line arguments
     */
    public static void main(String[] pArgs) {
        if (pArgs.length > 1) {
            String type = pArgs[0];
            String family = pArgs[1];
            boolean incuded = includes(family, type);
            System.out.println(
                    "Mime type family " + family
                    + (incuded ? " includes " : " does not include ")
                    + "type " + type
            );
        }
        if (pArgs.length > 0) {
            String str = pArgs[0];

            if (str.indexOf('/') >= 0) {
                // MIME
                String extension = getExtension(str);
                System.out.println("Default extension for MIME type '" + str + "' is "
                        + (extension != null ? ": '" + extension + "'" : "unknown") + ".");
                System.out.println("All possible: " + getExtensions(str));
            }
            else {
                // EXT
                String mimeType = getMIMEType(str);
                System.out.println("Default MIME type for extension '" + str + "' is "
                        + (mimeType != null ? ": '" + mimeType + "'" : "unknown") + ".");
                System.out.println("All possible: " + getMIMETypes(str));
            }

            return;
        }

        Set set = sMIMEToExt.keySet();
        String[] mimeTypes = new String[set.size()];
        int i = 0;
        for (Iterator iterator = set.iterator(); iterator.hasNext(); i++) {
            String mime = (String) iterator.next();
            mimeTypes[i] = mime;
        }
        Arrays.sort(mimeTypes);

        System.out.println("Known MIME types (" + mimeTypes.length + "):");
        for (int j = 0; j < mimeTypes.length; j++) {
            String mimeType = mimeTypes[j];

            if (j != 0) {
                System.out.print(", ");
            }

            System.out.print(mimeType);
        }

        System.out.println("\n");

        set = sExtToMIME.keySet();
        String[] extensions = new String[set.size()];
        i = 0;
        for (Iterator iterator = set.iterator(); iterator.hasNext(); i++) {
            String ext = (String) iterator.next();
            extensions[i] = ext;
        }
        Arrays.sort(extensions);

        System.out.println("Known file types (" + extensions.length + "):");
        for (int j = 0; j < extensions.length; j++) {
            String extension = extensions[j];

            if (j != 0) {
                System.out.print(", ");
            }

            System.out.print(extension);
        }
        System.out.println();
    }
}