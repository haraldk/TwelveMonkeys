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
 * @version $Id: SourceRenderFilter.java#1 $
 */
public class SourceRenderFilter extends ImageFilter {
    private String sizeWidthParam = "size.w";
    private String sizeHeightParam = "size.h";
    private String sizePercentParam = "size.percent";
    private String sizeUniformParam = "size.uniform";

    private String regionWidthParam = "aoi.w";
    private String regionHeightParam = "aoi.h";
    private String regionLeftParam = "aoi.x";
    private String regionTopParam = "aoi.y";
    private String regionPercentParam = "aoi.percent";
    private String regionUniformParam = "aoi.uniform";

    public void setRegionHeightParam(String pRegionHeightParam) {
        regionHeightParam = pRegionHeightParam;
    }

    public void setRegionWidthParam(String pRegionWidthParam) {
        regionWidthParam = pRegionWidthParam;
    }

    public void setRegionLeftParam(String pRegionLeftParam) {
        regionLeftParam = pRegionLeftParam;
    }

    public void setRegionTopParam(String pRegionTopParam) {
        regionTopParam = pRegionTopParam;
    }

    public void setSizeHeightParam(String pSizeHeightParam) {
        sizeHeightParam = pSizeHeightParam;
    }

    public void setSizeWidthParam(String pSizeWidthParam) {
        sizeWidthParam = pSizeWidthParam;
    }

    public void setRegionPercentParam(String pRegionPercentParam) {
        regionPercentParam = pRegionPercentParam;
    }

    public void setRegionUniformParam(String pRegionUniformParam) {
        regionUniformParam = pRegionUniformParam;
    }

    public void setSizePercentParam(String pSizePercentParam) {
        sizePercentParam = pSizePercentParam;
    }

    public void setSizeUniformParam(String pSizeUniformParam) {
        sizeUniformParam = pSizeUniformParam;
    }

    public void init() throws ServletException {
        if (triggerParams == null) {
            // Add all params as triggers
            triggerParams = new String[]{sizeWidthParam, sizeHeightParam,
                    sizeUniformParam, sizePercentParam,
                    regionLeftParam, regionTopParam,
                    regionWidthParam, regionHeightParam,
                    regionUniformParam, regionPercentParam};
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
        int width = ServletUtil.getIntParameter(pRequest, sizeWidthParam, -1);
        int height = ServletUtil.getIntParameter(pRequest, sizeHeightParam, -1);
        if (width > 0 || height > 0) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE, new Dimension(width, height));
        }

        // Size uniform/percent
        boolean uniform = ServletUtil.getBooleanParameter(pRequest, sizeUniformParam, true);
        if (!uniform) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE_UNIFORM, Boolean.FALSE);
        }
        boolean percent = ServletUtil.getBooleanParameter(pRequest, sizePercentParam, false);
        if (percent) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE_PERCENT, Boolean.TRUE);
        }

        // Area of interest parameters
        int x = ServletUtil.getIntParameter(pRequest, regionLeftParam, -1); // Default is center
        int y = ServletUtil.getIntParameter(pRequest, regionTopParam, -1);  // Default is center
        width = ServletUtil.getIntParameter(pRequest, regionWidthParam, -1);
        height = ServletUtil.getIntParameter(pRequest, regionHeightParam, -1);
        if (width > 0 || height > 0) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_AOI, new Rectangle(x, y, width, height));
        }

        // AOI uniform/percent
        uniform = ServletUtil.getBooleanParameter(pRequest, regionUniformParam, false);
        if (uniform) {
            pRequest.setAttribute(ImageServletResponse.ATTRIB_SIZE_UNIFORM, Boolean.TRUE);
        }
        percent = ServletUtil.getBooleanParameter(pRequest, regionPercentParam, false);
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