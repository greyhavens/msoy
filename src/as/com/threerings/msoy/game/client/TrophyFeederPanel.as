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
import com.threerings.msoy.ui.MsoyMediaContainer;

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

    /**
     * Shows a popup that asks if the user wants to post any of their earned trophies to their
     * Facebook feed.
     *
     * @return true if the popup was shown, false if it was not shown because no valid trophies
     * were supplied.
     */
    public static function showNew (gctx :GameContext, gameId :int, gameName :String,
        trophies :Array /* of Trophy */, onClose :Function) :Boolean
    {
        trophies = filterEarned(trophies);

        if (trophies.length == 0) {
            onClose();
            return false;
        }

        var tfp :TrophyFeederPanel;
        tfp = new TrophyFeederPanel(gctx.getMsoyContext(), trophies, gameName, MODE_NEW);
        tfp.addCloseCallback(onClose);
        tfp.open();
        return true;
    }

    public static function showExisting (gctx :GameContext, gameId :int, gameName :String) :void
    {
        (gctx.getClient().requireService(GameGameService) as GameGameService).getTrophies(
            gctx.getClient(), gameId, gctx.getMsoyContext().resultListener(
                function (trophies :TypedArray) :void {
                    new TrophyFeederPanel(gctx.getMsoyContext(), filterEarned(trophies), gameName,
                        trophies.length > 0 ? MODE_EXISTING : MODE_NONE).open();
                }, MsoyCodes.GAME_MSGS));
    }

    public function TrophyFeederPanel (
        ctx :MsoyContext, trophies :Array, gameName :String, mode :int)
    {
        super(ctx, Msgs.GAME.get("t.trophy_feeder"));
        _trophies = trophies;
        _gameName = gameName;
        _mode = mode;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        const descriptionWidth :int = 200;

        var title :Text = new Text();
        var titleKey :String;
        switch (_mode) {
        case MODE_NEW:
            titleKey = "m.trophy_feeder_title_new";
            break;
        case MODE_EXISTING:
            if (_trophies.length > 0) {
                titleKey = "m.trophy_feeder_title_existing";
            } else {
                titleKey = "m.trophy_feeder_title_none_earned";
            }
            break;
        case MODE_NONE:
            titleKey = "m.trophy_feeder_title_none_awarded";
            break;
        }
        title.text = Msgs.GAME.get(titleKey);
        title.styleName = "trophyFeederPanelTitle";
        title.width = (descriptionWidth + TrophySource.TROPHY_WIDTH) * 2;
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
            descrip.width = descriptionWidth;
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
        _ctx.getMsoyClient().dispatchEventToGWT(TROPHY_EVENT, [
            trophy.gameId, _gameName, trophy.name, trophy.description,
            trophy.trophyMedia.getMediaPath() ]);
        if (_mode == MODE_NEW) {
            close();
        }
    }

    protected static function filterEarned (trophies :Array) :Array
    {
        if (trophies == null) {
            return [];
        }
        return trophies.filter(function (trophy :Trophy, index :int, arr :Array) :Boolean {
            return trophy.whenEarned != null;
        });
    }

    protected var _trophies :Array /*of Trophy*/;
    protected var _gameName :String;
    protected var _mode :int;

    protected static const MODE_NEW :int = 0;
    protected static const MODE_EXISTING :int = 1;
    protected static const MODE_NONE :int = 2;

    /** Event dispatched to GWT when the user clicks a trophy. */
    protected static const TROPHY_EVENT :String = "trophy";
}
}
