//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains all of the information needed for the Transactions page.
 */
public class TransactionPageResult
    implements IsSerializable
{
    /** The user's current bling information. */
    public /* final */ BlingInfo blingInfo;

    /** The current page of elements */
    public List<MoneyTransaction> page;

    public TransactionPageResult (List<MoneyTransaction> page, BlingInfo blingInfo)
    {
        this.blingInfo = blingInfo;
        this.page = page;
    }

    /** For serialization purposes. */
    public TransactionPageResult () { }
}
