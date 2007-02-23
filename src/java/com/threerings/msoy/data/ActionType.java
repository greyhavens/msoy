package com.threerings.msoy.data;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;

/**
 * Represent an action taken by a user; used in logs for humanity assessment and
 * conversion analysis purposes.
 */
public enum ActionType
{
    CreatedProfile(1),
    UpdatedProfile(2),
    
    SentFriendInvite(10),
    AcceptedFriendInvite(11),
    
    PlayedGame(20),
    
    CreatedItem(30),
    BoughtItem(31),
    ListedItem(32);

    /**
     * Look up an {@link ActionType} by its numerical representation and return it.
     */
    public static ActionType getActionByNumber (int num)
    {
        return _reverse.get(num);
    }

    /**
     * Fetch the numerical representation of this {@link ActionType}.
     */
    public int getNumber ()
    {
        return _num;
    }
    
    ActionType (int num)
    {
        _num = num;
    }

    protected static IntMap<ActionType> _reverse;
    static {
        _reverse = new HashIntMap<ActionType>();
        for (ActionType type : ActionType.values()) {
            _reverse.put(type.getNumber(), type);
        }
    }

    protected int _num;
}
