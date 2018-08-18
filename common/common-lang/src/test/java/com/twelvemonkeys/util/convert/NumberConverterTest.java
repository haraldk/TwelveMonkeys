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

package com.twelvemonkeys.util.convert;

/**
 * NumberConverterTest
 * <p/>
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/test/java/com/twelvemonkeys/util/convert/NumberConverterTestCase.java#2 $
 */
public class NumberConverterTest extends PropertyConverterAbstractTest {
    protected PropertyConverter makePropertyConverter() {
        return new NumberConverter();
    }

    protected Conversion[] getTestConversions() {
        return new Conversion[] {
                new Conversion("0", 0),
                new Conversion("1", 1),
                new Conversion("-1001", -1001),
                new Conversion("1E3", 1000, null, "1000"),

                new Conversion("-2", -2l),
                new Conversion("2000651651854", 2000651651854l),
                new Conversion("2E10", 20000000000l, null, "20000000000"),

                new Conversion("3", 3.0f),
                new Conversion("3.1", 3.1f),
                new Conversion("3.2", 3.2f, "#.#"),
                //new Conversion("3,3", new Float(3), "#", "3"), // Seems to need parseIntegerOnly
                new Conversion("-3.4", -3.4f),
                new Conversion("-3.5E10", -3.5e10f, null, "-35000000512"),

                new Conversion("4", 4.0),
                new Conversion("4.1", 4.1),
                new Conversion("4.2", 4.2, "#.#"),
                //new Conversion("4,3", new Double(4), "#", "4"), // Seems to need parseIntegerOnly
                new Conversion("-4.4", -4.4),
                new Conversion("-4.5E97", -4.5e97, null, "-45000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
        };
    }
}
