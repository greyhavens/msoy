//
// $Id$

package com.threerings.msoy.money.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.money.data.all.MoneyTransaction;

public class HistoryListResult implements IsSerializable
{
    /** The total number of logged transactions available on the server. */
    public int totalCount;

    public List<MoneyTransaction> transactions;
}
