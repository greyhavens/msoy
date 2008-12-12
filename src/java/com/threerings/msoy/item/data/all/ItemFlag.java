//
// $Id$

package com.threerings.msoy.item.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A flag on an item.
 */
public class ItemFlag
    implements IsSerializable
{
    /** Kinds of flags. */
    public enum Flag
        implements IsSerializable
    {
        MATURE, COPYRIGHT;
    }

    /** Item flagged. */
    public ItemIdent itemIdent;

    /** Id of flagging member. */
    public int memberId;

    /** Kind of flag. */
    public Flag flag;

    /** User-entered comment. */
    public String comment;

    /** Time the flag was set. */
    public Date timestamp;
}
