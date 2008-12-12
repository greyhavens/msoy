//
// $Id$

package com.threerings.msoy.item.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.depot.ByteEnum;

/**
 * A flag on an item.
 */
public class ItemFlag
    implements IsSerializable
{
    /** Kinds of flags. */
    public enum Flag
        implements ByteEnum, IsSerializable
    {
        MATURE, COPYRIGHT;

        public byte toByte () {
            return (byte)ordinal();
        }

        public static Flag fromByte (byte byteVal) {
            return values()[byteVal];
        }
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
