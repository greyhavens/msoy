//
// $Id$

package com.threerings.msoy.client {

import mx.binding.utils.BindingUtils;

import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;

import mx.containers.HBox;
import mx.containers.Grid;
import mx.containers.VBox;

import mx.core.UIComponent;

import com.threerings.util.ConfigValueSetEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.ui.FloatingPanel;

public class ChatPrefsDialog extends FloatingPanel
{
    public function ChatPrefsDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.chat_prefs"));
        styleName = "sexyWindow";
        showCloseButton = true;
        open(true);

        // listen for preferences changes that happen without us..
        Prefs.config.addEventListener(ConfigValueSetEvent.CONFIG_VALUE_SET,
            handlePrefsUpdated, false, 0, true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var tainer :VBox = new VBox();
        tainer.label = Msgs.PREFS.get("t.chat");

        var ii :int;
        var grid :Grid = new Grid();

        GridUtil.addRow(grid, Msgs.PREFS.get("l.chat_size"), createFontSizeControl());

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
            var hbox :HBox = new HBox();
            hbox.addChild(FlexUtil.createSpacer(20));
            hbox.addChild(but);
            GridUtil.addRow(grid, hbox, [2, 1]);
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

    protected function createFontSizeControl () :UIComponent
    {
        _fontTest = new FontTestArea();

        var hbox :HBox = new HBox();

        var bbox :VBox = new VBox();
        _upFont = new CommandButton(null, adjustFont, +1);
        _downFont = new CommandButton(null, adjustFont, -1);
//        _upFont.setStyle("fontSize", 13);
//        _downFont.setStyle("fontSize", 13);
//        _upFont.width = 35;
//        _downFont.width = 35;
        _upFont.styleName = "plusButton";
        _downFont.styleName = "minusButton";
        bbox.addChild(_upFont);
        bbox.addChild(_downFont);

        hbox.addChild(_fontTest);
        hbox.addChild(bbox);
        adjustFont(0); // jiggle everything into place..
        return hbox;
    }

    protected function adjustFont (delta :int) :void
    {
        var size :int = delta + Prefs.getChatFontSize();
        size = Math.max(Prefs.CHAT_FONT_SIZE_MIN, Math.min(Prefs.CHAT_FONT_SIZE_MAX, size));
        Prefs.setChatFontSize(size);

        _upFont.enabled = size < Prefs.CHAT_FONT_SIZE_MAX;
        _downFont.enabled = size > Prefs.CHAT_FONT_SIZE_MIN;
    }

    /**
     * Handle prefs that update some other way, and reflect the changes in the UI.
     */
    protected function handlePrefsUpdated (event :ConfigValueSetEvent) :void
    {
        switch (event.name) {
        case Prefs.CHAT_FONT_SIZE:
            _fontTest.reloadFont();
            break;
        }
    }

    /** A place where the currently configured chat font is tested. */
    protected var _fontTest :FontTestArea;

    protected var _upFont :CommandButton;
    protected var _downFont :CommandButton;
}
}

import flash.text.TextFormat;

import mx.controls.TextArea;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.chat.client.ChatOverlay;

class FontTestArea extends TextArea
{
    public function FontTestArea ()
    {
        text = Msgs.PREFS.get("m.chat_test");
        editable = false;
        minWidth = 200;
        minHeight = 50;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        reloadFont();
    }

    public function reloadFont () :void
    {
        var tf :TextFormat = ChatOverlay.createChatFormat();

        setStyle("fontSize", tf.size);
        setStyle("fontWeight", tf.bold ? "bold" : "normal");
        setStyle("textAlign", tf.align);
        setStyle("fontStyle", tf.italic ? "italic" : "normal");
        setStyle("color", 0xFFFFFF);
        setStyle("fontFamily", tf.font);
    }
}
