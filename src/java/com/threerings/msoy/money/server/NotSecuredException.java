//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * Occurs when a member attempts to purchase an item for which they have not secured a price.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotSecuredException extends MoneyException
{
    public NotSecuredException (final int memberId, final CatalogIdent item, final PriceQuote quote)
    {
        super("The price of the item " + item + " was not secured previously by member "
            + memberId);
        _memberId = memberId;
        _item = item;
        _quote = quote;
    }

    public NotSecuredException (final int memberId, final CatalogIdent item,
        final PriceQuote quote, final Throwable cause)
    {
        this(memberId, item, quote);
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

    public PriceQuote getQuote ()
    {
        return _quote;
    }

    protected final int _memberId;
    protected final CatalogIdent _item;
    protected final PriceQuote _quote;
}
