//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

import com.threerings.msoy.web.data.MemberName;

import com.threerings.msoy.item.web.ItemIdent;

/**
 * Keeps a history of tagging events for a given item or group.
 */
public class TagHistory
    implements Streamable, IsSerializable
{
    public static final byte ACTION_ADDED = 1;
    public static final byte ACTION_REMOVED = 2;
    public static final byte ACTION_COPIED = 3;
    
    /** The item being operated on. Either this or groupId will be non-null */
    public ItemIdent item;

    /** The group id being operated on.  Either this or item will be non-null */
    public int groupId;

    /** The tag that was added or deleted, or null for COPIED. */
    public String tag;
    
    /** The member who added or deleted the tag. */
    public MemberName member;
    
    /** The action taken (ADDED or REMOVED or COPIED). */
    public byte action;

    /** The time of the tagging event. */
    public Date time;

    /** Returns the id as used by TagRepository for this TagHistory */
    public int getId ()
    {
        return item == null ? groupId : item.itemId;
    }
}
