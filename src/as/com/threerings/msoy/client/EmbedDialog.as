//
// $Id$

package com.threerings.msoy.client {

import flash.system.System;

import mx.controls.TextArea;
import mx.controls.Text;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.MsoyContext;

public class EmbedDialog extends FloatingPanel
{
    public function EmbedDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.embed_link_window"));
        showCloseButton = true;

        setStyle("horizontalAlign", "center");

        var url :String = ctx.getTopPanel().root.loaderInfo.loaderURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");
        var embedCode :String = Msgs.GENERAL.get("m.embed", ctx.getMsoyController().getSceneIdString(), url);

        var instruction :Text = new Text();
        instruction.width = 300;
        instruction.text = Msgs.GENERAL.get("l.embed_instruction");
        instruction.selectable = false;
        addChild(instruction);

        var html :TextArea = new TextArea();
        html.minHeight = 100;
        html.width = 300;
        html.editable = false;
        html.text = embedCode;
        addChild(html);

        addButtons(new CommandButton(
            Msgs.GENERAL.get("b.copy_to_clipboard"), System.setClipboard, embedCode),
            OK_BUTTON);
        setButtonWidth(0); // free-size
        open(true);
    }
}
}
