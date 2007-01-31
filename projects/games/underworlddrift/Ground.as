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

        _stripData = new BitmapData(WIDTH, HEIGHT, true, 0);
        addChild(new Bitmap(_stripData));
        _track = new Track();
        _scenery = new Scenery();
        addChild(_scenery);

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

        var translateRotate :Matrix = new Matrix();
        translateRotate.translate(0 - _camera.position.x, 0 - _camera.position.y);
        translateRotate.rotate(0 - _camera.angle);
        var thisTransform :Matrix = new Matrix();
        var stripHeight :Number = BEGINNING_STRIP_HEIGHT;
        var totalHeight :Number = 0;
        var currentRegion :Number = 1;
        for (var strip :int = 0; totalHeight < HEIGHT; strip++) {
            // split the image area into equal-height regions, from the beginning height to 1
            if ((totalHeight / HEIGHT) >= currentRegion / BEGINNING_STRIP_HEIGHT) {
                // avoid boundary conditions resulting in stripHeight = 0
                stripHeight = stripHeight > 1 ? stripHeight - 1 : 1;
                currentRegion += 1;
            }
            totalHeight += stripHeight;
            thisTransform.identity();
            thisTransform.concat(translateRotate);
            // scale
            var scaleFactor :Number = (HEIGHT - totalHeight) / _camera.height;
            thisTransform.scale(scaleFactor,scaleFactor);
            // move transformed space to view space
            thisTransform.translate(WIDTH / 2,  _camera.distance + 300 - totalHeight + stripHeight);
            // draw the track using the calculated transform and a clipping rect
            var clipping :Rectangle = new Rectangle(0, HEIGHT - totalHeight, WIDTH, 
                stripHeight);
            //_strips[strip].draw(_track, thisTransform, null, null, clipping);
            _stripData.draw(_track, thisTransform, null, null, clipping);
            // update off road flag
            var y :int = HEIGHT - totalHeight;
            if (y <= UnderworldDrift.KART_LOCATION.y &&
                y + stripHeight > UnderworldDrift.KART_LOCATION.y) {
                thisTransform.invert();
                _drivingOnRoad = _track.isOnRoad(thisTransform.transformPoint(
                    UnderworldDrift.KART_LOCATION));
            }
        }
        _scenery.updateItems(translateRotate, _camera.distance, 1 / _camera.height,
            _camera.height);
    }

    /** track instance */
    protected var _track :Track;

    /** scenery instance */
    protected var _scenery :Scenery;

    /** bitmap to draw strips on */
    protected var _stripData :BitmapData;

    /** camera instance */
    protected var _camera :Camera;

    /** flag to indicate that we're driving on the road */
    protected var _drivingOnRoad :Boolean = true;

    /** height of the ground in display pixels */
    protected static const HEIGHT :int = 3 * UnderworldDrift.DISPLAY_HEIGHT / 4;

    /** width of the ground in display pixels */
    protected static const WIDTH :int = UnderworldDrift.DISPLAY_WIDTH;

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 3;
}
}
