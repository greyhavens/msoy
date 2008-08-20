//
// $Id$

package com.threerings.msoy.item.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Holds common info about what a particular member thinks of an item.
 *
 * @author mjensen
 */
public class MemberItemInfo implements Streamable, IsSerializable
{
    /** The item's rating given by a member. */
    public byte memberRating;

    /** Indicates whether this is one of the member's favorite items. */
    public boolean favorite;
}
