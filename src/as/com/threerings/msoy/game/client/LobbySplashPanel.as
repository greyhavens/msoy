//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Text;

import com.threerings.flex.CommandButton;

import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.LobbyObject;

/**
 * Displays a splash screen for a game, allowing the player to select single or multiplayer.
 */
public class LobbySplashPanel extends VBox
{
    public function LobbySplashPanel (gctx :GameContext, ctrl :LobbyController, lobj :LobbyObject)
    {
        _gctx = gctx;
        _ctrl = ctrl;
        _lobj = lobj;

        styleName = "lobbySplashPanel";
        percentWidth = 100;
        percentHeight = 100;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var headerBox :HBox = new HBox();
        headerBox.percentWidth = 100;
        headerBox.styleName = "descriptionBox";
        addChild(headerBox);

        headerBox.addChild(MediaWrapper.createView(_lobj.game.getThumbnailMedia()));

        var infoBox :HBox = new HBox();
        infoBox.styleName = "infoBox";
        infoBox.percentWidth = 100;
        infoBox.percentHeight = 100;
        var info :Text = new Text();
        info.styleName = "lobbyInfo";
        info.percentWidth = 100;
        info.percentHeight = 100;
        info.text = _lobj.game.description;
        infoBox.addChild(info);
        headerBox.addChild(infoBox);

        if (_lobj.gameDef.match.getMinimumPlayers() <= 1) {
            var soloBtn :CommandButton =
                new CommandButton(Msgs.GAME.get("b.start_solo"), LobbyController.PLAY_SOLO);
            soloBtn.styleName = "lobbySplashButton";
            soloBtn.width = 220;
            addChild(soloBtn);
        }

        if (_lobj.gameDef.match.getMaximumPlayers() > 1) {
            var multiBtn :CommandButton =
                new CommandButton(Msgs.GAME.get("b.start_multi"), function () :void {
                    _ctrl.panel.setMode(_ctrl.haveActionableTables() ?
                        LobbyController.MODE_MATCH : LobbyController.MODE_CREATE);
                });
            multiBtn.styleName = "lobbySplashButton";
            multiBtn.width = 220;
            addChild(multiBtn);

            // TODO: add "playing now" label
        }
    }

    override public function parentChanged (parent :DisplayObjectContainer) :void
    {
        super.parentChanged(parent);

        trace("Parent changed " + this + " -> " + parent);
        // TODO: register as observer
    }

    protected var _gctx :GameContext;
    protected var _ctrl :LobbyController;
    protected var _lobj :LobbyObject;
}
}
