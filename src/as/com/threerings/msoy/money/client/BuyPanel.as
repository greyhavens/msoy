//
// $Id$

package com.threerings.msoy.money.client {

import mx.containers.HBox;
import mx.containers.VBox;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

public class BuyPanel extends VBox
{
    public function BuyPanel (ctx :MsoyContext, cmdOrFn :*, arg :Object = null)
    {
        setStyle("horizontalAlign", "center");
        _bars = new BuyButton(Currency.BARS, cmdOrFn, arg);
        _coins = new BuyButton(Currency.COINS, cmdOrFn, arg);
        _getBars = new CommandButton(Msgs.GENERAL.get("b.get_bars"), MsoyController.VIEW_URL,
            DeploymentConfig.billingURL + "whirled.wm?initUsername=" +
            encodeURIComponent(ctx.getClient().getClientObject().username.toString()));
        _getBars.styleName = "orangeButton";
    }

    public function setPriceQuote (quote :PriceQuote) :void
    {
        trace("got quote: " + quote.getBars() + ", " + quote.getCoins());
        _bars.setPriceQuote(quote);
        _coins.setPriceQuote(quote);
        FlexUtil.setVisible(_getBars, _bars.visible);
    }

//    override public function set enabled (value :Boolean) :void
//    {
//        super.enabled = true;
//
//        _enabled = value;
//        if (_bars != null) { // enabled gets set during our superclass construction.. wack
//            _bars.enabled = value;
//            _coins.enabled = value;
//            // always enable getBars
//        }
//    }
//
//    override public function get enabled () :Boolean
//    {
//        return _enabled;
//    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_bars);

        var hbox :HBox = new HBox();
        hbox.addChild(_getBars);
        hbox.addChild(_coins);
        addChild(hbox);
    }

    protected var _bars :BuyButton;
    protected var _coins :BuyButton;
    protected var _getBars :CommandButton;

    protected var _enabled :Boolean = true;
}
}
