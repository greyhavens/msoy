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

import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.HRule;

import com.adobe.images.JPGEncoder;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.util.ParameterUtil;
import com.threerings.util.StringUtil;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.utils.Base64Sender;

import com.threerings.msoy.applets.net.MediaUploader;

/**
 */
public class RemixControls extends VBox
{
    public function RemixControls (app :Application)
    {
        percentWidth = 100;
        percentHeight = 100;

        _controls = new Grid();
        _controls.setStyle("top", 0);
        _controls.setStyle("left", 0);
        _controls.setStyle("right", 0);
        _controls.percentWidth = 100;
        _controls.percentHeight = 100;
        addChild(_controls);

        var butBox :HBox = new HBox();
        butBox.setStyle("bottom", 0);
        butBox.percentWidth = 100;
        addChild(butBox);

        butBox.addChild(_saveBtn = new CommandButton("Save", commit));
        _saveBtn.enabled = false;

        ParameterUtil.getParameters(app, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        _params = params;
        var media :String = params["media"] as String;

        _pack = new EditableDataPack(media);
        _pack.addEventListener(Event.COMPLETE, handlePackComplete);
        _pack.addEventListener(ErrorEvent.ERROR, handlePackError);
    }

    protected function handlePackError (event :ErrorEvent) :void
    {
        trace("Error loading: " + event.text)
    }

    protected function handlePackComplete (event :Event) :void
    {
        updatePreview();

        addEventListener(FieldEditor.FIELD_CHANGED, handleFieldChanged);

        var name :String;
        var datas :Array = _pack.getDataFields();
        if (datas.length > 0) {
            GridUtil.addRow(_controls, "Data fields", "Used?", "Value", [2, 1]);
            addRule();
            for each (name in datas) {
                _controls.addChild(new DataEditor(_pack, name));
            }
        }

        var files :Array = _pack.getFileFields();
        if (files.length > 0) {
            if (datas.length > 0) {
                GridUtil.addRow(_controls, "", [4, 1]);
            }
            GridUtil.addRow(_controls, "Files", "Used?", "filename", [2, 1]);
            addRule();
            for each (name in files) {
                _controls.addChild(new FileEditor(_pack, name));
            }
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
        // send the bytes to our previewer
        var b64 :Base64Sender = new Base64Sender("remixPreview", "setMediaBytes");
        b64.sendBytes(_pack.serialize());
    }

    protected function commit () :void
    {
        var uploader :MediaUploader = new MediaUploader(_params["server"], _params["auth"]);
        uploader.addEventListener(Event.COMPLETE, handleUploadComplete);
        uploader.addEventListener(ProgressEvent.PROGRESS, handleUploadProgress);
        uploader.addEventListener(IOErrorEvent.IO_ERROR, handleUploadError);
        uploader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleUploadError);
        uploader.upload(_params["mediaId"], "datapack.zip", _pack.serialize());
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

    protected var _controls :Grid;

    protected var _saveBtn :CommandButton;

    protected var _pack :EditableDataPack;

    protected var _snapper :CameraSnapshotter;

    protected var _params :Object;
}
}
