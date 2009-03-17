//
// $Id$

package com.threerings.msoy.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import mx.containers.Grid;
import mx.containers.GridRow;
import mx.controls.Label;
import mx.core.ScrollPolicy;

import com.threerings.flex.GridUtil;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.data.all.VizMemberName;

/**
 * Panel showing a list of players that the user can select. Subclasses provide specific
 * functionality for what happens when the user selects "ok" or "cancel" as well as the text for
 * those actions.
 */
public class SelectPlayersPanel extends FloatingPanel
{
    public static function show (ctx :MsoyContext, playerNames :Array /* of VizMemberName */) :void
    {
        new SelectPlayersPanel(ctx, playerNames).open();
    }

    public function SelectPlayersPanel (ctx :MsoyContext, playerNames :Array /* of VizMemberName */)
    {
        super(ctx, getTitle());
        _playerNames = playerNames;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var title :Label = new Label();
        title.text = getTip();
        title.styleName = "selectPlayersPanelTitle";
        title.percentWidth = 100;
        addChild(title);

        var grid :Grid = new Grid();
        grid.maxHeight = 400;
        grid.horizontalScrollPolicy = ScrollPolicy.OFF;
        grid.verticalScrollPolicy = ScrollPolicy.AUTO;
        addChild(grid);

        var row :GridRow = null;
        var cell :int = 0;
        for each (var playerName :VizMemberName in _playerNames) {
            if (row == null) {
                row = new GridRow();
                grid.addChild(row);
            }

            var playerBox :PlayerBox = new PlayerBox(playerName);
            _playerBoxes.push(playerBox);
            GridUtil.addToRow(row, playerBox);
            if (++cell % 2 == 0) {
                row = null;
            }
        }

        addButtons(CANCEL_BUTTON, OK_BUTTON);
    }

    protected function getTitle () :String
    {
        return "Title";
    }

    protected function getTip () :String
    {
        return "Select some players...";
    }

    protected function getOkLabel () :String
    {
        return "Do It";
    }

    protected function getCancelLabel () :String
    {
        return "Skip";
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        if (buttonId == CANCEL_BUTTON) {
            return getCancelLabel();
        } else if (buttonId == OK_BUTTON) {
            return getOkLabel();
        } else {
            return "";
        }
    }

    protected var _playerNames :Array /*of VizMemberName*/;
    protected var _playerBoxes :Array /*of PlayerBox */ = [];
}
}

import flash.events.MouseEvent;

import mx.containers.HBox;
import mx.controls.Label;
import mx.core.ScrollPolicy;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VizMemberName;

class PlayerBox extends HBox
{
    public function PlayerBox (name :VizMemberName)
    {
        _name = name;
        styleName = "selectPlayersPanelPlayerBox";
        width = 200;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        addEventListener(MouseEvent.CLICK, handleClick);
    }

    public function isSelected () :Boolean
    {
        return _selected;
    }

    override protected function createChildren () :void
    {
        addChild(MediaWrapper.createView(_name.getPhoto(), MediaDesc.HALF_THUMBNAIL_SIZE));

        var label :Label = new Label();
        label.styleName = "selectPlayersPanelPlayerName";
        label.text = _name.toString();
        addChild(label);
    }

    protected function handleClick (evt :MouseEvent) :void
    {
        _selected = !_selected;
        if (_selected) {
            graphics.beginFill(0x3fa3cc);
            graphics.drawRoundRect(0, 0, width, height, 12);
            graphics.endFill();
        } else {
            graphics.clear();
        }
    }

    protected var _name :VizMemberName;
    protected var _selected :Boolean;
}
