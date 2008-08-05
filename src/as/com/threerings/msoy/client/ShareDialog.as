//
// $Id$

package com.threerings.msoy.client {

import flash.system.System;

import flash.events.Event;
import flash.events.FocusEvent;
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

    protected function createCopyable (field :TextInput) :HBox
    {
        var box :HBox = new HBox();
        box.percentWidth = 100;

        field.editable = false;
        field.percentWidth = 100;

        // On focus, select all
        field.addEventListener(FocusEvent.FOCUS_IN, function (... ignored) :void {
            field.selectionBeginIndex = 0;
            field.selectionEndIndex = field.text.length;
        });

        box.addChild(field);

        var button :CommandButton = new CommandButton(null, function () :void {
            System.setClipboard(field.text);
        });
        button.styleName = "copyButton";

        box.addChild(button);
        box.defaultButton = button;

        return box;
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

        // TODO: Close the dialog on submit
        var ok :CommandButton = new CommandButton(
            Msgs.GENERAL.get("b.email_ok"), MsoyController.EMAIL_SHARE, [emails, message]);

        box.addChild(ok);
        box.defaultButton = ok;

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

        var field :TextInput = new TextInput();
        field.text = url;
        box.addChild(createCopyable(field));

        return box;
    }

    protected function createSizeButton (code :TextInput, size :int) :RadioButton
    {
        var button :RadioButton = new RadioButton();
        button.groupName = "embed_size";
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

        box.addChild(createCopyable(code));

        return box;
    }

    protected static const EMBED_SIZES :Array = [
        [320, 240], [800, 550], ["100%", 550]
    ];
}
}
