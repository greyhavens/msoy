//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.List;

import com.threerings.gwt.util.PagedResult;

/**
 * Returns current exchange status information.
 */
public class ExchangeStatusData extends PagedResult<ExchangeData>
{
    /** The current exchange rate. */
    public float rate;

    /** The balance of the bar pool. */
    public int barPool;

    public ExchangeStatusData (int total, List<ExchangeData> page, float rate, int barPool)
    {
        this.total = total;
        this.page = page;
        this.rate = rate;
        this.barPool = barPool;
    }

    /** Suitable for unserialization. */
    public ExchangeStatusData () {}
}
