package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDPrintFlagsInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDPrintFlagsInfo.java,v 1.0 Jul 28, 2009 5:16:27 PM haraldk Exp$
 */
final class PSDPrintFlags extends PSDImageResource {
    boolean mLabels;
    boolean mCropMasks;
    boolean mColorBars;
    boolean mRegistrationMarks;
    boolean mNegative;
    boolean mFlip;
    boolean mInterpolate;
    boolean mCaption;

    PSDPrintFlags(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        mLabels = pInput.readBoolean();
        mCropMasks = pInput.readBoolean();
        mColorBars = pInput.readBoolean();
        mRegistrationMarks = pInput.readBoolean();
        mNegative = pInput.readBoolean();
        mFlip = pInput.readBoolean();
        mInterpolate = pInput.readBoolean();
        mCaption = pInput.readBoolean();

        pInput.skipBytes(mSize - 8);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", labels: ").append(mLabels);
        builder.append(", crop masks: ").append(mCropMasks);
        builder.append(", color bars: ").append(mColorBars);
        builder.append(", registration marks: ").append(mRegistrationMarks);
        builder.append(", negative: ").append(mNegative);
        builder.append(", flip: ").append(mFlip);
        builder.append(", interpolate: ").append(mInterpolate);
        builder.append(", caption: ").append(mCaption);

        builder.append("]");

        return builder.toString();
    }
}
