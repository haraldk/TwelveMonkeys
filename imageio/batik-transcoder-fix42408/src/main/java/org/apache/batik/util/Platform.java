/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.util;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.lang.reflect.Method;

/**
 * Platform specific functionality.
 *
 * @author <a href="mailto:cam%40mcc%2eid%2eau">Cameron McCormack</a>
 * @version $Id$
 */
public abstract class Platform {

    /**
     * Whether we are running on Mac OS X.
     */
    public static boolean isOSX =
        System.getProperty("os.name").equals("Mac OS X");

    /**
     * Whether we are running on JRE 1.3.
     */
    public static boolean isJRE13 =
        System.getProperty("java.version").startsWith("1.3");

    /**
     * Unmaximizes the specified Frame.
     */
    public static void unmaximize(Frame f) {
        if (!isJRE13) {
            try {
                Method m1 =
                    Frame.class.getMethod("getExtendedState", (Class[]) null);
                Method m2 =
                    Frame.class.getMethod("setExtendedState",
                                          new Class[] { Integer.TYPE });
                int i = ((Integer) m1.invoke(f, (Object[]) null)).intValue();
                m2.invoke(f, new Object[] { new Integer(i & ~6) });
            } catch (java.lang.reflect.InvocationTargetException ite) {
            } catch (NoSuchMethodException nsme) {
            } catch (IllegalAccessException iae) {
            }
        }
    }
    
    /**
     * Returns the active screen resolution. If Batik is run in a headless environment, this
     * method will return the default "96" unlike {@link Toolkit#getScreenResolution()} which
     * will throw a {@link HeadlessException}.
     * @return the screen resolution in dpi
     */
    public static int getScreenResolution() {
        if (GraphicsEnvironment.isHeadless()) {
            return 96;
        } else {
            return Toolkit.getDefaultToolkit().getScreenResolution();
        }
    }
}
