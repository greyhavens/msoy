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
        _scenery = new Scenery();
        addChild(_scenery);
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
            addChildAt(stripImage, strip);
        }

        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    /**
     * Returns true if the kart is currently driving on the road surface.
     */
    public function drivingOnRoad () :Boolean
    {
        return _drivingOnRoad;
    }

    /**
     * Handles Event.ENTER_FRAME.
     */
    protected function enterFrame (event :Event) :void
    {
        // shift along with the track
        if (_camera.position.y < -HALF_IMAGE_SIZE) {
            var xShift :int = _track.moveTrackForward();
            _scenery.moveSceneryForward(xShift);
            _camera.position.x += xShift;
            _camera.position.y += IMAGE_SIZE;
        }

        _scenery.clearItems();
 
        var thisTransform :Matrix = new Matrix();
        var totalHeight :Number = 0;
        var thisHeight :Number = 0;
        for (var strip :int = 0; strip < _strips.length; strip++) {
            thisHeight = _strips[strip].height;
            totalHeight += thisHeight;
            thisTransform.identity();
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
            // draw the track using the calculated transform and a clipping rect
            var clipping :Rectangle = new Rectangle(0, 0, WIDTH, thisHeight);
            _strips[strip].draw(_track, thisTransform, null, null, clipping);
            // update scenery
            _scenery.setTransform(totalHeight - thisHeight, totalHeight, thisTransform, 
                scaleFactor);
            //_strips[strip].draw(_scenery, thisTransform, null, null, clipping);
            // update off road flag
            var bitmap :Bitmap = getChildAt(strip) as Bitmap;
            if (bitmap.y <= UnderworldDrift.KART_LOCATION.y && 
                bitmap.y + bitmap.height > UnderworldDrift.KART_LOCATION.y) {
                thisTransform.invert();
                _drivingOnRoad = _track.isOnRoad(thisTransform.transformPoint(new Point(
                    UnderworldDrift.KART_LOCATION.x, 0)));
            }
        }

        _scenery.updateItems();
    }

    /** track instance */
    protected var _track :Track;

    /** scenery instance */
    protected var _scenery :Scenery;

    /** strips */
    protected var _strips :Array;

    /** camera instance */
    protected var _camera :Camera;

    /** flag to indicate that we're driving on the road */
    protected var _drivingOnRoad :Boolean = true;

    /** height of the ground in display pixels */
    protected static const HEIGHT :int = 3 * UnderworldDrift.DISPLAY_HEIGHT / 4;

    /** width of the ground in display pixels */
    protected static const WIDTH :int = UnderworldDrift.DISPLAY_WIDTH;

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 6;
}
}
