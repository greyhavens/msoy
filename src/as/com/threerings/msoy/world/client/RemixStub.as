package com.threerings.msoy.world.client {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.external.ExternalInterface;

import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;

import flash.utils.ByteArray;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.SimpleTextButton;

import com.threerings.msoy.utils.Base64Sender;

[SWF(width="200", height="200")]
public class RemixStub extends Sprite
{
    public function RemixStub ()
    {
        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        var media :String = params["media"] as String;

        _loader = new URLLoader();
        _loader.dataFormat = URLLoaderDataFormat.BINARY;
        _loader.addEventListener(Event.COMPLETE, handlePackComplete);
        _loader.load(new URLRequest(media));
    }

    protected function handlePackComplete (event :Event) :void
    {
        var btn :SimpleTextButton = new SimpleTextButton("preview");
        btn.addEventListener(MouseEvent.CLICK, handlePreview);
        addChild(btn);
    }

    protected function handlePreview (event :MouseEvent) :void
    {
        var b64 :Base64Sender = new Base64Sender("setMediaBytes");
        b64.sendBytes(ByteArray(_loader.data));
    }

    protected var _loader :URLLoader;
}
}
