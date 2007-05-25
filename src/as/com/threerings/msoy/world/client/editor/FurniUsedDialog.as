//
// $Id$

package com.threerings.msoy.world.client.editor {

import mx.core.ScrollPolicy;

import mx.containers.VBox;

import mx.controls.TextArea;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

public class FurniUsedDialog extends FloatingPanel 
{
    public function FurniUsedDialog (ctx :WorldContext, yesClosure :Function) 
    {
        super(ctx, Msgs.EDITING.get("t.furni_used"));
        _yesClosure = yesClosure;

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
        text.width = 300;
        text.height = 50;
        text.verticalScrollPolicy = ScrollPolicy.OFF;
        text.setStyle("borderStyle", "none");
        text.editable = false;
        text.selectable = false;
        text.text = Msgs.EDITING.get("m.furni_used", /* TODO */ "ITEM_LOCATION_NAME");
        content.addChild(text);

        // add buttons
    }

    protected var _yesClosure :Function;
}
}
