/*
 * Copyright (c) 2014, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.servlet.image;

import org.junit.Test;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IIOProviderContextListenerTest
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IIOProviderContextListenerTest.java,v 1.0 02.01.14 12:33 haraldk Exp$
 */
public class IIOProviderContextListenerTest {
    @Test
    public void testContextInitialized() {
        ServletContextListener listener = new IIOProviderContextListener();
        listener.contextInitialized(mock(ServletContextEvent.class));
    }

    @Test
    public void testContextDestroyed() {
        ServletContext context = mock(ServletContext.class);
        ServletContextEvent destroyed = mock(ServletContextEvent.class);
        when(destroyed.getServletContext()).thenReturn(context);

        ServletContextListener listener = new IIOProviderContextListener();
        listener.contextInitialized(mock(ServletContextEvent.class));
        listener.contextDestroyed(destroyed);
    }

    // Regression test for issue #29
    @Test
    public void testDestroyConcurrentModRegression() {
        ServletContext context = mock(ServletContext.class);
        ServletContextEvent destroyed = mock(ServletContextEvent.class);
        when(destroyed.getServletContext()).thenReturn(context);

        ServletContextListener listener = new IIOProviderContextListener();
        listener.contextInitialized(mock(ServletContextEvent.class));

        ImageReaderSpi provider1 = new MockImageReaderSpiOne();
        ImageReaderSpi provider2 = new MockImageReaderSpiToo();

        // NOTE: Fake registering for simplicity, but it still exposes the original problem with de-registering
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(provider1);
        registry.registerServiceProvider(provider2);
        assertTrue(registry.contains(provider1));
        assertTrue(registry.contains(provider2));

        listener.contextDestroyed(destroyed);

        assertFalse(registry.contains(provider1));
        assertFalse(registry.contains(provider2));
    }

    private static abstract class MockImageReaderSpiBase extends ImageReaderSpi {
        @Override
        public boolean canDecodeInput(Object source) throws IOException {
            return false;
        }

        @Override
        public ImageReader createReaderInstance(Object extension) throws IOException {
            return null;
        }

        @Override
        public String getDescription(Locale locale) {
            return "I'm a mock. So don't mock me.";
        }
    }

    private static final class MockImageReaderSpiOne extends MockImageReaderSpiBase {
    }

    private static final class MockImageReaderSpiToo extends MockImageReaderSpiBase {
    }
}
