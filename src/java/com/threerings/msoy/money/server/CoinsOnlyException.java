//
// $Id$

package com.threerings.msoy.money.server;

/**
 * Indicates that an attempt to purchase an item with bars failed because only
 * a price for coins has been secured.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class CoinsOnlyException extends MoneyException
{
    public CoinsOnlyException (final int memberId, final int itemId, final int itemType)
    {
        super("Member " + memberId + " attempted to buy item " + itemId + " of type " + itemType +
            " with bars that was only secured for purchase with coins.");
        this.memberId = memberId;
        this.itemId = itemId;
        this.itemType = itemType;
    }

    public CoinsOnlyException (final int memberId, final int itemId, final int itemType, final Throwable cause)
    {
        super("Member " + memberId + " attempted to buy item " + itemId + " of type " + itemType +
            " with bars that was only secured for purchase with coins.", cause);
        this.memberId = memberId;
        this.itemId = itemId;
        this.itemType = itemType;
    }

    public int getMemberId ()
    {
        return memberId;
    }

    public int getItemId ()
    {
        return itemId;
    }

    public int getItemType ()
    {
        return itemType;
    }

    private final int memberId;
    private final int itemId;
    private final int itemType;
}
