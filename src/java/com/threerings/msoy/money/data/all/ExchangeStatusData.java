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

    /** The target rate (copied from a runtime config) */
    public float targetRate;

    /** The balance of the bar pool. */
    public int barPool;

    /** The target bar pool amount. */
    public int targetBarPool;

    public ExchangeStatusData (
        int total, List<ExchangeData> page,
        float rate, float targetRate, int barPool, int targetBarPool)
    {
        this.total = total;
        this.page = page;
        this.rate = rate;
        this.targetRate = targetRate;
        this.barPool = barPool;
        this.targetBarPool = targetBarPool;
    }

    /** Suitable for unserialization. */
    public ExchangeStatusData () {}
}
