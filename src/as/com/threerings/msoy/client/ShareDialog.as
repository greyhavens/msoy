//
// $Id$

package com.threerings.msoy.client {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.events.MouseEvent;

import flash.external.ExternalInterface;

import flash.net.FileReference;
import flash.net.URLRequest;

import mx.events.FlexEvent;

import mx.containers.HBox;
import mx.containers.TabNavigator;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.controls.Text;
import mx.controls.TextArea;
import mx.controls.TextInput;

import mx.core.UIComponent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.FlexUtil;
import com.threerings.io.TypedArray;
import com.threerings.util.MailUtil;

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

        if (!ctx.getMyName().isGuest()) {
            addChild(FlexUtil.createText(Msgs.GENERAL.get("m.sharing"), 350));
            const affLink :CommandLinkButton = new CommandLinkButton(
                Msgs.GENERAL.get("b.sharing"), MsoyController.VIEW_URL,
                Msgs.GENERAL.get("u.affiliates"));
            affLink.styleName = "underLink";
            addChild(affLink);
        }

        var tabs :TabNavigator = new TabNavigator();
        tabs.styleName = "sexyTabber";
        tabs.setStyle("tabWidth", NaN);
        tabs.resizeToContent = true;
        tabs.width = 450;
        tabs.height = 200;

        tabs.addChild(createSocialBox());
        // TODO: guests can't abuse the email thingy?
//        if (!_memObj.isGuest()) {
            tabs.addChild(createEmailBox());
//        }
        tabs.addChild(createLinkBox());
        tabs.addChild(createEmbedBox());

        if (_ctx.getMsoyController().canManagePlace()) {
            tabs.addChild(createStubBox());
        }

        addChild(tabs);

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

    protected function createSocialBox () :VBox
    {
        var box :VBox = createContainer("t.social_share");
        box.setStyle("horizontalAlign", "center");

        box.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("m.social_share")));

        var row :HBox = new HBox();
        row.percentWidth = 100;
        row.setStyle("horizontalGap", 20);

        row.addChild(createSocialButton("b.share_facebook", 
            "http://b.static.ak.fbcdn.net/images/share/facebook_share_icon.gif?8:26981",
            popFacebook));
        row.addChild(createSocialButton("b.share_myspace", 
            "http://cms.myspacecdn.com/cms/post_myspace_icon.gif",
            popMyspace));

        box.addChild(row);
        return box;
    }

    protected function createSocialButton (
        msg :String, iconURL :String, callback :Function) :UIComponent
    {
        var box :HBox = new HBox();
        box.setStyle("horizontalGap", 0);

        var img :CommandButton = new CommandButton(null, callback);
        img.styleName = "imageButton";
        img.setStyle("image", iconURL);
        box.addChild(img);
        var link :CommandLinkButton = new CommandLinkButton(Msgs.GENERAL.get(msg), callback);
        link.styleName = "underLink"; // TODO: god it irks me that the default is un-underlined.
        box.addChild(link);
        return box;
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

        var send :CommandButton = new CommandButton(Msgs.GENERAL.get("b.send"), function () :void {
            sendShareEmail(emails.text, message.text);
        });
        send.styleName = "orangeButton";
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
        box.setStyle("horizontalAlign", "center");
        box.addChild(FlexUtil.createText(Msgs.GENERAL.get("m.stub_share"), 350));
        _downloadBtn = new CommandButton(Msgs.GENERAL.get("b.stub_share"),
            startDownload, [ url, "Whirled-" + roomOrGame + "-" + _placeId + "-stub.swf" ]);
        _downloadBtn.styleName = "orangeButton";
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
        box.addChild(new CopyableText(createLink()));
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
            _ctx.confirmListener("m.share_email_sent"));

        close(); // and make like the proverbial audi 5000
    }

    /**
     * Return a shareable link to this place.
     */
    protected function createLink () :String
    {
        var page :String;
        if (_inGame) {
            page = "world-game_l_" + _placeId;
        } else if (_placeId != 0) {
            page = "world-s" + _placeId;
        }
        return _ctx.getMsoyController().createPageLink(page, false);
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

    protected function getShareTitle () :String
    {
        return Msgs.GENERAL.get(_inGame ? "m.social_share_game" : "m.social_share_room");
    }

    /**
     * http://www.facebook.com/share_partners.php
     */
    protected function popFacebook () :void
    {
        var shareURL :String = "http://www.facebook.com/sharer.php" +
            "?u=" + encodeURIComponent(createLink()) +
            "&t=" + encodeURIComponent(getShareTitle());
        popShareLink(shareURL, "Whirled", "width=620,height=440");
    }

    /**
     * http://x.myspace.com/download/posttomyspacedeveloperdocumentation001.pdf
     */
    protected function popMyspace () :void
    {
        var shareURL :String = "http://www.myspace.com/index.cfm?fuseaction=postto" +
            "&u=" + encodeURIComponent(createLink()) +
            "&t=" + encodeURIComponent(getShareTitle()) +
            "&l=1" + // post to their Blog
            "&c=" + encodeURIComponent(getEmbedCode(DEFAULT_SIZE));
        popShareLink(shareURL, "Whirled", "width=1024,height=650");
    }

    /**
     * Open an external window via javascript.
     */
    protected function popShareLink (
        shareURL :String, windowTitle :String, windowParams :String = "") :void
    {
        try {
            if (ExternalInterface.available) {
                ExternalInterface.call("window.open", shareURL, windowTitle, windowParams);
                return;
            }
        } catch (e :Error) {
            // nada, handled below
        }
        // fall back to opening the URL in a new page
        _ctx.getMsoyController().handleViewUrl(shareURL);
    }

    // from FloatingPanel
    /** @inheritDoc */
    protected override function didOpen () :void
    {
        // TODO: remove this method when A/B test is finished
        super.didOpen();
        _ctx.getMsoyClient().trackClientAction("2008 12 share hint dialog opened", null);
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
