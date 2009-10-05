/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.imageio.util;

import com.twelvemonkeys.imageio.stream.URLImageInputStreamSpi;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;

import javax.imageio.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * ImageReaderAbstractTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: ImageReaderAbstractTestCase.java,v 1.0 Apr 1, 2008 10:36:46 PM haraldk Exp$
 */
public abstract class ImageReaderAbstractTestCase<T extends ImageReader> extends MockObjectTestCase {
    // TODO: Should we really test if he provider is installed?
    //       - Pro: Tests the META-INF/services config
    //       - Con: Not all providers should be installed at runtime...

    static {
        IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());
    }

    protected abstract List<TestData> getTestData();

    /**
     * Convenience method to get a list of test files from the classpath.
     * Currently only works for resources on the filesystem (not in jars or
     * archives).
     *
     * @param pResourceInFolder a resource in the correct classpath folder.
     * @return a list of files
     */
    protected final List<File> getInputsFromClasspath(final String pResourceInFolder) {
        URL resource = getClass().getClassLoader().getResource(pResourceInFolder);
        assertNotNull(resource);
        File dir;
        try {
            dir = new File(resource.toURI()).getParentFile();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        List<File> files = Arrays.asList(dir.listFiles());
        assertFalse(files.isEmpty());
        return files;
    }

    protected abstract ImageReaderSpi createProvider();

    protected abstract Class<T> getReaderClass();

    protected T createReader() {
        try {
            return getReaderClass().newInstance();
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract List<String> getFormatNames();

    protected abstract List<String> getSuffixes();

    protected abstract List<String> getMIMETypes();

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
            if (reader.getClass() == pReaderClass) {
                found = true;
            }
        }

        assertTrue(pReaderClass.getSimpleName() + " not installed for " + pFormat, found);
    }

    public void testProviderInstalledForNames() {
        Class<? extends ImageReader> readerClass = getReaderClass();
        for (String name : getFormatNames()) {
            assertProviderInstalledForName(name, readerClass);
        }
    }

    public void testProviderInstalledForSuffixes() {
        Class<? extends ImageReader> readerClass = getReaderClass();
        for (String suffix : getSuffixes()) {
            assertProviderInstalledForSuffix(suffix, readerClass);
        }
    }

    public void testProviderInstalledForMIMETypes() {
        Class<? extends ImageReader> readerClass = getReaderClass();
        for (String type : getMIMETypes()) {
            assertProviderInstalledForMIMEType(type, readerClass);
        }
    }

    public void testProviderCanRead() throws IOException {
        List<TestData> testData = getTestData();

        ImageReaderSpi provider = createProvider();
        for (TestData data : testData) {
            ImageInputStream stream = data.getInputStream();
            assertNotNull(stream);
            assertTrue("Provider is expected to be able to decode data: " + data, provider.canDecodeInput(stream));
        }
    }

    public void testProviderCanReadNull() {
        boolean canRead = false;
        try {
            canRead = createProvider().canDecodeInput(null);
        }
        catch (IllegalArgumentException ignore) {
        }
        catch (RuntimeException e) {
            fail("RuntimeException other than IllegalArgumentException thrown: " + e);
        }
        catch (IOException e) {
            fail("Could not test data for read: " + e);
        }
        assertFalse("ImageReader can read null input", canRead);
    }

    public void testSetInput() {
        // Should just pass with no exceptions
        ImageReader reader = createReader();
        assertNotNull(reader);
        for (TestData data : getTestData()) {
            reader.setInput(data.getInputStream());
        }
    }

    public void testSetInputNull() {
        // Should just pass with no exceptions
        ImageReader reader = createReader();
        assertNotNull(reader);
        reader.setInput(null);
    }

    public void testRead() {
        ImageReader reader = createReader();
        for (TestData data : getTestData()) {
            // TODO: Is it required to call reset before setInput?
            reader.setInput(data.getInputStream());

            // TODO: Require count to match?
//            System.out.println("reader.getNumImages(true): " + reader.getNumImages(true));

            for (int i = 0; i < data.getImageCount(); i++) {
                BufferedImage image = null;
                try {
                    image = reader.read(i);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    fail(String.format("Image %s index %s could not be read: %s", data.getInput(), i, e));
                }
                assertNotNull(String.format("Image %s index %s was null!", data.getInput(), i), image);

                assertEquals(
                        String.format("Image %s index %s has wrong width: %s", data.getInput(), i, image.getWidth()),
                        data.getDimension(i).width,
                        image.getWidth()
                );
                assertEquals(
                        String.format("Image %s index %s has wrong height: %s", data.getInput(), i, image.getHeight()),
                        data.getDimension(i).height, image.getHeight()
                );
            }
        }
    }

    public void testReadIndexNegative() {
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
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadIndexOutOfBounds() {
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
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadNoInput() {
        ImageReader reader = createReader();
        // Do not set input

        BufferedImage image = null;
        try {
            image = reader.read(0);
            fail("Read image with no input");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

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
    }

    public void testReadIndexNegativeWithParam() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(-1, reader.getDefaultReadParam());
            fail("Read image with illegal index");
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadIndexOutOfBoundsWithParam() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(99, reader.getDefaultReadParam());
            fail("Read image with index out of bounds");
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadNoInputWithParam() {
        ImageReader reader = createReader();
        // Do not set input

        BufferedImage image = null;
        try {
            image = reader.read(0, reader.getDefaultReadParam());
            fail("Read image with no input");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadWithNewParam() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(0, new ImageReadParam());
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: " + image.getWidth(),
                data.getDimension(0).width, image.getWidth());
        assertEquals("Read image has wrong height: " + image.getHeight(),
                data.getDimension(0).height, image.getHeight());
    }

    public void testReadWithDefaultParam() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(0, reader.getDefaultReadParam());
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: " + image.getWidth(),
                data.getDimension(0).width, image.getWidth());
        assertEquals("Read image has wrong height: " + image.getHeight(),
                data.getDimension(0).height, image.getHeight());
    }

    public void testReadWithNullParam() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        BufferedImage image = null;
        try {
            image = reader.read(0, null);
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: " + image.getWidth(),
                data.getDimension(0).width, image.getWidth());
        assertEquals("Read image has wrong height: " + image.getHeight(),
                data.getDimension(0).height, image.getHeight());
    }

    public void testReadWithSizeParam() {
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
                fail("Image could not be read: " + e);
            }
            assertNotNull("Image was null!", image);
            assertEquals("Read image has wrong width: " + image.getWidth(),
                    10, image.getWidth());
            assertEquals("Read image has wrong height: " + image.getHeight(),
                    10, image.getHeight());
        }
    }

    public void testReadWithSubsampleParam() {
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
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: ",
                (double) data.getDimension(0).width / 5.0, image.getWidth(), 1.0);
        assertEquals("Read image has wrong height: ",
                (double) data.getDimension(0).height / 5.0, image.getHeight(), 1.0);
    }

    public void testReadWithSourceRegionParam() {
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
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: " + image.getWidth(),
                10, image.getWidth());
        assertEquals("Read image has wrong height: " + image.getHeight(),
                10, image.getHeight());
    }

    public void testReadWithSizeAndSourceRegionParam() {
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
                fail("Image could not be read: " + e);
            }
            assertNotNull("Image was null!", image);
            assertEquals("Read image has wrong width: " + image.getWidth(),
                    20, image.getWidth());
            assertEquals("Read image has wrong height: " + image.getHeight(),
                    20, image.getHeight());
        }
    }

    public void testReadWithSubsampleAndSourceRegionParam() {
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
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: " + image.getWidth(),
                5, image.getWidth());
        assertEquals("Read image has wrong height: " + image.getHeight(),
                5, image.getHeight());

    }

    public void testReadAsRenderedImageIndexNegative() {
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
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadAsRenderedImageIndexOutOfBounds() {
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
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadAsRenderedImageNoInput() {
        ImageReader reader = createReader();
        // Do not set input

        BufferedImage image = null;
        try {
            image = reader.read(0, reader.getDefaultReadParam());
            fail("Read image with no input");
        }
        catch (IllegalStateException expected) {
            // Ignore
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNull(image);
    }

    public void testReadAsRenderedImage() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        RenderedImage image = null;
        try {
            image = reader.readAsRenderedImage(0, null);
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: " + image.getWidth(),
                data.getDimension(0).width, image.getWidth());
        assertEquals("Read image has wrong height: " + image.getHeight(),
                data.getDimension(0).height, image.getHeight());
    }

    public void testReadAsRenderedImageWithDefaultParam() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        RenderedImage image = null;
        try {
            image = reader.readAsRenderedImage(0, reader.getDefaultReadParam());
        }
        catch (IOException e) {
            fail("Image could not be read: " + e);
        }
        assertNotNull("Image was null!", image);
        assertEquals("Read image has wrong width: " + image.getWidth(),
                data.getDimension(0).width, image.getWidth());
        assertEquals("Read image has wrong height: " + image.getHeight(),
                data.getDimension(0).height, image.getHeight());
    }

    public void testGetDefaultReadParam() {
        ImageReader reader = createReader();
        ImageReadParam param = reader.getDefaultReadParam();
        assertNotNull(param);
    }

    public void testGetFormatName() {
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
    }

    public void testGetMinIndex() {
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
    }

    public void testGetMinIndexNoInput() {
        ImageReader reader = createReader();
        int num = 0;

        try {
            num = reader.getMinIndex();
        }
        catch (IllegalStateException ignore) {
        }
        assertEquals(0, num);
    }

    public void testGetNumImages() {
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
    }

    public void testGetNumImagesNoInput() {
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
    }

    public void testGetWidth() {
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
        assertEquals("Wrong width reported", data.getDimension(0).width, width);
    }

    public void testGetWidthIndexOutOfBounds() {
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
    }

    public void testGetWidthNoInput() {
        ImageReader reader = createReader();

        int width = 0;
        try {
            width = reader.getWidth(0);
            fail("Width read without imput");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image width: " + e);
        }
        assertEquals("Wrong width reported", 0, width);
    }

    public void testGetHeight() {
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
        assertEquals("Wrong height reported", data.getDimension(0).height, height);
    }

    public void testGetHeightNoInput() {
        ImageReader reader = createReader();

        int height = 0;
        try {
            height = reader.getHeight(0);
            fail("height read without imput");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image height: " + e);
        }
        assertEquals("Wrong height reported", 0, height);
    }

    public void testGetHeightIndexOutOfBounds() {
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
    }

    public void testGetAspectratio() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        float aspect = 0f;
        try {
            aspect = reader.getAspectRatio(0);
        }
        catch (IOException e) {
            fail("Could not read image aspectratio" + e);
        }
        Dimension d = data.getDimension(0);
        assertEquals("Wrong aspect aspectratio", d.getWidth() / d.getHeight(), aspect, 0.001);
    }

    public void testGetAspectratioNoInput() {
        ImageReader reader = createReader();

        float aspect = 0f;
        try {
            aspect = reader.getAspectRatio(0);
            fail("aspect read without imput");
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image aspectratio" + e);
        }
        assertEquals("Wrong aspect aspectratio", 0f, aspect, 0f);
    }

    public void testGetAspectratioIndexOutOfBounds() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        //float aspectratio = 0f;
        try {
            // NOTE: Some readers (like the com.sun.imageio stuff) ignores
            // index in getWidth/getHeight for formats with only one image...
            /*aspectratio =*/ reader.getAspectRatio(-1);
            //assertEquals("Wrong aspectratio aspectratio", data.getDimension().width / (float) data.getDimension().height, aspectratio, 0f);
        }
        catch (IndexOutOfBoundsException ignore) {
        }
        catch (IOException e) {
            fail("Could not read image aspectratio" + e);
        }
    }

    public void testDispose() {
        // TODO: Implement
    }

    public void testAddIIOReadProgressListener() {
        ImageReader reader = createReader();
        Mock mockListener = new Mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListener.proxy());
    }

    public void testAddIIOReadProgressListenerNull() {
        ImageReader reader = createReader();
        reader.addIIOReadProgressListener(null);
    }

    public void testAddIIOReadProgressListenerCallbacks() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        Mock mockListener = new Mock(IIOReadProgressListener.class);
        String started = "Started";
        mockListener.expects(once()).method("imageStarted").withAnyArguments().id(started);
        mockListener.stubs().method("imageProgress").withAnyArguments().after(started);
        mockListener.expects(once()).method("imageComplete").withAnyArguments().after(started);

        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListener.proxy());

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        mockListener.verify();
    }

    public void testMultipleAddIIOReadProgressListenerCallbacks() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        Mock mockListener = new Mock(IIOReadProgressListener.class);
        String started = "Started";
        mockListener.expects(once()).method("imageStarted").withAnyArguments().id(started);
        mockListener.stubs().method("imageProgress").withAnyArguments().after(started);
        mockListener.expects(once()).method("imageComplete").withAnyArguments().after(started);

        Mock mockListenerToo = new Mock(IIOReadProgressListener.class);
        String startedToo = "Started Two";
        mockListenerToo.expects(once()).method("imageStarted").withAnyArguments().id(startedToo);
        mockListenerToo.stubs().method("imageProgress").withAnyArguments().after(startedToo);
        mockListenerToo.expects(once()).method("imageComplete").withAnyArguments().after(startedToo);

        Mock mockListenerThree = new Mock(IIOReadProgressListener.class);
        String startedThree = "Started Three";
        mockListenerThree.expects(once()).method("imageStarted").withAnyArguments().id(startedThree);
        mockListenerThree.stubs().method("imageProgress").withAnyArguments().after(startedThree);
        mockListenerThree.expects(once()).method("imageComplete").withAnyArguments().after(startedThree);


        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListener.proxy());
        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListenerToo.proxy());
        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListenerThree.proxy());

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        mockListener.verify();
        mockListenerToo.verify();
        mockListenerThree.verify();
    }

    public void testRemoveIIOReadProgressListenerNull() {
        ImageReader reader = createReader();
        reader.removeIIOReadProgressListener(null);
    }

    public void testRemoveIIOReadProgressListenerNone() {
        ImageReader reader = createReader();
        Mock mockListener = new Mock(IIOReadProgressListener.class);
        reader.removeIIOReadProgressListener((IIOReadProgressListener) mockListener.proxy());
    }

    public void testRemoveIIOReadProgressListener() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());
        Mock mockListener = new Mock(IIOReadProgressListener.class);
        IIOReadProgressListener listener = (IIOReadProgressListener) mockListener.proxy();
        reader.addIIOReadProgressListener(listener);
        reader.removeIIOReadProgressListener(listener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods...
        mockListener.verify();
    }

    public void testRemoveIIOReadProgressListenerMultiple() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        Mock mockListener = new Mock(IIOReadProgressListener.class, "Listener1");
        IIOReadProgressListener listener = (IIOReadProgressListener) mockListener.proxy();
        reader.addIIOReadProgressListener(listener);

        Mock mockListenerToo = new Mock(IIOReadProgressListener.class, "Listener2");
        mockListenerToo.expects(once()).method("imageStarted").with(eq(reader), eq(0));
        mockListenerToo.stubs().method("imageProgress").withAnyArguments();
        mockListenerToo.expects(once()).method("imageComplete").with(eq(reader));
        IIOReadProgressListener listenerToo = (IIOReadProgressListener) mockListenerToo.proxy();
        reader.addIIOReadProgressListener(listenerToo);

        reader.removeIIOReadProgressListener(listener);

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods...
        mockListener.verify();
        mockListenerToo.verify();
    }

    public void testRemoveAllIIOReadProgressListeners() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        Mock mockListener = new Mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListener.proxy());

        reader.removeAllIIOReadProgressListeners();

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods...
        mockListener.verify();
    }

    public void testRemoveAllIIOReadProgressListenersMultiple() {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        Mock mockListener = new Mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListener.proxy());

        Mock mockListenerToo = new Mock(IIOReadProgressListener.class);
        reader.addIIOReadProgressListener((IIOReadProgressListener) mockListenerToo.proxy());

        reader.removeAllIIOReadProgressListeners();

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image");
        }

        // Should not have called any methods...
        mockListener.verify();
        mockListenerToo.verify();
    }

    public void testAbort() {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        Mock mockListener = new Mock(IIOReadProgressListener.class, "Progress1");
        mockListener.stubs().method("imageStarted").withAnyArguments();
        mockListener.stubs().method("imageProgress").withAnyArguments();
        mockListener.expects(once()).method("readAborted").with(eq(reader));
        mockListener.stubs().method("imageComplete").withAnyArguments();
        IIOReadProgressListener listener = (IIOReadProgressListener) mockListener.proxy();
        reader.addIIOReadProgressListener(listener);

        Mock mockListenerToo = new Mock(IIOReadProgressListener.class, "Progress2");
        mockListenerToo.stubs().method("imageStarted").withAnyArguments();
        mockListenerToo.stubs().method("imageProgress").withAnyArguments();
        mockListenerToo.expects(once()).method("readAborted").with(eq(reader));
        mockListenerToo.stubs().method("imageComplete").withAnyArguments();
        IIOReadProgressListener listenerToo = (IIOReadProgressListener) mockListenerToo.proxy();
        reader.addIIOReadProgressListener(listenerToo);

        // Create a listener that just makes the reader abort immediately...
        Mock abortingListener = new Mock(IIOReadProgressListener.class, "Aborter");
        abortingListener.stubs().method("readAborted").withAnyArguments();
        abortingListener.stubs().method("imageComplete").withAnyArguments();
        Stub abort = new Stub() {
            public Object invoke(Invocation pInvocation) throws Throwable {
                reader.abort();
                return null;
            }

            public StringBuffer describeTo(StringBuffer pStringBuffer) {
                pStringBuffer.append("aborting");
                return pStringBuffer;
            }
        };
        abortingListener.stubs().method("imageProgress").will(abort);
        abortingListener.stubs().method("imageStarted").will(abort);

        reader.addIIOReadProgressListener((IIOReadProgressListener) abortingListener.proxy());

        try {
            reader.read(0);
        }
        catch (IOException e) {
            fail("Could not read image: " + e.getMessage() );
        }

        mockListener.verify();
        mockListenerToo.verify();
    }

    public void testGetTypeSpecifiers() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
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

        assertTrue("ImageTypeSepcifier from getRawImageType should be in the iterator from getImageTypes", rawFound);
    }

    public void testSetDestination() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();
        Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
        while (types.hasNext()) {
            ImageTypeSpecifier type = types.next();

            BufferedImage destination = type.createBufferedImage(50, 50);
            param.setDestination(destination);

            BufferedImage result = reader.read(0, param);

            assertSame(destination, result);
        }
    }

    public void testSetDestinationRaw() throws IOException {
        ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        ImageTypeSpecifier type = reader.getRawImageType(0);
        BufferedImage destination = type.createBufferedImage(reader.getWidth(0), reader.getHeight(0));
        param.setDestination(destination);

        BufferedImage result = reader.read(0, param);

        assertSame(destination, result);
    }

    public void testSetDestinationIllegal() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        List<ImageTypeSpecifier> illegalTypes = createIllegalTypes(reader.getImageTypes(0));

        ImageReadParam param = reader.getDefaultReadParam();
        for (ImageTypeSpecifier illegalType : illegalTypes) {
            BufferedImage destination = illegalType.createBufferedImage(50, 50);
            param.setDestination(destination);

            try {
                reader.read(0, param);

                // NOTE: We allow the reader to read, as it's inconvenient to test all possible cases.
                // However, it may NOT fail with any other exception in that case.
                System.err.println("WARNING: Reader does not throw exception with non-declared destination: " + destination);
            }
            catch (IIOException expected) {
                // TODO: This is thrown by ImageReader.getDestination. But are we happy with that?
                // The problem is that the checkReadParamBandSettings throws IllegalArgumentException, which seems more appropriate...
                String message = expected.getMessage();
                assertTrue("Wrong message: " + message, message.toLowerCase().contains("destination"));
            }
            catch (IllegalArgumentException expected) {
                String message = expected.getMessage();
                assertTrue("Wrong message: " + message, message.toLowerCase().contains("dest"));
            }
        }
    }

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
            catch (IIOException expected) {
                // TODO: This is thrown by ImageReader.getDestination. But are we happy with that?
                String message = expected.getMessage();
                assertTrue(message.toLowerCase().contains("destination"));
                assertTrue(message.toLowerCase().contains("type"));
            }
            catch (IllegalArgumentException expected) {
                String message = expected.getMessage();
                assertTrue(message.toLowerCase().contains("destination"));
                assertTrue(message.toLowerCase().contains("type"));
            }
        }
    }

    private List<ImageTypeSpecifier> createIllegalTypes(Iterator<ImageTypeSpecifier> pValidTypes) {
        List<ImageTypeSpecifier> allTypes = new ArrayList<ImageTypeSpecifier>();
        for (int i = BufferedImage.TYPE_INT_RGB; i < BufferedImage.TYPE_BYTE_INDEXED; i++) {
            allTypes.add(ImageTypeSpecifier.createFromBufferedImageType(i));
        }

        List<ImageTypeSpecifier> illegalTypes = new ArrayList<ImageTypeSpecifier>(allTypes);
        while (pValidTypes.hasNext()) {
            ImageTypeSpecifier valid = pValidTypes.next();
            boolean removed = illegalTypes.remove(valid);

            // TODO: 4BYTE_ABGR (6) and 4BYTE_ABGR_PRE (7) is essentially the same type... 
            // !#$#§%$! ImageTypeSpecifier.equals is not well-defined
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
    public void testSetDestinationOffset() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();
        Point point = new Point(10, 10);
        param.setDestinationOffset(point);

        BufferedImage image = reader.read(0, param);

        assertNotNull(image);
        assertEquals(reader.getWidth(0) + point.x, image.getWidth());
        assertEquals(reader.getHeight(0) + point.y, image.getHeight());
    }

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
    }

    public void testSetDestinationType() throws IOException {
        final ImageReader reader = createReader();
        TestData data = getTestData().get(0);
        reader.setInput(data.getInputStream());

        ImageReadParam param = reader.getDefaultReadParam();

        Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
        while (types.hasNext()) {
            ImageTypeSpecifier type = types.next();
            param.setDestinationType(type);

            BufferedImage result = reader.read(0, param);

            assertEquals(type.getColorModel(), result.getColorModel());

//            assertEquals(type.getSampleModel(), result.getSampleModel());
            SampleModel expectedModel = type.getSampleModel();
            SampleModel resultModel = result.getSampleModel();

            assertEquals(expectedModel.getDataType(), resultModel.getDataType());
            assertEquals(expectedModel.getNumBands(), resultModel.getNumBands());
            assertEquals(expectedModel.getNumDataElements(), resultModel.getNumDataElements());
            assertTrue(Arrays.equals(expectedModel.getSampleSize(), resultModel.getSampleSize()));
            assertEquals(expectedModel.getTransferType(), resultModel.getTransferType());
            for (int i = 0; i < expectedModel.getNumBands(); i++) {
                assertEquals(expectedModel.getSampleSize(i), resultModel.getSampleSize(i));
            }
        }
    }

//    public void testSetDestinationTypeIllegal() throws IOException {
//        throw new UnsupportedOperationException("Method testSetDestinationTypeIllegal not implemented"); // TODO: Implement
//    }
//
//    public void testSetDestinationBands() throws IOException {
//        throw new UnsupportedOperationException("Method testSetDestinationBands not implemented"); // TODO: Implement
//    }
//
//    public void testSetSourceBands() throws IOException {
//        throw new UnsupportedOperationException("Method testSetDestinationBands not implemented"); // TODO: Implement
//    }

    protected URL getClassLoaderResource(final String pName) {
        return getClass().getResource(pName);
    }

    static final protected class TestData {
        private final Object mInput;
        private final List<Dimension> mSizes;
        private final List<BufferedImage> mImages;

        public TestData(final Object pInput, final Dimension... pSizes) {
            this(pInput, Arrays.asList(pSizes), null);
        }

        public TestData(final Object pInput, final BufferedImage... pImages) {
            this(pInput, null, Arrays.asList(pImages));
        }

        public TestData(final Object pInput, final List<Dimension> pSizes, final List<BufferedImage> pImages) {
            if (pInput == null) {
                throw new IllegalArgumentException("input == null");
            }

            mSizes = new ArrayList<Dimension>();
            mImages = new ArrayList<BufferedImage>();

            List<Dimension> sizes = pSizes;
            if (sizes == null) {
                sizes = new ArrayList<Dimension>();
                if (pImages != null) {
                    for (BufferedImage image : pImages) {
                        sizes.add(new Dimension(image.getWidth(), image.getHeight()));
                    }
                }
                else {
                    throw new IllegalArgumentException("Need either size or image");
                }
            }
            else if (pImages != null) {
                if (pImages.size() != pSizes.size()) {
                    throw new IllegalArgumentException("Size parameter and image size differs");
                }
                for (int i = 0; i < sizes.size(); i++) {
                    if (!new Dimension(pImages.get(i).getWidth(), pImages.get(i).getHeight()).equals(sizes.get(i))) {
                        throw new IllegalArgumentException("Size parameter and image size differs");
                    }

                }
            }

            mSizes.addAll(sizes);
            if (pImages != null) {
                mImages.addAll(pImages);
            }

            mInput = pInput;
        }

        public Object getInput() {
            return mInput;
        }

        public ImageInputStream getInputStream() {
            try {
                ImageInputStream stream = ImageIO.createImageInputStream(mInput);
                assertNotNull("Could not create ImageInputStream for input: " + mInput, stream);
                return stream;
            }
            catch (IOException e) {
                fail("Could not create ImageInputStream for input: " + mInput +
                        "\n caused by: " + e.getMessage());
            }
            return null;
        }

        public int getImageCount() {
            return mSizes.size();
        }

        public Dimension getDimension(final int pIndex) {
            return mSizes.get(pIndex);
        }

        public BufferedImage getImage(final int pIndex) {
            return mImages.get(pIndex);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ": " + String.valueOf(mInput);
        }
    }
}
