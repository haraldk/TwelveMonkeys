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

package com.twelvemonkeys.io;

import java.io.IOException;

/**
 * Interface for seekable streams.
 *
 * @see SeekableInputStream
 * @see SeekableOutputStream
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/io/Seekable.java#1 $
 */
public interface Seekable {

    /**
     * Returns the current byte position of the stream. The next read will take
     * place starting at this offset.
     *
     * @return a {@code long} containing the position of the stream.
     * @throws IOException if an I/O error occurs.
     */
    long getStreamPosition() throws IOException;

    /**
     * Sets the current stream position to the desired location.
     * The next read will occur at this location.
     * <p>
     * An {@code IndexOutOfBoundsException} will be thrown if pPosition is smaller
     * than the flushed position (as returned by {@link #getFlushedPosition()}).
     * </p>
     * <p>
     * It is legal to seek past the end of the file; an {@code EOFException}
     * will be thrown only if a read is performed.
     * </p>
     *
     * @param pPosition a long containing the desired file pointer position.
     *
     * @throws IndexOutOfBoundsException if {@code pPosition} is smaller than
     * the flushed position.
     * @throws IOException if any other I/O error occurs.
     */
    void seek(long pPosition) throws IOException;

    /**
     * Marks a position in the stream to be returned to by a subsequent call to
     * reset.
     * Unlike a standard {@code InputStream}, all {@code Seekable}
     * streams upport marking. Additionally, calls to {@code mark} and
     * {@code reset} may be nested arbitrarily.
     * <p>
     * Unlike the {@code mark} methods declared by the {@code Reader} or
     * {@code InputStream}
     * interfaces, no {@code readLimit} parameter is used. An arbitrary amount
     * of data may be read following the call to {@code mark}.
     * </p>
     */
    void mark();

    /**
     * Returns the file pointer to its previous position,
     * at the time of the most recent unmatched call to mark.
     * <p>
     * Calls to reset without a corresponding call to mark will either:
     * </p>
     * <ul>
     * <li>throw an {@code IOException}</li>
     * <li>or, reset to the beginning of the stream.</li>
     * </ul>
     * <p>
     * An {@code IOException} will be thrown if the previous marked position
     * lies in the discarded portion of the stream.
     * </p>
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.InputStream#reset()
     */
    void reset() throws IOException;

    /**
     * Discards the initial portion of the stream prior to the indicated
     * postion. Attempting to seek to an offset within the flushed portion of
     * the stream will result in an {@code IndexOutOfBoundsException}.
     * <p>
     * Calling {@code flushBefore} may allow classes implementing this
     * interface to free up resources such as memory or disk space that are
     * being used to store data from the stream.
     * </p>
     *
     * @param pPosition a long containing the length of the file prefix that
     * may be flushed.
     *
     * @throws IndexOutOfBoundsException if {@code pPosition} lies in the
     * flushed portion of the stream or past the current stream position.
     * @throws IOException if an I/O error occurs.
     */
    void flushBefore(long pPosition) throws IOException;

    /**
     * Discards the initial position of the stream prior to the current stream
     * position. Equivalent to {@code flushBefore(getStreamPosition())}.
     *
     * @throws IOException if an I/O error occurs.
     */
    void flush() throws IOException;

    /**
     * Returns the earliest position in the stream to which seeking may be
     * performed. The returned value will be the maximum of all values passed
     * into previous calls to {@code flushBefore}.
     *
     * @return the earliest legal position for seeking, as a {@code long}.
     *
     * @throws IOException if an I/O error occurs.
     */
    long getFlushedPosition() throws IOException;

    /**
     * Returns true if this {@code Seekable} stream caches data itself in order
     * to allow seeking backwards. Applications may consult this in order to
     * decide how frequently, or whether, to flush in order to conserve cache
     * resources.
     *
     * @return {@code true} if this {@code Seekable} caches data.
     * @see #isCachedMemory()
     * @see #isCachedFile()
     */
    boolean isCached();

    /**
     * Returns true if this {@code Seekable} stream caches data itself in order
     * to allow seeking backwards, and the cache is kept in main memory.
     * Applications may consult this in order to decide how frequently, or
     * whether, to flush in order to conserve cache resources.
     *
     * @return {@code true} if this {@code Seekable} caches data in main
     * memory.
     * @see #isCached()
     * @see #isCachedFile()
     */
    boolean isCachedMemory();

    /**
     * Returns true if this {@code Seekable} stream caches data itself in
     * order to allow seeking backwards, and the cache is kept in a
     * temporary file.
     * Applications may consult this in order to decide how frequently,
     * or whether, to flush in order to conserve cache resources.
     *
     * @return {@code true} if this {@code Seekable} caches data in a
     * temporary file.
     * @see #isCached
     * @see #isCachedMemory
     */
    boolean isCachedFile();

    /**
     * Closes the stream.
     *
     * @throws java.io.IOException if the stream can't be closed.
     */
    void close() throws IOException;
}
