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
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.TrophySource;

/**
 * Displays all of the trophies available in a game along with info on earned trophies if the
 * player is logged in.
 */
public class TrophyPanel extends FloatingPanel
{
    public static function show (ctx :GameContext, gameId :int, gameName :String) :void
    {
        (ctx.getClient().requireService(GameGameService) as GameGameService).getTrophies(
            ctx.getClient(), gameId, ctx.getMsoyContext().resultListener(
                function (trophies :TypedArray) :void {
                    new TrophyPanel(ctx, trophies, gameName).open();
                }, MsoyCodes.GAME_MSGS));
    }

    public function TrophyPanel (ctx :GameContext, trophies :Array, gameName :String)
    {
        super(ctx.getMsoyContext(), Msgs.GAME.get("t.trophy"));
        _gctx = ctx;
        _trophies = trophies;
        _gameName = gameName;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        if (_trophies.length == 0) {
            var none :Label = new Label();
            none.text = Msgs.GAME.get("m.tp_title_none", _gameName);
            addChild(none);
            addButtons(CANCEL_BUTTON);
            return;
        }

        var title :Label = new Label();
        title.text = Msgs.GAME.get("m.tp_title", _gameName);
        title.styleName = "trophyPanelTitle";
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
            name.styleName = "trophyPanelName";
            name.text = trophy.name;
            bits.addChild(name);
            var descrip :Text = new Text();
            descrip.width = 200;
            descrip.text = (trophy.description == null) ?
                Msgs.GAME.get("m.tp_secret") : trophy.description;
            bits.addChild(descrip);
            tbox.addChild(bits);
            if (trophy.description == null) {
                descrip.styleName = "trophyPanelHidden";
            }
            if (trophy.whenEarned == null) {
                tbox.alpha = 0.35;
                name.setStyle("color", 0xAAAAAA);
                descrip.setStyle("color", 0xAAAAAA);
            }
            GridUtil.addToRow(row, tbox);
            if (++cell % 2 == 0) {
                row = null;
            }
        }

        addButtons(CANCEL_BUTTON);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        return Msgs.GENERAL.get("b.ok");
    }

    protected var _gctx :GameContext;
    protected var _trophies :Array /*of Trophy*/;
    protected var _gameName :String;
}
}
