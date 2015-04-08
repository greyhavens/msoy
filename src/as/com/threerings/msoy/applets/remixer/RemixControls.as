//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.external.ExternalInterface;
import flash.net.URLVariables;
import flash.system.Security;
import flash.utils.ByteArray;

import com.whirled.remix.data.EditableDataPack;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;
import mx.controls.Image;
import mx.controls.Label;
import mx.controls.SWFLoader;
import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.util.ParameterUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.applets.net.MediaUploader;
import com.threerings.msoy.applets.ui.ConfirmDialog;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MsoyLogConfig;
import com.threerings.msoy.data.UberClientModes;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.utils.UberClientLoader;

/**
 */
public class RemixControls extends HBox
{
    // Magic fucking trial-and-error numbers, since flex can't lay out worth a shit.
    public static const CONTROLS_WIDTH :int = 325;
    public static const CONTROLS_MAX_HEIGHT :int = 440;

    public static const PREVIEW_WIDTH :int = 340;

    public function RemixControls (app :Application, viewStack :ViewStack)
    {
        _ctx = new RemixContext(app, viewStack);

        percentWidth = 100;
        percentHeight = 100;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        _previewContainer = new VBox();
        _previewContainer.width = PREVIEW_WIDTH;
        addChild(_previewContainer);
        _previewContainer.addChild(createPreviewHeader());

        var vbox :VBox = new VBox();
        vbox.percentHeight = 100;
        vbox.verticalScrollPolicy = ScrollPolicy.OFF;

        vbox.horizontalScrollPolicy = ScrollPolicy.OFF;
        vbox.width = CONTROLS_WIDTH;
        vbox.percentHeight = 100;
        vbox.setStyle("verticalGap", 0);
        addChild(vbox);

        var label :Label = new Label();
        label.percentWidth = 100;
        label.text = _ctx.REMIX.get("t.options");
        label.setStyle("color", 0x4995C6);
        label.setStyle("textAlign", "center");
        label.setStyle("fontSize", 16);
        vbox.addChild(label);
        vbox.addChild(FlexUtil.createSpacer(0, 8));
        vbox.addChild(createControlsHeader());

        _controls = new VBox();
        _controls.horizontalScrollPolicy = ScrollPolicy.OFF;
        _controls.setStyle("top", 0);
        _controls.setStyle("left", 0);
        _controls.setStyle("right", 0);
        _controls.setStyle("verticalGap", 0);
        _controls.percentWidth = 100;
        _controls.maxHeight = CONTROLS_MAX_HEIGHT;
        vbox.addChild(_controls);

        var butBox :HBox = new HBox();
        butBox.setStyle("bottom", 0);
        butBox.setStyle("horizontalAlign", "right");
        butBox.percentWidth = 100;
        vbox.addChild(FlexUtil.createSpacer(0, 8));
        vbox.addChild(butBox);

        _cancelBtn = new CommandButton(_ctx.REMIX.get("b.cancel"), cancel);
        _cancelBtn.styleName = "longThinOrangeButton";
        butBox.addChild(_cancelBtn);

        butBox.addChild(_saveBtn = new CommandButton("", commit));
        _saveBtn.styleName = "longThinOrangeButton";
        _saveBtn.enabled = false;

        try {
            ExternalInterface.addCallback("itemPurchased", itemPurchased);
        } catch (err :Error) {
            // whatever
        }

        Security.loadPolicyFile(DeploymentConfig.crossDomainURL);

        ParameterUtil.getParameters(app, function (params :Object) :void  {
            _params = params;
            setMustBuy("true" == params["mustBuy"]);

            var media :String = params["media"] as String;

            createPreviewer(params["type"] as String);

            _saveBtn.label = _ctx.REMIX.get(_mustBuy ? "b.save_and_buy" : "b.save_all");

            _ctx.pack = new EditableDataPack(media);
            _ctx.pack.addEventListener(Event.COMPLETE, handlePackComplete);
            _ctx.pack.addEventListener(ErrorEvent.ERROR, handlePackError);
        });
    }

    /**
     * Provide parameters pass-through for the uberclient.
     */
    public function getWhirledParams () :String
    {
        var copy :URLVariables = new URLVariables();
        for (var n :String in _params) {
            if (n != "media") {
                copy[n] = _params[n];
            }
        }
        return copy.toString();
    }

    protected function setMustBuy (mustBuy :Boolean) :void
    {
        _mustBuy = mustBuy;
        _saveBtn.label = _ctx.REMIX.get(_mustBuy ? "b.save_and_buy" : "b.save_all");
    }

    protected function itemPurchased (success :Boolean) :void
    {
        if (success) {
            setMustBuy(false);
            // we now need to react to a click to save the purchase
            new ConfirmDialog(_ctx, _ctx.REMIX.get("t.purchased"), _ctx.REMIX.get("m.purchased"),
                commit);

        } else {
            // re-enable buttons...
            _saveBtn.enabled = true;
            _cancelBtn.enabled = true;
        }
    }

    protected function createControlsHeader () :UIComponent
    {
        var box :HBox = new HBox();
        box.percentWidth = 100;
        box.setStyle("backgroundColor", 0xDEEDF7);
        box.setStyle("paddingTop", 2);
        box.setStyle("paddingLeft", 8);
        box.setStyle("paddingRight", 8);

        var label :Label = new Label();
        label.text = _ctx.REMIX.get("l.component");
        label.setStyle("textAlign", "left");
        label.setStyle("color", 0x2270A5);
        label.percentWidth = 50;
        box.addChild(label);

        label = new Label();
        label.text = _ctx.REMIX.get("l.value");
        label.setStyle("textAlign", "right");
        label.setStyle("color", 0x2270A5);
        label.percentWidth = 50;
        box.addChild(label);

        return box;
    }

    protected function createPreviewHeader () :UIComponent
    {
        var box :HBox = new HBox();
        box.percentWidth = 100;
        box.setStyle("horizontalGap", 0);

        var left :Image = new Image();
        left.source = new HEADER_BAR_LEFT();
        box.addChild(left);

        var mid :HBox = new HBox();
        mid.styleName = "headerMid";
        mid.percentWidth = 100;
        box.addChild(mid);

        var right :Image = new Image();
        right.source = new HEADER_BAR_RIGHT();
        box.addChild(right);

        var lbl :Label = new Label();
        lbl.text = _ctx.REMIX.get("t.preview");
        lbl.percentWidth = 100;
        lbl.setStyle("color", 0xFFFFFF);
        lbl.setStyle("textAlign", "center");
        lbl.setStyle("fontSize", 16);
        mid.addChild(lbl);

        // If we're on dev, include a buildstamp to aid debugging
        if (DeploymentConfig.devDeployment) {
            lbl.text += " (" + DeploymentConfig.buildTime + ")";
        }

        return box;
    }

    protected function createPreviewer (itemType :String) :void
    {
        var mode :int = getUberClientModeForType(itemType);

        _previewer = new UberClientLoader(mode);
        _previewer.width = PREVIEW_WIDTH;
        _previewer.height = 488;
        _previewer.addEventListener(Event.COMPLETE, handlePreviewerComplete);
        _previewer.addEventListener(IOErrorEvent.IO_ERROR, handlePreviewerError);
        _previewer.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handlePreviewerError);
        _previewer.load();
        _previewContainer.addChild(_previewer);
    }

    protected function getUberClientModeForType (itemType :String) :int
    {
        switch (itemType) {
        case "avatar":
            return UberClientModes.AVATAR_VIEWER;

        case "pet":
            return UberClientModes.PET_VIEWER;

        case "furniture":
            return UberClientModes.FURNI_VIEWER;

        case "decor":
            return UberClientModes.DECOR_VIEWER;

        case "toy":
            return UberClientModes.TOY_VIEWER;

        default:
            return UberClientModes.GENERIC_VIEWER;
        }
    }

    protected function handlePreviewerComplete (event :Event) :void
    {
        _previewReady = true;
        maybeUpdatePreview();
    }

    protected function handlePreviewerError (event :ErrorEvent) :void
    {
        trace("Previewer error: " + event);
    }

    protected function handlePackError (event :ErrorEvent) :void
    {
        trace("Error loading: " + event.text)
    }

    protected function handlePackComplete (event :Event) :void
    {
        addEventListener(FieldEditor.FIELD_CHANGED, handleFieldChanged);

        var name :String;
        for each (name in _ctx.pack.getDataFields()) {
            _controls.addChild(new DataEditor(_ctx, name));
        }

        var injectFileEditor :FileEditor;
        var injectMedia :String;
        for each (name in _ctx.pack.getFileFields()) {
            var fe :FileEditor = new FileEditor(_ctx, name);
            _controls.addChild(fe);
            if (("inject-" + name) in _params) {
                injectFileEditor = fe;
                injectMedia = String(_params["inject-" + name]);
            }
        }

        _packReady = true;

        // see if we need to inject any media
        if (injectFileEditor != null) {
            injectFileEditor.injectMedia(injectMedia);
            return; // do not yet update the preview
        }

        maybeUpdatePreview();
    }

    /**
     * Handle the FIELD_CHANGED event dispatched by FieldEditors.
     */
    protected function handleFieldChanged (event :Event) :void
    {
        _saveBtn.enabled = true;
        maybeUpdatePreview();
    }

    protected function maybeUpdatePreview () :void
    {
        if (_packReady && _previewReady) {
            // wait a frame...
            callLater(updatePreview);
        }
    }

    protected function updatePreview () :void
    {
        _bytes = _ctx.pack.serialize();
        _lastBytes = _bytes;
        sendPreview();
    }

    /**
     * Send the bytes to the previewer, automatically retrying until they get through.
     */
    protected function sendPreview () :void
    {
        if (_bytes == null) {
            return;
        }

        var result :Boolean;
        try {
            var o :Object = _previewer.content;
            o = o.application;
            result = Boolean(o.loadBytes(_bytes));

        } catch (err :Error) {
            result = false;
        }
        if (result) {
            _bytes = null;

        } else {
            // try every frame to send this preview..
            callLater(sendPreview);
        }
    }

    /**
     * Called to cancel remixing.
     */
    protected function cancel () :void
    {
        if (ExternalInterface.available) {
            _saveBtn.enabled = false;
            _cancelBtn.enabled = false;
            ExternalInterface.call("cancelRemix");
        }
    }

    /**
     * Called to save the changes and commit the remix.
     */
    protected function commit () :void
    {
        _saveBtn.enabled = false;
        _cancelBtn.enabled = false;
        _saveBtn.validateNow();
        _cancelBtn.validateNow();
        validateNow();
        saveRemix();
    }

    protected function saveRemix () :void
    {
        if (_mustBuy) {
            // tell the GWT side that we wish to purchase the remix
            if (ExternalInterface.available) {
                ExternalInterface.call("buyItem");
            }
            return;
        }

        if (_lastBytes == null) {
            _lastBytes = _ctx.pack.serialize();
        }

        var uploader :MediaUploader = new MediaUploader(_ctx);
        uploader.addEventListener(Event.COMPLETE, handleUploadComplete);
//        uploader.addEventListener(ProgressEvent.PROGRESS, handleUploadProgress);
        uploader.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        uploader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
        uploader.upload(_params["mediaId"], "datapack.zip", _lastBytes);
    }

//    protected function handleUploadProgress (event :ProgressEvent) :void
//    {
//        // Note: The goddamn URLLoader inside the MediaUploader is geared towards *downloading*,
//        // so we don't get progress events until the download phase of the operation, which
//        // is basically instantaneous.
//        trace(":: progress " + (event.bytesLoaded * 100 / event.bytesTotal).toPrecision(3));
//    }

    protected function handleUploadComplete (event :Event) :void
    {
        var uploader :MediaUploader = event.target as MediaUploader;

        var result :Object = uploader.getResult();
        uploader.close();

        if (ExternalInterface.available) {
            var forcedType :int = int(_params["forceMimeType"]);

            for (var mediaId :String in result) {
                var data :Object = result[mediaId];
                var mimeType :int = (forcedType == MediaMimeTypes.INVALID_MIME_TYPE) ?
                    data.mimeType : forcedType;
                ExternalInterface.call("setHash", mediaId, "", data.hash, mimeType,
                    data.constraint, /*data.expiration, data.signature,*/ data.width, data.height);
            }
        }
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        // TODO
        trace("Oh noes! : " + event.text);
        _saveBtn.enabled = true;
        _cancelBtn.enabled = true;

        var uploader :MediaUploader = event.target as MediaUploader;
        uploader.close();
    }

    protected var _previewReady :Boolean;
    protected var _packReady :Boolean;

    protected var _previewContainer :VBox;

    protected var _previewer :SWFLoader;

    protected var _controls :VBox;

    protected var _cancelBtn :CommandButton;
    protected var _saveBtn :CommandButton;

    protected var _ctx :RemixContext;

    /** The serialized pack we're currently trying to send to the previewer. */
    protected var _bytes :ByteArray;

    /** The last serialization of the pack. */
    protected var _lastBytes :ByteArray;

    protected var _params :Object;

    /** Do we need to buy this item before we can save the remix? */
    protected var _mustBuy :Boolean;

    // configure log levels
    MsoyLogConfig.init();

    [Embed(source="../../../../../../../pages/images/ui/box/header_left.png")]
    protected static const HEADER_BAR_LEFT :Class;

    [Embed(source="../../../../../../../pages/images/ui/box/header_right.png")]
    protected static const HEADER_BAR_RIGHT :Class;
}
}
