//
// $Id$

package com.threerings.msoy.item.gwt;

import com.threerings.msoy.web.data.ServiceException;

/**
 * An exception thrown by things that might have a cost associated with them.
 */
public class CostUpdatedException extends ServiceException
{
    /** The error message associated with this exception. */
    public static final String E_COST_INCREASED = "e.cost_increased";

    /**
     * Create a CostUpdatedException.
     */
    public CostUpdatedException (int flowCost, int goldCost)
    {
        super(E_COST_INCREASED);
        _flowCost = flowCost;
        _goldCost = goldCost;
    }

    /** Suitable for unserialization. */
    public CostUpdatedException ()
    {
    }

    /**
     * Get the new flow cost.
     */
    public int getFlowCost ()
    {
        return _flowCost;
    }

    /**
     * Get the new gold cost.
     */
    public int getGoldCost ()
    {
        return _goldCost;
    }

    protected int _flowCost;
    protected int _goldCost;
}
