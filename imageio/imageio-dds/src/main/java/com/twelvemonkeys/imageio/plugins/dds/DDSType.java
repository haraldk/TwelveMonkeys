/*
 * Copyright (c) 2024, Paul Allen, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.dds;

enum DDSType {
    DXT1(0x31545844),
    DXT2(0x32545844),
    DXT3(0x33545844),
    DXT4(0x34545844),
    DXT5(0x35545844),
    DXT10(0x30315844),
    A1R5G5B5((1 << 16) | 2),
    X1R5G5B5((2 << 16) | 2),
    A4R4G4B4((3 << 16) | 2),
    X4R4G4B4((4 << 16) | 2),
    R5G6B5((5 << 16) | 2),
    R8G8B8((1 << 16) | 3),
    A8B8G8R8((1 << 16) | 4),
    X8B8G8R8((2 << 16) | 4),
    A8R8G8B8((3 << 16) | 4),
    X8R8G8B8((4 << 16) | 4);

    private final int value;

    DDSType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static DDSType valueOf(int value) {
        for (DDSType type : DDSType.values()) {
            if (value == type.value()) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown type: 0x%08x", value));
    }
}
