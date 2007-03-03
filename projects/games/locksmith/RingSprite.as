package {

import flash.display.Sprite;
import flash.display.Shape;

public class RingSprite extends Sprite 
{
    public static const RINGS_SIZE :int = 500;
    public static const SIZE_PER_RING :int = 34;

    public static const STATIONARY :int = 0;
    public static const CLOCKWISE :int = -1;
    public static const COUNTER_CLOCKWISE :int = 1;

    public function RingSprite (ringNumber :int, holes :Array) 
    {
        _ringNumber = ringNumber;
        _holes = holes;
        _ring = new Shape();
        _ring.graphics.beginFill(_colorsInactive[ringNumber-1]);
        _ring.graphics.drawCircle(0, 0, (ringNumber + 1) * SIZE_PER_RING);
        _ring.graphics.endFill();
        addChild(_ring);
    }

    protected static const _colorsInactive :Array = [ 0x3E4E57, 0x51636E, 0x70828C, 0x899FAB ];
    protected static const _colorsActive :Array = [ 0x5B3C1C, 0x774E23, 0xA5662E, 0xCA7D38 ];

    protected var _ringNumber :int;
    protected var _position :int = 0;
    protected var _holes :Array;
    protected var _ring :Shape;
}
}
