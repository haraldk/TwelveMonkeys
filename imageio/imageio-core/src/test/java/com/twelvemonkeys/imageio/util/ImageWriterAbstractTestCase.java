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

import org.junit.Test;
import org.mockito.InOrder;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * ImageReaderAbstractTestCase class description.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: ImageReaderAbstractTestCase.java,v 1.0 18.nov.2004 17:38:33 haku Exp $
 */
public abstract class ImageWriterAbstractTestCase {

    protected abstract ImageWriter createImageWriter();

    protected abstract RenderedImage getTestData();

    @Test
    public void testSetOutput() throws IOException {
        // Should just pass with no exceptions
        ImageWriter writer = createImageWriter();
        assertNotNull(writer);
        writer.setOutput(ImageIO.createImageOutputStream(new ByteArrayOutputStream()));
    }

    @Test
    public void testSetOutputNull() {
        // Should just pass with no exceptions
        ImageWriter writer = createImageWriter();
        assertNotNull(writer);
        writer.setOutput(null);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testGetDefaultWriteParam() {
        ImageWriter writer = createImageWriter();
        ImageWriteParam param = writer.getDefaultWriteParam();
        assertNotNull("Default ImageWriteParam is null", param);
    }

    // TODO: Test writing with params
    // TODO: Source region and subsampling at least

    @Test
    public void testAddIIOWriteProgressListener() {
        ImageWriter writer = createImageWriter();
        writer.addIIOWriteProgressListener(mock(IIOWriteProgressListener.class));
    }

    @Test
    public void testAddIIOWriteProgressListenerNull() {
        ImageWriter writer = createImageWriter();
        writer.addIIOWriteProgressListener(null);
    }

    @Test
    public void testAddIIOWriteProgressListenerCallbacks() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));

        IIOWriteProgressListener listener = mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener(listener);

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        InOrder ordered = inOrder(listener);
        ordered.verify(listener).imageStarted(writer, 0);
        ordered.verify(listener, atLeastOnce()).imageProgress(eq(writer), anyInt());
        ordered.verify(listener).imageComplete(writer);
    }

    @Test
    public void testMultipleAddIIOWriteProgressListenerCallbacks() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));

        IIOWriteProgressListener listener = mock(IIOWriteProgressListener.class);
        IIOWriteProgressListener listenerToo = mock(IIOWriteProgressListener.class);
        IIOWriteProgressListener listenerThree = mock(IIOWriteProgressListener.class);

        writer.addIIOWriteProgressListener(listener);
        writer.addIIOWriteProgressListener(listenerToo);
        writer.addIIOWriteProgressListener(listenerThree);

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // At least imageStarted and imageComplete, plus any number of imageProgress
        InOrder ordered = inOrder(listener, listenerToo, listenerThree);

        ordered.verify(listener).imageStarted(writer, 0);
        ordered.verify(listenerToo).imageStarted(writer, 0);
        ordered.verify(listenerThree).imageStarted(writer, 0);

        ordered.verify(listener, atLeastOnce()).imageProgress(eq(writer), anyInt());
        ordered.verify(listenerToo, atLeastOnce()).imageProgress(eq(writer), anyInt());
        ordered.verify(listenerThree, atLeastOnce()).imageProgress(eq(writer), anyInt());

        ordered.verify(listener).imageComplete(writer);
        ordered.verify(listenerToo).imageComplete(writer);
        ordered.verify(listenerThree).imageComplete(writer);
    }

    @Test
    public void testRemoveIIOWriteProgressListenerNull() {
        ImageWriter writer = createImageWriter();
        writer.removeIIOWriteProgressListener(null);
    }

    @Test
    public void testRemoveIIOWriteProgressListenerNone() {
        ImageWriter writer = createImageWriter();
        writer.removeIIOWriteProgressListener(mock(IIOWriteProgressListener.class));
    }

    @Test
    public void testRemoveIIOWriteProgressListener() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));

        IIOWriteProgressListener listener = mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener(listener);
        writer.removeIIOWriteProgressListener(listener);

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        verifyZeroInteractions(listener);
    }

    @Test
    public void testRemoveIIOWriteProgressListenerMultiple() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));

        IIOWriteProgressListener listener = mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener(listener);

        IIOWriteProgressListener listenerToo = mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener(listenerToo);

        writer.removeIIOWriteProgressListener(listener);

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        verifyZeroInteractions(listener);

        // At least imageStarted and imageComplete, plus any number of imageProgress
        InOrder ordered = inOrder(listenerToo);
        ordered.verify(listenerToo).imageStarted(writer, 0);
        ordered.verify(listenerToo, atLeastOnce()).imageProgress(eq(writer), anyInt());
        ordered.verify(listenerToo).imageComplete(writer);

    }

    @Test
    public void testRemoveAllIIOWriteProgressListeners() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));


        IIOWriteProgressListener listener = mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener(listener);

        writer.removeAllIIOWriteProgressListeners();

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        verifyZeroInteractions(listener);
    }

    @Test
    public void testRemoveAllIIOWriteProgressListenersMultiple() throws IOException {
        ImageWriter writer = createImageWriter();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(buffer));


        IIOWriteProgressListener listener = mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener(listener);

        IIOWriteProgressListener listenerToo = mock(IIOWriteProgressListener.class);
        writer.addIIOWriteProgressListener(listenerToo);

        writer.removeAllIIOWriteProgressListeners();

        try {
            writer.write(getTestData());
        }
        catch (IOException e) {
            fail("Could not write image");
        }

        // Should not have called any methods...
        verifyZeroInteractions(listener);
        verifyZeroInteractions(listenerToo);
    }
}