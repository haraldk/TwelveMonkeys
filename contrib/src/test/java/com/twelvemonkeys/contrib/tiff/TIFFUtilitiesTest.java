/*
 * Copyright (c) 2013, Harald Kuhr
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

package com.twelvemonkeys.contrib.tiff;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFExtension;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFMedataFormat;
import com.twelvemonkeys.io.FileUtil;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.xpath.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * TIFFUtilitiesTest
 *
 * @author <a href="mailto:mail@schmidor.de">Oliver Schmidtmer</a>
 * @author last modified by $Author$
 * @version $Id$
 */
public class TIFFUtilitiesTest {

    @Test
    public void testMerge() throws IOException {
        // Files from ImageIO TIFF Plugin
        InputStream stream1 = getClassLoaderResource("/tiff/ccitt/group3_1d.tif").openStream();
        InputStream stream2 = getClassLoaderResource("/tiff/ccitt/group3_2d.tif").openStream();
        InputStream stream3 = getClassLoaderResource("/tiff/ccitt/group4.tif").openStream();

        File file1 = File.createTempFile("imageiotest", ".tif");
        File file2 = File.createTempFile("imageiotest", ".tif");
        File file3 = File.createTempFile("imageiotest", ".tif");
        File output = File.createTempFile("imageiotest", ".tif");

        byte[] data;

        data = FileUtil.read(stream1);
        FileUtil.write(file1, data);
        stream1.close();

        data = FileUtil.read(stream2);
        FileUtil.write(file2, data);
        stream2.close();

        data = FileUtil.read(stream3);
        FileUtil.write(file3, data);
        stream3.close();

        List<File> input = Arrays.asList(file1, file2, file3);
        TIFFUtilities.merge(input, output);

        ImageInputStream iis = ImageIO.createImageInputStream(output);
        ImageReader reader = ImageIO.getImageReaders(iis).next();
        reader.setInput(iis);
        Assert.assertEquals(3, reader.getNumImages(true));

        iis.close();
        output.delete();
        file1.delete();
        file2.delete();
        file3.delete();
    }

    @Test
    public void testSplit() throws IOException {
        InputStream inputStream = getClassLoaderResource("/contrib/tiff/multipage.tif").openStream();
        File inputFile = File.createTempFile("imageiotest", "tif");
        byte[] data = FileUtil.read(inputStream);
        FileUtil.write(inputFile, data);
        inputStream.close();

        File outputDirectory = Files.createTempDirectory("imageio").toFile();

        TIFFUtilities.split(inputFile, outputDirectory);

        ImageReader reader = ImageIO.getImageReadersByFormatName("TIF").next();

        File[] outputFiles = outputDirectory.listFiles();
        Assert.assertEquals(3, outputFiles.length);
        for (File outputFile : outputFiles) {
            ImageInputStream iis = ImageIO.createImageInputStream(outputFile);
            reader.setInput(iis);
            Assert.assertEquals(1, reader.getNumImages(true));
            iis.close();
            outputFile.delete();
        }
        outputDirectory.delete();
        inputFile.delete();
    }

    @Test
    public void testRotate() throws IOException, XPathExpressionException {
        ImageReader reader = ImageIO.getImageReadersByFormatName("TIF").next();

        InputStream inputStream = getClassLoaderResource("/contrib/tiff/multipage.tif").openStream();
        File inputFile = File.createTempFile("imageiotest", ".tif");
        byte[] data = FileUtil.read(inputStream);
        FileUtil.write(inputFile, data);
        inputStream.close();

        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression expression = xPath.compile("TIFFIFD/TIFFField[@number='274']/TIFFBytes/TIFFByte/@value");

        // rotate all pages
        ImageInputStream inputTest1 = ImageIO.createImageInputStream(inputFile);
        File outputTest1 = File.createTempFile("imageiotest", ".tif");
        ImageOutputStream iosTest1 = ImageIO.createImageOutputStream(outputTest1);
        TIFFUtilities.rotatePages(inputTest1, iosTest1, 90);
        iosTest1.close();

        ImageInputStream checkTest1 = ImageIO.createImageInputStream(outputTest1);
        reader.setInput(checkTest1);
        for (int i = 0; i < 3; i++) {
            Node metaData = reader.getImageMetadata(i)
                    .getAsTree(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME);
            short orientation = ((Number) expression.evaluate(metaData, XPathConstants.NUMBER)).shortValue();
            Assert.assertEquals(orientation, TIFFExtension.ORIENTATION_RIGHTTOP);
        }
        checkTest1.close();

        // rotate single page further
        ImageInputStream inputTest2 = ImageIO.createImageInputStream(outputTest1);
        File outputTest2 = File.createTempFile("imageiotest", ".tif");
        ImageOutputStream iosTest2 = ImageIO.createImageOutputStream(outputTest2);
        TIFFUtilities.rotatePage(inputTest2, iosTest2, 90, 1);
        iosTest2.close();

        ImageInputStream checkTest2 = ImageIO.createImageInputStream(outputTest2);
        reader.setInput(checkTest2);
        for (int i = 0; i < 3; i++) {
            Node metaData = reader.getImageMetadata(i)
                    .getAsTree(TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME);
            short orientation = ((Number) expression.evaluate(metaData, XPathConstants.NUMBER)).shortValue();
            Assert.assertEquals(orientation, i == 1
                                             ? TIFFExtension.ORIENTATION_BOTRIGHT
                                             : TIFFExtension.ORIENTATION_RIGHTTOP);
        }
        checkTest2.close();
    }

    @Test
    public void testApplyOrientation() throws IOException {
        InputStream inputStream = getClassLoaderResource("/contrib/tiff/multipage.tif").openStream();
        File inputFile = File.createTempFile("imageiotest", "tif");
        byte[] data = FileUtil.read(inputStream);
        FileUtil.write(inputFile, data);
        inputStream.close();

        BufferedImage image = ImageIO.read(inputFile);

        // rotate by 90�
        BufferedImage image90 = TIFFUtilities.applyOrientation(image, TIFFExtension.ORIENTATION_RIGHTTOP);
        // rotate by 270�
        BufferedImage image360 = TIFFUtilities.applyOrientation(image90, TIFFExtension.ORIENTATION_LEFTBOT);

        byte[] original = ((DataBufferByte) image.getData().getDataBuffer()).getData();
        byte[] rotated = ((DataBufferByte) image360.getData().getDataBuffer()).getData();

        Assert.assertArrayEquals(original, rotated);
    }

    protected URL getClassLoaderResource(final String pName) {
        return getClass().getResource(pName);
    }
}
