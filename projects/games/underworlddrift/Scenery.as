package {

import flash.display.Sprite;
import flash.display.DisplayObject;

import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.collections.ArrayCollection;
import mx.collections.IViewCursor;

public class Scenery extends Sprite
{
    public function Scenery () 
    {
        generateBlock();
        moveSceneryForward(0);
        moveSceneryForward(0);
    }

    /**
     * Called when the objects should be re-scaled for display in a new frame.
     */
    public function updateItems (translateRotate :Matrix, distance :Number, maxScale :Number,
        cameraHeight :Number, maxStripHeight :int) :void
    {
        var thisTransform :Matrix = new Matrix();
        var viewRect :Rectangle = new Rectangle(-distance / 2, -distance, distance, 
            distance - distance / maxScale);
        var transformedPoint :Point;
        var cursor :IViewCursor = _items.createCursor();
        while (!cursor.afterLast) {
            thisTransform.identity();
            // translate and rotate item's origin
            thisTransform.concat(translateRotate);
            transformedPoint = thisTransform.transformPoint(cursor.current.origin);
            // check if item's origin is in potential display area
            if (viewRect.containsPoint(transformedPoint)) {
                // scale and translate origin to the display area
                var rowScaleFactor :Number = 1 + (1 - (((-transformedPoint.y) - 
                    distance/maxScale) / (distance - distance/maxScale))) * (maxScale - 1);
                var scaleFactor :Number = distance / (-transformedPoint.y);
                thisTransform.scale(scaleFactor, scaleFactor);
                var thisHeight :Number = cameraHeight * (rowScaleFactor - 1);
                thisTransform.translate(UnderworldDrift.DISPLAY_WIDTH / 2, 
                    distance + thisHeight);
                transformedPoint = thisTransform.transformPoint(cursor.current.origin);
                Log.testing("transformedPoint: " + transformedPoint);
                cursor.current.sprite.x = transformedPoint.x;
                cursor.current.sprite.y = transformedPoint.y;
                // scale item
                cursor.current.sprite.width = cursor.current.startWidth * scaleFactor * 0.1;
                cursor.current.sprite.height = cursor.current.startHeight * scaleFactor * 0.1;
            } else {
                // make sure its off the display
                cursor.current.sprite.x = cursor.current.sprite.y = -Ground.HALF_IMAGE_SIZE;
                cursor.current.sprite.width = cursor.current.startWidth;
                cursor.current.sprite.height = cursor.current.startHeight;
            }
            cursor.moveNext();
        }
    }

    /** 
     * As in the Track, this slides all the objects down by one IMAGE_SIZE, so that the track
     * can continue forward forever.
     */
    public function moveSceneryForward (xShift :int) :void
    {
        var cursor :IViewCursor = _items.createCursor();
        while (!cursor.afterLast) {
            if (cursor.current.origin.y > Ground.HALF_IMAGE_SIZE) {
                removeChild(cursor.current.sprite);
                cursor.remove();
            } else {
                cursor.current.origin.y += Ground.IMAGE_SIZE;
                cursor.current.origin.x += xShift;
                cursor.moveNext();
            }
        }
        generateBlock();
    }

    protected function generateBlock () :void
    {
        for (var ii :int = 0; ii < 10; ii++) {
            var item :Object = {origin: new Point(), sprite :new SKULL()};
            item.origin.x = Math.floor(Math.random() * Ground.IMAGE_SIZE) -
                Ground.HALF_IMAGE_SIZE;
            item.origin.y = Math.floor(Math.random() * Ground.IMAGE_SIZE) -
                Ground.HALF_IMAGE_SIZE - Ground.IMAGE_SIZE;
            item.startWidth = item.sprite.width;
            item.startHeight = item.sprite.height;
            // get that new sprite off the display, thank you
            item.sprite.x = item.sprite.y = -Ground.HALF_IMAGE_SIZE;
            addChild(item.sprite);
            _items.addItem(item);
        }
    }

    /** Collection of scenery objects */
    protected var _items :ArrayCollection = new ArrayCollection();

    /** skull object */
    [Embed(source='rsrc/objects.swf#monster_skull')]
    protected static const SKULL :Class;
}
}
