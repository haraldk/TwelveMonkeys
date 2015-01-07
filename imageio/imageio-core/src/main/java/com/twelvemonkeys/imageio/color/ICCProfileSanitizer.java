package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.lang.SystemUtil;

import java.awt.color.ICC_Profile;

/**
 * ProfileCleaner.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ProfileCleaner.java,v 1.0 06/01/15 harald.kuhr Exp$
 */
interface ICCProfileSanitizer {
    void fixProfile(ICC_Profile profile, byte[] profileHeader);

    static class Factory {
        static ICCProfileSanitizer get() {
            // Strategy pattern:
            //  - KCMSSanitizerStrategy - Current behaviour, default for Java 1.6 and Oracle JRE 1.7
            //  - LCMSSanitizerStrategy - New behaviour, default for OpenJDK 1.7 and all java 1.8
            //              (unless KCMS switch -Dsun.java2d.cmm=sun.java2d.cmm.kcms.KcmsServiceProvider present)
            // TODO: Allow user-specific strategy selection, should heuristics not work..?
            // -Dcom.twelvemonkeys.imageio.color.ICCProfileSanitizer=com.foo.bar.FooCMSSanitizer

            ICCProfileSanitizer instance;

            // Explicit System properties
            if ("sun.java2d.cmm.kcms.KcmsServiceProvider".equals(System.getProperty("sun.java2d.cmm")) && SystemUtil.isClassAvailable("sun.java2d.cmm.kcms.CMM")) {
                instance = new KCMSSanitizerStrategy();
            }
            else if ("sun.java2d.cmm.lcms.LcmsServiceProvider".equals(System.getProperty("sun.java2d.cmm")) && SystemUtil.isClassAvailable("sun.java2d.cmm.lcms.LCMS")) {
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
