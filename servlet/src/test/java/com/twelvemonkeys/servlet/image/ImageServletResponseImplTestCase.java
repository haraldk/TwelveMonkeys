package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.image.BufferedImageIcon;
import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.io.FastByteArrayOutputStream;
import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.servlet.OutputStreamAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ImageServletResponseImplTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/image/ImageServletResponseImplTestCase.java#6 $
 */
public class ImageServletResponseImplTestCase {
    private static final String CONTENT_TYPE_BMP = "image/bmp";
    private static final String CONTENT_TYPE_FOO = "foo/bar";
    private static final String CONTENT_TYPE_GIF = "image/gif";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String CONTENT_TYPE_TEXT = "text/plain";

    private static final String IMAGE_NAME_PNG = "12monkeys-splash.png";
    private static final String IMAGE_NAME_GIF = "tux.gif";
    private static final String IMAGE_NAME_PNG_INDEXED = "star.png";

    private static final Dimension IMAGE_DIMENSION_PNG = new Dimension(300, 410);
    private static final Dimension IMAGE_DIMENSION_GIF = new Dimension(250, 250);
    private static final Dimension IMAGE_DIMENSION_PNG_INDEXED = new Dimension(199, 192);

    private static final int STREAM_DEFAULT_SIZE = 2000;

    private HttpServletRequest request;
    private ServletContext context;

    @Before
    public void init() throws Exception {
        request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        context = mock(ServletContext.class);
        when(context.getResource("/" + IMAGE_NAME_PNG)).thenReturn(getClass().getResource(IMAGE_NAME_PNG));
        when(context.getResource("/" + IMAGE_NAME_GIF)).thenReturn(getClass().getResource(IMAGE_NAME_GIF));
        when(context.getResource("/" + IMAGE_NAME_PNG_INDEXED)).thenReturn(getClass().getResource(IMAGE_NAME_PNG_INDEXED));
        when(context.getMimeType("file.bmp")).thenReturn(CONTENT_TYPE_BMP);
        when(context.getMimeType("file.foo")).thenReturn(CONTENT_TYPE_FOO);
        when(context.getMimeType("file.gif")).thenReturn(CONTENT_TYPE_GIF);
        when(context.getMimeType("file.jpeg")).thenReturn(CONTENT_TYPE_JPEG);
        when(context.getMimeType("file.png")).thenReturn(CONTENT_TYPE_PNG);
        when(context.getMimeType("file.txt")).thenReturn(CONTENT_TYPE_TEXT);

        MockLogger mockLogger = new MockLogger();
        doAnswer(mockLogger).when(context).log(anyString());
        doAnswer(mockLogger).when(context).log(anyString(), any(Throwable.class));
        //noinspection deprecation
        doAnswer(mockLogger).when(context).log(any(Exception.class), anyString());
    }

    private void fakeResponse(HttpServletRequest pRequest, ImageServletResponseImpl pImageResponse) throws IOException {
        String uri = pRequest.getRequestURI();
        int index = uri.lastIndexOf('/');
        assertTrue(uri, index >= 0);

        String name = uri.substring(index + 1);
        InputStream in = getClass().getResourceAsStream(name);

        if (in == null) {
            pImageResponse.sendError(HttpServletResponse.SC_NOT_FOUND, uri + " not found");
        }
        else {
            String ext = name.substring(name.lastIndexOf("."));
            pImageResponse.setContentType(context.getMimeType("file" + ext));
            pImageResponse.setContentLength(234);
            try {
                ServletOutputStream out = pImageResponse.getOutputStream();
                try {
                    FileUtil.copy(in, out);
                }
                finally {
                    out.close();
                }
            }
            finally {
                in.close();
            }
        }
    }

    @Test
    public void testBasicResponse() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);
        assertEquals(IMAGE_DIMENSION_PNG.width, image.getWidth());
        assertEquals(IMAGE_DIMENSION_PNG.height, image.getHeight());

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());
        
        verify(response).setContentType(CONTENT_TYPE_PNG);
        verify(response).getOutputStream();
    }

    // Test that wrapper works as a no-op, in case the image does not need to be decoded
    // This is not a very common use case, as filters should avoid wrapping the response
    // for performance reasons, but we still want that to work

    @Test
    public void testNoOpResponse() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // TODO: Is there a way we can avoid calling flush?
        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is untouched
        assertTrue("Data differs", Arrays.equals(FileUtil.read(getClass().getResourceAsStream(IMAGE_NAME_PNG)), out.toByteArray()));

        verify(response).setContentType(CONTENT_TYPE_PNG);
        verify(response).getOutputStream();
    }

    // Transcode original PNG to JPEG with no other changes
    @Test
    public void testTranscodeResponsePNGToJPEG() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Force transcode to JPEG
        imageResponse.setOutputContentType("image/jpeg");

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Assert JPEG
        ByteArrayInputStream input = out.createInputStream();
        assertEquals(0xFF, input.read());
        assertEquals(0xD8, input.read());
        assertEquals(0xFF, input.read());

        // Test that image data is still readable
        /*
        File tempFile = File.createTempFile("imageservlet-test-", ".jpeg");
        FileOutputStream stream = new FileOutputStream(tempFile);
        out.writeTo(stream);
        stream.close();
        System.err.println("open " + tempFile);
        */

        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(IMAGE_DIMENSION_PNG.width, outImage.getWidth());
        assertEquals(IMAGE_DIMENSION_PNG.height, outImage.getHeight());

        BufferedImage image = flatten(ImageIO.read(context.getResource("/" + IMAGE_NAME_PNG)), Color.BLACK);

        /*
        tempFile = File.createTempFile("imageservlet-test-", ".png");
        stream = new FileOutputStream(tempFile);
        ImageIO.write(image, "PNG", stream);
        stream.close();
        System.err.println("open " + tempFile);
        */

        // JPEG compression trashes the image completely...
        assertSimilarImage(image, outImage, 144f);

        verify(response).setContentType(CONTENT_TYPE_JPEG);
        verify(response).getOutputStream();
    }

    // WORKAROUND: Bug in GIFImageWriteParam, compression type is not set by default
    // (even if there's only one possible compression mode/type combo; MODE_EXPLICIT/"LZW")
   @Test
    public void testTranscodeResponsePNGToGIFWithQuality() throws IOException {
       FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);

       HttpServletResponse response = mock(HttpServletResponse.class);
       when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));
       when(request.getAttribute(ImageServletResponse.ATTRIB_OUTPUT_QUALITY)).thenReturn(.5f); // Force quality setting in param

       ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
       fakeResponse(request, imageResponse);

       // Force transcode to GIF
       imageResponse.setOutputContentType("image/gif");

       // Flush image to wrapped response
       imageResponse.flush();

       assertTrue("Content has no data", out.size() > 0);

       // Assert GIF
       ByteArrayInputStream stream = out.createInputStream();
       assertEquals('G', stream.read());
       assertEquals('I', stream.read());
       assertEquals('F', stream.read());
       assertEquals('8', stream.read());
       assertEquals('9', stream.read());
       assertEquals('a', stream.read());

       // Test that image data is still readable
       BufferedImage outImage = ImageIO.read(out.createInputStream());
       assertNotNull(outImage);
       assertEquals(IMAGE_DIMENSION_PNG.width, outImage.getWidth());
       assertEquals(IMAGE_DIMENSION_PNG.height, outImage.getHeight());

       BufferedImage image = ImageIO.read(context.getResource("/" + IMAGE_NAME_PNG));

       // Should keep transparency, but is now binary
//       showIt(image, outImage, null);
       assertSimilarImageTransparent(image, outImage, 50f);

       verify(response).setContentType(CONTENT_TYPE_GIF);
       verify(response).getOutputStream();
   }

    // WORKAROUND: Bug in GIFImageWriter may throw NPE if transparent pixels
    // See: http://bugs.sun.com/view_bug.do?bug_id=6287936
    @Test
    public void testTranscodeResponsePNGToGIF() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Force transcode to GIF
        imageResponse.setOutputContentType("image/gif");

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(IMAGE_DIMENSION_PNG.width, outImage.getWidth());
        assertEquals(IMAGE_DIMENSION_PNG.height, outImage.getHeight());

        BufferedImage image = ImageIO.read(context.getResource("/" + IMAGE_NAME_PNG));

        // Should keep transparency, but is now binary
//        showIt(image, outImage, null);
        assertSimilarImageTransparent(image, outImage, 50f);

        verify(response).setContentType(CONTENT_TYPE_GIF);
        verify(response).getOutputStream();
    }

    @Test
    public void testTranscodeResponseIndexColorModelGIFToJPEG() throws IOException {
        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_GIF);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Force transcode to JPEG
        imageResponse.setOutputContentType("image/jpeg");

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Assert JPEG
        ByteArrayInputStream stream = out.createInputStream();
        assertEquals(0xFF, stream.read());
        assertEquals(0xD8, stream.read());
        assertEquals(0xFF, stream.read());

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(IMAGE_DIMENSION_GIF.width, outImage.getWidth());
        assertEquals(IMAGE_DIMENSION_GIF.height, outImage.getHeight());

        BufferedImage image = flatten(ImageIO.read(context.getResource("/" + IMAGE_NAME_GIF)), Color.WHITE);

        assertSimilarImage(image, outImage, 96f);
    }

    @Test
    // TODO: Insert bug id/reference here for regression tracking
    public void testIndexedColorModelResizePNG() throws IOException {
        // Results differ with algorithm, so we test each algorithm by itself
        int[] algorithms = new int[] {Image.SCALE_DEFAULT, Image.SCALE_FAST, Image.SCALE_SMOOTH, Image.SCALE_REPLICATE, Image.SCALE_AREA_AVERAGING, 77};

        for (int algorithm : algorithms) {
            Dimension size = new Dimension(100, 100);

            // Custom setup
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
            when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE_UNIFORM)).thenReturn(false);
            when(request.getAttribute(ImageServletResponse.ATTRIB_IMAGE_RESAMPLE_ALGORITHM)).thenReturn(algorithm);
            when(request.getContextPath()).thenReturn("/ape");
            when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG_INDEXED);

            FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

            ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
            fakeResponse(request, imageResponse);

            imageResponse.getImage();

            // Flush image to wrapped response
            imageResponse.flush();

            assertTrue("Content has no data", out.size() > 0);

            // Assert format is still PNG
            ByteArrayInputStream inputStream = out.createInputStream();
            assertEquals(0x89, inputStream.read());
            assertEquals('P', inputStream.read());
            assertEquals('N', inputStream.read());
            assertEquals('G', inputStream.read());

            // Test that image data is still readable
            BufferedImage outImage = ImageIO.read(out.createInputStream());
            assertNotNull(outImage);
            assertEquals(size.width, outImage.getWidth());
            assertEquals(size.height, outImage.getHeight());

            BufferedImage read = ImageIO.read(context.getResource("/" + IMAGE_NAME_PNG_INDEXED));
            BufferedImage image = ImageUtil.createResampled(read, size.width, size.height, imageResponse.getResampleAlgorithmFromRequest());

//            showIt(image, outImage, null);

            assertSimilarImageTransparent(image, outImage, 10f);
        }
    }

    private static BufferedImage flatten(final BufferedImage pImage, final Color pBackgroundColor) {
        BufferedImage image = ImageUtil.toBuffered(pImage, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.DstOver);
            g.setColor(pBackgroundColor);
            g.fillRect(0, 0, pImage.getWidth(), pImage.getHeight());
        }
        finally {
            g.dispose();
        }

        return image;
    }

    /**
     * Makes sure images are the same, taking JPEG artifacts into account.
     *
     * @param pExpected the expected image
     * @param pActual the actual image
     * @param pArtifactThreshold the maximum allowed difference between the expected and actual pixel value
     */
    private void assertSimilarImage(final BufferedImage pExpected, final BufferedImage pActual, final float pArtifactThreshold) {
        for (int y = 0; y < pExpected.getHeight(); y++) {
            for (int x = 0; x < pExpected.getWidth(); x++) {
                int expected = pExpected.getRGB(x, y);
                int actual = pActual.getRGB(x, y);

                // Multiply in the alpha component
                float alpha = ((expected >> 24) & 0xff) / 255f;

                assertEquals(alpha * ((expected >> 16) & 0xff), (actual >> 16) & 0xff, pArtifactThreshold);
                assertEquals(alpha * ((expected >> 8) & 0xff), (actual >> 8) & 0xff, pArtifactThreshold);
                assertEquals(alpha * ((expected) & 0xff), actual & 0xff, pArtifactThreshold);
            }
        }
    }

    private void assertSimilarImageTransparent(final BufferedImage pExpected, final BufferedImage pActual, final float pArtifactThreshold) {
        IndexColorModel icm = pActual.getColorModel() instanceof IndexColorModel ? (IndexColorModel) pActual.getColorModel() : null;
        Object pixel = null;

        for (int y = 0; y < pExpected.getHeight(); y++) {
            for (int x = 0; x < pExpected.getWidth(); x++) {
                int expected = pExpected.getRGB(x, y);
                int actual = pActual.getRGB(x, y);

                if (icm != null) {
                    // Look up, using ICM

                    int alpha = (expected >> 24) & 0xff;
                    boolean transparent = alpha < 0x40;

                    if (transparent) {
                        int expectedLookedUp = icm.getRGB(icm.getTransparentPixel());
                        assertRGBEquals(x, y, expectedLookedUp & 0xff000000, actual & 0xff000000, 0);
                    }
                    else {
                        pixel = icm.getDataElements(expected, pixel);
                        int expectedLookedUp = icm.getRGB(pixel);
                        assertRGBEquals(x, y, expectedLookedUp & 0xffffff, actual & 0xffffff, pArtifactThreshold);
                    }
                }
                else {
                    // Multiply out alpha for each component if pre-multiplied
//                    int expectedR = (int) ((((expected >> 16) & 0xff) * alpha) / 255f);
//                    int expectedG = (int) ((((expected >> 8) & 0xff) * alpha) / 255f);
//                    int expectedB = (int) (((expected & 0xff) * alpha) / 255f);

                    assertRGBEquals(x, y, expected, actual, pArtifactThreshold);
                }
            }
        }
    }

    private void assertRGBEquals(int x, int y, int expected, int actual, float pArtifactThreshold) {
        int expectedA = (expected >> 24) & 0xff;
        int expectedR = (expected >> 16) & 0xff;
        int expectedG = (expected >>  8) & 0xff;
        int expectedB =  expected        & 0xff;

        try {
            assertEquals("Alpha", expectedA, (actual >> 24) & 0xff, pArtifactThreshold);
            assertEquals("RGB", 0, (Math.abs(expectedR - ((actual >> 16) & 0xff)) +
                    Math.abs(expectedG - ((actual >> 8) & 0xff)) +
                    Math.abs(expectedB - ((actual) & 0xff))) / 3.0, pArtifactThreshold);
        }
        catch (AssertionError e) {
            AssertionError assertionError = new AssertionError(String.format("@[%d,%d] expected: 0x%08x but was: 0x%08x", x, y, expected, actual));
            assertionError.initCause(e);
            throw assertionError;
        }
    }

    @Test
    public void testReplaceResponse() throws IOException {
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        // Do something with image
        // NOTE: BMP writer can't write ARGB so this image is converted (same goes for JPEG) 
        // TODO: Make conversion testing more explicit
        image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        imageResponse.setImage(image);
        imageResponse.setOutputContentType("image/bmp");

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());
        assertSimilarImage(image, outImage, 0);
        
        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_BMP);
    }

    // TODO: Test with AOI attributes (rename thes to source-region?)
    // TODO: Test with scale attributes
    // More?

    // Make sure we don't change semantics here...

    @Test
    public void testNotFoundInput() throws IOException {
        // Need special setup
        request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/monkey-business.gif");

        HttpServletResponse response = mock(HttpServletResponse.class);

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);
        
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    // NOTE: This means it's up to some Filter to decide wether we should filter the given request

    @Test
    public void testUnsupportedInput() throws IOException {
        assertFalse("Test is invalid, rewrite test", ImageIO.getImageReadersByFormatName("txt").hasNext());

        // Need special setup
        request =  mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/foo.txt");

        HttpServletResponse response = mock(HttpServletResponse.class);

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);

        fakeResponse(request, imageResponse);
        try {
            // Force transcode
            imageResponse.setOutputContentType("image/png");

            // Flush image to wrapped response
            imageResponse.flush();

            fail("Should throw IOException in case of unspupported input");
        }
        catch (IOException e) {
            String message = e.getMessage().toLowerCase();
            assertTrue("Wrong message: " + e.getMessage(), message.contains("transcode"));
            assertTrue("Wrong message: " + e.getMessage(), message.contains("reader"));
            assertTrue("Wrong message: " + e.getMessage(), message.contains("text"));
            
            // Failure here suggests a different error condition than the one we expected
        }
    }

    @Test
    public void testUnsupportedOutput() throws IOException {
        assertFalse("Test is invalid, rewrite test", ImageIO.getImageWritersByFormatName("foo").hasNext());

        HttpServletResponse response = mock(HttpServletResponse.class);

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);

        fakeResponse(request, imageResponse);
        try {
            // Force transcode to unsupported format
            imageResponse.setOutputContentType("application/xml+foo");

            // Flush image to wrapped response
            imageResponse.flush();

            fail("Should throw IOException in case of unspupported output");
        }
        catch (IOException e) {
            String message = e.getMessage().toLowerCase();
            assertTrue("Wrong message: " + e.getMessage(), message.contains("transcode"));
            assertTrue("Wrong message: " + e.getMessage(), message.contains("writer"));
            assertTrue("Wrong message: " + e.getMessage(), message.contains("foo"));
            
            // Failure here suggests a different error condition than the one we expected
        }
    }

    // TODO: Test that we handle image conversion to a suitable format, before writing response
    // For example: Read a PNG with transparency and store as B/W WBMP


    // TODO: Create ImageFilter test case, that tests normal use, as well as chaining

    @Test
    public void testReadWithSourceRegion() throws IOException {
        Rectangle sourceRegion = new Rectangle(100, 100, 100, 100);
        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);
        assertEquals(sourceRegion.width, image.getWidth());
        assertEquals(sourceRegion.height, image.getHeight());

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithNonSquareSourceRegion() throws IOException {
        Rectangle sourceRegion = new Rectangle(100, 100, 100, 80);
        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);
        assertEquals(sourceRegion.width, image.getWidth());
        assertEquals(sourceRegion.height, image.getHeight());

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithCenteredUniformSourceRegion() throws IOException {
        // Negative x/y values means centered
        Rectangle sourceRegion = new Rectangle(-1, -1, 300, 300);

        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI_UNIFORM)).thenReturn(true);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        assertEquals(sourceRegion.width, image.getWidth());
        assertEquals(sourceRegion.height, image.getHeight());

        BufferedImage original = ImageIO.read(getClass().getResource(IMAGE_NAME_PNG));

        // Sanity check
        assertNotNull(original);
        assertEquals(IMAGE_DIMENSION_PNG.width, original.getWidth());
        assertEquals(IMAGE_DIMENSION_PNG.height, original.getHeight());

        // Center
        sourceRegion.setLocation(
                (int) Math.round((IMAGE_DIMENSION_PNG.width - sourceRegion.getWidth()) / 2.0),
                (int) Math.round((IMAGE_DIMENSION_PNG.height - sourceRegion.getHeight()) / 2.0)
        );

        // Test that we have exactly the pixels we should
        for (int y = 0; y < sourceRegion.height; y++) {
            for (int x = 0; x < sourceRegion.width; x++) {
                assertEquals(original.getRGB(x + sourceRegion.x, y + sourceRegion.y), image.getRGB(x, y));
            }
        }

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithCenteredUniformNonSquareSourceRegion() throws IOException {
        // Negative x/y values means centered
        Rectangle sourceRegion = new Rectangle(-1, -1, 410, 300);

        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI_UNIFORM)).thenReturn(true);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Image wider than bounding box", IMAGE_DIMENSION_PNG.width >= image.getWidth());
        assertTrue("Image taller than bounding box", IMAGE_DIMENSION_PNG.height >= image.getHeight());
        assertTrue("Image not maximized to bounding box", IMAGE_DIMENSION_PNG.width == image.getWidth() || IMAGE_DIMENSION_PNG.height == image.getHeight());

        // Above tests that one of the sides equal, we now need to test that the other follows aspect
        double destAspect = sourceRegion.getWidth() / sourceRegion.getHeight();
        double srcAspect = IMAGE_DIMENSION_PNG.getWidth() / IMAGE_DIMENSION_PNG.getHeight();

        if (srcAspect >= destAspect) {
            // Dst is narrower than src
            assertEquals(IMAGE_DIMENSION_PNG.height, image.getHeight());
            assertEquals(
                    "Image width does not follow aspect",
                    Math.round(IMAGE_DIMENSION_PNG.getHeight() * destAspect), image.getWidth()
            );
        }
        else {
            // Dst is wider than src
            assertEquals(IMAGE_DIMENSION_PNG.width, image.getWidth());
            assertEquals(
                    "Image height does not follow aspect",
                    Math.round(IMAGE_DIMENSION_PNG.getWidth() / destAspect), image.getHeight()
            );
        }

        BufferedImage original = ImageIO.read(getClass().getResource(IMAGE_NAME_PNG));

        // Sanity check
        assertNotNull(original);
        assertEquals(IMAGE_DIMENSION_PNG.width, original.getWidth());
        assertEquals(IMAGE_DIMENSION_PNG.height, original.getHeight());

        // Center
        sourceRegion.setLocation(
                (int) Math.round((IMAGE_DIMENSION_PNG.width - image.getWidth()) / 2.0),
                (int) Math.round((IMAGE_DIMENSION_PNG.height - image.getHeight()) / 2.0)
        );
        sourceRegion.setSize(image.getWidth(), image.getHeight());

        // Test that we have exactly the pixels we should
        for (int y = 0; y < sourceRegion.height; y++) {
            for (int x = 0; x < sourceRegion.width; x++) {
                assertEquals(original.getRGB(x + sourceRegion.x, y + sourceRegion.y), image.getRGB(x, y));
            }
        }

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithResize() throws IOException {
        Dimension size = new Dimension(100, 120);

        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        assertTrue("Image wider than bounding box", size.width >= image.getWidth());
        assertTrue("Image taller than bounding box", size.height >= image.getHeight());
        assertTrue("Image not maximized to bounding box", size.width == image.getWidth() || size.height == image.getHeight());

        // Above tests that one of the sides equal, we now need to test that the other follows aspect
        if (size.width == image.getWidth()) {
            assertEquals(Math.round(size.getWidth() * IMAGE_DIMENSION_PNG.getWidth() / IMAGE_DIMENSION_PNG.getHeight()), image.getHeight());
        }
        else {
            assertEquals(Math.round(size.getHeight() * IMAGE_DIMENSION_PNG.getWidth() / IMAGE_DIMENSION_PNG.getHeight()), image.getWidth());
        }

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithNonUniformResize() throws IOException {
        Dimension size = new Dimension(150, 150);
        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE_UNIFORM)).thenReturn(false);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);
        assertEquals(size.width, image.getWidth());
        assertEquals(size.height, image.getHeight());

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithSourceRegionAndResize() throws IOException {
        Rectangle sourceRegion = new Rectangle(100, 100, 200, 200);
        Dimension size = new Dimension(100, 120);

        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        assertTrue("Image wider than bounding box", size.width >= image.getWidth());
        assertTrue("Image taller than bounding box", size.height >= image.getHeight());
        assertTrue("Image not maximized to bounding box", size.width == image.getWidth() || size.height == image.getHeight());

        // Above tests that one of the sides equal, we now need to test that the other follows aspect
        if (size.width == image.getWidth()) {
            assertEquals(Math.round(size.getWidth() * sourceRegion.getWidth() / sourceRegion.getHeight()), image.getHeight());
        }
        else {
            assertEquals(Math.round(size.getHeight() * sourceRegion.getWidth() / sourceRegion.getHeight()), image.getWidth());
        }

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithSourceRegionAndNonUniformResize() throws IOException {
        Rectangle sourceRegion = new Rectangle(100, 100, 200, 200);
        Dimension size = new Dimension(150, 150);
        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE_UNIFORM)).thenReturn(false);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);
        assertEquals(size.width, image.getWidth());
        assertEquals(size.height, image.getHeight());

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithUniformSourceRegionAndResizeSquare() throws IOException {
        Rectangle sourceRegion = new Rectangle(-1, -1, 300, 300);
        Dimension size = new Dimension(100, 120);

        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI_UNIFORM)).thenReturn(true);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Image wider than bounding box", size.width >= image.getWidth());
        assertTrue("Image taller than bounding box", size.height >= image.getHeight());
        assertTrue("Image not maximized to bounding box", size.width == image.getWidth() || size.height == image.getHeight());

        // Above tests that one of the sides equal, we now need to test that the other follows aspect
        if (size.width == image.getWidth()) {
            assertEquals(
                    "Image height does not follow aspect",
                    Math.round(size.getWidth() / (sourceRegion.getWidth() / sourceRegion.getHeight())), image.getHeight()
            );
        }
        else {
            System.out.println("size: " + size);
            System.out.println("image: " + new Dimension(image.getWidth(), image.getHeight()));
            assertEquals(
                    "Image width does not follow aspect",
                    Math.round(size.getHeight() * (sourceRegion.getWidth() / sourceRegion.getHeight())), image.getWidth()
            );
        }

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithNonSquareUniformSourceRegionAndResize() throws IOException {
        Rectangle sourceRegion = new Rectangle(-1, -1, 170, 300);
        Dimension size = new Dimension(150, 120);

        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI_UNIFORM)).thenReturn(true);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        // Flush image to wrapped response
        imageResponse.flush();

//        File tempFile = File.createTempFile("test", ".png");
//        FileUtil.write(tempFile, out.toByteArray());
//        System.out.println("tempFile: " + tempFile);

        assertTrue("Image wider than bounding box", size.width >= image.getWidth());
        assertTrue("Image taller than bounding box", size.height >= image.getHeight());
        assertTrue("Image not maximized to bounding box", size.width == image.getWidth() || size.height == image.getHeight());

        // Above tests that one of the sides equal, we now need to test that the other follows aspect
        if (size.width == image.getWidth()) {
            assertEquals(
                    "Image height does not follow aspect",
                    Math.round(size.getWidth() / (sourceRegion.getWidth() / sourceRegion.getHeight())), image.getHeight()
            );
        }
        else {
//            System.out.println("size: " + size);
//            System.out.println("image: " + new Dimension(image.getWidth(), image.getHeight()));
            assertEquals(
                    "Image width does not follow aspect",
                    Math.round(size.getHeight() * (sourceRegion.getWidth() / sourceRegion.getHeight())), image.getWidth()
            );
        }

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    @Test
    public void testReadWithAllNegativeSourceRegion() throws IOException {
        Rectangle sourceRegion = new Rectangle(-1, -1, -1, -1);
        Dimension size = new Dimension(100, 120);

        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI_UNIFORM)).thenReturn(true);
        when(request.getAttribute(ImageServletResponse.ATTRIB_AOI)).thenReturn(sourceRegion);
        when(request.getAttribute(ImageServletResponse.ATTRIB_SIZE)).thenReturn(size);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

        FastByteArrayOutputStream out = new FastByteArrayOutputStream(STREAM_DEFAULT_SIZE);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Make sure image is correctly loaded
        BufferedImage image = imageResponse.getImage();
        assertNotNull(image);

        assertTrue("Image wider than bounding box", size.width >= image.getWidth());
        assertTrue("Image taller than bounding box", size.height >= image.getHeight());
        assertTrue("Image not maximized to bounding box", size.width == image.getWidth() || size.height == image.getHeight());

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(out.createInputStream());
        assertNotNull(outImage);
        assertEquals(image.getWidth(), outImage.getWidth());
        assertEquals(image.getHeight(), outImage.getHeight());

        verify(response).getOutputStream();
        verify(response).setContentType(CONTENT_TYPE_PNG);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Absolute AOI
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void testGetAOIAbsolute() {
        assertEquals(new Rectangle(10, 10, 100, 100), ImageServletResponseImpl.getAOI(200, 200, 10, 10, 100, 100, false, false));
    }

    @Test
    public void testGetAOIAbsoluteOverflowX() {
        assertEquals(new Rectangle(10, 10, 90, 100), ImageServletResponseImpl.getAOI(100, 200, 10, 10, 100, 100, false, false));
    }

    @Test
    public void testGetAOIAbsoluteOverflowW() {
        assertEquals(new Rectangle(0, 10, 100, 100), ImageServletResponseImpl.getAOI(100, 200, 0, 10, 110, 100, false, false));
    }

    @Test
    public void testGetAOIAbsoluteOverflowY() {
        assertEquals(new Rectangle(10, 10, 100, 90), ImageServletResponseImpl.getAOI(200, 100, 10, 10, 100, 100, false, false));
    }

    @Test
    public void testGetAOIAbsoluteOverflowH() {
        assertEquals(new Rectangle(10, 0, 100, 100), ImageServletResponseImpl.getAOI(200, 100, 10, 0, 100, 110, false, false));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Uniform AOI centered
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void testGetAOIUniformCenteredS2SUp() {
        assertEquals(new Rectangle(0, 0, 100, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 333, 333, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredS2SDown() {
        assertEquals(new Rectangle(0, 0, 100, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 33, 33, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredS2SNormalized() {
        assertEquals(new Rectangle(0, 0, 100, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 100, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredS2W() {
        assertEquals(new Rectangle(0, 25, 100, 50), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 200, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredS2WNormalized() {
        assertEquals(new Rectangle(0, 25, 100, 50), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 100, 50, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredS2N() {
        assertEquals(new Rectangle(25, 0, 50, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 100, 200, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredS2NNormalized() {
        assertEquals(new Rectangle(25, 0, 50, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 50, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2S() {
        assertEquals(new Rectangle(50, 0, 100, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 333, 333, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2SNormalized() {
        assertEquals(new Rectangle(50, 0, 100, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 100, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2W() {
        assertEquals(new Rectangle(0, 0, 200, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 100, 50, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2WW() {
        assertEquals(new Rectangle(0, 25, 200, 50), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 200, 50, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2WN() {
        assertEquals(new Rectangle(25, 0, 150, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 75, 50, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2WNNormalized() {
        assertEquals(new Rectangle(25, 0, 150, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 150, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2WNormalized() {
        assertEquals(new Rectangle(0, 0, 200, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 200, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2N() {
        assertEquals(new Rectangle(75, 0, 50, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 100, 200, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredW2NNormalized() {
        assertEquals(new Rectangle(75, 0, 50, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 50, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2S() {
        assertEquals(new Rectangle(0, 50, 100, 100), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 333, 333, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2SNormalized() {
        assertEquals(new Rectangle(0, 50, 100, 100), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2W() {
        assertEquals(new Rectangle(0, 75, 100, 50), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 200, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2WNormalized() {
        assertEquals(new Rectangle(0, 75, 100, 50), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 50, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2N() {
        assertEquals(new Rectangle(0, 0, 100, 200), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 50, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2NN() {
        assertEquals(new Rectangle(25, 0, 50, 200), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 25, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2NW() {
        assertEquals(new Rectangle(0, 33, 100, 133), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 75, 100, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2NWNormalized() {
        assertEquals(new Rectangle(0, 37, 100, 125), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 125, false, true));
    }

    @Test
    public void testGetAOIUniformCenteredN2NNormalized() {
        assertEquals(new Rectangle(0, 0, 100, 200), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 200, false, true));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Absolute AOI centered
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void testGetAOICenteredS2SUp() {
        assertEquals(new Rectangle(0, 0, 100, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 333, 333, false, false));
    }

    @Test
    public void testGetAOICenteredS2SDown() {
        assertEquals(new Rectangle(33, 33, 33, 33), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 33, 33, false, false));
    }

    @Test
    public void testGetAOICenteredS2SSame() {
        assertEquals(new Rectangle(0, 0, 100, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 100, 100, false, false));
    }

    @Test
    public void testGetAOICenteredS2WOverflow() {
        assertEquals(new Rectangle(0, 0, 100, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 200, 100, false, false));
    }

    @Test
    public void testGetAOICenteredS2W() {
        assertEquals(new Rectangle(40, 45, 20, 10), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 20, 10, false, false));
    }

    @Test
    public void testGetAOICenteredS2WMax() {
        assertEquals(new Rectangle(0, 25, 100, 50), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 100, 50, false, false));
    }

    @Test
    public void testGetAOICenteredS2NOverflow() {
        assertEquals(new Rectangle(0, 0, 100, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 100, 200, false, false));
    }

    @Test
    public void testGetAOICenteredS2N() {
        assertEquals(new Rectangle(45, 40, 10, 20), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 10, 20, false, false));
    }

    @Test
    public void testGetAOICenteredS2NMax() {
        assertEquals(new Rectangle(25, 0, 50, 100), ImageServletResponseImpl.getAOI(100, 100, -1, -1, 50, 100, false, false));
    }

    @Test
    public void testGetAOICenteredW2SOverflow() {
        assertEquals(new Rectangle(0, 0, 200, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 333, 333, false, false));
    }

    @Test
    public void testGetAOICenteredW2S() {
        assertEquals(new Rectangle(75, 25, 50, 50), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 50, 50, false, false));
    }

    @Test
    public void testGetAOICenteredW2SMax() {
        assertEquals(new Rectangle(50, 0, 100, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 100, 100, false, false));
    }

    @Test
    public void testGetAOICenteredW2WOverflow() {
        assertEquals(new Rectangle(0, 0, 200, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 300, 200, false, false));
    }

    @Test
    public void testGetAOICenteredW2W() {
        assertEquals(new Rectangle(50, 25, 100, 50), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 100, 50, false, false));
    }

    @Test
    public void testGetAOICenteredW2WW() {
        assertEquals(new Rectangle(10, 40, 180, 20), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 180, 20, false, false));
    }

    @Test
    public void testGetAOICenteredW2WN() {
        assertEquals(new Rectangle(62, 25, 75, 50), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 75, 50, false, false));
    }

    @Test
    public void testGetAOICenteredW2WSame() {
        assertEquals(new Rectangle(0, 0, 200, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 200, 100, false, false));
    }

    @Test
    public void testGetAOICenteredW2NOverflow() {
        assertEquals(new Rectangle(50, 0, 100, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 100, 200, false, false));
    }

    @Test
    public void testGetAOICenteredW2N() {
        assertEquals(new Rectangle(83, 25, 33, 50), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 33, 50, false, false));
    }

    @Test
    public void testGetAOICenteredW2NMax() {
        assertEquals(new Rectangle(75, 0, 50, 100), ImageServletResponseImpl.getAOI(200, 100, -1, -1, 50, 100, false, false));
    }

    @Test
    public void testGetAOICenteredN2S() {
        assertEquals(new Rectangle(33, 83, 33, 33), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 33, 33, false, false));
    }

    @Test
    public void testGetAOICenteredN2SMax() {
        assertEquals(new Rectangle(0, 50, 100, 100), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 100, false, false));
    }

    @Test
    public void testGetAOICenteredN2WOverflow() {
        assertEquals(new Rectangle(0, 50, 100, 100), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 200, 100, false, false));
    }

    @Test
    public void testGetAOICenteredN2W() {
        assertEquals(new Rectangle(40, 95, 20, 10), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 20, 10, false, false));
    }

    @Test
    public void testGetAOICenteredN2WMax() {
        assertEquals(new Rectangle(0, 75, 100, 50), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 50, false, false));
    }

    @Test
    public void testGetAOICenteredN2N() {
        assertEquals(new Rectangle(45, 90, 10, 20), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 10, 20, false, false));
    }

    @Test
    public void testGetAOICenteredN2NSame() {
        assertEquals(new Rectangle(0, 0, 100, 200), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 200, false, false));
    }

    @Test
    public void testGetAOICenteredN2NN() {
        assertEquals(new Rectangle(37, 50, 25, 100), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 25, 100, false, false));
    }

    @Test
    public void testGetAOICenteredN2NW() {
        assertEquals(new Rectangle(12, 50, 75, 100), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 75, 100, false, false));
    }

    @Test
    public void testGetAOICenteredN2NWMax() {
        assertEquals(new Rectangle(0, 37, 100, 125), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 125, false, false));
    }

    @Test
    public void testGetAOICenteredN2NMax() {
        assertEquals(new Rectangle(0, 0, 100, 200), ImageServletResponseImpl.getAOI(100, 200, -1, -1, 100, 200, false, false));
    }

    // TODO: Test percent

    // TODO: Test getSize()...

    private static class BlackLabel extends JLabel {
        private final Paint checkeredBG;
        private boolean opaque = true;

        public BlackLabel(final String text, final BufferedImage outImage) {
            super(text, new BufferedImageIcon(outImage), JLabel.CENTER);
            setOpaque(true);
            setBackground(Color.BLACK);
            setForeground(Color.WHITE);
            setVerticalAlignment(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);
            setHorizontalTextPosition(JLabel.CENTER);

            checkeredBG = createTexture();
        }

        @Override
        public boolean isOpaque() {
            return opaque && super.isOpaque();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;

            int iconHeight = getIcon() == null ? 0 : getIcon().getIconHeight() + getIconTextGap();

            // Paint checkered bg behind icon
            g.setPaint(checkeredBG);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Paint black bg behind text
            g.setColor(getBackground());
            g.fillRect(0, iconHeight, getWidth(), getHeight() - iconHeight);

            try {
                opaque = false;
                super.paintComponent(g);
            }
            finally {
                opaque = true;
            }
        }

        private static Paint createTexture() {
            GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            BufferedImage pattern = graphicsConfiguration.createCompatibleImage(20, 20);
            Graphics2D g = pattern.createGraphics();
            try {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, pattern.getWidth(), pattern.getHeight());
                g.setColor(Color.GRAY);
                g.fillRect(0, 0, pattern.getWidth() / 2, pattern.getHeight() / 2);
                g.fillRect(pattern.getWidth() / 2, pattern.getHeight() / 2, pattern.getWidth() / 2, pattern.getHeight() / 2);
            }
            finally {
                g.dispose();
            }

            return new TexturePaint(pattern, new Rectangle(pattern.getWidth(), pattern.getHeight()));
        }
    }

    private static class MockLogger implements Answer<Void> {
        public Void answer(InvocationOnMock invocation) throws Throwable {
            // either log(String), log(String, Throwable) or log(Exception, String)
            Object[] arguments = invocation.getArguments();

            String msg = (String) (arguments[0] instanceof String ? arguments[0] : arguments[1]);
            Throwable t = (Throwable) (arguments[0] instanceof Exception ? arguments[0] : arguments.length > 1 ? arguments[1] : null);

            System.out.println("mock-context: " + msg);
            if (t != null) {
                t.printStackTrace(System.out);
            }

            return null;
        }
    }

}
