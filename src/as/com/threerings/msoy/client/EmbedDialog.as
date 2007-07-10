//
// $Id$

package com.threerings.msoy.client {

import mx.controls.TextArea;
import mx.controls.Text;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.WorldContext;

public class EmbedDialog extends FloatingPanel
{
    public function EmbedDialog (ctx :WorldContext)
    {
        super(ctx, Msgs.GENERAL.get("t.embed_link_window"));

        setStyle("horizontalAlign", "center");

        var instruction :Text = new Text();
        instruction.width = 300;
        instruction.text = Msgs.GENERAL.get("l.embed_instruction");
        instruction.selectable = false;
        addChild(instruction);

        var html :TextArea = new TextArea();
        html.minHeight = 100;
        html.width = 300;
        html.editable = false;
        var url :String = ctx.getTopPanel().root.loaderInfo.loaderURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");
        html.text = Msgs.GENERAL.get("m.embed", ctx.getMsoyController().getSceneIdString(), url);
        addChild(html);
        addButtons(FloatingPanel.OK_BUTTON);

        open(true);
    }
}
}
