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

package com.twelvemonkeys.imageio.util;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOWriteProgressListener;

/**
 * ProgressListenerBase
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: ProgressListenerBase.java,v 1.0 26.aug.2005 14:29:42 haku Exp$
 */
public abstract class ProgressListenerBase implements IIOReadProgressListener, IIOWriteProgressListener {
    protected ProgressListenerBase() {
    }

    public void imageComplete(ImageReader source) {
    }

    public void imageProgress(ImageReader source, float percentageDone) {
    }

    public void imageStarted(ImageReader source, int imageIndex) {
    }

    public void readAborted(ImageReader source) {
    }

    public void sequenceComplete(ImageReader source) {
    }

    public void sequenceStarted(ImageReader source, int minIndex) {
    }

    public void thumbnailComplete(ImageReader source) {
    }

    public void thumbnailProgress(ImageReader source, float percentageDone) {
    }

    public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
    }

    public void imageComplete(ImageWriter source) {
    }

    public void imageProgress(ImageWriter source, float percentageDone) {
    }

    public void imageStarted(ImageWriter source, int imageIndex) {
    }

    public void thumbnailComplete(ImageWriter source) {
    }

    public void thumbnailProgress(ImageWriter source, float percentageDone) {
    }

    public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
    }

    public void writeAborted(ImageWriter source) {
    }
}
