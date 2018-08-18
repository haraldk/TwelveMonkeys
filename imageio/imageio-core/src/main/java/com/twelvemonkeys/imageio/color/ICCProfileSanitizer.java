/*
 * Copyright (c) 2015, Harald Kuhr
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

package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.lang.SystemUtil;

import java.awt.color.ICC_Profile;

/**
 * ICCProfileSanitizer.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ProfileCleaner.java,v 1.0 06/01/15 harald.kuhr Exp$
 */
interface ICCProfileSanitizer {
    void fixProfile(ICC_Profile profile);

    boolean validationAltersProfileHeader();

    class Factory {
        static ICCProfileSanitizer get() {
            // Strategy pattern:
            //  - KCMSSanitizerStrategy - Current behaviour, default for Java 1.6 and Oracle JRE 1.7
            //  - LCMSSanitizerStrategy - New behaviour, default for OpenJDK 1.7 and all java 1.8
            //              (unless KCMS switch -Dsun.java2d.cmm=sun.java2d.cmm.kcms.KcmsServiceProvider present)
            // TODO: Allow user-specific strategy selection, should heuristics not work..?
            // -Dcom.twelvemonkeys.imageio.color.ICCProfileSanitizer=com.foo.bar.FooCMSSanitizer

            // TODO: Support for explicit Java 7 settings: sun.java2d.cmm.kcms.CMM
            // (the CMSManager was changed from using direct class to ServiceProvider in Java 8,
            // so the class names/interfaces are different).

            // Here's the evolution of Java Color Management:

            // Java 6:
            // sun.awt.color.CMM (as the one and only)

            // Java 7:
            // sun.java2d.cmm.CMSManager (using default sun.java2d.cmm=sun.java2d.cmm.kcms.CMM)
            // sun.java2d.cmm.PCMM
            // sun.java2d.cmm.kcms.CMM implements PCMM (similar to Java 6 CMM)
            // sun.java2d.cmm.lcms.LCMS implements PCMM

            // Java 8:
            // sun.java2d.cmm.CMSManager (using default sun.java2d.cmm=sun.java2d.cmm.lcms.LcmsServiceProvider)
            // sun.java2d.cmm.CMMServiceProvider (getModule() method, that returns PCMM)
            // sun.java2d.cmm.PCMM
            // sun.java2d.cmm.kcms.KcmsServiceProvider
            // sun.java2d.cmm.kcms.CMM implements PCMM (similar to Java 6 CMM)
            // sun.java2d.cmm.lcms.LcmsServiceProvider
            // sun.java2d.cmm.lcms.LCMS implements PCMM

            // TODO: Consider a different option, invoking CMSManager.getModule() through reflection to get actual used instance
            // Default to using a NullSanitizerStrategy on non-Sun/Oracle systems?

            ICCProfileSanitizer instance;

            // Explicit System properties
            String cmmProperty = System.getProperty("sun.java2d.cmm");
            if ("sun.java2d.cmm.kcms.KcmsServiceProvider".equals(cmmProperty) && SystemUtil.isClassAvailable("sun.java2d.cmm.kcms.CMM")) {
                instance = new KCMSSanitizerStrategy();
            }
            else if ("sun.java2d.cmm.lcms.LcmsServiceProvider".equals(cmmProperty) && SystemUtil.isClassAvailable("sun.java2d.cmm.lcms.LCMS")) {
                instance = new LCMSSanitizerStrategy();
            }
            // Default for Java 1.8+ or OpenJDK 1.7+ (no KCMS available)
            else if (SystemUtil.isClassAvailable("java.util.stream.Stream")
                    || SystemUtil.isClassAvailable("java.lang.invoke.CallSite") && !SystemUtil.isClassAvailable("sun.java2d.cmm.kcms.CMM")) {
                instance = new LCMSSanitizerStrategy();
            }
            // Default for all Java versions <= 1.7
            else {
                instance = new KCMSSanitizerStrategy();
            }

            if (ColorSpaces.DEBUG) {
                System.out.println("ICC ProfileCleaner instance: " + instance.getClass().getName());
            }

            return instance;
        }
    }
}
