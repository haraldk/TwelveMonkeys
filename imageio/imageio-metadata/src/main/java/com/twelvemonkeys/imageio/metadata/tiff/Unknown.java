package com.twelvemonkeys.imageio.metadata.tiff;

/**
 * Unknown
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: Unknown.java,v 1.0 Oct 8, 2010 3:38:45 PM haraldk Exp$
 * @see <a href="http://en.wikipedia.org/wiki/There_are_known_knowns">We also know there are known unknowns</a>
 */
final class Unknown {
    private final short type;
    private final int count;
    private final long pos;

    public Unknown(final short type, final int count, final long pos) {
        this.type = type;
        this.count = count;
        this.pos = pos;
    }

    @Override
    public int hashCode() {
        return (int) (pos ^ (pos >>> 32)) + count * 37 + type * 97;
    }

    @Override
    public boolean equals(final Object other) {
        if (other != null && other.getClass() == getClass()) {
            Unknown unknown = (Unknown) other;
            return pos == unknown.pos && type == unknown.type && count == unknown.count;
        }

        return false;
    }

    @Override
    public String toString() {
        if (count == 1) {
            return String.format("Unknown(%d)@%08x", type, pos);
        }
        else {
            return String.format("Unknown(%d)[%d]@%08x", type, count, pos);
        }
    }
}
