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
        _level = LevelFactory.createLevel(0, this);

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
     * Returns true if the kart is currently up against a wall.
     */
    public function drivingIntoWall () :Boolean
    {
        return _drivingIntoWall;
    }

    /**
     * Set the kart location.  This is used by Level at the beginning of the game to correctly
     * position the kart on a starting location.
     *
     * TODO: This will need to set camera angle as well
     */
    public function setKartLocation (location :Point) :void
    {
        var trans :Matrix = new Matrix();
        // TODO: Figure out what's up with the magic number 32
        trans.translate(location.x - _camera.position.x, location.y - _camera.position.y + 32);
        _camera.position = trans.transformPoint(_camera.position);
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
            // update status flags
            var y :int = HEIGHT - totalHeight;
            if (y <= UnderwhirledDrift.KART_LOCATION.y &&
                y + stripHeight > UnderwhirledDrift.KART_LOCATION.y) {
                thisTransform.invert();
                var transformedLoc :Point = thisTransform.transformPoint(
                    UnderwhirledDrift.KART_LOCATION);
                _drivingOnRoad = _level.isOnRoad(transformedLoc);
                _drivingIntoWall = _level.isOnWall(transformedLoc);
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

    /** flag to indicate that we're up against a wall */
    protected var _drivingIntoWall :Boolean = false;

    /** height of the ground in display pixels */
    protected static const HEIGHT :int = 3 * UnderwhirledDrift.DISPLAY_HEIGHT / 4;

    /** width of the ground in display pixels */
    protected static const WIDTH :int = UnderwhirledDrift.DISPLAY_WIDTH;

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 3;
}
}
