/*
 * Copyright (c) 2024, Harald Kuhr
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

import com.twelvemonkeys.imageio.util.ImageReaderAbstractTest;

import javax.imageio.spi.ImageReaderSpi;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DDSImageReaderTest extends ImageReaderAbstractTest<DDSImageReader> {
    @Override
    protected ImageReaderSpi createProvider() {
        return new DDSImageReaderSpi();
    }

    @Override
    protected List<TestData> getTestData() {
        Dimension dim256 = new Dimension(256, 256);
        Dimension dim128 = new Dimension(128, 128);
        Dimension dim64 = new Dimension(64, 64);
        Dimension dim32 = new Dimension(32, 32);
        Dimension dim16 = new Dimension(16, 16);
        Dimension dim8 = new Dimension(8, 8);
        Dimension dim4 = new Dimension(4, 4);
        Dimension dim2 = new Dimension(2, 2);
        Dimension dim1 = new Dimension(1, 1);

        return Arrays.asList(
                new TestData(getClassLoaderResource("/dds/dds_A1R5G5B5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A1R5G5B5_mipmap.dds"), dim256, dim128, dim64, dim32, dim16, dim8, dim4, dim2, dim1),
                new TestData(getClassLoaderResource("/dds/dds_A4R4G4B4.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A4R4G4B4_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_A8B8G8R8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A8B8G8R8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_A8R8G8B8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_A8R8G8B8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT1.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT1_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT2.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT2_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT3.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT3_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT4.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT4_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_DXT5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_DXT5_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_R5G6B5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_R5G6B5_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_R8G8B8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_R8G8B8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X1R5G5B5.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X1R5G5B5_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X4R4G4B4.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X4R4G4B4_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X8B8G8R8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X8B8G8R8_mipmap.dds"), dim256, dim128, dim64),
                new TestData(getClassLoaderResource("/dds/dds_X8R8G8B8.dds"), dim256),
                new TestData(getClassLoaderResource("/dds/dds_X8R8G8B8_mipmap.dds"), dim256, dim128, dim64)
        );
    }

    @Override
    protected List<String> getFormatNames() {
        return Arrays.asList("DDS", "dds");
    }

    @Override
    protected List<String> getSuffixes() {
        return Collections.singletonList("dds");
    }

    @Override
    protected List<String> getMIMETypes() {
        return Collections.singletonList("image/vnd-ms.dds");
    }
}
