package com.threerings.msoy.ui {

import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Graphics;

import flash.geom.ColorTransform;
import flash.geom.Matrix;

import mx.skins.ProgrammaticSkin;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.util.MultiLoader;

/**
 * Class to use as the default image when the source image is not set. This is also used to
 * determine the "measured" size of the skin.
 *  
 * @default null
 */
[Style(name="defaultImage", type="Class", inherit="no")]

/**
 * Display object to use as the source image for the skin. This will be scaled to the width and
 * height being displayed. The color will also be adjusted depending on how name given to the skin.
 * 
 * @default null
 */
[Style(name="source", type="DisplayObject", inherit="no")]

/**
 * Implements button skinning using a single image set at runtime. Brightens the image for roll
 * over and translates it down when clicked.
 * 
 */
public class ImageButtonSkin extends ProgrammaticSkin
{
    /** @inheritDoc */
    // from ProgrammaticSkin
    override public function get measuredWidth () :Number
    {
        measure();
        return _measuredWidth;
    }

    /** @inheritDoc */
    // from ProgrammaticSkin
    override public function get measuredHeight () :Number
    {
        measure();
        return _measuredHeight;
    }

    /** @inheritDoc */
    // from ProgrammaticSkin
    override protected function updateDisplayList (w :Number, h :Number) :void
    {
        // Non-positive values will crash the bitmap render, just bail
        if (w <= 0 || h <= 0) {
            return;
        }

        // Fall back to default if no source is given
        var source :DisplayObject = getStyle("source") as DisplayObject;
        if (source == null) {
            var defaultImage :Class = getStyle("defaultImage") as Class;
            if (defaultImage == null) {
                return;
            }
            
            source = new defaultImage();
        }

        // Brighten or darken
        // TODO: disabledSkin isn't used in msoy, would probably need a better effect
        var adjust :ColorTransform = null;
        if (name == "overSkin" || name == "downSkin") {
            adjust = new ColorTransform(1, 1, 1, 25, 25, 25);
        } else if (name == "disabledSkin") {
            adjust = new ColorTransform(1, 1, 1, -25, -25, -25);
        }

        // Scale
        var scale :Matrix = new Matrix();
        scale.scale(w / source.width, h / source.height);

        // Translate if pressed
        if (name == "downSkin") {
            scale.translate(0, 1);
        }

        // Scale to bitmap
        var bmd :BitmapData = new BitmapData(w, h, true, 0x000000);
        bmd.draw(source, scale, adjust);

        // Render onto skin
        var g:Graphics = graphics;
        g.clear();
        g.beginBitmapFill(bmd);
        g.drawRect(0, 0, w, h);
        g.endFill();
    }

    /**
     * Fills in the measured width and height using the default image.
     */
    protected function measure () :void
    {
        if (_measured) {
            return;
        }

        var defaultImage :Class = getStyle("defaultImage") as Class;
        if (defaultImage != null) {
            var image :DisplayObject = new defaultImage();
            _measuredWidth = image.width;
            _measuredHeight = image.height;
        }

        _measured = true;
    }

    protected var _measuredWidth :Number = 50;
    protected var _measuredHeight :Number = 22;
    protected var _measured :Boolean;
}
}
