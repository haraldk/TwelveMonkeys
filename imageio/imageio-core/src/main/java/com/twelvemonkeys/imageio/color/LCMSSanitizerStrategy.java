package com.twelvemonkeys.imageio.color;

import com.twelvemonkeys.lang.Validate;

import java.awt.color.ICC_Profile;

/**
 * LCMSProfileCleaner.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: LCMSProfileCleaner.java,v 1.0 06/01/15 harald.kuhr Exp$
 */
final class LCMSSanitizerStrategy implements ICCProfileSanitizer {
    public void fixProfile(final ICC_Profile profile) {
        Validate.notNull(profile, "profile");
        // Let LCMS handle things internally for now
    }
}
