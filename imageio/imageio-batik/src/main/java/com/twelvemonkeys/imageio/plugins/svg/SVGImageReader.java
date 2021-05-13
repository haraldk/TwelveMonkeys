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
import com.twelvemonkeys.lang.StringUtil;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.bridge.*;
import org.apache.batik.css.parser.CSSLexicalUnit;
import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.gvt.CanvasGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.renderer.ConcreteImageRendererFactory;
import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.gvt.renderer.ImageRendererFactory;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.xml.LexicalUnits;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGSVGElement;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.spi.ImageReaderSpi;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
 * @see <A href="http://www.mail-archive.com/batik-dev@xml.apache.org/msg00992.html">batik-dev</A>
 */
public class SVGImageReader extends ImageReaderBase {

    final static boolean DEFAULT_ALLOW_EXTERNAL_RESOURCES =
            "true".equalsIgnoreCase(System.getProperty("com.twelvemonkeys.imageio.plugins.svg.allowexternalresources"));

    private Rasterizer rasterizer;
    private boolean allowExternalResources = DEFAULT_ALLOW_EXTERNAL_RESOURCES;

    /**
     * Creates an {@code SVGImageReader}.
     *
     * @param pProvider the provider
     */
    public SVGImageReader(final ImageReaderSpi pProvider) {
        super(pProvider);
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
    public void setInput(Object pInput, boolean seekForwardOnly, boolean ignoreMetadata) {
        super.setInput(pInput, seekForwardOnly, ignoreMetadata);

        if (imageInput != null) {
            TranscoderInput input = new TranscoderInput(IIOUtil.createStreamAdapter(imageInput));
            rasterizer.setInput(input);
        }
    }

    public BufferedImage read(int pIndex, ImageReadParam pParam) throws IOException {
        checkBounds(pIndex);

        if (pParam instanceof SVGReadParam) {
            SVGReadParam svgParam = (SVGReadParam) pParam;

            // set the external-resource-resolution preference
            allowExternalResources = svgParam.isAllowExternalResources();

            // Get the base URI
            // This must be done before converting the params to hints
            String baseURI = svgParam.getBaseURI();
            rasterizer.transcoderInput.setURI(baseURI);

            // Set ImageReadParams as hints
            // Note: The cast to Map invokes a different method that preserves
            // unset defaults, DO NOT REMOVE!
            rasterizer.setTranscodingHints((Map) paramsToHints(svgParam));
        }

        Dimension size = null;
        if (pParam != null) {
            size = pParam.getSourceRenderSize();
        }
        if (size == null) {
            size = new Dimension(getWidth(pIndex), getHeight(pIndex));
        }

        BufferedImage destination = getDestination(pParam, getImageTypes(pIndex), size.width, size.height);

        // Read in the image, using the Batik Transcoder
        try {
            processImageStarted(pIndex);

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
        catch (TranscoderException e) {
            Throwable cause = unwrapException(e);
            throw new IIOException(cause.getMessage(), cause);
        }
    }

    private static Throwable unwrapException(TranscoderException ex) {
        // The TranscoderException is generally useless...
        return ex.getException() != null ? ex.getException() : ex;
    }

    private TranscodingHints paramsToHints(SVGReadParam pParam) throws IOException {
        TranscodingHints hints = new TranscodingHints();
        // Note: We must allow generic ImageReadParams, so converting to
        //       TanscodingHints should be done outside the SVGReadParam class.

        // Set dimensions
        Dimension size = pParam.getSourceRenderSize();
        Dimension origSize = new Dimension(getWidth(0), getHeight(0));
        if (size == null) {
            // SVG is not a pixel based format, but we'll scale it, according to
            // the subsampling for compatibility
            size = getSourceRenderSizeFromSubsamping(pParam, origSize);
        }

        if (size != null) {
            hints.put(ImageTranscoder.KEY_WIDTH, (float) size.getWidth());
            hints.put(ImageTranscoder.KEY_HEIGHT, (float) size.getHeight());
        }

        // Set area of interest
        Rectangle region = pParam.getSourceRegion();
        if (region != null) {
            hints.put(ImageTranscoder.KEY_AOI, region);

            // Avoid that the batik transcoder scales the AOI up to original image size
            if (size == null) {
                hints.put(ImageTranscoder.KEY_WIDTH, (float) region.getWidth());
                hints.put(ImageTranscoder.KEY_HEIGHT, (float) region.getHeight());
            }
            else {
                // Need to resize here...
                double xScale = size.getWidth() / origSize.getWidth();
                double yScale =  size.getHeight() / origSize.getHeight();

                hints.put(ImageTranscoder.KEY_WIDTH, (float) (region.getWidth() * xScale));
                hints.put(ImageTranscoder.KEY_HEIGHT, (float) (region.getHeight() * yScale));
            }
        }
        else if (size != null) {
            // Allow non-uniform scaling
            hints.put(ImageTranscoder.KEY_AOI, new Rectangle(origSize));
        }

        // Background color
        Paint bg = pParam.getBackgroundColor();
        if (bg != null) {
            hints.put(ImageTranscoder.KEY_BACKGROUND_COLOR, bg);
        }

        return hints;
    }

    private Dimension getSourceRenderSizeFromSubsamping(ImageReadParam pParam, Dimension pOrigSize) {
        if (pParam.getSourceXSubsampling() > 1 || pParam.getSourceYSubsampling() > 1) {
            return new Dimension((int) (pOrigSize.width / (float) pParam.getSourceXSubsampling()),
                                 (int) (pOrigSize.height / (float) pParam.getSourceYSubsampling()));
        }
        return null;
    }

    public SVGReadParam getDefaultReadParam() {
        return new SVGReadParam();
    }

    public int getWidth(int pIndex) throws IOException {
        checkBounds(pIndex);
        try {
            return rasterizer.getDefaultWidth();
        }
        catch (TranscoderException e) {
            throw new IIOException(e.getMessage(), e);
        }
    }

    public int getHeight(int pIndex) throws IOException {
        checkBounds(pIndex);
        try {
            return rasterizer.getDefaultHeight();
        }
        catch (TranscoderException e) {
            throw new IIOException(e.getMessage(), e);
        }
    }

    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
        return Collections.singleton(ImageTypeSpecifier.createFromRenderedImage(rasterizer.createImage(1, 1))).iterator();
    }

    /**
     * An image transcoder that stores the resulting image.
     * <p>
     * NOTE: This class includes a lot of copy and paste code from the Batik classes
     * and needs major refactoring!
     * </p>
     */
    private class Rasterizer extends SVGAbstractTranscoder /*ImageTranscoder*/ {

        private BufferedImage image;
        private TranscoderInput transcoderInput;
        private float defaultWidth;
        private float defaultHeight;
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
        protected void transcode(Document document, final String uri, final TranscoderOutput output) throws TranscoderException {
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

            // get the 'width' and 'height' attributes of the SVG document
            UnitProcessor.Context uctx
                    = UnitProcessor.createContext(ctx, rootElement);
            String widthStr = rootElement.getAttributeNS(null, SVGConstants.SVG_WIDTH_ATTRIBUTE);
            String heightStr = rootElement.getAttributeNS(null, SVGConstants.SVG_HEIGHT_ATTRIBUTE);
            if (!StringUtil.isEmpty(widthStr)) {
                defaultWidth = UnitProcessor.svgToUserSpace(widthStr, SVGConstants.SVG_WIDTH_ATTRIBUTE, UnitProcessor.HORIZONTAL_LENGTH, uctx);
            }
            if(!StringUtil.isEmpty(heightStr)){
                defaultHeight = UnitProcessor.svgToUserSpace(heightStr, SVGConstants.SVG_HEIGHT_ATTRIBUTE, UnitProcessor.VERTICAL_LENGTH, uctx);
            }

            boolean hasWidth = defaultWidth > 0.0;
            boolean hasHeight = defaultHeight > 0.0;

            if (!hasWidth || !hasHeight) {
                String viewBoxStr = rootElement.getAttributeNS
                        (null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE);
                if (viewBoxStr.length() != 0) {
                    float[] rect = ViewBox.parseViewBoxAttribute(rootElement, viewBoxStr, null);
                    // if one dimension is given, calculate other by aspect ratio in viewBox
                    // or use viewBox if no dimension is given
                    if (hasWidth) {
                        defaultHeight = defaultWidth * rect[3] / rect[2];
                    }
                    else if (hasHeight) {
                        defaultWidth = defaultHeight * rect[2] / rect[3];
                    }
                    else {
                        defaultWidth = rect[2];
                        defaultHeight = rect[3];
                    }
                }
                else {
                    if (hasHeight) {
                        defaultWidth = defaultHeight;
                    }
                    else if (hasWidth) {
                        defaultHeight = defaultWidth;
                    }
                    else {
                        // fallback to batik default sizes
                        defaultWidth = 400;
                        defaultHeight = 400;
                    }
                }
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

        private BufferedImage readImage() throws TranscoderException {
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
                    throw exception;
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
            setImageSize(defaultWidth, defaultHeight);

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
                throw new TranscoderException(ex);
            }

            if (Px.isIdentity() && (width != defaultWidth || height != defaultHeight)) {
                // The document has no viewBox, we need to resize it by hand.
                // we want to keep the document size ratio
                float xscale, yscale;
                xscale = width / defaultWidth;
                yscale = height / defaultHeight;
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
                throw new TranscoderException(ex);
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
                TranscoderException exception = new TranscoderException(ex.getMessage());
                exception.initCause(ex);
                throw exception;
            }
            finally {
                if (context != null) {
                    context.dispose();
                }
            }
        }

        private synchronized void init() throws TranscoderException {
            if (!initialized) {
                if (transcoderInput == null) {
                    throw new IllegalStateException("input == null");
                }

                initialized = true;

                super.transcode(transcoderInput, null);
            }
        }

        private BufferedImage getImage() throws TranscoderException {
            if (image == null) {
                image = readImage();
            }

            return image;
        }

        int getDefaultWidth() throws TranscoderException {
            init();
            return (int) Math.ceil(defaultWidth);
        }

        int getDefaultHeight() throws TranscoderException {
            init();
            return (int) Math.ceil(defaultHeight);
        }

        void setInput(final TranscoderInput pInput) {
            transcoderInput = pInput;
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
