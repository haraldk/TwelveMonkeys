/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFWriter;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.stream.SubImageOutputStream;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * EXIF metadata.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: XMPData.java,v 1.0 Jul 28, 2009 5:50:34 PM haraldk Exp$
 *
 * @see <a href="http://en.wikipedia.org/wiki/Exchangeable_image_file_format">Wikipedia</a>
 * @see <a href="http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html">Aware systems TIFF tag reference</a>
 * @see <a href="http://partners.adobe.com/public/developer/tiff/index.html">Adobe TIFF developer resources</a>
 */
final class PSDEXIF1Data extends PSDDirectoryResource {

    PSDEXIF1Data(final short pId, final ImageInputStream pInput) throws IOException {
        super(pId, pInput);
    }

    @Override
    Directory parseDirectory() throws IOException {
        // The data is in essence an embedded TIFF file.
        return new TIFFReader().read(new ByteArrayImageInputStream(data));
    }

    static void writeData(final ImageOutputStream output, final Collection<? extends Entry> directory) throws IOException {
        output.writeInt(PSD.RESOURCE_TYPE);
        output.writeShort(PSD.RES_EXIF_DATA_1);
        output.writeShort(0); // Zero-length Pascal name + pad
        output.writeInt(0); // Dummy length

        long beforeExif = output.getStreamPosition();
        new TIFFWriter().write(directory, new SubImageOutputStream(output));

        long afterExif = output.getStreamPosition();
        if ((afterExif - beforeExif) % 2 != 0) {
            afterExif++;
            output.write(0); // Pad
        }

        // Update length
        output.seek(beforeExif - 4);
        output.writeInt((int) (afterExif - beforeExif));
        output.seek(afterExif);
    }
}
