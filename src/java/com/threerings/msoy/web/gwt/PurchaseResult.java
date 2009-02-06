//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.money.data.all.BalanceInfo;
import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * A base class for purchase results.
 */
public abstract class PurchaseResult
    implements IsSerializable
{
    /** Any updated balances. */
    public BalanceInfo balances;

    /** Another price quote if they wish to buy again. */
    public PriceQuote quote;

    /** The percentage of the purchase price the charity received. Optional. */
    public float charityPercentage;

    /** The name of the charity that received the donation. Optional. */
    public MemberName charity;
}
