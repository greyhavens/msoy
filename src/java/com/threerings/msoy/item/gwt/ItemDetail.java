//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * This class supplies detailed information for an item, some of which is relative to a given
 * member.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly limited in their contents
 * as they must be translatable into JavaScript ({@link IsSerializable}) and must work with the
 * Presents streaming system ({@link Streamable}).
 */
public class ItemDetail implements Streamable, IsSerializable
{
    /** The Item of which we're a Detail. */
    public Item item;

    /** A display-friendly expansion of Item.creatorId. */
    public MemberCard creator;

    /** If non-null, contains the String name for the usage location specified in the Item. */
    public String useLocation;

    /** Contains member rating and favorite information about the item. */
    public MemberItemInfo memberItemInfo;

    /** The tags on this item. */
    public List<String> tags;

    /** Encoded memories, or null if not applicable. */
    public String memories;

    /** If requested, the theme groups that have stamped this item. */
    public List<GroupName> themes;
}
