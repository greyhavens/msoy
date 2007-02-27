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
    /** height of the ground in display pixels */
    public static const HEIGHT :int = UnderwhirledDrift.DISPLAY_HEIGHT - 
        UnderwhirledDrift.SKY_HEIGHT;

    /** width of the ground in display pixels */
    public static const WIDTH :int = UnderwhirledDrift.DISPLAY_WIDTH;
    
    public function Ground (camera :Camera)
    {
        _camera = camera;

        _stripData = new BitmapData(WIDTH, HEIGHT, true, 0);
        addChild(new Bitmap(_stripData));
    }

    /**
     * set the level object used by this object.
     */
    public function setLevel (level :Level) :void
    {
        _level = level;

        // don't start attempting to render frames until we have a valid level object
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
        trans.translate(location.x - _camera.position.x, location.y - _camera.position.y + 
            UnderwhirledDrift.KART_OFFSET);
        _camera.position = trans.transformPoint(_camera.position);
    }

    /**
     * Set Scenery.  this and setKartLocation() might work better as a initFinished() to be called
     * from Level.
     */
    public function setScenery (scenery :Scenery) :void
    {
        addChild(_scenery = scenery);
    }

    /**
     * Get the current kart location.
     */
    public function getKartLocation () :Point
    {
        return _kartLocation;
    }

    /**
     * Handles Event.ENTER_FRAME.
     */
    protected function enterFrame (event :Event) :void
    {
        var translateRotate :Matrix = new Matrix();
        translateRotate.translate(-_camera.position.x, -_camera.position.y);
        translateRotate.rotate(-_camera.angle);
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
            thisTransform.translate(WIDTH / 2,  _camera.distance + HEIGHT - totalHeight + 
                stripHeight);
            // draw the track using the calculated transform and a clipping rect
            var clipping :Rectangle = new Rectangle(0, HEIGHT - totalHeight, WIDTH, 
                stripHeight);
            _stripData.draw(_level, thisTransform, null, null, clipping);
            // update status flags
            var y :int = HEIGHT - totalHeight;
            if (y <= UnderwhirledDrift.KART_LOCATION.y &&
                y + stripHeight > UnderwhirledDrift.KART_LOCATION.y) {
                thisTransform.invert();
                _kartLocation = thisTransform.transformPoint(
                    UnderwhirledDrift.KART_LOCATION);
                _drivingOnRoad = _level.isOnRoad(_kartLocation);
                _drivingIntoWall = _level.isOnWall(_kartLocation);
            }
        }
        if (_scenery != null) {
            _scenery.updateItems(translateRotate, _camera);
        }
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

    /** Obstacles */
    protected var _scenery :Scenery;

    /** The current location of the kart */
    protected var _kartLocation :Point = new Point();

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 3;
}
}
