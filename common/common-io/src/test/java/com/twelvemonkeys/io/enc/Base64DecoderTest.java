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

package com.twelvemonkeys.io.enc;

import com.twelvemonkeys.io.FileUtil;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Base64DecoderTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/io/enc/Base64DecoderTestCase.java#1 $
 */
public class Base64DecoderTest extends DecoderAbstractTest {

    public Decoder createDecoder() {
        return new Base64Decoder();
    }

    public Encoder createCompatibleEncoder() {
        return new Base64Encoder();
    }

    @Test
    public void testEmptyDecode2() throws IOException {
        String data = "";

        InputStream in = new DecoderStream(new ByteArrayInputStream(data.getBytes()), createDecoder());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        FileUtil.copy(in, bytes);

        assertEquals("Strings does not match", "", new String(bytes.toByteArray()));
    }

    @Test
    public void testShortDecode() throws IOException {
        String data = "dGVzdA==";

        InputStream in = new DecoderStream(new ByteArrayInputStream(data.getBytes()), createDecoder());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        FileUtil.copy(in, bytes);

        assertEquals("Strings does not match", "test", new String(bytes.toByteArray()));
    }

    @Test
    public void testLongDecode() throws IOException {
        String data = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVlciBhZGlwaXNjaW5nIGVsaXQuIEZ1" +
                "c2NlIGVzdC4gTW9yYmkgbHVjdHVzIGNvbnNlY3RldHVlciBqdXN0by4gVml2YW11cyBkYXBpYnVzIGxh" +
                "b3JlZXQgcHVydXMuIE51bmMgdml2ZXJyYSBkaWN0dW0gbmlzbC4gSW50ZWdlciB1bGxhbWNvcnBlciwg" +
                "bmlzaSBpbiBkaWN0dW0gYW1ldC4=";

        InputStream in = new DecoderStream(new ByteArrayInputStream(data.getBytes()), createDecoder());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        FileUtil.copy(in, bytes);

        assertEquals("Strings does not match",
                     "Lorem ipsum dolor sit amet, consectetuer adipiscing " +
                     "elit. Fusce est. Morbi luctus consectetuer justo. Vivamus " +
                     "dapibus laoreet purus. Nunc viverra dictum nisl. Integer " +
                     "ullamcorper, nisi in dictum amet.",
                     new String(bytes.toByteArray()));
    }
}