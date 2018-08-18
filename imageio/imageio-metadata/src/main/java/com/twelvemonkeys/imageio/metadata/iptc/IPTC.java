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

/**
 * IPTC
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: IPTC.java,v 1.0 Nov 11, 2009 6:20:21 PM haraldk Exp$
 */
public interface IPTC {
    int ENVELOPE_RECORD = 1 << 8;
    int APPLICATION_RECORD = 2 << 8;

    /** 1:05: Destination */
    int TAG_DESTINATION = ENVELOPE_RECORD | 5;
    /** 1:50: Product ID */
    int TAG_PRODUCT_ID = ENVELOPE_RECORD | 50;
    /** 1:90: Coded Character Set */
    int TAG_CODED_CHARACTER_SET = ENVELOPE_RECORD | 90;

    /** 2:00 Record Version (mandatory) */
    int TAG_RECORD_VERSION = APPLICATION_RECORD;       // 0x0200

    /** 2:03 Object Type Reference */
    int TAG_OBJECT_TYPE_REFERENCE = APPLICATION_RECORD | 3;
    /** 2:04 Object Attribute Reference (repeatable) */
    int TAG_OBJECT_ATTRIBUTE_REFERENCE = APPLICATION_RECORD | 4;
    /** 2:05 Object Name */
    int TAG_OBJECT_NAME = APPLICATION_RECORD | 5;   // 0x0205
    /** 2:07 Edit Status */
    int TAG_EDIT_STATUS = APPLICATION_RECORD | 7;
    /** 2:08 Editorial Update */
    int TAG_EDITORIAL_UPDATE = APPLICATION_RECORD | 8;
    /** 2:10 Urgency */
    int TAG_URGENCY = APPLICATION_RECORD | 10;
    /** 2:12 Subect Reference (repeatable) */
    int TAG_SUBJECT_REFERENCE = APPLICATION_RECORD | 12;
    /** 2:15 Category */
    int TAG_CATEGORY = APPLICATION_RECORD | 15;  // 0x020f
    /** 2:20 Supplemental Category (repeatable) */
    int TAG_SUPPLEMENTAL_CATEGORIES = APPLICATION_RECORD | 20;
    /** 2:22 Fixture Identifier */
    int TAG_FIXTURE_IDENTIFIER = APPLICATION_RECORD | 22;
    /** 2:25 Keywords (repeatable) */
    int TAG_KEYWORDS = APPLICATION_RECORD | 25;
    /** 2:26 Content Locataion Code (repeatable) */
    int TAG_CONTENT_LOCATION_CODE = APPLICATION_RECORD | 26;
    /** 2:27 Content Locataion Name (repeatable) */
    int TAG_CONTENT_LOCATION_NAME = APPLICATION_RECORD | 27;
    /** 2:30 Release Date */
    int TAG_RELEASE_DATE = APPLICATION_RECORD | 30;
    /** 2:35 Release Time */
    int TAG_RELEASE_TIME = APPLICATION_RECORD | 35;
    /** 2:37 Expiration Date */
    int TAG_EXPIRATION_DATE = APPLICATION_RECORD | 37;
    /** 2:38 Expiration Time */
    int TAG_EXPIRATION_TIME = APPLICATION_RECORD | 38;
    /** 2:40 Special Instructions */
    int TAG_SPECIAL_INSTRUCTIONS = APPLICATION_RECORD | 40;  // 0x0228
    /** 2:42 Action Advised (1: Kill, 2: Replace, 3: Append, 4: Reference) */
    int TAG_ACTION_ADVICED = APPLICATION_RECORD | 42;
    /** 2:45 Reference Service (repeatable in triplets with 2:47 and 2:50) */
    int TAG_REFERENCE_SERVICE = APPLICATION_RECORD | 45;
    /** 2:47 Reference Date (mandatory if 2:45 present) */
    int TAG_REFERENCE_DATE = APPLICATION_RECORD | 47;
    /** 2:50 Reference Number (mandatory if 2:45 present) */
    int TAG_REFERENCE_NUMBER = APPLICATION_RECORD | 50;
    /** 2:55 Date Created */
    int TAG_DATE_CREATED = APPLICATION_RECORD | 55; // 0x0237
    /** 2:60 Time Created */
    int TAG_TIME_CREATED = APPLICATION_RECORD | 60;
    /** 2:62 Digital Creation Date */
    int TAG_DIGITAL_CREATION_DATE = APPLICATION_RECORD | 62;
    /** 2:63 Digital Creation Date */
    int TAG_DIGITAL_CREATION_TIME = APPLICATION_RECORD | 63;
    /** 2:65 Originating Program */
    int TAG_ORIGINATING_PROGRAM = APPLICATION_RECORD | 65;
    /** 2:70 Program Version (only valid if 2:65 present) */
    int TAG_PROGRAM_VERSION = APPLICATION_RECORD | 70;
    /** 2:75 Object Cycle (a: morning, p: evening, b: both) */
    int TAG_OBJECT_CYCLE = APPLICATION_RECORD | 75;
    /** 2:80 By-line (repeatable) */
    int TAG_BY_LINE = APPLICATION_RECORD | 80;  // 0x0250
    /** 2:85 By-line Title (repeatable) */
    int TAG_BY_LINE_TITLE = APPLICATION_RECORD | 85;  // 0x0255
    /** 2:90 City */
    int TAG_CITY = APPLICATION_RECORD | 90;  // 0x025a
    /** 2:92 Sub-location */
    int TAG_SUB_LOCATION = APPLICATION_RECORD | 92;
    /** 2:95 Province/State */
    int TAG_PROVINCE_OR_STATE = APPLICATION_RECORD | 95;  // 0x025f
    /** 2:100 Country/Primary Location Code */
    int TAG_COUNTRY_OR_PRIMARY_LOCATION_CODE = APPLICATION_RECORD | 100;
    /** 2:101 Country/Primary Location Name */
    int TAG_COUNTRY_OR_PRIMARY_LOCATION = APPLICATION_RECORD | 101; // 0x0265
    /** 2:103 Original Transmission Reference */
    int TAG_ORIGINAL_TRANSMISSION_REFERENCE = APPLICATION_RECORD | 103; // 0x0267
    /** 2:105 Headline */
    int TAG_HEADLINE = APPLICATION_RECORD | 105; // 0x0269
    /** 2:110 Credit */
    int TAG_CREDIT = APPLICATION_RECORD | 110; // 0x026e
    /** 2:115 Source */
    int TAG_SOURCE = APPLICATION_RECORD | 115; // 0x0273
    /** 2:116 Copyright Notice */
    int TAG_COPYRIGHT_NOTICE = APPLICATION_RECORD | 116; // 0x0274
    /** 2:118 Contact */
    int TAG_CONTACT = APPLICATION_RECORD | 118;
    /** 2:120 Catption/Abstract */
    int TAG_CAPTION = APPLICATION_RECORD | 120; // 0x0278
    /** 2:122 Writer/Editor (repeatable) */
    int TAG_WRITER = APPLICATION_RECORD | 122; // 0x027a
    /** 2:125 Rasterized Caption (binary data) */
    int TAG_RASTERIZED_CATPTION = APPLICATION_RECORD | 125;
    /** 2:130 Image Type */
    int TAG_IMAGE_TYPE = APPLICATION_RECORD | 130;
    /** 2:131 Image Orientation */
    int TAG_IMAGE_ORIENTATION = APPLICATION_RECORD | 131;
    /** 2:135 Language Identifier */
    int TAG_LANGUAGE_IDENTIFIER = APPLICATION_RECORD | 135;

    // TODO: 2:150-2:154 Audio

    // TODO: Should we expose this field?
    /**
     * 2:199 JobMinder Assignment Data (Custom IPTC field).
     * A common custom IPTC field used by a now discontinued application called JobMinder.
     *
     * @see <a href="http://www.jobminder.net/">JobMinder Homepage</a>
     */
    int CUSTOM_TAG_JOBMINDER_ASSIGNMENT_DATA = APPLICATION_RECORD | 199;

    // TODO: Other custom fields in 155-200 range?

    // TODO: 2:200-2:202 Object Preview Data

    final class Tags {
        static boolean isArray(final short tagId) {
            switch (tagId) {
                case IPTC.TAG_DESTINATION:
                case IPTC.TAG_PRODUCT_ID:
                case IPTC.TAG_SUBJECT_REFERENCE:
                case IPTC.TAG_SUPPLEMENTAL_CATEGORIES:
                case IPTC.TAG_KEYWORDS:
                case IPTC.TAG_CONTENT_LOCATION_CODE:
                case IPTC.TAG_CONTENT_LOCATION_NAME:
                case IPTC.TAG_BY_LINE:
                    return true;

                default:
                    return false;
            }
        }
    }
}
