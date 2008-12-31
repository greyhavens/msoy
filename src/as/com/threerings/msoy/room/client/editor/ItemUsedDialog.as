//
// $Id$

package com.threerings.msoy.room.client.editor {

import mx.core.ScrollPolicy;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.TextArea;

import com.threerings.flex.CommandButton;

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
        buttons.addChild(new CommandButton(Msgs.EDITING.get("b.item_used_yes"), closeWithYes));
        var spacer :HBox = new HBox();
        spacer.percentWidth = 100;
        buttons.addChild(spacer);
        buttons.addChild(new CommandButton(Msgs.EDITING.get("b.item_used_no"), close));
        content.addChild(buttons);
    }

    protected function closeWithYes () :void
    {
        _yesClosure();
        close();
    }

    protected var _yesClosure :Function;
    protected var _type :String;
}
}
