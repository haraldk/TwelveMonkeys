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
    void fixProfile(ICC_Profile profile, byte[] profileHeader);

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
            // sun.javard.cmm.lcms.LCMS implements PCMM

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
