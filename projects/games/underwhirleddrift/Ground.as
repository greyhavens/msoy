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

        UnderwhirledDrift.registerEventListener(this, Event.ENTER_FRAME, enterFrame);
    }

    /**
     * set the level object used by this object.
     */
    public function setLevel (level :Level) :void
    {
        _level = level;
    }

    public function getLevel () :Level 
    {
        return _level;
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
        // don't use the kart offset for now, see comment on wackiness in enterFrame
        trans.translate(location.x - _camera.position.x, location.y - _camera.position.y);
        //trans.translate(location.x - _camera.position.x, location.y - _camera.position.y + 
            //UnderwhirledDrift.KART_OFFSET);
        _camera.position = trans.transformPoint(_camera.position);
        // TEMP: Until we do camera angles too
        _camera.angle = 0;
    }

    /**
     * Set Scenery.  
     */
    public function setScenery (scenery :Scenery) :void
    {
        if (_scenery != null) {
            removeChild(_scenery);
        } 
        addChild(_scenery = scenery);
    }

    public function getScenery () :Scenery
    {
        return _scenery;
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
        var findKart :Matrix = new Matrix();
        findKart.rotate(_camera.angle);
        findKart.translate(_camera.position.x, _camera.position.y);
        var kartLocation :Point = findKart.transformPoint(new Point(0, Camera.DISTANCE_FROM_KART));
        var translateRotate :Matrix = new Matrix();
        translateRotate.translate(-kartLocation.x, -kartLocation.y);
        translateRotate.rotate(-_camera.angle);
        // This *should* be putting the rotation under the kart, and the putting the camera back
        // where it should be.  Its not working, so for now we're just going to fake out the 
        // system by leaving the view back where it was, and calculating the kart's on-screen
        // position the same way we used to.  This is wacky and wrong, but it works for now.
        //translateRotate.translate(0, -Camera.DISTANCE_FROM_KART);
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
            // update status flags - This is wacky and wrong, see comment above
            var y :int = HEIGHT - totalHeight;
            if (y <= UnderwhirledDrift.KART_LOCATION.y &&
                y + stripHeight > UnderwhirledDrift.KART_LOCATION.y) {
                thisTransform.invert();
                _kartLocation = thisTransform.transformPoint(
                    UnderwhirledDrift.KART_LOCATION);
            }
        }
        if (_scenery != null) {
            _scenery.updateItems(translateRotate, _camera, _kartLocation);
        }
    }

    /** track instance */
    protected var _level :Level;

    /** bitmap to draw strips on */
    protected var _stripData :BitmapData;

    /** camera instance */
    protected var _camera :Camera;

    /** Obstacles */
    protected var _scenery :Scenery;

    /** The current location of the kart */
    protected var _kartLocation :Point = new Point();

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 3;
}
}
