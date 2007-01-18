package {

import flash.display.Sprite;
import flash.display.DisplayObject;

import flash.geom.Matrix;
import flash.geom.Point;

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
     * Clears all the current scales and image locations, in prep for running a new frame.
     */
    public function clearItems () :void 
    {
        var cursor :IViewCursor = _items.createCursor();
        while (!cursor.afterLast) {
            cursor.current.screen.x = -Ground.HALF_IMAGE_SIZE;
            cursor.current.screen.y = -Ground.HALF_IMAGE_SIZE;
            cursor.current.scale = 0;
            cursor.moveNext();
        }
    }

    /** 
     * Sets a new transform for objects on a row of the display.  
     */
    public function setTransform(minY :Number, maxY :Number, transform :Matrix, scale :Number) :void
    {
        // scale coming in is much too large - bring it down a notch
        //scale *= SCALE_REDUCTION;
        var cursor :IViewCursor = _items.createCursor();
        while (!cursor.afterLast) {
            var newLocation :Point = transform.transformPoint(cursor.current.origin);
            if (newLocation.y < maxY - minY && newLocation.y >= 0 &&
                newLocation.x < UnderworldDrift.DISPLAY_WIDTH && newLocation.x >= 0) {
                cursor.current.screen.x = newLocation.x;
                cursor.current.screen.y = minY + newLocation.y;
                cursor.current.scale = scale;
            }
            cursor.moveNext();
        }
    }

    /**
     * Called when setting the correct scales and locations for this frame is complete, 
     * indicating that the sprite's real locations should be updated.
     */
    public function updateItems () :void
    {
        var cursor :IViewCursor = _items.createCursor();
        while (!cursor.afterLast) {
            //cursor.current.sprite.x = 355;
            //cursor.current.sprite.y = 100;
            cursor.current.sprite.scaleX = cursor.current.sprite.scaleY = 1;
            cursor.current.sprite.x = cursor.current.screen.x;
            cursor.current.sprite.y = cursor.current.screen.y;
            //cursor.current.sprite.scaleX = cursor.current.scale;
            //cursor.current.sprite.scaleY = cursor.current.scale;
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
                // TEMP
                //cursor.current.sprite.y = cursor.current.origin.y;
                //cursor.current.sprite.x = cursor.current.origin.x;
                cursor.moveNext();
            }
        }
        generateBlock();
    }

    protected function generateBlock () :void
    {
        for (var ii :int = 0; ii < 10; ii++) {
            var item :Object = {origin: new Point(), screen: new Point(), 
                scale: 0, sprite :new SKULL()};
            item.origin.x = Math.floor(Math.random() * Ground.IMAGE_SIZE) -
                Ground.HALF_IMAGE_SIZE;
            item.origin.y = Math.floor(Math.random() * Ground.IMAGE_SIZE) -
                Ground.HALF_IMAGE_SIZE - Ground.IMAGE_SIZE;
            // get that new sprite off the display, thank you
            item.sprite.x = item.sprite.y = -Ground.HALF_IMAGE_SIZE;
            //item.sprite.x = item.origin.x;
            //item.sprite.y = item.origin.y;
            addChild(item.sprite);
            _items.addItem(item);
        }
    }

    /** Array of blocks of scenery objects */
    protected var _items :ArrayCollection = new ArrayCollection();

    /** skull object */
    [Embed(source='rsrc/objects.swf#monster_skull')]
    protected static const SKULL :Class;

    /** amount to reduce the scale brought in from the Ground */
    protected static const SCALE_REDUCTION :Number = 0.1;
}
}
