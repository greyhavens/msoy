//
// $Id$

package com.threerings.msoy.item.web;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * One entry in the 1-to-1 mapping between tags and tag ID's.
 */
public class Tag
    implements Streamable, IsSerializable
{
    /** This tag's id. */
    public int tagId;

    /** The actual tag, a string. */
    public String tag;
}
