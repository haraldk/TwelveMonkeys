package com.twelvemonkeys.imageio.plugins.psd;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDVersionInfo
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PSDVersionInfo.java,v 1.0 Nov 6, 2009 1:02:19 PM haraldk Exp$
 */
final class PSDVersionInfo extends PSDImageResource {

    int mVersion;
    boolean mHasRealMergedData;
    String mWriter;
    String mReader;
    int mFileVersion;

    PSDVersionInfo(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        /*
        4 bytes version
        1 byte hasRealMergedData
        Unicode string: writer name
        Unicode string: reader name
        4 bytes file version.         
         */

        mVersion = pInput.readInt();
        mHasRealMergedData = pInput.readBoolean();

        mWriter = PSDUtil.readUnicodeString(pInput);
        mReader = PSDUtil.readUnicodeString(pInput);
        
        mFileVersion = pInput.readInt();
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", version: ").append(mVersion);
        builder.append(", hasRealMergedData: ").append(mHasRealMergedData);
        builder.append(", writer: ").append(mWriter);
        builder.append(", reader: ").append(mReader);
        builder.append(", file version: ").append(mFileVersion);
        builder.append("]");

        return builder.toString();
    }
}
