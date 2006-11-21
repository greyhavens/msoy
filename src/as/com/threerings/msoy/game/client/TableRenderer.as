package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.util.MediaContainer;
import com.threerings.util.Name;

import com.threerings.mx.controls.CommandButton;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.data.MsoyTable;

public class TableRenderer extends HBox
{
    /** The context, initialized by our ClassFactory. */
    public var ctx :MsoyContext;

    /** The panel we're rendering to. */
    public var panel :LobbyPanel;

    public function TableRenderer ()
    {
        super();
        includeInLayout = false;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _background = new MediaContainer(
            panel.controller.game.getTableMedia().getMediaPath());
        _background.mouseEnabled = false;
        _background.mouseChildren = false;
        addChildAt(_background, 0);
        _background.addEventListener(
            MediaContainer.SIZE_KNOWN, handleBkgSizeKnown, false, 0, true);
    }

    override protected function measure () :void
    {
        super.measure();
        measuredWidth = Math.max(
            _background.getContentWidth(), measuredWidth);
        measuredHeight = Math.max(
            _background.getContentHeight(), measuredHeight);
    }

    protected function handleBkgSizeKnown (event :Event) :void
    {
        invalidateSize();
    }

    override public function set data (newData :Object) :void
    {
        super.data = newData;

        recheckTable();
    }

    protected function recheckTable () :void
    {
        var table :MsoyTable = (data as MsoyTable);
        var childCount :int = numChildren; 
        var seats :int = (table == null) ? 0 : table.occupants.length;
        var ourName :Name = ctx.getClientObject().getVisibleName();
        var childOffset :int = 0; 
        if (childCount > 0 && getChildAt(0) == _background) {
            childOffset++;
            childCount--;
        }
        var nn :int = Math.max(childCount, seats);

        for (var ii :int = 0; ii < nn; ii++) {
            var displayIndex :int = ii + childOffset;
            if (ii >= seats) {
                removeChildAt(numChildren - 1);
                continue;
            }

            var comp :UIComponent = (displayIndex < numChildren)
                ? UIComponent(getChildAt(displayIndex))
                : null;
            var occupant :Name = (table.occupants[ii] as Name);
            if (occupant == null || occupant.equals(ourName)) {
                var btn :CommandButton;
                if (comp is CommandButton) {
                    btn = (comp as CommandButton);
                } else {
                    if (comp != null) {
                        removeChildAt(displayIndex);
                    }
                    btn = new CommandButton();
                    addChildAt(btn, displayIndex);
                }
                if (occupant == null) {
                    btn.setCommand(LobbyController.SIT, [ table.tableId , ii ]);
                    btn.label = ctx.xlate("game", "b.sit");
                    btn.enabled = !panel.isSeated();

                } else {
                    btn.setCommand(LobbyController.LEAVE, table.tableId);
                    btn.label = ctx.xlate("game", "b.leave");
                    btn.enabled = true;
                }

            } else {
                var lbl :HeadShotSprite;
                if (comp is HeadShotSprite) {
                    lbl = (comp as HeadShotSprite);
                } else {
                    if (comp != null) {
                        removeChildAt(displayIndex);
                    }
                    lbl = new HeadShotSprite();
                    addChildAt(lbl, displayIndex);
                }
                lbl.setUser(occupant, table.headShots[ii] as MediaDesc);
            }
        }

        // if we are the creator, add a button for starting the game now
        if (table != null &&
            (table.tconfig.minimumPlayerCount < table.tconfig.desiredPlayerCount)
                && ourName.equals(table.occupants[0])) {
            btn = new CommandButton(LobbyController.START_TABLE, table.tableId);
            btn.label = ctx.xlate("game", "b.start_now");
            btn.enabled = table.mayBeStarted();
            addChild(btn);
        }
    }

    /** Holds our game's branding background. */
    protected var _background :MediaContainer;
}
}
