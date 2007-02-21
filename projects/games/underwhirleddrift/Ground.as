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
    public function Ground (camera :Camera)
    {
        _camera = camera;

        _stripData = new BitmapData(WIDTH, HEIGHT, true, 0);
        addChild(new Bitmap(_stripData));
        _level = new Level(0);

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
            //_strips[strip].draw(_level, thisTransform, null, null, clipping);
            _stripData.draw(_level, thisTransform, null, null, clipping);
            // update off road flag
            var y :int = HEIGHT - totalHeight;
            if (y <= UnderwhirledDrift.KART_LOCATION.y &&
                y + stripHeight > UnderwhirledDrift.KART_LOCATION.y) {
                thisTransform.invert();
                _drivingOnRoad = _level.isOnRoad(thisTransform.transformPoint(
                    UnderwhirledDrift.KART_LOCATION));
            }
        }
        //_scenery.updateItems(translateRotate, _camera.distance, 1 / _camera.height,
            //_camera.height);
    }

    /** track instance */
    protected var _level :Level;

    /** bitmap to draw strips on */
    protected var _stripData :BitmapData;

    /** camera instance */
    protected var _camera :Camera;

    /** flag to indicate that we're driving on the road */
    protected var _drivingOnRoad :Boolean = true;

    /** height of the ground in display pixels */
    protected static const HEIGHT :int = 3 * UnderwhirledDrift.DISPLAY_HEIGHT / 4;

    /** width of the ground in display pixels */
    protected static const WIDTH :int = UnderwhirledDrift.DISPLAY_WIDTH;

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 3;
}
}
