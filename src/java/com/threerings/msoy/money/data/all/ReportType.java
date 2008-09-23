//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.IsSerializable;

/** The format of a transaction report. */
public enum ReportType
    implements IsSerializable
{
    COINS(null, Currency.COINS, Currency.COINS.getSmallIcon()),
    BARS(null, Currency.BARS, Currency.BARS.getSmallIcon()),
    BLING(null, Currency.BLING, Currency.BLING.getSmallIcon()),
    CREATOR(EnumSet.of(TransactionType.CREATOR_PAYOUT), null, "/images/profile/browseitems.png"),
    ;

    private ReportType (EnumSet<TransactionType> transactions, Currency currency, String icon)
    {
        this.transactions = transactions;
        this.currency = currency;
        this.icon = icon;
    }

    public transient EnumSet<TransactionType> transactions;
    public transient Currency currency;
    public transient String icon;
}
