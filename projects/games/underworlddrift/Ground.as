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
    public function Ground ()
    {
        _background = new Shape();
        _background.graphics.beginBitmapFill((new BACKGROUND_IMAGE() as Bitmap).bitmapData);
        // draw over the same coordinates as road tiles, so that the same transform applies
        _background.graphics.drawRect(-HALF_IMAGE_SIZE, -HALF_IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE);
        _background.graphics.endFill();
        
        // set up the ground objects
        _trackVector = new TRACK_IMAGE();
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

    public function moveForward (moving :Boolean) :void 
    { 
        _movingForward = moving;
    }

    public function moveBackward (moving :Boolean) :void
    {
        _movingBackward = moving;
    }

    public function turnLeft (turning :Boolean) :void
    {
        _turningLeft = turning;
    }

    public function turnRight (turning :Boolean) :void
    {
        _turningRight = turning
    }

    /**
     * Handles Event.ENTER_FRAME.
     */
    protected function enterFrame (event :Event) :void
    {
        // TODO: base these speeds on something fairer than enterFrame.  Using this method,
        // the person with the fastest computer (higher framerate) gets to drive more quickly.
        // rotate camera
        if (_turningRight) {
            _cameraAngle += 0.0745;
        } else if (_turningLeft) {
            _cameraAngle -= 0.0745;
        }

        // move camera
        var rotation :Matrix;
        if (_movingForward) {
            rotation = new Matrix();
            rotation.rotate(_cameraAngle);
            _cameraPosition = _cameraPosition.add(rotation.transformPoint(new Point(0, -10)));
        } else if (_movingBackward) {
            rotation = new Matrix();
            rotation.rotate(_cameraAngle);
            _cameraPosition = _cameraPosition.add(rotation.transformPoint(new Point(0, 10)));
        }

        var thisTransform :Matrix;
        var totalHeight :Number = 0;
        var thisHeight :Number = 0;
        for (var strip :int = 0; strip < _strips.length; strip++) {
            thisHeight = _strips[strip].height;
            totalHeight += thisHeight;
            thisTransform = new Matrix();
            // get the camera to the origin
            thisTransform.translate(0 - _cameraPosition.x, 0 - _cameraPosition.y);
            // rotate
            thisTransform.rotate(0 - _cameraAngle);
            // scale
            var scaleFactor :Number = (_cameraHeight + HEIGHT - totalHeight) / _cameraHeight;
            thisTransform.scale(scaleFactor,scaleFactor);
            // move transformed space to view space
            thisTransform.translate(WIDTH / 2,  _cameraDistance + thisHeight);
            // blank out display
            _strips[strip].draw(new BitmapData(WIDTH, HEIGHT));
            _strips[strip].draw(_background, thisTransform);
            _strips[strip].draw(_trackVector, thisTransform);
        }
    }

    /** background shape instance */
    protected var _background :Shape;

    /** track tile instance */
    protected var _trackVector :IBitmapDrawable;

    /** strips */
    protected var _strips :Array;

    /** angle of camera */
    protected var _cameraAngle :Number = 0;

    /** position of camera */
    protected var _cameraPosition :Point = new Point(0, HALF_IMAGE_SIZE);

    /** height of the camera */
    protected var _cameraHeight :Number = 10;

    /** distance from the camera to the projection plane */
    protected var _cameraDistance :Number = 800;

    /** flag to indicate forward movement */
    protected var _movingForward :Boolean = false;

    /** flag to indicate backward movement */
    protected var _movingBackward :Boolean = false;

    /** flag to indicate rotation to the right */
    protected var _turningRight :Boolean = false;

    /** flag to indicate rotation to the left */
    protected var _turningLeft :Boolean = false;

    /** track image */
    [Embed(source='rsrc/track.swf#track3')]
    protected static const TRACK_IMAGE :Class;

    /** test background tile image */
    [Embed(source='rsrc/blue_ground.png')]
    protected static const BACKGROUND_IMAGE :Class;

    /** The length and width of the track image tiles (they are square) */
    protected static const IMAGE_SIZE :int = 1024;

    /** half the image size - used in a few calculations */
    protected static const HALF_IMAGE_SIZE :int = IMAGE_SIZE / 2;

    /** height of the ground in display pixels */
    protected static const HEIGHT :int = UnderworldDrift.DISPLAY_HEIGHT / 2;

    /** width of the ground in display pixels */
    protected static const WIDTH :int = UnderworldDrift.DISPLAY_WIDTH;

    /** The height of the largest strip, at the bottom of the image. */
    protected static const BEGINNING_STRIP_HEIGHT :int = 10;
}
}
