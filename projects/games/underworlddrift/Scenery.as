package {

import flash.display.Sprite;
import flash.display.DisplayObject;

import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;

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
    public function updateItems (translateRotate :Matrix, distance :Number, minScale :Number,
        cameraHeight :Number) :void
    {
        var thisTransform :Matrix = new Matrix();
        var maxDistance :Number = distance / minScale;
        var viewRect :Rectangle = new Rectangle(-maxDistance / 2, -maxDistance, maxDistance, 
            maxDistance);
        for (var ii :int = 0; ii < _items.length; ii++) {
            _items[ii].transformedOrigin = translateRotate.transformPoint(_items[ii].origin);
        }
        // sort list so that items that are farther away appear in the list first.
        _items.sort(sortOnTransformedY);
        for (ii = 0; ii < _items.length; ii++) {
            if (viewRect.containsPoint(_items[ii].transformedOrigin)) {
                // scale and translate origin to the display area
                var scaleFactor :Number = distance / (-_items[ii].transformedOrigin.y);
                var totalHeight :Number = scaleFactor * cameraHeight;
                thisTransform.identity();
                thisTransform.scale(scaleFactor, scaleFactor);
                thisTransform.translate(UnderworldDrift.DISPLAY_WIDTH / 2, distance + totalHeight);
                _items[ii].transformedOrigin = thisTransform.transformPoint(
                    _items[ii].transformedOrigin);
                // position item
                _items[ii].sprite.x = _items[ii].transformedOrigin.x;
                _items[ii].sprite.y = _items[ii].transformedOrigin.y - 
                    scaleFactor * _items[ii].startHeight / 2;
                // scale item
                _items[ii].sprite.width = _items[ii].startWidth * scaleFactor;
                _items[ii].sprite.height = _items[ii].startHeight * scaleFactor;
                // set correct index
                setChildIndex(_items[ii].sprite, ii);
            } else {
                // make sure its off the display
                _items[ii].sprite.x = _items[ii].sprite.y = -Ground.HALF_IMAGE_SIZE;
                _items[ii].sprite.width = _items[ii].startWidth;
                _items[ii].sprite.height = _items[ii].startHeight;
            }
        }
    }

    /** 
     * As in the Track, this slides all the objects down by one IMAGE_SIZE, so that the track
     * can continue forward forever.
     */
    public function moveSceneryForward (xShift :int) :void
    {
        var newLength :int = -1;
        _items.sort(sortOnOriginY);
        for (var ii :int = 0; ii < _items.length; ii++) {
            if (_items[ii].origin.y > Ground.HALF_IMAGE_SIZE) {
                if (newLength == -1) newLength = ii;
                removeChild(_items[ii].sprite);
            } else {
                _items[ii].origin.y += Ground.IMAGE_SIZE;
                _items[ii].origin.x += xShift;
            }
        }
        if (newLength != -1) {
            _items.length = newLength;
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
            item.startWidth = item.sprite.width * 0.1;
            item.startHeight = item.sprite.height * 0.1;
            // get that new sprite off the display, thank you
            item.sprite.x = item.sprite.y = -Ground.HALF_IMAGE_SIZE;
            addChild(item.sprite);
            _items.push(item);
        }
    }
    
    protected function sortOnOriginY (obj1 :Object, obj2 :Object) :int
    {
        return obj1.origin.y < obj2.origin.y ? -1 : (obj2.origin.y < obj1.origin.y ? 1 : 0);
    }

    protected function sortOnTransformedY (obj1 :Object, obj2 :Object) :int
    {
        return obj1.transformedOrigin.y < obj2.transformedOrigin.y ? -1 : 
            (obj2.transformedOrigin.y < obj1.transformedOrigin.y ? 1 : 0);
    }

    /** Collection of scenery objects */
    protected var _items :Array = new Array();

    /** skull object */
    [Embed(source='rsrc/objects.swf#monster_skull')]
    protected static const SKULL :Class;
}
}
