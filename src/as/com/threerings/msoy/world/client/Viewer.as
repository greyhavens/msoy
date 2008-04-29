//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Bitmap;
import flash.display.Sprite;

import com.threerings.util.ParameterUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * A generic viewer, like the AvatarViewer, but for any item.
 * This will grow and change.
 */
[SWF(width="320", height="240")]
public class Viewer extends Sprite
{
    public static const WIDTH :int = 320; // MediaDesc PREVIEW_SIZE
    public static const HEIGHT :int = 240; // MediaDesc PREVIEW_SIZE

    public function Viewer (params :Object = null)
    {
        var bmp :Bitmap = Bitmap(new BACKGROUND());
        graphics.beginBitmapFill(bmp.bitmapData);
        graphics.drawRect(0, 0, WIDTH, HEIGHT);
        graphics.endFill();

        if (params == null) {
            ParameterUtil.getParameters(this, gotParams);
        } else {
            gotParams(params);
        }
    }

    protected function gotParams (params :Object) :void
    {
        var media :String = params["media"] as String;

        _sprite = new ViewerSprite();
        _sprite.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
        _sprite.setMedia(media);
        addChild(_sprite);
    }

    protected function handleSizeKnown (event :ValueEvent) :void
    {
        var width :Number = event.value[0];
        var height :Number = event.value[1];
        var scale :Number = Math.min(1, Math.min(WIDTH / width, HEIGHT / height));
        _sprite.setScale(scale);

        _sprite.x = (WIDTH - (scale * width)) / 2;
        _sprite.y = (HEIGHT - (scale * height)) / 2;
    }

    protected var  _sprite :ViewerSprite;

    [Embed(source="../../../../../../../pages/images/item/detail_preview_bg.png")]
    protected static const BACKGROUND :Class;
}
}

import com.threerings.msoy.world.client.EntityBackend;
import com.threerings.msoy.world.client.MsoySprite;

/**
 * A generic MsoySprite used for viewing, with no backend logic.
 */
class ViewerSprite extends MsoySprite
{
    public function ViewerSprite ()
    {
        super(null);
    }

    public function setScale (scale :Number) :void
    {
        _scale = scale;
        scaleUpdated();
    }

    override public function capturesMouse () :Boolean
    {
        return true;
    }

    override public function getMediaScaleX () :Number
    {
        return _scale;
    }

    override public function getMediaScaleY () :Number
    {
        return _scale;
    }

    override protected function createBackend () :EntityBackend
    {
        return null;
    }

    override protected function allowSetMedia () :Boolean
    {
        return true;
    }

    protected var _scale :Number = 1;
}
