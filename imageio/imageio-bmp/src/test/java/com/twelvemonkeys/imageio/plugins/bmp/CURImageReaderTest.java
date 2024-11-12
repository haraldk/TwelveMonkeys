/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.bmp;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CURImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: CURImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class CURImageReaderTest extends ImageReaderAbstractTest<CURImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new CURImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/cur/hand.cur"), new Dimension(32, 32)),
                new TestData(getClassLoaderResource("/cur/zoom.cur"), new Dimension(32, 32))
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Collections.singletonList("cur");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("cur");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Arrays.asList("image/vnd.microsoft.cursor", "image/cursor", "image/x-cursor");
    }

    private void assertHotSpot(final TestData pTestData, final ImageReadParam pParam, final Point pExpected) throws IOException {
        CURImageReader reader = createReader();
        reader.setInput(pTestData.getInputStream());

        BufferedImage image = reader.read(0, pParam);

        // We can only be sure the hotspot is defined, if no param, but if defined, it must be correct
        Object hotspot = image.getProperty("cursor_hotspot");
        if (hotspot != Image.UndefinedProperty || pParam == null) {

            // Typically never happens, because of weirdness with UndefinedProperty
            assertNotNull(hotspot, "Hotspot for cursor not present");

            // Image weirdness
            assertNotSame(Image.UndefinedProperty, hotspot, "Hotspot for cursor undefined (java.awt.Image.UndefinedProperty)");

            assertTrue(hotspot instanceof Point, String.format("Hotspot not a java.awt.Point: %s", hotspot.getClass()));
            assertEquals(pExpected, hotspot);
        }

        assertNotNull(reader.getHotSpot(0), "Hotspot for cursor not present");
        assertEquals(pExpected, reader.getHotSpot(0));
    }

    @Test
    public void testHandHotspot() throws IOException {
        assertHotSpot(getTestData().get(0), null, new Point(15, 15));
    }

    @Test
    public void testZoomHotspot() throws IOException {
        assertHotSpot(getTestData().get(1), null, new Point(13, 11));
    }

    @Test
    public void testHandHotspotWithParam() throws IOException {
        ImageReadParam param = new ImageReadParam();
        assertHotSpot(getTestData().get(0), param, new Point(15, 15));
    }

    @Test
    public void testHandHotspotExplicitDestination() throws IOException {
        CURImageReader reader = createReader();
        reader.setInput(getTestData().get(0).getInputStream());
        BufferedImage image = reader.read(0);

        // Create dest image with same data, except properties...
        BufferedImage dest = new BufferedImage(
                image.getColorModel(), image.getRaster(), image.getColorModel().isAlphaPremultiplied(), null
        );
        ImageReadParam param = new ImageReadParam();
        param.setDestination(dest);

        assertHotSpot(getTestData().get(0), param, new Point(15, 15));
    }

    // TODO: Test cursor is transparent

    @Test
    @Disabled("Known issue")
    @Override
    public void testNotBadCaching() throws IOException {
        super.testNotBadCaching();
    }

    @Test
    @Disabled("Known issue: Subsampled reading currently not supported")
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        super.testReadWithSubsampleParamPixels();
    }
}