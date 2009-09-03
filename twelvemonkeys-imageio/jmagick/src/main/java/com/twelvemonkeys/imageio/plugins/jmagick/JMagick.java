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

package com.twelvemonkeys.imageio.plugins.jmagick;

import com.twelvemonkeys.lang.SystemUtil;
import magick.MagickImage;

import java.io.IOException;
import java.util.Properties;

/**
 * JMagick helper class, to iron out a few wrinkles.
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: JMagick.java,v 1.0 16.jan.2006 14:20:30 haku Exp$
 */
class JMagick {

    static Properties sProperties;

    static {
        // NOTE: Workaround for strange defaults in JMagick:
        // Makes JMagick load dll's using the context classloader, rather than
        // the system classloader as default...
        if (System.getProperty("jmagick.systemclassloader") == null) {
            System.setProperty("jmagick.systemclassloader", "no");
        }

        // Makes this class fail on init, if JMagick is unavailable
        try {
            MagickImage.class.getClass();
            new MagickImage(); // Loads the JNI lib, if needed
        }
        catch (Error e) {
            System.err.print("JMagick not available: ");
            System.err.println(e);
            System.err.println("Make sure JMagick libraries are available in java.library.path. Current value: ");
            System.err.println("\"" + System.getProperty("java.library.path") + "\"");

            throw e;
        }

        // Load custom properties for the JMagickReader
        try {
            sProperties = SystemUtil.loadProperties(JMagickReader.class);
        }
        catch (IOException e) {
            System.err.println("Could not read properties for JMagickReader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void init() {
        // No-op.
    }   
}
