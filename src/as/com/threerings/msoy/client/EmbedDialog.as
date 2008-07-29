//
// $Id$

package com.threerings.msoy.client {

import flash.system.System;

import mx.controls.Text;
import mx.controls.TextArea;

import com.threerings.flex.CommandButton;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.ui.FloatingPanel;

public class EmbedDialog extends FloatingPanel
{
    public function EmbedDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.embed_link_window"));
        showCloseButton = true;

        setStyle("horizontalAlign", "center");

        var url :String = ctx.getTopPanel().root.loaderInfo.loaderURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");
        
        // embed tracking: memberId is the affiliate and "room" as the vector
        var memberObject :MemberObject = ctx.getClient().getClientObject() as MemberObject;
        var affiliate :String = memberObject ? memberObject.getMemberId().toString() : "";
        var flashVars :String = "sceneId=" + ctx.getMsoyController().getSceneIdString() + 
            "&" + TrackingCookie.makeFlashVars(affiliate, TrackingCookie.ROOM_VECTOR, "");
        
        var embedCode :String = Msgs.GENERAL.get("m.embed", flashVars, url, "100%", "600");

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
