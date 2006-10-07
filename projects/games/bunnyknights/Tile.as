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

    public static const TYPE_BLOCK :int = 0;
    public static const TYPE_BLACK :int = 1;
    public static const TYPE_LADDER :int = 2;
    public static const TYPE_BLOCK_REC :int = 3;
    public static const TYPE_CHECKER :int = 4;
    public static const TYPE_COLUMN_SHORT :int = 5;
    public static const TYPE_COLUMN_TALL :int = 6;
    public static const TYPE_COLUMN_GIANT :int = 7;
    public static const TYPE_EARTH :int = 8;
    public static const TYPE_EARTH_GRASS :int = 9;
    public static const TYPE_STATUE_BLUE :int = 10;
    public static const TYPE_STATUE_ORANGE :int = 11;
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

    public static function block (x :int, y :int, type :int = TYPE_BLOCK) :Tile
    {
        return new Tile(x, y, type);
    }

    public static function statue (
            x :int, y :int, type :int = TYPE_STATUE_BLUE) :Tile
    {
        return new Tile(x, y, type, LAYER_BACK, EFFECT_NONE);
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
        type :int = TYPE_BLOCK, layer :int = LAYER_MIDDLE, 
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
          case TYPE_BLOCK:
            return Bitmap(new blueBlockAsset());
          case TYPE_BLOCK_REC:
            return Bitmap(new blueBlockRecAsset());
          case TYPE_CHECKER:
            return Bitmap(new blueCheckerAsset());
          case TYPE_COLUMN_SHORT:
            return Bitmap(new blueColumnShortAsset());
          case TYPE_COLUMN_TALL:
            return Bitmap(new blueColumnTallAsset());
          case TYPE_COLUMN_GIANT:
            return Bitmap(new blueColumnGiantAsset());
          case TYPE_EARTH:
            return Bitmap(new earthAsset());
          case TYPE_EARTH_GRASS:
            return Bitmap(new earthGrassAsset());
          case TYPE_STATUE_BLUE:
            return Bitmap(new statueBlueAsset());
          case TYPE_STATUE_ORANGE:
            return Bitmap(new statueOrangeAsset());
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
            return Bitmap(new blueBlockAsset());
        }
    }

    protected var _sprite :Sprite;

    [Embed(source="rsrc/blue_block.gif")]
    protected var blueBlockAsset :Class;

    [Embed(source="rsrc/blue_block_rec.gif")]
    protected var blueBlockRecAsset :Class;

    [Embed(source="rsrc/blue_checker.gif")]
    protected var blueCheckerAsset :Class;

    [Embed(source="rsrc/blue_column_giant.gif")]
    protected var blueColumnGiantAsset :Class;

    [Embed(source="rsrc/blue_column_short.gif")]
    protected var blueColumnShortAsset :Class;

    [Embed(source="rsrc/blue_column_tall.gif")]
    protected var blueColumnTallAsset :Class;

    [Embed(source="rsrc/earth.gif")]
    protected var earthAsset :Class;
    
    [Embed(source="rsrc/earth_grass.gif")]
    protected var earthGrassAsset :Class;

    [Embed(source="rsrc/statue_blue.gif")]
    protected var statueBlueAsset :Class;

    [Embed(source="rsrc/statue_orange.gif")]
    protected var statueOrangeAsset :Class;

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
