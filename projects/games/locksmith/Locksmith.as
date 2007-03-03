package {

import flash.display.Sprite;

import flash.events.KeyboardEvent;

import flash.ui.Keyboard;

import com.threerings.ezgame.EZGameControl;
import com.threerings.ezgame.HostCoordinator;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.StateChangedEvent;

import com.threerings.util.ArrayUtil;

[SWF(width="500", height="500")]
public class Locksmith extends Sprite implements MessageReceivedListener, StateChangedListener
{
    public static const DISPLAY_SIZE :int = 500;

    public function Locksmith ()
    {
        _gameCtrl = new EZGameControl(this);

        if (_gameCtrl.isConnected()) {
            _gameCtrl.registerListener(this);
            _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyDownHandler);
            _coord = new HostCoordinator(_gameCtrl);
        } else {
            // TODO: Something that makes sense
        }
    }

    public function keyDownHandler (event :KeyboardEvent) :void
    {
        switch(event.keyCode) {
        case Keyboard.LEFT:
            break;
        case Keyboard.RIGHT:
            break;
        }
    }

    public function messageReceived (event :MessageReceivedEvent) :void
    {
        if (event.name == "newRings") {
            var rings :Array = event.value as Array;
            for (var ring :int = 0; ring < 4; ring++) {
                addChild(_rings[ring] = new RingSprite(ring+1, rings[ring]));
                _rings[ring].x += DISPLAY_SIZE / 2;
                _rings[ring].y += DISPLAY_SIZE / 2;
            }
        }
    }

    public function stateChanged (event :StateChangedEvent) :void
    {
        if (event.type == StateChangedEvent.GAME_STARTED && _coord.status == 
            HostCoordinator.STATUS_HOST) {
            // create rings, with random holes
            var rings :Array;
            var holes :Array;
            for (var ii :int = 1; ii <= 4; ii++) {
                holes = new Array();
                for (var hole :int = 0; hole < (ii == 4 ? 6 : Math.pow(2, ii + 1) / 2); hole++) {
                    var pos :int = 0;
                    do {
                        pos = ((Math.random() * 8) as int);
                    } while (ArrayUtil.contains(holes, pos));
                    holes.push(pos);
                }
                rings.push(holes);
            }
            _gameCtrl.sendMessage("newRings", rings);
        }
    }

    protected var _rings :Array;

    protected var _gameCtrl :EZGameControl;

    protected var _coord :HostCoordinator;
}
}
