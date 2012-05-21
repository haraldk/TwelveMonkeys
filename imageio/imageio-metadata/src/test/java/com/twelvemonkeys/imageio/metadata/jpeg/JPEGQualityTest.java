/*
 * Copyright (c) 2012, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.metadata.jpeg;

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * JPEGQualityTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: JPEGQualityTest.java,v 1.0 10.04.12 12:39 haraldk Exp$
 */
public class JPEGQualityTest {

    private static final float DELTA = .000001f;

    @Test
    public void testGetQuality() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getClass().getResourceAsStream("/jpeg/9788245605525.jpg"));

        try {
            assertEquals(.92f, JPEGQuality.getJPEGQuality(stream), DELTA);
        }
        finally {
            stream.close();
        }
    }

    @Test
    public void testGetQualityAltSample1() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getClass().getResourceAsStream("/jpeg/exif-rgb-thumbnail-bad-exif-kodak-dc210.jpg"));

        try {
            assertEquals(.79f, JPEGQuality.getJPEGQuality(stream), DELTA);
        }
        finally {
            stream.close();
        }
    }

    @Test
    public void testGetQualityAltSample2() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getClass().getResourceAsStream("/jpeg/ts_open_300dpi.jpg"));

        try {
            assertEquals(.99f, JPEGQuality.getJPEGQuality(stream), DELTA);
        }
        finally {
            stream.close();
        }
    }

    @Ignore("Need a JPEG test image with bad DQT data...")
    @Test
    public void testGetQualityBadData() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getClass().getResourceAsStream("/bad-data"));

        try {
            assertEquals(-1f, JPEGQuality.getJPEGQuality(stream), DELTA);
        }
        finally {
            stream.close();
        }
    }

    @Test
    public void testWriteWithQualitySettingMatchesGetQuality() throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/jpeg").next(); // If this fails, we have a more serious problem

        for (int i = 0; i < 10; i++) {
            // TODO: Figure out why we get -1 for input quality 0.1 and 0.3...
            if (i == 0 || i == 2) {
                continue;
            }

            // Set quality
            float quality = (i + 1f) / 10f;
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            // Write image
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            ImageOutputStream output = ImageIO.createImageOutputStream(temp);

            try {
                writer.setOutput(output);
                writer.write(null, new IIOImage(createTestImage(), null, null), param);
            }
            finally {
                output.close();
            }

            // Test quality
            ImageInputStream input = new ByteArrayImageInputStream(temp.toByteArray());

            try {
                assertEquals(quality, JPEGQuality.getJPEGQuality(input), 0f);
            }
            finally {
                input.close();
            }
        }
    }

    @Test
    public void testGetQTables() {
        fail("Not implemented");
    }

    private BufferedImage createTestImage() {
        BufferedImage image = new BufferedImage(90, 60, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = image.createGraphics();

        try {
            g.setColor(Color.WHITE);
            g.fillOval(15, 0, 60, 60);
            g.setColor(Color.RED);
            g.fill(new Polygon(new int[] {0, 90, 0, 0}, new int[] {0, 0, 60, 0}, 4));
        }
        finally {
            g.dispose();
        }

        return image;
    }
}
