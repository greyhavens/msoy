package com.threerings.msoy.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;

/**
 * Represent an action taken by a user; used in logs for humanity assessment and
 * conversion analysis purposes.
 */
public enum UserAction
{
    CREATED_PROFILE(1),
    UPDATED_PROFILE(2),
    
    SENT_FRIEND_INVITE(10),
    ACCEPTED_FRIEND_INVITE(11),
    
    PLAYED_GAME(20),
    
    CREATED_ITEM(30),
    BOUGHT_ITEM(31),
    LISTED_ITEM(32);

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
    
    UserAction (int num)
    {
        _num = num;
    }

    protected static IntMap<UserAction> _reverse;
    static {
        _reverse = new HashIntMap<UserAction>();
        for (UserAction type : UserAction.values()) {
            _reverse.put(type.getNumber(), type);
        }
    }

    protected int _num;
}
