package {
import flash.display.Sprite;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.IBitmapDrawable;
import flash.display.Shape;

import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;

import flash.events.Event;

public class Ground extends Sprite
{
    /** The length and width of the track image tiles (they are square) */
    public static const IMAGE_SIZE :int = 1024;

    /** half the image size - used in a few calculations */
    public static const HALF_IMAGE_SIZE :int = IMAGE_SIZE / 2;

    public function Ground (camera :Camera)
    {
        _camera = camera;

        // set up the ground objects
        _track = new Track();
        _strips = new Array();
        var stripImage :Bitmap;
        var stripHeight :Number = BEGINNING_STRIP_HEIGHT;
        var stripHeightCeiling :int = 0;
        var totalHeight :Number = 0;
        var currentRegion :Number = 1;
        for (var strip :int = 0; totalHeight <= HEIGHT; strip++) {
            // split the image area into equal-height regions, from the beginning height to 1
            if ((totalHeight / HEIGHT) >= currentRegion / BEGINNING_STRIP_HEIGHT) {
                // avoid boundary condition resulting in stripHeight = 0
                stripHeight = stripHeight > 1 ? stripHeight - 1 : 1;
                currentRegion += 1;
            }
            totalHeight += stripHeight;
            // draw from the bottom up
            _strips[strip] = new BitmapData(WIDTH, stripHeight, false);
            stripImage = new Bitmap(_strips[strip]);
            stripImage.y = HEIGHT - totalHeight;
            addChild(stripImage);
        }

        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    /**
     * Handles Event.ENTER_FRAME.
     */
    protected function enterFrame (event :Event) :void
    {
        // shift along with the track
        if (_camera.position.y < -HALF_IMAGE_SIZE) {
            _camera.position.x += _track.moveTrackForward();
            _camera.position.y += IMAGE_SIZE;
        }
 
        var thisTransform :Matrix;
        var totalHeight :Number = 0;
        var thisHeight :Number = 0;
        for (var strip :int = 0; strip < _strips.length; strip++) {
            thisHeight = _strips[strip].height;
            totalHeight += thisHeight;
            thisTransform = new Matrix();
            // get the camera to the origin
            thisTransform.translate(0 - _camera.position.x, 0 - _camera.position.y);
            // rotate
            thisTransform.rotate(0 - _camera.angle);
            // scale
            var scaleFactor :Number = (_camera.height + HEIGHT - totalHeight) / _camera.height;
            thisTransform.scale(scaleFactor,scaleFactor);
            // move transformed space to view space
            thisTransform.translate(WIDTH / 2,  _camera.distance + thisHeight);
            // blank out this strip
            _strips[strip].draw(new BitmapData(WIDTH, thisHeight));
            // draw the background, then track using the calculated transform and a clipping rect
            var clipping :Rectangle = new Rectangle(0, 0, WIDTH, thisHeight);
            _strips[strip].draw(_track, thisTransform, null, null, clipping);
        }
    }

    /** track instance */
    protected var _track :Track;

    /** strips */
    protected var _strips :Array;

    /** camera instance */
    protected var _camera :Camera;

    /** height of the ground in display pixels */
    protected static const HEIGHT :int = 3 * UnderworldDrift.DISPLAY_HEIGHT / 4;

    /** width of the ground in display pixels */
    protected static const WIDTH :int = UnderworldDrift.DISPLAY_WIDTH;

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 6;
}
}
