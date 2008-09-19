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
    CREATED_PROFILE(1, "m.created_profile"),
    UPDATED_PROFILE(2),
    CREATED_ACCOUNT(3, "m.created_account"),

    SENT_FRIEND_INVITE(10),
    ACCEPTED_FRIEND_INVITE(11),
    TRANSFER_FROM_GUEST(12, "m.transfer_from_guest"),
    INVITED_FRIEND_JOINED(13, "m.invited_friend_joined"),

    PLAYED_GAME(20, "m.played_game"),
    COMPLETED_QUEST(21, "m.completed_quest"),

    CREATED_ITEM(30),
    BOUGHT_ITEM(31),
    LISTED_ITEM(32),
    RETURNED_ITEM(33),
    RECEIVED_PAYOUT(34),
    UPDATED_LISTING(35),
    UPDATED_PRICING(36),

    EARNED_BADGE(40, "m.earned_badge"),
    
    BOUGHT_BARS(50, "m.bought_bars");

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

    /**
     * Get the message key that can be used to describe this action.
     */
    public String getMessage ()
    {
        return _message;
    }

    UserAction (final int num)
    {
        this(num, "m.unknown");
    }

    UserAction (final int num, final String message)
    {
        _num = num;
        _message = message;
    }

    protected final int _num;
    protected final String _message;

    protected static IntMap<UserAction> _reverse;
    static {
        _reverse = new HashIntMap<UserAction>();
        for (final UserAction type : UserAction.values()) {
            _reverse.put(type.getNumber(), type);
        }
    }
}
