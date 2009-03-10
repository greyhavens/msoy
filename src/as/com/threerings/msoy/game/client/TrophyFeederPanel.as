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

import com.threerings.flex.GridUtil;
import com.threerings.io.TypedArray;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.TrophySource;

/**
 * Displays a grid of trophies and allows the user to post the winning of each trophy as an
 * external news feed item.
 */
public class TrophyFeederPanel extends FloatingPanel
{
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
        grid.verticalScrollPolicy = ScrollPolicy.ON;
        addChild(grid);

        var row :GridRow = null;
        var cell :int = 0;
        for each (var trophy :Trophy in _trophies) {
            if (row == null) {
                row = new GridRow();
                grid.addChild(row);
            }
            var tbox :HBox = new HBox();
            tbox.addChild(new MediaWrapper(new MsoyMediaContainer(trophy.trophyMedia),
                    TrophySource.TROPHY_WIDTH, TrophySource.TROPHY_HEIGHT));
            var bits :VBox = new VBox();
            bits.setStyle("verticalGap", 0);
            var name :Label = new Label();
            name.styleName = "trophyFeedPanelName";
            name.text = trophy.name;
            bits.addChild(name);
            var descrip :Text = new Text();
            descrip.width = 200;
            descrip.text = (trophy.description == null) ? "" : trophy.description;
            bits.addChild(descrip);
            tbox.addChild(bits);
            GridUtil.addToRow(row, tbox);
            if (++cell % 2 == 0) {
                row = null;
            }
        }

        showCloseButton = true;
    }

    protected var _trophies :Array /*of Trophy*/;
    protected var _gameName :String;
}
}
