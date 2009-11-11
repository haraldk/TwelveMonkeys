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
final class PSDPrintFlagsInformation extends PSDImageResource {
    int mVersion;
    boolean mCropMasks;
    int mField;
    long mBleedWidth;
    int mBleedScale;

    PSDPrintFlagsInformation(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        mVersion = pInput.readUnsignedShort();
        mCropMasks = pInput.readBoolean();
        mField = pInput.readUnsignedByte(); // TODO: Is this really pad?
        mBleedWidth = pInput.readUnsignedInt();
        mBleedScale = pInput.readUnsignedShort();

        pInput.skipBytes(mSize - 10);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", version: ").append(mVersion);
        builder.append(", crop masks: ").append(mCropMasks);
        builder.append(", field: ").append(mField);
        builder.append(", bleed width: ").append(mBleedWidth);
        builder.append(", bleed scale: ").append(mBleedScale);

        builder.append("]");

        return builder.toString();
    }
}