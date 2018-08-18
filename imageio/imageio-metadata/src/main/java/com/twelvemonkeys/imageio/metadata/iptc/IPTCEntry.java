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

package com.twelvemonkeys.imageio.metadata.iptc;

import com.twelvemonkeys.imageio.metadata.AbstractEntry;

/**
* IPTCEntry
*
* @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
* @author last modified by $Author: haraldk$
* @version $Id: IPTCEntry.java,v 1.0 Nov 13, 2009 8:57:04 PM haraldk Exp$
*/
class IPTCEntry extends AbstractEntry {
    public IPTCEntry(final int tagId, final Object value) {
        super(tagId, value);
    }

    @Override
    public String getFieldName() {
        switch ((Integer) getIdentifier()) {
            case IPTC.TAG_RECORD_VERSION:
                return "RecordVersion";
            case IPTC.TAG_KEYWORDS:
                return "Keywords";
            case IPTC.TAG_SPECIAL_INSTRUCTIONS:
                return "Instructions";
            case IPTC.TAG_DIGITAL_CREATION_DATE:
                return "DigitalCreationDate";
            case IPTC.TAG_DIGITAL_CREATION_TIME:
                return "DigitalCreationTime";
            case IPTC.TAG_DATE_CREATED:
                return "DateCreated";
            case IPTC.TAG_TIME_CREATED:
                return "TimeCreated";
            case IPTC.TAG_BY_LINE_TITLE:
                return "ByLineTitle";
            case IPTC.TAG_CITY:
                return "City";
            case IPTC.TAG_SUB_LOCATION:
                return "SubLocation";
            case IPTC.TAG_PROVINCE_OR_STATE:
                return "StateProvince";
            case IPTC.TAG_COUNTRY_OR_PRIMARY_LOCATION_CODE:
                return "CountryCode";
            case IPTC.TAG_COUNTRY_OR_PRIMARY_LOCATION:
                return "Country";
            case IPTC.TAG_SOURCE:
                return "Source";
            case IPTC.TAG_CAPTION:
                return "Caption";
            case IPTC.TAG_COPYRIGHT_NOTICE:
                return "CopyrightNotice";
            case IPTC.TAG_BY_LINE:
                return "ByLine";
            // TODO: More tags...
        }

        return null;
    }

    @Override
    protected String getNativeIdentifier() {
        int identifier = (Integer) getIdentifier();
        return String.format("%d:%02d", identifier >> 8, identifier & 0xff);
    }
}
