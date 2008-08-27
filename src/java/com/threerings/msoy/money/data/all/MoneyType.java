//
// $Id$

package com.threerings.msoy.money.data.all;

/**
 * Indicates the type of money.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public enum MoneyType
{
    /**
     * Bars are usually purchased for some real money amount and may be required to purchase some
     * items.
     */
    BARS,

    /** Coins are awarded from actions in Whirled and can be used to purchase some items. */
    COINS,

    /**
     * Bling is awarded when other players purchase or use some content created by a content
     * creator. It can be exchanged for real money.
     */
    BLING;
}
