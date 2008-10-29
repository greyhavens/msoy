//
// $Id$

package com.threerings.msoy.client {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.events.MouseEvent;

import flash.net.FileReference;
import flash.net.URLRequest;

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

import mx.core.UIComponent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;
import com.threerings.io.TypedArray;
import com.threerings.util.MailUtil;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.FloatingPanel;

public class ShareDialog extends FloatingPanel
{
    public function ShareDialog (ctx :MsoyContext)
    {
        super(ctx);

        // find out about our current place
        var pinfo :Array = _ctx.getMsoyController().getPlaceInfo();
        _inGame = Boolean(pinfo[0]);
        _placeName = (pinfo[1] as String);
        _placeId = int(pinfo[2]);

        title = Msgs.GENERAL.get(_inGame ? "t.share_game" : "t.share_room");
        showCloseButton = true;
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

        if (_ctx.getMsoyController().canManagePlace()) {
            cord.addChild(createStubBox());
            cord.height += 35;
        }

        addChild(cord);

        //addButtons(_copyBtn, OK_BUTTON);
        //setButtonWidth(0); // free-size
        open(false);
    }

    public function getEmbedCode (size :int) :String
    {
        var flashVars :String = VisitorInfo.makeFlashVars(_placeId, _inGame);
        // if we're not a guest, include the "aff=<memberId>" flashvar to affiliate users to us
        const memName :MemberName = _ctx.getMyName();
        if (!memName.isGuest()) {
            flashVars += "&aff=" + memName.getMemberId();
        }
        if (size == 0) { // mini TV view
            flashVars += "&featuredPlace=true";
        }

        const fullLink :String = _ctx.getMsoyController().createPageLink("", false);
        const swfUrl :String = DeploymentConfig.serverURL.replace(
            /(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");

        return Msgs.GENERAL.get("m.embed", flashVars, swfUrl,
            EMBED_SIZES[size][0], EMBED_SIZES[size][1], fullLink);
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

    protected function createStubBox () :VBox
    {
        const roomOrGame :String = (_inGame ? "game" : "room");
        var stubArgs :String = roomOrGame + "=" + _placeId;
        const memName :MemberName = _ctx.getMyName();
        if (!memName.isGuest()) {
            stubArgs += "&aff=" + memName.getMemberId();
        }
        const url :String = DeploymentConfig.serverURL + "stubdlsvc?args=" +
            encodeURIComponent(stubArgs);

        var box :VBox = createContainer("t.stub_share");
        box.addChild(FlexUtil.createText(Msgs.GENERAL.get("m.stub_share"), 300));
        _downloadBtn = new CommandButton(Msgs.GENERAL.get("b.stub_share"),
            startDownload, [ url, "Whirled-" + roomOrGame + "-" + _placeId + "-stub.swf" ]);
        box.addChild(_downloadBtn);
        box.addChild(_downloadError = FlexUtil.createLabel(""));
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

        var page :String;
        if (_inGame) {
            page = "world-game_l_" + _placeId;
        } else if (_placeId != 0) {
            page = "world-s" + _placeId;
        }
        const url :String = _ctx.getMsoyController().createPageLink(page, false);
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
        if (!_inGame) {
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
            if (email.length == 0 || emails.indexOf(email) >= 0) {
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
            _ctx.getClient(), _inGame, _placeName, _placeId, emails, message,
            new ReportingListener(_ctx, MsoyCodes.GENERAL_MSGS, null, "m.share_email_sent"));

        close(); // and make like the proverbial audi 5000
    }

    protected function startDownload (url :String, localFile :String) :void
    {
        _downloadBtn.enabled = false;
        _downloadError.text = "";

        _fileRef = new FileReference();
        _fileRef.addEventListener(Event.CANCEL, handleDownloadStopEvent);
        _fileRef.addEventListener(Event.COMPLETE, handleDownloadStopEvent);
        _fileRef.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleDownloadStopEvent);
        _fileRef.addEventListener(IOErrorEvent.IO_ERROR, handleDownloadStopEvent);
        _fileRef.download(new URLRequest(url), localFile);
    }

    protected function handleDownloadStopEvent (event :Event) :void
    {
        _downloadBtn.enabled = true;
        if (event is ErrorEvent) {
            _downloadError.text = ErrorEvent(event).text;
        }
    }

    protected var _inGame :Boolean;
    protected var _placeName :String;
    protected var _placeId :int;

    /** We need to keep this in scope or the download will halt. */
    protected var _fileRef :FileReference;

    protected var _sizeGroup :RadioButtonGroup = new RadioButtonGroup();
    protected var _status :Text;

    protected var _downloadBtn :CommandButton;
    protected var _downloadError :Label;

    protected static const EMBED_SIZES :Array = [
        [350, 200], [400, 415], [700, 575], ["100%", 575]
    ];

    // Default to full 100% size
    protected static const DEFAULT_SIZE :int = 3;
}
}
