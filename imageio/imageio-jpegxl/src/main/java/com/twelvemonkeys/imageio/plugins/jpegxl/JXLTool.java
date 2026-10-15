/*
 * Copyright (c) 2026, TwelveMonkeys contributors
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
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.imageio.plugins.jpegxl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the external libjxl tools ({@code cjxl}/{@code djxl}).
 * This plugin delegates JPEG XL coding to libjxl because there is no
 * pure-Java JPEG XL codec; {@code cjxl} is also the only tool that can
 * transcode legacy JPEG to JPEG XL losslessly (bit-exact).
 */
final class JXLTool {
    private JXLTool() {
    }

    static Path createTempDir() throws IOException {
        return Files.createTempDirectory("twelvemonkeys-jpegxl-");
    }

    static void deleteRecursively(final Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                }
                catch (IOException ignored) {
                    // Best effort cleanup of temp files.
                }
            });
        }
        catch (IOException ignored) {
            // Best effort cleanup of temp files.
        }
    }

    static void run(final String tool, final Path... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(tool);
        for (Path arg : args) {
            command.add(arg.toString());
        }

        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        }
        catch (IOException e) {
            throw new IOException("JPEG XL requires the libjxl '" + tool + "' tool on PATH", e);
        }

        try {
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IOException("'" + tool + "' failed with exit code " + exit);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running '" + tool + "'", e);
        }
    }
}
