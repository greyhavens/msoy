package com.threerings.msoy.applets.remixer {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.MouseEvent;

import flash.external.ExternalInterface;

import flash.utils.ByteArray;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.SimpleTextButton;

import com.threerings.msoy.world.data.MsoyDataPack;

import com.threerings.msoy.utils.Base64Sender;

[SWF(width="200", height="200")]
public class Remixer extends Sprite
{
    public function Remixer ()
    {
        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        var media :String = params["media"] as String;

        _pack = new MsoyDataPack(media);
        _pack.addEventListener(Event.COMPLETE, handlePackComplete);
        _pack.addEventListener(ErrorEvent.ERROR, handlePackError);
    }

    protected function handlePackComplete (event :Event) :void
    {
        var btn :SimpleTextButton = new SimpleTextButton("preview");
        btn.addEventListener(MouseEvent.CLICK, handlePreview);
        addChild(btn);
    }

    protected function handlePackError (event :ErrorEvent) :void
    {
        trace("Error loading: " + event.text)
    }

    protected function handlePreview (event :MouseEvent) :void
    {
        // send the bytes to our previewer
        var b64 :Base64Sender = new Base64Sender("remixPreview", "setMediaBytes");
        b64.sendBytes(_pack.toByteArray());
    }

    protected var _pack :MsoyDataPack;
}
}
