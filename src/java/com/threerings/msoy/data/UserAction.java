//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.threerings.util.ActionScript;

/**
 * Represent an action taken by a user; used in logs for humanity assessment and conversion
 * analysis purposes.
 */
@ActionScript(omit=true)
public enum UserAction
{
    CREATED_PROFILE(1),
    UPDATED_PROFILE(2),
    CREATED_ACCOUNT(3),

    SENT_FRIEND_INVITE(10),
    ACCEPTED_FRIEND_INVITE(11),
    TRANSFER_FROM_GUEST(12),
    INVITED_FRIEND_JOINED(13),

    PLAYED_GAME(20),
    COMPLETED_QUEST(21),

    CREATED_ITEM(30),
    BOUGHT_ITEM(31),
    LISTED_ITEM(32),
    RETURNED_ITEM(33),
    RECEIVED_PAYOUT(34),
    UPDATED_LISTING(35),
    UPDATED_PRICING(36),

    EARNED_BADGE(40),
    
    BOUGHT_BARS(50);

    /**
     * Look up an {@link UserAction} by its numerical representation and return it.
     */
    public static UserAction getActionByNumber (final int num)
    {
        return _reverse.get(num);
    }

    /**
     * Fetch the numerical representation of this {@link UserAction}.
     */
    public int getNumber ()
    {
        return _num;
    }

    UserAction (final int num)
    {
        _num = num;
    }

    protected final int _num;

    protected static IntMap<UserAction> _reverse;
    static {
        _reverse = new HashIntMap<UserAction>();
        for (final UserAction type : UserAction.values()) {
            _reverse.put(type.getNumber(), type);
        }
    }
}
