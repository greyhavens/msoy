//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.IsSerializable;

/** The format of a transaction report. */
public enum ReportType
    implements IsSerializable
{
    COINS(null, Currency.COINS),
    BARS(null, Currency.BARS),
    BLING(null, Currency.BLING),
    CREATOR(EnumSet.of(TransactionType.CREATOR_PAYOUT), null),
    ;

    private ReportType (EnumSet<TransactionType> transactions, Currency currency)
    {
        this.transactions = transactions;
        this.currency = currency;
    }

    public transient EnumSet<TransactionType> transactions;
    public transient Currency currency;
}
