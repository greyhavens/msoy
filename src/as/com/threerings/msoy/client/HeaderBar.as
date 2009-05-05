//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;

import flash.utils.Dictionary;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.HBox;

import mx.controls.Image;
import mx.controls.Label;

import com.threerings.flash.TextFieldUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.FlexUtil;

import com.threerings.presents.client.ClientAdapter;

import com.threerings.presents.dobj.AttributeChangeAdapter;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.client.ChatTabBar;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.world.client.WorldController;

public class HeaderBar extends HBox
{
    public static const HEIGHT :int = 17;

    public function HeaderBar (ctx :MsoyContext, topPanel :TopPanel, chatTabs :ChatTabBar)
    {
        _ctx = ctx;
        _tabs = chatTabs;
        styleName = "headerBar";

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        percentWidth = 100;
        height = HEIGHT;

        // listen for location name and ownership changes
        topPanel.addEventListener(TopPanel.LOCATION_NAME_CHANGED, locationNameChanged);
        topPanel.addEventListener(TopPanel.LOCATION_OWNER_CHANGED, locationOwnerChanged);
    }

    public function getChatTabs () :ChatTabBar
    {
        return _tabs;
    }

    public function stretchSpacer (stretch :Boolean) :void
    {
        var ownTabs :Boolean = (_tabsContainer.parent == this);
        var stretchTabs :Boolean = !(stretch && ownTabs);
        var stretchSpacer :Boolean = (stretch || !ownTabs);
        if (stretchTabs == isNaN(_tabsContainer.percentWidth)) {
            _tabsContainer.percentWidth = stretchTabs ? 100 : NaN;
        }
        if (stretchSpacer == isNaN(_spacer.percentWidth)) {
            _spacer.percentWidth = stretchSpacer ? 100 : NaN;
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _backBtn = new CommandButton();
        _backBtn.toolTip = Msgs.GENERAL.get("b.back");
        _backBtn.setCommand(MsoyController.MOVE_BACK);
        _backBtn.styleName = "headerBarBack";
        addChild(_backBtn);

        _loc = new Label();
        _loc.styleName = "locationName";
        _loc.width = WHIRLED_LOGO_WIDTH;
        FlexUtil.setVisible(_loc, false);
        addChild(_loc);

        _tabsContainer = new TabsContainer(this);
        _tabsContainer.horizontalScrollPolicy = ScrollPolicy.OFF;
        addChild(_tabsContainer);

        _tabsContainer.addChild(_tabs);

        var controlBox :HBox = new HBox();
        controlBox.styleName = "headerBox";
        controlBox.percentHeight = 100;
        addChild(controlBox);

        _spacer = new Spacer(this);
        addChild(_spacer);

        _owner = new HBox();
        _owner.styleName = "headerBox";
        _owner.percentHeight = 100;
        addChild(_owner);

        _closeBox = new HBox();
        _closeBox.styleName = "headerCloseBox";
        addChild(_closeBox);

        var heightBtn :CommandButton = new CommandButton(null, WorldController.TOGGLE_HEIGHT);
        heightBtn.toolTip = Msgs.GENERAL.get("i.height");
        heightBtn.styleName = "heightButton";
        _closeBox.addChild(heightBtn);

        var closeBtn :CommandButton = new CommandButton(null, MsoyController.CLOSE_PLACE_VIEW);
        closeBtn.styleName = "closeButton";
        _closeBox.addChild(closeBtn);
        FlexUtil.setVisible(_closeBox, false); // start out hidden

        _ctx.getUIState().addEventListener(UIState.STATE_CHANGE, handleUIStateChange);
        handleUIStateChange(null);

        // configure some bits if we're embedded
        const embedded :Boolean = _ctx.getMsoyClient().isEmbedded();
        setCompVisible(_closeBox, !embedded);

        // add a coins display
        if (embedded) {
            // disabled for the time being
            // TODO: remove permanently
            if (_moneyEnabled) {
                addCurrencyIcon(Currency.COINS);
                _coinsLabel = new Label();
                _coinsLabel.styleName = "currencyLabel";
                addChild(_coinsLabel);

                addCurrencyIcon(Currency.BARS);
                _barsLabel = new Label();
                _barsLabel.styleName = "currencyLabel";
                addChild(_barsLabel);
            }

            // set up a listener to hear about userobject changes
            _ctx.getClient().addClientObserver(
                new ClientAdapter(null, clientDidChange, clientDidChange));
            clientDidChange();
        }
    }

    protected function addCurrencyIcon (currency :Currency) :void
    {
        addChild(FlexUtil.createSpacer(10));
        const icon :Image = new Image();
        icon.source = currency.getEmbedHeaderIcon();
        const hb :HBox = new HBox();
        hb.setStyle("verticalAlign", "middle");
        hb.percentHeight = 100;
        hb.addChild(icon);
        addChild(hb);
    }

    protected function locationNameChanged (event :ValueEvent) :void
    {
        var name :String = (event.value as String);
        _loc.text = name;
        _loc.validateNow();
        // allow text to center under the whirled logo if its not too long.
        _loc.width = Math.max(WHIRLED_LOGO_WIDTH, _loc.textWidth + TextFieldUtil.WIDTH_PAD);

        if (!(_ctx.getPlaceView() is RoomView)) {
            _tabs.locationName = name;
        }

        // update our window title with the location name
        _ctx.getMsoyClient().setWindowTitle(name);
        checkBackButton();
    }

    protected function locationOwnerChanged (event :ValueEvent) :void
    {
        while (_owner.numChildren > 0) {
            _owner.removeChildAt(0);
        }
        if (event.value != null) {
            var name :String = event.value.toString(), cmd :String, arg :Object;
            if (event.value is MemberName) {
                cmd = MsoyController.VIEW_MEMBER;
                arg = (event.value as MemberName).getMemberId();
            } else if (event.value is GroupName) {
                cmd = MsoyController.VIEW_GROUP;
                arg = (event.value as GroupName).getGroupId();
            }
            var nameLink :CommandLinkButton = new CommandLinkButton(
                Msgs.GENERAL.get("m.room_owner", name), cmd, arg);
            nameLink.styleName = "headerLink";
            _owner.addChild(nameLink);
        }
    }

    protected function setCompVisible (comp :UIComponent, visible :Boolean) :void
    {
        _visibles[comp] = FlexUtil.setVisible(comp, visible);
    }

    /**
     * Called when the client object is set up or changes. Only used in an embedded client.
     */
    protected function clientDidChange (... ignored) :void
    {
        const cliObj :MemberObject = _ctx.getClient().getClientObject() as MemberObject;
        if (cliObj != null) {
            cliObj.addListener(new AttributeChangeAdapter(clientAttrChanged));
            if (_moneyEnabled) {
                _coinsLabel.text = Currency.COINS.format(cliObj.coins);
                _barsLabel.text = Currency.BARS.format(cliObj.bars);
            }
        }
    }

    /**
     * Only used in an embedded client.
     */
    protected function clientAttrChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.COINS:
            if (_moneyEnabled) {
                _coinsLabel.text = Currency.COINS.format(int(event.getValue()));
            }
            break;

        case MemberObject.BARS:
            if (_moneyEnabled) {
                _barsLabel.text = Currency.BARS.format(int(event.getValue()));
            }
            break;
        }
    }

    protected function handleUIStateChange (event :Event) :void
    {
        var state :UIState = _ctx.getUIState();
        _tabsContainer.visible = state.showChat;
        _backBtn.visible = !(state.embedded && state.inGame);
        checkBackButton();
    }

    protected function checkBackButton () :void
    {
        var ctrl :MsoyController = _ctx.getMsoyController();
        _backBtn.enabled = (ctrl != null) && ctrl.canMoveBack();
    }

    protected static const WHIRLED_LOGO_WIDTH :int = 124;

    protected var _ctx :MsoyContext;

    protected var _loc :Label;
    protected var _owner :HBox;
    protected var _spacer :HBox;

    protected var _visibles :Dictionary = new Dictionary(true);

    protected var _backBtn :CommandButton;

    protected var _closeBox :HBox;

    protected var _tabs :ChatTabBar;

    protected var _tabsContainer :TabsContainer;

    /** The currency labels, used only when embedded. Disabled for now.
     * TODO: remove. */
    protected var _moneyEnabled :Boolean;
    protected var _coinsLabel :Label;
    protected var _barsLabel :Label;
}
}

import mx.containers.HBox;

import com.threerings.msoy.client.HeaderBar;

class Spacer extends HBox
{
    public function Spacer (headerBar :HeaderBar)
    {
        _headerBar = headerBar;

        setStyle("borderThickness", 0);
        setStyle("borderStyle", "none");
        percentWidth = 100;
    }

    override public function setActualSize (w :Number, h :Number) :void
    {
        super.setActualSize(w, h);
        _headerBar.stretchSpacer(w != 0);
    }

    protected var _headerBar :HeaderBar;
}

class TabsContainer extends HBox
{
    public function TabsContainer (headerBar :HeaderBar)
    {
        _headerBar = headerBar;

        setStyle("borderThickness", 0);
        setStyle("borderStyle", "none");
        setStyle("horizontalGap", 0);
        explicitMinWidth = 0;
    }

    override public function setActualSize (w :Number, h :Number) :void
    {
        super.setActualSize(w, h);
        if (w > getExplicitOrMeasuredWidth()) {
            _headerBar.stretchSpacer(true);
        }
    }

    protected var _headerBar :HeaderBar;
}
