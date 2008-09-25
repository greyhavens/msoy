//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Text;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.FlexUtil;
import com.threerings.util.Name;

import com.threerings.parlor.data.Table;

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

        var infoBox :HBox = new HBox();
        infoBox.percentWidth = 100;
        infoBox.styleName = "infoBox";
        addChild(infoBox);

        infoBox.addChild(MediaWrapper.createView(_lobj.game.getThumbnailMedia()));

        var info :Text = new Text();
        info.styleName = "lobbyInfo";
        info.percentWidth = 100;
        info.percentHeight = 100;
        info.text = _lobj.game.description;
        infoBox.addChild(info);

        var babBox :VBox = new VBox();
        babBox.percentWidth = 100;
        babBox.percentHeight = 100;
        babBox.setStyle("verticalGap", 60);
        babBox.setStyle("horizontalAlign", "center");
        babBox.setStyle("verticalAlign", "middle");
        addChild(babBox);

        if (_lobj.gameDef.match.getMinimumPlayers() <= 1) {
            var soloBtn :CommandButton =
                new CommandButton(Msgs.GAME.get("b.start_solo"), LobbyController.PLAY_SOLO);
            soloBtn.styleName = "lobbySplashButton";
            soloBtn.width = 220;
            babBox.addChild(soloBtn);
        }

        if (_lobj.gameDef.match.getMaximumPlayers() > 1) {
            var multiBtn :CommandButton =
                new CommandButton(Msgs.GAME.get("b.start_multi"), function () :void {
                    _ctrl.panel.setMode(_ctrl.haveActionableTables() ?
                        LobbyController.MODE_MATCH : LobbyController.MODE_CREATE);
                });
            multiBtn.styleName = "lobbySplashButton";
            multiBtn.width = 220;

            // count up how many players are "playing" this game now
            var count :int = 0;
            for each (var table :Table in _lobj.tables.toArray()) {
                count += table.players.filter(function (pname :Name, ii :int, a :Array) :Boolean {
                    return (pname != null);
                }).length;
                count += table.watchers.length;
            }

            // if we have none, show nothing, otherwise show "XX playing now!"
            if (count == 0) {
                babBox.addChild(multiBtn);
            } else {
                var yaBox :VBox = new VBox();
                yaBox.setStyle("horizontalAlign", "center");
                yaBox.setStyle("verticalGap", 2);
                yaBox.addChild(multiBtn);
                var msg :String = Msgs.GAME.get("m.lsp_playing", count);
                yaBox.addChild(FlexUtil.createLabel(msg, "lobbySplashPlaying"));
                babBox.addChild(yaBox);
            }
        }

        // add a link back to this game's instructions
        var instrLink :CommandLinkButton = new CommandLinkButton(
            Msgs.GAME.get("m.lsp_instructions"), _gctx.showGameInstructions);
        instrLink.styleName = "lobbySplashInstructions";
        addChild(instrLink);
    }

    protected var _gctx :GameContext;
    protected var _ctrl :LobbyController;
    protected var _lobj :LobbyObject;
}
}
