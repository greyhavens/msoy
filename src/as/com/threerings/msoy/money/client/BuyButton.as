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

        styleName = "buyButton" + currency;
    }

    public function setValue (value :int) :void
    {
        // TODO: update UI

//        _arg = [ currency, value ];
    }
}
}
