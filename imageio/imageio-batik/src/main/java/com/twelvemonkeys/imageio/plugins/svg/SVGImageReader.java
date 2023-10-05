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

package com.twelvemonkeys.imageio.plugins.svg;

import com.twelvemonkeys.image.ImageUtil;
import com.twelvemonkeys.imageio.ImageReaderBase;
import com.twelvemonkeys.imageio.util.IIOUtil;
import com.twelvemonkeys.imageio.util.ImageTypeSpecifiers;
import com.twelvemonkeys.lang.StringUtil;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.bridge.*;
import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.gvt.CanvasGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.renderer.ConcreteImageRendererFactory;
import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.gvt.renderer.ImageRendererFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGSVGElement;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Image reader for SVG document fragments.
 *
 * @author Harald Kuhr
 * @author Inpspired by code from the Batik Team
 * @version $Id: $
 * @see <a href="http://www.mail-archive.com/batik-dev@xml.apache.org/msg00992.html">batik-dev</a>
 */
public class SVGImageReader extends ImageReaderBase {

    final static boolean DEFAULT_ALLOW_EXTERNAL_RESOURCES =
            "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.svg.allowExternalResources",
                    System.getProperty("com.twelvemonkeys.imageio.plugins.svg.allowexternalresources")));

    private Rasterizer rasterizer;
    private boolean allowExternalResources = DEFAULT_ALLOW_EXTERNAL_RESOURCES;

    /**
     * Creates an {@code SVGImageReader}.
     *
     * @param provider the provider
     */
    public SVGImageReader(final ImageReaderSpi provider) {
        super(provider);
    }

    protected void resetMembers() {
        rasterizer = new Rasterizer();
    }

    @Override
    public void dispose() {
        super.dispose();
        rasterizer = null;
    }

    @Override
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);

        if (imageInput != null) {
            TranscoderInput transcoderInput = new TranscoderInput(IIOUtil.createStreamAdapter(imageInput));
            rasterizer.setInput(transcoderInput);
        }
    }

    public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
        checkBounds(imageIndex);

        if (param instanceof SVGReadParam) {
            SVGReadParam svgParam = (SVGReadParam) param;

            // set the external-resource-resolution preference
            allowExternalResources = svgParam.isAllowExternalResources();

            // Get the base URI
            // This must be done before converting the params to hints
            String baseURI = svgParam.getBaseURI();
            rasterizer.transcoderInput.setURI(baseURI);

            // Set ImageReadParams as hints
            // Note: The cast to Map invokes a different method that preserves
            // unset defaults, DO NOT REMOVE!
            //noinspection rawtypes
            rasterizer.setTranscodingHints((Map) paramsToHints(svgParam));
        }

        Dimension size = null;
        if (param != null) {
            size = param.getSourceRenderSize();
        }
        if (size == null) {
            size = new Dimension(getWidth(imageIndex), getHeight(imageIndex));
        }

        BufferedImage destination = getDestination(param, getImageTypes(imageIndex), size.width, size.height);

        // Read in the image, using the Batik Transcoder
        processImageStarted(imageIndex);

        BufferedImage image = rasterizer.getImage();

        Graphics2D g = destination.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g.drawImage(image, 0, 0, null); // TODO: Dest offset?
        }
        finally {
            g.dispose();
        }

        processImageComplete();

        return destination;
    }

    private static Throwable unwrapException(TranscoderException ex) {
        // The TranscoderException is generally useless...
        return ex.getException() != null ? ex.getException() : ex;
    }

    private TranscodingHints paramsToHints(SVGReadParam param) throws IOException {
        TranscodingHints hints = new TranscodingHints();
        // Note: We must allow generic ImageReadParams, so converting to
        //       TanscodingHints should be done outside the SVGReadParam class.

        // Set dimensions
        Dimension size = param.getSourceRenderSize();
        Rectangle viewBox = rasterizer.getViewBox();
        if (size == null) {
            // SVG is not a pixel based format, but we'll scale it, according to
            // the subsampling for compatibility
            size = getSourceRenderSizeFromSubsamping(param, viewBox.getSize());
        }

        if (size != null) {
            hints.put(ImageTranscoder.KEY_WIDTH, (float) size.getWidth());
            hints.put(ImageTranscoder.KEY_HEIGHT, (float) size.getHeight());
        }

        // Set area of interest
        Rectangle region = param.getSourceRegion();
        if (region != null) {
            hints.put(ImageTranscoder.KEY_AOI, region);

            // Avoid that the batik transcoder scales the AOI up to original image size
            if (size == null) {
                hints.put(ImageTranscoder.KEY_WIDTH, (float) region.getWidth());
                hints.put(ImageTranscoder.KEY_HEIGHT, (float) region.getHeight());
            }
            else {
                // Need to resize here...
                double xScale = size.getWidth() / viewBox.getWidth();
                double yScale =  size.getHeight() / viewBox.getHeight();

                hints.put(ImageTranscoder.KEY_WIDTH, (float) (region.getWidth() * xScale));
                hints.put(ImageTranscoder.KEY_HEIGHT, (float) (region.getHeight() * yScale));
            }
        }
        else if (size != null) {
            // Allow non-uniform scaling
            hints.put(ImageTranscoder.KEY_AOI, viewBox);
        }

        // Background color
        Paint bg = param.getBackgroundColor();
        if (bg != null) {
            hints.put(ImageTranscoder.KEY_BACKGROUND_COLOR, bg);
        }

        return hints;
    }

    private Dimension getSourceRenderSizeFromSubsamping(ImageReadParam param, Dimension origSize) {
        if (param.getSourceXSubsampling() > 1 || param.getSourceYSubsampling() > 1) {
            return new Dimension((int) (origSize.width / (float) param.getSourceXSubsampling()),
                    (int) (origSize.height / (float) param.getSourceYSubsampling()));
        }
        return null;
    }

    public SVGReadParam getDefaultReadParam() {
        return new SVGReadParam();
    }

    public int getWidth(int imageIndex) throws IOException {
        checkBounds(imageIndex);

        return rasterizer.getDefaultWidth();
    }

    public int getHeight(int imageIndex) throws IOException {
        checkBounds(imageIndex);
        return rasterizer.getDefaultHeight();
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
        return Collections.singleton(ImageTypeSpecifiers.createFromRenderedImage(rasterizer.createImage(1, 1))).iterator();
    }

    /**
     * An image transcoder that stores the resulting image.
     * <p>
     * NOTE: This class includes a lot of copy and paste code from the Batik classes
     * and needs major refactoring!
     * </p>
     */
    private class Rasterizer extends SVGAbstractTranscoder {
        private BufferedImage image;
        private TranscoderInput transcoderInput;
        private final Rectangle2D viewBox = new Rectangle2D.Float();
        private final Dimension defaultSize = new Dimension();
        private boolean initialized = false;
        private SVGOMDocument document;
        private String uri;
        private GraphicsNode gvtRoot;
        private TranscoderException exception;
        private BridgeContext context;

        private BufferedImage createImage(final int width, final int height) {
            return ImageUtil.createTransparent(width, height); // BufferedImage.TYPE_INT_ARGB
        }

        //  This is cheating... We don't fully transcode after all
        protected void transcode(Document document, final String uri, final TranscoderOutput output) {
            // Sets up root, curTxf & curAoi
            // ----
            if (document != null) {
                if (!(document.getImplementation() instanceof SVGDOMImplementation)) {
                    DOMImplementation impl = (DOMImplementation) hints.get(KEY_DOM_IMPLEMENTATION);
                    document = DOMUtilities.deepCloneDocument(document, impl);
                }

                if (uri != null) {
                    try {
                        URL url = new URL(uri);
                        ((SVGOMDocument) document).setURLObject(url);
                    }
                    catch (MalformedURLException ignore) {
                    }
                }
            }

            ctx = createBridgeContext();
            SVGOMDocument svgDoc = (SVGOMDocument) document;

            // build the GVT tree
            builder = new GVTBuilder();
            // flag that indicates if the document is dynamic
            boolean isDynamic =
                    (hints.containsKey(KEY_EXECUTE_ONLOAD) &&
                            (Boolean) hints.get(KEY_EXECUTE_ONLOAD) &&
                            BaseScriptingEnvironment.isDynamicDocument(ctx, svgDoc));

            if (isDynamic) {
                ctx.setDynamicState(BridgeContext.DYNAMIC);
            }

            // Modified code below:
            GraphicsNode root = null;
            try {
                root = builder.build(ctx, svgDoc);
            }
            catch (BridgeException ex) {
                // Note: This might fail, but we STILL have the dimensions we need
                // However, we need to reparse later...
                exception = new TranscoderException(ex);
            }

            // ----
            SVGSVGElement rootElement = svgDoc.getRootElement();

            // Get the viewBox
            String viewBoxStr = rootElement.getAttributeNS(null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE);
            if (viewBoxStr.length() != 0) {
                float[] rect = ViewBox.parseViewBoxAttribute(rootElement, viewBoxStr, null);
                viewBox.setFrame(rect[0], rect[1], rect[2], rect[3]);
            }

            // Get the 'width' and 'height' attributes of the SVG document
            double width = 0;
            double height = 0;
            UnitProcessor.Context uctx = UnitProcessor.createContext(ctx, rootElement);
            String widthStr = rootElement.getAttributeNS(null, SVGConstants.SVG_WIDTH_ATTRIBUTE);
            String heightStr = rootElement.getAttributeNS(null, SVGConstants.SVG_HEIGHT_ATTRIBUTE);
            if (!StringUtil.isEmpty(widthStr)) {
                width = UnitProcessor.svgToUserSpace(widthStr, SVGConstants.SVG_WIDTH_ATTRIBUTE, UnitProcessor.HORIZONTAL_LENGTH, uctx);
            }
            if (!StringUtil.isEmpty(heightStr)) {
                height = UnitProcessor.svgToUserSpace(heightStr, SVGConstants.SVG_HEIGHT_ATTRIBUTE, UnitProcessor.VERTICAL_LENGTH, uctx);
            }

            boolean hasWidth = width > 0.0;
            boolean hasHeight = height > 0.0;

            if (!hasWidth || !hasHeight) {
                if (!viewBox.isEmpty()) {
                    // If one dimension is given, calculate other by aspect ratio in viewBox
                    if (hasWidth) {
                        height = width * viewBox.getHeight() / viewBox.getWidth();
                    }
                    else if (hasHeight) {
                        width = height * viewBox.getWidth() / viewBox.getHeight();
                    }
                    else {
                        // ...or use viewBox if no dimension is given
                        width = viewBox.getWidth();
                        height = viewBox.getHeight();
                    }
                }
                else {
                    // No viewBox, just assume square size
                    if (hasHeight) {
                        width = height;
                    }
                    else if (hasWidth) {
                        height = width;
                    }
                    else {
                        // ...or finally fall back to Batik default sizes
                        width = 400;
                        height = 400;
                    }
                }
            }

            // We now have a size, in the rare case we don't have a viewBox; set it to this size
            defaultSize.setSize(width, height);
            if (viewBox.isEmpty()) {
                viewBox.setRect(0, 0, width, height);
            }

            // Hack to work around exception above
            if (root != null) {
                gvtRoot = root;
            }
            this.document = svgDoc;
            this.uri = uri;

            // Hack to avoid the transcode method wacking my context...
            context = ctx;
            ctx = null;
        }

        private BufferedImage readImage() throws IOException {
            init();

            if (abortRequested()) {
                processReadAborted();
                return null;
            }

            processImageProgress(10f);

            // Hacky workaround below...
            if (gvtRoot == null) {
                // Try to reparse, if we had no URI last time...
                if (uri != transcoderInput.getURI()) {
                    try {
                        context.dispose();
                        document.setURLObject(new URL(transcoderInput.getURI()));
                        transcode(document, transcoderInput.getURI(), null);
                    }
                    catch (MalformedURLException ignore) {
                        // Ignored
                    }
                }

                if (gvtRoot == null) {
                    Throwable cause = unwrapException(exception);
                    throw new IIOException(cause.getMessage(), cause);
                }
            }
            ctx = context;
            // /Hacky

            if (abortRequested()) {
                processReadAborted();
                return null;
            }
            processImageProgress(20f);

            // ----
            SVGSVGElement root = document.getRootElement();
            // ----


            // ----
            setImageSize(defaultSize.width, defaultSize.height);

            if (abortRequested()) {
                processReadAborted();
                return null;
            }
            processImageProgress(40f);

            // compute the preserveAspectRatio matrix
            AffineTransform Px;
            String ref = new ParsedURL(uri).getRef();

            try {
                Px = ViewBox.getViewTransform(ref, root, width, height, null);
            }
            catch (BridgeException ex) {
                throw new IIOException(ex.getMessage(), ex);
            }

            if (Px.isIdentity() && (width != defaultSize.width || height != defaultSize.height)) {
                // The document has no viewBox, we need to resize it by hand.
                // we want to keep the document size ratio
                float xscale, yscale;
                xscale = width / defaultSize.width;
                yscale = height / defaultSize.height;
                float scale = Math.min(xscale, yscale);
                Px = AffineTransform.getScaleInstance(scale, scale);
            }
            // take the AOI into account if any
            if (hints.containsKey(KEY_AOI)) {
                Rectangle2D aoi = (Rectangle2D) hints.get(KEY_AOI);
                // transform the AOI into the image's coordinate system
                aoi = Px.createTransformedShape(aoi).getBounds2D();
                AffineTransform Mx = new AffineTransform();
                double sx = width / aoi.getWidth();
                double sy = height / aoi.getHeight();
                Mx.scale(sx, sy);
                double tx = -aoi.getX();
                double ty = -aoi.getY();
                Mx.translate(tx, ty);
                // take the AOI transformation matrix into account
                // we apply first the preserveAspectRatio matrix
                Px.preConcatenate(Mx);
                curAOI = aoi;
            }
            else {
                curAOI = new Rectangle2D.Float(0, 0, width, height);
            }

            if (abortRequested()) {
                processReadAborted();
                return null;
            }
            processImageProgress(50f);

            CanvasGraphicsNode cgn = getCanvasGraphicsNode(gvtRoot);
            if (cgn != null) {
                cgn.setViewingTransform(Px);
                curTxf = new AffineTransform();
            }
            else {
                curTxf = Px;
            }

            try {
                // dispatch an 'onload' event if needed
                if (ctx.isDynamic()) {
                    BaseScriptingEnvironment se;
                    se = new BaseScriptingEnvironment(ctx);
                    se.loadScripts();
                    se.dispatchSVGLoadEvent();
                }
            }
            catch (BridgeException ex) {
                throw new IIOException(ex.getMessage(), ex);
            }

            this.root = gvtRoot;
            // ----

            // NOTE: The code below is copied and pasted from the Batik
            // ImageTranscoder class' transcode() method:

            // prepare the image to be painted
            int w = (int) (width + 0.5);
            int h = (int) (height + 0.5);

            // paint the SVG document using the bridge package
            // create the appropriate renderer
            ImageRendererFactory rendFactory = new ConcreteImageRendererFactory();
            // ImageRenderer renderer = rendFactory.createDynamicImageRenderer();
            ImageRenderer renderer = rendFactory.createStaticImageRenderer();
            renderer.updateOffScreen(w, h);
            renderer.setTransform(curTxf);
            renderer.setTree(this.root);
            this.root = null; // We're done with it...

            if (abortRequested()) {
                processReadAborted();
                return null;
            }
            processImageProgress(75f);

            try {
                // now we are sure that the aoi is the image size
                Shape raoi = new Rectangle2D.Float(0, 0, width, height);
                // Warning: the renderer's AOI must be in user space
                renderer.repaint(curTxf.createInverse().createTransformedShape(raoi));
                // NOTE: repaint above cause nullpointer exception with fonts..???

                BufferedImage rend = renderer.getOffScreen();
                renderer = null; // We're done with it...

                BufferedImage dest = createImage(w, h);

                Graphics2D g2d = GraphicsUtil.createGraphics(dest);
                try {
                    if (hints.containsKey(ImageTranscoder.KEY_BACKGROUND_COLOR)) {
                        Paint bgcolor = (Paint) hints.get(ImageTranscoder.KEY_BACKGROUND_COLOR);
                        g2d.setComposite(AlphaComposite.SrcOver);
                        g2d.setPaint(bgcolor);
                        g2d.fillRect(0, 0, w, h);
                    }

                    if (rend != null) { // might be null if the svg document is empty
                        g2d.drawRenderedImage(rend, new AffineTransform());
                    }
                }
                finally {
                    if (g2d != null) {
                        g2d.dispose();
                    }
                }

                if (abortRequested()) {
                    processReadAborted();
                    return null;
                }
                processImageProgress(99f);

                return dest;
            }
            catch (Exception ex) {
                throw new IIOException(ex.getMessage(), ex);
            }
            finally {
                if (context != null) {
                    context.dispose();
                }
            }
        }

        private synchronized void init() throws IIOException {
            if (!initialized) {
                if (transcoderInput == null) {
                    throw new IllegalStateException("input == null");
                }

                initialized = true;

                try {
                    super.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, allowExternalResources);
                    super.transcode(transcoderInput, null);
                }
                catch (TranscoderException e) {
                    Throwable cause = unwrapException(e);
                    throw new IIOException(cause.getMessage(), cause);
                }
            }
        }

        private BufferedImage getImage() throws IOException {
            if (image == null) {
                image = readImage();
            }

            return image;
        }

        int getDefaultWidth() throws IOException {
            init();
            return defaultSize.width;
        }

        int getDefaultHeight() throws IOException {
            init();
            return defaultSize.height;
        }

        Rectangle getViewBox() throws IOException {
            init();
            return viewBox.getBounds();
        }

        void setInput(final TranscoderInput input) {
            transcoderInput = input;
        }

        @Override
        protected UserAgent createUserAgent() {
            return new SVGImageReaderUserAgent();
        }

        private class SVGImageReaderUserAgent extends SVGAbstractTranscoderUserAgent {
            @Override
            public void displayError(Exception e) {
                displayError(e.getMessage());
            }

            @Override
            public void displayError(String message) {
                displayMessage(message);
            }

            @Override
            public void displayMessage(String message) {
                processWarningOccurred(message.replaceAll("[\\r\\n]+", " "));
            }

            @Override
            public ExternalResourceSecurity getExternalResourceSecurity(ParsedURL resourceURL, ParsedURL docURL) {
                if (allowExternalResources) {
                    return super.getExternalResourceSecurity(resourceURL, docURL);
                }
                return new EmbededExternalResourceSecurity(resourceURL);
            }
        }
    }
}
