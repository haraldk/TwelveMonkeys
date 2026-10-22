/*
 * Copyright (c) 2026, Harald Kuhr
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

package com.twelvemonkeys.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IgnoreCaseMapTest
 */
public class IgnoreCaseMapTest {
    @Test
    public void testGetIgnoresCase() {
        Map<String, String> map = new IgnoreCaseMap<>();
        map.put("TITLE", "value");

        assertEquals("value", map.get("title"));
        assertEquals("value", map.get("Title"));
        assertTrue(map.containsKey("title"));
    }

    @Test
    public void testGetIgnoresCaseIndependentOfDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();

        try {
            // In the Turkish locale, "i".toUpperCase() is the dotted capital I (U+0130), not "I"
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            Map<String, String> map = new IgnoreCaseMap<>();
            map.put("TITLE", "value");

            assertEquals("value", map.get("title"));
            assertTrue(map.containsKey("title"));
        }
        finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
