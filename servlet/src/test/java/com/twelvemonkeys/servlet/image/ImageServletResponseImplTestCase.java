package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.image.BufferedImageIcon;
import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.io.FileUtil;
import com.twelvemonkeys.servlet.OutputStreamAdapter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * ImageServletResponseImplTestCase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/test/java/com/twelvemonkeys/servlet/image/ImageServletResponseImplTestCase.java#6 $
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

    private static final Dimension IMAGE_DIMENSION_PNG = new Dimension(300, 410);
    private static final Dimension IMAGE_DIMENSION_GIF = new Dimension(250, 250);

    private HttpServletRequest request;
    private ServletContext context;

    @Before
    public void init() throws Exception {
        request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_PNG);

//        mockRequest.stubs().method("getAttribute").will(returnValue(null));
//        mockRequest.stubs().method("getContextPath").will(returnValue("/ape"));
//        mockRequest.stubs().method("getRequestURI").will(returnValue("/ape/" + IMAGE_NAME_PNG));
//        mockRequest.stubs().method("getParameter").will(returnValue(null));
//        request = (HttpServletRequest) mockRequest.proxy();

        context = mock(ServletContext.class);
        when(context.getResource("/" + IMAGE_NAME_PNG)).thenReturn(getClass().getResource(IMAGE_NAME_PNG));
        when(context.getResource("/" + IMAGE_NAME_GIF)).thenReturn(getClass().getResource(IMAGE_NAME_GIF));
        when(context.getMimeType("file.bmp")).thenReturn(CONTENT_TYPE_BMP);
        when(context.getMimeType("file.foo")).thenReturn(CONTENT_TYPE_FOO);
        when(context.getMimeType("file.gif")).thenReturn(CONTENT_TYPE_GIF);
        when(context.getMimeType("file.jpeg")).thenReturn(CONTENT_TYPE_JPEG);
        when(context.getMimeType("file.png")).thenReturn(CONTENT_TYPE_PNG);
        when(context.getMimeType("file.txt")).thenReturn(CONTENT_TYPE_TEXT);


//        Mock mockContext = mock(ServletContext.class);
//        mockContext.stubs().method("getResource").with(eq("/" + IMAGE_NAME_PNG)).will(returnValue(getClass().getResource(IMAGE_NAME_PNG)));
//        mockContext.stubs().method("getResource").with(eq("/" + IMAGE_NAME_GIF)).will(returnValue(getClass().getResource(IMAGE_NAME_GIF)));
//        mockContext.stubs().method("log").withAnyArguments(); // Just suppress the logging
//        mockContext.stubs().method("getMimeType").with(eq("file.bmp")).will(returnValue(CONTENT_TYPE_BMP));
//        mockContext.stubs().method("getMimeType").with(eq("file.foo")).will(returnValue(CONTENT_TYPE_FOO));
//        mockContext.stubs().method("getMimeType").with(eq("file.gif")).will(returnValue(CONTENT_TYPE_GIF));
//        mockContext.stubs().method("getMimeType").with(eq("file.jpeg")).will(returnValue(CONTENT_TYPE_JPEG));
//        mockContext.stubs().method("getMimeType").with(eq("file.png")).will(returnValue(CONTENT_TYPE_PNG));
//        mockContext.stubs().method("getMimeType").with(eq("file.txt")).will(returnValue(CONTENT_TYPE_TEXT));
//        context = (ServletContext) mockContext.proxy();
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_PNG));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_PNG));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

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
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));
//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_JPEG));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Force transcode to JPEG
        imageResponse.setOutputContentType("image/jpeg");

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        /*
        File tempFile = File.createTempFile("imageservlet-test-", ".jpeg");
        FileOutputStream stream = new FileOutputStream(tempFile);
        out.writeTo(stream);
        stream.close();
        System.err.println("open " + tempFile);
        */

        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

    @Ignore
    @Test
    public void testTranscodeResponsePNGToGIFWithQuality() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_GIF));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Force transcode to GIF
        imageResponse.setOutputContentType("image/gif");
        // TODO: Set quality...!

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(outImage);
        assertEquals(IMAGE_DIMENSION_PNG.width, outImage.getWidth());
        assertEquals(IMAGE_DIMENSION_PNG.height, outImage.getHeight());

        BufferedImage image = ImageIO.read(context.getResource("/" + IMAGE_NAME_PNG));

        /*
        showIt(outImage, image);
        */

        // Should keep transparency, but is now binary
        assertSimilarImageTransparent(image, outImage, 5f);

        verify(response).setContentType(CONTENT_TYPE_GIF);
        verify(response).getOutputStream();
    }

    @Ignore
    @Test
    public void testTranscodeResponsePNGToGIF() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));

//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_GIF));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Force transcode to GIF
        imageResponse.setOutputContentType("image/gif");

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(outImage);
        assertEquals(IMAGE_DIMENSION_PNG.width, outImage.getWidth());
        assertEquals(IMAGE_DIMENSION_PNG.height, outImage.getHeight());

        BufferedImage image = ImageIO.read(context.getResource("/" + IMAGE_NAME_PNG));

        BufferedImage diff = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgbIn = image.getRGB(x, y);
                int rgbOut = outImage.getRGB(x, y);
                
                int aDiff = (rgbIn >> 24) & 0xff - (rgbOut >> 24) & 0xff;
                int rDiff = (rgbIn >> 16) & 0xff - (rgbOut >> 16) & 0xff;
                int gDiff = (rgbIn >> 8) & 0xff - (rgbOut >> 8) & 0xff;
                int bDiff = rgbIn & 0xff - rgbOut & 0xff;
                
                int gray = Math.min((int) Math.round((Math.abs(rDiff) + Math.abs(gDiff) + Math.abs(bDiff)) / 3d), 255);
                
                diff.setRGB(x, y, gray << 16 | gray << 8 | gray);
            }
        }
        
        
        /**/
        showIt(image, outImage, diff);
        //*/

        // Should keep transparency, but is now binary
        assertSimilarImageTransparent(image, outImage, 5f);

        verify(response).setContentType(CONTENT_TYPE_GIF);
        verify(response).getOutputStream();
    }

    private static void showIt(final BufferedImage expected, final BufferedImage actual, final BufferedImage diff) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                    panel.add(new BlackLabel("expected", expected));
                    panel.add(new BlackLabel("actual", actual));
                    if (diff != null) {
                        panel.add(new BlackLabel("diff", diff));
                    }
                    JScrollPane scroll = new JScrollPane(panel);
                    scroll.setBorder(BorderFactory.createEmptyBorder());
                    JOptionPane.showMessageDialog(null, scroll);
                }
            });
        }
        catch (InterruptedException ignore) {
        }
        catch (InvocationTargetException ignore) {
        }
    }

    @Test
    public void testTranscodeResponseIndexedCM() throws IOException {
        // Custom setup
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/ape");
        when(request.getRequestURI()).thenReturn("/ape/" + IMAGE_NAME_GIF);
//        Mock mockRequest = mock(HttpServletRequest.class);
//        mockRequest.stubs().method("getAttribute").will(returnValue(null));
//        mockRequest.stubs().method("getContextPath").will(returnValue("/ape"));
//        mockRequest.stubs().method("getRequestURI").will(returnValue("/ape/" + IMAGE_NAME_GIF));
//        mockRequest.stubs().method("getParameter").will(returnValue(null));
//        HttpServletRequest request = (HttpServletRequest) mockRequest.proxy();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));
//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_JPEG));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

        ImageServletResponseImpl imageResponse = new ImageServletResponseImpl(request, response, context);
        fakeResponse(request, imageResponse);

        // Force transcode to JPEG
        imageResponse.setOutputContentType("image/jpeg");

        // Flush image to wrapped response
        imageResponse.flush();

        assertTrue("Content has no data", out.size() > 0);

        // Test that image data is still readable
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(outImage);
        assertEquals(IMAGE_DIMENSION_GIF.width, outImage.getWidth());
        assertEquals(IMAGE_DIMENSION_GIF.height, outImage.getHeight());

        BufferedImage image = flatten(ImageIO.read(context.getResource("/" + IMAGE_NAME_GIF)), Color.WHITE);

        assertSimilarImage(image, outImage, 96f);
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
        for (int y = 0; y < pExpected.getHeight(); y++) {
            for (int x = 0; x < pExpected.getWidth(); x++) {
                int expected = pExpected.getRGB(x, y);
                int actual = pActual.getRGB(x, y);

                int alpha = (expected >> 24) & 0xff;
                boolean transparent = alpha < 40;
                if (transparent) {
                    assertEquals(0, (actual >> 24) & 0xff);
                }
                else {
                    assertEquals((expected >> 16) & 0xff, (actual >> 16) & 0xff, pArtifactThreshold);
                    assertEquals((expected >>  8) & 0xff, (actual >>  8) & 0xff, pArtifactThreshold);
                    assertEquals( expected        & 0xff,  actual        & 0xff, pArtifactThreshold);
                }
            }
        }
    }

    @Test
    public void testReplaceResponse() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));
//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_BMP));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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
//        Mock mockRequest = mock(HttpServletRequest.class);
//        mockRequest.stubs().method("getAttribute").will(returnValue(null));
//        mockRequest.stubs().method("getContextPath").will(returnValue("/ape"));
//        mockRequest.stubs().method("getRequestURI").will(returnValue("/ape/monkey-business.gif"));
//        mockRequest.stubs().method("getParameter").will(returnValue(null));
//        request = (HttpServletRequest) mockRequest.proxy();

        HttpServletResponse response = mock(HttpServletResponse.class);
//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("sendError").with(eq(404), ANYTHING);
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

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
//        Mock mockRequest = mock(HttpServletRequest.class);
//        mockRequest.stubs().method("getAttribute").will(returnValue(null));
//        mockRequest.stubs().method("getContextPath").will(returnValue("/ape"));
//        mockRequest.stubs().method("getRequestURI").will(returnValue("/ape/foo.txt"));
//        mockRequest.stubs().method("getParameter").will(returnValue(null));
//        request = (HttpServletRequest) mockRequest.proxy();

        HttpServletResponse response = mock(HttpServletResponse.class);
//        Mock mockResponse = mock(HttpServletResponse.class);
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

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
//        Mock mockResponse = mock(HttpServletResponse.class);
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

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
        
//        Mock mockRequest = mock(HttpServletRequest.class);
//        mockRequest.stubs().method("getAttribute").withAnyArguments().will(returnValue(null));
//        mockRequest.stubs().method("getAttribute").with(eq(ImageServletResponse.ATTRIB_AOI)).will(returnValue(sourceRegion));
//        mockRequest.stubs().method("getContextPath").will(returnValue("/ape"));
//        mockRequest.stubs().method("getRequestURI").will(returnValue("/ape/" + IMAGE_NAME_PNG));
//        mockRequest.stubs().method("getParameter").will(returnValue(null));
//        HttpServletRequest request = (HttpServletRequest) mockRequest.proxy();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new OutputStreamAdapter(out));
//        Mock mockResponse = mock(HttpServletResponse.class);
//        mockResponse.expects(once()).method("setContentType").with(eq(CONTENT_TYPE_PNG));
//        mockResponse.expects(once()).method("getOutputStream").will(returnValue(new OutputStreamAdapter(out)));
//        HttpServletResponse response = (HttpServletResponse) mockResponse.proxy();

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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();

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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
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
        BufferedImage outImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
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
        public BlackLabel(final String text, final BufferedImage outImage) {
            super(text, new BufferedImageIcon(outImage), JLabel.CENTER);
            setOpaque(true);
            setBackground(Color.BLACK);
            setForeground(Color.WHITE);
            setVerticalAlignment(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);
            setHorizontalTextPosition(JLabel.CENTER);
        }
    }
}
