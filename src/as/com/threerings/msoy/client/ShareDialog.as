//
// $Id$

package com.threerings.msoy.client {

import flash.events.MouseEvent;

import mx.events.FlexEvent;

import mx.containers.Accordion;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.controls.Text;
import mx.controls.TextArea;
import mx.controls.TextInput;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;
import com.threerings.io.TypedArray;
import com.threerings.util.MailUtil;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.FloatingPanel;

public class ShareDialog extends FloatingPanel
{
    public function ShareDialog (ctx :MsoyContext)
    {
        super(ctx);

        const sceneAndGame :Array = _ctx.getMsoyController().getSceneAndGame();
        _sceneId = int(sceneAndGame[0]);
        _gameId = int(sceneAndGame[1]);

        title = Msgs.GENERAL.get(_gameId == 0 ? "t.share_room" : "t.share_game");
        showCloseButton = true;
        styleName = "sexyWindow";
        setStyle("paddingTop", 0);
        setStyle("paddingBottom", 0);
        setStyle("paddingLeft", 0);
        setStyle("paddingRight", 0);

        var cord :Accordion = new Accordion();

        cord.resizeToContent = true;
        cord.width = 323;
        cord.height = 260;
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

    //
    // TODO: Everything below this line is a train wreck
    //       MXML?
    //

    public function getEmbedCode (size :int) :String
    {
        _memObj = _ctx.getClient().getClientObject() as MemberObject;

        var url :String = DeploymentConfig.serverURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");
        
        var affiliate :String = _memObj.isGuest() ? "" : String(_memObj.getMemberId());
        var flashVars :String = "";
        var vector :String;
        if (_sceneId != 0) {
            flashVars += "sceneId=" + _sceneId;
            vector = TrackingCookie.ROOM_VECTOR;

        } else if (_gameId != 0) {
            flashVars += "gameLobby=" + _gameId;
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
        var box :VBox = createContainer("t.email_share");

        box.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("l.email_addresses")));
        var emails :TextInput = new TextInput();
        emails.percentWidth = 100;
        box.addChild(emails);

        box.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("l.email_message")));
        var message :TextArea = new TextArea();
        message.percentWidth = 100;
        box.addChild(message);

        var row :HBox = new HBox();
        row.setStyle("horizontalAlign", "right");
        row.percentWidth = 100;

        row.addChild(_status = new Text());
        _status.percentWidth = 100;

        var send :CommandButton = new CommandButton(null, function () :void {
            sendShareEmail(emails.text, message.text);
        });
        send.styleName = "sendButton";
        row.addChild(send);

        box.addChild(row);
        box.defaultButton = send;

        return box;
    }

    protected function createContainer (key :String) :VBox
    {
        var box :VBox = new VBox();

        box.label = Msgs.GENERAL.get(key);
        box.percentWidth = 100;
        box.percentHeight = 100;

        box.setStyle("paddingLeft", 6);
        box.setStyle("paddingRight", 6);
        box.setStyle("paddingTop", 6);
        box.setStyle("paddingBottom", 6);

        return box;
    }

    protected function createLinkBox () :VBox
    {
        var box :VBox = createContainer("t.grab_link");
        box.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("l.link_instruction")));

        var url :String = DeploymentConfig.serverURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/");
        if (_gameId != 0) {
            url += "#world-game_l_" + _gameId;
        } else if (_sceneId != 0) {
            url += "#world-s" + _sceneId;
        }
        box.addChild(new CopyableText(url));

        return box;
    }

    protected function createSizeControl (size :int) :VBox
    {
        var box :VBox = new VBox();
        box.setStyle("horizontalAlign", "center");
        box.setStyle("verticalGap", 0);
        box.percentWidth = 100;

        var button :RadioButton = new RadioButton();
        button.group = _sizeGroup;
        button.value = size;
        button.labelPlacement = "top";

        var label :Label = FlexUtil.createLabel(Msgs.GENERAL.get("l.embed_size" + size));
        label.setStyle("fontWeight", "bold");
        box.addChild(label);

        box.addChild(button);

        label = FlexUtil.createLabel(Msgs.GENERAL.get("l.embed_dimensions", EMBED_SIZES[size]));
        label.setStyle("color", "gray");
        box.addChild(label);

        box.addEventListener(MouseEvent.CLICK, function (... ignored) :void {
            button.selected = true;
        });

        if (size == DEFAULT_SIZE) {
            button.selected = true;
        }

        return box;
    }

    protected function createEmbedBox () :VBox
    {
        var box :VBox = createContainer("t.embed");
        box.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("l.embed_choose_size")));

        var code :TextInput = new TextInput();
        var checks :HBox = new HBox();
        checks.percentWidth = 100;
        box.addChild(checks);

        // TODO: review once we figure some shit out
        // the small scenes cannot host non-rooms, at least for now
        if (_sceneId != 0) {
            for (var ii :int = 0; ii < EMBED_SIZES.length; ii++) {
                checks.addChild(createSizeControl(ii));
            }

        } else {
            // Games don't have all the options
            for (var jj :int = 2; jj < EMBED_SIZES.length; jj++) {
                checks.addChild(createSizeControl(jj));
            }
        }

        _sizeGroup.addEventListener(FlexEvent.VALUE_COMMIT, function (... ignored) :void {
            code.text = getEmbedCode(_sizeGroup.selectedValue as int);
        });

        var info :Label = FlexUtil.createLabel(Msgs.GENERAL.get("l.embed_instruction"));
        info.width = 300;
        info.selectable = false;
        box.addChild(info);

        box.addChild(new CopyableText(code));

        return box;
    }

    protected function sendShareEmail (emailText :String, message :String) :void
    {
        var emails :TypedArray = TypedArray.create(String);
        for each (var email :String in emailText.split(/[ ,]/)) {
            if (email.length == 0) {
                // skip it
            } else if (MailUtil.isValidAddress(email)) {
                emails.push(email);
            } else {
                _status.text = Msgs.GENERAL.get("e.invalid_addr", email);
            }
        }
        if (emails.length == 0) {
            return; // we either have no email addresses or bogus addresses, so stop here
        }

        // send the emails and messages off to the server for delivery
        (_ctx.getClient().requireService(MemberService) as MemberService).emailShare(
            _ctx.getClient(), _sceneId, _gameId, emails, message,
            new ReportingListener(_ctx, MsoyCodes.GENERAL_MSGS, null, "m.share_email_sent"));

        close(); // and make like the proverbial audi 5000
    }

    protected var _memObj :MemberObject;
    protected var _sceneId :int;
    protected var _gameId :int;
    protected var _sizeGroup :RadioButtonGroup = new RadioButtonGroup();
    protected var _status :Text;

    protected static const EMBED_SIZES :Array = [
        [350, 200], [400, 415], [700, 575], ["100%", 575]
    ];

    // Default to full 100% size
    protected static const DEFAULT_SIZE :int = 3;
}
}
