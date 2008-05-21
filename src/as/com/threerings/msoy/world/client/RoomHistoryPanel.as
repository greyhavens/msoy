// 
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.controls.Label;

import mx.controls.scrollClasses.ScrollBar;

import mx.core.mx_internal;
import mx.core.UIComponent;
import mx.core.ScrollPolicy;

import mx.managers.PopUpManager;

import com.threerings.flash.MathUtil;

import com.threerings.flex.CommandLinkButton;

import com.threerings.msoy.client.HeaderBar;

public class RoomHistoryPanel extends TitleWindow
{
    public function RoomHistoryPanel (ctx :WorldContext, backstack :Array, backstackIdx :int)
    {
        _ctx = ctx;
        _backstack = backstack;
        _backstackIdx = backstackIdx;

        styleName = "roomHistoryPanel";
        titleIcon = TITLE_ICON;

        width = WIDTH;
        height = 130;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;
    }

    public function open () :void
    {
        y = HeaderBar.HEIGHT;
        // listen for clicks on the top panel so we can go away if something else grabs the player's
        // attention.
        _ctx.getTopPanel().addEventListener(MouseEvent.CLICK, externalClick);
        PopUpManager.addPopUp(this, _ctx.getTopPanel(), false);
    }

    public function close () :void
    {
        _ctx.getTopPanel().removeEventListener(MouseEvent.CLICK, externalClick);
        PopUpManager.removePopUp(this);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // wtf.  If you just add this to the title window, it put the scroll bar outside of the 
        // normal display area, drawing hte border to the left of the scroll bar, instead of 
        // including it as one might expect.  Therefore, we have to make another box for the sole
        // purpose of ensuring the scroll bar is inside the damn window border.
        var container :VBox = new VBox();
        container.horizontalScrollPolicy = ScrollPolicy.OFF;
        container.verticalScrollPolicy = ScrollPolicy.ON;
        container.styleName = "roomHistoryPanelContainer";
        container.percentWidth = 100;
        container.height = CONTAINER_HEIGHT;
        addChild(container);

        for (var ii :int = 0; ii < _backstack.length; ii++) {
            var data :Object = _backstack[ii];
            container.addChild(createRoomBox(ii, data.name as String, ii == _backstackIdx));
        }

        if (_backstack.length > 6) {
            callLater(function () :void { 
                // do something fancy to put the active room as close to the middle as the display
                // as we can.
                var clamped :int = MathUtil.clamp(_backstackIdx, 3, _backstack.length - 4) - 3;
                container.verticalScrollPosition = 
                    container.verticalScrollBar.maxScrollPosition * 
                    (clamped / (_backstack.length - 7));
            });
        }
    }

    override protected function layoutChrome (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.layoutChrome(unscaledWidth, unscaledHeight);

        // we want our title icon image centered
        mx_internal::titleIconObject.x = (unscaledWidth - mx_internal::titleIconObject.width) / 2;
    }

    protected function externalClick (event :MouseEvent) :void
    {
        // will cause the controller to shut us down.
        _ctx.getWorldController().handlePopRoomHistoryList();
    }

    protected function createRoomBox (idx :int, roomName :String, embolden :Boolean) :UIComponent
    {
        var roomBox :HBox = new HBox();
        roomBox.styleName = "roomHistoryPanelRoomBox";
        roomBox.horizontalScrollPolicy = ScrollPolicy.OFF;
        roomBox.verticalScrollPolicy = ScrollPolicy.OFF;
        roomBox.percentWidth = 100;
        roomBox.height = 17;
        var linkBtn :CommandLinkButton = 
            new CommandLinkButton(roomName, 
                                  _ctx.getWorldController().handleVisitBackstackIndex, 
                                  idx);
        // should be able to set percentWidth = 100 here, but long names break the layout instead
        // of truncating the text like it should
        linkBtn.width = WIDTH - PADDING * 2 - ScrollBar.THICKNESS;
        linkBtn.styleName = "roomHistoryPanelLink";
        if (embolden) {
            linkBtn.enabled = false;
            linkBtn.setStyle("fontWeight", "bold");
        }
        roomBox.addChild(linkBtn);
        return roomBox;
    }

    [Embed(source="../../../../../../../rsrc/media/skins/roomhistory/recentrooms.png")]
    protected static const TITLE_ICON :Class;

    protected static const WIDTH :int = 170;
    protected static const CONTAINER_HEIGHT :int = 107;
    protected static const PADDING :int = 6;

    protected var _ctx :WorldContext;
    protected var _backstack :Array;
    protected var _backstackIdx :int;
}
}
