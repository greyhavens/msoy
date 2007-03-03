//
// $Id$
//
// @project@ - a game for Whirled

package {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.MouseEvent;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.MessageReceivedEvent;

import com.whirled.WhirledGameControl;

[SWF(width="400", height="400")]
public class @project@ extends Sprite
    implements Game
{
    public function @project@ ()
    {
        _control = new WhirledGameControl(this);
    }

    // from interface Game
    public function setGameObject (gameObj :EZGame) :void
    {
        // set up our listeners
        _gameObj = gameObj;

        // Uncomment these to hear when your game starts and ends
        // _gameObj.addEventListener(StateChangedEvent.GAME_STARTED, gameStarted);
        // _gameObj.addEventListener(StateChangedEvent.GAME_ENDED, gameEnded);

        // Uncomment this to hear when a game property is changed
        // _gameObj.addEventListener(PropertyChangedEvent.TYPE, propChanged);
    }

    /**
     * This is called when your game starts.
     */
    protected function gameStarted (event :StateChangedEvent) :void
    {
    }

    /**
     * This is called when your game ends.
     */
    protected function gameEnded (event :StateChangedEvent) :void
    {
    }

    /**
     * This is called when a game property changes.
     */
    protected function propChanged (event :PropertyChangedEvent) :void
    {
    }

    protected var _control :WhirledGameControl;
    protected var _gameObj :EZGame;
}
}
