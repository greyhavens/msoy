//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.Grid;
import mx.containers.GridRow;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.core.ScrollPolicy;
import mx.controls.Label;
import mx.controls.Text;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;

import com.threerings.io.TypedArray;

import com.threerings.util.Log;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.MsoyMediaContainer;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.game.data.all.Trophy;

import com.threerings.msoy.item.data.all.TrophySource;

/**
 * Displays a grid of trophies and allows the user to post the winning of each trophy as an
 * external news feed item. TODO: if people like this, allow selecting multiple trophies to
 * send.
 */
public class TrophyFeederPanel extends FloatingPanel
{
    public var log :Log = Log.getLog(this);

    public static function show (gctx :GameContext, gameId :int, gameName :String,
                                 trophies :TypedArray /* of Trophy */, onClose :Function) :void
    {
        if (trophies != null) {
            trophies.filter(function (trophy :Trophy, index :int, arr :Array) :Boolean {
                return trophy.whenEarned != null;
            });
        }

        // for testing, grab the trophies from the server if none are given
        if (trophies == null || trophies.length == 0) {
            (gctx.getClient().requireService(GameGameService) as GameGameService).getTrophies(
                gctx.getClient(), gameId, gctx.getMsoyContext().resultListener(
                    function (trophies :TypedArray) :void {
                        if (trophies.length > 0) {
                            show(gctx, gameId, gameName, trophies, onClose);

                        } else {
                            onClose();
                        }
                    }, MsoyCodes.GAME_MSGS));

        } else {
            var tfp :TrophyFeederPanel;
            tfp = new TrophyFeederPanel(gctx.getMsoyContext(), trophies, gameName);
            tfp.setCloseCallback(onClose);
            tfp.open();
        }
    }

    public function TrophyFeederPanel (ctx :MsoyContext, trophies :TypedArray, gameName :String)
    {
        super(ctx, Msgs.GAME.get("t.trophy_feeder"));
        _trophies = trophies;
        _gameName = gameName;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var title :Label = new Label();
        title.text = Msgs.GAME.get("m.trophy_feeder_title");
        title.styleName = "trophyFeederPanelTitle";
        addChild(title);

        var grid :Grid = new Grid();
        grid.maxHeight = 400;
        grid.horizontalScrollPolicy = ScrollPolicy.OFF;
        grid.verticalScrollPolicy = ScrollPolicy.AUTO;
        addChild(grid);

        var row :GridRow = null;
        var cell :int = 0;
        for each (var trophy :Trophy in _trophies) {
            if (row == null) {
                row = new GridRow();
                grid.addChild(row);
            }
            var tbox :HBox = new HBox();
            var tbtn :CommandButton = new CommandButton();
            tbtn.styleName = "trophyFeedTrophy";
            tbtn.setStyle("image", new MsoyMediaContainer(trophy.trophyMedia));
            tbtn.setCallback(postTrophyToFeed, trophy);
            tbtn.width = TrophySource.TROPHY_WIDTH;
            tbtn.height = TrophySource.TROPHY_HEIGHT;
            tbox.addChild(tbtn);
            var bits :VBox = new VBox();
            bits.setStyle("verticalGap", 0);
            var name :Label = new Label();
            name.styleName = "trophyFeedPanelName";
            name.text = trophy.name;
            bits.addChild(name);
            var descrip :Text = new Text();
            descrip.width = 200;
            descrip.text = trophy.description;
            bits.addChild(descrip);
            tbox.addChild(bits);
            GridUtil.addToRow(row, tbox);
            if (++cell % 2 == 0) {
                row = null;
            }
        }

        showCloseButton = true;
    }

    protected function postTrophyToFeed (trophy :Trophy) :void
    {
        log.info("Here we go...", "trophy", trophy);
        _ctx.getMsoyClient().dispatchEventToGWT(TROPHY_EVENT, [
            trophy.gameId, _gameName, trophy.name, trophy.description,
            trophy.trophyMedia.getMediaPath() ]);
    }

    protected var _trophies :TypedArray /*of Trophy*/;
    protected var _gameName :String;

    /** Event dispatched to GWT when the user clicks a trophy. */
    protected static const TROPHY_EVENT :String = "trophy";
}
}
