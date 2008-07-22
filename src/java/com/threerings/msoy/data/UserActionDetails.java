//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.item.data.all.Item;

/**
 * Container for all the situation-specific details of a user action.
 */
public class UserActionDetails
{
    /** Default value for {@link #otherMemberId} and {@link #itemId}. */
    public static final int INVALID_ID = -1;

    /** Enum describing the action type. */
    public final UserAction action;

    /** Id of the member object performing the action. */
    public final int memberId;

    /**
     * If this is an action between two members, id of the other member involved.
     * For unary actions, this defaults to {@link #INVALID_ID}.
     */
    public final int otherMemberId;

    /**
     * Type of the item involved in the user action.
     * If the action did not involve an item, defaults to {@link Item.NOT_A_TYPE}.
     */
    public final byte itemType;

    /**
     * Id of the item involved in the user action.
     * If the action did not involve an item, defaults to {@link #INVALID_ID}.
     */
    public final int itemId;

    /**
     * Optional other information about the action. Defaults to an empty string.
     */
    public final String misc;

    /**
     * Constructor for transactions involving an item and two members.
     */
    public UserActionDetails (
        int memberId, UserAction action, int otherMemberId, byte itemType, int itemId, String misc)
    {
        this.action = action;
        this.memberId = memberId;
        this.otherMemberId = otherMemberId;
        this.itemType = itemType;
        this.itemId = itemId;
        this.misc = misc;
    }

    /**
     * Constructor for transactions involving an item and two members.
     */
    public UserActionDetails (
        int memberId, UserAction action, int otherMemberId, byte itemType, int itemId)
    {
        this(memberId, action, otherMemberId, itemType, itemId, "");
    }

    /**
     * Constructor for an item-less action between two members.
     * Leaves item type and id at default values.
     */
    public UserActionDetails (int memberId, UserAction action, int otherMemberId)
    {
        this(memberId, action, otherMemberId, Item.NOT_A_TYPE, INVALID_ID);
    }

    /**
     * Constructor for a unary item action.
     * Leaves other member's id at its default value.
     */
    public UserActionDetails (int memberId, UserAction action, byte itemType, int itemId)
    {
        this(memberId, action, INVALID_ID, itemType, itemId);
    }

    /**
     * Constructor for a unary, item-less action.
     * Leaves action as null, and item type and id at default values.
     */
    public UserActionDetails (int memberId, UserAction action)
    {
        this(memberId, action, INVALID_ID, Item.NOT_A_TYPE, INVALID_ID);
    }

    @Override
    public String toString()
    {
        return "UserActionDetails [memberId=" + memberId + ", action=" + action +
            ", otherMemberId=" + otherMemberId + ", itemType=" + itemType +
            ", itemId=" + itemId + "]";
    }
}
