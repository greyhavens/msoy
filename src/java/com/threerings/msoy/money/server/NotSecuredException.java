//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Occurs when a member attempts to purchase an item for which they have not secured a price.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotSecuredException extends MoneyException
{
    public NotSecuredException (final int memberId, final ItemIdent item)
    {
        super("The price of the item " + item + " was not secured previously by member "
            + memberId);
        _memberId = memberId;
        _item = item;
    }

    public NotSecuredException (final int memberId, final ItemIdent item, final Throwable cause)
    {
        super("The price of the item " + item + " was not secured previously by member "
            + memberId, cause);
        _memberId = memberId;
        _item = item;
    }

    public int getMemberId ()
    {
        return _memberId;
    }

    public ItemIdent getItem ()
    {
        return _item;
    }

    private final int _memberId;
    private final ItemIdent _item;
}
