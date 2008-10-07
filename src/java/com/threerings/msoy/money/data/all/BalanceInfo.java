//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains updated info on one or more of a user's balances.
 */
public class BalanceInfo
    implements IsSerializable
{
    /** Updated coin balance, if any. */
    public Integer coins;

    /** Updated bar balance, if any. */
    public Integer bars;

    /** Updated bling balance, if any. */
    public Integer bling;
}
