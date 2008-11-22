//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.TextEvent;

import mx.controls.ButtonBar;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.containers.VBox;

import mx.events.FlexEvent;
import mx.events.ValidationResultEvent;

import mx.validators.RegExpValidator;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.applets.net.Downloader;

public class URLFileChooser extends Downloader
{
    public function URLFileChooser (ctx :RemixContext)
    {
        super(ctx);

        _ctx = ctx;

        title = ctx.REMIX.get("t.choose_url");
    }

    override public function startDownload (url :String = null, forcedName :String = null) :void
    {
        url = url || _entry.text;

        if (url != null) {
            _ok.enabled = false;
            _entry.enabled = false;
        }

        super.startDownload(url, forcedName);
    }

    override protected function configureUI (box :VBox) :void
    {
        var lbl :Label = new Label();
        lbl.text = _ctx.REMIX.get("m.url_prompt");
        box.addChild(lbl);

        _entry = new TextInput();
        _entry.text = "http://"; // start 'em off right
        _entry.addEventListener(FlexEvent.ENTER, handleEntryEnter);
        _entry.percentWidth = 100;
        _entry.minWidth = 300;
        box.addChild(_entry);

        super.configureUI(box);

        var bar :ButtonBar = new ButtonBar();
        bar.addChild(_ok = new CommandButton(_ctx.REMIX.get("b.ok"), startDownload));
        _ok.enabled = false;
        bar.addChild(new CommandButton(_ctx.REMIX.get("b.cancel"), close));
        box.addChild(bar);

        _validator = new RegExpValidator();
        _validator.expression = URL_REGEXP;
        _validator.flags = URL_FLAGS;
        _validator.source = _entry;
        _validator.property = "text";
        _validator.addEventListener(ValidationResultEvent.VALID, checkValid);
        _validator.addEventListener(ValidationResultEvent.INVALID, checkValid);
        _validator.triggerEvent = Event.CHANGE; //TextEvent.TEXT_INPUT;
        _validator.trigger = _entry;
    }

    protected function handleEntryEnter (event :FlexEvent) :void
    {
        _validator.validate();
        if (_ok.enabled) {
            startDownload();
        }
    }

    protected function checkValid (event :ValidationResultEvent) :void
    {
        _ok.enabled = (event.type == ValidationResultEvent.VALID);
    }

    override protected function downloadStopped () :void
    {
        super.downloadStopped();
        _entry.enabled = true;
    }

    protected var _ctx :RemixContext;

    protected var _entry :TextInput;

    protected var _ok :CommandButton;

    protected var _validator :RegExpValidator;

    protected static const URL_REGEXP :String = "(http|https|ftp)://\\S+";
    
    protected static const URL_FLAGS :String = "i";
}
}
