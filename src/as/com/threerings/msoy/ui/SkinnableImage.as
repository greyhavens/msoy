//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;

import mx.controls.Image;
import mx.core.IFlexDisplayObject;

[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * Extends of mx.control.Image functionality with automatic image loading from the style sheet
 * (e.g. via an external style sheet file).
 */
public class SkinnableImage extends Image
{
    public function SkinnableImage (styleName :String = null)
    {
        if (styleName != null) {
            this.styleName = styleName;
        }
    }

    override public function styleChanged (styleProp:String) :void
    {
        super.styleChanged(styleProp);

        var cls : Class = Class(getStyle("backgroundSkin"));
        if (cls != null) {
            updateSkin(cls);
        }
    }

    protected function updateSkin (skinclass : Class) : void
    {
        if (_skin != null) {
            removeChild(_skin);
        }

        _skin = DisplayObject (IFlexDisplayObject (new skinclass()));
        this.width = _skin.width;
        this.height = _skin.height;
        _skin.x = 0;
        _skin.y = 0;
        addChild(_skin);
    }

    protected var _skin : DisplayObject;
}
}
