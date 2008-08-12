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
import com.threerings.flex.FlexUtil;
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
        // TODO: guests can't abuse the email thingy?
//        if (!_memObj.isGuest()) {
            cord.addChild(createEmailBox());
//        }
        cord.addChild(createLinkBox());
        cord.addChild(createEmbedBox());

        addChild(cord);

        //addButtons(_copyBtn, OK_BUTTON);
        //setButtonWidth(0); // free-size
        open(false);
    }

    public function getEmbedCode (size :int) :String
    {
        _memObj = _ctx.getClient().getClientObject() as MemberObject;

        var url :String = DeploymentConfig.serverURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");
        
        var affiliate :String = _memObj.isGuest() ? "" : String(_memObj.getMemberId());
        var flashVars :String = "";
        const sceneAndGame :Array = _ctx.getMsoyController().getSceneAndGame();
        var vector :String;
        if (sceneAndGame[0] != 0) {
            flashVars += "sceneId=" + sceneAndGame[0];
            vector = TrackingCookie.ROOM_VECTOR;

        } else if (sceneAndGame[1] != 0) {
            // TODO: go to the game's whirled, not just the lobby
            flashVars += "gameLobby=" + sceneAndGame[1];
            vector = TrackingCookie.GAME_VECTOR;

        } else {
            vector = TrackingCookie.GENERIC_VECTOR;
        }
        flashVars += "&" + TrackingCookie.makeFlashVars(affiliate, vector, "");

        if (size == 0) {
            flashVars += "&featuredPlace=true";
        }

        return Msgs.GENERAL.get("m.embed", flashVars, url,
            EMBED_SIZES[size][0], EMBED_SIZES[size][1]);
    }

    protected function createEmailBox () :VBox
    {
        var box :VBox = new VBox();
        box.label = "Email this room"; // TODO!
        box.percentWidth = 100;
        box.percentHeight = 100;

        box.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("l.email_addresses")));

        var emails :TextInput = new TextInput();
        box.addChild(emails);

        box.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("l.email_message")));

        var message :TextArea = new TextArea();
        box.addChild(message);

        var send :CommandButton = new CommandButton(null, function () :void {
            _ctx.getMsoyController().handleEmailShare(emails.text, message);
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
        box.label = "Grab the link"; // TODO
        box.percentWidth = 100;
        box.percentHeight = 100;

        var info :Label = new Label();
        info.width = 300;
        info.text = Msgs.GENERAL.get("l.link_instruction");
        box.addChild(info);

        var url :String = DeploymentConfig.serverURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/");
        const sceneAndGame :Array = _ctx.getMsoyController().getSceneAndGame();
        if (sceneAndGame[0] != 0) {
            url += "#world-s" + sceneAndGame[0];

        } else if (sceneAndGame[1] != 0) {
            url += "#world-game_g_" + sceneAndGame[1];
        }

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

        // TODO: review once we figure some shit out
        // the small scenes cannot host non-rooms, at least for now
        const sceneAndGame :Array = _ctx.getMsoyController().getSceneAndGame();
        if (sceneAndGame[0] != 0) {
            GridUtil.addRow(grid,
                createSizeButton(code, 0), createSizeButton(code, 1), initialSize);

        } else {
            // games may only be in the large size, for now
            GridUtil.addRow(grid, initialSize);
        }

        initialSize.selected = true;

        box.addChild(grid);

        var info :Label = FlexUtil.createLabel(Msgs.GENERAL.get("l.embed_instruction"));
        info.width = 300;
        info.selectable = false;
        box.addChild(info);

        box.addChild(new CopyableText(code));

        return box;
    }

    protected static const EMBED_SIZES :Array = [
        [350, 200], [400, 415], ["100%", 575]
    ];

    protected var _memObj :MemberObject;
}
}
