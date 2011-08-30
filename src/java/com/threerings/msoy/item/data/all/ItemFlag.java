//
// $Id$

package com.threerings.msoy.item.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.util.ByteEnum;

/**
 * A flag on an item.
 */
public class ItemFlag
    implements IsSerializable
{
    /** Kinds of flags. */
    public enum Kind
        implements IsSerializable, ByteEnum
    {
        MATURE(0), COPYRIGHT(1), STOLEN(2), UNATTRIBUTED(3), SCAM(4), BROKEN(5);

        // from ByteEnum
        public byte toByte ()
        {
            return _value;
        }

        Kind (int value)
        {
            _value = (byte)value;
        }

        protected byte _value;
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
