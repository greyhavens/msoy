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

[SWF(width="640", height="480")]
public class BunnyKnights extends Sprite
    implements Game, PropertyChangedListener, StateChangedListener
{
    public function BunnyKnights ()
    {
    }

    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObject = gameObj;
        graphics.clear();
        graphics.beginFill(0x2222CC);
        graphics.drawRect(0, 0, 640, 480);
        graphics.endFill();
        _bunny = new Bunny();
        addChild(_bunny);
        _bunny.y = 300;
        stage.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    protected function keyDownHandler (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
          case Keyboard.LEFT:
            _bunny.x = Math.max(0, _bunny.x - 5);
            break;
          case Keyboard.RIGHT:
            _bunny.x = Math.min(640 - _bunny.width, _bunny.x + 5);
            break;
        }
    }

    /** Out game object. */
    protected var _gameObject :EZGame;

    protected var _bunny :Bunny;
}
}
