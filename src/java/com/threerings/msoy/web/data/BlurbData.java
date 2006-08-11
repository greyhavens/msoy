//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains metadata for a particular blurb on a member's personal page.
 */
public class BlurbData implements IsSerializable
{
    /** The type code for a profile blurb. */
    public static final int PROFILE = 0;

    /** The type code for a friends blurb. */
    public static final int FRIENDS = 1;

    /** Indicates which kind of blurb this is. */
    public int type;

    /** Used to distinguish multiple copies of the same type of blurb on a
     * page; identifies special content for this blurb. */
    public int blurbId;

    /** Arbitrary layout information interpreted by the layout code. */
    public String layoutData;
}
