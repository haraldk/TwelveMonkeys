/*
 * Copyright (c) 2009, Harald Kuhr
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

package com.twelvemonkeys.servlet.image;

import com.twelvemonkeys.servlet.ServletUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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