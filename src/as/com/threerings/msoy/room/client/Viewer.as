//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.utils.ByteArray;

import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;

import com.threerings.msoy.client.MsoyParameters;

import com.threerings.msoy.data.UberClientModes;

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
            gotParams(MsoyParameters.get());
        } else {
            gotParams(params);
        }
    }

    /**
     * Load the media to display at a ByteArray.
     */
    public function loadBytes (bytes :ByteArray) :void
    {
        (_sprite as MsoySprite).setZippedMediaBytes(bytes);
    }

    protected function gotParams (params :Object) :void
    {
        var media :String = params["media"] as String;
        var mode :int = int(params["mode"]);

        switch (mode) {
        // AVATAR handled elsewhere. This class is TEMP, anyway.

        default: // FURNI, TOY, DECOR
            _sprite = new FurniViewerSprite();
            break;

        case UberClientModes.PET_VIEWER:
            _sprite = new PetViewerSprite();
            break;
        }

        var d :DisplayObject = _sprite as DisplayObject;

        d.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
        _sprite.setMedia(media);
        addChild(d);
    }

    protected function handleSizeKnown (event :ValueEvent) :void
    {
        var width :Number = event.value[0];
        var height :Number = event.value[1];
        var scale :Number = Math.min(1, Math.min(WIDTH / width, HEIGHT / height));
        _sprite.setScale(scale);

        var d :DisplayObject = _sprite as DisplayObject;
        d.x = (WIDTH - (scale * width)) / 2;
        d.y = (HEIGHT - (scale * height)) / 2;
    }

    protected var  _sprite :ViewerSprite;

    [Embed(source="../../../../../../../pages/images/item/detail_preview_bg.png")]
    protected static const BACKGROUND :Class;
}
}

import com.threerings.msoy.room.client.FurniSprite;
import com.threerings.msoy.room.client.PetSprite;

import com.threerings.msoy.room.data.FurniData;

interface ViewerSprite
{
    function setScale (scale :Number) :void;

    function setMedia (url :String) :void;
}

/**
 * A simple sprite used for viewing.
 */
class FurniViewerSprite extends FurniSprite
    implements ViewerSprite
{
    public function FurniViewerSprite ()
    {
        super(null, new FurniData());
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

    /** @inheritDoc */
    // from MsoySprite
    override protected function getSpriteMediaScaleX () :Number
    {
        return _scale;
    }

    /** @inheritDoc */
    // from MsoySprite
    override protected function getSpriteMediaScaleY () :Number
    {
        return _scale;
    }

    override protected function allowSetMedia () :Boolean
    {
        return true;
    }

    protected var _scale :Number = 1;
}

/**
 * A simple sprite used for viewing.
 */
class PetViewerSprite extends PetSprite
    implements ViewerSprite
{
    public function PetViewerSprite ()
    {
        super(null, null, {});
    }

    public function setScale (scale :Number) :void
    {
        _scale = scale;
        scaleUpdated();
    }

    override public function getState () :String
    {
        return null;
    }

    override protected function allowSetMedia () :Boolean
    {
        return true;
    }
}
