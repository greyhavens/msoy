//
// $Id$

package com.threerings.msoy.client {

import mx.binding.utils.BindingUtils;

import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;

import mx.containers.Grid;
import mx.containers.VBox;

import com.threerings.flex.GridUtil;
import com.threerings.msoy.ui.FloatingPanel;

public class ChatPrefsDialog extends FloatingPanel
{
    public function ChatPrefsDialog (ctx :WorldContext)
    {
        super(ctx, Msgs.GENERAL.get("t.chat_prefs"));
        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var tainer :VBox = new VBox();
        tainer.label = Msgs.PREFS.get("t.chat");

        var ii :int;
        var grid :Grid = new Grid();

        var history :CheckBox = new CheckBox();
        history.selected = Prefs.getShowingChatHistory();
        BindingUtils.bindSetter(Prefs.setShowingChatHistory, history, "selected");
        GridUtil.addRow(grid, Msgs.PREFS.get("l.chat_history"), history);

        var decay :ComboBox = new ComboBox();
        var choices :Array = [];
        for (ii = 0; ii < 3; ii++) {
            choices.push(Msgs.PREFS.get("l.chat_decay" + ii));
        }
        decay.dataProvider = choices;
        decay.selectedIndex = Prefs.getChatDecay();
        BindingUtils.bindSetter(Prefs.setChatDecay, decay, "selectedIndex");
        GridUtil.addRow(grid, Msgs.PREFS.get("l.chat_decay"), decay);

        GridUtil.addRow(grid, Msgs.PREFS.get("l.chat_filter"), [2, 1]);
        var filterGroup :RadioButtonGroup = new RadioButtonGroup();
        var filterChoice :int = Prefs.getChatFilterLevel();
        for (ii= 0; ii < 4; ii++) {
            var but :RadioButton = new RadioButton();
            but.label = Msgs.PREFS.get("l.chat_filter" + ii);
            but.selected = (ii == filterChoice);
            but.value = ii;
            but.group = filterGroup;
            GridUtil.addRow(grid, but, [2, 1]);
        }
        BindingUtils.bindSetter(Prefs.setChatFilterLevel, filterGroup, "selectedValue");

        var lbl :Label = new Label();
        lbl.text = Msgs.PREFS.get("m.chat_filter_note");
        lbl.setStyle("fontSize", 8);
        GridUtil.addRow(grid, lbl, [2, 1]);

        tainer.addChild(grid);
        addChild(tainer);

        addButtons(OK_BUTTON);
    }
}
}
