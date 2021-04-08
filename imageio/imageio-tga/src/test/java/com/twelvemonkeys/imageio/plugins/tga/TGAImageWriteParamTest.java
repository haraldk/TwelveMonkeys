/*
 * Copyright (c) 2021, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tga;

import org.junit.Test;

import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 * TGAImageWriteParamTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: TGAImageWriteParamTest.java,v 1.0 08/04/2021 haraldk Exp$
 */
public class TGAImageWriteParamTest {
    @Test
    public void testDefaultCopyFromMetadata() {
        TGAImageWriteParam param = new TGAImageWriteParam();
        assertTrue(param.canWriteCompressed());
        assertEquals(ImageWriteParam.MODE_COPY_FROM_METADATA, param.getCompressionMode());
    }

    @Test
    public void testIsRLENoParamNoMetadata() {
        assertFalse(TGAImageWriteParam.isRLE(null, null));
    }

    @Test
    public void testIsRLEParamCantWriteCompressedNoMetadata() {
        // Base class has canWriteCompressed == false, need to test
        ImageWriteParam param = new ImageWriteParam(null);
        assumeFalse(param.canWriteCompressed());

        assertFalse(TGAImageWriteParam.isRLE(param, null));
    }

    @Test
    public void testIsRLEParamDefaultNoMetadata() {
        TGAImageWriteParam param = new TGAImageWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
        assertFalse(TGAImageWriteParam.isRLE(param, null));
    }

    @Test
    public void testIsRLEParamExplicitNoMetadata() {
        TGAImageWriteParam param = new TGAImageWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        assertFalse(TGAImageWriteParam.isRLE(param, null));

        param.setCompressionType("RLE");
        assertTrue(TGAImageWriteParam.isRLE(param, null));
    }

    @Test
    public void testIsRLEParamDisabledNoMetadata() {
        TGAImageWriteParam param = new TGAImageWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_DISABLED);

        assertFalse(TGAImageWriteParam.isRLE(param, null));
    }

    @Test
    public void testIsRLEParamCopyNoMetadata() {
        ImageWriteParam param = new TGAImageWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);

        assertFalse(TGAImageWriteParam.isRLE(param, null));
    }

    @Test
    public void testIsRLEParamCantWriteCompressedAndMetadata() {
        // Base class has canWriteCompressed == false, need to test
        ImageWriteParam param = new ImageWriteParam(null);
        assumeFalse(param.canWriteCompressed());

        assertFalse(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), false), null)));
        assertFalse(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), true), null)));
    }

    @Test
    public void testIsRLEParamCopyAndMetadataNoCompression() {
        ImageWriteParam param = new TGAImageWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);

        assertTrue(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), true), null)));
    }

    @Test
    public void testIsRLEParamCopyAndMetadataRLE() {
        ImageWriteParam param = new TGAImageWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);

        assertTrue(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), true), null)));
    }

    @Test
    public void testIsRLEParamExplicitAndMetadata() {
        TGAImageWriteParam param = new TGAImageWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        assertFalse(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), false), null)));
        assertFalse(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), true), null)));

        param.setCompressionType("RLE");
        assertTrue(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), false), null)));
        assertTrue(TGAImageWriteParam.isRLE(param, new TGAMetadata(TGAHeader.from(new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR), true), null)));
    }

}