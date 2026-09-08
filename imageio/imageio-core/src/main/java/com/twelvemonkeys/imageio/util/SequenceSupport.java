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

package com.twelvemonkeys.imageio.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

/**
 * A tiny utility class that keeps state for sequences.
 * For use by {@code ImageWriter} implementations that supports sequence (multiple images in same stream).
 *
 * @see ImageWriter#canWriteSequence()
 */
public final class SequenceSupport {
    // Initial state, no sequence running
    private int index = -1;

    /**
     * Resets the sequence to initial state, regardless of the current sequence state.
     */
    public void reset() {
        index = -1;
    }

    /**
     * Starts a new sequence.
     *
     * @throws IllegalStateException if a sequence is already running.
     * @see ImageWriter#prepareWriteSequence(IIOMetadata)
     */
    public void start() {
        if (index >= 0) {
            throw new IllegalStateException("prepareWriteSequence already invoked");
        }

        index = 0;
    }

    /**
     * Advances the current sequence.
     *
     * @return the current sequence index.
     * @throws IllegalStateException if a sequence is not running.
     * @see ImageWriter#writeToSequence(IIOImage, ImageWriteParam)
     */
    public int advance() {
        if (index < 0) {
            throw new IllegalStateException("prepareWriteSequence not invoked");
        }

        return index++;
    }

    /**
     * Gets the current sequence index.
     *
     * @return the current sequence index, or {@code -1} if a sequence is not running.
     */
    public int current() {
        // This method does not throw IllegalStateException, to allow
        // ImageWriters to use the index in cases that may or may not
        // happen "inside" a sequence.
        // I'm not entirely sure if this is a good idea...
        return index;
    }

    /**
     * Ends the current sequence.
     * The sequence is reset to initial state, and a new sequence may be started.
     *
     * @return the current (last) sequence index
     * @throws IllegalStateException if a sequence is not running.
     * @see ImageWriter#endWriteSequence()
     */
    public int end() {
        if (index < 0) {
            throw new IllegalStateException("prepareWriteSequence not invoked");
        }

        int last = index;
        index = -1;

        return last;
    }
}
