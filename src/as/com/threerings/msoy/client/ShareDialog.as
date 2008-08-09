//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.events.FlexEvent;

import mx.containers.Accordion;
import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.TitleWindow;

import mx.controls.RadioButton;
import mx.controls.Label;
import mx.controls.TextArea;
import mx.controls.TextInput;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.FloatingPanel;

public class ShareDialog extends FloatingPanel
{
    public function ShareDialog (ctx :MsoyContext)
    {
        super(ctx);
        showCloseButton = true;

        styleName = "shareWindow";
        //setStyle("horizontalAlign", "center");

        var cord :Accordion = new Accordion();

        cord.width = 323;
        cord.height = 250;
        cord.addChild(createEmailBox());
        cord.addChild(createLinkBox());
        cord.addChild(createEmbedBox());

        addChild(cord);

        //addButtons(_copyBtn, OK_BUTTON);
        //setButtonWidth(0); // free-size
        open(false);
    }

    public function getEmbedCode (size :int) :String
    {
        var url :String = _ctx.getTopPanel().root.loaderInfo.loaderURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");
        
        var memberObject :MemberObject = _ctx.getClient().getClientObject() as MemberObject;
        var affiliate :String = memberObject ? memberObject.getMemberId().toString() : "";
        var flashVars :String = "sceneId=" + _ctx.getMsoyController().getSceneIdString() + 
            "&" + TrackingCookie.makeFlashVars(affiliate, TrackingCookie.ROOM_VECTOR, "");

        return Msgs.GENERAL.get("m.embed", flashVars, url, EMBED_SIZES[size][0], EMBED_SIZES[size][1]);
    }

    protected function createEmailBox () :VBox
    {
        var box :VBox = new VBox();
        box.label = "Email this room";
        box.percentWidth = 100;
        box.percentHeight = 100;

        var info :Label = new Label();
        info.text = Msgs.GENERAL.get("l.email_addresses");
        box.addChild(info);

        var emails :TextInput = new TextInput();
        box.addChild(emails);

        info = new Label();
        info.text = Msgs.GENERAL.get("l.email_message");
        box.addChild(info);

        var message :TextArea = new TextArea();
        box.addChild(message);

        var send :CommandButton = new CommandButton(null, function () :void {
            _ctx.getMsoyController().handleEmailShare(emails, message);
            close();
        });
        send.styleName = "sendButton";

        box.addChild(send);
        box.defaultButton = send;

        return box;
    }

    protected function createLinkBox () :VBox
    {
        var box :VBox = new VBox();
        box.label = "Grab the link";
        box.percentWidth = 100;
        box.percentHeight = 100;

        var info :Label = new Label();
        info.width = 300;
        info.text = Msgs.GENERAL.get("l.link_instruction");
        box.addChild(info);

        var url :String = _ctx.getTopPanel().root.loaderInfo.loaderURL;
        // TODO: Proper way to get the URL for both rooms and games
        url = url.replace(/(http:\/\/[^\/]*).*/,
            "$1/#world-s" + _ctx.getMsoyController().getSceneIdString());

        box.addChild(new CopyableText(url));

        return box;
    }

    protected function createSizeButton (code :TextInput, size :int) :RadioButton
    {
        var button :RadioButton = new RadioButton();
        button.groupName = "embedSize";
        //button.styleName = "embedSize" + size;
        button.label = Msgs.GENERAL.get("l.embed_size" + size);
        button.labelPlacement = "top";

        button.addEventListener(FlexEvent.VALUE_COMMIT, function (... ignored) :void {
            if (button.selected) {
                code.text = getEmbedCode(size);
            }
        });

        return button;
    }

    protected function createEmbedBox () :VBox
    {
        var box :VBox = new VBox();
        box.percentWidth = 100;
        box.percentHeight = 100;
        box.label = "Embed";

        var choose :Label = new Label();
            choose.text = Msgs.GENERAL.get("l.embed_choose_size");
        box.addChild(choose);

        var code :TextInput = new TextInput();

        var grid :Grid = new Grid();
        var initialSize :RadioButton = createSizeButton(code, 2);
        grid.percentWidth = 100;
        //GridUtil.addRow(grid, "small", "medium", "large");
        GridUtil.addRow(grid,
            createSizeButton(code, 0), createSizeButton(code, 1), initialSize);

        initialSize.selected = true;

        box.addChild(grid);

        var info :Label = new Label();
            info.width = 300;
            info.text = Msgs.GENERAL.get("l.embed_instruction");
            info.selectable = false;
        box.addChild(info);

        box.addChild(new CopyableText(code));

        return box;
    }

    protected static const EMBED_SIZES :Array = [
        [320, 240], [800, 550], ["100%", 550]
    ];
}
}
