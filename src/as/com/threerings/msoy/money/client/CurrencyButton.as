//
// $Id$

package com.threerings.msoy.money.client {

import com.threerings.flex.CommandButton;

import com.threerings.msoy.money.data.all.Currency;

public class CurrencyButton extends CommandButton
{
    public function CurrencyButton (cmdOrFn :* = null, arg :Object = null)
    {
        super(null, cmdOrFn, arg);
    }

    public function setValue (currency :Currency, value :int) :void
    {
        // TODO
    }
}
}
