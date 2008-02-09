//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.display.BitmapData;

import flash.events.Event;

import flash.net.FileFilter;

import flash.utils.ByteArray;

import mx.controls.ButtonBar;
import mx.controls.HRule;
import mx.controls.Label;

import mx.containers.HBox;
import mx.containers.TitleWindow;
import mx.containers.VBox;

import mx.managers.PopUpManager;

import com.threerings.util.ValueEvent;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.msoy.applets.image.CameraSnapshotControl;
import com.threerings.msoy.applets.image.ImagePreview;

import com.threerings.msoy.applets.upload.Uploader;

public class PopupFilePreview extends TitleWindow
{
    public function PopupFilePreview (
        parent :FileEditor, name :String, pack :EditableDataPack, serverURL :String)
    {
        _parent = parent;
        _name = name;
        _pack = pack;
        _serverURL = serverURL;
        var entry :Object = pack.getFileEntry(name);
        _type = entry.type;

        this.title = name;

        var box :VBox = new VBox();
        addChild(box);

        _image = new ImagePreview();
        _image.maxWidth = 300;
        _image.maxHeight = 300;
        box.addChild(_image);

        var hbox :HBox = new HBox();
        var lbl :Label = new Label();
        lbl.text = "Select new:";
        hbox.addChild(lbl);
        hbox.addChild(new CommandButton("Upload file", handleChooseFile));
        if ((_type == "Image" || _type == "DisplayObject") && CameraSnapshotter.hasCamera()) {
            hbox.addChild(new CommandButton("Take picture", handleChooseCamera));
        }

        box.addChild(hbox);

        var hrule :HRule = new HRule();
        hrule.percentWidth = 100;
        hrule.setStyle("strokeWidth", 1);
        hrule.setStyle("strokeColor", 0x000000);
        box.addChild(hrule);

        var buttonBar :ButtonBar = new ButtonBar();
        buttonBar.addChild(_ok = new CommandButton("OK", close, true));
        buttonBar.addChild(new CommandButton("Cancel", close, false));
        box.addChild(buttonBar);

        setImage(pack.getFile(name));

        PopUpManager.addPopUp(this, parent, true);
        PopUpManager.centerPopUp(this);
    }

    public function setImage (bytes :ByteArray) :void
    {
        _image.setImage(bytes);
        _ok.enabled = (bytes != null);
    }

    public function setBitmap (bitmapData :BitmapData) :void
    {
        _image.setBitmap(bitmapData);
        _ok.enabled = (bitmapData != null);
    }

    protected function close (save :Boolean) :void
    {
        var saved :Boolean = false;
        if (save) {
            var ba :ByteArray = _image.getImage(true);
            if (ba != null) {
                _parent.updateValue(_filename, ba);
                saved = true;
            }
        }
        if (!saved) {
            _parent.updateValue(null, null);
        }

        PopUpManager.removePopUp(this);
    }

    protected function handleChooseFile () :void
    {
        var uploader :Uploader = new Uploader(_serverURL, getFilters());
        uploader.addEventListener(Event.COMPLETE, handleFileChosen);
    }

    protected function handleFileChosen (event :ValueEvent) :void
    {
        var value :Array = event.value as Array;
        if (value != null) {
            _filename = value[0] as String;
            setImage(value[1] as ByteArray);
        }
    }

    protected function handleChooseCamera () :void
    {
        new CameraSnapshotControl(this, function (bitmapData :BitmapData) :void {
            setBitmap(bitmapData);
            _filename = "cameragrab.jpg";
        });
    }

    protected function getFilters () :Array
    {
        var array :Array = [];

        switch (_type) {
        case "Blob":
            return null; // no filter: show all files

        case "DisplayObject":
            array.push(new FileFilter("Flash movies", "*.swf"));
            // fall through to Image
        case "Image":
            array.push(new FileFilter("Images", "*.jpg;*.jpeg;*.gif;*.png"));
            return array;

        default:
            throw new Error("Don't understand " + _type + " files yet.");
        }
    }

    protected var _parent :FileEditor;

    protected var _name :String;

    protected var _pack :EditableDataPack;

    protected var _type :String;

    protected var _serverURL :String;

    protected var _filename :String;

    protected var _ok :CommandButton;

    protected var _image :ImagePreview;
}
}
