//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.utils.Dictionary;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import flexlib.containers.ButtonScrollingCanvas;

import com.threerings.flash.TextFieldUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.FlexWrapper;

import com.threerings.util.Command;
import com.threerings.util.CommandEvent;
import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.MsoyClient;

import com.threerings.msoy.chat.client.ChatTabBar;

import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.world.client.WorldController;

public class HeaderBar extends HBox
{
    public static const HEIGHT :int = 17;

    public function HeaderBar (ctx :MsoyContext, chatTabs :ChatTabBar) 
    {
        _ctx = ctx;
        _tabs = chatTabs;
        styleName = "headerBar";

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        percentWidth = 100;
        height = HEIGHT;

        // TODO: should we be doing this?
        addEventListener(Event.ADDED_TO_STAGE, function (evt :Event) :void {
            _ctx.getMsoyClient().setWindowTitle(getChatTabs().locationName);
        });

        _ctx.getMsoyClient().addEventListener(MsoyClient.EMBEDDED_STATE_KNOWN, handleEmbeddedKnown);
    }

    public function getChatTabs () :ChatTabBar
    {
        return _tabs;
    }

    public function setLocationName (loc :String) :void
    {
        _loc.text = loc;
        _loc.validateNow();
        // allow text to center under the whirled logo if its not too long.
        _loc.width = Math.max(WHIRLED_LOGO_WIDTH, _loc.textWidth + TextFieldUtil.WIDTH_PAD);

        // TODO: hard-coded shite should not be here
        if (!(_ctx.getTopPanel().getPlaceView() is RoomView)) {
            _tabs.locationName = loc; // Msgs.CHAT.get("l.game_channel");
        }
    }

    /**
     * Shows or clears the owner link. Passing "" for the owner will clear the link.
     */
    public function setOwnerLink (owner :String, onClick :Function = null, arg :Object = null) :void
    {
        while (_owner.numChildren > 0) {
            _owner.removeChildAt(0);
        }
        if (owner != "") {
            var nameLink :CommandLinkButton = new CommandLinkButton(
                Msgs.GENERAL.get("m.room_owner", owner), onClick, arg);
            nameLink.styleName = "headerLink";
            _owner.addChild(nameLink);
        }
    }

    /**
     * Shows or clears the instructions link. Passing null for the onClick function will clear the 
     * link.
     */
    public function setInstructionsLink (onClick :Function , arg :Object = null) :void
    {
        _instructionsLink.setCallback(onClick, arg);
        setCompVisible(_instructionsLink, (onClick != null));
    }

    public function miniChanged () :void
    {
        if (_ctx.getTopPanel().isMinimized()) {
            for each (var comp :UIComponent in _extras) {
                FlexUtil.setVisible(comp, false);
            }
            stretchSpacer(false);
        } else {
            for each (comp in _extras) {
                FlexUtil.setVisible(comp, (_visibles[comp] === undefined) || _visibles[comp]);
            }
            stretchSpacer(true);
        }
    }

    /**
     * Grab the tabs and the associated trimmings in the container that contains them, so they
     * can be moved somewhere else.
     */
    public function removeTabsContainer () :UIComponent
    {
        if (_tabsContainer.parent == this) {
            removeChild(_tabsContainer);
        }
        FlexUtil.setVisible(_loc, true);
        _loc.validateNow();
        // allow text to center under the whirled logo if its not too long.
        _loc.width = Math.max(WHIRLED_LOGO_WIDTH, _loc.textWidth + TextFieldUtil.WIDTH_PAD);
        stretchSpacer(false);
        //FlexUtil.setVisible(_goBtn, false);
        return _tabsContainer;
    }

    public function replaceTabsContainer () :void
    {
        if (_tabsContainer.parent != null) {
            _tabsContainer.parent.removeChild(_tabsContainer);
        }
        _tabs.locationName = null;
        if (_loc.parent == this) {
            FlexUtil.setVisible(_loc, false);
        }
        //FlexUtil.setVisible(_goBtn, true);
        addChildAt(_tabsContainer, 1);
    }

    public function stretchSpacer (stretch :Boolean) :void
    {
        var mini :Boolean = _ctx.getTopPanel().isMinimized();
        var ownTabs :Boolean = (_tabsContainer.parent == this);
        var stretchTabs :Boolean = !(stretch && ownTabs && !mini);
        var stretchSpacer :Boolean = (stretch || !ownTabs) && !mini;
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

        _goBtn = new CommandButton();
        _goBtn.toolTip = Msgs.GENERAL.get("i.go");
        _goBtn.setCommand(MsoyController.POP_GO_MENU, _goBtn);
        _goBtn.styleName = "headerBarGoButton";
        // we lock it to the scrollbar thickness so that it matches the gutter area on the left
        _goBtn.width = ScrollBar.THICKNESS;
        addChild(_goBtn);

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

        _instructionsLink = new CommandLinkButton(Msgs.GENERAL.get("b.instructions"));
        _instructionsLink.styleName = "headerLink";
        controlBox.addChild(_instructionsLink);
        setInstructionsLink(null);
        _extras.push(_instructionsLink);

        _spacer = new Spacer(this);
        addChild(_spacer);

        _owner = new HBox();
        _owner.styleName = "headerBox";
        _owner.percentHeight = 100;
        addChild(_owner);
        _extras.push(_owner);

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
        FlexUtil.setVisible(_closeBox, false) // start out hidden
    }

    protected function handleEmbeddedKnown (event :ValueEvent) :void
    {
        const embedded :Boolean = event.value as Boolean;
        setCompVisible(_closeBox, !embedded);
    }

    protected function setCompVisible (comp :UIComponent, visible :Boolean) :void
    {
        _visibles[comp] = FlexUtil.setVisible(comp, visible);
    }

    private static const log :Log = Log.getLog(HeaderBar);

    protected static const WHIRLED_LOGO_WIDTH :int = 124;

    protected var _ctx :MsoyContext;

    protected var _loc :Label;
    protected var _owner :HBox;
    protected var _spacer :HBox;

    protected var _instructionsLink :CommandLinkButton;

    protected var _visibles :Dictionary = new Dictionary(true);

    protected var _closeBox :HBox;

    protected var _tabs :ChatTabBar;

    /** Bits that get removed when in minimized view */
    protected var _extras :Array = [];

    protected var _tabsContainer :TabsContainer;

    /** The go button. */
    protected var _goBtn :CommandButton;
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
