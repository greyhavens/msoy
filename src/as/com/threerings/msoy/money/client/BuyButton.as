//
// $Id$

package com.threerings.msoy.money.client {

import com.threerings.flex.CommandButton;

import com.threerings.msoy.money.data.all.Currency;

public class BuyButton extends CommandButton
{
    public function BuyButton (currency :Currency, cmdOrFn :* = null, arg :Object = null)
    {
        super(null, cmdOrFn, arg);

        if (currency == Currency.BARS) {
            styleName = "orangeButton";
        }
        setStyle("icon", currency.getLargeIconClass());
        setStyle("fontSize", 24);
        setStyle("fontFamily", "_sans");
    }

    public function setValue (value :int) :void
    {
        label = "" + value;
    }
}
}
