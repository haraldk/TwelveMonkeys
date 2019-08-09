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

package com.twelvemonkeys.image;

import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.ArrayList;
import java.util.List;

/**
 * AbstractImageSource
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/AbstractImageSource.java#1 $
 */
public abstract class AbstractImageSource implements ImageProducer {
    private List<ImageConsumer> consumers = new ArrayList<ImageConsumer>();
    protected int width;
    protected int height;
    protected int xOff;
    protected int yOff;

    // ImageProducer interface
    public void addConsumer(final ImageConsumer pConsumer) {
        if (consumers.contains(pConsumer)) {
            return;
        }

        consumers.add(pConsumer);

        try {
            initConsumer(pConsumer);
            sendPixels(pConsumer);

            if (isConsumer(pConsumer)) {
                pConsumer.imageComplete(ImageConsumer.STATICIMAGEDONE);

                // Get rid of "sticky" consumers...
                if (isConsumer(pConsumer)) {
                    pConsumer.imageComplete(ImageConsumer.IMAGEERROR);
                    removeConsumer(pConsumer);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();

            if (isConsumer(pConsumer)) {
                pConsumer.imageComplete(ImageConsumer.IMAGEERROR);
            }
        }
    }

    public void removeConsumer(final ImageConsumer pConsumer) {
        consumers.remove(pConsumer);
    }

    /**
     * This implementation silently ignores this instruction. If pixel data is
     * not in TDLR order by default, subclasses must override this method.
     *
     * @param pConsumer the consumer that requested the resend
     *
     * @see ImageProducer#requestTopDownLeftRightResend(java.awt.image.ImageConsumer)
     */
    public void requestTopDownLeftRightResend(final ImageConsumer pConsumer) {
        // ignore
    }

    public void startProduction(final ImageConsumer pConsumer) {
        addConsumer(pConsumer);
    }

    public boolean isConsumer(final ImageConsumer pConsumer) {
        return consumers.contains(pConsumer);
    }

    protected abstract void initConsumer(ImageConsumer pConsumer);

    protected abstract void sendPixels(ImageConsumer pConsumer);
}
