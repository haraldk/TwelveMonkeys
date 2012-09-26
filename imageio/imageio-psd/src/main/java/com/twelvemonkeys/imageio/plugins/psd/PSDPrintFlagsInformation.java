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
    int version;
    boolean cropMasks;
    int field;
    long bleedWidth;
    int bleedScale;

    PSDPrintFlagsInformation(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        version = pInput.readUnsignedShort();
        cropMasks = pInput.readBoolean();
        field = pInput.readUnsignedByte(); // TODO: Is this really pad?
        bleedWidth = pInput.readUnsignedInt();
        bleedScale = pInput.readUnsignedShort();

        pInput.skipBytes(size - 10);
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", version: ").append(version);
        builder.append(", crop masks: ").append(cropMasks);
        builder.append(", field: ").append(field);
        builder.append(", bleed width: ").append(bleedWidth);
        builder.append(", bleed scale: ").append(bleedScale);

        builder.append("]");

        return builder.toString();
    }
}