//
// $Id$

package com.threerings.msoy.money.client {

import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.flex.CommandLinkButton;
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
        _getBars = new CommandLinkButton(Msgs.GENERAL.get("b.get_bars"), MsoyController.VIEW_URL,
            DeploymentConfig.billingURL + "whirled.wm?initUsername=" +
            encodeURIComponent(ctx.getClient().getClientObject().username.toString()));
        _getBars.styleName = "underLink";

        _barPanel = new VBox();
        _barPanel.setStyle("horizontalAlign", "center");
        _coinPanel = new VBox();
        _coinPanel.setStyle("horizontalAlign", "center");

        _switchToCoins = new CommandLinkButton(Msgs.GENERAL.get("b.switchToCoins"), switchCurrency);
        _switchToCoins.styleName = "underLink"

        _barPanel.addChild(_bars);
        _barPanel.addChild(_barLabel = new Label());
        _barPanel.addChild(_getBars);
        _barPanel.addChild(_switchToCoins);

        _coinPanel.addChild(_coins);

        // hide everything until we're ready to roll
        FlexUtil.setVisible(_coinPanel, false);
        FlexUtil.setVisible(_barPanel, false);
    }

    public function setPriceQuote (quote :PriceQuote) :void
    {
        _quote = quote;

        var barOnly :Boolean = (quote.getCoins() < 0);
        FlexUtil.setVisible(_getBars, barOnly);
        FlexUtil.setVisible(_switchToCoins, !barOnly);

        _bars.setPriceQuote(quote);
        _coins.setPriceQuote(quote);

        var barTip :String = null;
        if (barOnly) {
            FlexUtil.setVisible(_coinPanel, false);
            FlexUtil.setVisible(_barPanel, true);
        } else {
            FlexUtil.setVisible(_coinPanel, !_altCurrency);
            FlexUtil.setVisible(_barPanel, _altCurrency);
            var change :int = quote.getCoinChange();
            if (change > 0) {
                barTip = Msgs.GENERAL.get("m.coinChange", Currency.COINS.format(change));
            }
        }
        FlexUtil.setVisible(_barLabel, (barTip != null));
        if (barTip != null) {
            _barLabel.text = barTip;
        }
    }

    override public function set enabled (value :Boolean) :void
    {
        // Note: we do not save the state, see note in getter.
        if (_bars != null) { // enabled gets set during our superclass construction.. wack
            _bars.enabled = value;
            _coins.enabled = value;
            // always enable getBars
        }
    }

    /**
     * This container can be set enabled or disabled, but it will always read back
     * out as enabled, due to the fact that if this ever returns false, then our superclass
     * will draw a white box over us, which we do not want.
     */
    override public function get enabled () :Boolean
    {
        return true;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_barPanel);
        addChild(_coinPanel);
    }

    protected function switchCurrency () :void
    {
        _altCurrency = !_altCurrency;
        setPriceQuote(_quote);
    }

    protected var _quote :PriceQuote;

    protected var _barPanel :VBox;
    protected var _coinPanel :VBox;

    protected var _bars :BuyButton;
    protected var _coins :BuyButton;
    protected var _getBars :CommandLinkButton;
    protected var _switchToCoins :CommandLinkButton;
    protected var _barLabel :Label;

    protected var _altCurrency :Boolean;
}
}
