//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Button;
import mx.controls.TextArea;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.world.client.WorldContext;

public class ItemUsedDialog extends FloatingPanel 
{
    public function ItemUsedDialog (ctx :WorldContext, type :String, yesClosure :Function) 
    {
        super(ctx, Msgs.EDITING.get("t.item_used"));
        _yesClosure = yesClosure;
        _type = type;

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var content :VBox = new VBox();
        content.percentWidth = 100;
        content.percentHeight = 100;
        addChild(content);

        // display message
        var text :TextArea = new TextArea();
        text.width = 250;
        text.height = 75;
        text.verticalScrollPolicy = ScrollPolicy.OFF;
        text.setStyle("borderStyle", "none");
        text.editable = false;
        text.selectable = false;
        text.text = Msgs.EDITING.get("m.item_used", _type);
        content.addChild(text);

        // add buttons
        var buttons :HBox = new HBox();
        buttons.percentWidth = 100;
        var button :Button = new Button();
        button.label = Msgs.EDITING.get("b.item_used_yes");
        button.addEventListener(MouseEvent.CLICK, onButton);
        buttons.addChild(button);
        var spacer :HBox = new HBox();
        spacer.percentWidth = 100;
        buttons.addChild(spacer);
        button = new Button();
        button.label = Msgs.EDITING.get("b.item_used_no");
        button.addEventListener(MouseEvent.CLICK, onButton);
        buttons.addChild(button);
        content.addChild(buttons);
    }

    protected function onButton (evt :MouseEvent) :void
    {
        var button :Button = evt.target as Button;
        if (button != null && button.label == Msgs.EDITING.get("b.item_used_yes")) {
            _yesClosure();
        }
        close();
    }

    protected var _yesClosure :Function;
    protected var _type :String;
}
}
