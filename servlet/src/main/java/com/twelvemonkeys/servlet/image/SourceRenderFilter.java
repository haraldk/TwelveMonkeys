package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.servlet.ServletUtil;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.IOException;

/**
 * A {@link javax.servlet.Filter} that extracts request parameters, and sets the
 * corresponding request attributes from {@link ImageServletResponse}.
 * Only affects how the image is decoded, and must be applied before any
 * other image filters in the chain.
 * <p/>
 * @see ImageServletResponse#ATTRIB_SIZE
 * @see ImageServletResponse#ATTRIB_AOI
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-servlet/src/main/java/com/twelvemonkeys/servlet/image/SourceRenderFilter.java#1 $
 */
public class SourceRenderFilter extends ImageFilter {
    private String mSizeWidthParam = "size.w";
    private String mSizeHeightParam = "size.h";
    private String mSizePercentParam = "size.percent";
    private String mSizeUniformParam = "size.uniform";

    private String mRegionWidthParam = "aoi.w";
    private String mRegionHeightParam = "aoi.h";
    private String mRegionLeftParam = "aoi.x";
    private String mRegionTopParam = "aoi.y";
    private String mRegionPercentParam = "aoi.percent";
    private String mRegionUniformParam = "aoi.uniform";

    public void setRegionHeightParam(String pRegionHeightParam) {
        mRegionHeightParam = pRegionHeightParam;
    }

    public void setRegionWidthParam(String pRegionWidthParam) {
        mRegionWidthParam = pRegionWidthParam;
    }

    public void setRegionLeftParam(String pRegionLeftParam) {
        mRegionLeftParam = pRegionLeftParam;
    }

    public void setRegionTopParam(String pRegionTopParam) {
        mRegionTopParam = pRegionTopParam;
    }

    public void setSizeHeightParam(String pSizeHeightParam) {
        mSizeHeightParam = pSizeHeightParam;
    }

    public void setSizeWidthParam(String pSizeWidthParam) {
        mSizeWidthParam = pSizeWidthParam;
    }

    public void setRegionPercentParam(String pRegionPercentParam) {
        mRegionPercentParam = pRegionPercentParam;
    }

    public void setRegionUniformParam(String pRegionUniformParam) {
        mRegionUniformParam = pRegionUniformParam;
    }

    public void setSizePercentParam(String pSizePercentParam) {
        mSizePercentParam = pSizePercentParam;
    }

    public void setSizeUniformParam(String pSizeUniformParam) {
        mSizeUniformParam = pSizeUniformParam;
    }

    public void init() throws ServletException {
        if (mTriggerParams == null) {
            // Add all params as triggers
            mTriggerParams = new String[]{mSizeWidthParam, mSizeHeightParam,
                    mSizeUniformParam, mSizePercentParam,
                    mRegionLeftParam, mRegionTopParam,
                    mRegionWidthParam, mRegionHeightParam,
                    mRegionUniformParam, mRegionPercentParam};
        }
    }

    /**
     * Extracts request parameters, and sets the corresponding request
     * attributes if specified.
     *
     * @param pRequest
     * @param pResponse
     * @param pChain
     * @throws IOException
     * @throws ServletException
     */
    protected void doFilterImpl(ServletRequest pRequest, ServletResponse pResponse, FilterChain pChain) throws IOException, ServletException {
        // TODO: Max size configuration, to avoid DOS attacks? OutOfMemory

        // Size parameters
        int width = ServletUtil.getIntParameter(pRequest, mSizeWidthParam, -1);
        int height = ServletUtil.getIntParameter(pRequest, mSizeHeightParam, -1);
        if (width > 0 || height > 0) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE, new Dimension(width, height));
        }

        // Size uniform/percent
        boolean uniform = ServletUtil.getBooleanParameter(pRequest, mSizeUniformParam, true);
        if (!uniform) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE_UNIFORM, Boolean.FALSE);
        }
        boolean percent = ServletUtil.getBooleanParameter(pRequest, mSizePercentParam, false);
        if (percent) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE_PERCENT, Boolean.TRUE);
        }

        // Area of interest parameters
        int x = ServletUtil.getIntParameter(pRequest, mRegionLeftParam, -1); // Default is center
        int y = ServletUtil.getIntParameter(pRequest, mRegionTopParam, -1);  // Default is center
        width = ServletUtil.getIntParameter(pRequest, mRegionWidthParam, -1);
        height = ServletUtil.getIntParameter(pRequest, mRegionHeightParam, -1);
        if (width > 0 || height > 0) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_AOI, new Rectangle(x, y, width, height));
        }

        // AOI uniform/percent
        uniform = ServletUtil.getBooleanParameter(pRequest, mRegionUniformParam, false);
        if (uniform) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE_UNIFORM, Boolean.TRUE);
        }
        percent = ServletUtil.getBooleanParameter(pRequest, mRegionPercentParam, false);
        if (percent) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE_PERCENT, Boolean.TRUE);
        }

        super.doFilterImpl(pRequest, pResponse, pChain);
    }

    /**
     * This implementation does no filtering, and simply returns the image
     * passed in.
     *
     * @param pImage
     * @param pRequest
     * @param pResponse
     * @return {@code pImage}
     */
    protected RenderedImage doFilter(BufferedImage pImage, ServletRequest pRequest, ImageServletResponse pResponse) {
        return pImage;
    }
}