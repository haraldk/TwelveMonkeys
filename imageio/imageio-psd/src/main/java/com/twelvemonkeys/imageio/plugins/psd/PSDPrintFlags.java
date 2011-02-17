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
    boolean labels;
    boolean cropMasks;
    boolean colorBars;
    boolean registrationMarks;
    boolean negative;
    boolean flip;
    boolean interpolate;
    boolean caption;

    PSDPrintFlags(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        labels = pInput.readBoolean();
        cropMasks = pInput.readBoolean();
        colorBars = pInput.readBoolean();
        registrationMarks = pInput.readBoolean();
        negative = pInput.readBoolean();
        flip = pInput.readBoolean();
        interpolate = pInput.readBoolean();
        caption = pInput.readBoolean();

        pInput.skipBytes(size - 8);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", labels: ").append(labels);
        builder.append(", crop masks: ").append(cropMasks);
        builder.append(", color bars: ").append(colorBars);
        builder.append(", registration marks: ").append(registrationMarks);
        builder.append(", negative: ").append(negative);
        builder.append(", flip: ").append(flip);
        builder.append(", interpolate: ").append(interpolate);
        builder.append(", caption: ").append(caption);

        builder.append("]");

        return builder.toString();
    }
}
