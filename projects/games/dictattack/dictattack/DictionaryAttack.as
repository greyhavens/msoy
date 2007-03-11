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

        // see if we're wired up to the world
        _control = new WhirledGameControl(this);
        if (!_control.isConnected()) {
            // set up some defaults so that we're visible
            _view = new GameView(_control, null);
            _view.init(Content.BOARD_SIZE, 4);
            addChild(_view);
            // TODO: attract mode?
            return;
        }

        // wire up some listeners
        _control.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
        _control.addEventListener(StateChangedEvent.GAME_ENDED, gameDidEnd);
    }

    protected function gameDidStart (event :StateChangedEvent) :void
    {
        // TODO: get this info from the game config
        var size :int = Content.BOARD_SIZE;

        // zero out the scores
        var pcount :int = _control.seating.getPlayerIds().length;
        if (_control.amInControl()) {
            _control.set(Model.SCORES, new Array(pcount).map(function (): int { return 0; }));
        }

        // create our model and our view, and initialze them
        _model = new Model(size, _control);
        _view = new GameView(_control, _model);
        _view.init(size, pcount);
        addChild(_view);

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
