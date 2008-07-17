//
// $Id$

package com.threerings.msoy.money.server;

/**
 * Occurs when a member attempts to purchase an item for which they have not secured a price.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotSecuredException extends MoneyException
{
    public NotSecuredException (final int memberId, final int itemId, final int itemType)
    {
        super("The price of the item " + itemId + " of type " + itemType + 
            " was not secured previously by member " + memberId);
        this.memberId = memberId;
        this.itemId = itemId;
        this.itemType = itemType;
    }
    
    public NotSecuredException (final int memberId, final int itemId, final int itemType, final Throwable cause)
    {
        super("The price of the item " + itemId + " of type " + itemType + 
            " was not secured previously by member " + memberId, cause);
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
