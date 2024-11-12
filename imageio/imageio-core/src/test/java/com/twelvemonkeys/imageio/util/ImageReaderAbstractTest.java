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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;
import com.twelvemonkeys.lang.Validate;

import org.junit.jupiter.api.Disabled;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.min;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ImageReaderAbstractTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageReaderAbstractTest.java,v 1.0 Apr 1, 2008 10:36:46 PM haraldk Exp$
 */
public abstract class ImageReaderAbstractTest<T extends ImageReader> {
    // TODO: Should we really test if the provider is installed?
    //       - Pro: Tests the META-INF/services config
    //       - Con: Not all providers should be installed at runtime...
    // TODO: Create own subclass for testing the Spis?

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
        ImageIO.setUseCache(false);
    }

    @SuppressWarnings("unchecked")
    private final Class<T> readerClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    protected final ImageReaderSpi provider = createProvider();

    protected abstract ImageReaderSpi createProvider();

    protected final T createReader() throws IOException {
        return readerClass.cast(provider.createReaderInstance(null));
    }

    protected abstract List<TestData> getTestData();

    protected abstract List<String> getFormatNames();

    protected abstract List<String> getSuffixes();

    protected abstract List<String> getMIMETypes();

    protected static void failBecause(String message, Throwable exception) {
        throw new AssertionError(message, exception);
    }

    protected void assertProviderInstalledForName(final String pFormat, final Class<? extends ImageReader> pReaderClass) {
        assertProviderInstalled0(pFormat.toUpperCase(), pReaderClass, ImageIO.getImageReadersByFormatName(pFormat.toUpperCase()));
        assertProviderInstalled0(pFormat.toLowerCase(), pReaderClass, ImageIO.getImageReadersByFormatName(pFormat.toLowerCase()));
    }

    protected void assertProviderInstalledForMIMEType(final String pType, final Class<? extends ImageReader> pReaderClass) {
        assertProviderInstalled0(pType, pReaderClass, ImageIO.getImageReadersByMIMEType(pType));
    }

    protected void assertProviderInstalledForSuffix(final String pType, final Class<? extends ImageReader> pReaderClass) {
        assertProviderInstalled0(pType, pReaderClass, ImageIO.getImageReadersBySuffix(pType));
    }

    private void assertProviderInstalled0(final String pFormat, final Class<? extends ImageReader> pReaderClass, final Iterator<ImageReader> pReaders) {
        boolean found = false;
        while (pReaders.hasNext()) {
            ImageReader reader = pReaders.next();
            if (reader.getClass() == pReaderClass && isOurProvider(reader.getOriginatingProvider())) {
                found = true;
            }
        }

        assertTrue(found, String.format("%s not provided by %s for '%s'", pReaderClass.getSimpleName(), provider.getClass().getSimpleName(), pFormat));
    }

    private boolean isOurProvider(final ImageReaderSpi spi) {
        return provider.getClass().isInstance(spi);
    }

    @Test
    public void testProviderInstalledForNames() {
        for (String name : getFormatNames()) {
            assertProviderInstalledForName(name, readerClass);
        }
    }

    @Test
    public void testProviderInstalledForSuffixes() {
        for (String suffix : getSuffixes()) {
            assertProviderInstalledForSuffix(suffix, readerClass);
        }
    }

    @Test
    public void testProviderInstalledForMIMETypes() {
        for (String type : getMIMETypes()) {
            assertProviderInstalledForMIMEType(type, readerClass);
        }
    }

    @Test
    public void testProviderCanRead() throws IOException {
        List<TestData> testData = getTestData();

        for (TestData data : testData) {
            ImageInputStream stream = data.getInputStream();
            assertNotNull(stream);
            assertTrue(provider.canDecodeInput(stream), "Provider is expected to be able to decode data: " + data);
        }
    }

    @Test
    public void testProviderCanReadNull() {
        boolean canRead = false;

        try {
            canRead = provider.canDecodeInput(null);
        }
        catch (IllegalArgumentException ignore) {
        }
        catch (RuntimeException e) {
            failBecause("RuntimeException other than IllegalArgumentException thrown", e);
        }
        catch (IOException e) {
            failBecause("Could not test data for read", e);
        }

        assertFalse(canRead, "ImageReader can read null input");
    }

    @Test
    public void testSetInput() throws IOException {
        // Should just pass with no exceptions
        ImageReader reader = createReader();
        assertNotNull(reader);

        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());
        }

        reader.dispose();
    }

    @Test
    public void testSetInputNull() throws IOException {
        // Should just pass with no exceptions
        ImageReader reader = createReader();
        assertNotNull(reader);
        reader.setInput(null);
        reader.dispose();
    }

    @Test
    public void testRead() throws IOException {
        ImageReader reader = createReader();

        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());

            for (int i = 0; i < data.getImageCount(); i++) {
                BufferedImage image = null;

                try {
                    image = reader.read(i);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    failBecause(String.format("Image %s index %s could not be read: %s", data.getInput(), i, e), e);
                }

                assertNotNull(image, String.format("Image %s index %s was null!", data.getInput(), i));

                assertEquals(
                        data.getDimension(i).width,
                        image.getWidth(),
                        String.format("Image %s index %s has wrong width: %s", data.getInput(), i, image.getWidth())
                );
                assertEquals(
                        data.getDimension(i).height, image.getHeight(),
                        String.format("Image %s index %s has wrong height: %s", data.getInput(), i, image.getHeight())
                );
            }
        }

        reader.dispose();
    }

    @Test
    public void testReadIndexNegative() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(-1);
            fail("Read image with illegal index");
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }
        assertNull(image);

        reader.dispose();
    }

    @Test
    public void testReadIndexOutOfBounds() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(Integer.MAX_VALUE); // TODO: This might actually not throw exception...
            fail("Read image with index out of bounds");
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }
        assertNull(image);

        reader.dispose();
    }

    @Test
    public void testReRead() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream(), false); // Explicitly allow repositioning, even though it's the default

        BufferedImage first = reader.read(0);
        assertNotNull(first);

        BufferedImage second = reader.read(0);
        assertNotNull(second);

        // TODO: These images should be exactly the same, but there's no equals for images
        assertEquals(first.getType(), second.getType());
        assertEquals(first.getWidth(), second.getWidth());
        assertEquals(first.getHeight(), second.getHeight());

        reader.dispose();
    }

    @Test
    public void testReadNoInput() throws IOException {
        ImageReader reader = createReader();
        // Do not set input

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reader.read(0);
        });
    }

    @Test
    public void testReadIndexNegativeWithParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        try {
            assertThrows(IndexOutOfBoundsException.class, () -> {
                reader.read(-1, reader.getDefaultReadParam());
            });
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadIndexOutOfBoundsWithParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        try {
            assertThrows(IndexOutOfBoundsException.class, () -> {
                reader.read(Short.MAX_VALUE, reader.getDefaultReadParam());
            });
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadNoInputWithParam() throws IOException {
        ImageReader reader = createReader();
        // Do not set input
        try {
            assertThrows(IllegalStateException.class, () -> {
                reader.read(0);
            });
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadWithNewParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(0, new ImageReadParam());
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNotNull(image, "Image was null!");
        assertEquals(data.getDimension(0).width, image.getWidth(), "Read image has wrong width: " + image.getWidth());
        assertEquals(data.getDimension(0).height, image.getHeight(), "Read image has wrong height: " + image.getHeight());

        reader.dispose();
    }

    @Test
    public void testReadWithDefaultParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(0, reader.getDefaultReadParam());
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNotNull(image, "Image was null!");
        assertEquals(data.getDimension(0).width, image.getWidth(), "Read image has wrong width: " + image.getWidth());
        assertEquals(data.getDimension(0).height, image.getHeight(), "Read image has wrong height: " + image.getHeight());

        reader.dispose();
    }

    @Test
    public void testReadWithNullParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(0, null);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNotNull(image, "Image was null!");
        assertEquals(data.getDimension(0).width, image.getWidth(), "Read image has wrong width: " + image.getWidth());
        assertEquals(data.getDimension(0).height, image.getHeight(), "Read image has wrong height: " + image.getHeight());

        reader.dispose();
    }

    @Test
    public void testReadWithSizeParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        ImageReadParam param = reader.getDefaultReadParam();

        if (param.canSetSourceRenderSize()) {
            param.setSourceRenderSize(new Dimension(10, 10));

            BufferedImage image = null;
            try {
                image = reader.read(0, param);
            }
            catch (IOException e) {
                failBecause("Image could not be read", e);
            }

            assertNotNull(image, "Image was null!");
            assertEquals(10, image.getWidth(), "Read image has wrong width: " + image.getWidth());
            assertEquals(10, image.getHeight(), "Read image has wrong height: " + image.getHeight());
        }

        reader.dispose();
    }

    @Test
    public void testReadWithSubsampleParamDimensions() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        ImageReadParam param = reader.getDefaultReadParam();

        param.setSourceSubsampling(5, 5, 0, 0);

        BufferedImage image = null;
        try {
            image = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNotNull(image, "Image was null!");
        assertEquals((data.getDimension(0).width + 4) / 5, image.getWidth(), "Read image has wrong width: ");
        assertEquals((data.getDimension(0).height + 4) / 5, image.getHeight(), "Read image has wrong height: ");

        reader.dispose();
    }

    @Test
    public void testReadWithSubsampleParamPixels() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        BufferedImage image = null;
        BufferedImage subsampled = null;
        try {
            image = reader.read(0, param);

            param.setSourceSubsampling(2, 2, 0, 0);
            subsampled = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertSubsampledImageDataEquals("Subsampled image data does not match expected", image, subsampled, param);

        reader.dispose();
    }

    // TODO: Subsample all test data
    // TODO: Subsample with varying ratios and offsets

    @SuppressWarnings("SameParameterValue")
    protected final void assertSubsampledImageDataEquals(String message, BufferedImage expected, BufferedImage actual, ImageReadParam param) throws IOException {
        assertNotNull(expected, "Expected image was null");
        assertNotNull(actual, "Actual image was null!");

        if (expected == actual) {
            return;
        }

        int xOff = param.getSubsamplingXOffset();
        int yOff = param.getSubsamplingYOffset();
        int xSub = param.getSourceXSubsampling();
        int ySub = param.getSourceYSubsampling();

        assertEquals((expected.getWidth() - xOff + xSub - 1) / xSub, actual.getWidth(), "Subsampled image has wrong width: ");
        assertEquals((expected.getHeight() - yOff + ySub - 1) / ySub, actual.getHeight(), "Subsampled image has wrong height: ");
        assertEquals(expected.getType(), actual.getType(), "Subsampled has different type");

        for (int y = 0; y < actual.getHeight(); y++) {
            for (int x = 0; x < actual.getWidth(); x++) {
                int expectedRGB = expected.getRGB(xOff + x * xSub, yOff + y * ySub);
                int actualRGB = actual.getRGB(x, y);

                try {
                    assertEquals((expectedRGB >>> 24) & 0xff, (actualRGB >>> 24) & 0xff, 5);
                    assertEquals((expectedRGB >> 16) & 0xff, (actualRGB >> 16) & 0xff, 5);
                    assertEquals((expectedRGB >> 8) & 0xff, (actualRGB >> 8) & 0xff, 5);
                    assertEquals(expectedRGB & 0xff, actualRGB & 0xff, 5);
                }
                catch (AssertionError e) {
                    File tempExpected = File.createTempFile("junit-expected-", ".png");
                    System.err.println("tempExpected.getAbsolutePath(): " + tempExpected.getAbsolutePath());
                    ImageIO.write(expected, "PNG", tempExpected);
                    File tempActual = File.createTempFile("junit-actual-", ".png");
                    System.err.println("tempActual.getAbsolutePath(): " + tempActual.getAbsolutePath());
                    ImageIO.write(actual, "PNG", tempActual);

                    assertEquals(String.format("#%08x", expectedRGB), String.format("#%08x", actualRGB), String.format("%s ARGB at (%d, %d)", message, x, y));
                }
            }
        }
    }

    public static void assertImageDataEquals(String message, BufferedImage expected, BufferedImage actual) {
        assertNotNull(expected, "Expected image was null");
        assertNotNull(actual, "Actual image was null!");

        if (expected == actual) {
            return;
        }

        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                int expectedRGB = expected.getRGB(x, y);
                int actualRGB = actual.getRGB(x, y);

                assertEquals((expectedRGB >> 24) & 0xff, (actualRGB >> 24) & 0xff, 5, String.format("%s alpha at (%d, %d)", message, x, y));
                assertEquals((expectedRGB >> 16) & 0xff, (actualRGB >> 16) & 0xff, 5, String.format("%s red at (%d, %d)", message, x, y));
                assertEquals((expectedRGB >> 8) & 0xff, (actualRGB >> 8) & 0xff, 5, String.format("%s green at (%d, %d)", message, x, y));
                assertEquals(expectedRGB & 0xff, actualRGB & 0xff, 5, String.format("%s blue at (%d, %d)", message, x, y));
            }
        }
    }

    @Test
    public void testReadWithSourceRegionParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        ImageReadParam param = reader.getDefaultReadParam();

        param.setSourceRegion(new Rectangle(0, 0, 10, 10));

        BufferedImage image = null;
        try {
            image = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNotNull(image, "Image was null!");
        assertEquals(10, image.getWidth(), "Read image has wrong width: " + image.getWidth());
        assertEquals(10, image.getHeight(), "Read image has wrong height: " + image.getHeight());

        reader.dispose();
    }

    @Test
    public void testReadWithSourceRegionParamEqualImage() throws IOException {
        // Default invocation
        assertReadWithSourceRegionParamEqualImage(new Rectangle(3, 3, 9, 9), getTestData().get(0), 0);
    }

    protected void assertReadWithSourceRegionParamEqualImage(final Rectangle r, final TestData data, @SuppressWarnings("SameParameterValue") final int imageIndex) throws IOException {
        ImageReader reader = createReader();
        try (ImageInputStream inputStream = data.getInputStream()) {
            reader.setInput(inputStream);
            ImageReadParam param = reader.getDefaultReadParam();

            // Read full image and get sub image for comparison
            BufferedImage original = reader.read(imageIndex, param);
            final BufferedImage roi = original.getSubimage(r.x, r.y, r.width, r.height);

            param.setSourceRegion(r);

            final BufferedImage image = reader.read(imageIndex, param);

            assertNotNull(image, "Image was null!");
            assertEquals(r.width, image.getWidth(), "Read image has wrong width: " + image.getWidth());
            assertEquals(r.height, image.getHeight(), "Read image has wrong height: " + image.getHeight());

            try {
                assertImageDataEquals("Images differ", roi, image);
            }
            catch (AssertionError e) {
                File tempExpected = File.createTempFile("junit-expected-", ".png");
                System.err.println("tempExpected.getAbsolutePath(): " + tempExpected.getAbsolutePath());

                Graphics2D graphics = original.createGraphics();
                try {
                    graphics.setColor(Color.RED);
                    graphics.draw(r);
                }
                finally {
                    graphics.dispose();
                }

                ImageIO.write(original, "PNG", tempExpected);
                File tempActual = File.createTempFile("junit-actual-", ".png");
                System.err.println("tempActual.getAbsolutePath(): " + tempActual.getAbsolutePath());
                ImageIO.write(image, "PNG", tempActual);

                throw e;
            }
        }
        finally {
            reader.dispose();
        }
    }

    @Test
    public void testReadWithSizeAndSourceRegionParam() throws IOException {
        // TODO: Is this test correct???
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        ImageReadParam param = reader.getDefaultReadParam();

        if (param.canSetSourceRenderSize()) {
            Dimension size = data.getDimension(0);
            size = new Dimension(size.width * 2, size.height * 2);

            param.setSourceRenderSize(size);
            param.setSourceRegion(new Rectangle(0, 0, 10, 10));

            BufferedImage image = null;
            try {
                image = reader.read(0, param);
            }
            catch (IOException e) {
                failBecause("Image could not be read", e);
            }

            assertNotNull(image, "Image was null!");
            assertEquals(10, image.getWidth(), "Read image has wrong width: " + image.getWidth());
            assertEquals(10, image.getHeight(), "Read image has wrong height: " + image.getHeight());
        }

        reader.dispose();
    }

    @Test
    public void testReadWithSubsampleAndSourceRegionParam() throws IOException {
        // NOTE: The "standard" (com.sun.imageio.plugin.*) ImageReaders pass
        // this test, so the test should be correct...
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        ImageReadParam param = reader.getDefaultReadParam();

        param.setSourceSubsampling(2, 2, 0, 0);
        param.setSourceRegion(new Rectangle(0, 0, 10, 10));

        BufferedImage image = null;
        try {
            image = reader.read(0, param);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }
        assertNotNull(image, "Image was null!");
        assertEquals(5, image.getWidth(), "Read image has wrong width: " + image.getWidth());
        assertEquals(5, image.getHeight(), "Read image has wrong height: " + image.getHeight());

        reader.dispose();
    }

    @Test
    public void testReadAsRenderedImageIndexNegative() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        RenderedImage image = null;
        try {
            image = reader.readAsRenderedImage(-1, reader.getDefaultReadParam());
            fail("Read image with illegal index");
        }
        catch (IndexOutOfBoundsException expected) {
            // Ignore
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNull(image);

        reader.dispose();
    }

    @Test
    public void testReadAsRenderedImageIndexOutOfBounds() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        RenderedImage image = null;
        try {
            image = reader.readAsRenderedImage(reader.getNumImages(true), reader.getDefaultReadParam());
            fail("Read image with index out of bounds");
        }
        catch (IndexOutOfBoundsException expected) {
            // Ignore
        }
        catch (IIOException e) {
            // Allow this to bubble up, due to a bug in the Sun JPEGImageReader
            throw e;
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNull(image);

        reader.dispose();
    }

    @Test
    public void testReadAsRenderedImageNoInput() throws IOException {
        ImageReader reader = createReader();
        // Do not set input

        RenderedImage image = null;
        try {
            image = reader.readAsRenderedImage(0, reader.getDefaultReadParam());
            fail("Read image with no input");
        }
        catch (IllegalStateException expected) {
            // Ignore
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNull(image);

        reader.dispose();
    }

    @Test
    public void testReadAsRenderedImage() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        RenderedImage image = null;
        try {
            image = reader.readAsRenderedImage(0, null);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNotNull(image, "Image was null!");
        assertEquals( data.getDimension(0).width, image.getWidth(),
                "Read image has wrong width: " + image.getWidth());
        assertEquals( data.getDimension(0).height, image.getHeight(),
                "Read image has wrong height: " + image.getHeight());

        reader.dispose();
    }

    @Test
    public void testReadAsRenderedImageWithDefaultParam() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        RenderedImage image = null;
        try {
            image = reader.readAsRenderedImage(0, reader.getDefaultReadParam());
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        assertNotNull(image, "Image was null!");
        assertEquals(data.getDimension(0).width, image.getWidth(),
                "Read image has wrong width: " + image.getWidth());
        assertEquals(data.getDimension(0).height, image.getHeight(),
                "Read image has wrong height: " + image.getHeight());

        reader.dispose();
    }

    @Test
    public void testGetDefaultReadParam() throws IOException {
        ImageReader reader = createReader();
        ImageReadParam param = reader.getDefaultReadParam();
        assertNotNull(param);
        reader.dispose();
    }

    @Test
    public void testGetFormatName() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        String name = null;
        try {
            name = reader.getFormatName();
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        assertNotNull(name);
        reader.dispose();
    }

    @Test
    public void testGetMinIndex() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        int num = 0;

        try {
            num = reader.getMinIndex();
        }
        catch (IllegalStateException ignore) {
        }
        assertEquals(0, num);
        reader.dispose();
    }

    @Test
    public void testGetMinIndexNoInput() throws IOException {
        ImageReader reader = createReader();
        int num = 0;

        try {
            num = reader.getMinIndex();
        }
        catch (IllegalStateException ignore) {
        }
        assertEquals(0, num);
        reader.dispose();
    }

    @Test
    public void testGetNumImages() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        int num = -1;
        try {
            num = reader.getNumImages(false);
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        assertTrue(num == -1 || num > 0);

        try {
            num = reader.getNumImages(true);
        }
        catch (IOException e) {
            fail(e.getMessage());
        }

        assertTrue(num > 0);
        assertTrue(data.getImageCount() <= num);
        if (data.getImageCount() != num) {
            System.err.println("WARNING: Image count not equal to test data count");
        }
        reader.dispose();
    }

    @Test
    public void testGetNumImagesNoInput() throws IOException {
        ImageReader reader = createReader();
        int num = -1;

        try {
            num = reader.getNumImages(false);
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(-1, num);

        try {
            num = reader.getNumImages(true);
            fail("Should throw IllegalStateException");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(-1, num);
        reader.dispose();
    }

    @Test
    public void testGetWidth() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        int width = 0;
        try {
            width = reader.getWidth(0);
        }
        catch (IOException e) {
            fail("Could not read image width: " + e);
        }
        assertEquals(data.getDimension(0).width, width, "Wrong width reported");
        reader.dispose();
    }

    @Test
    public void testGetWidthIndexOutOfBounds() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        //int width = 0;
        try {
            /*width = */reader.getWidth(-1);
            // NOTE: Some readers (like the com.sun.imageio stuff) ignores
            // index in getWidth/getHeight for formats with only one image...
            //assertEquals("Wrong width reported", data.getDimension().width, width);
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image aspect ratio: " + e);
        }
        reader.dispose();
    }

    @Test
    public void testGetWidthNoInput() throws IOException {
        ImageReader reader = createReader();

        int width = 0;
        try {
            width = reader.getWidth(0);
            fail("Width read without input");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image width: " + e);
        }
        assertEquals(0, width, "Wrong width reported");
        reader.dispose();
    }

    @Test
    public void testGetHeight() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        int height = 0;
        try {
            height = reader.getHeight(0);
        }
        catch (IOException e) {
            fail("Could not read image height: " + e);
        }
        assertEquals(data.getDimension(0).height, height, "Wrong height reported");
        reader.dispose();
    }

    @Test
    public void testGetHeightNoInput() throws IOException {
        ImageReader reader = createReader();

        int height = 0;
        try {
            height = reader.getHeight(0);
            fail("height read without input");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image height: " + e);
        }
        assertEquals(0, height, "Wrong height reported");
        reader.dispose();
    }

    @Test
    public void testGetHeightIndexOutOfBounds() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        //int height = 0;
        try {
            /*height =*/ reader.getHeight(-1);
            // NOTE: Some readers (like the com.sun.imageio stuff) ignores
            // index in getWidth/getHeight for formats with only one image...
            //assertEquals("Wrong height reported", data.getDimension().height, height);
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image height: " + e);
        }
        reader.dispose();
    }

    @Test
    public void testGetAspectRatio() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        float aspectRatio = 0f;
        try {
            aspectRatio = reader.getAspectRatio(0);
        }
        catch (IOException e) {
            fail("Could not read image aspect ratio" + e);
        }
        Dimension d = data.getDimension(0);
        assertEquals(d.getWidth() / d.getHeight(), aspectRatio, 0.001, "Wrong aspect aspect ratio");
        reader.dispose();
    }

    @Test
    public void testGetAspectRatioNoInput() throws IOException {
        ImageReader reader = createReader();

        float aspectRatio = 0f;
        try {
            aspectRatio = reader.getAspectRatio(0);
            fail("aspect read without input");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image aspect ratio" + e);
        }
        assertEquals(0f, aspectRatio, 0f, "Wrong aspect aspect ratio");
        reader.dispose();
    }

    @Test
    public void testGetAspectRatioIndexOutOfBounds() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        //float aspectRatio = 0f;
        try {
            // NOTE: Some readers (like the com.sun.imageio stuff) ignores
            // index in getWidth/getHeight for formats with only one image...
            /*aspectRatio =*/ reader.getAspectRatio(-1);
            //assertEquals("Wrong aspect ratio", data.getDimension().width / (float) data.getDimension().height, aspectRatio, 0f);
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image aspect ratio" + e);
        }
        reader.dispose();
    }

    @Test
    public void testDisposeBeforeRead() throws IOException {
        ImageReader reader = createReader();
        reader.dispose(); // Just pass with no exceptions
    }

    @Test
    public void testDisposeAfterRead() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        reader.dispose(); // Just pass with no exceptions
    }

    @Test
    public void testAddIIOReadProgressListener() throws IOException {
        ImageReader reader = createReader();
        reader.addIIOReadProgressListener(mock(IIOReadProgressListener.class));
        reader.dispose();
    }

    @Test
    public void testAddIIOReadProgressListenerNull() throws IOException {
        ImageReader reader = createReader();
        reader.addIIOReadProgressListener(null);
        reader.dispose();
    }

    @Test
    public void testAddIIOReadProgressListenerCallbacks() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        InOrder ordered = inOrder(listener);
        ordered.verify(listener).imageStarted(reader, 0);
        ordered.verify(listener, atLeastOnce()).imageProgress(eq(reader), anyFloat());
        ordered.verify(listener).imageComplete(reader);
        reader.dispose();
    }

    @Test
    public void testMultipleAddIIOReadProgressListenerCallbacks() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        IIOReadProgressListener listenerToo = mock(IIOReadProgressListener.class);
        IIOReadProgressListener listenerThree = mock(IIOReadProgressListener.class);

        reader.addIIOReadProgressListener(listener);
        reader.addIIOReadProgressListener(listenerToo);
        reader.addIIOReadProgressListener(listenerThree);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        InOrder ordered = inOrder(listener, listenerToo, listenerThree);

        ordered.verify(listener).imageStarted(reader, 0);
        ordered.verify(listenerToo).imageStarted(reader, 0);
        ordered.verify(listenerThree).imageStarted(reader, 0);

        ordered.verify(listener, atLeastOnce()).imageProgress(eq(reader), anyFloat());
        ordered.verify(listenerToo, atLeastOnce()).imageProgress(eq(reader), anyFloat());
        ordered.verify(listenerThree, atLeastOnce()).imageProgress(eq(reader), anyFloat());

        ordered.verify(listener).imageComplete(reader);
        ordered.verify(listenerToo).imageComplete(reader);
        ordered.verify(listenerThree).imageComplete(reader);
        reader.dispose();
    }

    @Test
    public void testRemoveIIOReadProgressListenerNull() throws IOException {
        ImageReader reader = createReader();
        reader.removeIIOReadProgressListener(null);
        reader.dispose();
    }

    @Test
    public void testRemoveIIOReadProgressListenerNone() throws IOException {
        ImageReader reader = createReader();
        reader.removeIIOReadProgressListener(mock(IIOReadProgressListener.class));
        reader.dispose();
    }

    @Test
    public void testRemoveIIOReadProgressListener() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listener);
        reader.removeIIOReadProgressListener(listener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods...
        verifyNoInteractions(listener);
        reader.dispose();
    }

    @Test
    public void testRemoveIIOReadProgressListenerMultiple() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class, "Listener1");
        reader.addIIOReadProgressListener(listener);


        IIOReadProgressListener listenerToo = mock(IIOReadProgressListener.class, "Listener2");
        reader.addIIOReadProgressListener(listenerToo);

        reader.removeIIOReadProgressListener(listener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods on listener1...
        verifyNoInteractions(listener);

        InOrder ordered = inOrder(listenerToo);
        ordered.verify(listenerToo).imageStarted(reader, 0);
        ordered.verify(listenerToo, atLeastOnce()).imageProgress(eq(reader), anyFloat());
        ordered.verify(listenerToo).imageComplete(reader);
        reader.dispose();
    }

    @Test
    public void testRemoveAllIIOReadProgressListeners() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listener);

        reader.removeAllIIOReadProgressListeners();

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods...
        verifyNoInteractions(listener);
        reader.dispose();
    }

    @Test
    public void testRemoveAllIIOReadProgressListenersMultiple() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listener);

        IIOReadProgressListener listenerToo = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listenerToo);

        reader.removeAllIIOReadProgressListeners();

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods...
        verifyNoInteractions(listener);
        verifyNoInteractions(listenerToo);
        reader.dispose();
    }

    @Test
    public void testAbort() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class, "Progress1");
        reader.addIIOReadProgressListener(listener);

        IIOReadProgressListener listenerToo = mock(IIOReadProgressListener.class, "Progress2");
        reader.addIIOReadProgressListener(listenerToo);

        // Create a listener that just makes the reader abort immediately...
        IIOReadProgressListener abortingListener = mock(IIOReadProgressListener.class, "Aborter");
        Answer<Void> abort = new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                reader.abort();
                return null;
            }
        };
        doAnswer(abort).when(abortingListener).imageStarted(any(ImageReader.class), anyInt());
        doAnswer(abort).when(abortingListener).imageProgress(any(ImageReader.class), anyFloat());

        reader.addIIOReadProgressListener(abortingListener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            failBecause("Image could not be read", e);
        }

        verify(listener).readAborted(reader);
        verify(listenerToo).readAborted(reader);
        reader.dispose();
    }

    @Test
    public void testGetTypeSpecifiers() throws IOException {
        final ImageReader reader = createReader();
        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());

            ImageTypeSpecifier rawType = reader.getRawImageType(0);
            assertNotNull(rawType);

            Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);

            assertNotNull(types);
            assertTrue(types.hasNext());

            // TODO: This might fail even though the specifiers are obviously equal, if the
            // color spaces they use are not the SAME instance, as ColorSpace uses identity equals
            // and Interleaved ImageTypeSpecifiers are only equal if color spaces are equal...
            boolean rawFound = false;
            while (types.hasNext()) {
                ImageTypeSpecifier type = types.next();
                if (type.equals(rawType)) {
                    rawFound = true;
                    break;
                }
            }

            assertTrue(rawFound, "ImageTypeSpecifier from getRawImageType should be in the iterator from getImageTypes");
        }

        reader.dispose();
    }

    @Test
    public void testSetDestination() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        Dimension size = data.getDimension(0);

        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();
        Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
        while (types.hasNext()) {
            ImageTypeSpecifier type = types.next();

            BufferedImage destination = type.createBufferedImage(size.width, size.height);
            param.setDestination(destination);

            BufferedImage result = null;
            try {
                result = reader.read(0, param);
            }
            catch (Exception e) {
                failBecause("Could not read " + data.getInput() + " with explicit destination " + destination, e);
            }

            assertSame(destination, result);
        }
        reader.dispose();
    }

    @Test
    public void testSetDestinationRaw() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        ImageTypeSpecifier type = reader.getRawImageType(0);

        if (type != null) {
            BufferedImage destination = type.createBufferedImage(reader.getWidth(0), reader.getHeight(0));
            param.setDestination(destination);

            BufferedImage result = null;
            try {
                result = reader.read(0, param);
            }
            catch (Exception e) {
                failBecause("Image could not be read", e);
            }

            assertSame(destination, result);
        }
        else {
            System.err.println("WARNING: Test skipped due to reader.getRawImageType(0) returning null");
        }
        reader.dispose();
    }

    @Test
    public void testSetDestinationIllegal() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        Dimension size = data.getDimension(0);

        reader.setInput(data.getInputStream());

        List<ImageTypeSpecifier> illegalTypes = createIllegalTypes(reader.getImageTypes(0));

        ImageReadParam param = reader.getDefaultReadParam();
        for (ImageTypeSpecifier illegalType : illegalTypes) {
            BufferedImage destination = illegalType.createBufferedImage(size.width, size.height);
            param.setDestination(destination);

            try {
                BufferedImage result = reader.read(0, param);

                // NOTE: We allow the reader to read, as it's inconvenient to test all possible cases.
                // However, it may NOT fail with any other exception in that case.
                // TODO: Special case for BufferedImage type 2/3 and 6/7
                System.err.println("WARNING: Reader does not throw exception with non-declared destination: " + destination);

                // Test that the destination is really taken into account
                assertSame(destination, result);
            }
            catch (IIOException expected) {
                // TODO: This is thrown by ImageReader.getDestination. But are we happy with that?
                // The problem is that the checkReadParamBandSettings throws IllegalArgumentException, which seems more appropriate...
                String message = expected.getMessage().toLowerCase();
                if (!(message.contains("destination") || message.contains("band size") || // For JDK classes
                        ((destination.getType() == BufferedImage.TYPE_BYTE_BINARY ||
                                destination.getType() == BufferedImage.TYPE_BYTE_INDEXED) &&
                                message.contains("indexcolormodel")))) {
                    failBecause(
                            "Wrong message: " + message + " for type " + destination.getType(), expected
                    );
                }
            }
            catch (IllegalArgumentException expected) {
                String message = expected.getMessage().toLowerCase();
                assertTrue(message.contains("dest"), "Wrong message: " + message);
            }
        }
        reader.dispose();
    }

    @Test
    public void testSetDestinationTypeIllegal() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        List<ImageTypeSpecifier> illegalTypes = createIllegalTypes(reader.getImageTypes(0));

        ImageReadParam param = reader.getDefaultReadParam();
        for (ImageTypeSpecifier illegalType : illegalTypes) {
            param.setDestinationType(illegalType);

            try {
                reader.read(0, param);
                fail("Expected to throw exception with illegal type specifier");
            }
            catch (IIOException | IllegalArgumentException expected) {
                // TODO: This is thrown by ImageReader.getDestination. But are we happy with that?
                String message = expected.getMessage().toLowerCase();
                if (!(message.contains("destination") && message.contains("type")
                        || message.contains("num source & dest bands differ"))) {
                    // Allow this to bubble up, due to a bug in the Sun PNGImageReader
                    throw expected;
                }
            }
        }
        reader.dispose();
    }

    private List<ImageTypeSpecifier> createIllegalTypes(Iterator<ImageTypeSpecifier> pValidTypes) {
        List<ImageTypeSpecifier> allTypes = new ArrayList<>();
        for (int i = BufferedImage.TYPE_INT_RGB; i < BufferedImage.TYPE_BYTE_INDEXED; i++) {
            allTypes.add(ImageTypeSpecifier.createFromBufferedImageType(i));
        }

        List<ImageTypeSpecifier> illegalTypes = new ArrayList<>(allTypes);
        while (pValidTypes.hasNext()) {
            ImageTypeSpecifier valid = pValidTypes.next();
            boolean removed = illegalTypes.remove(valid);

            // TODO: 4BYTE_ABGR (6) and 4BYTE_ABGR_PRE (7) is essentially the same type...
            // #$@*%$! ImageTypeSpecifier.equals is not well-defined
            if (!removed) {
                for (Iterator<ImageTypeSpecifier> iterator = illegalTypes.iterator(); iterator.hasNext();) {
                    ImageTypeSpecifier illegalType = iterator.next();
                    if (illegalType.getBufferedImageType() == valid.getBufferedImageType()) {
                        iterator.remove();
                    }
                }
            }
        }

        return illegalTypes;
    }

    // TODO: Test dest offset + destination set?
    // TODO: Test that destination offset is used for image data, not just image dimensions...
    @Test
    public void testSetDestinationOffset() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();
        Point point = new Point(37, 42);
        param.setDestinationOffset(point);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(reader.getWidth(0) + point.x, image.getWidth());
        assertEquals(reader.getHeight(0) + point.y, image.getHeight());
        reader.dispose();
    }

    @Test
    public void testSetDestinationOffsetNull() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();
        try {
            param.setDestinationOffset(null);
            fail("Null offset not allowed");
        }
        catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("offset"));
        }
        reader.dispose();
    }

    @Test
    public void testSetDestinationType() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
        while (types.hasNext()) {
            ImageTypeSpecifier type = types.next();
            param.setDestinationType(type);

            BufferedImage result = null;
            try {
                result = reader.read(0, param);
            }
            catch (Exception e) {
                failBecause("Could not read " + data.getInput() + " with explicit destination type " + type, e);
            }

            assertNotNull(result);
            assertEquals(type.getColorModel(), result.getColorModel());

            // The following logically tests
            // assertEquals(type.getSampleModel(), result.getSampleModel());
            // but SampleModel does not have a proper equals method.
            SampleModel expectedModel = type.getSampleModel();
            SampleModel resultModel = result.getSampleModel();

            assertEquals(expectedModel.getDataType(), resultModel.getDataType());
            assertEquals(expectedModel.getNumBands(), resultModel.getNumBands());
            assertEquals(expectedModel.getNumDataElements(), resultModel.getNumDataElements());
            assertArrayEquals(expectedModel.getSampleSize(), resultModel.getSampleSize());
            assertEquals(expectedModel.getTransferType(), resultModel.getTransferType());
            for (int i = 0; i < expectedModel.getNumBands(); i++) {
                assertEquals(expectedModel.getSampleSize(i), resultModel.getSampleSize(i));
            }
        }
        reader.dispose();
    }

    @Test
    public void testNotBadCaching() throws IOException {
        T reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage one = reader.read(0);
        BufferedImage two = reader.read(0);

        // Test for same BufferedImage instance
        assertNotSame(one, two, "Multiple reads return same (mutable) image");

        // Test for same backing storage (array)
        one.setRGB(0, 0, Color.BLACK.getRGB());
        two.setRGB(0, 0, Color.WHITE.getRGB());
        assertTrue(one.getRGB(0, 0) != two.getRGB(0, 0));

        reader.dispose();
    }

    @Test
    public void testReadThumbnails() throws IOException {
        T reader = createReader();

        if (reader.readerSupportsThumbnails()) {
            for (TestData testData : getTestData()) {
                try (ImageInputStream inputStream = testData.getInputStream()) {
                    reader.setInput(inputStream);

                    int numImages = reader.getNumImages(true);

                    for (int i = 0; i < numImages; i++) {
                        int numThumbnails = reader.getNumThumbnails(0);

                        for (int t = 0; t < numThumbnails; t++) {
                            BufferedImage thumbnail = reader.readThumbnail(0, t);

                            assertNotNull(thumbnail);
                        }
                    }
                }
            }
        }

        reader.dispose();
    }

    @Test
    public void testThumbnailProgress() throws IOException {
        T reader = createReader();

        IIOReadProgressListener listener = mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener(listener);

        if (reader.readerSupportsThumbnails()) {
            for (TestData testData : getTestData()) {
                try (ImageInputStream inputStream = testData.getInputStream()) {

                    reader.setInput(inputStream);

                    int numThumbnails = reader.getNumThumbnails(0);
                    for (int i = 0; i < numThumbnails; i++) {
                        reset(listener);

                        reader.readThumbnail(0, i);

                        InOrder order = inOrder(listener);
                        order.verify(listener).thumbnailStarted(reader, 0, i);
                        order.verify(listener, atLeastOnce()).thumbnailProgress(reader, 100f);
                        order.verify(listener).thumbnailComplete(reader);
                    }
                }
            }
        }

        reader.dispose();
    }

    @Test
    public void testNotBadCachingThumbnails() throws IOException {
        T reader = createReader();

        if (reader.readerSupportsThumbnails()) {
            for (TestData data : getTestData()) {
                reader.setInput(data.getInputStream());

                int images = reader.getNumImages(true);
                for (int i = 0; i < images; i++) {
                    int thumbnails = reader.getNumThumbnails(0);

                    for (int j = 0; j < thumbnails; j++) {
                        BufferedImage one = reader.readThumbnail(i, j);
                        BufferedImage two = reader.readThumbnail(i, j);

                        assertNotSame(one, two, "Multiple reads return same (mutable) image");

                        Graphics2D g = one.createGraphics();
                        try {
                            g.setColor(Color.WHITE);
                            g.setXORMode(Color.BLACK);
                            g.fillRect(0, 0, one.getWidth(), one.getHeight());
                        }
                        finally {
                            g.dispose();
                        }

                        assertTrue(one.getRGB(0, 0) != two.getRGB(0, 0));
                    }

                    if (thumbnails > 0) {
                        // We've tested thumbnails, let's get out of here
                        return;
                    }
                }
            }

            fail("No thumbnails tested for reader that supports thumbnails.");
        }
        reader.dispose();
    }

    protected List<TestData> getTestDataForAffineTransformOpCompatibility() {
        // Allow subclasses to filter out test data that can't be converted to a compatible image without data loss
        return getTestData();
    }

    @Test
    public void testAffineTransformOpCompatibility() throws IOException {
        // Test that the output of normal images are compatible with AffineTransformOp. Is unlikely to work on all test data
        ImageReader reader = createReader();

        for (TestData testData : getTestDataForAffineTransformOpCompatibility()) {
            Dimension size = testData.getDimension(0);
            try (ImageInputStream input = testData.getInputStream()) {
                reader.setInput(input);

                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceRegion(new Rectangle(size.width, size.height));

                BufferedImage originalImage = reader.read(0, param);

                AffineTransform transform = AffineTransform.getTranslateInstance(10, 10);
                AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

                try {
                    BufferedImage resultImage = op.filter(originalImage, null); // The exception happens here
                    assertNotNull(resultImage);
                }
                catch (ImagingOpException e) {
                    fail(e.getMessage() + ".\n\t"
                            + originalImage + "\n\t"
                            + testData);
                }
            }
        }

        reader.dispose();
    }

    @Disabled("TODO: Implement")
    @Test
    public void testSetDestinationBands() {
        throw new UnsupportedOperationException("Method testSetDestinationBands not implemented"); // TODO: Implement
    }

    @Disabled("TODO: Implement")
    @Test
    public void testSetSourceBands() {
        throw new UnsupportedOperationException("Method testSetDestinationBands not implemented"); // TODO: Implement
    }

    @Test
    public void testProviderAndMetadataFormatNamesMatch() throws IOException {
        ImageReader reader = createReader();
        reader.setInput(getTestData().get(0).getInputStream());

        IIOMetadata imageMetadata = reader.getImageMetadata(0);
        if (imageMetadata != null) {
            assertEquals(provider.getNativeImageMetadataFormatName(), imageMetadata.getNativeMetadataFormatName());
        }

        IIOMetadata streamMetadata = reader.getStreamMetadata();
        if (streamMetadata != null) {
            assertEquals(provider.getNativeStreamMetadataFormatName(), streamMetadata.getNativeMetadataFormatName());
        }
        reader.dispose();
    }

    protected URL getClassLoaderResource(final String pName) {
        return getClass().getResource(pName);
    }

    /**
     * Slightly fuzzy RGB equals method. Variable tolerance.
     */
    public static void assertRGBEquals(String message, int expectedRGB, int actualRGB, int tolerance) {
        try {
            assertEquals((expectedRGB >>> 24) & 0xff, (actualRGB >>> 24) & 0xff, 0);
            assertEquals((expectedRGB >>  16) & 0xff, (actualRGB >>  16) & 0xff, tolerance);
            assertEquals((expectedRGB >>   8) & 0xff, (actualRGB >>   8) & 0xff, tolerance);
            assertEquals((expectedRGB       ) & 0xff, (actualRGB       ) & 0xff, tolerance);
        }
        catch (AssertionError e) {
            assertEquals(message, String.format("#%08x", expectedRGB), String.format("#%08x", actualRGB));
        }
    }

    static final protected class TestData {
        private final Object input;
        private final List<Dimension> sizes;
        private final List<BufferedImage> images;

        public TestData(final Object input, final Dimension... dimensions) {
            this(input, Arrays.asList(dimensions), null);
        }

        public TestData(final Object input, final BufferedImage... images) {
            this(input, null, Arrays.asList(images));
        }

        public TestData(final Object input, final List<Dimension> dimensions, final List<BufferedImage> images) {
            Validate.notNull(input, "input");
            Validate.isTrue(dimensions != null || images != null, "Need either dimensions or image");

            List<Dimension> combinedDimensions;
            if (dimensions == null) {
                // Copy dimensions  from images
                combinedDimensions = new ArrayList<>(images.size());

                for (BufferedImage image : images) {
                    combinedDimensions.add(new Dimension(image.getWidth(), image.getHeight()));
                }
            }
            else {
                // Validate equal dimensions
                if (images != null) {
                    if (images.size() != dimensions.size()) {
                        throw new IllegalArgumentException("Dimensions and images parameter's size differs");
                    }

                    for (int i = 0; i < dimensions.size(); i++) {
                        if (!new Dimension(images.get(i).getWidth(), images.get(i).getHeight()).equals(dimensions.get(i))) {
                            throw new IllegalArgumentException("Dimensions and images parameter's dimensions differ");
                        }
                    }
                }

                combinedDimensions = new ArrayList<>(dimensions);
            }

            this.sizes = Collections.unmodifiableList(combinedDimensions);

            this.images = images != null
                    ? Collections.unmodifiableList(new ArrayList<>(images))
                    : Collections.<BufferedImage>emptyList();

            this.input = input;
        }

        public Object getInput() {
            return input;
        }

        public ImageInputStream getInputStream() {
            try {
                ImageInputStream stream = ImageIO.createImageInputStream(input);
                assertNotNull(stream, "Could not create ImageInputStream for input: " + input);

                return stream;
            }
            catch (IOException e) {
                failBecause("Could not create ImageInputStream for input: " + input, e);
            }

            return null;
        }

        public int getImageCount() {
            return sizes.size();
        }

        public Dimension getDimension(final int index) {
            return sizes.get(index);
        }

        @SuppressWarnings("unused")
        public BufferedImage getImage(final int index) {
            return images.get(index);
        }

        @Override
        public String toString() {
            return String.format("%s: %s", getClass().getSimpleName(), input);
        }
    }
}
