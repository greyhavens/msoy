//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.List;

import com.threerings.gwt.util.PagedResult;

/**
 * Contains all of the information needed for the Transactions page.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class TransactionPageResult extends PagedResult<MoneyTransaction>
{
    /** The user's current bling information. */
    public /* final */ BlingInfo blingInfo;
    
    public TransactionPageResult (int total, List<MoneyTransaction> page, BlingInfo blingInfo)
    {
        this.blingInfo = blingInfo;
        this.total = total;
        this.page = page;
    }
    
    /** For serialization purposes. */
    public TransactionPageResult () { }
}
