//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;

import flash.utils.ByteArray;

import mx.controls.Label;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.applets.net.Downloader;

public class FileEditor extends FieldEditor
{
    public function FileEditor (ctx :RemixContext, name :String)
    {
        var entry :Object = ctx.pack.getFileEntry(name);
        super(ctx, name, entry);
    }

    /**
     * "Inject" the media at the specified url into this file field, as if the user
     * had directly uploaded it.
     */
    public function injectMedia (url :String) :void
    {
        var dl :Downloader = new Downloader(_ctx);
        dl.addEventListener(Event.COMPLETE, handleInjectionComplete);
        dl.startDownload(url);
    }

    protected function handleInjectionComplete (event :ValueEvent) :void
    {
        var filename :String = String(event.value[0]);
        var bytes :ByteArray = ByteArray(event.value[1]);

        filename = _ctx.createFilename(_ctx.changeFilename(filename, "image"), bytes);
        updateValue(filename, bytes);
    }

    override protected function getUI (entry :Object) :Array
    {
        _label = new Label();
        _label.selectable = false;
        //_label.setStyle("color", NAME_AND_VALUE_COLOR);
        _label.text = entry.value as String;

        var change :CommandButton = createEditButton(showFile);
        change.enabled = (entry.value != null);

        return [ _label, change, change ];
    }

    internal function updateValue (filename :String, bytes :ByteArray) :void
    {
        if (filename == null) {
            if (_bytes == null) {
                _used.selected = (_ctx.pack.getFileEntry(_name).value != null);
            }
            return;
        }

        _component.enabled = true;
        _label.text = filename;
        _bytes = bytes;
        updateEntry();

        _ctx.pack.replaceFile(_name, filename, bytes);
        setChanged();
    }

    override protected function handleUsedToggled (selected :Boolean) :void
    {
        if (selected && _bytes == null) {
            // pop up the damn chooser
            showFile();

        } else {
            super.handleUsedToggled(selected);
        }
    }

    override protected function updateEntry () :void
    {
        if (_bytes != null) {
            _ctx.pack.replaceFile(_name, _used.selected ? _label.text : null, _bytes);
            setChanged();
        }
    }

    protected function showFile () :void
    {
        new PopupFilePreview(this, _name, _ctx);
    }

    protected var _label :Label;

    protected var _bytes :ByteArray;
}
}
