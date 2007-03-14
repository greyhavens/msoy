//
// $Id$

package {

import flash.external.ExternalInterface;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import com.threerings.ezgame.StateChangedEvent;

import com.whirled.WhirledGameControl;

[SWF(width="540", height="420", backgroundColor=0xAA3311)]
public class Go extends Sprite
{
    public const WIDTH :Number = 540;
    public const HEIGHT :Number = 420;

    /**
     * Creates and initializes our game.
     */
    public function Go ()
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        _control = new WhirledGameControl(this);
        if (_control.isConnected()) {
            _control.addEventListener(StateChangedEvent.TURN_CHANGED, turnChanged);
            _control.addEventListener(StateChangedEvent.GAME_STARTED, gameDidStart);
            _control.addEventListener(StateChangedEvent.GAME_ENDED, gameDidEnd);
            _model = new GoModel(Content.BOARD_SIZE, _control);

        } else {
            _model = null;
        }

        _view = new GoView(_control, _model);
        _view.init(Content.BOARD_SIZE);

        addChild(_view);
    }

    protected function gameDidStart (event :StateChangedEvent) :void
    {
        _model.gameDidStart();
        _view.gameDidStart();
    }

    protected function gameDidEnd (event :StateChangedEvent) :void
    {
        _model.gameDidEnd();
        _view.gameDidEnd();
    }

    protected function turnChanged (event :StateChangedEvent) :void
    {
        if (_control.amInControl()) {
            // test for end game and such
        }

        _view.turnChanged();
    }

    protected function handleUnload (event :Event) :void
    {
        // TODO: clean up things that need cleaning up
    }

    protected var _control :WhirledGameControl;
    protected var _model :GoModel;
    protected var _view :GoView;
}

}
