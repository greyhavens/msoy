//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.money.data.all.PriceQuote;

import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.money.gwt.CostUpdatedException;

/**
 * Occurs when a member attempts to purchase an item for which they have not secured a price.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotSecuredException extends MoneyException
{
    public NotSecuredException (final int memberId, final Object wareKey, final PriceQuote quote)
    {
        super("The price of the item " + wareKey + " was not secured previously by member "
            + memberId);
        _memberId = memberId;
        _wareKey = wareKey;
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

    public Object getWareKey ()
    {
        return _wareKey;
    }

    public PriceQuote getQuote ()
    {
        return _quote;
    }

    @Override
    public ServiceException toServiceException ()
    {
        return new CostUpdatedException(_quote);
    }

    protected final int _memberId;
    protected final Object _wareKey;
    protected final PriceQuote _quote;
}
