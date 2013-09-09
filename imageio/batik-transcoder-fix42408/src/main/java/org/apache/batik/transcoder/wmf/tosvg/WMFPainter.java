/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.apache.batik.transcoder.wmf.tosvg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.text.AttributedCharacterIterator;
import java.util.Iterator;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.apache.batik.ext.awt.geom.Polygon2D;
import org.apache.batik.ext.awt.geom.Polyline2D;
import org.apache.batik.transcoder.wmf.WMFConstants;
import org.apache.batik.util.Platform;

/**
  * Core class for rendering the WMF image. It is able to render a
  * WMF file in a <tt>Graphics</tt> object.
  *
  * @version $Id$
  * @author <a href="mailto:luano@asd.ie">Luan O'Carroll</a>
  */
public class WMFPainter extends AbstractWMFPainter {
    /**
     * Size of the buffer used for reading input WMF files
     */
    private static final int INPUT_BUFFER_SIZE = 30720;

    private static final Integer INTEGER_0 = new Integer( 0 );

    private float scale, scaleX, scaleY, conv;
    private float xOffset, yOffset;
    private float vpX, vpY, vpW, vpH;

    private Color frgdColor;
    private Color bkgdColor;
    private boolean opaque = false;
    private transient boolean firstEffectivePaint = true;

    private static BasicStroke solid
        = new BasicStroke( 1.0f,
                           BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_ROUND );

    private static BasicStroke textSolid
        = new BasicStroke( 1.0f,
                           BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_ROUND );

    private transient ImageObserver observer = new ImageObserver() {
        public boolean imageUpdate(Image img, int flags, int x,
            int y, int width, int height) {
                return false;
        }
    };

    /** Basic constructor.
     *  @param currentStore where the WMF records are stored
     *  @param scale the scale of the image
     */
    public WMFPainter(WMFRecordStore currentStore, float scale) {
        this(currentStore, 0, 0, scale);
    }

    /** Basic constructor.
     *  @param scale the scale of the image
     *  @param currentStore where the WMF records are stored
     *  @param xOffset x value for offset
     *  @param yOffset y value for offset
     */
    public WMFPainter(WMFRecordStore currentStore, int xOffset, int yOffset, float scale) {
        setRecordStore(currentStore);
        TextureFactory.getInstance().reset();
        this.conv = scale;
        this.xOffset = -xOffset;
        this.yOffset = -yOffset;
        this.scale = (float)currentStore.getWidthPixels() / (float)currentStore.getWidthUnits() * scale;
        this.scale = this.scale * (float)currentStore.getWidthPixels() / (float)currentStore.getVpW() ;
        float xfactor  = (float)currentStore.getVpW() / (float)currentStore.getWidthPixels()
            * (float)currentStore.getWidthUnits() / (float)currentStore.getWidthPixels();
        float yfactor  = (float)currentStore.getVpH() / (float)currentStore.getHeightPixels()
            * (float)currentStore.getHeightUnits() / (float)currentStore.getHeightPixels();
        this.xOffset = this.xOffset * xfactor;
        this.yOffset = this.yOffset * yfactor;
        scaleX = this.scale;
        scaleY = this.scale;
    }

    /**
     * Renders the WMF image(s).
     */
    public void paint( Graphics g ) {
        // Objects on DC stack;
        float fontHeight = 10;
        float fontAngle = 0;
        float penWidth = 0;
        float startX = 0;
        float startY = 0;
        int brushObject = -1;
        int penObject = -1;
        int fontObject = -1;
        Font font = null;
        int lastObjectIdx;
        Stack dcStack = new Stack();

        int numRecords = currentStore.getNumRecords();
        int numObjects = currentStore.getNumObjects();
        vpX = currentStore.getVpX() * scale;
        vpY = currentStore.getVpY() * scale;
        vpW = currentStore.getVpW() * scale;
        vpH = currentStore.getVpH() * scale;

        if ( !currentStore.isReading()) {
            GdiObject gdiObj;
            int gdiIndex;
            g.setPaintMode();

            /** added stroke definition for lines
             */
            Graphics2D g2d = (Graphics2D)g;
            g2d.setStroke(solid);

            brushObject = -1;
            penObject = -1;
            fontObject = -1;
            frgdColor = null;
            bkgdColor = Color.white;
            for ( int i = 0; i < numObjects; i++ ) {
                gdiObj = currentStore.getObject( i );
                gdiObj.clear();
            }

            float w = vpW;
            float h = vpH;

            g2d.setColor( Color.black );

            for ( int iRec = 0; iRec < numRecords; iRec++ ) {
                MetaRecord mr = currentStore.getRecord( iRec );

                switch ( mr.functionId ) {
                case WMFConstants.META_SETWINDOWORG:
                    currentStore.setVpX( vpX = -(float)mr.elementAt( 0 ) );
                    currentStore.setVpY( vpY = -(float)mr.elementAt( 1 ) );
                    vpX = vpX * scale;
                    vpY = vpY * scale;
                    break;

                case WMFConstants.META_SETWINDOWORG_EX: // ???? LOOKS SUSPICIOUS
                case WMFConstants.META_SETWINDOWEXT:
                    vpW = (float)mr.elementAt( 0 );
                    vpH = (float)mr.elementAt( 1 );


                    scaleX = scale;
                    scaleY = scale;
                    solid = new BasicStroke(scaleX*2,
                           BasicStroke.CAP_BUTT,
                           BasicStroke.JOIN_ROUND );

                      // Handled in the read function.
                      break;

                case WMFConstants.META_SETVIEWPORTORG:
                case WMFConstants.META_SETVIEWPORTEXT:
                case WMFConstants.META_OFFSETWINDOWORG:
                case WMFConstants.META_SCALEWINDOWEXT:
                case WMFConstants.META_OFFSETVIEWPORTORG:
                case WMFConstants.META_SCALEVIEWPORTEXT:
                    break;

                case WMFConstants.META_SETPOLYFILLMODE:
                    break;

                case WMFConstants.META_CREATEPENINDIRECT:
                    {
                        int objIndex = 0;
                        int penStyle = mr.elementAt( 0 );
                        Color newClr;
                        if ( penStyle == WMFConstants.META_PS_NULL ) {
                            newClr = Color.white;      // (255,255,255)
                            //objIndex = numObjects + 8;
                            objIndex = addObjectAt( currentStore, NULL_PEN, newClr, objIndex );
                        } else {
                            penWidth = mr.elementAt( 4 );
                            setStroke(g2d, penStyle, penWidth, scaleX);
                            newClr = new Color( mr.elementAt( 1 ),
                                                mr.elementAt( 2 ),
                                                mr.elementAt( 3 ) );
                            objIndex = addObjectAt( currentStore, PEN, newClr, objIndex );
                        }
                    }
                    break;

                case WMFConstants.META_CREATEBRUSHINDIRECT:
                    {
                        int objIndex = 0;
                        int brushStyle = mr.elementAt( 0 );
                        Color clr = new Color( mr.elementAt( 1 ),
                                               mr.elementAt( 2 ),
                                               mr.elementAt( 3 ) );
                        if ( brushStyle == WMFConstants.BS_SOLID ) {
                            objIndex = addObjectAt( currentStore, BRUSH, clr, objIndex );
                        } else if (brushStyle == WMFConstants.BS_HATCHED) {
                            int hatch = mr.elementAt( 4 );
                            Paint paint;
                            if (! opaque)
                                paint = TextureFactory.getInstance().getTexture(hatch, clr);
                            else
                                paint = TextureFactory.getInstance().getTexture(hatch, clr, bkgdColor);
                            if (paint != null)
                                objIndex = addObjectAt( currentStore, BRUSH, paint, objIndex );
                            else {
                                clr = Color.black;
                                objIndex = addObjectAt( currentStore, NULL_BRUSH, clr, objIndex );
                            }
                        } else {
                            clr = Color.black;
                            objIndex = addObjectAt( currentStore, NULL_BRUSH, clr, objIndex );
                        }
                    }
                    break;

                case WMFConstants.META_CREATEFONTINDIRECT:
                    {
                        float size = (int)( scaleY * mr.elementAt( 0 ));
                        int charset = mr.elementAt( 3 );

                        int italic = mr.elementAt( 1 );
                        int weight = mr.elementAt( 2 );
                        int style = italic > 0 ? Font.ITALIC : Font.PLAIN;
                        style |= (weight > 400) ? Font.BOLD : Font.PLAIN;

                        String face = ((MetaRecord.StringRecord)mr).text;
                        // management of font names
                        int d = 0;
                        while   ((d < face.length()) &&
                                ((Character.isLetterOrDigit(face.charAt(d))) ||
                                 (Character.isWhitespace(face.charAt(d))))) d++;
                        if (d > 0) face = face.substring(0,d);
                        else face = "System";

                        if ( size < 0 ) size = -size /* * -1.3 */;
                        int objIndex = 0;

                        fontHeight = size;

                        Font f = new Font(face, style, (int)size);
                        f = f.deriveFont(size);

                        int underline = mr.elementAt( 4 );
                        int strikeOut = mr.elementAt( 5 );
                        int orient = mr.elementAt( 6 );
                        int escape = mr.elementAt( 7 );

                        WMFFont wf = new WMFFont(f, charset, underline,
                            strikeOut, italic, weight, orient, escape);
                        objIndex = addObjectAt( currentStore, FONT, wf , objIndex );
                    }
                    break;

                case WMFConstants.META_CREATEBRUSH:
                case WMFConstants.META_CREATEPATTERNBRUSH:
                case WMFConstants.META_CREATEBITMAPINDIRECT:
                case WMFConstants.META_CREATEBITMAP:
                case WMFConstants.META_CREATEREGION: {
                    int objIndex = addObjectAt( currentStore, PALETTE, INTEGER_0, 0 );
                    }
                    break;

                case WMFConstants.META_CREATEPALETTE: {
                    int objIndex = addObjectAt( currentStore, OBJ_REGION, INTEGER_0, 0 );
                    }
                    break;

                case WMFConstants.META_SELECTPALETTE:
                case WMFConstants.META_REALIZEPALETTE:
                case WMFConstants.META_ANIMATEPALETTE:
                case WMFConstants.META_SETPALENTRIES:
                case WMFConstants.META_RESIZEPALETTE:
                    break;

                case WMFConstants.META_SELECTOBJECT:
                    gdiIndex = mr.elementAt( 0 );
                    if (( gdiIndex & 0x80000000 ) != 0 ) // Stock Object
                        break;
                    if ( gdiIndex >= numObjects ) {
                        gdiIndex -= numObjects;

                        switch ( gdiIndex ) {
                        case WMFConstants.META_OBJ_NULL_BRUSH:
                            brushObject = -1;
                            break;
                        case WMFConstants.META_OBJ_NULL_PEN:
                            penObject = -1;
                            break;
                        case WMFConstants.META_OBJ_WHITE_BRUSH:
                        case WMFConstants.META_OBJ_LTGRAY_BRUSH:
                        case WMFConstants.META_OBJ_GRAY_BRUSH:
                        case WMFConstants.META_OBJ_DKGRAY_BRUSH:
                        case WMFConstants.META_OBJ_BLACK_BRUSH:
                        case WMFConstants.META_OBJ_WHITE_PEN:
                        case WMFConstants.META_OBJ_BLACK_PEN:
                        case WMFConstants.META_OBJ_OEM_FIXED_FONT:
                        case WMFConstants.META_OBJ_ANSI_FIXED_FONT:
                        case WMFConstants.META_OBJ_ANSI_VAR_FONT:
                        case WMFConstants.META_OBJ_SYSTEM_FONT:
                        case WMFConstants.META_OBJ_DEVICE_DEFAULT_FONT:
                        case WMFConstants.META_OBJ_DEFAULT_PALETTE:
                        case WMFConstants.META_OBJ_SYSTEM_FIXED_FONT:
                            break;
                        }
                        break;
                    }
                    gdiObj = currentStore.getObject( gdiIndex );
                    if ( !gdiObj.used )
                        break;
                    switch( gdiObj.type ) {
                    case PEN:
                        g2d.setColor( (Color)gdiObj.obj );
                        penObject = gdiIndex;
                        break;
                    case BRUSH:
                        if (gdiObj.obj instanceof Color) g2d.setColor( (Color)gdiObj.obj );
                        else if (gdiObj.obj instanceof Paint) {
                            g2d.setPaint((Paint)gdiObj.obj);
                        } else g2d.setPaint(getPaint((byte[])(gdiObj.obj)));
                        brushObject = gdiIndex;
                        break;
                    case FONT: {
                        this.wmfFont =  ((WMFFont)gdiObj.obj);
                        Font f = this.wmfFont.font;
                        g2d.setFont(f);
                        fontObject = gdiIndex;
                        }
                        break;
                    case NULL_PEN:
                        penObject = -1;
                        break;
                    case NULL_BRUSH:
                        brushObject = -1;
                        break;
                    }
                    break;

                case WMFConstants.META_DELETEOBJECT:
                    gdiIndex = mr.elementAt( 0 );
                    gdiObj = currentStore.getObject( gdiIndex );
                    if ( gdiIndex == brushObject ) brushObject = -1;
                    else if ( gdiIndex == penObject ) penObject = -1;
                    else if ( gdiIndex == fontObject ) fontObject = -1;
                    gdiObj.clear();
                    break;

                case WMFConstants.META_POLYPOLYGON:
                    {
                        int numPolygons = mr.elementAt( 0 );
                        int[] pts = new int[ numPolygons ];
                        for ( int ip = 0; ip < numPolygons; ip++ )
                            pts[ ip ] = mr.elementAt( ip + 1 );

                        int offset = numPolygons+1;
                        List v = new ArrayList( numPolygons );
                        for ( int j = 0; j < numPolygons; j++ ) {
                            int count = pts[ j ];
                            float[] xpts = new float[count];
                            float[] ypts = new float[count];
                            for ( int k = 0; k < count; k++ ) {
                                xpts[k] = scaleX * (vpX + xOffset + mr.elementAt( offset + k*2   ) );
                                ypts[k] = scaleY * (vpY + yOffset + mr.elementAt( offset + k*2+1 ) );
                            }

                            offset += count*2;
                            Polygon2D pol = new Polygon2D(xpts, ypts, count);
                            v.add(pol);
                        }
                        /* need to do this for POLYPOLYGON, because only
                         * GeneralPaths can handle filling for complex WMF shapes, so
                         * we need to get all the Polygons and then convert them to a GeneralPath
                         */
                        if ( brushObject >= 0 ) {
                            setBrushPaint( currentStore, g2d, brushObject );
                            fillPolyPolygon(g2d, v);
                            firstEffectivePaint = false;
                        }
                        // painting with NULL PEN
                        if (penObject >= 0) {
                            setPenColor( currentStore, g2d, penObject );
                            drawPolyPolygon(g2d, v);
                            firstEffectivePaint = false;
                        }
                        break;
                    }

                case WMFConstants.META_POLYGON:
                    {
                        int count = mr.elementAt( 0 );
                        float[] _xpts = new float[ count ];
                        float[] _ypts = new float[ count ];
                        for ( int k = 0; k < count; k++ ) {
                            _xpts[k] = scaleX * ( vpX + xOffset + mr.elementAt( k*2+1 ) );
                            _ypts[k] = scaleY * ( vpY + yOffset + mr.elementAt( k*2+2 ) );
                        }
                        Polygon2D pol = new Polygon2D(_xpts, _ypts, count);
                        paint(brushObject, penObject, pol, g2d);
                    }
                    break;

                case WMFConstants.META_MOVETO:
                    startX = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                    startY = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                    break;

                case WMFConstants.META_LINETO:
                    {
                        float endX = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        float endY = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        // painting with NULL PEN
                        Line2D.Float line = new Line2D.Float(startX, startY, endX, endY);
                        paintWithPen(penObject, line, g2d);
                        startX = endX;
                        startY = endY;
                    }
                    break;

                case WMFConstants.META_POLYLINE:
                    {
                        int count = mr.elementAt( 0 );
                        float[] _xpts = new float[ count ];
                        float[] _ypts = new float[ count ];
                        for ( int k = 0; k < count; k++ ) {
                            _xpts[k] = scaleX * ( vpX + xOffset + mr.elementAt( k*2+1 ) );
                            _ypts[k] = scaleY * ( vpY + yOffset + mr.elementAt( k*2+2 ) );
                        }
                        Polyline2D pol = new Polyline2D(_xpts, _ypts, count);
                        paintWithPen(penObject, pol, g2d);
                    }
                    break;

                case WMFConstants.META_RECTANGLE:
                    {
                        float x1, y1, x2, y2;
                        x1 = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        x2 = scaleX * ( vpX + xOffset + mr.elementAt( 2 ) );
                        y1 = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        y2 = scaleY * ( vpY + yOffset + mr.elementAt( 3 ) );

                        Rectangle2D.Float rec = new Rectangle2D.Float(x1, y1, x2-x1, y2-y1);
                        paint(brushObject, penObject, rec, g2d);
                    }
                    break;

                case WMFConstants.META_ROUNDRECT:
                    {
                        float x1, y1, x2, y2, x3, y3;
                        x1 = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        x2 = scaleX * ( vpX + xOffset + mr.elementAt( 2 ) );
                        x3 = scaleX * (float)(mr.elementAt( 4 ) );
                        y1 = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        y2 = scaleY * ( vpY + yOffset + mr.elementAt( 3 ) );
                        y3 = scaleY * (float)(mr.elementAt( 5 ) );

                        RoundRectangle2D rec =
                            new RoundRectangle2D.Float(x1, y1, x2-x1, y2-y1, x3, y3);

                        paint(brushObject, penObject, rec, g2d);
                    }
                    break;

                case WMFConstants.META_ELLIPSE:
                    {
                        float x1 = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        float x2 = scaleX * ( vpX + xOffset + mr.elementAt( 2 ) );
                        float y1 = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        float y2 = scaleY * ( vpY + yOffset + mr.elementAt( 3 ) );

                        Ellipse2D.Float el = new Ellipse2D.Float(x1, y1, x2-x1, y2-y1);
                        paint(brushObject, penObject, el, g2d);
                    }
                    break;

                case WMFConstants.META_SETTEXTALIGN:
                    currentHorizAlign =
                            WMFUtilities.getHorizontalAlignment( mr.elementAt( 0 ) );
                    currentVertAlign =
                            WMFUtilities.getVerticalAlignment( mr.elementAt( 0 ) );
                    break;

                case WMFConstants.META_SETTEXTCOLOR:
                    frgdColor = new Color( mr.elementAt( 0 ),
                                           mr.elementAt( 1 ),
                                           mr.elementAt( 2 ) );
                    g2d.setColor(frgdColor);
                    break;

                case WMFConstants.META_SETBKCOLOR:
                    bkgdColor = new Color( mr.elementAt( 0 ),
                                           mr.elementAt( 1 ),
                                           mr.elementAt( 2 ) );
                    g2d.setColor(bkgdColor);
                    break;

                case WMFConstants.META_EXTTEXTOUT:
                    try {
                        byte[] bstr = ((MetaRecord.ByteRecord)mr).bstr;
                        String sr = WMFUtilities.decodeString(wmfFont, bstr);

                        float x = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        float y = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        if ( frgdColor != null ) g2d.setColor( frgdColor );
                        else g2d.setColor( Color.black );

                        FontRenderContext frc = g2d.getFontRenderContext();

                        Point2D.Double pen = new Point2D.Double( 0, 0 );
                        GeneralPath gp = new GeneralPath( GeneralPath.WIND_NON_ZERO );
                        TextLayout layout = new TextLayout( sr, g2d.getFont(), frc );

                        int flag = mr.elementAt( 2 );
                        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                        boolean clipped = false;
                        Shape clip = null;
                        // process clipped texts
                        if ((flag & WMFConstants.ETO_CLIPPED) != 0) {
                            clipped = true;
                            x1 = mr.elementAt( 3 );
                            y1 = mr.elementAt( 4 );
                            x2 = mr.elementAt( 5 );
                            y2 = mr.elementAt( 6 );
                            clip = g2d.getClip();
                            g2d.setClip(x1, y1, x2, y2);
                        }

                        firstEffectivePaint = false;
                        y += getVerticalAlignmentValue(layout, currentVertAlign);

                        drawString(flag, g2d,
                            getCharacterIterator(g2d, sr, wmfFont, currentHorizAlign),
                            x, y, layout, wmfFont, currentHorizAlign);
                        if (clipped) g2d.setClip(clip);
                    } catch ( Exception e ) {
                    }
                    break;

                case WMFConstants.META_TEXTOUT:
                case WMFConstants.META_DRAWTEXT:
                    try {
                        byte[] bstr = ((MetaRecord.ByteRecord)mr).bstr;
                        String sr = WMFUtilities.decodeString(wmfFont, bstr);

                        float x = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        float y = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        if ( frgdColor != null ) g2d.setColor( frgdColor );
                        else g2d.setColor( Color.black );

                        FontRenderContext frc = g2d.getFontRenderContext();

                        Point2D.Double pen = new Point2D.Double( 0, 0 );
                        GeneralPath gp = new GeneralPath( GeneralPath.WIND_NON_ZERO );
                        TextLayout layout = new TextLayout( sr, g2d.getFont(), frc );

                        firstEffectivePaint = false;
                        y += getVerticalAlignmentValue(layout, currentVertAlign);

                        drawString(-1, g2d,
                            getCharacterIterator(g2d, sr, wmfFont),
                            x, y, layout, wmfFont, currentHorizAlign);
                    } catch ( Exception e ) {
                    }
                    break;

                case WMFConstants.META_ARC:
                case WMFConstants.META_PIE:
                    {
                        double left, top, right, bottom;
                        double xstart, ystart, xend, yend;
                        left = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        top =  scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        right = scaleX * ( vpX + xOffset + mr.elementAt( 2 ) );
                        bottom = scaleY * ( vpY + yOffset + mr.elementAt( 3 ) );
                        xstart = scaleX * ( vpX + xOffset + mr.elementAt( 4 ) );
                        ystart = scaleY * ( vpY + yOffset + mr.elementAt( 5 ) );
                        xend = scaleX * ( vpX + xOffset + mr.elementAt( 6 ) );
                        yend = scaleY * ( vpY + yOffset + mr.elementAt( 7 ) );
                        setBrushPaint( currentStore, g2d, brushObject );

                        double cx = left + (right - left)/2;
                        double cy = top + (bottom - top)/2;
                        double startAngle = - Math.toDegrees( Math.atan2(ystart - cy, xstart - cx) );
                        double endAngle   = - Math.toDegrees( Math.atan2(yend - cy, xend - cx) );

                        double extentAngle = endAngle - startAngle;
                        if (extentAngle < 0) extentAngle += 360;
                        if (startAngle < 0) startAngle +=360;

                        Arc2D.Double arc = new Arc2D.Double(left, top, right - left, bottom - top,
                            startAngle, extentAngle, Arc2D.OPEN);
                        if ( mr.functionId == WMFConstants.META_ARC ) g2d.draw(arc);
                        else g2d.fill(arc);
                        firstEffectivePaint = false;
                    }
                    break;


                case WMFConstants.META_CHORD:
                {
                        double left, top, right, bottom;
                        double xstart, ystart, xend, yend;
                        left   = scaleX * ( vpX + xOffset + mr.elementAt( 0 ) );
                        top    = scaleY * ( vpY + yOffset + mr.elementAt( 1 ) );
                        right  = scaleX * ( vpX + xOffset + mr.elementAt( 2 ) );
                        bottom = scaleY * ( vpY + yOffset + mr.elementAt( 3 ) );
                        xstart = scaleX * ( vpX + xOffset + mr.elementAt( 4 ) );
                        ystart = scaleY * ( vpY + yOffset + mr.elementAt( 5 ) );
                        xend   = scaleX * ( vpX + xOffset + mr.elementAt( 6 ) );
                        yend   = scaleY * ( vpY + yOffset + mr.elementAt( 7 ) );
                        setBrushPaint( currentStore, g2d, brushObject );

                        double cx = left + (right - left)/2;
                        double cy = top + (bottom - top)/2;
                        double startAngle = - Math.toDegrees( Math.atan2(ystart - cy, xstart - cx) );
                        double endAngle   = - Math.toDegrees( Math.atan2(yend - cy, xend - cx) );

                        double extentAngle = endAngle - startAngle;
                        if (extentAngle < 0) extentAngle += 360;
                        if (startAngle < 0) startAngle +=360;

                        Arc2D.Double arc = new Arc2D.Double(left, top, right - left, bottom - top,
                            startAngle, extentAngle, Arc2D.CHORD);
                        paint(brushObject, penObject, arc, g2d);
                        firstEffectivePaint = false;
                    }
                    break;

                case WMFConstants.META_SAVEDC:
                    dcStack.push( new Float( penWidth ));
                    dcStack.push( new Float( startX ));
                    dcStack.push( new Float( startY ));
                    dcStack.push( new Integer( brushObject ));
                    dcStack.push( new Integer( penObject ));
                    dcStack.push( new Integer( fontObject ));
                    dcStack.push( frgdColor );
                    dcStack.push( bkgdColor );
                    break;

                case WMFConstants.META_RESTOREDC:
                    bkgdColor = (Color)dcStack.pop();
                    frgdColor = (Color)dcStack.pop();
                    fontObject = ((Integer)(dcStack.pop())).intValue();
                    penObject = ((Integer)(dcStack.pop())).intValue();
                    brushObject = ((Integer)(dcStack.pop())).intValue();
                    startY = ((Float)(dcStack.pop())).floatValue();
                    startX = ((Float)(dcStack.pop())).floatValue();
                    penWidth = ((Float)(dcStack.pop())).floatValue();
                    break;

                case WMFConstants.META_POLYBEZIER16:
                    try
                        {
                            setPenColor( currentStore, g2d, penObject );

                            int pointCount = mr.elementAt( 0 );
                            int bezierCount = ( pointCount-1 ) / 3;
                            float _startX = scaleX * ( vpX + xOffset + mr.elementAt( 1 ) );
                            float _startY = scaleY * ( vpY + yOffset + mr.elementAt( 2 ) );

                            GeneralPath gp = new GeneralPath( GeneralPath.WIND_NON_ZERO );
                            gp.moveTo( _startX, _startY );

                            for ( int j = 0; j < bezierCount; j++ ) {
                                int j6 = j*6;
                                float cp1X = scaleX * ( vpX + xOffset + mr.elementAt( j6+3 ) );
                                float cp1Y = scaleY * ( vpY + yOffset + mr.elementAt( j6+4 ) );

                                float cp2X = scaleX * ( vpX + xOffset + mr.elementAt( j6+5 ) );
                                float cp2Y = scaleY * ( vpY + yOffset + mr.elementAt( j6+6 ) );

                                float endX = scaleX * ( vpX + xOffset + mr.elementAt( j6+7 ) );
                                float endY = scaleY * ( vpY + yOffset + mr.elementAt( j6+8 ) );

                                gp.curveTo( cp1X, cp1Y, cp2X, cp2Y, endX, endY );
                                _startX = endX;
                                _startY = endY;
                            }
                                //gp.closePath();
                            g2d.setStroke( solid );
                            g2d.draw( gp );
                            firstEffectivePaint = false;
                        }
                    catch ( Exception e ) {
                    }
                    break;

                case WMFConstants.META_EXCLUDECLIPRECT:
                case WMFConstants.META_INTERSECTCLIPRECT:

                case WMFConstants.META_OFFSETCLIPRGN:
                case WMFConstants.META_SELECTCLIPREGION:

                case WMFConstants.META_SETMAPMODE:
                case WMFConstants.META_SETRELABS:
                case WMFConstants.META_SETSTRETCHBLTMODE:
                case WMFConstants.META_SETTEXTCHAREXTRA:
                case WMFConstants.META_SETTEXTJUSTIFICATION:
                case WMFConstants.META_FLOODFILL:
                    break;

                case WMFConstants.META_SETBKMODE:
                    {
                        int mode = mr.elementAt( 0 );
                        opaque = (mode == WMFConstants.OPAQUE);
                    }
                    break;

                    //UPDATED : added SETROP2
                case WMFConstants.META_SETROP2:
                    {
                        float rop = (float)(mr.ElementAt( 0 ).intValue());
                        Paint paint = null;
                        boolean ok = false;
                        if (rop == WMFConstants.META_BLACKNESS) {
                            paint = Color.black;
                            ok = true;
                        } else if (rop == WMFConstants.META_WHITENESS) {
                            paint = Color.white;
                            ok = true;
                        } else if (rop == WMFConstants.META_PATCOPY) {
                            if ( brushObject >= 0 ) {
                                paint = getStoredPaint(currentStore, brushObject);
                                ok = true;
                            }
                        }

                        if (ok) {
                            if (paint != null) {
                                g2d.setPaint(paint);
                            } else {
                                setBrushPaint( currentStore, g2d, brushObject );
                            }
                        }
                    }
                    break;
                case WMFConstants.META_PATBLT:
                    {
                        float rop = (float)(mr.elementAt( 0 ) );
                        float height = scaleY * (float)(mr.elementAt( 1 ) );
                        float width = scaleX * (float)(mr.elementAt( 2 ) );
                        float left = scaleX * (vpX + xOffset + mr.elementAt( 3 ) );
                        float top  = scaleY * (vpY + yOffset + mr.elementAt( 4 ) );

                        Paint paint = null;
                        boolean ok = false;
                        if (rop == WMFConstants.META_BLACKNESS) {
                            paint = Color.black;
                            ok = true;
                        } else if (rop == WMFConstants.META_WHITENESS) {
                            paint = Color.white;
                            ok = true;
                        } else if (rop == WMFConstants.META_PATCOPY) {
                            if ( brushObject >= 0 ) {
                                paint = getStoredPaint(currentStore, brushObject);
                                ok = true;
                            }
                        }
                        if (ok) {
                            Color oldClr = g2d.getColor();
                            if (paint != null) {
                                g2d.setPaint(paint);
                            } else {
                                setBrushPaint( currentStore, g2d, brushObject );
                            }

                            /* avoid to draw a huge rectangle on beginning of the drawing
                             * This is caused by the fact that a WMF file always seems to contain
                             * a META_PATBLT rectangle at its beginning.
                             */
                            Rectangle2D.Float rec = new Rectangle2D.Float(left, top, width, height);
                            g2d.fill(rec);
                            g2d.setColor(oldClr);
                        }
                    }
                    break;
                case WMFConstants.META_DIBSTRETCHBLT:
                    {
                        int height = mr.elementAt( 1 );
                        int width = mr.elementAt( 2 );
                        int sy = mr.elementAt( 3 );
                        int sx = mr.elementAt( 4 );
                        float dy = conv * currentStore.getVpWFactor() * (vpY + yOffset + mr.elementAt( 7 ) );
                        float dx = conv * currentStore.getVpHFactor() * (vpX + xOffset + mr.elementAt( 8 ) );
                        float heightDst = (float)(mr.elementAt( 5 ) );
                        float widthDst = (float)(mr.elementAt( 6 ) );
                        widthDst = widthDst * conv * currentStore.getVpWFactor();
                        heightDst = heightDst * conv * currentStore.getVpHFactor();
                        byte[] bitmap = ((MetaRecord.ByteRecord)mr).bstr;

                        BufferedImage img = getImage(bitmap, width, height);
                        if (img != null) {
                            g2d.drawImage(img, (int)dx, (int)dy, (int)(dx + widthDst),
                            (int)(dy + heightDst), sx, sy, sx + width,
                            sy + height, bkgdColor, observer);
                        }
                    }
                    break;
                    case WMFConstants.META_STRETCHDIB:  {
                        int height = mr.elementAt( 1 );
                        int width = mr.elementAt( 2 );
                        int sy = mr.elementAt( 3 );
                        int sx = mr.elementAt( 4 );
                        float dy = conv * currentStore.getVpWFactor() * 
                            (vpY + yOffset + (float)mr.elementAt( 7 ));
                        float dx = conv * currentStore.getVpHFactor() * 
                            (vpX + xOffset + (float)mr.elementAt( 8 ));                        
                        float heightDst = (float)(mr.elementAt( 5 ));
                        float widthDst = (float)(mr.elementAt( 6 ));
                        widthDst = widthDst * conv * currentStore.getVpWFactor();
                        heightDst = heightDst * conv * currentStore.getVpHFactor();
                        byte[] bitmap = ((MetaRecord.ByteRecord)mr).bstr;

                        BufferedImage img = getImage(bitmap, width, height);
                        if (img != null) {
                            if (opaque) {
                                g2d.drawImage(img, (int)dx, (int)dy, (int)(dx + widthDst),
                                    (int)(dy + heightDst), sx, sy, sx + width,
                                    sy + height, bkgdColor, observer);
                            } else {
                                //g2d.setComposite(AlphaComposite.SrcOver);
                                g2d.drawImage(img, (int)dx, (int)dy, (int)(dx + widthDst),
                                    (int)(dy + heightDst), sx, sy, sx + width,
                                    sy + height, observer);                                
                            }
                        }                        
                    }
                    break;                                        
                case WMFConstants.META_DIBBITBLT:
                    {
                        int rop = mr.ElementAt( 0 ).intValue();
                        float height = (mr.ElementAt( 1 ).intValue() *
                                        conv * currentStore.getVpWFactor());
                        float width  = (mr.ElementAt( 2 ).intValue() *
                                        conv * currentStore.getVpHFactor());
                        int sy = mr.ElementAt( 3 ).intValue();
                        int sx = mr.ElementAt( 4 ).intValue();
                        float dy = (conv * currentStore.getVpWFactor() *
                                    (vpY + yOffset +
                                     (float)mr.ElementAt( 5 ).intValue()));
                        float dx = (conv * currentStore.getVpHFactor() *
                                    (vpX + xOffset +
                                     (float)mr.ElementAt( 6 ).intValue()));
                        if (mr instanceof MetaRecord.ByteRecord) {
                            byte[] bitmap = ((MetaRecord.ByteRecord)mr).bstr;

                            BufferedImage img = getImage(bitmap);
                            if (img != null) {
                                int withSrc = img.getWidth();
                                int heightSrc = img.getHeight();
                                if (opaque) {
                                    g2d.drawImage(img, (int)dx, (int)dy,
                                                  (int)(dx + width),
                                                  (int)(dy + height),
                                                  sx, sy,
                                                  sx + withSrc,
                                                  sy + heightSrc,
                                                  bkgdColor, observer);
                                } else {
                                    //g2d.setComposite(AlphaComposite.SrcOver);
                                    g2d.drawImage(img, (int)dx, (int)dy,
                                                  (int)(dx + width),
                                                  (int)(dy + height),
                                                  sx, sy,
                                                  sx + withSrc,
                                                  sy + heightSrc, observer);
                                }
                            }
                        } else {
                            if (opaque) {
                                Color col = g2d.getColor();
                                g2d.setColor(bkgdColor);
                                g2d.fill(new Rectangle2D.Float(dx, dy,
                                                               width, height));
                                g2d.setColor(col);
                            }

                        }
                    }
                 break;
            case WMFConstants.META_DIBCREATEPATTERNBRUSH:
                {
                    int objIndex = 0;
                    byte[] bitmap = ((MetaRecord.ByteRecord)mr).bstr;
                    objIndex = addObjectAt( currentStore, BRUSH,
                                            bitmap, objIndex );
                }
            break;
                case WMFConstants.META_SETPIXEL:
                case WMFConstants.META_BITBLT:
                case WMFConstants.META_STRETCHBLT:
                case WMFConstants.META_ESCAPE:
                case WMFConstants.META_FILLREGION:
                case WMFConstants.META_FRAMEREGION:
                case WMFConstants.META_INVERTREGION:
                case WMFConstants.META_PAINTREGION:
                case WMFConstants.META_SETMAPPERFLAGS:
                case WMFConstants.META_SETDIBTODEV:
                default:
                    {
                    }
                    //os << " ------Unknown Function------";
                    break;
                }
            }

        }
    }

    private Paint getPaint(byte[] bit) {
        Dimension d = getImageDimension(bit);
        BufferedImage img = getImage(bit);
        Rectangle2D rec = new Rectangle2D.Float(0, 0, (float)d.width, (float)d.height);
        TexturePaint paint = new TexturePaint(img, rec);
        return paint;
    }

    /** Draw an AttributedCharacterIterator taking into account the following characteristics. :
     *  <ul>
     *  <li>the orientation (escapement) of the WMF Font</li>
     *  <li>the aligment of the text</li>
     *  </ul>
     *  <p>The other characteristics of the text, deriving from the WMF Font, must have been
     *  collected before to constitute the AttributedCharacterIterator.</p>
     */
    private void drawString(int flag, Graphics2D g2d, AttributedCharacterIterator ati,
        float x, float y, TextLayout layout, WMFFont wmfFont, int align) {

        if (wmfFont.escape == 0) {
            if (flag != -1) fillTextBackground(-1, flag, g2d, x, y, 0, layout);
            float width = (float)(layout.getBounds().getWidth());
            if (align == WMFConstants.TA_CENTER) {
                g2d.drawString(ati, x-width/2, y);
            } else if (align == WMFConstants.TA_RIGHT) {
                g2d.drawString(ati, x-width, y);
            } else {
                g2d.drawString(ati, x, y);
            }
        } else {
            AffineTransform tr = g2d.getTransform();
            float angle = - (float)(wmfFont.escape * Math.PI / 1800f);

            float width = (float)(layout.getBounds().getWidth());
            float height = (float)(layout.getBounds().getHeight());
            if (align == WMFConstants.TA_CENTER) {
                g2d.translate(-width/2, height/2);
                g2d.rotate(angle, x - width/2, y);
            } else if (align == WMFConstants.TA_RIGHT) {
                g2d.translate(-width/2, height/2);
                g2d.rotate(angle, x - width, y);
            } else {
                g2d.translate(0, height/2);
                g2d.rotate(angle, x, y);
            }
            if (flag != -1) fillTextBackground(align, flag, g2d, x, y, width, layout);
            Stroke _st = g2d.getStroke();
            g2d.setStroke(textSolid);
            g2d.drawString(ati, x, y);
            g2d.setStroke(_st);
            g2d.setTransform(tr);
        }
    }

    private void fillTextBackground(int align, int flag, Graphics2D g2d, float x, float y,
        float width, TextLayout layout) {

        float _x = x;
        if (align == WMFConstants.TA_CENTER) _x = x-width/2;
        else if (align == WMFConstants.TA_RIGHT) _x = x - width;

        if ((flag & WMFConstants.ETO_OPAQUE) != 0) {
            Color c = g2d.getColor();
            AffineTransform tr = g2d.getTransform();
            g2d.setColor(bkgdColor);
            g2d.translate(_x, y);
            g2d.fill(layout.getBounds());
            g2d.setColor(c);
            g2d.setTransform(tr);
        } else if (opaque) {
            Color c = g2d.getColor();
            AffineTransform tr = g2d.getTransform();
            g2d.setColor(bkgdColor);
            g2d.translate(_x, y);
            g2d.fill(layout.getBounds());
            g2d.setColor(c);
            g2d.setTransform(tr);
        }
    }

    /** Just to be consistent with PolyPolygon filling.
     */
    private void drawPolyPolygon(Graphics2D g2d, List pols) {
        Iterator it = pols.iterator();
        while (it.hasNext()) {
            Polygon2D pol = (Polygon2D)(it.next());
            g2d.draw(pol);
        }
    }

    /** Need to do this for POLYPOLYGON, because only GeneralPaths can handle complex
     * WMF shapes.
     */
    private void fillPolyPolygon(Graphics2D g2d, List pols) {
        // if there is only one Polygon, there is no need of a path
        if (pols.size() == 1) g2d.fill((Polygon2D)(pols.get(0)));
        // the real stuff : we create an EVEN_ODD path, and add all the Shapes to it
        else {
            GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            for (int i = 0; i < pols.size(); i++) {
                Polygon2D pol = (Polygon2D)(pols.get(i));
                path.append(pol, false);
            }
            g2d.fill(path);
        }
    }

    private void setStroke(Graphics2D g2d, int penStyle, float penWidth, float scale) {
        float _width;
        if (penWidth == 0) _width = 1;
        else _width = penWidth;
        float _scale = (float)Platform.getScreenResolution() /
            currentStore.getMetaFileUnitsPerInch();
        // need to do this, to put the width in sync with the general scale of the image
        float factor = scale  / _scale;
        _width = _width * _scale * factor;
        _scale = currentStore.getWidthPixels() * 1f/350f; // necessary to handle dash patterns

        BasicStroke stroke;
        if (penStyle == WMFConstants.META_PS_SOLID) {
            stroke = new BasicStroke(_width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
            g2d.setStroke(stroke);
        } else if (penStyle == WMFConstants.META_PS_DOT) {
            float[] dash = {1.0f * _scale, 5f * _scale};
            stroke = new BasicStroke(_width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f * _scale, dash, 0f);
            g2d.setStroke(stroke);
        } else if (penStyle == WMFConstants.META_PS_DASH) {
            float[] dash = {5f * _scale, 2f * _scale};
            stroke = new BasicStroke(_width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f * _scale, dash, 0f);
            g2d.setStroke(stroke);
        } else if (penStyle == WMFConstants.META_PS_DASHDOT) {
            float[] dash = {5f * _scale, 2f * _scale, 1.0f * _scale, 2f * _scale};
            stroke = new BasicStroke(_width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f * _scale, dash, 0f);
            g2d.setStroke(stroke);
        } else if (penStyle == WMFConstants.META_PS_DASHDOTDOT) {
            float[] dash = {5f * _scale, 2f * _scale, 1.0f * _scale, 2f * _scale, 1f * _scale, 2f * _scale};
            stroke = new BasicStroke(_width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 15f * _scale, dash, 0f);
            g2d.setStroke(stroke);
        } else {
            stroke = new BasicStroke(_width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
            g2d.setStroke(stroke);
        }
    }

    private void setPenColor( WMFRecordStore currentStore, Graphics2D g2d, int penObject) {
        if ( penObject >= 0 ) {
            GdiObject gdiObj = currentStore.getObject( penObject );
            g2d.setColor( (Color)gdiObj.obj );
            penObject = -1;
        }
    }

    private int getHorizontalAlignement(int align) {
        int v = align;
        v = v % 24; // skip baseline alignment
        v = v % 8;  // skip bottom aligment
        if (v >= 6) return WMFConstants.TA_CENTER;
        else if (v >= 2) return WMFConstants.TA_RIGHT;
        else return WMFConstants.TA_LEFT;
    }

    private void setBrushPaint( WMFRecordStore currentStore, Graphics2D g2d, int brushObject) {
        if ( brushObject >= 0 ) {
            GdiObject gdiObj = currentStore.getObject( brushObject );
            if (gdiObj.obj instanceof Color) g2d.setColor( (Color)gdiObj.obj );
            else if (gdiObj.obj instanceof Paint) g2d.setPaint( (Paint)gdiObj.obj );
            else g2d.setPaint(getPaint((byte[])gdiObj.obj));
            brushObject = -1;
        }
    }

    private Paint getStoredPaint(WMFRecordStore currentStore, int object) {
        if ( object >= 0 ) {
            GdiObject gdiObj = currentStore.getObject( object );
            if (gdiObj.obj instanceof Paint) return (Paint)gdiObj.obj;
            else return getPaint((byte[])gdiObj.obj);
        } else return null;
    }

    /** Draw or / and fill the Shape, depending on the pen or brush Objects selected.
     */
    private void paint(int brushObject, int penObject, Shape shape, Graphics2D g2d) {
        if ( brushObject >= 0 ) {
            Paint paint = getStoredPaint(currentStore, brushObject);
            if (!((firstEffectivePaint) && (paint.equals(Color.white)))) {
                setBrushPaint( currentStore, g2d, brushObject );
                g2d.fill(shape);
                firstEffectivePaint = false;
            }
        }
        // FIXED : painting with NULL PEN
        if (penObject >= 0) {
            Paint paint = getStoredPaint(currentStore, penObject);
            if (!((firstEffectivePaint) && (paint.equals(Color.white)))) {
                setPenColor( currentStore, g2d, penObject );
                g2d.draw(shape);
                firstEffectivePaint = false;
            }
        }
    }

    /** Draw the Shape, depending on the pen or brush Objects selected.
     */
    private void paintWithPen(int penObject, Shape shape, Graphics2D g2d) {
        if (penObject >= 0) {
            Paint paint = getStoredPaint(currentStore, penObject);
            if (!((firstEffectivePaint) && (paint.equals(Color.white)))) {
                setPenColor( currentStore, g2d, penObject );
                g2d.draw(shape);
                firstEffectivePaint = false;
            }
        }
    }

    private float getVerticalAlignmentValue(TextLayout layout, int vertAlign) {
        if (vertAlign == WMFConstants.TA_BOTTOM) return -layout.getDescent();
        else if (vertAlign == WMFConstants.TA_TOP) return layout.getAscent();
        else return 0;
    }

    /**
     * Returns the WMFRecordStore this WMFPainter renders
     */
    public WMFRecordStore getRecordStore(){
        return currentStore;
    }

    private transient BufferedInputStream bufStream = null;

}