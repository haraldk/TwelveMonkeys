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

    int version;
    boolean hasRealMergedData;
    String writer;
    String reader;
    int fileVersion;

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

        version = pInput.readInt();
        hasRealMergedData = pInput.readBoolean();

        writer = PSDUtil.readUnicodeString(pInput);
        reader = PSDUtil.readUnicodeString(pInput);
        
        fileVersion = pInput.readInt();
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        builder.append(", version: ").append(version);
        builder.append(", hasRealMergedData: ").append(hasRealMergedData);
        builder.append(", writer: ").append(writer);
        builder.append(", reader: ").append(reader);
        builder.append(", file version: ").append(fileVersion);
        builder.append("]");

        return builder.toString();
    }
}
