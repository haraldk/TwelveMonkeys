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

package com.twelvemonkeys.imageio.plugins.pict;

import com.twelvemonkeys.lang.Validate;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import static java.lang.Math.sqrt;

/**
 * Emulates an Apple QuickDraw rendering context, backed by a Java {@link Graphics2D}.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: QuickDrawContext.java,v 1.0 Oct 3, 2007 1:24:35 AM haraldk Exp$
 */
// TODO: It would actually be possible to implement a version of this interface
// that wrote opcodes/data to a stream... Or maybe a QDGraphics would be better.
// TODO: Optimize for pensize 1,1?
// TODO: Do we really need the Xxx2D stuff?
// TODO: Support COPY_DITHER
class QuickDrawContext {

    /*
    // The useful parts of the QD Graphics Port:
   portRect:   Rect;       {port rectangle}
   visRgn:     RgnHandle;  {visible region}
   clipRgn:    RgnHandle;  {clipping region}
   bkPat:      Pattern;    {background pattern}
   fillPat:    Pattern;    {fill pattern}
   pnLoc:      Point;      {pen location}
   pnSize:     Point;      {pen size}
   pnMode:     Integer;    {pattern mode}
   pnPat:      Pattern;    {pen pattern}
   pnVis:      Integer;    {pen visibility}
   txFont:     Integer;    {font number for text}
   txFace:     Style;      {text's font style}
   txMode:     Integer;    {source mode for text}
   txSize:     Integer;    {font size for text}
   spExtra:    Fixed;      {extra space}
   fgColor:    LongInt;    {foreground color}
   bkColor:    LongInt;    {background color}
   colrBit:    Integer;    {color bit}
   ..
   picSave:       Handle;        {picture being saved, used internally}
   rgnSave:       Handle;        {region being saved, used internally}
   polySave:      Handle;        {polygon being saved, used internally}
     */

    /*
    // Color Graphics Port;
   chExtra:       Integer;       {added width for nonspace characters}
   pnLocHFrac:    Integer;       {pen fraction}
   portRect:      Rect;          {port rectangle}
   visRgn:        RgnHandle;     {visible region}
   clipRgn:       RgnHandle;     {clipping region}
   bkPixPat:      PixPatHandle;  {background pattern}
   rgbFgColor:    RGBColor;      {requested foreground color}
   rgbBkColor:    RGBColor;      {requested background color}
   pnLoc:         Point;         {pen location}
   pnSize:        Point;         {pen size}
   pnMode:        Integer;       {pattern mode}
   pnPixPat:      PixPatHandle;  {pen pattern}
   fillPixPat:    PixPatHandle;  {fill pattern}
   pnVis:         Integer;       {pen visibility}
   txFont:        Integer;       {font number for text}
   txFace:        Style;         {text's font style}
   txMode:        Integer;       {source mode for text}
   txSize:        Integer;       {font size for text}
   spExtra:       Fixed;         {added width for space characters}
   fgColor:       LongInt;       {actual foreground color}
   bkColor:       LongInt;       {actual background color}
   colrBit:       Integer;       {plane being drawn}
   ..
   picSave:       Handle;        {picture being saved, used internally}
   rgnSave:       Handle;        {region being saved, used internally}
   polySave:      Handle;        {polygon being saved, used internally}
     */
    private final Graphics2D graphics;

    private Pattern background;

    // http://developer.apple.com/documentation/mac/quickdraw/QuickDraw-68.html#HEADING68-0
    // Upon the creation of a graphics port, QuickDraw assigns these initial
    // values to the graphics pen: a size of (1,1), a pattern of all-black pixels,
    // and the patCopy pattern mode. After changing any of these values,
    // you can use the PenNormal procedure to return these initial values to the
    // graphics pen.

    // TODO: Consider creating a Pen/PenState class?
    private int penVisibility = 0;
    private Point2D penPosition = new Point();
    private Pattern penPattern;
    private Dimension2D penSize = new Dimension();
    private int penMode;

    // TODO: Make sure setting bgColor/fgColor does not reset pattern, and pattern not resetting bg/fg!
    private Color bgColor = Color.WHITE;
    private Color fgColor = Color.BLACK;

    private int textMode;
    private Pattern textPattern = new BitMapPattern(Color.BLACK);
    private Pattern fillPattern;

    QuickDrawContext(final Graphics2D pGraphics) {
        graphics = Validate.notNull(pGraphics, "graphics");

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        setPenNormal();
    }

    protected void dispose() {
        graphics.dispose();
    }

    // ClosePicture
    public void closePicture() {
        dispose();
    }

    // ClipRgn
    public void setClipRegion(Shape pClip) {
        graphics.setClip(pClip);
    }

    // Font number (sic), integer
    void setTextFont(int fontFamily) {
        // ..?
        System.err.println("QuickDrawContext.setTextFont: " + fontFamily);
    }

    public void setTextFont(final String fontName) {
        // TODO: Need mapping between known QD font names and Java font names?
        Font current = graphics.getFont();
        graphics.setFont(Font.decode(fontName).deriveFont(current.getStyle(), (float) current.getSize()));
    }

    // Sets the text's font style (0..255)
    void setTextFace(final int face) {
        int style = 0;
        if ((face & QuickDraw.TX_BOLD_MASK) > 0) {
            style |= Font.BOLD;
        }
        if ((face & QuickDraw.TX_ITALIC_MASK) > 0) {
            style |= Font.ITALIC;
        }

        // TODO: Other face options, like underline, shadow, etc...

        graphics.setFont(graphics.getFont().deriveFont(style));
    }

    void setTextMode(int pSourceMode) {
        // ..?
        System.err.println("QuickDrawContext.setTextMode");
        textMode = pSourceMode;
    }

    public void setTextSize(int pSize) {
        graphics.setFont(graphics.getFont().deriveFont((float) pSize));
    }

    // Numerator (Point), denominator (Point)
    void setTextRatio() {
        // TODO
        System.err.println("QuickDrawContext.setTextRatio");
    }

    // TODO: spExtra added width for space characters
    // TODO: chExtra added width for nonspace characters

    public void setOrigin(Point2D pOrigin) {
        graphics.translate(pOrigin.getX(), pOrigin.getY());
    }

    public void setForeground(final Color pColor) {
        fgColor = pColor;
        penPattern = new BitMapPattern(pColor);
    }

    Color getForeground() {
        return fgColor;
    }

    public void setBackground(final Color pColor) {
        bgColor = pColor;
        background = new BitMapPattern(pColor);
    }

    Color getBackground() {
        return bgColor;
    }

    /*
    // Pen management:
    // NOTE: The HidePen procedure is called by the OpenRgn, OpenPicture, and OpenPoly routines so that you can create regions, pictures, and polygons without drawing on the screen.
    //       ShowPen is called by the procedures CloseRgn, ClosePoly, and ClosePicture
    GetPenState // All pen state incl. position (PenState type?)
    SetPenState
    */

    /**
     * HidePen  Visibility (decrements visibility by one!)
     */
    public void hidePen() {
        penVisibility--;
    }

    /**
     * ShowPen Visibility (increments visibility by one!)
     */
    public void showPen() {
        penVisibility++;
    }

    /**
     * Tells whether pen is visible.
     *
     * @return {@code true} if pen is visible
     */
    private boolean isPenVisible() {
        return penVisibility >= 0;
    }

    /**
     * Returns the pen position.
     * GetPen
     *
     * @return the current pen position
     */
    public Point2D getPenPosition() {
        return (Point2D) penPosition.clone();
    }

    /**
     * Sets the pen size.
     * PenSize
     *
     * @param pSize the new size
     */
    public void setPenSize(Dimension2D pSize) {
        penSize.setSize(pSize);
        graphics.setStroke(getStroke(penSize));
    }

    /**
     * PenMode // Sets pen pattern mode
     *
     * @param pPenMode the new pen mode
     */
    public void setPenMode(int pPenMode) {
        // TODO: Handle HILITE (+50)
        // TODO: Handle DITHER_COPY (+64)
        switch (pPenMode) {
            // Boolean source transfer modes
            case QuickDraw.SRC_COPY:
            case QuickDraw.SRC_OR:
            case QuickDraw.SRC_XOR:
            case QuickDraw.SRC_BIC:
            case QuickDraw.NOT_SRC_COPY:
            case QuickDraw.NOT_SRC_OR:
            case QuickDraw.NOT_SRC_XOR:
            case QuickDraw.NOT_SRC_BIC:
                // Boolean pattern transfer modes
            case QuickDraw.PAT_COPY:
            case QuickDraw.PAT_OR:
            case QuickDraw.PAT_XOR:
            case QuickDraw.PAT_BIC:
            case QuickDraw.NOT_PAT_COPY:
            case QuickDraw.NOT_PAT_OR:
            case QuickDraw.NOT_PAT_XOR:
            case QuickDraw.NOT_PAT_BIC:
                // Aritmetic transfer modes
            case QuickDraw.BLEND:
            case QuickDraw.ADD_PIN:
            case QuickDraw.ADD_OVER:
            case QuickDraw.SUB_PIN:
            case QuickDraw.TRANSPARENT:
            case QuickDraw.ADD_MAX:
            case QuickDraw.SUB_OVER:
            case QuickDraw.ADD_MIN:
            case QuickDraw.GRAYISH_TEXT_OR:
                penMode = pPenMode;
                break;

            default:
                throw new IllegalArgumentException("Undefined pen mode: " + pPenMode);
        }
    }

    /**
     * PenPat & PenPixPat // Sets pen bit pattern or pix pattern
     *
     * @param pPattern the new pattern
     */
    public void setPenPattern(final Pattern pPattern) {
        penPattern = pPattern;
    }

    /**
     * PenNormal // Reset (except posiotion)
     */
    public final void setPenNormal() {
        // NOTE: Shold not change pen location
        // TODO: What about visibility? Probably not touch
        setPenPattern(QuickDraw.BLACK);
        setPenSize(new Dimension(1, 1));
        penMode = QuickDraw.SRC_COPY;
    }

    /*
    // Background pattern:
    BackPat // Used by the Erase* methods
    *BackPixPat
    */
    public void setBackgroundPattern(final Pattern pPaint) {
        background = pPaint;
    }

    public void setFillPattern(final Pattern fillPattern) {
        this.fillPattern = fillPattern;
    }

    private Composite getCompositeFor(final int pMode) {
        switch (pMode & ~QuickDraw.DITHER_COPY) {
            // Boolean source transfer modes
            case QuickDraw.SRC_COPY:
                return AlphaComposite.Src; // Or, SRC_OVER?
            case QuickDraw.SRC_OR:
                return AlphaComposite.SrcOver; // Or, DST_OUT?
            case QuickDraw.SRC_XOR:
                return AlphaComposite.Xor;
            case QuickDraw.SRC_BIC:
                return AlphaComposite.Clear;
            case QuickDraw.NOT_SRC_XOR:
                return QuickDrawComposite.NotSrcXor;
            case QuickDraw.NOT_SRC_COPY:
            case QuickDraw.NOT_SRC_OR:
            case QuickDraw.NOT_SRC_BIC:
                throw new UnsupportedOperationException("Not implemented for mode " + pMode);
                // Boolean pattern transfer modes
            case QuickDraw.PAT_COPY:
                return AlphaComposite.Src; // Tested
            case QuickDraw.PAT_OR:
                return AlphaComposite.SrcOver;  // Or, DST_OUT?
            case QuickDraw.PAT_XOR:
                return AlphaComposite.Xor;
            case QuickDraw.PAT_BIC:
                return AlphaComposite.Clear;
            case QuickDraw.NOT_PAT_COPY:
            case QuickDraw.NOT_PAT_OR:
            case QuickDraw.NOT_PAT_XOR:
            case QuickDraw.NOT_PAT_BIC:
                throw new UnsupportedOperationException("Not implemented for mode " + pMode);
                // Aritmetic transfer modes
            case QuickDraw.BLEND:
                return AlphaComposite.SrcOver.derive(.5f);
            case QuickDraw.ADD_PIN:
            case QuickDraw.ADD_OVER:
            case QuickDraw.SUB_PIN:
            case QuickDraw.TRANSPARENT:
                throw new UnsupportedOperationException("Not implemented for mode " + pMode);
            case QuickDraw.ADD_MAX:
                return QuickDrawComposite.AddMax;
            case QuickDraw.SUB_OVER:
                throw new UnsupportedOperationException("Not implemented for mode " + pMode);
            case QuickDraw.ADD_MIN:
                return QuickDrawComposite.AddMin;
            case QuickDraw.GRAYISH_TEXT_OR:
                throw new UnsupportedOperationException("Not implemented for mode " + pMode);

            default:
                throw new IllegalArgumentException("Unknown pnMode: " + pMode);
        }
    }

    /**
     * Sets up context for text drawing.
     */
    protected void setupForText() {
        graphics.setPaint(textPattern);
        graphics.setComposite(getCompositeFor(textMode));
    }

    /**
     * Sets up context for line drawing/painting.
     */
    protected void setupForPaint() {
        graphics.setPaint(penPattern);
        graphics.setComposite(getCompositeFor(penMode));
        //graphics.setStroke(getStroke(penSize));
    }

    private Stroke getStroke(final Dimension2D pPenSize) {
        // TODO: OPTIMIZE: Only create stroke if changed!
        if (pPenSize.getWidth() <= 1.0 && pPenSize.getWidth() == pPenSize.getHeight()) {
            return new BasicStroke((float) pPenSize.getWidth());
        }
        return new RectangleStroke(new Rectangle2D.Double(0, 0, pPenSize.getWidth(), pPenSize.getHeight()));
    }

    /**
     * Sets up paint context for fill.
     *
     * @param pPattern the pattern to use for filling.
     */
    protected void setupForFill(final Pattern pPattern) {
        graphics.setPaint(pPattern);
        graphics.setComposite(getCompositeFor(QuickDraw.PAT_COPY));
    }

    protected void setupForErase() {
        graphics.setPaint(background);
        graphics.setComposite(getCompositeFor(QuickDraw.PAT_COPY)); // TODO: Check spec
    }

    protected void setupForInvert() {
        // TODO: Setup for invert
        graphics.setColor(Color.BLACK);
        graphics.setXORMode(Color.WHITE);
    }

    /*

    // Line drawing:
    MoveTo // Moves to new pos
    Move // distance (MoveTo(h+dh,v+dv))
    LineTo // Draws from current pos to new pos, stores new pos
    Line // dinstance (LineTo(h+dh,v+dv))
    */

    public void moveTo(final double pX, final double pY) {
        penPosition.setLocation(pX, pY);
    }

    public final void moveTo(final Point2D pPosition) {
        moveTo(pPosition.getX(), pPosition.getY());
    }

    public final void move(final double pDeltaX, final double pDeltaY) {
        moveTo(penPosition.getX() + pDeltaX, penPosition.getY() + pDeltaY);
    }

    public void lineTo(final double pX, final double pY) {
        Shape line = new Line2D.Double(penPosition.getX(), penPosition.getY(), pX, pY);

        // TODO: Add line to current shape if recording...

        if (isPenVisible()) {
            // NOTE: Workaround for known Mac JDK bug: Paint, not frame
            paintShape(graphics.getStroke().createStrokedShape(line));
        }

        moveTo(pX, pY);
    }

    public final void lineTo(final Point2D pPosition) {
        lineTo(pPosition.getX(), pPosition.getY());
    }

    public final void line(final double pDeltaX, final double pDeltaY) {
        lineTo(penPosition.getX() + pDeltaX, penPosition.getY() + pDeltaY);
    }

    /*
   // Drawing With Color QuickDraw Colors:
   * RGBForeColor
   * RGBBackColor
   * SetCPixel <-- TODO
   * FillCRect
   * FillCRoundRect
   * FillCOval
   * FillCArc
   * FillCPoly
   * FillCRgn
   * OpColor // sets the maximum color values for the addPin and subPin arithmetic transfer modes, and the weight color for the blend arithmetic transfer mode.
   * HiliteColor // (SKIP?) See http://developer.apple.com/documentation/mac/quickdraw/QuickDraw-199.html#MARKER-9-151

   // Creating and Managing Rectangles (SKIP? Rely on awt Rectangle):
   SetRect
   OffsetRect
   InsetRect
   SectRect
   UnionRect
   PtInRect
   Pt2Rect
   PtToAngle
   EqualRect
   EmptyRect // Reset
   */

    // Drawing Rectangles:

    /**
     * FrameRect(r) // outline rect with the size, pattern, and pattern mode of
     * the graphics pen.
     *
     * @param pRectangle the rectangle to frame
     */
    public void frameRect(final Rectangle2D pRectangle) {
        frameShape(pRectangle);
    }

    /**
     * PaintRect(r) // fills a rectangle's interior with the pattern of the
     * graphics pen, using the pattern mode of the graphics pen.
     *
     * @param pRectangle the rectangle to paint
     */
    public void paintRect(final Rectangle2D pRectangle) {
        paintShape(pRectangle);
    }

    /**
     * FillRect(r, pat) // fills a rectangle's interior with any pattern you
     * specify. The procedure transfers the pattern with the patCopy pattern
     * mode, which directly copies your requested pattern into the shape.
     *
     * @param pRectangle the rectangle to fill
     * @param pPattern   the pattern to use
     */
    public void fillRect(final Rectangle2D pRectangle, Pattern pPattern) {
        fillShape(pRectangle, pPattern);
    }

    /**
     * EraseRect(r) // fills the rectangle's interior with the background pattern
     *
     * @param pRectangle the rectangle to erase
     */
    public void eraseRect(final Rectangle2D pRectangle) {
        eraseShape(pRectangle);
    }

    /**
     * InvertRect(r) // reverses the color of all pixels in the rect
     *
     * @param pRectangle the rectangle to invert
     */
    public void invertRect(final Rectangle2D pRectangle) {
        invertShape(pRectangle);
    }

    // http://developer.apple.com/documentation/mac/quickdraw/QuickDraw-102.html#HEADING102-0
    // Drawing Rounded Rectangles:
    private static RoundRectangle2D.Double toRoundRect(final Rectangle2D pRectangle, final int pArcW, final int pArcH) {
        return new RoundRectangle2D.Double(
                pRectangle.getX(), pRectangle.getY(),
                pRectangle.getWidth(), pRectangle.getHeight(),
                pArcW, pArcH);
    }

    /**
     * FrameRoundRect(r,int,int) // outline round rect with the size, pattern, and pattern mode of
     * the graphics pen.
     *
     * @param pRectangle the rectangle to frame
     * @param pArcW      width of the oval defining the rounded corner.
     * @param pArcH      height of the oval defining the rounded corner.
     */
    public void frameRoundRect(final Rectangle2D pRectangle, int pArcW, int pArcH) {
        frameShape(toRoundRect(pRectangle, pArcW, pArcH));
    }

    /**
     * PaintRooundRect(r,int,int) // fills a rectangle's interior with the pattern of the
     * graphics pen, using the pattern mode of the graphics pen.
     *
     * @param pRectangle the rectangle to paint
     * @param pArcW      width of the oval defining the rounded corner.
     * @param pArcH      height of the oval defining the rounded corner.
     */
    public void paintRoundRect(final Rectangle2D pRectangle, int pArcW, int pArcH) {
        paintShape(toRoundRect(pRectangle, pArcW, pArcH));
    }

    /**
     * FillRoundRect(r,int,int,pat) // fills a rectangle's interior with any pattern you
     * specify. The procedure transfers the pattern with the patCopy pattern
     * mode, which directly copies your requested pattern into the shape.
     *
     * @param pRectangle the rectangle to fill
     * @param pArcW      width of the oval defining the rounded corner.
     * @param pArcH      height of the oval defining the rounded corner.
     * @param pPattern   the pattern to use
     */
    public void fillRoundRect(final Rectangle2D pRectangle, int pArcW, int pArcH, Pattern pPattern) {
        fillShape(toRoundRect(pRectangle, pArcW, pArcH), pPattern);
    }

    /**
     * EraseRoundRect(r,int,int) // fills the rectangle's interior with the background pattern
     *
     * @param pRectangle the rectangle to erase
     * @param pArcW      width of the oval defining the rounded corner.
     * @param pArcH      height of the oval defining the rounded corner.
     */
    public void eraseRoundRect(final Rectangle2D pRectangle, int pArcW, int pArcH) {
        eraseShape(toRoundRect(pRectangle, pArcW, pArcH));
    }

    /**
     * InvertRoundRect(r,int,int) // reverses the color of all pixels in the rect
     *
     * @param pRectangle the rectangle to invert
     * @param pArcW      width of the oval defining the rounded corner.
     * @param pArcH      height of the oval defining the rounded corner.
     */
    public void invertRoundRect(final Rectangle2D pRectangle, int pArcW, int pArcH) {
        invertShape(toRoundRect(pRectangle, pArcW, pArcH));
    }

    // Drawing Ovals:
    private static Ellipse2D.Double toOval(final Rectangle2D pRectangle) {
        Ellipse2D.Double ellipse = new Ellipse2D.Double();
        ellipse.setFrame(pRectangle);
        return ellipse;
    }

    /**
     * FrameOval(r) // outline oval with the size, pattern, and pattern mode of
     * the graphics pen.
     *
     * @param pRectangle the rectangle to frame
     */
    public void frameOval(final Rectangle2D pRectangle) {
        frameShape(toOval(pRectangle));
    }

    /**
     * PaintOval(r) // fills an oval's interior with the pattern of the
     * graphics pen, using the pattern mode of the graphics pen.
     *
     * @param pRectangle the rectangle to paint
     */
    public void paintOval(final Rectangle2D pRectangle) {
        paintShape(toOval(pRectangle));
    }

    /**
     * FillOval(r, pat) // fills an oval's interior with any pattern you
     * specify. The procedure transfers the pattern with the patCopy pattern
     * mode, which directly copies your requested pattern into the shape.
     *
     * @param pRectangle the rectangle to fill
     * @param pPattern   the pattern to use
     */
    public void fillOval(final Rectangle2D pRectangle, Pattern pPattern) {
        fillShape(toOval(pRectangle), pPattern);
    }

    /**
     * EraseOval(r) // fills the oval's interior with the background pattern
     *
     * @param pRectangle the rectangle to erase
     */
    public void eraseOval(final Rectangle2D pRectangle) {
        eraseShape(toOval(pRectangle));
    }

    /**
     * InvertOval(r) // reverses the color of all pixels in the oval
     *
     * @param pRectangle the rectangle to invert
     */
    public void invertOval(final Rectangle2D pRectangle) {
        invertShape(toOval(pRectangle));
    }

    // http://developer.apple.com/documentation/mac/quickdraw/QuickDraw-114.html#HEADING114-0
    // NOTE: Differs from Java 2D arcs, in start angle, and rotation
    // Drawing Arcs and Wedges:

    /**
     * Converts a rectangle to an arc.
     *
     * @param pRectangle  the framing rectangle
     * @param pStartAngle start angle in degrees (starting from 12'o clock, this differs from Java)
     * @param pArcAngle   rotation angle in degrees (starting from {@code pStartAngle}, this differs from Java arcs)
     * @param pClosed     specifies if the arc should be closed
     * @return the arc
     */
    private static Arc2D.Double toArc(final Rectangle2D pRectangle, int pStartAngle, int pArcAngle, final boolean pClosed) {
        return new Arc2D.Double(pRectangle, 90 - pStartAngle, -pArcAngle, pClosed ? Arc2D.PIE : Arc2D.OPEN);
    }

    /**
     * FrameArc(r,int,int) // outline arc with the size, pattern, and pattern mode of
     * the graphics pen.
     *
     * @param pRectangle  the rectangle to frame
     * @param pStartAngle start angle in degrees (starting from 12'o clock, this differs from Java)
     * @param pArcAngle   rotation angle in degrees (starting from {@code pStartAngle}, this differs from Java arcs)
     */
    public void frameArc(final Rectangle2D pRectangle, int pStartAngle, int pArcAngle) {
        frameShape(toArc(pRectangle, pStartAngle, pArcAngle, false));
    }

    /**
     * PaintArc(r,int,int) // fills an arc's interior with the pattern of the
     * graphics pen, using the pattern mode of the graphics pen.
     *
     * @param pRectangle  the rectangle to paint
     * @param pStartAngle start angle in degrees (starting from 12'o clock, this differs from Java)
     * @param pArcAngle   rotation angle in degrees (starting from {@code pStartAngle}, this differs from Java arcs)
     */
    public void paintArc(final Rectangle2D pRectangle, int pStartAngle, int pArcAngle) {
        paintShape(toArc(pRectangle, pStartAngle, pArcAngle, true));
    }

    /**
     * FillArc(r,int,int, pat) // fills an arc's interior with any pattern you
     * specify. The procedure transfers the pattern with the patCopy pattern
     * mode, which directly copies your requested pattern into the shape.
     *
     * @param pRectangle  the rectangle to fill
     * @param pStartAngle start angle in degrees (starting from 12'o clock, this differs from Java)
     * @param pArcAngle   rotation angle in degrees (starting from {@code pStartAngle}, this differs from Java arcs)
     * @param pPattern    the pattern to use
     */
    public void fillArc(final Rectangle2D pRectangle, int pStartAngle, int pArcAngle, Pattern pPattern) {
        fillShape(toArc(pRectangle, pStartAngle, pArcAngle, true), pPattern);
    }

    /**
     * EraseArc(r,int,int) // fills the arc's interior with the background pattern
     *
     * @param pRectangle  the rectangle to erase
     * @param pStartAngle start angle in degrees (starting from 12'o clock, this differs from Java)
     * @param pArcAngle   rotation angle in degrees (starting from {@code pStartAngle}, this differs from Java arcs)
     */
    public void eraseArc(final Rectangle2D pRectangle, int pStartAngle, int pArcAngle) {
        eraseShape(toArc(pRectangle, pStartAngle, pArcAngle, true));
    }

    /**
     * InvertArc(r,int,int) // reverses the color of all pixels in the arc
     *
     * @param pRectangle  the rectangle to invert
     * @param pStartAngle start angle in degrees (starting from 12'o clock, this differs from Java)
     * @param pArcAngle   rotation angle in degrees (starting from {@code pStartAngle}, this differs from Java arcs)
     */
    public void invertArc(final Rectangle2D pRectangle, int pStartAngle, int pArcAngle) {
        invertShape(toArc(pRectangle, pStartAngle, pArcAngle, true));
    }

    /*
   // http://developer.apple.com/documentation/mac/quickdraw/QuickDraw-120.html#HEADING120-0
   // Creating and Managing Polygons:
   // - Use Shape?
   OpenPoly // Returns a reference to the polygon, used by the paint or other methods
   ClosePoly // Close (use LineTo with invisible pen to create)
   OffsetPoly
   KillPoly // Set internal reference to null
   */

    // Drawing Polygons:
    // TODO: What is the Xxx2D equivalent of Polygon!? GeneralPath?
    // FramePoly
    public void framePoly(final Polygon pPolygon) {
        // TODO: The old PICTImageReader does not draw the last connection line,
        // unless the start and end point is the same...
        // Find out what the spec says.
        //if (pPolygon.xpoints[0] == pPolygon.xpoints[pPolygon.npoints - 1] &&
        //        pPolygon.ypoints[0] == pPolygon.ypoints[pPolygon.npoints - 1]) {
        //}
        frameShape(pPolygon);
    }

    // From the source:
    // "Four of these procedures--PaintPoly, ErasePoly, InvertPoly, and FillPoly--
    // temporarily convert the polygon into a region to perform their operations"
    // PaintPoly
    public void paintPoly(final Polygon pPolygon) {
        paintShape(pPolygon);
    }

    // FillPoly
    public void fillPoly(final Polygon pPolygon, final Pattern pPattern) {
        fillShape(pPolygon, pPattern);
    }

    // ErasePoly
    public void erasePoly(final Polygon pPolygon) {
        eraseShape(pPolygon);
    }

    // InvertPoly
    public void invertPoly(final Polygon pPolygon) {
        invertShape(pPolygon);
    }

    /*
   // http://developer.apple.com/documentation/mac/quickdraw/QuickDraw-131.html#HEADING131-0
   // Creating and Managing Regions:
   // TODO: Java equiv? Area?
   NewRgn // new Region?
   OpenRgn // Start collecting region information
   CloseRgn
   DisposeRgn
   CopyRgn
   SetEmptyRgn
   SetRectRgn
   RectRgn
   OffsetRgn
   InsetRgn
   SectRgn
   UnionRgn
   DiffRgn
   XorRgn
   PtInRgn
   RectInRgn
   EqualRgn
   EmptyRgn
    */

    // Drawing Regions:
    // FrameRgn
    public void frameRegion(final Area pArea) {
        frameShape(pArea);
    }

    // PaintRgn
    public void paintRegion(final Area pArea) {
        paintShape(pArea);
    }

    // FillRgn
    public void fillRegion(final Area pArea, final Pattern pPattern) {
        fillShape(pArea, pPattern);
    }

    // EraseRgn
    public void eraseRegion(final Area pArea) {
        eraseShape(pArea);
    }

    // InvertRgn
    public void invertRegion(final Area pArea) {
        invertShape(pArea);
    }

    // TODO: All other operations can delegate to these! :-)
    private void frameShape(final Shape pShape) {
        if (isPenVisible()) {
            setupForPaint();

            Stroke stroke = getStroke(penSize);
            Shape shape = stroke.createStrokedShape(pShape);
            graphics.draw(shape);
        }
    }

    private void paintShape(final Shape pShape) {
        setupForPaint();
        graphics.fill(pShape); // Yes, fill
    }

    private void fillShape(final Shape pShape, final Pattern pPattern) {
        setupForFill(pPattern);
        graphics.fill(pShape);
    }

    private void invertShape(final Shape pShape) {
        setupForInvert();
        graphics.fill(pShape);
    }

    private void eraseShape(final Shape pShape) {
        setupForErase();
        graphics.fill(pShape);
    }

    /*
   // Scaling and Mapping Points, Rectangles, Polygons, and Regions:
   ScalePt // Use the getXPtCoord/Y from the reader?
   MapPt
   MapRect
   MapRgn
   MapPoly

   // Calculating Black-and-White Fills (SKIP?):
   SeedFill // MacPaint paint-bucket tool
   CalcMask // Calculates where paint would not flow (see above)

   // Copying Images (SKIP?):
   */

    /**
     * CopyBits.
     * <p>
     * Note that the destination is always {@code this}.
     * </p>
     *
     * @param pSrcBitmap the source bitmap to copy pixels from
     * @param pSrcRect   the source rectangle
     * @param pDstRect   the destination rectangle
     * @param pMode      the blending mode
     * @param pMaskRgn   the mask region
     */
    public void copyBits(BufferedImage pSrcBitmap, Rectangle pSrcRect, Rectangle pDstRect, int pMode, Shape pMaskRgn) {
        graphics.setComposite(getCompositeFor(pMode));
        if (pMaskRgn != null) {
            setClipRegion(pMaskRgn);
        }

        graphics.drawImage(
                pSrcBitmap,
                pDstRect.x,
                pDstRect.y,
                pDstRect.x + pDstRect.width,
                pDstRect.y + pDstRect.height,
                pSrcRect.x,
                pSrcRect.y,
                pSrcRect.x + pSrcRect.width,
                pSrcRect.y + pSrcRect.height,
                null
        );

        setClipRegion(null);
    }

    /**
     * CopyMask
     */
    public void copyMask(BufferedImage pSrcBitmap,
                         BufferedImage pMaskBitmap,
                         Rectangle pSrcRect,
                         Rectangle pMaskRect,
                         Rectangle pDstRect,
                         int pSrcCopy,
                         Shape pMaskRgn) {
        throw new UnsupportedOperationException("Method copyMask not implemented"); // TODO: Implement
    }

    /**
     * CopyDeepMask -- available to basic QuickDraw only in System 7, combines the functionality of both CopyBits and CopyMask
     */
    public void copyDeepMask(BufferedImage pSrcBitmap,
                             BufferedImage pMaskBitmap,
                             Rectangle pSrcRect,
                             Rectangle pMaskRect,
                             Rectangle pDstRect,
                             int pSrcCopy,
                             Shape pMaskRgn) {
        throw new UnsupportedOperationException("Method copyDeepMask not implemented"); // TODO: Implement
    }

   /*
   // Drawing With the Eight-Color System:
   ForeColor // color of the "ink" used to frame, fill, and paint.
   BackColor
   ColorBit

   // Getting Pattern Resources:
   GetPattern
   GetIndPattern

   // http://developer.apple.com/documentation/mac/Text/Text-128.html#HEADING128-9
   // Graphics Ports and Text Drawing

   // Setting Text Characteristics:
   TextFont // specifies the font to be used.
   TextFace // specifies the glyph style.
   TextMode // specifies the transfer mode.
   TextSize // specifies the font size.
   SpaceExtra // specifies the amount of pixels by which to widen or narrow each space character in a range of text.
   CharExtra // specifies the amount of pixels by which to widen or narrow each glyph other than the space characters in a range of text (CharExtra).
   GetFontInfo

   // Drawing Text:
   DrawChar // draws the glyph of a single 1-byte character.
   */

    /**
     * DrawString - draws the text of a Pascal string.
     *
     * @param pString a Pascal string (a string of length less than or equal to 255 chars).
     */
    public void drawString(String pString) {
        setupForText();
        graphics.drawString(pString, (float) getPenPosition().getX(), (float) getPenPosition().getY());
    }

   /*
   DrawText // draws the glyphs of a sequence of characters.
   DrawJustified // draws a sequence of text that is widened or narrowed by a specified number of pixels.

   // Measuring Text:
   CharWidth // returns the horizontal extension of a single glyph.
   StringWidth // returns the width of a Pascal string.
   TextWidth // returns the width of the glyphs of a text segment.
   MeasureText // fills an array with an entry for each character identifying the width of each character's glyph as measured from the left side of the entire text segment.
   MeasureJustified // fills an array with an entry for each character in a style run identifying the width of each character's glyph as measured from the left side of the text segment.

   // Laying Out a Line of Text:
   GetFormatOrder // determines the display order of style runs for a line of text containing multiple style runs with mixed directions
   VisibleLength // eliminates trailing spaces from the last style run on the line.
   PortionLine // determines how to distribute the total slop value for a line among the style runs on that line.

   // Determining the Caret Position, and Selecting and Highlighting Text:
   PixelToChar // converts a pixel location associated with a glyph in a range of text to a byte offset within the style run.
   CharToPixel // converts a byte offset to a pixel location. The pixel location is measured from the left edge of the style run.
   HiliteText // returns three pairs of offsets marking the endpoints of ranges of text to be highlighted.


// Color Constants
�whiteColor   =�30;
�blackColor   = 33
�yellowColor  = 69;
 magentaColor =�137;
�redColor     =�205;
�cyanColor    =�273;
�greenColor   =�341;
�blueColor    =�409;
    */

    // TODO: Simplify! Extract to upper level class
    static class RectangleStroke implements Stroke {

        private Shape mShapes[];

        private boolean repeat = true;

        private AffineTransform mTransform = new AffineTransform();

        private static final float FLATNESS = 1;

        public RectangleStroke(Shape pShape) {
            this(new Shape[]{pShape});
        }

        RectangleStroke(Shape pShapes[]) {
            mShapes = new Shape[pShapes.length];

            for (int i = 0; i < mShapes.length; i++) {
                Rectangle2D bounds = pShapes[i].getBounds2D();
                mTransform.setToTranslation(-bounds.getCenterX(), -bounds.getCenterY());
                mShapes[i] = mTransform.createTransformedShape(pShapes[i]);
            }
        }

        public Shape createStrokedShape(Shape shape) {
            GeneralPath result = new GeneralPath();

            PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
            float points[] = new float[6];
            float moveX = 0, moveY = 0;
            float lastX = 0, lastY = 0;
            float thisX = 0, thisY = 0;
            int type = 0;
            float next = 0;
            int currentShape = 0;
            int length = mShapes.length;

            while (currentShape < length && !it.isDone()) {
                type = it.currentSegment(points);
                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        moveX = lastX = points[0];
                        moveY = lastY = points[1];
                        result.moveTo(moveX, moveY);
                        next = 0;
                        break;

                    case PathIterator.SEG_CLOSE:
                        points[0] = moveX;
                        points[1] = moveY;
                        // Fall through

                    case PathIterator.SEG_LINETO:
                        thisX = points[0];
                        thisY = points[1];
                        float dx = thisX - lastX;
                        float dy = thisY - lastY;
                        float distance = (float) sqrt(dx * dx + dy * dy);
                        if (distance >= next) {
                            float r = 1.0f / distance;
                            //float angle = (float) Math.atan2(dy, dx);
                            while (currentShape < length && distance >= next) {
                                float x = lastX + next * dx * r;
                                float y = lastY + next * dy * r;
                                mTransform.setToTranslation(x, y);
                                //mTransform.rotate(angle);
                                result.append(mTransform.createTransformedShape(mShapes[currentShape]), false);
                                next += 1;
                                currentShape++;
                                if (repeat) {
                                    currentShape %= length;
                                }
                            }
                        }
                        next -= distance;
                        lastX = thisX;
                        lastY = thisY;
                        break;
                }
                it.next();
            }

            return result;
        }
    }
}