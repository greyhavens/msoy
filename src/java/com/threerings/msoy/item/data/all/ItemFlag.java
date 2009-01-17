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
    public enum Kind
        implements IsSerializable
    {
        MATURE, COPYRIGHT, STOLEN;
    }

    /** Item flagged. */
    public ItemIdent itemIdent;

    /** Id of flagging member. */
    public int memberId;

    /** Kind of flag. */
    public Kind kind;

    /** User-entered comment. */
    public String comment;

    /** Time the flag was set. */
    public Date timestamp;
}
