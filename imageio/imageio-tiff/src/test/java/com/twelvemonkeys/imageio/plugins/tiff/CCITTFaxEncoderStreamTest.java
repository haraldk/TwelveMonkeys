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

package com.twelvemonkeys.imageio.plugins.tiff;

import org.junit.Test;

import com.twelvemonkeys.imageio.plugins.tiff.CCITTFaxEncoderStream.Code;

import java.io.IOException;
import static org.junit.Assert.*;

/**
 * CCITTFaxEncoderStreamTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author$
 * @version $Id$
 */
public class CCITTFaxEncoderStreamTest {

    @Test
    public void testBuildCodes() throws IOException {
        assertTrue(CCITTFaxEncoderStream.WHITE_TERMINATING_CODES.length == 64);
        for (Code code : CCITTFaxEncoderStream.WHITE_TERMINATING_CODES) {
            assertNotNull(code);
        }
        assertTrue(CCITTFaxEncoderStream.WHITE_NONTERMINATING_CODES.length == 40);
        for (Code code : CCITTFaxEncoderStream.WHITE_NONTERMINATING_CODES) {
            assertNotNull(code);
        }
        assertTrue(CCITTFaxEncoderStream.BLACK_TERMINATING_CODES.length == 64);
        for (Code code : CCITTFaxEncoderStream.BLACK_TERMINATING_CODES) {
            assertNotNull(code);
        }
        assertTrue(CCITTFaxEncoderStream.BLACK_NONTERMINATING_CODES.length == 40);
        for (Code code : CCITTFaxEncoderStream.BLACK_NONTERMINATING_CODES) {
            assertNotNull(code);
        }
    }
}
