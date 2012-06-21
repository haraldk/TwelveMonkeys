/*
 * Copyright (c) 2008, Harald Kuhr
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

package com.twelvemonkeys.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.util.HashMap;

// TODO: Could this be used for change-aware classloader? Woo..
/**
 * Monitors changes is files and system resources.
 *
 * Based on example code and ideas from
 * <A href="http://www.javaworld.com/javaworld/javatips/jw-javatip125.html">Java
 * Tip 125: Set your timer for dynamic properties</A>.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haku $
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/util/ResourceMonitor.java#1 $
 */
public abstract class ResourceMonitor {

    private static final ResourceMonitor INSTANCE = new ResourceMonitor() {};

    private Timer timer;

    private final Map<Object, ResourceMonitorTask> timerEntries;

    public static ResourceMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a {@code ResourceMonitor}.
     */
    protected ResourceMonitor() {
        // Create timer, run timer thread as daemon...
        timer = new Timer(true);
        timerEntries = new HashMap<Object, ResourceMonitorTask>();
    }

    /**
     * Add a monitored {@code Resource} with a {@code ResourceChangeListener}.
     *
     * The {@code reourceId} might be a {@code File} a {@code URL} or a
     * {@code String} containing a file path, or a path to a resource in the
     * class path. Note that class path resources are resolved using the
     * given {@code ResourceChangeListener}'s {@code ClassLoader}, then
     * the current {@code Thread}'s context class loader, if not found.
     *
     * @param pListener   pListener to notify when the file changed.
     * @param pResourceId id of the resource to monitor (a {@code File}
     * a {@code URL} or a {@code String} containing a file path).
     * @param pPeriod     polling pPeriod in milliseconds.
     *
     * @see ClassLoader#getResource(String)
     */
    public void addResourceChangeListener(ResourceChangeListener pListener,
                                          Object pResourceId, long pPeriod) throws IOException {
        // Create the task
        ResourceMonitorTask task = new ResourceMonitorTask(pListener, pResourceId);

        // Get unique Id
        Object resourceId = getResourceId(pResourceId, pListener);

        // Remove the old task for this Id, if any, and register the new one
        synchronized (timerEntries) {
            removeListenerInternal(resourceId);
            timerEntries.put(resourceId, task);
        }

        timer.schedule(task, pPeriod, pPeriod);
    }

    /**
     * Remove the {@code ResourceChangeListener} from the notification list.
     *
     * @param pListener   the pListener to be removed.
     * @param pResourceId name of the resource to monitor.
     */
    public void removeResourceChangeListener(ResourceChangeListener pListener, Object pResourceId) {
        synchronized (timerEntries) {
            removeListenerInternal(getResourceId(pResourceId, pListener));
        }
    }

    private void removeListenerInternal(Object pResourceId) {
        ResourceMonitorTask task = timerEntries.remove(pResourceId);

        if (task != null) {
            task.cancel();
        }
    }

    private Object getResourceId(Object pResourceName, ResourceChangeListener pListener) {
        return pResourceName.toString() + System.identityHashCode(pListener);
    }

    private void fireResourceChangeEvent(ResourceChangeListener pListener, Resource pResource) {
        pListener.resourceChanged(pResource);
    }

    /**
     *
     */
    private class ResourceMonitorTask extends TimerTask {
        ResourceChangeListener listener;
        Resource monitoredResource;
        long lastModified;

        public ResourceMonitorTask(ResourceChangeListener pListener, Object pResourceId) throws IOException {
            listener = pListener;
            lastModified = 0;

            String resourceId = null;
            File file = null;
            URL url = null;
            if (pResourceId instanceof File) {
                file = (File) pResourceId;
                resourceId = file.getAbsolutePath(); // For use by exception only
            }
            else if (pResourceId instanceof URL) {
                url = (URL) pResourceId;
                if ("file".equals(url.getProtocol())) {
                    file = new File(url.getFile());
                }
                resourceId = url.toExternalForm();  // For use by exception only
            }
            else if (pResourceId instanceof String) {
                resourceId = (String) pResourceId; // This one might be looked up
                file = new File(resourceId);
            }

            if (file != null && file.exists()) {
                // Easy, this is a file
                monitoredResource = new FileResource(pResourceId, file);
                //System.out.println("File: " + monitoredResource);
            }
            else {
                // No file there, but is it on CLASSPATH?
                if (url == null) {
                    url = pListener.getClass().getClassLoader().getResource(resourceId);
                }
                if (url == null) {
                    url = Thread.currentThread().getContextClassLoader().getResource(resourceId);
                }

                if (url != null && "file".equals(url.getProtocol())
                        && (file = new File(url.getFile())).exists()) {
                    // It's a file in classpath, so try this as an optimization
                    monitoredResource = new FileResource(pResourceId, file);
                    //System.out.println("File: " + monitoredResource);
                }
                else if (url != null) {
                    // No, not a file, might even be an external resource
                    monitoredResource = new URLResource(pResourceId, url);
                    //System.out.println("URL: " + monitoredResource);
                }
                else {
                    throw new FileNotFoundException(resourceId);
                }
            }

            lastModified = monitoredResource.lastModified();
        }

        public void run() {
            long lastModified = monitoredResource.lastModified();

            if (lastModified != this.lastModified) {
                this.lastModified = lastModified;
                fireResourceChangeEvent(listener, monitoredResource);
            }
        }
    }
}
