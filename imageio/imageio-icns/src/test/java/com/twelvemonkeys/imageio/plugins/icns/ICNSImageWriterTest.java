/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.icns;

import com.twelvemonkeys.imageio.util.ImageWriterAbstractTest;

import org.junit.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

/**
 * ICNSImageWriterTest.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: harald.kuhr$
 * @version $Id: ICNSImageWriterTest.java,v 1.0 25/08/2018 harald.kuhr Exp$
 */
public class ICNSImageWriterTest extends ImageWriterAbstractTest<ICNSImageWriter> {

    @Override
    protected ImageWriterSpi createProvider() {
        return new ICNSImageWriterSpi();
    }

    @Override
    protected List<? extends RenderedImage> getTestData() {
        return asList(
                new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_BINARY),
                new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_INDEXED),
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
//                new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB), // Only supported for compression None/RLE
                new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB),
                new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteNonSquare() throws IOException {
        // ICNS only supports square icons (except some arcane 16x12 we don't currently support)
        ImageWriter writer = createWriter();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(new ByteArrayOutputStream())) {

            writer.setOutput(stream);

            writer.write(new BufferedImage(32, 64, BufferedImage.TYPE_INT_ARGB));

        }
        finally {
            writer.dispose();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteBadSize() throws IOException {
        // ICNS only supports sizes in multiples of 2 (16, 32, 64, ..., 1024 + 48 and 96)
        ImageWriter writer = createWriter();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(new ByteArrayOutputStream())) {

            writer.setOutput(stream);

            writer.write(new BufferedImage(17, 17, BufferedImage.TYPE_INT_ARGB));

        }
        finally {
            writer.dispose();
        }
    }

    @Test
    public void testSequencesSupported() throws IOException {
        ImageWriter writer = createWriter();
        try {
            assertTrue(writer.canWriteSequence());
        }
        finally {
            writer.dispose();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteSequenceNotStarted() throws IOException {
        // ICNS only supports sizes in multiples of 2 (16, 32, 64, ..., 1024 + 48 and 96)
        ImageWriter writer = createWriter();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(new ByteArrayOutputStream())) {

            writer.setOutput(stream);

            BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            writer.writeToSequence(new IIOImage(image, null, null), writer.getDefaultWriteParam());

        }
        finally {
            writer.dispose();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testEndSequenceNotStarted() throws IOException {
        // ICNS only supports sizes in multiples of 2 (16, 32, 64, ..., 1024 + 48 and 96)
        ImageWriter writer = createWriter();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(new ByteArrayOutputStream())) {

            writer.setOutput(stream);
            writer.endWriteSequence();
        }
        finally {
            writer.dispose();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testPrepareSequenceAlreadyStarted() throws IOException {
        // ICNS only supports sizes in multiples of 2 (16, 32, 64, ..., 1024 + 48 and 96)
        ImageWriter writer = createWriter();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(new ByteArrayOutputStream())) {

            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            writer.prepareWriteSequence(null);
        }
        finally {
            writer.dispose();
        }
    }

    @Test
    public void testWriteSequence() throws IOException {
        ImageWriter writer = createWriter();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);

            writer.prepareWriteSequence(null);
            for (RenderedImage image : getTestData()) {
                IIOImage iioImage = new IIOImage(image, null, null);
                writer.writeToSequence(iioImage, writer.getDefaultWriteParam());
            }
            writer.endWriteSequence();
        }
        finally {
            writer.dispose();
        }
    }
}