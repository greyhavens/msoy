package {

import flash.display.Sprite;

public class MoonBase extends Sprite
{

    public function MoonBase (name :String, playerIndex :int)
    {
        _name = name;
        _playerIndex = playerIndex;
        _health = BASE_MAX_HEALTH;

        updateGraphics();
    }

    protected function updateGraphics () :void
    {
        var color :uint = isDestroyed() ? 0x000000 : BASE_COLORS[_playerIndex];

        // TODO: Show something about our state of disrepair

        graphics.clear();
        graphics.beginFill(color);
        graphics.drawCircle(0, 0, MOON_BASE_RADIUS);
        graphics.endFill();
    }

    /**
     * A robot just smashed up against us. Ouch.
     */
    public function takeDamage () :void
    {
        _health -= ROBOT_HIT_DAMAGE;

        if (isDestroyed()) {
            destroyBase();
        }

        updateGraphics();
    }

    /**
     * Returns true if we've taken enough damage to be dead.
     */
    public function isDestroyed () :Boolean
    {
        return _health <= 0;
    }

    public function getPlayerIndex () :int
    {
        return _playerIndex;
    }

    /**
     * The bitter end.
     */
    protected function destroyBase() :void
    {
        if (!isDestroyed()) {
            // Reports of my death have been greatly exagerated.
            return;
        }

        // TODO: really report this and do sane things.
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
    protected static const ROBOT_HIT_DAMAGE :int = 30;

    public static const MOON_BASE_RADIUS :int = 15;
}
}
