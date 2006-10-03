package {

import flash.display.Sprite;

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
        addChild(new Bunny());
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObject = gameObj;
    }

    // from StateChangedListener
    public function stateChanged (event :StateChangedEvent) :void
    {
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
    }

    /** Out game object. */
    protected var _gameObject :EZGame;
}
}
