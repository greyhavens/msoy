//
// $Id$

package com.threerings.msoy.money.data.all {

import com.threerings.util.Enum;

public final class Currency extends Enum
{
    /** Coins are awarded from actions in Whirled and can be used to purchase some items. */
    public static const COINS :Currency = new Currency("COINS");

    /**
     * Bars are usually purchased for some real money amount and may be required to purchase some
     * items.
     */
    public static const BARS :Currency = new Currency("BARS");

    /**
     * Bling is awarded when other players purchase or use some content created by a content
     * creator. It can be exchanged for real money.
     */
    public static const BLING :Currency = new Currency("BLING");
    finishedEnumerating(Currency);

    /** @private this is an enum */
    public function Currency (name :String)
    {
        super(name);
    }

    /**
     * Format a currency value.
     */
    public function format (value :int) :String
    {
        var postfix :String = "";
        if (this == BLING) {
            const cents :int = Math.abs(value % 100);
            value = int(value / 100);
            postfix = "." + int(cents / 10) + (cents % 10); // always print two decimal places
        }

        // TODO: this will change, we want commas
        return "" + value + postfix;
    }

    /**
     * Used to display just the name of the currency.
     */
    public function getLabel () :String
    {
        return "l." + toString().toLowerCase();
    }

    /**
     * Used when translating a currency with a value:
     * MessageBundle.get(currency.getKey(), amount) == "5 bars", or "1 bar"
     */
    public function getKey () :String
    {
        return "m." + toString().toLowerCase();
    }

    /**
     * Get an array of all the Currency values.
     */
    public static function values () :Array
    {
        return Enum.values(Currency);
    }
}
}
