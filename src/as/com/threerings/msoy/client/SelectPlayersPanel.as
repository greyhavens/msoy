//
// $Id$

package com.threerings.msoy.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

import mx.containers.Grid;
import mx.containers.GridRow;
import mx.controls.CheckBox;
import mx.controls.Label;
import mx.controls.Text;
import mx.core.ScrollPolicy;

import com.threerings.util.Log;

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
        new SelectPlayersPanel(ctx, playerNames).maybeOpen();
    }

    public function SelectPlayersPanel (ctx :MsoyContext, playerNames :Array /* of VizMemberName */)
    {
        super(ctx, getPanelTitle());
        _playerNames = playerNames;
    }

    /**
     * Opens the dialog if the user has not previously asked "not to show again".
     */
    public function maybeOpen () :void
    {
        if (getPrefsName() != null && !Prefs.getAutoshow(getPrefsName())) {
            Log.getLog(this).info("Suppressing player selection dialog", "name", getPrefsName());
            return;
        }
        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var title :Label = new Label();
        title.text = getTitle();
        title.styleName = "selectPlayersPanelTitle";
        title.percentWidth = 100;
        addChild(title);

        var tip :Text = new Text();
        tip.text = getTip();
        tip.styleName = "selectPlayersPanelTip";
        tip.percentWidth = 100;
        addChild(tip);

        _status = new Label();
        _status.styleName = "selectPlayersPanelStatus";
        _status.percentWidth = 100;
        addChild(_status);

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

            var playerBox :PlayerBox = new PlayerBox(playerName, updateStatus);
            _playerBoxes.push(playerBox);
            GridUtil.addToRow(row, playerBox);
            if (++cell % 2 == 0) {
                row = null;
            }
        }

        if (allowFutureSuppression()) {
            _dontShowAgain = new CheckBox();
            _dontShowAgain.styleName = "selectPlayersPanelDontShowAgain";
            _dontShowAgain.label = "Don't show this dialog again";
            _dontShowAgain.percentWidth = 100;
            addChild(_dontShowAgain);
        }

        addButtons(CANCEL_BUTTON, OK_BUTTON);

        updateStatus();
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        super.buttonClicked(buttonId);

        if (_dontShowAgain != null && _dontShowAgain.selected && getPrefsName() != null) {
            Prefs.setAutoshow(getPrefsName(), false);
        }
    }

    protected function updateStatus () :void
    {
        var count :int = 0;
        for each (var box :PlayerBox in _playerBoxes) {
            if (box.isSelected()) {
                count++;
            }
        }
        _status.text = Msgs.GENERAL.get("m.selected_players", count);
        getButton(OK_BUTTON).enabled = count > 0;
    }

    protected function getPanelTitle () :String
    {
        return "Title";
    }

    protected function getTitle () :String
    {
        return "Select some players...";
    }

    protected function getTip () :String
    {
        return Msgs.GENERAL.get("p.select_players");
    }

    protected function getOkLabel () :String
    {
        return "Do It";
    }

    protected function getCancelLabel () :String
    {
        return "Skip";
    }

    protected function allowFutureSuppression () :Boolean
    {
        return true;
    }

    protected function getPrefsName () :String
    {
        return null;
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
    protected var _dontShowAgain :CheckBox;
    protected var _status :Label;
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
    public function PlayerBox (name :VizMemberName, onSelChange :Function)
    {
        _name = name;
        _onSelChange = onSelChange;
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
        _onSelChange();
    }

    protected var _name :VizMemberName;
    protected var _onSelChange :Function;
    protected var _selected :Boolean;
}
