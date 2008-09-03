//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Indicates the type of money.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public enum Currency
    implements IsSerializable
{
    /** Coins are awarded from actions in Whirled and can be used to purchase some items. */
    COINS,

    /**
     * Bars are usually purchased for some real money amount and may be required to purchase some
     * items.
     */
    BARS,

    /**
     * Bling is awarded when other players purchase or use some content created by a content
     * creator. It can be exchanged for real money.
     */
    BLING;

    /**
     * Used to display just the name of the currency.
     */
    public String getLabel ()
    {
        return "l." + toString().toLowerCase();
    }

    /**
     * Used when translating a currency with a value:
     * MessageBundle.get(currency.getKey(), amount) == "5 bars", or "1 bar"
     */
    public String getKey ()
    {
        return "m." + toString().toLowerCase();
    }
}
