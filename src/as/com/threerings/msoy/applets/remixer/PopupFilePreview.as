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

import mx.core.UIComponent;

import mx.managers.PopUpManager;

import com.threerings.util.ArrayUtil;
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
        parent :FileEditor, name :String, ctx :RemixContext, serverURL :String)
    {
        _parent = parent;
        _name = name;
        _ctx = ctx;
        _serverURL = serverURL;
        var entry :Object = ctx.pack.getFileEntry(name);
        _type = entry.type;

        this.title = name;

        var box :VBox = new VBox();
        addChild(box);

        var hbox :HBox = new HBox();
        box.addChild(hbox);

        var controlBox :VBox = new VBox();
        var previewBox :VBox = new VBox();
        hbox.addChild(controlBox);
        hbox.addChild(previewBox);

        controlBox.addChild(makeHeader("Change..."));
        controlBox.addChild(makeBullet(
            new CommandLinkButton("Upload a new file...", handleChooseFile)));
// Flash security prevents us from using most loaded content as 'data'
//        controlBox.addChild(makeBullet(
//            new CommandLinkButton("Use file at a URL...", handleChooseURL)));
        var filenames :Array = ctx.pack.getFilenames();
        if (filenames.length > 0) {
            // we need to wrap the array commandbutton arg in another array...
            controlBox.addChild(makeBullet(
                new CommandLinkButton("Use an existing file from the remix...",
                handleChooseExistingFile, [ filenames ])));
        }
        if ((_type == "Image" || _type == "DisplayObject") && CameraSnapshotter.hasCamera()) {
            controlBox.addChild(makeBullet(
                new CommandLinkButton("Take a snapshot with my webcam...", handleChooseCamera)));
        }

        previewBox.addChild(makeHeader("Preview"));
        _image = new ImagePreview(Number(entry.width), Number(entry.height));
        _image.addEventListener(ImagePreview.SIZE_KNOWN, handleSizeKnown);
        _image.maxWidth = 400;
        _image.maxHeight = 300;
        _image.minWidth = 200;
        _image.minHeight = 100;
        previewBox.addChild(_image);
        _label = new Label();
        previewBox.addChild(_label);

        var hrule :HRule = new HRule();
        hrule.percentWidth = 100;
        hrule.setStyle("strokeWidth", 1);
        hrule.setStyle("strokeColor", 0x000000);
        box.addChild(hrule);

        var buttonBar :ButtonBar = new ButtonBar();
        buttonBar.addChild(_ok = new CommandButton("Save", close, true));
        buttonBar.addChild(new CommandButton("Cancel", close, false));
        box.addChild(buttonBar);

        setImage(entry.value, ctx.pack.getFile(name));

        PopUpManager.addPopUp(this, parent, true);
        PopUpManager.centerPopUp(this);
    }

    public function setImage (filename :String, bytes :ByteArray) :void
    {
        setFilename(filename, bytes);
        _image.setImage(bytes);
        _ok.enabled = (bytes != null);
    }

    public function setBitmap (filename :String, bitmapData :BitmapData) :void
    {
        setFilename(filename);
        _image.setImage(bitmapData);
        _ok.enabled = (bitmapData != null);
    }

    protected function handleSizeKnown (event :ValueEvent) :void
    {
        // append the size to the name label
        _label.text = _filename + " (" + event.value[0] + "x" + event.value[1] + ")";
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

    protected function handleChooseURL () :void
    {
        var ufc :URLFileChooser = new URLFileChooser();
        ufc.addEventListener(Event.COMPLETE, handleFileChosen);
    }

    protected function handleChooseExistingFile (filenames :Array) :void
    {
        var efc :ExistingFileChooser = new ExistingFileChooser(_ctx.pack, filenames);
        efc.addEventListener(Event.COMPLETE, handleFileChosen);
    }

    protected function handleFileChosen (event :ValueEvent) :void
    {
        var value :Array = event.value as Array;
        if (value != null) {
            setImage(value[0] as String, value[1] as ByteArray);
        }
    }

    protected function handleChooseCamera () :void
    {
        new CameraSnapshotControl(this, function (bitmapData :BitmapData) :void {
            setBitmap("cameragrab.jpg", bitmapData);
        });
    }

    protected function makeHeader (title :String) :UIComponent
    {
        var box :HBox = new HBox();
        box.percentWidth = 100;
        box.setStyle("backgroundColor", 0x000000);

        var lbl :Label = new Label();
        lbl.text = title;
        lbl.percentWidth = 100;
        lbl.setStyle("color", 0xFFFFFF);
        lbl.setStyle("textAlign", "center");
        lbl.setStyle("fontWeight", "bold");
        lbl.setStyle("fontSize", 12);
        box.addChild(lbl);

        return box;
    }

    protected function makeBullet (comp :UIComponent) :UIComponent
    {
        var box :HBox = new HBox();
        box.setStyle("horizontalGap", 0);

        var lbl :Label = new Label();
        lbl.text = "-";
        box.addChild(lbl);
        box.addChild(comp);

        return box;
    }

    protected function setFilename (filename :String, bytes :ByteArray = null) :void
    {
        if (filename == null) {
            _filename = null;
            _label.text = "<no file>";

        } else {
            _filename = _ctx.createFilename(filename, bytes);
            _label.text = _filename;
        }
    }

    protected function getFilters () :Array
    {
        switch (_type) {
        case "Blob":
            return null; // no filter: show all files

        case "DisplayObject":
            return [ new FileFilter("Images and SWFs", "*.jpg;*.jpeg;*.gif;*.png;*.swf") ];

        case "Image":
            return [ new FileFilter("Images", "*.jpg;*.jpeg;*.gif;*.png") ];

        default:
            throw new Error("Don't understand " + _type + " files yet.");
        }
    }

    protected var _parent :FileEditor;

    protected var _name :String;

    protected var _ctx :RemixContext;

    protected var _type :String;

    protected var _serverURL :String;

    protected var _filename :String;

    protected var _ok :CommandButton;

    protected var _image :ImagePreview;

    protected var _label :Label;
}
}
