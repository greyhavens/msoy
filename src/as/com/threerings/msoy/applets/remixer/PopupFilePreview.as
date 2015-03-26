//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.display.BitmapData;
import flash.events.Event;
import flash.external.ExternalInterface;
import flash.net.FileFilter;
import flash.utils.ByteArray;

import com.adobe.images.PNGEncoder;

import mx.containers.HBox;
import mx.containers.TitleWindow;
import mx.containers.VBox;
import mx.controls.ButtonBar;
import mx.controls.HRule;
import mx.controls.Label;
import mx.controls.TextArea;
import mx.core.UIComponent;
import mx.managers.PopUpManager;
import mx.validators.Validator;

import com.threerings.util.ValueEvent;

import com.threerings.display.CameraSnapshotter;

import com.threerings.orth.data.MediaDesc;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;
import com.threerings.flex.PopUpUtil;

import com.threerings.msoy.applets.image.CameraSnapshotControl;
import com.threerings.msoy.applets.image.DisplayCanvas;
import com.threerings.msoy.applets.image.NewImageDialog;
import com.threerings.msoy.applets.image.SizeRestriction;
import com.threerings.msoy.applets.net.Downloader;
import com.threerings.msoy.applets.net.Uploader;
// import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaDescImpl;

public class PopupFilePreview extends TitleWindow
{
    public function PopupFilePreview (parent :FileEditor, name :String, ctx :RemixContext)
    {
        _parent = parent;
        _name = name;
        _ctx = ctx;
        var entry :Object = ctx.pack.getFileEntry(name);
        _type = entry.type;
        _sizeRestriction = new SizeRestriction(Number(entry.width), Number(entry.height),
            Number(entry.maxWidth), Number(entry.maxHeight));
        var imageOk :Boolean = (_type == "Image" || _type == "DisplayObject" || _type == "Blob");
        var textType :Boolean = (_type == "XML" || _type == "Text");

        var externalAvail :Boolean = false;
        try {
            externalAvail = ExternalInterface.available;
        } catch (e :Error) {
            // nada
        }

        this.title = name;

        var box :VBox = new VBox();
        box.setStyle("horizontalAlign", "right");
        addChild(box);

        var hbox :HBox = new HBox();
        box.addChild(hbox);

        var controlBox :VBox = new VBox();
        var previewBox :VBox = new VBox();
        hbox.addChild(controlBox);
        hbox.addChild(previewBox);

        controlBox.addChild(makeHeader(_ctx.REMIX.get("t.file_change")));
        if (externalAvail && imageOk && !("true" == _ctx.params["noPickPhoto"])) {
            controlBox.addChild(makeBullet(
                new CommandLinkButton(_ctx.REMIX.get("b.use_image"), handleChoosePhoto)));
        }
        controlBox.addChild(makeBullet(
            new CommandLinkButton(_ctx.REMIX.get("b.use_newfile"), handleChooseFile)));
        if (imageOk) {
            controlBox.addChild(makeBullet(
                new CommandLinkButton(_ctx.REMIX.get("b.create_image"), handleChooseNewImage)));
        }
// Flash security prevents us from using most loaded content as 'data'
//        controlBox.addChild(makeBullet(
//            new CommandLinkButton(_ctx.REMIX.get("b.use_url"), handleChooseURL)));
        var filenames :Array = ctx.pack.getFilenames();
        if (filenames.length > 0) {
            // we need to wrap the array commandbutton arg in another array...
            controlBox.addChild(makeBullet(
                new CommandLinkButton(_ctx.REMIX.get("b.use_file"),
                handleChooseExistingFile, [ filenames ])));
        }
        if (imageOk && CameraSnapshotter.hasCamera()) {
            controlBox.addChild(makeBullet(
                new CommandLinkButton(_ctx.REMIX.get("b.use_snapshot"), handleChooseCamera)));
        }

        previewBox.addChild(makeHeader(_ctx.REMIX.get("t.preview")));
        if (imageOk) {
            _image = new DisplayCanvas(400, 400);
            _image.addEventListener(DisplayCanvas.SIZE_KNOWN, handleSizeKnown);
            previewBox.addChild(_image);

        } else if (textType) {
            _text = new TextArea();
            _text.width = 400;
            _text.height = 400;
            _text.editable = false;
            previewBox.addChild(_text);
        }

        hbox = new HBox();
        if (imageOk || textType) {
            hbox.addChild(_edit = new CommandButton(_ctx.REMIX.get("b.edit"), doEdit));
        }
        _label = new Label();
        _label.maxWidth = 250;
        hbox.addChild(_label);
        previewBox.addChild(hbox);

        var hrule :HRule = new HRule();
        hrule.percentWidth = 100;
        hrule.setStyle("strokeWidth", 1);
        hrule.setStyle("strokeColor", 0x000000);
        box.addChild(hrule);

        var buttonBar :ButtonBar = new ButtonBar();
        buttonBar.addChild(new CommandButton(_ctx.REMIX.get("b.cancel"), close, false));
        buttonBar.addChild(_ok = new CommandButton(_ctx.REMIX.get("b.save"), close, true));
        box.addChild(buttonBar);

        setFile(entry.value, ctx.pack.getFile(name));

        setPopped(true);
    }

    public function setFile (filename :String, bytes :ByteArray) :void
    {
        if (filename == null) {
            _filename = null;
            _label.text = _ctx.REMIX.get("m.no_file");

        } else {
            _filename = _ctx.createFilename(filename, bytes);
            _label.text = _filename;
        }
        _bytes = bytes;

        if (_image != null) {
            _image.setImage(bytes);

        } else if (_text != null) {
            _text.text = bytesToString(bytes);
        }
        _ok.enabled = (bytes != null);
        if (_edit != null) {
            _edit.enabled = (bytes != null);
        }
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

    /**
     * A callback from javascript when the photo chooser has picked a photo.
     */
    protected function setPhotoUrl (url :String) :void
    {
        // massage the url from javascript (I admit I'm cargo-culting here)
        url = "" + url;

        var lastDot :int = url.lastIndexOf(".");
        var name :String = "photo" + ((lastDot == -1) ? "" : url.substr(lastDot));

        var downloader :Downloader = new Downloader(_ctx);
        downloader.addEventListener(Event.COMPLETE, handleFileChosen);
        downloader.startDownload(url, name);
    }

    protected function handleChoosePhoto () :void
    {
        ExternalInterface.addCallback("setPhotoUrl", setPhotoUrl);
        ExternalInterface.call("pickPhoto");
    }

    protected function handleChooseFile () :void
    {
        var uploader :Uploader = new Uploader(_ctx, "remixuploadsvc", getFilters());
        uploader.addEventListener(Event.COMPLETE, handleFileUploadComplete);
    }

    protected function handleChooseNewImage () :void
    {
        var newImage :NewImageDialog = new NewImageDialog(_ctx, _sizeRestriction);
        newImage.addEventListener(Event.COMPLETE, handleImageCreated);
    }

    protected function handleChooseExistingFile (filenames :Array) :void
    {
        var efc :ExistingFileChooser = new ExistingFileChooser(_ctx, filenames);
        efc.addEventListener(Event.COMPLETE, handleFileChosen);
    }

    protected function handleFileUploadComplete (event :ValueEvent) :void
    {
        if (event.value == null) {
            // upload was cancelled, do nothing
            return;
        }

        // make a URL from the data we get back
        var stuff :Array = event.value as Array;
        var filename :String = stuff.shift();
        var response :String = stuff.shift();
        stuff = response.split(" ");

        var desc :HashMediaDesc = new HashMediaDesc(
            HashMediaDesc.stringToHash(stuff[0]), parseInt(stuff[1]), MediaDescImpl.NOT_CONSTRAINED);
        var url :String = desc.getMediaPath();

        // now, download the mofo
        var downloader :Downloader = new Downloader(_ctx);
        downloader.addEventListener(Event.COMPLETE, handleFileChosen);
        downloader.startDownload(url, filename);
    }

    protected function handleFileChosen (event :ValueEvent) :void
    {
        var value :Array = event.value as Array;
        if (value == null) {
            // nothing
            return;
        }

        var fname :String = value[0] as String;
        var data :ByteArray = value[1] as ByteArray;

        if (_image == null || _sizeRestriction == null ||
                (_sizeRestriction.forced == null && isNaN(_sizeRestriction.maxWidth) &&
                isNaN(_sizeRestriction.maxHeight))) {
            setFile(fname, data);

        } else {
            // otherwise, for now, let's just always route it through the editor
            // TODO: check the size, if it's cool then use it directly...
            doEdit(data, fname);
        }
    }

    protected function handleImageCreated (event :ValueEvent) :void
    {
        // go straight to editing mode
        doEdit(event.value, "drawn.png");
    }

    protected function handleChooseCamera () :void
    {
        new CameraSnapshotControl(_ctx, this, handleSnapshotTaken);
    }

    protected function handleSnapshotTaken (bitmapData :BitmapData) :void
    {
        if (bitmapData == null) {
            return; // do nothing, they cancelled
        }
        const fname :String = "cameragrab.png";
        if (_sizeRestriction.isValid(bitmapData.width, bitmapData.height)) {
            setFile(fname, PNGEncoder.encode(bitmapData));
        } else {
            doEdit(bitmapData, fname);
        }
    }

    protected function makeHeader (title :String) :UIComponent
    {
        var box :HBox = new HBox();
        box.percentWidth = 100;
        box.setStyle("backgroundColor", 0xf97527);

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
            return [ new FileFilter(_ctx.REMIX.get("m.DisplayObject"),
                "*.jpg;*.jpeg;*.gif;*.png;*.swf") ];

        case "Image":
            return [ new FileFilter(_ctx.REMIX.get("m.Image"), "*.jpg;*.jpeg;*.gif;*.png") ];

        case "XML":
            return [ new FileFilter(_ctx.REMIX.get("m.XML"), "*") ];

        case "Text":
            return [ new FileFilter(_ctx.REMIX.get("m.Text"), "*") ];

        default:
            throw new Error("Don't understand " + _type + " files yet.");
        }
    }

    protected function doEdit (image :Object = null, newFilename :String = null) :void
    {
        setPopped(false);

        _newFilename = newFilename;
        var source :Object = (image != null) ? image : _bytes;

        if (_image != null) {
            var iEditor :ImageEditor = new ImageEditor(_ctx, source, _sizeRestriction);
            iEditor.addEventListener(ImageEditor.IMAGE_UPDATED, handleEditorClosed);
            iEditor.addEventListener(ImageEditor.IMAGE_UPDATED, handleImageEditorClosed);
        } else if (_text != null) {
            var validator :Validator = (_type == "XML") ? new XMLValidator(_ctx) : null;
            var tEditor :PopupStringEditor = new PopupStringEditor(validator);
            tEditor.addEventListener(Event.CLOSE, handleEditorClosed);
            var entry :Object = _ctx.pack.getFileEntry(_name);
            entry.useArea = true;
            entry.value = _text.text;
            entry.fromString = stringToBytes;
            tEditor.open(_ctx, this, entry, updateTextValue);
        }
    }

    protected function handleEditorClosed (... ignored) :void
    {
        // re-pop ourselves
        setPopped(true);
    }

    protected function handleImageEditorClosed (event :ValueEvent) :void
    {
        var array :Array = event.value as Array;
        if (array != null) {
            updateFile(ByteArray(array[0]), array[1] as String);
        }
    }

    protected function updateTextValue (newValue :ByteArray) :void
    {
        updateFile(newValue);
    }

    protected function updateFile (ba :ByteArray, newExtension :String = null) :void
    {
        var file :String = (_newFilename != null) ? _newFilename : _filename;
        _newFilename = null;
        _filename = _ctx.createFilename(file, ba, newExtension);
        setFile(_filename, ba);
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

    protected function bytesToString (ba :ByteArray) :String
    {
        ba.position = 0;
        return ba.readUTFBytes(ba.bytesAvailable);
    }

    protected function stringToBytes (s :String) :ByteArray
    {
        var ba :ByteArray = new ByteArray();
        ba.writeUTFBytes(s);
        return ba;
    }

    protected var _parent :FileEditor;

    protected var _name :String;

    protected var _ctx :RemixContext;

    protected var _type :String;

    protected var _sizeRestriction :SizeRestriction;

    protected var _filename :String;

    protected var _ok :CommandButton;

    protected var _edit :CommandButton;

    protected var _image :DisplayCanvas;
    protected var _text :TextArea;

    protected var _label :Label;

    protected var _bytes :ByteArray;

    /* If non-null when we return from the editor, the base filename to use. */
    protected var _newFilename :String;
}
}
