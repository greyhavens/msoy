//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.display.BitmapData;

import flash.events.Event;

import flash.geom.Point;

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

import com.adobe.images.PNGEncoder;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.CameraSnapshotter;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.PopUpUtil;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.msoy.applets.image.CameraSnapshotControl;
import com.threerings.msoy.applets.image.DisplayCanvas;
import com.threerings.msoy.applets.image.NewImageDialog;

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
        controlBox.addChild(makeBullet(
            new CommandLinkButton("Create a new image...", handleChooseNewImage)));
// Flash security prevents us from using most loaded content as 'data'
//        controlBox.addChild(makeBullet(
//            new CommandLinkButton("Use file at a URL...", handleChooseURL)));
        var filenames :Array = ctx.pack.getFilenames();
        if (filenames.length > 0) {
            // we need to wrap the array commandbutton arg in another array...
            controlBox.addChild(makeBullet(
                new CommandLinkButton("Use a file from the remix...",
                handleChooseExistingFile, [ filenames ])));
        }
        if ((_type == "Image" || _type == "DisplayObject") && CameraSnapshotter.hasCamera()) {
            controlBox.addChild(makeBullet(
                new CommandLinkButton("Take a snapshot with my webcam...", handleChooseCamera)));
        }

        previewBox.addChild(makeHeader("Preview"));
        _image = new DisplayCanvas(450, 450);
        _image.addEventListener(DisplayCanvas.SIZE_KNOWN, handleSizeKnown);
        previewBox.addChild(_image);
        _label = new Label();

        hbox = new HBox();
        hbox.addChild(_edit = new CommandButton("Edit", doEdit));
        hbox.addChild(_label);
        previewBox.addChild(hbox);

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

        setPopped(true);
    }

    public function setImage (filename :String, bytes :ByteArray) :void
    {
        if (filename == null) {
            _filename = null;
            _label.text = "<no file>";

        } else {
            _filename = _ctx.createFilename(filename, bytes);
            _label.text = _filename;
        }
        _bytes = bytes;
        _image.setImage(bytes);
        _ok.enabled = (bytes != null);
        _edit.enabled = (bytes != null);
    }

    protected function handleSizeKnown (event :ValueEvent) :void
    {
        // append the size to the name label
        _label.text = _filename + " (" + event.value[0] + "x" + event.value[1] + ")";
        callLater(PopUpUtil.center, [ this ]);
    }

    protected function close (save :Boolean) :void
    {
        setPopped(false);

        if (save && _bytes != null) {
            _parent.updateValue(_filename, _bytes);
        } else {
            _parent.updateValue(null, null);
        }
    }

    protected function handleChooseFile () :void
    {
        var uploader :Uploader = new Uploader(_serverURL, getFilters());
        uploader.addEventListener(Event.COMPLETE, handleFileChosen);
    }

    protected function handleChooseNewImage () :void
    {
        var newImage :NewImageDialog = new NewImageDialog(getForcedSize());
        newImage.addEventListener(Event.COMPLETE, handleImageCreated);
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

    protected function handleImageCreated (event :ValueEvent) :void
    {
        // go straight to editing mode
        doEdit(event.value, "drawn.png");
    }

    protected function handleChooseCamera () :void
    {
        new CameraSnapshotControl(this, function (bitmapData :BitmapData) :void {
            setImage("cameragrab.png", PNGEncoder.encode(bitmapData));
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
        box.setStyle("horizontalGap", -10);

        var lbl :Label = new Label();
        lbl.text = "-";
        box.addChild(lbl);
        box.addChild(comp);

        return box;
    }

    protected function getFilters () :Array
    {
        // Note: returning one filter is preferable to returning many because
        // the first filter will control the set of files the user initially sees and
        // switching filters is usually a tiny pulldown that users don't notice.
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

    protected function doEdit (image :Object = null, newFilename :String = null) :void
    {
        setPopped(false);

        _newFilename = newFilename;
        var source :Object = (image != null) ? image : _bytes;
        var editor :ImageEditor = new ImageEditor(_ctx, source, getForcedSize());
        editor.addEventListener(PopupImageEditor.IMAGE_UPDATED, handleEditorClosed);
        //editor.title = _name;
    }

    protected function handleEditorClosed (event :ValueEvent) :void
    {
        setPopped(true);

        var array :Array = event.value as Array;
        if (array != null) {
            var file :String = (_newFilename != null) ? _newFilename : _filename;
            _newFilename = null;

            var ba :ByteArray = ByteArray(array[0]);
            _filename = _ctx.createFilename(file, ba, array[1] as String);
            setImage(_filename, ba);
        }
    }

    /**
     * Get the forced size of this entry, or null if none.
     */
    protected function getForcedSize () :Point
    {
        var entry :Object = _ctx.pack.getFileEntry(_name);
        var ww :Number = Number(entry.width);
        var hh :Number = Number(entry.height);
        return (isNaN(ww) || isNaN(hh)) ? null : new Point(ww, hh);
    }

    protected function setPopped (vis :Boolean) :void
    {
        if (vis) {
            PopUpManager.addPopUp(this, _parent, true);
            PopUpUtil.center(this);
        } else {
            PopUpManager.removePopUp(this);
        }
    }

    protected var _parent :FileEditor;

    protected var _name :String;

    protected var _ctx :RemixContext;

    protected var _type :String;

    protected var _serverURL :String;

    protected var _filename :String;

    protected var _ok :CommandButton;

    protected var _edit :CommandButton;

    protected var _image :DisplayCanvas;

    protected var _label :Label;

    protected var _bytes :ByteArray;

    /* If non-null when we return from the editor, the base filename to use. */
    protected var _newFilename :String;
}
}
