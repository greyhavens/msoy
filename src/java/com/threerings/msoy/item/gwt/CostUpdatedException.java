//
// $Id$

package com.threerings.msoy.item.gwt;

import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.web.gwt.ServiceException;

/**
 * An exception thrown by things that might have a cost associated with them.
 */
public class CostUpdatedException extends ServiceException
{
    /** The error message associated with this exception. */
    public static final String E_COST_UPDATED = "e.cost_updated";

    /**
     * Create a CostUpdatedException.
     */
    public CostUpdatedException (PriceQuote quote)
    {
        super(E_COST_UPDATED);
        _quote = quote;
    }

    /** Suitable for unserialization. */
    public CostUpdatedException ()
    {
    }

    /**
     * Get the new price quote.
     */
    public PriceQuote getQuote ()
    {
        return _quote;
    }

    protected PriceQuote _quote;
}
