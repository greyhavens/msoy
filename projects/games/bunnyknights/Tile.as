package {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Sprite;


public class Tile
{
    public static const LAYER_BACK :int = 0;
    public static const LAYER_ACTION_BACK :int = 1;
    public static const LAYER_MIDDLE :int = 2;
    public static const LAYER_ACTION_MIDDLE :int = 3;
    public static const LAYER_FRONT :int = 4;
    public static const LAYER_ACTION_FRONT :int = 5;
    public static const LAYER_HUD :int = 6;

    public static const TYPE_BRICK :int = 0;
    public static const TYPE_BLACK :int = 1;
    public static const TYPE_LADDER :int = 2;
    public static const TYPE_SWITCH :int = 100;
    public static const TYPE_DOOR :int = 200;

    public static const EFFECT_NONE :int = 0;
    public static const EFFECT_SOLID :int = 1;
    public static const EFFECT_LADDER :int = 2;
    public static const EFFECT_SWITCH :int = 100;
    public static const EFFECT_DOOR_CLOSED :int = 200;
    public static const EFFECT_DOOR_OPENED :int = 201;

    public static const TILE_SIZE :int = 16;
    public static const LADDER_SIZE :int = 2;
    public static const SWITCH_SIZE :int = 2;

    public static const STATE_CLOSED :int = 0;
    public static const STATE_OPENED :int = 1;
    
    public var x :int, y :int;
    public var type :int;
    public var layer :int;
    public var effect :int;
    public var state :int;
    public var width :int, height :int;
    public var assoc :int = -1;

    public static function brick (x :int, y :int) :Tile
    {
        return new Tile(x, y);
    }

    public static function ladder (x :int, y :int) :Tile
    {
        return new Tile(x, y, TYPE_LADDER, LAYER_FRONT, EFFECT_LADDER);
    }

    public static function door (x :int, y :int) :Tile
    {
        return new Tile(x, y, TYPE_DOOR, LAYER_FRONT, EFFECT_DOOR_CLOSED);
    }

    public static function tswitch (x :int, y :int) :Tile
    {
        return new Tile(x, y, TYPE_SWITCH, LAYER_FRONT, EFFECT_SWITCH);
    }

    public function Tile (x :int, y :int, 
        type :int = TYPE_BRICK, layer :int = LAYER_MIDDLE, 
        effect :int = EFFECT_SOLID)
    {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.type = type;
        this.effect = effect;
        width = getBitmapData().width / TILE_SIZE;
        height = getBitmapData().height / TILE_SIZE;
    }

    public function setState (state :int) :void
    {
        if (this.state != state) {
            BunnyKnights.log("Setting state of tiletype=" + type + 
                " to state=" + state);
            this.state = state;
            if (_sprite != null) {
                BunnyKnights.log("Switching sprite bitmap");
                _sprite.removeChildAt(0);
                _sprite.addChildAt(getBitmap(), 0);
            }
            if (type == TYPE_DOOR) {
                if (state == STATE_OPENED) {
                    effect = EFFECT_DOOR_OPENED;
                } else {
                    effect = EFFECT_DOOR_CLOSED;
                }
            }
        }
    }

    public function getSprite () :Sprite
    {
        if (_sprite == null) {
            _sprite = new Sprite();
            _sprite.addChildAt(getBitmap(), 0);
        }
        return _sprite;
    }

    public function getBitmapData () :BitmapData
    {
        return getBitmap().bitmapData;
    }

    public function getShadow (shadow :int, x :int, y :int) :int
    {
        if (effect == EFFECT_NONE || shadow == EFFECT_LADDER) 
            return shadow;
        if (type == TYPE_DOOR && x < 2 && y < 2) 
            return EFFECT_SOLID;
        return effect;
    }

    protected function getBitmap () :Bitmap
    {
        switch (type) {
          case TYPE_BRICK:
            return Bitmap(new blueTileAsset());
          case TYPE_LADDER:
            return Bitmap(new ladderTileAsset());
          case TYPE_SWITCH:
            if (state == STATE_OPENED) {
                return Bitmap(new switchOnTileAsset());
            }
            return Bitmap(new switchTileAsset());
          case TYPE_DOOR:
            if (state == STATE_OPENED) {
                return Bitmap(new doorOpenedTileAsset());
            }
            return Bitmap(new doorTileAsset());
          default:
            return Bitmap(new blueTileAsset());
        }
    }

    protected var _sprite :Sprite;

    [Embed(source="rsrc/blue_tile.gif")]
    protected var blueTileAsset :Class;

    [Embed(source="rsrc/ladder_middle.gif")]
    protected var ladderTileAsset :Class;

    [Embed(source="rsrc/switch_off.gif")]
    protected var switchTileAsset :Class;

    [Embed(source="rsrc/switch_on.gif")]
    protected var switchOnTileAsset :Class;

    [Embed(source="rsrc/door_closed.gif")]
    protected var doorTileAsset :Class;

    [Embed(source="rsrc/door_open.gif")]
    protected var doorOpenedTileAsset :Class;
}
}
