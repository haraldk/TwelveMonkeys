/*
 * Copyright (c) 2014, Harald Kuhr
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

package com.twelvemonkeys.imageio.path;

import static com.twelvemonkeys.lang.Validate.isTrue;

/**
 * Adobe path segment.
 *
 * @see <a href="http://www.adobe.com/devnet-apps/photoshop/fileformatashtml/#50577409_17587">Adobe Photoshop Path resource format</a>
 * @author <a href="mailto:jpalmer@itemmaster.com">Jason Palmer, itemMaster LLC</a>
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
*/
final class AdobePathSegment {
    static final int CLOSED_SUBPATH_LENGTH_RECORD = 0;
    static final int CLOSED_SUBPATH_BEZIER_LINKED = 1;
    static final int CLOSED_SUBPATH_BEZIER_UNLINKED = 2;
    static final int OPEN_SUBPATH_LENGTH_RECORD = 3;
    static final int OPEN_SUBPATH_BEZIER_LINKED = 4;
    static final int OPEN_SUBPATH_BEZIER_UNLINKED = 5;
    static final int PATH_FILL_RULE_RECORD = 6;
    static final int CLIPBOARD_RECORD = 7;
    static final int INITIAL_FILL_RULE_RECORD = 8;

    static final String[] SELECTOR_NAMES = {
            "Closed subpath length record",
            "Closed subpath Bezier knot, linked",
            "Closed subpath Bezier knot, unlinked",
            "Open subpath length record",
            "Open subpath Bezier knot, linked",
            "Open subpath Bezier knot, unlinked",
            "Path fill rule record",
            "Clipboard record",
            "Initial fill rule record"
    };

    final int selector;
    final int length;

    // TODO: Consider keeping these in 8.24FP format
    // Control point preceding knot
    final double cppy;
    final double cppx;

    // Anchor point
    final double apy;
    final double apx;

    // Control point leaving knot
    final double cply;
    final double cplx;

    AdobePathSegment(final int selector,
                     final double cppy, final double cppx,
                     final double apy, final double apx,
                     final double cply, final double cplx) {
        this(selector, -1, cppy, cppx, apy, apx, cply, cplx);
    }

    AdobePathSegment(int fillRuleSelector) {
        this(isTrue(fillRuleSelector == PATH_FILL_RULE_RECORD, fillRuleSelector, "Expected fill rule record (6): %s"),
             0, -1, -1, -1, -1, -1, -1);
    }

    AdobePathSegment(final int lengthSelector, final int length) {
        this(isTrue(lengthSelector == CLOSED_SUBPATH_LENGTH_RECORD || lengthSelector == OPEN_SUBPATH_LENGTH_RECORD, lengthSelector, "Expected path length record (0 or 3): %s"),
             length,
             -1, -1, -1, -1, -1, -1);
    }

    private AdobePathSegment(final int selector, final int length,
                             final double cppy, final double cppx,
                             final double apy, final double apx,
                             final double cply, final double cplx) {
        // Validate selector, size and points
        switch (selector) {
            case CLOSED_SUBPATH_LENGTH_RECORD:
            case OPEN_SUBPATH_LENGTH_RECORD:
                isTrue(length >= 0, length, "Expected positive length: %d");
                break;
            case CLOSED_SUBPATH_BEZIER_LINKED:
            case CLOSED_SUBPATH_BEZIER_UNLINKED:
            case OPEN_SUBPATH_BEZIER_LINKED:
            case OPEN_SUBPATH_BEZIER_UNLINKED:
                isTrue(
                        cppx >= 0 && cppx <= 1 && cppy >= 0 && cppy <= 1,
                        String.format("Expected point in range [0...1]: (%f, %f)", cppx ,cppy)
                );
                break;
            case PATH_FILL_RULE_RECORD:
            case CLIPBOARD_RECORD:
            case INITIAL_FILL_RULE_RECORD:
                break;
            default:
                throw new IllegalArgumentException("Unknown selector: " + selector);
        }

        this.selector = selector;
        this.length = length;
        this.cppy = cppy;
        this.cppx = cppx;
        this.apy = apy;
        this.apx = apx;
        this.cply = cply;
        this.cplx = cplx;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AdobePathSegment that = (AdobePathSegment) other;

        return Double.compare(that.apx, apx) == 0
                && Double.compare(that.apy, apy) == 0
                && Double.compare(that.cplx, cplx) == 0
                && Double.compare(that.cply, cply) == 0
                && Double.compare(that.cppx, cppx) == 0
                && Double.compare(that.cppy, cppy) == 0
                && selector == that.selector
                && length == that.length;

    }

    @Override
    public int hashCode() {
        long tempBits;

        int result = selector;
        result = 31 * result + length;
        tempBits = Double.doubleToLongBits(cppy);
        result = 31 * result + (int) (tempBits ^ (tempBits >>> 32));
        tempBits = Double.doubleToLongBits(cppx);
        result = 31 * result + (int) (tempBits ^ (tempBits >>> 32));
        tempBits = Double.doubleToLongBits(apy);
        result = 31 * result + (int) (tempBits ^ (tempBits >>> 32));
        tempBits = Double.doubleToLongBits(apx);
        result = 31 * result + (int) (tempBits ^ (tempBits >>> 32));
        tempBits = Double.doubleToLongBits(cply);
        result = 31 * result + (int) (tempBits ^ (tempBits >>> 32));
        tempBits = Double.doubleToLongBits(cplx);
        result = 31 * result + (int) (tempBits ^ (tempBits >>> 32));

        return result;
    }

    @Override
    public String toString() {
        switch (selector) {
            case INITIAL_FILL_RULE_RECORD:
            case PATH_FILL_RULE_RECORD:
                return String.format("Rule(selector=%s, rule=%d)", SELECTOR_NAMES[selector], length);
            case CLOSED_SUBPATH_LENGTH_RECORD:
            case OPEN_SUBPATH_LENGTH_RECORD:
                return String.format("Len(selector=%s, length=%d)", SELECTOR_NAMES[selector], length);
            default:
                // fall-through
        }

        return String.format("Pt(pre=(%.3f, %.3f), knot=(%.3f, %.3f), post=(%.3f, %.3f), selector=%s)", cppx, cppy, apx, apy, cplx, cply, SELECTOR_NAMES[selector]);
    }
}
