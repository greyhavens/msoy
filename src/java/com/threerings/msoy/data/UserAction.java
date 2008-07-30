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
    CREATED_PROFILE(1, 500),
    UPDATED_PROFILE(2, 0),
    CREATED_ACCOUNT(3, 1000),

    SENT_FRIEND_INVITE(10, 0),
    ACCEPTED_FRIEND_INVITE(11, 0),
    TRANSFER_FROM_GUEST(12, 0),
    INVITED_FRIEND_JOINED(13, 1000),

    PLAYED_GAME(20, 0),
    COMPLETED_QUEST(21, 0),

    CREATED_ITEM(30, 100),
    BOUGHT_ITEM(31, 0),
    LISTED_ITEM(32, 0),
    RETURNED_ITEM(33, 0),
    RECEIVED_PAYOUT(34, 0),
    UPDATED_LISTING(35, 0),
    UPDATED_PRICING(36, 0),

    EARNED_BADGE(40, 0),

    ;

    /**
     * Look up an {@link UserAction} by its numerical representation and return it.
     */
    public static UserAction getActionByNumber (int num)
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

    /**
     * Returns the amount of flow granted when this action is performed.
     */
    public int getFlow ()
    {
        return _flow;
    }

    UserAction (int num, int flow)
    {
        _num = num;
        _flow = flow;
    }

    protected final int _num;
    protected final int _flow;

    protected static IntMap<UserAction> _reverse;
    static {
        _reverse = new HashIntMap<UserAction>();
        for (UserAction type : UserAction.values()) {
            _reverse.put(type.getNumber(), type);
        }
    }
}
