//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.IsSerializable;

/** The format of a transaction report. */
public enum ReportType
    implements IsSerializable
{
    COINS(0, null, Currency.COINS, Currency.COINS.getSmallIcon()),
    BARS(1, null, Currency.BARS, Currency.BARS.getSmallIcon()),
    BLING(2, null, Currency.BLING, Currency.BLING.getSmallIcon()),
    CREATOR(3, EnumSet.of(TransactionType.CREATOR_PAYOUT), null, "/images/profile/browseitems.png"),
    ;

    public int toIndex ()
    {
        return _index;
    }

    public static ReportType fromIndex (int index)
    {
        for (ReportType r : values()) {
            if (r.toIndex() == index) {
                return r;
            }
        }
        return COINS; // Fall back to default
    }

    private ReportType (
        int index, EnumSet<TransactionType> transactions, Currency currency, String icon)
    {
        _index = index;
        this.transactions = transactions;
        this.currency = currency;
        this.icon = icon;
    }

    public transient EnumSet<TransactionType> transactions;
    public transient Currency currency;
    public transient String icon;

    protected transient int _index;
}
