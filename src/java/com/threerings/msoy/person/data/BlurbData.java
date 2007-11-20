//
// $Id$

package com.threerings.msoy.person.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains metadata for a particular blurb on a member's personal page.
 */
public class BlurbData implements IsSerializable
{
    /** A tiny class to wrap a failure reason during blurb resolution. */
    public static class ResolutionFailure implements IsSerializable
    {
        public String cause;

        public ResolutionFailure () {
        }

        public ResolutionFailure (String cause) {
            this.cause = cause;
        }
    }

    /** The type code for a profile blurb. */
    public static final int PROFILE = 0;

    /** The type code for a friends blurb. */
    public static final int FRIENDS = 1;

    /** The type code for a groups blurb. */
    public static final int GROUPS = 2;

    /** The type code for the ratings blurb. */
    public static final int RATINGS = 3;

    /** The type code for the trophies blurb. */
    public static final int TROPHIES = 4;

    /** Indicates which kind of blurb this is. */
    public int type;

    /** Used to distinguish multiple copies of the same type of blurb on a page; identifies special
     * content for this blurb. */
    public int blurbId;

    /** Arbitrary layout information interpreted by the layout code. */
    public String layoutData;

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return "[t=" + type + ", id=" + blurbId + ", ldata=" + layoutData + "]";
    }
}
