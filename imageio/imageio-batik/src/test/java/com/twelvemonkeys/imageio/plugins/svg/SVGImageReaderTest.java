/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.svg;

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SVGImageReaderTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: SVGImageReaderTest.java,v 1.0 Apr 1, 2008 10:39:17 PM haraldk Exp$
 */
public class SVGImageReaderTest extends ImageReaderAbstractTest<SVGImageReader> {
    private SVGImageReaderSpi provider = new SVGImageReaderSpi();

    protected List<TestData> getTestData() {
        return Arrays.asList(
                new TestData(getClassLoaderResource("/svg/batikLogo.svg"), new Dimension(450, 500)),
                new TestData(getClassLoaderResource("/svg/red-square.svg"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/svg/blue-square.svg"), new Dimension(100, 100)),
                new TestData(getClassLoaderResource("/svg/Android_robot.svg"), new Dimension(400, 400))
        );
    }

    protected ImageReaderSpi createProvider() {
        return provider;
    }

    @Override
    protected SVGImageReader createReader() {
        return new SVGImageReader(createProvider());
    }

    protected Class<SVGImageReader> getReaderClass() {
        return SVGImageReader.class;
    }

    protected List<String> getFormatNames() {
        return Collections.singletonList("svg");
    }

    protected List<String> getSuffixes() {
        return Collections.singletonList("svg");
    }

    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/svg+xml");
    }

    @Test
    public void testScaleViewBox() throws IOException {
        URL svgUrl = getClassLoaderResource("/svg/quadrants.svg");

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        SVGImageReader reader = createReader();
        SVGReadParam param = new SVGReadParam();

        int[] sizes = new int[]{16, 32, 64, 128};
        for (int size : sizes) {
            try (InputStream svgStream = svgUrl.openStream(); ImageInputStream iis = ImageIO.createImageInputStream(svgStream)) {
                reader.reset();
                reader.setInput(iis);


                param.setSourceRenderSize(new Dimension(size, size));
                BufferedImage image = reader.read(0, param);
                checkQuadrantColors(image);
            }
            finally {
                reader.dispose();
            }
        }
    }

    private void checkQuadrantColors(BufferedImage image) {
        int quadPoint = image.getWidth() / 2;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int current = image.getRGB(x, y);
                if (x < quadPoint) {
                    if (y < quadPoint) {
                        assertEquals("x=" + x + " y=" + y + " q=" + quadPoint, 0xFF0000FF, current);
                    }
                    else {
                        assertEquals("x=" + x + " y=" + y + " q=" + quadPoint, 0xFFFF0000, current);
                    }
                }
                else {
                    if (y < quadPoint) {
                        assertEquals("x=" + x + " y=" + y + " q=" + quadPoint, 0xFF00FF00, current);
                    }
                    else {
                        assertEquals("x=" + x + " y=" + y + " q=" + quadPoint, 0xFF000000, current);
                    }
                }
            }
        }
    }

    @Test
    @Override
    public void testReadWithSizeParam() {
        try {
            super.testReadWithSizeParam();
        }
        catch (AssertionError failure) {
            Throwable cause = failure;

            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause instanceof ImagingOpException && cause.getMessage().equals("Unable to transform src image")) {
                // This is a very strange regression introduced by the later JDK/JRE (at least it's in 7u45)
                // Haven't found a workaround yet
                System.err.println("WARNING: Oracle JRE 7u45 broke my SVGImageReader (known issue): " + cause.getMessage());
            }
            else {
                throw failure;
            }
        }
    }

    @Test
    @Ignore("Known issue: Source region reading not supported")
    @Override
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        super.testReadWithSourceRegionParamEqualImage();
    }

    @Test
    @Ignore("Known issue: Subsampled reading not supported")
    @Override
    public void testReadWithSubsampleParamPixels() throws IOException {
        super.testReadWithSubsampleParamPixels();
    }

    @Test
    public void testRepeatedRead() throws IOException {
        Dimension dim = new Dimension(100, 100);
        ImageReader reader = createReader();
        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRenderSize(dim);

        TestData redSquare = new TestData(getClassLoaderResource("/svg/red-square.svg"), dim);
        reader.setInput(redSquare.getInputStream());
        BufferedImage imageRed = reader.read(0, param);
        assertEquals(0xFF0000, imageRed.getRGB(50, 50) & 0xFFFFFF);

        TestData blueSquare = new TestData(getClassLoaderResource("/svg/blue-square.svg"), dim);
        reader.setInput(blueSquare.getInputStream());
        BufferedImage imageBlue = reader.read(0, param);
        assertEquals(0x0000FF, imageBlue.getRGB(50, 50) & 0xFFFFFF);
    }

    @Test
    public void testEmbeddedBaseURI() throws URISyntaxException, IOException {
        URL resource = getClassLoaderResource("/svg/barChart.svg");

        SVGImageReader reader = createReader();

        TestData data = new TestData(resource, (Dimension) null);
        try (ImageInputStream stream = data.getInputStream()) {
            reader.setInput(stream);

            IIOReadWarningListener listener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(listener);

            SVGReadParam param = reader.getDefaultReadParam();
            param.setBaseURI(resource.toURI().toASCIIString());
            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
            assertEquals(450, image.getWidth());
            assertEquals(500, image.getHeight());

            // CSS and embedded resources all go!
            verifyZeroInteractions(listener);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testEmbeddedBeforeBaseURI() throws URISyntaxException, IOException {
        // Asking for metadata, width, height etc, before attempting to read using a param,
        // will cause the document to be parsed without a base URI.
        // This will work, but may not use the CSS...
        URL resource = getClassLoaderResource("/svg/barChart.svg");

        SVGImageReader reader = createReader();

        TestData data = new TestData(resource, (Dimension) null);
        try (ImageInputStream stream = data.getInputStream()) {
            reader.setInput(stream);

            IIOReadWarningListener listener = mock(IIOReadWarningListener.class);
            reader.addIIOReadWarningListener(listener);

            assertEquals(450, reader.getWidth(0));
            assertEquals(500, reader.getHeight(0));

            // Expect the warning about the missing CSS
            verify(listener, atMost(1)).warningOccurred(any(ImageReader.class), anyString());
            reset(listener);

            SVGReadParam param = reader.getDefaultReadParam();
            param.setBaseURI(resource.toURI().toASCIIString());
            BufferedImage image = reader.read(0, param);

            assertNotNull(image);
            assertEquals(450, image.getWidth());
            assertEquals(500, image.getHeight());

            // No more warnings now that the base URI is set
            verifyZeroInteractions(listener);
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testEmbeddedNoBaseURI() throws IOException {
        // With no base URI, we will throw an exception, about the missing embedded resource
        URL resource = getClassLoaderResource("/svg/barChart.svg");

        SVGImageReader reader = createReader();

        TestData data = new TestData(resource, (Dimension) null);
        try (ImageInputStream stream = data.getInputStream()) {
            reader.setInput(stream);

            BufferedImage image = reader.read(0);

            assertNotNull(image);
            assertEquals(450, image.getWidth());
            assertEquals(500, image.getHeight());
        }
        catch (IIOException allowed) {
            assertTrue(allowed.getMessage().contains("batikLogo.svg")); // The embedded resource we don't find
        }
        finally {
            reader.dispose();
        }
    }
}