package {
import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.IBitmapDrawable;
import flash.display.Shape;

import flash.geom.Matrix;

import flash.events.Event;

public class Ground extends Sprite
{
    public function Ground ()
    {
        _background = new Shape();
        _background.graphics.beginBitmapFill((new BACKGROUND_IMAGE() as Bitmap).bitmapData);
        // draw over the same coordinates as road tiles, so that the same transform applies
        _background.graphics.drawRect(-HALF_IMAGE_SIZE, -HALF_IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE);
        _background.graphics.endFill();
        
        // set up the ground objects
        _trackVector = new TRACK_IMAGE();
        _transforms = new Array();
        _strips = new Array();
        var stripImage :Bitmap;
        var stripHeight :Number = BEGINNING_STRIP_HEIGHT;
        var stripHeightCeiling :int = 0;
        var totalHeight :Number = 0;
        for (var strip :int = 0; totalHeight <= HEIGHT; strip++,
            stripHeight = (stripHeight - SCALE_FACTOR) > 1 ? stripHeight - SCALE_FACTOR : 1) {
            // find the ceiling for this value, but don't let ceiling(1) = 2
            stripHeightCeiling = Math.round(stripHeight + 0.49);
            totalHeight += stripHeightCeiling;
            _transforms[strip] = new Matrix();
            _transforms[strip].scale(
                X_SCALE * (1 - (totalHeight / HEIGHT) * WIDTH_PERCENT_SCALE), 1);
            _transforms[strip].translate(X_SHIFT, Y_SHIFT + strip * SOURCE_HEIGHT + SOURCE_HEIGHT);
            _transforms[strip].scale(1, stripHeightCeiling / SOURCE_HEIGHT);
            // draw from the bottom up
            _strips[strip] = new BitmapData(WIDTH, stripHeightCeiling, false);
            stripImage = new Bitmap(_strips[strip]);
            stripImage.y = HEIGHT - totalHeight;
            addChild(stripImage);
        }

        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    public function moveForward () :void 
    {
        translateTransforms(0, 1);
    }

    public function moveBackward () :void
    {
        translateTransforms(0, -1);
    }

    protected function translateTransforms(x :Number, y :Number) :void
    {
        for (var i :int = 0; i < _transforms.length; i++) {
            _transforms[i].translate(x, y);
        }
    }

    /**
     * Handles Event.ENTER_FRAME.
     */
    protected function enterFrame (event :Event) :void
    {
        for (var strip :int = 0; strip < _strips.length; strip++) {
            _strips[strip].draw(_background, _transforms[strip]);
            _strips[strip].draw(_trackVector, _transforms[strip]);
        }
    }

    /** background shape instance */
    protected var _background :Shape;

    /** track tile instance */
    protected var _trackVector :IBitmapDrawable;

    /** strips */
    protected var _strips :Array;

    /** transforms */
    protected var _transforms :Array;

    /** track image */
    [Embed(source='rsrc/track.swf#track3')]
    protected static const TRACK_IMAGE :Class;

    /** test background tile image */
    [Embed(source='rsrc/pixel_magma.png')]
    protected static const BACKGROUND_IMAGE :Class;

    /** The length and width of the track image tiles (they are square) */
    protected static const IMAGE_SIZE :int = 1024;

    /** half the image size - used in a few calculations */
    protected static const HALF_IMAGE_SIZE :int = IMAGE_SIZE / 2;

    /** height of the ground in display pixels */
    protected static const HEIGHT :int = UnderworldDrift.DISPLAY_HEIGHT / 2;

    /** width of the ground in display pixels */
    protected static const WIDTH :int = UnderworldDrift.DISPLAY_WIDTH;

    /** align the vector so that the the display area is centered */
    protected static const X_SHIFT :Number = HALF_IMAGE_SIZE - (IMAGE_SIZE - WIDTH) / 2;

    /** align the bottom of the vector with the top of the display area. */
    protected static const Y_SHIFT :Number = 0 - HALF_IMAGE_SIZE;

    /** The amount to stretch the X direction at the bottom of the image */
    protected static const X_SCALE :Number = 7;

    /** The number of pixels from the source image in the y direction to scale into a single 
     * strip */
    protected static const SOURCE_HEIGHT :int = 9;

    /** The amount to reduce the size of the strip per row. */
    protected static const SCALE_FACTOR :Number = 0.05;

    /** Reduce the effect of the calculation that determines how fast the width is reduced for
     * perspective. */
    protected static const WIDTH_PERCENT_SCALE :Number = 0.9;

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 4;
}
}
