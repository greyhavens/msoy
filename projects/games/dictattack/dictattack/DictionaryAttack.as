//
// $Id$

package dictattack {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import com.threerings.ezgame.StateChangedEvent;

import com.whirled.WhirledGameControl;

[SWF(width="453", height="553")]
public class DictionaryAttack extends Sprite
{
    /**
     * Creates and initializes our game.
     */
    public function DictionaryAttack ()
    {
        // wire up our unloader
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        // create and wire ourselves into our multiplayer game control
        _control = new WhirledGameControl(this);
        _control.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
        _control.addEventListener(StateChangedEvent.GAME_ENDED, gameDidEnd);

        // TODO: get this info from the game config
        var size :int = Content.BOARD_SIZE;
        var pcount :int = _control.isConnected() ? _control.seating.getPlayerIds().length : 4;

        // create our model and our view, and initialize them
        _model = new Model(size, _control);
        _view = new GameView(_control, _model);
        _view.init(size, pcount);
        addChild(_view);
    }

    protected function gameDidStart (event :StateChangedEvent) :void
    {
        // zero out the scores
        var pcount :int = _control.seating.getPlayerIds().length;
        if (_control.amInControl()) {
            _control.set(Model.SCORES, new Array(pcount).map(function (): int { return 0; }));
        }

        // for now we have just one round
        _model.roundDidStart();
        _view.roundDidStart();
    }

    protected function gameDidEnd (event :StateChangedEvent) :void
    {
        // for now we have just one round
        _model.roundDidEnd();
        _view.roundDidEnd();
    }

    protected function handleUnload (event :Event) :void
    {
        // TODO: clean up things that need cleaning up
    }

    protected var _control :WhirledGameControl;
    protected var _model :Model;
    protected var _view :GameView;
}

}
