package {

import flash.display.Sprite;

public class Bonus extends Sprite
{
    public static const BLUE_BLAZES :int = 0;
    public static const FIREBALL :int = 1;
    public static const SHIELD :int = 2;

    public static const SHIELD_DURATION :int = 20000; // in ms

    /**
     * Randomly selects a bonus and displays it.
     */
    public function Bonus ()
    {
        _type = Math.floor(3 * Math.random());
        switch (_type) {
        case BLUE_BLAZES: addChild(new BLUE_BLAZES_THUMB()); break;
        case FIREBALL: addChild(new FIREBALL_THUMB()); break;
        case SHIELD: addChild(new SHIELD_THUMB()); break;
        }
    }

    public function activate (kart :Kart) :void 
    {
        switch (_type) {
        case BLUE_BLAZES: activateBlueBlazes(kart); break;
        case FIREBALL: activateFireball(kart); break;
        case SHIELD: activateShield(kart); break;
        }
    }

    public function get type () :int
    {
        return _type;
    }

    public static function getGameSprite (type :int) :Sprite
    {
        switch (type) {
        case FIREBALL: return new FIREBALL_GAME();
        case SHIELD: return new SHIELD_GAME();
        default: return null;
        }
    }

    protected function activateBlueBlazes (kart :Kart) :void
    {
        kart.boostSpeed(3);
    }

    protected function activateFireball (kart :Kart) :void
    {
        kart.dispatchEvent(new KartEvent(KartEvent.FIREBALL, true));
    }

    protected function activateShield (kart :Kart) :void
    {
        kart.dispatchEvent(new KartEvent(KartEvent.SHIELD, true));
    }

    [Embed('rsrc/power_ups.swf#blue_blazes_ui')]
    protected static const BLUE_BLAZES_THUMB :Class;

    [Embed('rsrc/power_ups.swf#fireball_game')]
    protected static const FIREBALL_GAME :Class;
    [Embed('rsrc/power_ups.swf#fireball_ui')]
    protected static const FIREBALL_THUMB :Class;

    [Embed('rsrc/power_ups.swf#shield_game')]
    protected static const SHIELD_GAME :Class;
    [Embed('rsrc/power_ups.swf#shield_ui')]
    protected static const SHIELD_THUMB :Class;

    protected var _type :int;

    protected var _gameSprite :Sprite;
}
}
