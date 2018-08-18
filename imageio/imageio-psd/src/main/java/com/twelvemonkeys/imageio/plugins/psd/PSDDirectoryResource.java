/*
 * Copyright (c) 2017, Harald Kuhr
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

package com.twelvemonkeys.imageio.plugins.psd;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.lang.StringUtil;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * PSDDirectoryResource
 */
abstract class PSDDirectoryResource extends PSDImageResource {
    byte[] data;
    private Directory directory;

    PSDDirectoryResource(short resourceId, ImageInputStream input) throws IOException {
        super(resourceId, input);
    }

    @Override
    protected void readData(final ImageInputStream pInput) throws IOException {
        data = new byte[(int) size]; // TODO: Fix potential overflow, or document why that can't happen (read spec)
        pInput.readFully(data);
    }

    abstract Directory parseDirectory() throws IOException;

    final void initDirectory() throws IOException {
        if (directory == null) {
            directory = parseDirectory();
        }
    }

    Directory getDirectory() {
        return directory;
    }

    @Override
    public String toString() {
        StringBuilder builder = toStringBuilder();

        int length = Math.min(256, data.length);
        String data = StringUtil.decode(this.data, 0, length, "UTF-8").replace('\n', ' ').replaceAll("\\s+", " ");
        builder.append(", data: \"").append(data);

        if (length < this.data.length) {
            builder.append("...");
        }

        builder.append("\"]");

        return builder.toString();
    }
}
