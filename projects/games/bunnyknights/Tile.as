package {

import flash.display.Sprite;

import mx.core.BitmapAsset;

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

    public static const EFFECT_NONE :int = 0;
    public static const EFFECT_SOLID :int = 1;
    
    public var x :int, y :int;
    public var type :int;
    public var layer :int;
    public var effect :int;

    public function Tile (x :int, y :int, 
        type :int = TYPE_BRICK, layer :int = LAYER_MIDDLE, 
        effect :int = EFFECT_NONE)
    {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.type = type;
        this.effect = effect;
    }

    public function getSprite () :Sprite
    {
        if (_sprite == null) {
            _sprite = new Sprite();
            _sprite.addChild(BitmapAsset(new tileAsset()));
        }
        return _sprite;
    }

    protected var _sprite :Sprite;

    [Embed(source="rsrc/blue_tile.gif")]
    protected var tileAsset :Class;
}
}
