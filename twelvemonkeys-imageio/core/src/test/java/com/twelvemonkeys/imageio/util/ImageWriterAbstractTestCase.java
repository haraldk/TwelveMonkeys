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

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * ImageReaderAbstractTestCase class description.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ImageReaderAbstractTestCase.java,v 1.0 18.nov.2004 17:38:33 haku Exp $
 */
public abstract class ImageWriterAbstractTestCase extends MockObjectTestCase {

    protected abstract ImageWriter createImageWriter();

    protected abstract RenderedImage getTestData();

    public void testSetOutput() throws IOException {
        // Should just pass with no exceptions
        ImageWriter writer = createImageWriter();
        assertNotNull(writer);
        writer.setOutput(ImageIO.createImageOutputStream(new ByteArrayOutputStream()));
    }

    public void testSetOutputNull() {
        // Should just pass with no exceptions
        ImageWriter writer = createImageWriter();
        assertNotNull(writer);
        writer.setOutput(null);
    }

    public void testWrite() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));
        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        assertTrue("No image data written", buffer.size() > 0);
    }

    public void testWrite2() {
        // Note: There's a difference between new ImageOutputStreamImpl and
        // ImageIO.createImageOutputStream... Make sure writers handle both
        // cases
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            writer.setOutput(ImageIO.createImageOutputStream(buffer));
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail(e.getMessage());
        }

        assertTrue("No image data written", buffer.size() > 0);
    }

    public void testWriteNull() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));
        try {
            writer.write((RenderedImage) null);
        }
        catch(IllegalArgumentException ignore) {
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        assertTrue("Image data written", buffer.size() == 0);
    }

    public void testWriteNoOutput() {
        ImageWriter writer = createImageWriter();
        try {
            writer.write(getTestData());
        }
        catch (IllegalStateException ignore) {
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testGetDefaultWriteParam() {
        ImageWriter writer = createImageWriter();
        ImageWriteParam param = writer.getDefaultWriteParam();
        assertNotNull("Default ImageWriteParam is null", param);
    }

    // TODO: Test writing with params
    // TODO: Source region and subsampling at least

    public void testAddIIOWriteProgressListener() {
        ImageWriter writer = createImageWriter();
        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());
    }

    public void testAddIIOWriteProgressListenerNull() {
        ImageWriter writer = createImageWriter();
        writer.addIIOWriteProgressListener(null);
    }

    public void testAddIIOWriteProgressListenerCallbacks() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));

        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        String started = "Started";
        mockListener.expects(once()).method("imageStarted").withAnyArguments().id(started);
        mockListener.stubs().method("imageProgress").withAnyArguments().after(started);
        mockListener.expects(once()).method("imageComplete").withAnyArguments().after(started);

        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        mockListener.verify();
    }

    public void testMultipleAddIIOWriteProgressListenerCallbacks() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));

        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        String started = "Started";
        mockListener.expects(once()).method("imageStarted").withAnyArguments().id(started);
        mockListener.stubs().method("imageProgress").withAnyArguments().after(started);
        mockListener.expects(once()).method("imageComplete").withAnyArguments().after(started);

        Mock mockListenerToo = new Mock(IIOWriteProgressListener.class);
        String startedToo = "Started Two";
        mockListenerToo.expects(once()).method("imageStarted").withAnyArguments().id(startedToo);
        mockListenerToo.stubs().method("imageProgress").withAnyArguments().after(startedToo);
        mockListenerToo.expects(once()).method("imageComplete").withAnyArguments().after(startedToo);

        Mock mockListenerThree = new Mock(IIOWriteProgressListener.class);
        String startedThree = "Started Three";
        mockListenerThree.expects(once()).method("imageStarted").withAnyArguments().id(startedThree);
        mockListenerThree.stubs().method("imageProgress").withAnyArguments().after(startedThree);
        mockListenerThree.expects(once()).method("imageComplete").withAnyArguments().after(startedThree);


        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListenerToo.proxy());
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListenerThree.proxy());

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        mockListener.verify();
        mockListenerToo.verify();
        mockListenerThree.verify();
    }


    public void testRemoveIIOWriteProgressListenerNull() {
        ImageWriter writer = createImageWriter();
        writer.removeIIOWriteProgressListener(null);
    }

    public void testRemoveIIOWriteProgressListenerNone() {
        ImageWriter writer = createImageWriter();
        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        writer.removeIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());
    }

    public void testRemoveIIOWriteProgressListener() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));

        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        IIOWriteProgressListener listener = (IIOWriteProgressListener) mockListener.proxy();
        writer.addIIOWriteProgressListener(listener);
        writer.removeIIOWriteProgressListener(listener);

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        mockListener.verify();
    }

    public void testRemoveIIOWriteProgressListenerMultiple() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));


        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());

        Mock mockListenerToo = new Mock(IIOWriteProgressListener.class);
        mockListenerToo.stubs().method("imageStarted").withAnyArguments();
        mockListenerToo.stubs().method("imageProgress").withAnyArguments();
        mockListenerToo.stubs().method("imageComplete").withAnyArguments();
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListenerToo.proxy());

        writer.removeIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        mockListener.verify();
        mockListenerToo.verify();
    }


    public void testRemoveAllIIOWriteProgressListeners() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));


        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());

        writer.removeAllIIOWriteProgressListeners();

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        mockListener.verify();
    }

    public void testRemoveAllIIOWriteProgressListenersMultiple() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));


        Mock mockListener = new Mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListener.proxy());

        Mock mockListenerToo = new Mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener((IIOWriteProgressListener) mockListenerToo.proxy());

        writer.removeAllIIOWriteProgressListeners();

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        mockListener.verify();
        mockListenerToo.verify();
    }

}