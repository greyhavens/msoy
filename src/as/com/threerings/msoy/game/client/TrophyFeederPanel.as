//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.Grid;
import mx.containers.GridRow;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.Text;
import mx.core.ScrollPolicy;

import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyMediaContainer;

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
     * Facebook feed. In this mode, when a trophy is selected for publishing, the popup will
     * automatically close.
     *
     * @return true if the popup was shown, false if it was not shown because no valid trophies
     * were supplied.
     */
    public static function showNew (
        ctx :MsoyContext, gameName :String, gameDescription :String,
        trophies :Array /* of Trophy */, onClose :Function) :void
    {
        // these trophies will always be earned
        var tfp :TrophyFeederPanel;
        tfp = new TrophyFeederPanel(ctx, trophies, gameName, gameDescription, MODE_NEW);
        tfp.addCloseCallback(onClose);
        tfp.open();
    }

    /**
     * Shows previously earned trophies. In this mode, the panel always pops up and does not close
     * until the user closes it. When the user clicks a trophy, it is disabled for feedback. The
     * facebook popup can take up to about 30 seconds to appear.
     */
    public static function showExisting (ctx :MsoyContext, gameName :String,
        gameDescription :String, trophies :Array) :void
    {
        new TrophyFeederPanel(ctx, trophies, gameName, gameDescription, MODE_EXISTING).open();
    }

    /**
     * Assembles the argument list for a GWT trophy event and dispatches it.
     */
    public static function postTrophy (ctx :MsoyContext, trophy :Trophy, gameName :String,
        gameDesc :String, gameThumbnail :String, manual :Boolean) :void
    {
        ctx.getMsoyClient().dispatchEventToGWT(TROPHY_EVENT, [
            trophy.gameId, gameName, trophy.name, trophy.description,
            trophy.trophyMedia.getMediaPath(), gameDesc, trophy.ident, gameThumbnail,
            manual ]);
    }

    public function TrophyFeederPanel (
        ctx :MsoyContext, trophies :Array, gameName :String, gameDescription :String, mode :int)
    {
        super(ctx, Msgs.GAME.get("t.trophy_feeder"));
        _trophies = trophies;
        _gameName = gameName;
        _gameDescription = gameDescription;
        _mode = mode;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        const descriptionWidth :int = 200;

        var title :Text = new Text();
        title.text = Msgs.GAME.get(
            _mode == MODE_NEW ? "m.trophy_feeder_title_new" : "m.trophy_feeder_title_existing");
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
            tbtn.setCallback(postTrophyToFeed, [trophy, tbtn]);
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

    protected function postTrophyToFeed (trophy :Trophy, btn :CommandButton) :void
    {
        // TODO: get the game thumbnail. ugh
        var gameThumbnail :String = null;
        postTrophy(_ctx, trophy, _gameName, _gameDescription, gameThumbnail, true);
        btn.enabled = false;
        if (_mode == MODE_NEW) {
            close();
        }
    }

    protected var _trophies :Array /*of Trophy*/;
    protected var _gameName :String;
    protected var _gameDescription :String;
    protected var _mode :int;

    protected static const MODE_NEW :int = 0;
    protected static const MODE_EXISTING :int = 1;

    /** Event dispatched to GWT when the user clicks a trophy. */
    protected static const TROPHY_EVENT :String = "trophy";
}
}
