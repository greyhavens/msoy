//
// $Id$

package com.threerings.msoy.client {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.SecurityErrorEvent;
import flash.external.ExternalInterface;
import flash.net.FileReference;
import flash.net.URLRequest;

import mx.containers.HBox;
import mx.containers.TabNavigator;
import mx.containers.VBox;
import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.Image;
import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.controls.Text;
import mx.controls.TextArea;
import mx.controls.TextInput;
import mx.core.UIComponent;
import mx.events.FlexEvent;

import com.threerings.io.TypedArray;

import com.threerings.util.Command;
import com.threerings.util.MailUtil;
import com.threerings.util.Util;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.data.PlaceInfo;
import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.FloatingPanel;

public class ShareDialog extends FloatingPanel
{
    public function ShareDialog (ctx :MsoyContext)
    {
        super(ctx);

        // find out about our current place
        _place = _ctx.getMsoyController().getPlaceInfo();

        title = Msgs.GENERAL.get(_place.inGame ? "t.share_game" : "t.share_room");
        showCloseButton = true;

        _autoFriend = new CheckBox();
        _autoFriend.label = Msgs.GENERAL.get("b.share_auto_friend");
        _autoFriend.toolTip = Msgs.GENERAL.get("i.share_auto_friend_tip");
        _autoFriend.selected = true;

        if (ctx.isRegistered()) {
            var sharing :Text = FlexUtil.createText("", 400, "shareDialogHeader");
            var barURL :String = DeploymentConfig.serverURL + "images/ui/bars_small.png";
            sharing.htmlText = Msgs.GENERAL.get("m.sharing", barURL);
            addChild(sharing);
            const affLink :CommandLinkButton = new CommandLinkButton(
                Msgs.GENERAL.get("b.sharing"), MsoyController.VIEW_URL,
                Msgs.GENERAL.get("u.affiliates"));
            affLink.styleName = "underLink";
            addChild(affLink);
            addChild(_autoFriend);
        }

        var tabs :TabNavigator = new TabNavigator();
        tabs.styleName = "sexyTabber";
        tabs.setStyle("tabWidth", NaN);
        tabs.resizeToContent = true;
        tabs.width = 430;
        tabs.height = 240;

        tabs.addChild(createSocialBox());
        if (ctx.isRegistered()) {
            tabs.addChild(createEmailBox());
        }
        tabs.addChild(createLinkBox());
        tabs.addChild(createEmbedBox());

        if (_ctx.getMsoyController().canManagePlace()) {
            tabs.addChild(createStubBox());
        }
        addChild(tabs);
        open(false);

        // Hack to jiggle the text in the first tab, so that it all shows up.
        var btn :Button = tabs.getTabAt(0);
        var lbl :String = btn.label;
        btn.label = "";
        btn.label = lbl;
    }

    public function getEmbedCode (size :int) :String
    {
        var flashVars :String = _place.makeEmbedVars();
        flashVars = includeAffiliate(flashVars);
        if (size == 0) { // mini TV view
            flashVars += "&featuredPlace=true";
        }

        const fullLink :String = _ctx.getMsoyController().createSharableLink("", getAutoFriend());
        const swfUrl :String = DeploymentConfig.serverURL.replace(
            /(http:\/\/[^\/]*).*/, "$1/clients/world-client.swf");

        return Msgs.GENERAL.get("m.embed", flashVars, swfUrl,
            EMBED_SIZES[size][0], EMBED_SIZES[size][1], fullLink);
    }

    protected function createSocialBox () :VBox
    {
        var box :VBox = createContainer("t.social_share");
        box.setStyle("horizontalAlign", "center");

        box.addChild(FlexUtil.createText(Msgs.GENERAL.get("m.social_share"), 400));

        var row :HBox = new HBox();
        row.percentWidth = 100;
        row.setStyle("horizontalGap", 20);

        row.addChild(createSocialButton("b.share_facebook",
            "http://b.static.ak.fbcdn.net/images/share/facebook_share_icon.gif?8:26981",
            popFacebook));
        row.addChild(createSocialButton("b.share_myspace",
            "http://cms.myspacecdn.com/cms/post_myspace_icon.gif",
            popMyspace));
        row.addChild(createSocialButton(null,
            DeploymentConfig.serverURL + "images/ui/digg.png",
            popDigg));
        box.addChild(row);

        row = new HBox();
        row.percentWidth = 100;
        row.setStyle("horizontalGap", 20);

        row.addChild(createSocialButton("b.share_twitter",
            "http://www.blogsdna.com/wp-content/uploads/2008/05/small-twitter.jpg", // TODO
            popTwitter));

        box.addChild(row);
        return box;
    }

    /**
     * @param msg the text for the button, or null.
     * @param the class or URL of the icon, or null.
     * @param callback cmon
     */
    protected function createSocialButton (
        msg :String, iconURL :String, callback :Function) :UIComponent
    {
        var box :HBox = new HBox();
        box.setStyle("horizontalGap", 0);

        if (iconURL != null) {
            var img :Image = new Image();
            img.source = iconURL;
            img.buttonMode = true;
            Command.bind(img, MouseEvent.CLICK, callback);
            box.addChild(img);
        }
        if (msg != null) {
            var link :CommandLinkButton = new CommandLinkButton(Msgs.GENERAL.get(msg), callback);
            link.styleName = "underLink"; // TODO: god it irks me that the default is un-underlined.
            box.addChild(link);
        }
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
        var box :VBox = createContainer("t.stub_share");
        box.setStyle("horizontalAlign", "center");
        box.addChild(FlexUtil.createText(Msgs.GENERAL.get("m.stub_share"), 400));
        var stub :CommandButton = new CommandButton(Msgs.GENERAL.get("b.stub_share"),
            startDownload, [ stubURL(), "Whirled-" + (_place.inGame ? "game" : "room") + "-" +
                             _place.id + "-stub.swf" ]);
        stub.styleName = "orangeButton";
        box.addChild(stub);
        _downloadBtns.push(stub);

        if (_place.inGame) {
            // add an additional button for mochiad -enabled games
            box.addChild(FlexUtil.createSpacer(10, 10));

            box.addChild(FlexUtil.createText(
                Msgs.GENERAL.get("m.mochi_share", EMBED_SIZES[2].join("x")),
                400));
            var hbox :HBox = new HBox();
            hbox.addChild(FlexUtil.createLabel(Msgs.GENERAL.get("l.mochi_id")));
            var mochiIdField :TextInput = new TextInput();
            mochiIdField.maxChars = 16;
            mochiIdField.restrict = "0-9a-f";
            hbox.addChild(mochiIdField);
            var mochi :CommandButton = new CommandButton(Msgs.GENERAL.get("b.mochi_share"),
                function () :void {
                    if (mochiIdField.text.length != 16) {
                        _downloadError.text = Msgs.GENERAL.get("e.mochi_id");
                        return;
                    }

                    startDownload(stubURL(true) +
                        "&mochiId=" + encodeURIComponent(mochiIdField.text),
                        "Whirled-game-" + _place.id + "-mochi-stub.swf");
                });
            mochi.styleName = "orangeButton";
            hbox.addChild(mochi);
            box.addChild(hbox);
            _downloadBtns.push(mochi);
        }

        box.addChild(_downloadError = FlexUtil.createLabel(""));
        _downloadError.setStyle("color", 0xFF0000);
        return box;
    }

    protected function stubURL (mochi :Boolean = false) :String
    {
        // NOTE: these parameters are used for embedding our games on other flash game sites, so
        // backwards compatibility is important. See MsoyParameters.massageEmbedParameters
        var stubArgs :String;
        if (_place.inGame && _place.avrGame) {
            stubArgs = "avrgame=" + _place.gameId;
            if (_place.sceneId != 0) {
                stubArgs += "&room=" + _place.sceneId;
            }
        } else {
            stubArgs = (_place.inGame ? "game" : "room") + "=" + _place.id;
        }
        stubArgs = includeAffiliate(stubArgs);
        if (mochi) {
            stubArgs += "&vec=e.mochi.games." + _place.id;
        }
        return DeploymentConfig.serverURL + "stubdlsvc?args=" + encodeURIComponent(stubArgs);
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
        box.addChild(FlexUtil.createText(Msgs.GENERAL.get("l.link_instruction"), 400));
        var text :CopyableText;
        function updateLink () :void {
            if (text != null) {
                box.removeChild(text);
            }
            box.addChild(text = new CopyableText(createLink()));
        }
        _autoFriend.addEventListener(Event.CHANGE, Util.adapt(updateLink));
        updateLink();
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
        box.addChild(FlexUtil.createText(Msgs.GENERAL.get("l.embed_choose_size"), 400));

        var code :TextInput = new TextInput();
        var checks :HBox = new HBox();
        checks.percentWidth = 100;
        box.addChild(checks);

        // the small scenes cannot host non-rooms, at least for now
        for (var ii :int = _place.inGame ? 2 : 0; ii < EMBED_SIZES.length; ii++) {
            checks.addChild(createSizeControl(ii));
        }

        function updateText () :void {
            code.text = getEmbedCode(_sizeGroup.selectedValue as int);
        }

        _sizeGroup.addEventListener(FlexEvent.VALUE_COMMIT, Util.adapt(updateText));
        _autoFriend.addEventListener(Event.CHANGE, Util.adapt(updateText));

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
        (_ctx.getClient().requireService(MsoyService) as MsoyService).emailShare(
            _place.inGame, _place.name, _place.id, emails, message,
            getAutoFriend(), _ctx.confirmListener("m.share_email_sent"));

        close(); // and make like the proverbial audi 5000
    }

    /**
     * Return a shareable link to this place.
     */
    protected function createLink (forDigg :Boolean = false) :String
    {
        var page :String;
        if (_place.inGame) {
            page = "world-game_p_" + _place.gameId;
        } else if (_place.sceneId != 0) {
            page = "world-s" + _place.sceneId;
        }
        if (forDigg) {
            // we want everyone to "digg" the same URL, so we can't include affiliate information
            // in the URLs we share on Digg
            return DeploymentConfig.serverURL + "go/" + page;
        } else {
            return _ctx.getMsoyController().createSharableLink(page, getAutoFriend());
        }
    }

    protected function startDownload (url :String, localFile :String) :void
    {
        enableDownloadButtons(false);
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
        enableDownloadButtons(true);
        if (event is ErrorEvent) {
            _downloadError.text = ErrorEvent(event).text;
        }
    }

    protected function enableDownloadButtons (setEnabled :Boolean) :void
    {
        for each (var btn :UIComponent in _downloadBtns) {
            btn.enabled = setEnabled;
        }
    }

    protected function getShareTitle () :String
    {
        return Msgs.GENERAL.get(_place.inGame ? "m.social_share_game" : "m.social_share_room",
            _place.name);
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

    protected function popDigg () :void
    {
        var link :String = createLink(true /* digg link */);
        var shareURL :String = "http://digg.com/submit?media=news&topic=playable_web_games" +
            "&url=" + encodeURIComponent(link) +
            "&title=" + encodeURIComponent(getShareTitle()) +
            "&bodytext=" + encodeURIComponent(getShareTitle()); // TODO
        _ctx.getMsoyController().handleViewUrl(shareURL, "_blank");
    }

    protected function popTwitter () :void
    {
        _ctx.getMsoyController().handleTweet(
            Msgs.GENERAL.get("m.tweet_" + (_place.inGame ? "game" : "room"), _place.name,
            createLink()));
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

    /**
     * Checks if the sharing link should cause followers to automatically request friendship.
     */
    protected function getAutoFriend () :Boolean
    {
        return _autoFriend.selected;
    }

    /**
     * Tacks on the "aff" argument for an embedded flash client.
     */
    protected function includeAffiliate (args :String) :String
    {
        // include the "aff=<memberId>" flashvar to affiliate users to us... registered users only
        if (_ctx.isRegistered()) {
            var id :int = _ctx.getMyId();
            if (args.length > 0) {
                args += "&";
            }
            args += "aff=" + (getAutoFriend() ? -id : id);
        }
        return args;
    }

    protected var _place :PlaceInfo;
    protected var _autoFriend :CheckBox;

    /** We need to keep this in scope or the download will halt. */
    protected var _fileRef :FileReference;

    protected var _sizeGroup :RadioButtonGroup = new RadioButtonGroup();
    protected var _status :Text;

    protected var _downloadBtns :Array = [];
    protected var _downloadError :Label;

    protected static const EMBED_SIZES :Array = [
        [350, 200], [400, 415], [700, 575], ["100%", 575]
    ];

    // Default to full 100% size
    protected static const DEFAULT_SIZE :int = 3;
}
}
