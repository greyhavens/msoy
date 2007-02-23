package com.threerings.msoy.data;

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
        switch(num) {
        case 1:
            return CreatedProfile;
        case 2:
            return UpdatedProfile;
        case 10:
            return AcceptedFriendInvite;
        case 20:
            return PlayedGame;
        case 30:
            return CreatedItem;
        case 31:
            return BoughtItem;
        case 32:
            return ListedItem;
        }
        return null;
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

    protected int _num;
}
