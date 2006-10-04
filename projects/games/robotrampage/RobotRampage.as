package {

import flash.display.Sprite;
import flash.events.KeyboardEvent;
import flash.external.ExternalInterface;
import flash.ui.Keyboard;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;

import flash.text.StyleSheet;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;

[SWF(width="640", height="480")]
public class RobotRampage extends Sprite
    implements Game, PropertyChangedListener, StateChangedListener
{
    public function RobotRampage ()
    {
        _robotFactory = new RobotFactory();
    }

    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        _myIndex = _gameObj.getMyIndex();

        graphics.clear();
        graphics.beginFill(0xCCCCFF);
        graphics.drawRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        graphics.endFill();

        addMoonBases();

        addRobot();
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    public function addMoonBases () : void
    {
        var names :Array = _gameObj.getPlayerNames();
        var count :int = names.length;
        _bases = new Array(count);

        var radius :int = (Math.min(SCREEN_WIDTH, SCREEN_HEIGHT) / 2) - 
            MoonBase.MOON_BASE_RADIUS;

        for (var ii :int = 0; ii < count; ii++) {
            // Make new base
            var base :MoonBase = new MoonBase(names[ii], ii);
            _bases[ii] = base;
            var angle :Number = ii * 2 * Math.PI / count;

            // Place it someplace sane-ish
            /* FIXME: This needs some fancier brains since we want to throw 
             * some perspective on this, so the area immediately in front of 
             * you is bigger.
             * Plus, with that, we always want ourselves to be at the bottom, 
             * viewing the kite (or triangle, or...single line) shaped playfield
             * from that perspective.
             * For now though, meh.
             */
            base.x = (SCREEN_WIDTH/2) - (radius * Math.sin(angle));
            base.y = (SCREEN_HEIGHT/2) + (radius * Math.cos(angle));

            addChild(base);
        }
    }

    /**
     * Adds a new robot with random attributes and no target.
     */
    public function addRobot () : void
    {
        var robot :Robot = new Robot(_robotFactory);
        robot.x = width/2;
        robot.y = height/2;

        addChild(robot);
    }

    /** Our game object. */
    protected var _gameObj :EZGame;

    /** The index of this particular player. */
    protected var _myIndex :int;

    /** An array of moonbases. */
    protected var _bases :Array;

    /** A factory for building robots. */
    protected var _robotFactory :RobotFactory;

    /** Width of our play area. */
    protected static const SCREEN_WIDTH :int = 640;

    /** Height of our play area. */
    protected static const SCREEN_HEIGHT :int = 480;

}
}
