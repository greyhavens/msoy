package {

import flash.display.Sprite;

public class MoonBase extends Sprite
{

    public function MoonBase (name :String, playerIndex :int)
    {
        _name = name;
        _playerIndex = playerIndex;
        _health = BASE_MAX_HEALTH;


        graphics.clear();
        graphics.beginFill(uint(BASE_COLORS[_playerIndex]));
        graphics.drawCircle(0, 0, MOON_BASE_RADIUS);
        graphics.endFill();
    }


    /** The name of the player controlling this base. */
    protected var _name :String;

    /** The index of the player controlling this base. */
    protected var _playerIndex :int;

    /** The current health of this base. */
    protected var _health :int;

    /** The colors to make our moon bases. */
    protected static const BASE_COLORS :Array = 
        [0xFFFF00, 0xFF00FF, 0x00FFFF, 0xFFFFFF];

    /** The maximum health of a base. */
    protected static const BASE_MAX_HEALTH :int = 100;

    /** The damage done by a robot hitting us. */
    protected static const ROBOT_HIT_DAMAGE :int = 10;

    public static const MOON_BASE_RADIUS :int = 10;
}
}
