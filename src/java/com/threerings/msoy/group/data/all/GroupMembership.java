//
// $Id$

package com.threerings.msoy.group.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.depot.ByteEnum;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.all.GroupName;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, IsSerializable, DSet.Entry
{
    /** Ranks assigned to group members. */
    public enum Rank implements ByteEnum
    {
        /** Unused rank code. This is not ever stored in a GroupMembership record, but is useful
         * for methods that return a user's rank as a byte. */
        NON_MEMBER((byte)0),

        /** Rank code for a member. */
        MEMBER((byte)1),

        /** Rank code for a manager. */
        MANAGER((byte)2);

        // from ByteEnum
        public byte toByte () {
            return _value;
        }

        /**
         * Translates a persisted value back to an instance, for depot.
         */
        public static Rank fromByte (byte b) {
            for (Rank r : values()) {
                if (r._value == b) {
                    return r;
                }
            }
            throw new IllegalArgumentException("Rank not found for value " + b);
        }

        Rank (byte value)
        {
            _value = value;
        }

        protected byte _value;
    }

    /** The group's identity. */
    public GroupName group;

    /** This member's rank in the group. */
    public Rank rank;

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return group.getGroupId();
    }
}
