package com.twelvemonkeys.imageio.plugins.iff;

import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;

import javax.imageio.IIOException;
import java.awt.image.BufferedImage;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * XS24Chunk.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XS24Chunk.java,v 1.0 01/02/2022 haraldk Exp$
 */
final class XS24Chunk extends IFFChunk {
    private byte[] data;
    int width;
    int height;

    XS24Chunk(final int chunkLength) {
        super(IFF.CHUNK_XS24, chunkLength);
    }

    @Override
    void readChunk(final DataInput input) throws IOException {
        width = input.readUnsignedShort();
        height = input.readUnsignedShort();
        input.readShort(); // Not sure what this is?

        int dataLength = width * height * 3;
        if (dataLength > chunkLength - 6) {
            throw new IIOException("Bad XS24 chunk: " + width + " * " + height + " * 3 > chunk length (" + chunkLength + ")");
        }

        data = new byte[dataLength];

        input.readFully(data);

        // Skip pad
        for (int i = 0; i < chunkLength - dataLength - 6; i++) {
            input.readByte();
        }
    }

    @Override
    void writeChunk(final DataOutput output) {
        throw new InternalError("Not implemented: writeChunk()");
    }

    @Override
    public String toString() {
        return super.toString()
                + "{thumbnail=" + data.length + '}';
    }

    public BufferedImage thumbnail() {
        BufferedImage thumbnail = ImageTypeSpecifiers.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR)
                                                     .createBufferedImage(width, height);
        thumbnail.getRaster().setDataElements(0, 0, width, height, data);
        return thumbnail;
    }
}
