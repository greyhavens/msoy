package {

import flash.display.Sprite;
import flash.display.DisplayObject;
import flash.display.BitmapData;

import flash.events.Event;

import mx.core.MovieClipAsset;

import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.ArrayUtil;

public class Scenery extends Sprite
{
    public static const OBSTACLE: int = 1;
    public static const BONUS: int = 2;
    public static const KART: int = 3;
    public static const FIREBALL: int = 4;

    public function Scenery (objects :Array) 
    {
        for (var ii :int = 0; ii < objects.length; ii++) {
            var item :Object = {origin: objects[ii].point, sprite :new objects[ii].cls()};
            initializeObject(item, objects[ii].type);
        }
    }

    /**
     * Called when the objects should be re-scaled for display in a new frame.
     */
    public function updateItems (translateRotate :Matrix, camera :Camera, kartLocation :Point) :void
    {
        var thisTransform :Matrix = new Matrix();
        var minScale :Number = 1 / camera.height;
        var maxScale :Number = Ground.HEIGHT / camera.height;
        var maxDistance :Number = camera.distance / minScale;
        var viewRect :Rectangle = new Rectangle(-maxDistance / 2, -maxDistance, maxDistance, 
            maxDistance);
        var nonChildren :int = 0;
        _collidingObject = null;
        for (var ii :int = 0; ii < _items.length; ii++) {
            _items[ii].transformedOrigin = translateRotate.transformPoint(_items[ii].origin);
        }
        // sort list so that items that are farther away appear in the list first.
        _items.sort(sortOnTransformedY);
        for (ii = 0; ii < _items.length; ii++) {
            if (viewRect.containsPoint(_items[ii].transformedOrigin)) {
                // scale and translate origin to the display area
                var scaleFactor :Number = camera.distance / (-_items[ii].transformedOrigin.y);
                var totalHeight :Number = scaleFactor * camera.height;
                thisTransform.identity();
                thisTransform.scale(scaleFactor, scaleFactor);
                thisTransform.translate(UnderwhirledDrift.DISPLAY_WIDTH / 2, camera.distance + 
                    totalHeight);
                _items[ii].transformedOrigin = thisTransform.transformPoint(
                    _items[ii].transformedOrigin);
                // position item
                _items[ii].sprite.x = _items[ii].transformedOrigin.x;
                _items[ii].sprite.y = _items[ii].transformedOrigin.y;
                // scale item
                _items[ii].sprite.width = _items[ii].startWidth * scaleFactor;
                _items[ii].sprite.height = _items[ii].startHeight * scaleFactor;
                // some special handling for karts
                if (_items[ii] is KartObstacle) {
                    var kart :KartObstacle = _items[ii] as KartObstacle;
                    kart.sprite.y += UnderwhirledDrift.KART_OFFSET * 
                        (scaleFactor / maxScale);
                    kart.updateAngleFrom(camera.position);
                }
                // set correct index - sometimes there are items that are not currently displayed
                if (_items[ii].sprite.parent == this) {
                    setChildIndex(_items[ii].sprite, ii - nonChildren);
                } else {
                    // prevent going out of bounds on the above setChildIndex call
                    nonChildren++;
                }
            } else {
                // make sure its off the display
                _items[ii].sprite.x = _items[ii].sprite.y = -100000;
                _items[ii].sprite.width = _items[ii].startWidth;
                _items[ii].sprite.height = _items[ii].startHeight;
            }

            if (_items[ii].sprite.parent == this && 
                Point.distance(_items[ii].origin, kartLocation) < _items[ii].radius + _kartRadius) {
                _collidingObject = _items[ii];
            }
        }
    }

    public function getCollidingObject () :Object
    {
        return _collidingObject;
    }

    public function addKart (kart :KartObstacle) :void
    {
        initializeObject(kart, KART);
    }

    public function removeObject (obj :Object) :void
    {
        if (obj.sprite.parent == this) {
            removeChild(obj.sprite);
            ArrayUtil.removeFirst(_items, obj);
        }
    }

    /**
     * This is needed so that the Scenery knows how much area the kart takes up, to properly 
     * detect collisions.
     */
    public function registerKart (kart :KartSprite) :void
    {
        _kartRadius = (getOpaqueWidth(kart) * 0.1) / 2;
    }

    public function shootFireball (pos :Point, angle :Number, isMyFireball :Boolean) :void
    {
        // slightly hacky to get the anchor point down on the bottom, like it is for every other
        // scenery object.  TODO: ask nick for a new one
        var fireball :Sprite = new Sprite();
        var bonus :Sprite = Bonus.getGameSprite(Bonus.FIREBALL);
        fireball.addChild(bonus);
        bonus.y = -fireball.height;
        // isMyFireball: don't let us shoot ourselves due to lag... if we make fireballs bounce
        // off of walls in the futre, this will need to be smarter, as it will indeed be 
        // possible to shoot ourselves
        var bonusObj :Object = { origin: pos, sprite: fireball, isMyFireball: isMyFireball};
        var bonusObjListener :Function = function (event :Event) :void {
            bonusObj.origin = bonusObj.origin.add(Point.polar(Bonus.FIREBALL_VELOCITY, angle));
            if (Math.abs(bonusObj.origin.x) > 2000 || Math.abs(bonusObj.origin.y) > 2000) {
                // don't let the damn thing float to infinity (and beyond!)
                fireball.removeEventListener(Event.ENTER_FRAME, bonusObjListener);
                fireball.parent.removeChild(fireball);
                ArrayUtil.removeFirst(_items, bonusObj);
            }
        };
        fireball.addEventListener(Event.ENTER_FRAME, bonusObjListener);
        initializeObject(bonusObj, FIREBALL);
    }

    public function removeBonus (pos :Point) :void 
    {
        var bonus :Object;
        for (var ii: int = 0; ii < _items.length; ii++) {
            if (_items[ii].origin.equals(pos)) {
                bonus = _items[ii];
                break;
            }
        }
        if (bonus != null && bonus.sprite.parent == this) {
            removeChild(bonus.sprite);
        } 
    }

    protected function initializeObject (obj :Object, type :int) :void
    {
        obj.sceneryType = type;
        obj.startWidth = obj.sprite.width * 0.1;
        obj.startHeight = obj.sprite.height * 0.1;
        // get that new sprite off the display, thank you
        obj.sprite.x = obj.sprite.y = -100000;
        obj.radius = (getOpaqueWidth(obj.sprite) * 0.1) / 2;

        addChild(obj.sprite);
        _items.push(obj);
    }

    protected function getOpaqueWidth(sprite :Sprite) :int
    {
        var bitmap :BitmapData = new BitmapData(sprite.width, 25, true, 0);
        var trans :Matrix = new Matrix();
        // test the first 25 rows of pixels, and use the widest width found;
        trans.translate(sprite.width / 2, 25);
        bitmap.draw(sprite, trans);
        var width :int = 0;
        for (var row: int = 0; row < bitmap.height; row++) {
            for (var left: int = 0; (bitmap.getPixel32(left, row) & 0xFF000000) == 0 && 
                left < sprite.width; left++);
            for (var right: int = sprite.width; (bitmap.getPixel32(right, row) & 0xFF000000) == 0 &&
                right >= 0; right--);
            width = right - left > width ? right - left : width;
        }
        return width;
    }
    
    protected function sortOnTransformedY (obj1 :Object, obj2 :Object) :int
    {
        return obj1.transformedOrigin.y < obj2.transformedOrigin.y ? -1 : 
            (obj2.transformedOrigin.y < obj1.transformedOrigin.y ? 1 : 0);
    }

    /** Collection of scenery objects */
    protected var _items :Array = new Array();

    protected var _collidingObject :Object;

    protected var _kartRadius :Number;
}
}
