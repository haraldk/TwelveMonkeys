/*
 * Copyright (c) 2012, Harald Kuhr
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

package com.twelvemonkeys.imageio.metadata.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.MetadataReaderAbstractTest;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PhotoshopReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: PhotoshopReaderTest.java,v 1.0 04.01.12 12:01 haraldk Exp$
 */
public class PSDReaderTest extends MetadataReaderAbstractTest {
    @Override
    protected InputStream getData() throws IOException {
        return getResource("/psd/psd-jpeg-segment.bin").openStream();
    }

    @Override
    protected PSDReader createReader() {
        return new PSDReader();
    }

    @Test
    public void testPhotoshopDirectoryContents() throws IOException {
        Directory directory = createReader().read(getDataAsIIS());

        assertEquals(23, directory.size());

        assertNotNull(directory.getEntryById(0x0404));
        assertNotNull(directory.getEntryById(0x0425));
        assertNotNull(directory.getEntryById(0x03ea));
        assertNotNull(directory.getEntryById(0x03e9));
        assertNotNull(directory.getEntryById(0x03ed));
        assertNotNull(directory.getEntryById(0x0426));

        // TODO: More
    }

    @Test
    public void testPhotoshopResourcePHUT() throws IOException {
        // Test sample contains non-8BIM resource: PHUT (PhotoDeluxe)
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/psd/friends-phut-resource.jpg"))) {
            stream.seek(38);

            Directory directory = createReader().read(new SubImageInputStream(stream, 298));

            assertEquals(9, directory.size()); // 6 8BIM + 2 PHUT + 1 8 BIM
        }
    }
}
