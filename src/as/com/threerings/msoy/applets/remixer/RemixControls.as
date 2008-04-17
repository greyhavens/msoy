//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;

import flash.external.ExternalInterface;

import flash.utils.ByteArray;

import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Grid;
import mx.containers.GridRow;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.HRule;
import mx.controls.Image;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.controls.SWFLoader;
import mx.controls.Text;

import com.adobe.images.JPGEncoder;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.util.ParameterUtil;
import com.threerings.util.StringUtil;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.applets.net.MediaUploader;

import com.threerings.msoy.client.DeploymentConfig;

/**
 */
public class RemixControls extends HBox
{
    public static const CONTROLS_WIDTH :int = 325;

    public static const PREVIEW_WIDTH :int = 340;

    public function RemixControls (app :Application, viewStack :ViewStack)
    {
        percentWidth = 100;
        percentHeight = 100;

        var vbox :VBox = new VBox();
        vbox.width = PREVIEW_WIDTH;
        addChild(vbox);
        vbox.addChild(createPreviewHeader());

        _previewer = new SWFLoader();
        _previewer.width = PREVIEW_WIDTH;
        _previewer.height = 488;
        _previewer.addEventListener(Event.COMPLETE, handlePreviewerEvent);
        _previewer.addEventListener(IOErrorEvent.IO_ERROR, handlePreviewerEvent);
        _previewer.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handlePreviewerEvent);
        _previewer.load("/clients/" + DeploymentConfig.version + "/avatarviewer.swf");
        vbox.addChild(_previewer);

        vbox = new VBox();

        vbox.horizontalScrollPolicy = ScrollPolicy.OFF;
        vbox.width = CONTROLS_WIDTH;
        vbox.maxHeight = 550;
        addChild(vbox);

        var label :Label = new Label();
        label.percentWidth = 100;
        label.text = "Remixable Options";
        label.setStyle("color", 0x4995C6);
        label.setStyle("textAlign", "center");
        label.setStyle("fontSize", 16);
        vbox.addChild(label);

        _controls = new Grid();
        _controls.setStyle("top", 0);
        _controls.setStyle("left", 0);
        _controls.setStyle("right", 0);
        _controls.setStyle("verticalGap", 0);
        _controls.percentWidth = 100;
        _controls.percentHeight = 100;
        //_controls.maxWidth = CONTROLS_WIDTH;
        vbox.addChild(_controls);

        var butBox :HBox = new HBox();
        butBox.setStyle("bottom", 0);
        butBox.percentWidth = 100;
        vbox.addChild(butBox);

        butBox.addChild(_saveBtn = new CommandButton("Save", commit));
        _saveBtn.enabled = false;


        ParameterUtil.getParameters(app, function (params :Object) :void  {
            _params = params;
            var media :String = params["media"] as String;

            _ctx = new RemixContext(new EditableDataPack(media), viewStack);
            _ctx.pack.addEventListener(Event.COMPLETE, handlePackComplete);
            _ctx.pack.addEventListener(ErrorEvent.ERROR, handlePackError);
        });
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
        lbl.text = "Preview";
        lbl.percentWidth = 100;
        lbl.setStyle("color", 0xFFFFFF);
        lbl.setStyle("textAlign", "center");
        lbl.setStyle("fontSize", 16);
        mid.addChild(lbl);

        return box;
    }

    protected function handlePreviewerEvent (event :Event) :void
    {
        // TODO
        trace("Previewer event: " + event);
    }

    protected function handlePackError (event :ErrorEvent) :void
    {
        trace("Error loading: " + event.text)
    }

    protected function handlePackComplete (event :Event) :void
    {
        updatePreview();

        addEventListener(FieldEditor.FIELD_CHANGED, handleFieldChanged);

        GridUtil.addRow(_controls, "Field", "Used?", "Value", [2, 1]);
        addRule();

        var name :String;
        var datas :Array = _ctx.pack.getDataFields();
        var rowCount :int = 0;
        if (datas.length > 0) {
            for each (name in datas) {
                colorRow(addSpacer(), rowCount);
                var de :DataEditor = new DataEditor(_ctx, name);
                colorRow(de, rowCount);
                _controls.addChild(de);
                colorRow(addDescription(_ctx.pack.getDataEntry(name)), rowCount);
                rowCount++;
            }
        }

        var files :Array = _ctx.pack.getFileFields();
        if (files.length > 0) {
            for each (name in files) {
                colorRow(addSpacer(), rowCount);
                var fe :FileEditor = new FileEditor(_ctx, name, _params["server"]);
                colorRow(fe, rowCount);
                _controls.addChild(fe);
                colorRow(addDescription(_ctx.pack.getFileEntry(name)), rowCount);
                rowCount++;
            }
        }
    }

    protected function addDescription (entry :Object) :GridRow
    {
        var lbl :Text = new Text();
        lbl.width = CONTROLS_WIDTH;
        lbl.setStyle("paddingLeft", 20);
        lbl.text = entry.info;

        return GridUtil.addRow(_controls, lbl, [4, 1]);
    }

    protected function addSpacer () :GridRow
    {
        var spacer :Spacer = new Spacer();
        spacer.height = 5;
        return GridUtil.addRow(_controls, spacer, [4, 1]);
    }

    protected function colorRow (row :GridRow, rowCount :int) :void
    {
        if (rowCount % 2 == 1) {
            row.setStyle("backgroundColor", 0xFFFFCC);
        }
    }

    protected function addRule () :void
    {
        var rule :HRule = new HRule();
        rule.percentWidth = 100;
        rule.setStyle("strokeWidth", 1);
        rule.setStyle("strokeColor", 0x000000);
        GridUtil.addRow(_controls, rule, [5, 1]);
    }

    /**
     * Handle the FIELD_CHANGED event dispatched by FieldEditors.
     */
    protected function handleFieldChanged (event :Event) :void
    {
        updatePreview();
        _saveBtn.enabled = true;
    }

    protected function updatePreview () :void
    {
        _bytes = _ctx.pack.serialize();
        sendPreview();
    }

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

    protected function commit () :void
    {
        var uploader :MediaUploader = new MediaUploader(_params["server"], _params["auth"]);
        uploader.addEventListener(Event.COMPLETE, handleUploadComplete);
        uploader.addEventListener(ProgressEvent.PROGRESS, handleUploadProgress);
        uploader.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        uploader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
        uploader.upload(_params["mediaId"], "datapack.zip", _ctx.pack.serialize());
    }

    protected function handleUploadProgress (event :ProgressEvent) :void
    {
        // TODO
        // unfortunately, it seems that the uploader doesn't show upload progress, only
        // the progress of downloading the data back from the server.

        //trace(":: progress " + (event.bytesLoaded * 100 / event.bytesTotal).toPrecision(3));
    }

    protected function handleUploadComplete (event :Event) :void
    {
        var uploader :MediaUploader = event.target as MediaUploader;

        var result :Object = uploader.getResult();
        trace("Got result: " + result);

        if (ExternalInterface.available) {
            ExternalInterface.call("setHash", result.mediaId, result.hash, result.mimeType,
                result.constraint, result.width, result.height);
        }
    }

    protected function handleUploadError (event :ErrorEvent) :void
    {
        // TODO
        trace("Oh noes! : " + event.text);
    }

    protected var _previewer :SWFLoader;

    protected var _controls :Grid;

    protected var _saveBtn :CommandButton;

    protected var _ctx :RemixContext;

    protected var _snapper :CameraSnapshotter;

    protected var _bytes :ByteArray;

    protected var _params :Object;

    [Embed(source="../../../../../../../pages/images/ui/box/header_left.png")]
    protected static const HEADER_BAR_LEFT :Class;

    [Embed(source="../../../../../../../pages/images/ui/box/header_right.png")]
    protected static const HEADER_BAR_RIGHT :Class;
}
}
