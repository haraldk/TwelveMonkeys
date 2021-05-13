/*
 * Copyright (c) 2016, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.tiff;

import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.nio.ByteOrder;

import static com.twelvemonkeys.imageio.plugins.tiff.TIFFMedataFormat.SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME;
import static com.twelvemonkeys.imageio.plugins.tiff.TIFFStreamMetadata.SUN_NATIVE_STREAM_METADATA_FORMAT_NAME;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * TIFFStreamMetadataTest
 */
public class TIFFStreamMetadataTest {
    // Test that we configure byte order of stream correctly (MM + II)

    @Test(expected = IllegalArgumentException.class)
    public void testConfigureStreamNullStream() throws IIOInvalidTreeException {
        TIFFStreamMetadata.configureStreamByteOrder(new TIFFStreamMetadata(), null);
    }

    @Test
    public void testConfigureStreamNullMetadata() throws IIOInvalidTreeException {
        ImageOutputStream stream = mock(ImageOutputStream.class);
        TIFFStreamMetadata.configureStreamByteOrder(null, stream);

        verify(stream, never()).setByteOrder(any(ByteOrder.class));
    }

    @Test
    public void testConfigureStreamMM() throws IIOInvalidTreeException {
        ImageOutputStream stream = mock(ImageOutputStream.class);
        TIFFStreamMetadata.configureStreamByteOrder(new TIFFStreamMetadata(BIG_ENDIAN), stream);

        verify(stream, only()).setByteOrder(BIG_ENDIAN);
    }

    @Test
    public void testConfigureStreamII() throws IIOInvalidTreeException {
        ImageOutputStream stream = mock(ImageOutputStream.class);
        TIFFStreamMetadata.configureStreamByteOrder(new TIFFStreamMetadata(LITTLE_ENDIAN), stream);

        verify(stream, only()).setByteOrder(LITTLE_ENDIAN);
    }

    @Test
    public void testConfigureStreamForeign() throws IIOInvalidTreeException {
        ImageOutputStream stream = mock(ImageOutputStream.class);
        IIOMetadata metadata = mock(IIOMetadata.class);
        when(metadata.getMetadataFormatNames()).thenReturn(new String[]{SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, "com_foo_supertiff_9.42"});
        when(metadata.getAsTree(eq(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME))).thenReturn(createForeignTree(LITTLE_ENDIAN));

        TIFFStreamMetadata.configureStreamByteOrder(metadata, stream);

        verify(stream, only()).setByteOrder(LITTLE_ENDIAN);
    }

    @Test
    public void testConfigureStreamImageMetadata() throws IIOInvalidTreeException {
        ImageOutputStream stream = mock(ImageOutputStream.class);
        IIOMetadata metadata = mock(IIOMetadata.class);
        when(metadata.getMetadataFormatNames()).thenReturn(new String[]{SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME});

        try {
            TIFFStreamMetadata.configureStreamByteOrder(metadata, stream);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
            assertTrue(expected.getMessage().toLowerCase().contains("unsupported stream metadata format"));
            assertTrue(expected.getMessage().contains("expected " + SUN_NATIVE_STREAM_METADATA_FORMAT_NAME));
            assertTrue(expected.getMessage().contains(SUN_NATIVE_IMAGE_METADATA_FORMAT_NAME));
        }
    }

    private IIOMetadataNode createForeignTree(ByteOrder order) {
        IIOMetadataNode root = new IIOMetadataNode(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME);
        IIOMetadataNode byteOrder = new IIOMetadataNode("ByteOrder");
        byteOrder.setAttribute("value", order == LITTLE_ENDIAN ? "LITTLE_ENDIAN" : "BIG_ENDIAN");
        root.appendChild(byteOrder);
        return root;
    }

    // Test that we merge correctly with "forreign" metadata class, as long as format names are the same (MM + II)
    @Test(expected = IllegalArgumentException.class)
    public void testMergeNull() throws IIOInvalidTreeException {
        new TIFFStreamMetadata().mergeTree(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergeIllegal() throws IIOInvalidTreeException {
        new TIFFStreamMetadata().mergeTree("com.foo.bar", createForeignTree(BIG_ENDIAN));
    }

    @Test
    public void testMergeII() throws IIOInvalidTreeException {
        TIFFStreamMetadata metadata = new TIFFStreamMetadata();
        metadata.mergeTree(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, createForeignTree(LITTLE_ENDIAN));
        assertEquals(LITTLE_ENDIAN, metadata.byteOrder);
    }

    @Test
    public void testMergeMM() throws IIOInvalidTreeException {
        TIFFStreamMetadata metadata = new TIFFStreamMetadata();
        metadata.byteOrder = LITTLE_ENDIAN;
        metadata.mergeTree(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, createForeignTree(BIG_ENDIAN));
        assertEquals(BIG_ENDIAN, metadata.byteOrder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAsTreeNull() {
        new TIFFStreamMetadata().getAsTree(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAsTreeIllegal() {
        new TIFFStreamMetadata().getAsTree("com.foo.bar");
    }

    @Test
    public void testGetAsTreeNative() {
        Node root = new TIFFStreamMetadata().getAsTree(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME);
        assertNotNull(root);
        assertThat(root, instanceOf(IIOMetadataNode.class));
        assertEquals(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, root.getNodeName());
        NodeList childNodes = root.getChildNodes();
        assertEquals(1, childNodes.getLength());
        assertThat(childNodes.item(0), instanceOf(IIOMetadataNode.class));
        IIOMetadataNode byteOrder = (IIOMetadataNode) childNodes.item(0);
        assertEquals("ByteOrder", byteOrder.getNodeName());
        assertEquals("BIG_ENDIAN", byteOrder.getAttribute("value"));
    }

    @Test
    public void testGetAsTreeNativeII() {
        Node root = new TIFFStreamMetadata(LITTLE_ENDIAN).getAsTree(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME);
        assertNotNull(root);
        assertThat(root, instanceOf(IIOMetadataNode.class));
        assertEquals(SUN_NATIVE_STREAM_METADATA_FORMAT_NAME, root.getNodeName());
        NodeList childNodes = root.getChildNodes();
        assertEquals(1, childNodes.getLength());
        assertThat(childNodes.item(0), instanceOf(IIOMetadataNode.class));
        IIOMetadataNode byteOrder = (IIOMetadataNode) childNodes.item(0);
        assertEquals("ByteOrder", byteOrder.getNodeName());
        assertEquals("LITTLE_ENDIAN", byteOrder.getAttribute("value"));
    }
}
