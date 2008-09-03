//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.item.data.all.CatalogIdent;

/**
 * Occurs when a member attempts to purchase an item for which they have not secured a price.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotSecuredException extends MoneyException
{
    public NotSecuredException (final int memberId, final CatalogIdent item)
    {
        super("The price of the item " + item + " was not secured previously by member "
            + memberId);
        _memberId = memberId;
        _item = item;
    }

    public NotSecuredException (final int memberId, final CatalogIdent item, final Throwable cause)
    {
        this(memberId, item);
        initCause(cause);
    }

    public int getMemberId ()
    {
        return _memberId;
    }

    public CatalogIdent getItem ()
    {
        return _item;
    }

    private final int _memberId;
    private final CatalogIdent _item;
}
